package com.vibecheck.lifepulse.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.ui.expenses.TransactionSheet

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    onSeeAllHabits: () -> Unit = {},
    onSeeAllExpenses: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val save by viewModel.expenseSave.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showEntry by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    // The navigation scaffold owns system bars and bottom-navigation insets.
    Scaffold(
        modifier = modifier,
        containerColor = DashboardDesign.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        DashboardContent(
            state, Modifier.padding(padding), onSeeAllHabits, onSeeAllExpenses,
            onAddEntry = { viewModel.resetExpenseSave(); showEntry = true },
            onToggleHabit = { viewModel.toggleHabit(it) }
        )
    }
    if (showEntry) TransactionSheet(
        accounts = state.accounts, categories = state.categories, state = save,
        onDismiss = { showEntry = false }, onSave = viewModel::saveTransaction,
        onAddCategory = { name, color, kind -> viewModel.addCategory(name, color, kind) },
        onDeleteCategory = { viewModel.deleteCategory(it) }
    )
}

@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onHabits: () -> Unit,
    onMoney: () -> Unit,
    onAddEntry: () -> Unit,
    onToggleHabit: (Long) -> Unit
) {
    val habits = remember(state.habits) { state.habits.sortedBy { it.completedToday }.take(4) }
    val entryEnabled = !state.isLoading && state.accounts.isNotEmpty()
    BoxWithConstraints(modifier.fillMaxSize().background(DashboardDesign.Canvas), contentAlignment = Alignment.TopCenter) {
        // Larger text returns to one column instead of squeezing two readable panes.
        val twoColumns = maxWidth >= DashboardDesign.TwoColumnWidth && LocalDensity.current.fontScale < 1.5f
        LazyColumn(
            Modifier.widthIn(max = DashboardDesign.MaxContentWidth).fillMaxSize().testTag("dashboard"),
            contentPadding = PaddingValues(DashboardDesign.Gutter),
            verticalArrangement = Arrangement.spacedBy(DashboardDesign.Gap)
        ) {
            item(key = "header") { DashboardHeader(state.date, entryEnabled, onAddEntry) }
            if (state.isLoading) {
                item(key = "loading") { DashboardLoading() }
            } else if (twoColumns) {
                item(key = "overview") {
                    Row(horizontalArrangement = Arrangement.spacedBy(DashboardDesign.SectionGap)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DashboardDesign.Gap)) {
                            DashboardHabitOverview(state, onHabits)
                            if (habits.isNotEmpty()) {
                                DashboardSectionTitle(R.string.dashboard_your_habits, onHabits)
                                habits.forEach { habit ->
                                    key(habit.id) { DashboardHabitRow(habit, { onToggleHabit(habit.id) }) }
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DashboardDesign.Gap)) {
                            DashboardMoneyOverview(state, onMoney)
                            DashboardSectionTitle(R.string.dashboard_recent_spending, onMoney)
                            if (state.recentExpenses.isEmpty()) DashboardSpendingEmpty(entryEnabled, onAddEntry, onMoney)
                            state.recentExpenses.forEach { expense ->
                                key(expense.id) { DashboardExpenseRow(expense, state.accounts, onMoney) }
                            }
                        }
                    }
                }
            } else {
                item(key = "today") { DashboardHabitOverview(state, onHabits) }
                if (habits.isNotEmpty()) {
                    item(key = "habits_heading") { DashboardSectionTitle(R.string.dashboard_your_habits, onHabits) }
                    items(habits, key = { "habit_${it.id}" }) { habit ->
                        DashboardHabitRow(habit, { onToggleHabit(habit.id) })
                    }
                }
                item(key = "money") {
                    DashboardMoneyOverview(state, onMoney, Modifier.padding(top = DashboardDesign.SmallGap))
                }
                item(key = "spending_heading") { DashboardSectionTitle(R.string.dashboard_recent_spending, onMoney) }
                if (state.recentExpenses.isEmpty()) {
                    item(key = "spending_empty") { DashboardSpendingEmpty(entryEnabled, onAddEntry, onMoney) }
                } else {
                    items(state.recentExpenses, key = { "expense_${it.id}" }) { expense ->
                        DashboardExpenseRow(expense, state.accounts, onMoney)
                    }
                }
            }
        }
    }
}
