package com.vibecheck.lifepulse.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import java.time.LocalDate

/** Illustrative data is confined to previews; totals use the existing dashboard derivation. */
internal fun dashboardPreviewState(): DashboardUiState {
    val date = LocalDate.of(2026, 10, 21)
    val noon = DateUtils.startOfDay(date) + 12 * 60 * 60 * 1000
    return dashboardState(
        date,
        listOf(
            Habit(1, "Read a few pages", HabitFrequency.DAILY, noon, currentStreak = 5),
            Habit(2, "Make time for a walk", HabitFrequency.DAILY, noon),
            Habit(3, "Plan the week", HabitFrequency.WEEKLY, noon, completedToday = true, currentStreak = 3)
        ),
        listOf(
            Expense(1, 1, "Coffee & breakfast", "#FF7A5C", 1460, noon, "Before the morning walk"),
            Expense(2, 2, "Groceries", "#87CEEB", 6200, noon - 86_400_000, ""),
            Expense(3, 3, "Books", "#FFD400", 1299, noon - 2 * 86_400_000, ""),
            Expense(4, null, "Salary", "#D8EFCB", 250000, noon, "", TransactionType.INCOME)
        ),
        emptyList(), listOf(Account(1, "Everyday account", 50000))
    )
}

@Composable
private fun DashboardPreview(state: DashboardUiState) {
    NeoBrutalismTheme {
        Surface(color = DashboardDesign.Canvas) {
            DashboardContent(state, onHabits = {}, onMoney = {}, onAddEntry = {}, onToggleHabit = {})
        }
    }
}

@Preview(name = "Home • populated phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun DashboardPhonePreview() = DashboardPreview(dashboardPreviewState())

@Preview(name = "Home • first use", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun DashboardEmptyPreview() = DashboardPreview(
    DashboardUiState(date = LocalDate.of(2026, 10, 21), accounts = listOf(Account(1, "Cash", 0)), isLoading = false)
)

@Preview(name = "Home • loading", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun DashboardLoadingPreview() = DashboardPreview(DashboardUiState())

@Preview(name = "Home • tablet", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun DashboardTabletPreview() = DashboardPreview(dashboardPreviewState())

@Preview(name = "Home • large text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun DashboardLargeTextPreview() = DashboardPreview(dashboardPreviewState())

@Preview(name = "Home • long content", widthDp = 320, heightDp = 900, fontScale = 1.3f, showBackground = true)
@Composable
private fun DashboardLongContentPreview() = DashboardPreview(dashboardPreviewState().let { state ->
    state.copy(
        habits = state.habits.map { it.copy(title = "Make time for a long walk around the neighbourhood") },
        spentThisMonth = 12345678901234,
        accountBalance = -9876543210123,
        recentExpenses = state.recentExpenses.map { it.copy(amountMinor = 12345678901234, categoryName = "Household essentials and groceries") }
    )
})

@Preview(name = "Components • habit states", widthDp = 360, showBackground = true)
@Composable
private fun DashboardHabitStatesPreview() {
    NeoBrutalismTheme {
        Surface(color = DashboardDesign.Canvas) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardHabitRow(Habit(1, "Read a few pages", HabitFrequency.DAILY, 0), {})
                DashboardHabitRow(Habit(2, "Plan the week", HabitFrequency.WEEKLY, 0, completedToday = true, currentStreak = 3), {})
            }
        }
    }
}
