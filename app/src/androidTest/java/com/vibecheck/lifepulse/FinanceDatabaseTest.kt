package com.vibecheck.lifepulse

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.core.Ledger
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.DefaultCategories
import com.vibecheck.lifepulse.data.local.entity.AccountEntity
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import com.vibecheck.lifepulse.data.local.entity.HabitEntity
import com.vibecheck.lifepulse.data.repository.ExpenseRepositoryImpl
import com.vibecheck.lifepulse.data.repository.FinanceCurrencyStore
import com.vibecheck.lifepulse.di.DatabaseModule
import com.vibecheck.lifepulse.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Locale
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: LifePulseDatabase
    private lateinit var repository: ExpenseRepositoryImpl
    private var expenseCategory = 0L
    private var incomeCategory = 0L

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, LifePulseDatabase::class.java).build()
        database.financeSettingsDao().save(com.vibecheck.lifepulse.data.local.entity.FinanceSettingsEntity(currencyCode = "USD", confirmed = true))
        repository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao(), database.accountDao(), database, context)
        database.accountDao().insert(AccountEntity(1, "Cash"))
        database.accountDao().insert(AccountEntity(2, "Bank"))
        expenseCategory = database.categoryDao().insertCategory(CategoryEntity(name = "Food", colorHex = "#FF7043"))
        incomeCategory = database.categoryDao().insertCategory(CategoryEntity(name = "Salary", colorHex = "#66BB6A", kind = "INCOME"))
    }
    @After fun cleanup() { database.close() }

    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Expected validation to reject the operation")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun transferIsAtomicAndExcludedFromSpending() = runBlocking {
        repository.saveTransaction(TransactionDraft(type = TransactionType.INCOME, amountMinor = 10000, accountId = 1, categoryId = incomeCategory))
        repository.saveTransaction(TransactionDraft(type = TransactionType.EXPENSE, amountMinor = 2000, accountId = 1, categoryId = expenseCategory))
        repository.saveTransaction(TransactionDraft(type = TransactionType.TRANSFER, amountMinor = 3000, accountId = 1, toAccountId = 2))
        val transactions = repository.observeTransactions().first()
        val accounts = repository.observeAccounts().first()
        assertEquals(5000L, Ledger.balance(accounts[0], transactions))
        assertEquals(3000L, Ledger.balance(accounts[1], transactions))
        assertEquals(20.0, repository.observeTotalInRange(0, Long.MAX_VALUE).first(), 0.00001)
        rejected { repository.saveTransaction(TransactionDraft(type = TransactionType.TRANSFER, amountMinor = 500, accountId = 1, toAccountId = 1)) }
        assertEquals(3, repository.observeTransactions().first().size)
    }

    @Test fun partialRepaymentsCanBeEditedAndDeletedWithoutDoubleCounting() = runBlocking {
        val loan = repository.saveTransaction(TransactionDraft(type = TransactionType.LEND, amountMinor = 10000, accountId = 1, person = "Alex", dateTimestamp = 1000))
        val repayment = repository.saveTransaction(TransactionDraft(type = TransactionType.REPAYMENT_RECEIVED, amountMinor = 2500, accountId = 2, loanId = loan, person = "Alex", dateTimestamp = 2000))
        var transactions = repository.observeTransactions().first()
        assertEquals(7500L, Ledger.outstanding(transactions.first { it.id == loan }, transactions))
        rejected { repository.saveTransaction(TransactionDraft(type = TransactionType.REPAYMENT_RECEIVED, amountMinor = 8000, accountId = 2, loanId = loan, dateTimestamp = 3000)) }
        rejected { repository.deleteExpense(loan) }
        repository.saveTransaction(TransactionDraft(id = repayment, type = TransactionType.REPAYMENT_RECEIVED, amountMinor = 5000, accountId = 2, loanId = loan, person = "Alex", dateTimestamp = 2000))
        transactions = repository.observeTransactions().first()
        assertEquals(5000L, Ledger.outstanding(transactions.first { it.id == loan }, transactions))
        repository.deleteExpense(repayment)
        repository.deleteExpense(loan)
        assertTrue(repository.observeTransactions().first().isEmpty())
    }

    @Test fun borrowingIsNotIncomeAndRepaymentMustMatchLoanDirection() = runBlocking {
        val loan = repository.saveTransaction(TransactionDraft(type = TransactionType.BORROW, amountMinor = 5000, accountId = 1, person = "Sam", dateTimestamp = 1000))
        rejected { repository.saveTransaction(TransactionDraft(type = TransactionType.REPAYMENT_RECEIVED, amountMinor = 100, accountId = 1, loanId = loan, dateTimestamp = 2000)) }
        rejected { repository.saveTransaction(TransactionDraft(type = TransactionType.REPAYMENT_PAID, amountMinor = 100, accountId = 1, loanId = loan, dateTimestamp = 500)) }
        repository.saveTransaction(TransactionDraft(type = TransactionType.REPAYMENT_PAID, amountMinor = 2000, accountId = 1, loanId = loan, person = "Sam", dateTimestamp = 2000))
        assertEquals(3000L, Ledger.balance(repository.observeAccounts().first()[0], repository.observeTransactions().first()))
        assertEquals(0.0, repository.observeTotalInRange(0, Long.MAX_VALUE).first(), 0.00001)
    }

    @Test fun editingAndCategoryDeletionPreserveHistory() = runBlocking {
        val id = repository.saveTransaction(TransactionDraft(amountMinor = 1234, accountId = 1, categoryId = expenseCategory, note = "Lunch", dateTimestamp = 1000))
        repository.deleteCategory(expenseCategory)
        assertTrue(repository.observeCategories().first().none { it.id == expenseCategory })
        repository.saveTransaction(TransactionDraft(id = id, amountMinor = 1500, accountId = 2, categoryId = expenseCategory, note = "Updated", dateTimestamp = 2000))
        val record = repository.observeTransactions().first().single()
        assertEquals("Food", record.categoryName)
        assertEquals(1500L, record.amountMinor)
        assertEquals(2L, record.accountId)
        assertEquals("Updated", record.note)
        rejected { repository.saveTransaction(TransactionDraft(type = TransactionType.INCOME, amountMinor = 100, accountId = 1, categoryId = expenseCategory)) }
    }

    @Test fun firstReleaseSeedsDefaultsAndKeepsRecordsAfterReopening() = runBlocking {
        val prefix = "release-baseline-${System.nanoTime()}"
        val isolated = object : ContextWrapper(context) {
            override fun getDatabasePath(name: String): File = context.getDatabasePath("$prefix-$name")
        }
        val legacyFile = isolated.getDatabasePath("lifepulse.db")
        legacyFile.parentFile!!.mkdirs()
        legacyFile.writeText("Unreleased development data")
        var fresh = DatabaseModule.provideDatabase(isolated)
        try {
            assertEquals(1, fresh.openHelper.writableDatabase.version)
            assertEquals("Unreleased development data", legacyFile.readText())
            assertTrue(fresh.habitDao().getAllHabitsOnce().isEmpty())
            assertTrue(fresh.expenseDao().observeAllTransactions().first().isEmpty())
            assertEquals(0L, fresh.accountDao().get(1)?.openingMinor)
            val categories = fresh.categoryDao().observeCategories().first()
            assertEquals(DefaultCategories.all(context).size, categories.size)
            assertTrue(categories.all { it.isDefault && !it.isDeleted && it.kind == "EXPENSE" })
            val currency = FinanceCurrencyStore(isolated, fresh, Locale.US)
            assertFalse(currency.initialize().confirmed)
            currency.select("USD")
            val repo = ExpenseRepositoryImpl(fresh.expenseDao(), fresh.categoryDao(), fresh.accountDao(), fresh, isolated)
            val transaction = repo.saveTransaction(TransactionDraft(amountMinor = 1234, accountId = 1,
                categoryId = categories.first().id, note = "First release entry"))
            val habit = fresh.habitDao().insertHabit(HabitEntity(title = "Walk", frequency = "DAILY"))
            fresh.close()
            fresh = DatabaseModule.provideDatabase(isolated)
            assertEquals(1234L, fresh.expenseDao().get(transaction)?.amountMinor)
            assertEquals("Walk", fresh.habitDao().getHabitById(habit)?.title)
            assertEquals("USD", fresh.financeSettingsDao().get()?.currencyCode)
            assertTrue(fresh.financeSettingsDao().get()!!.confirmed)
            assertEquals(1, fresh.accountDao().observeAccounts().first().size)
            assertEquals(categories.size, fresh.categoryDao().observeCategories().first().size)
        } finally {
            fresh.close()
            context.deleteDatabase("$prefix-${LifePulseDatabase.NAME}")
            context.deleteDatabase("$prefix-lifepulse.db")
        }
    }
}
