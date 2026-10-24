package com.vibecheck.lifepulse

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.FinanceSettingsEntity
import com.vibecheck.lifepulse.data.repository.ExpenseRepositoryImpl
import com.vibecheck.lifepulse.data.repository.FinanceCurrencyStore
import com.vibecheck.lifepulse.ui.expenses.CurrencyGate
import com.vibecheck.lifepulse.ui.expenses.CurrencySetupViewModel
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CurrencyGateUiTest {
    @get:Rule val compose = createComposeRule()
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "currency-gate-${System.nanoTime()}"
    private val context = object : ContextWrapper(appContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = super.getSharedPreferences(prefsName, mode)
    }
    private lateinit var database: LifePulseDatabase
    private val store = ViewModelStore()
    private var originalCurrency = "USD"
    @Before fun setup() {
        originalCurrency = Money.currencyCode
        database = Room.inMemoryDatabaseBuilder(context, LifePulseDatabase::class.java).build()
    }
    @After fun cleanup() { store.clear(); database.close(); appContext.deleteSharedPreferences(prefsName); Money.currencyCode = originalCurrency }

    private fun showGate() {
        val repository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao(), database.accountDao(), database, context)
        val viewModel = CurrencySetupViewModel(repository)
        store.put("currency_setup", viewModel)
        compose.setContent { NeoBrutalismTheme {
            CurrencyGate(viewModel) { Box(Modifier.fillMaxSize().testTag("financial_content")) }
        } }
    }

    @Test fun financialScreensOpenOnlyAfterCurrencyIsConfirmedAndPersisted() {
        runBlocking { FinanceCurrencyStore(context, database, Locale.ENGLISH).initialize() }
        showGate()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("currency_search").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("financial_content").assertDoesNotExist()
        compose.onNodeWithText("Save currency").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("JPY")
        compose.onNodeWithTag("currency_JPY").performClick()
        compose.onNode(hasSetTextAction()).performImeAction()
        // Invoke the accessible action while Android is moving the footer after IME dismissal.
        compose.onNodeWithText("Save currency").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("financial_content").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("JPY", Money.currencyCode)
        runBlocking { assertEquals(FinanceSettingsEntity(currencyCode = "JPY", confirmed = true), database.financeSettingsDao().get()) }
    }

    @Test fun savedCurrencyIsAppliedBeforeFinancialContentAppears() {
        runBlocking { database.financeSettingsDao().save(FinanceSettingsEntity(currencyCode = "GBP", confirmed = true)) }
        Money.currencyCode = "USD"
        showGate()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("financial_content").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("GBP", Money.currencyCode)
        compose.onNodeWithTag("currency_search").assertDoesNotExist()
    }
}
