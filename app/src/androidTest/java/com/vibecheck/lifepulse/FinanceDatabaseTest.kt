package com.vibecheck.lifepulse

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibecheck.lifepulse.core.Ledger
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.AccountEntity
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import com.vibecheck.lifepulse.data.repository.ExpenseRepositoryImpl
import com.vibecheck.lifepulse.di.DatabaseModule
import com.vibecheck.lifepulse.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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

    @Test fun legacyMigrationPreservesExpensesAndHabits() = runBlocking {
        val name = "finance-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.vibecheck.lifepulse.data.local.LifePulseDatabase/4.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (index in 0 until entities.length()) {
                        val entity = entities.getJSONObject(index)
                        val table = entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.getJSONArray("indices")
                        for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.writableDatabase.apply {
            execSQL("INSERT INTO categories (id,name,colorHex,isDefault,isDeleted) VALUES (1,'Food','#FF7043',1,0)")
            execSQL("INSERT INTO expenses (id,categoryId,amount,dateTimestamp,note) VALUES (7,1,12.345,1000,'Old lunch')")
            execSQL("INSERT INTO habits (id,title,frequency,createdAt) VALUES (1,'Walk','DAILY',0)")
            execSQL("INSERT INTO habit_logs (id,habitId,completedDate) VALUES (1,1,'2026-10-01')")
        }
        helper.close()
        val upgraded = Room.databaseBuilder(context, LifePulseDatabase::class.java, name)
            .addMigrations(DatabaseModule.MIGRATION_4_5).build()
        try {
            val repo = ExpenseRepositoryImpl(upgraded.expenseDao(), upgraded.categoryDao(), upgraded.accountDao(), upgraded, context)
            val record = repo.observeTransactions().first().single()
            assertEquals(7L, record.id)
            assertEquals(1235L, record.amountMinor)
            assertEquals("Old lunch", record.note)
            assertEquals("Food", record.categoryName)
            assertEquals(TransactionType.EXPENSE, record.type)
            assertEquals(1L, record.accountId)
            assertEquals("Cash", repo.observeAccounts().first().single().name)
            assertEquals("Walk", upgraded.habitDao().getHabitById(1)?.title)
            assertEquals(listOf("2026-10-01"), upgraded.habitDao().getCompletionDates(1))
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
}
