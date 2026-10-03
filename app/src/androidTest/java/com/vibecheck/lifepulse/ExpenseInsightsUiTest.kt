package com.vibecheck.lifepulse

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.expenses.*
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ExpenseInsightsUiTest {
    @get:Rule val compose = createComposeRule()
    private val month = YearMonth.now()
    private val names = listOf("Food & coffee", "Shopping", "Transport", "Bills", "Entertainment", "Health", "Travel")
    private fun transactions(): List<Expense> = (0..5).flatMap { offset ->
        names.mapIndexed { index, name ->
            Expense((offset * 10 + index + 1).toLong(), (index + 1).toLong(), name, "#FFD400",
                ((7 - index) * (offset + 2) * 1250).toLong(),
                month.minusMonths(offset.toLong()).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                "", accountId = if (index == 6) 2 else 1)
        }
    }

    @Test fun categoryHighlightAndMonthBarNavigationWork() {
        var state by mutableStateOf(ExpenseUiState(transactions = transactions(), selectedMonth = month, isLoading = false))
        compose.setContent { NeoBrutalismTheme { Box(Modifier.systemBarsPadding()) {
            ExpenseInsightsScreen(state, { state = state.copy(selectedMonth = it) }, {}, {})
        } } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("01 / THE BIG PICTURE").fetchSemanticsNodes().isNotEmpty() }
        screenshot("insights-overview.png")
        compose.onNodeWithTag("insight_slice_category_1").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("spending_insights").performScrollToIndex(2)
        screenshot("insights-categories.png")
        val previous = month.minusMonths(1)
        compose.onNodeWithTag("spending_insights").performScrollToIndex(3)
        compose.onNodeWithTag("insight_month_$previous").assertIsDisplayed()
        screenshot("insights-trend.png")
        compose.onNodeWithTag("insight_month_$previous").performClick()
        compose.waitUntil(5000) { state.selectedMonth == previous }
    }

    @Test fun accountFilterAndEmptyStateStayUsable() {
        var state by mutableStateOf(ExpenseUiState(transactions = transactions(), selectedMonth = month,
            accounts = listOf(Account(1, "Cash", 0), Account(2, "Bank", 0), Account(3, "Savings", 0)), isLoading = false))
        compose.setContent { NeoBrutalismTheme { Box(Modifier.systemBarsPadding()) {
            ExpenseInsightsScreen(state, {}, { state = state.copy(selectedAccountId = it) }, {})
        } } }
        compose.onNodeWithTag("insights_account").performClick()
        compose.onNode(hasText("Savings") and hasAnyAncestor(isPopup())).performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("No spending in either period").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No expenses for this month").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to spending").assertHasClickAction()
        compose.runOnIdle { assertEquals(3L, state.selectedAccountId) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Wait for Android's rendered frame as well as Compose's semantics before capturing.
        instrumentation.uiAutomation.waitForIdle(300, 3000)
        val target = File(instrumentation.targetContext.getExternalFilesDir(null), name)
        target.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
