package com.vibecheck.lifepulse

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.expenses.*
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CurrencyUiTest {
    @get:Rule val compose = createComposeRule()
    private var originalCurrency = "USD"
    @Before fun setup() { originalCurrency = Money.currencyCode }
    @After fun cleanup() { Money.currencyCode = originalCurrency }

    @Test fun missingSuggestionRequiresSearchAndExplicitSelection() {
        var saved: String? = null
        compose.setContent { NeoBrutalismTheme {
            CurrencyPicker(null, { saved = it }, Modifier.fillMaxSize().systemBarsPadding())
        } }
        compose.onNodeWithText("Save currency").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("JPY")
        compose.onNodeWithTag("currency_JPY").performClick().assertIsSelected()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Save currency").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals("JPY", saved) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.waitForIdle(300, 3000)
        File(instrumentation.targetContext.getExternalFilesDir(null), "currency-picker.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun currencyWithFinancialRecordsCannotBeChangedInPicker() {
        compose.setContent { NeoBrutalismTheme {
            CurrencyPicker("USD", { fail("Locked currency should not be saved") },
                Modifier.fillMaxSize().systemBarsPadding(), locked = true)
        } }
        compose.onNode(hasSetTextAction()).performTextInput("JPY")
        compose.onNodeWithTag("currency_JPY").assertIsNotEnabled()
        compose.onNodeWithText("Save currency").assertDoesNotExist()
    }

    @Test fun threeDecimalTransactionSavesExactMinorUnitsAndCurrency() {
        Money.currencyCode = "KWD"
        var draft: TransactionDraft? = null
        compose.setContent { NeoBrutalismTheme {
            TransactionSheet(listOf(Account(1, "Cash", 0)), listOf(Category(1, "Food", "#FF7043")), SaveState(),
                onDismiss = {}, onSave = { draft = it }, onAddCategory = { _, _, _ -> }, onDeleteCategory = {})
        } }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("12.345")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.onNodeWithText("Save expense").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(12345L, draft?.amountMinor)
            assertEquals("KWD", draft?.currencyCode)
        }
    }
}
