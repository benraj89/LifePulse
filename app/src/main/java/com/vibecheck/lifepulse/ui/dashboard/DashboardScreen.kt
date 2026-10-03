package com.vibecheck.lifepulse.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.components.ColorDot
import com.vibecheck.lifepulse.ui.expenses.TransactionSheet
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.format.DateTimeFormatter

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
    // The navigation scaffold already handles the system bars and bottom navigation.
    Scaffold(modifier = modifier, containerColor = NeoColors.Background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        DashboardContent(state, Modifier.padding(padding), onSeeAllHabits, onSeeAllExpenses,
            onAddEntry = { viewModel.resetExpenseSave(); showEntry = true }, onToggleHabit = { viewModel.toggleHabit(it) })
    }
    if (showEntry) TransactionSheet(accounts = state.accounts, categories = state.categories, state = save,
        onDismiss = { showEntry = false }, onSave = viewModel::saveTransaction,
        onAddCategory = { name, color, kind -> viewModel.addCategory(name, color, kind) },
        onDeleteCategory = { viewModel.deleteCategory(it) })
}

@Composable
internal fun DashboardContent(state: DashboardUiState, modifier: Modifier = Modifier,
                              onHabits: () -> Unit, onMoney: () -> Unit, onAddEntry: () -> Unit,
                              onToggleHabit: (Long) -> Unit) {
    LazyColumn(modifier.fillMaxSize().testTag("dashboard"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("LifePulse", style = NeoTypography.headlineMedium, color = NeoColors.OnSurface)
                    Text(state.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")), style = NeoTypography.bodySmall,
                        color = NeoColors.OnSurface)
                }
                NeoButton("Add entry", onAddEntry, enabled = !state.isLoading && state.accounts.isNotEmpty(),
                    backgroundColor = NeoColors.Coral, contentColor = NeoColors.OnSurface, shape = RoundedCornerShape(4.dp),
                    shadowOffset = 3.dp, horizontalPadding = 12.dp, verticalPadding = 8.dp,
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) })
            }
        }
        if (state.isLoading) {
            item { Text("Loading your overview…", style = NeoTypography.bodyMedium) }
        } else {
            item { TodayCard(state, onHabits, onMoney) }
            item { MoneyOverview(state, onMoney) }
            item { SectionHeader("Your habits", onHabits) }
            if (state.habits.isEmpty()) {
                item { DashboardEmpty("Start a habit", "Build a routine with a daily, weekly, or monthly habit.", onHabits) }
            } else {
                val habits = state.habits.sortedBy { it.completedToday }.take(4)
                items(habits, key = { "habit_${it.id}" }) { habit -> HabitQuickRow(habit, { onToggleHabit(habit.id) }) }
            }
            item { SectionHeader("Recent spending", onMoney) }
            if (state.recentExpenses.isEmpty()) {
                item { DashboardEmpty("No spending yet", "Tap Add entry to record your first expense.", onAddEntry) }
            } else {
                items(state.recentExpenses, key = { "expense_${it.id}" }) { ExpenseRow(it, state.accounts, onMoney) }
            }
        }
    }
}

@Composable
private fun TodayCard(state: DashboardUiState, onHabits: () -> Unit, onMoney: () -> Unit) {
    DashboardCard("TODAY", NeoColors.Yellow) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onHabits).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Habit progress", style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
                Text(state.habitProgressLabel, style = NeoTypography.titleLarge, fontWeight = FontWeight.Black)
                Box(Modifier.fillMaxWidth().height(8.dp).background(NeoColors.Surface).border(2.dp, NeoColors.Border)) {
                    if (state.habitProgressFraction > 0) Box(Modifier.fillMaxWidth(state.habitProgressFraction.coerceIn(0f, 1f))
                        .fillMaxHeight().background(NeoColors.Ink))
                }
                Text(when {
                    state.totalHabits == 0 -> "Ready when you are"
                    state.completedHabits == state.totalHabits -> "All done for now!"
                    else -> "${state.totalHabits - state.completedHabits} still to do"
                }, style = NeoTypography.bodySmall)
            }
            Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
            OverviewStat("Spent today", state.spentToday,
                Modifier.weight(1f).fillMaxHeight().background(NeoColors.Surface).clickable(onClick = onMoney))
        }
    }
}

@Composable
private fun MoneyOverview(state: DashboardUiState, onMoney: () -> Unit) {
    DashboardCard("YOUR MONEY · ${state.date.format(DateTimeFormatter.ofPattern("MMM")).uppercase()}", NeoColors.Surface, onMoney) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            OverviewStat("Account balance", state.accountBalance, Modifier.weight(1f).fillMaxHeight().background(NeoColors.MintGreen))
            Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
            OverviewStat("Month's spending", state.spentThisMonth, Modifier.weight(1f).fillMaxHeight().background(NeoColors.Yellow))
        }
        HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
        OverviewStat("Month's income", state.incomeThisMonth, Modifier.fillMaxWidth(), inline = true)
        if (state.owedToYou > 0 || state.owedByYou > 0) {
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                OverviewStat("People owe you", state.owedToYou, Modifier.weight(1f).fillMaxHeight().background(NeoColors.Cyan))
                Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
                OverviewStat("You owe people", state.owedByYou, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun DashboardCard(title: String, color: Color, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    NeoCard(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        backgroundColor = color, shape = RoundedCornerShape(4.dp), contentPadding = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().background(NeoColors.Ink).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, color = NeoColors.White,
                    modifier = Modifier.weight(1f))
                Text(if (onClick != null) "→" else "///", color = NeoColors.Lime, style = NeoTypography.labelLarge)
            }
            content()
        }
    }
}

@Composable
private fun OverviewStat(label: String, amount: Long, modifier: Modifier, inline: Boolean = false) {
    if (inline) Row(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = NeoTypography.labelSmall, modifier = Modifier.weight(1f))
        Text(Money.format(amount), style = NeoTypography.titleMedium, fontWeight = FontWeight.Black)
    } else Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
        Text(Money.format(amount), style = NeoTypography.titleLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SectionHeader(title: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = NeoTypography.titleMedium, modifier = Modifier.weight(1f), color = NeoColors.OnSurface)
        TextButton(onClick = onAction) { Text("See all", style = NeoTypography.labelLarge, color = NeoColors.OnSurface) }
    }
}

@Composable
private fun DashboardEmpty(title: String, hint: String, onClick: () -> Unit) {
    NeoColumnCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(4.dp), contentPadding = 12.dp) {
        Text(title, style = NeoTypography.titleMedium)
        Text(hint, style = NeoTypography.bodySmall)
    }
}

@Composable
private fun HabitQuickRow(habit: Habit, onToggle: () -> Unit) {
    NeoCard(Modifier.fillMaxWidth().testTag("dashboard_habit_${habit.id}"),
        backgroundColor = if (habit.completedToday) NeoColors.MintGreen else NeoColors.Surface,
        shape = RoundedCornerShape(4.dp), contentPadding = 6.dp, shadowOffsetX = 3.dp, shadowOffsetY = 3.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(habit.completedToday, { onToggle() }, colors = CheckboxDefaults.colors(
                checkedColor = NeoColors.Ink, checkmarkColor = NeoColors.Lime, uncheckedColor = NeoColors.Border))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(habit.title, style = NeoTypography.titleMedium)
                val period = when (habit.frequency) {
                    HabitFrequency.DAILY -> "Today"
                    HabitFrequency.WEEKLY -> "This week"
                    HabitFrequency.MONTHLY -> "This month"
                }
                val unit = when (habit.frequency) {
                    HabitFrequency.DAILY -> "day"
                    HabitFrequency.WEEKLY -> "week"
                    HabitFrequency.MONTHLY -> "month"
                }
                Text("$period · ${if (habit.completedToday) "Done" else "To do"}" +
                    (if (habit.currentStreak > 0) " · ${habit.currentStreak} $unit streak" else ""),
                    style = NeoTypography.bodySmall)
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, accounts: List<Account>, onClick: () -> Unit) {
    NeoCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(4.dp),
        contentPadding = 10.dp, shadowOffsetX = 3.dp, shadowOffsetY = 3.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ColorDot(expense.categoryColorHex, size = 12)
            Column(Modifier.weight(1f)) {
                Text(expense.categoryName, style = NeoTypography.titleMedium)
                Text(listOfNotNull(accounts.firstOrNull { it.id == expense.accountId }?.name,
                    DateUtils.formatTimestamp(expense.dateTimestamp, "dd MMM")).joinToString(" · "), style = NeoTypography.bodySmall)
                if (expense.note.isNotBlank()) Text(expense.note, style = NeoTypography.bodySmall, maxLines = 2)
            }
            Text(Money.format(expense.amountMinor), style = NeoTypography.titleMedium, fontWeight = FontWeight.Black,
                modifier = Modifier.widthIn(max = 140.dp))
        }
    }
}
