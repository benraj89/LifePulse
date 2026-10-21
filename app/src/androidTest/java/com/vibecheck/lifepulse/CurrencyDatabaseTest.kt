package com.vibecheck.lifepulse

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.*
import com.vibecheck.lifepulse.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CurrencyDatabaseTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "currency-test-${System.nanoTime()}"
    private val context = object : ContextWrapper(appContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(prefsName, mode)
    }
    private lateinit var database: LifePulseDatabase
    @Before fun setup() { database = Room.inMemoryDatabaseBuilder(context, LifePulseDatabase::class.java).build() }
    @After fun cleanup() { database.close(); appContext.deleteSharedPreferences(prefsName) }

    @Test fun regionSuggestionRequiresConfirmationAndDoesNotFollowLaterRegionChanges() = runBlocking {
        val store = FinanceCurrencyStore(context, database, Locale.JAPAN)
        val suggestion = store.initialize()
        assertEquals("JPY", suggestion.currencyCode)
        assertFalse(suggestion.confirmed)
        store.select("JPY")
        val later = FinanceCurrencyStore(context, database, Locale.GERMANY).initialize()
        assertEquals("JPY", later.currencyCode)
        assertTrue(later.confirmed)
    }

    @Test fun missingRegionLeavesSelectionEmptyInsteadOfAssigningINR() = runBlocking {
        val initial = FinanceCurrencyStore(context, database, Locale.ENGLISH).initialize()
        assertNull(initial.currencyCode)
        assertFalse(initial.confirmed)
    }

    @Test fun legacyPreferenceMigratesOnceAndDatabaseWinsAfterward() = runBlocking {
        context.getSharedPreferences("finance_settings", Context.MODE_PRIVATE).edit().putString("currency", "GBP").commit()
        val store = FinanceCurrencyStore(context, database, Locale.JAPAN)
        assertEquals("GBP", store.initialize().currencyCode)
        context.getSharedPreferences("finance_settings", Context.MODE_PRIVATE).edit().putString("currency", "INR").commit()
        assertEquals("GBP", store.initialize().currencyCode)
        assertTrue(database.financeSettingsDao().get()!!.confirmed)
    }

    @Test fun missingLegacyPreferenceRequiresOriginalCurrencyWithoutReinterpretingMinorUnits() = runBlocking {
        database.accountDao().insert(AccountEntity(1, "Cash", 1234))
        val store = FinanceCurrencyStore(context, database, Locale.JAPAN)
        val initial = store.initialize()
        assertNull(initial.currencyCode)
        assertFalse(initial.confirmed)
        try { store.select("JPY"); fail("Legacy records used hundredths") }
        catch (_: IllegalArgumentException) { }
        store.select("GBP")
        assertEquals("GBP", database.financeSettingsDao().get()?.currencyCode)
        assertEquals(1234L, database.accountDao().get(1)?.openingMinor)
    }

    @Test fun transactionsPreventRelabellingEvenWhenTheirAmountWouldFitAnotherCurrency() = runBlocking {
        val store = FinanceCurrencyStore(context, database, Locale.US)
        store.select("USD")
        database.accountDao().insert(AccountEntity(1, "Cash"))
        database.expenseDao().insertExpense(ExpenseEntity(1, amountMinor = 1234, type = "LEND", person = "Alex"))
        try { store.select("JPY"); fail("Existing records must retain their currency") }
        catch (_: IllegalArgumentException) { }
        assertEquals("USD", database.financeSettingsDao().get()?.currencyCode)
        assertEquals(1234L, database.expenseDao().get(1)?.amountMinor)
    }

    @Test fun negativeOpeningBalanceAlsoLocksCurrencyButEmptyAccountsDoNot() = runBlocking {
        val store = FinanceCurrencyStore(context, database, Locale.US)
        store.select("USD")
        database.accountDao().insert(AccountEntity(1, "Cash"))
        store.select("KWD")
        database.accountDao().update(AccountEntity(1, "Cash", -1))
        try { store.select("USD"); fail("Opening balances must retain their currency") }
        catch (_: IllegalArgumentException) { }
        assertEquals("KWD", database.financeSettingsDao().get()?.currencyCode)
    }

    private suspend fun assertTotal(code: String, minor: Long, expected: Double) {
        FinanceCurrencyStore(context, database, Locale.US).select(code)
        database.accountDao().insert(AccountEntity(1, "Cash"))
        database.categoryDao().insertCategory(CategoryEntity(1, "Food", "#FF7043"))
        val repository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao(), database.accountDao(), database, context)
        repository.saveTransaction(com.vibecheck.lifepulse.domain.model.TransactionDraft(amountMinor = minor, accountId = 1, categoryId = 1))
        assertEquals(expected, repository.observeTotalInRange(0, Long.MAX_VALUE).first(), 0.000001)
        assertEquals(expected, repository.observeTotalsByCategory(0, Long.MAX_VALUE).first().single().total, 0.000001)
        assertEquals(minor, Money.parse(Money.input(minor, code), code))
    }
    @Test fun yenTotalsUseWholeUnits() = runBlocking { assertTotal("JPY", 1234, 1234.0) }
    @Test fun dinarTotalsUseThousandths() = runBlocking { assertTotal("KWD", 12345, 12.345) }

    @Test fun draftCreatedBeforeCurrencyChangeCannotBeSavedUnderAnotherCurrency() = runBlocking {
        val store = FinanceCurrencyStore(context, database, Locale.US)
        store.select("USD")
        database.accountDao().insert(AccountEntity(1, "Cash"))
        val draft = com.vibecheck.lifepulse.domain.model.TransactionDraft(type = com.vibecheck.lifepulse.domain.model.TransactionType.LEND,
            amountMinor = 1234, accountId = 1, person = "Alex", currencyCode = "USD")
        store.select("JPY")
        val repository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao(), database.accountDao(), database, context)
        try { repository.saveTransaction(draft); fail("The draft must not be reinterpreted as yen") }
        catch (_: IllegalArgumentException) { }
        assertTrue(repository.observeTransactions().first().isEmpty())
    }
}
