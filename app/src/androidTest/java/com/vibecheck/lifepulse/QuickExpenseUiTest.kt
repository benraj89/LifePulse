package com.vibecheck.lifepulse

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.components.AddExpenseSheet
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class QuickExpenseUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickExpensePreservesCategorySelectionAndExactMoney() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var categories by mutableStateOf(listOf(Category(1, "Food", "#FF7043"), Category(2, "Transport", "#42A5F5")))
        val submitted = AtomicReference<TransactionDraft?>()
        compose.setContent {
            NeoBrutalismTheme {
                AddExpenseSheet(categories = categories, accounts = listOf(Account(1, "Cash", 0), Account(2, "Bank", 0)),
                    onDismiss = {}, onSave = { account, category, amount, note, timestamp ->
                        submitted.set(TransactionDraft(accountId = account, categoryId = category, amountMinor = amount,
                            note = note, dateTimestamp = timestamp))
                    })
            }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("0.10")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.onNodeWithText("Cash  ▾").performScrollTo().performClick()
        compose.onNodeWithText("Bank").performClick()
        compose.onNodeWithText("Food").performScrollTo().performClick()
        compose.onNodeWithText("Transport").performClick()
        compose.runOnIdle { categories = categories + Category(3, "Travel", "#66BB6A") }
        compose.onNodeWithText("Transport").assertExists()
        compose.onNodeWithText(context.getString(R.string.add_expense_save)).performScrollTo().performClick()
        compose.runOnIdle {
            assertNotNull(submitted.get())
            assertEquals(2L, submitted.get()!!.accountId)
            assertEquals(2L, submitted.get()!!.categoryId)
            assertEquals(10L, submitted.get()!!.amountMinor)
        }
    }
}
