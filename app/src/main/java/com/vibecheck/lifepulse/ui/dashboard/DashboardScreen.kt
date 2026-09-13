package com.vibecheck.lifepulse.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.ui.components.AddExpenseSheet
import com.vibecheck.lifepulse.ui.components.ColorDot
import com.vibecheck.lifepulse.ui.components.EmptyState
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import com.vibecheck.lifepulse.ui.neobrutalism.NeoButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    onSeeAllHabits: () -> Unit = {},
    onSeeAllExpenses: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExpenseSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = NeoColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = NeoTypography.headlineMedium,
                        color = NeoColors.OnSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeoColors.Background,
                    titleContentColor = NeoColors.OnSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NeoColors.Background)
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(
                    habitProgressLabel = state.habitProgressLabel,
                    habitProgressFraction = state.habitProgressFraction,
                    spentToday = state.spentToday,
                    spentThisMonth = state.spentThisMonth
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(IntrinsicSize.Max)
                ) {
                    NeoButton(
                        text = stringResource(R.string.dashboard_add_expense),
                        onClick = { showExpenseSheet = true },
                        backgroundColor = NeoColors.Primary,
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = NeoColors.OnPrimary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    NeoButton(
                        text = stringResource(R.string.dashboard_all_habits),
                        onClick = onSeeAllHabits,
                        backgroundColor = NeoColors.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.dashboard_section_todays_habits),
                    actionLabel = stringResource(R.string.action_see_all),
                    onAction = onSeeAllHabits
                )
            }

            if (state.habits.isEmpty()) {
                item {
                    EmptyState(
                        stringResource(R.string.dashboard_empty_habits_title),
                        stringResource(R.string.dashboard_empty_habits_subtitle),
                        imageRes = R.drawable.empty_hobbie
                    )
                }
            } else {
                items(state.habits, key = { "habit_${it.id}" }) { habit ->
                    HabitQuickRow(habit = habit, onToggle = { viewModel.toggleHabit(habit.id) })
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.dashboard_section_recent_expenses),
                    actionLabel = stringResource(R.string.action_see_all),
                    onAction = onSeeAllExpenses
                )
            }

            if (state.recentExpenses.isEmpty()) {
                item {
                    EmptyState(
                        stringResource(R.string.dashboard_empty_expenses_title),
                        stringResource(R.string.dashboard_empty_expenses_subtitle),
                        imageRes = R.drawable.empty_expense
                    )
                }
            } else {
                items(state.recentExpenses, key = { "expense_${it.id}" }) { expense ->
                    ExpenseRow(expense)
                }
            }
        }
    }

    if (showExpenseSheet) {
        AddExpenseSheet(
            categories = state.categories,
            onDismiss = { showExpenseSheet = false },
            onSave = viewModel::addExpense,
            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
            onDeleteCategory = { category -> viewModel.deleteCategory(category.id) }
        )
    }
}

/** Top summary card: habit progress on the left, today's spend on the right. */
@Composable
fun SummaryCard(
    habitProgressLabel: String,
    habitProgressFraction: Float,
    spentToday: Double,
    spentThisMonth: Double,
    modifier: Modifier = Modifier
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = NeoColors.Accent,
        contentPadding = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.today_s_habit_progress),
                    style = NeoTypography.labelLarge,
                    color = NeoColors.OnSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = habitProgressLabel,
                    style = NeoTypography.headlineMedium,
                    color = NeoColors.OnSurface
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { habitProgressFraction },
                    modifier = Modifier.fillMaxWidth(0.85f),
                    color = NeoColors.OnSurface,
                    trackColor = NeoColors.Surface,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    stringResource(R.string.today_s_total_spent),
                    style = NeoTypography.labelLarge,
                    color = NeoColors.OnSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = spentToday.asCurrency(),
                    style = NeoTypography.headlineMedium,
                    color = NeoColors.OnSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.this_month, spentThisMonth.asCurrency()),
                    style = NeoTypography.bodyMedium,
                    color = NeoColors.OnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = NeoTypography.titleLarge, color = NeoColors.OnSurface)
        TextButton(onClick = onAction) {
            Text(
                text = actionLabel,
                style = NeoTypography.labelLarge,
                color = NeoColors.OnSurface
            )
        }
    }
}

@Composable
private fun HabitQuickRow(habit: Habit, onToggle: () -> Unit) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeoColors.Surface,
        shape = RoundedCornerShape(12.dp),
        borderWidth = 2.dp,
        shadowOffsetX = 4.dp,
        shadowOffsetY = 4.dp,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = habit.completedToday,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeoColors.Primary,
                    checkmarkColor = NeoColors.OnSurface,
                    uncheckedColor = NeoColors.Border
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title, style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
                Text(
                    stringResource(R.string.dashboard_streak_format, habit.currentStreak),
                    style = NeoTypography.bodyMedium,
                    color = NeoColors.OnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeoColors.Surface,
        shape = RoundedCornerShape(12.dp),
        borderWidth = 2.dp,
        shadowOffsetX = 4.dp,
        shadowOffsetY = 4.dp,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorDot(expense.categoryColorHex, size = 16)
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.categoryName, style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
                Text(
                    listOfNotNull(
                        expense.note.takeIf { it.isNotBlank() },
                        DateUtils.formatTimestamp(expense.dateTimestamp)
                    ).joinToString(" • "),
                    style = NeoTypography.bodyMedium,
                    color = NeoColors.OnSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                expense.amount.asCurrency(),
                style = NeoTypography.titleMedium,
                fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface
            )
        }
    }
}

fun Double.asCurrency(): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault()).format(this)

@Preview(showBackground = true)
@Composable
private fun SummaryCardPreview() {
    NeoBrutalismTheme {
        SummaryCard(
            habitProgressLabel = "3/5 Completed",
            habitProgressFraction = 0.6f,
            spentToday = 24.50,
            spentThisMonth = 412.30
        )
    }
}



