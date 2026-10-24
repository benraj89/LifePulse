package com.vibecheck.lifepulse.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.domain.model.Account
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.components.ColorDot
import com.vibecheck.lifepulse.ui.neobrutalism.neoHardShadow
import com.vibecheck.lifepulse.ui.neobrutalism.NeoSticker
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBurst
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCheck
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun DashboardHeader(date: LocalDate, entryEnabled: Boolean, onAddEntry: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = DashboardDesign.SmallGap)) {
        val stackAction = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
        if (stackAction) {
            Column(verticalArrangement = Arrangement.spacedBy(DashboardDesign.Gap)) {
                DashboardIdentity(date)
                DashboardPrimaryAction(onAddEntry, entryEnabled, Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DashboardDesign.Gap)) {
                DashboardIdentity(date, Modifier.weight(1f))
                DashboardPrimaryAction(onAddEntry, entryEnabled)
            }
        }
    }
}

@Composable
private fun DashboardIdentity(date: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.dashboard_title), style = DashboardDesign.Heading,
            color = DashboardDesign.Ink, modifier = Modifier.semantics { heading() })
        Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
            style = DashboardDesign.Meta, color = DashboardDesign.SecondaryInk)
    }
}

@Composable
private fun DashboardPrimaryAction(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val offset by animateDpAsState(if (pressed && enabled) 2.dp else 0.dp, tween(90), label = "dashboardActionPress")
    Button(
        onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.padding(end = 4.dp, bottom = 4.dp)
            .neoHardShadow(shape = DashboardDesign.ControlShape, offsetX = 4.dp - offset, offsetY = 4.dp - offset)
            .offset(x = offset, y = offset).heightIn(min = 48.dp),
        shape = DashboardDesign.ControlShape, border = BorderStroke(2.dp, DashboardDesign.Ink),
        elevation = null, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DashboardDesign.Action, contentColor = DashboardDesign.Ink,
            disabledContainerColor = DashboardDesign.Disabled,
            disabledContentColor = DashboardDesign.SecondaryInk
        )
    ) {
        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.transaction_add), style = DashboardDesign.Label)
    }
}

/** Only featured panels cast a hard shadow; list rows stay flat. */
@Composable
internal fun DashboardPanel(
    modifier: Modifier = Modifier,
    color: Color = DashboardDesign.Paper,
    content: @Composable () -> Unit
) {
    Surface(
        modifier.padding(end = 4.dp, bottom = 4.dp)
            .neoHardShadow(shape = DashboardDesign.Shape, offsetX = 4.dp, offsetY = 4.dp),
        shape = DashboardDesign.Shape, color = color, contentColor = DashboardDesign.Ink,
        border = BorderStroke(2.dp, DashboardDesign.Ink), content = content
    )
}

@Composable
internal fun DashboardHabitOverview(state: DashboardUiState, onHabits: () -> Unit) {
    DashboardPanel(Modifier.fillMaxWidth(), DashboardDesign.Habit) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(DashboardDesign.Gap)) {
            if (state.totalHabits == 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dashboard_ready), style = DashboardDesign.Title, modifier = Modifier.weight(1f))
                    NeoBurst()
                }
                Text(stringResource(R.string.dashboard_start_habit_hint), style = DashboardDesign.Body)
                DashboardTextAction(stringResource(R.string.dashboard_start_habit), onHabits)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeoSticker(stringResource(R.string.dashboard_today_label))
                    Text(stringResource(R.string.dashboard_habit_progress_done, state.completedHabits, state.totalHabits),
                        style = DashboardDesign.Metric, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
                    NeoBurst(color = NeoColors.BubblegumPink)
                }
                val progress by animateFloatAsState(state.habitProgressFraction.coerceIn(0f, 1f), tween(180), label = "dashboardHabitProgress")
                LinearProgressIndicator(
                    progress = { progress }, color = DashboardDesign.Ink, trackColor = DashboardDesign.Paper,
                    strokeCap = StrokeCap.Butt, gapSize = 0.dp, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(8.dp).border(1.dp, DashboardDesign.Ink)
                )
                Text(stringResource(if (state.completedHabits == state.totalHabits) R.string.dashboard_all_done
                    else R.string.dashboard_habits_remaining, state.totalHabits - state.completedHabits),
                    style = DashboardDesign.Meta)
            }
        }
    }
}

@Composable
internal fun DashboardSectionTitle(@StringRes title: Int, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = DashboardDesign.SmallGap), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(title), style = DashboardDesign.Title, color = DashboardDesign.Ink,
            modifier = Modifier.weight(1f).semantics { heading() })
        TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = DashboardDesign.Ink)) {
            Text(stringResource(R.string.action_see_all), style = DashboardDesign.Label)
        }
    }
}

@Composable
internal fun DashboardHabitRow(habit: Habit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val fill by animateColorAsState(if (habit.completedToday) DashboardDesign.Completed else DashboardDesign.Paper,
        tween(120), label = "dashboardHabitCompletion")
    val status = stringResource(if (habit.completedToday) R.string.action_done else R.string.habit_to_do)
    val period = stringResource(when (habit.frequency) {
        HabitFrequency.DAILY -> R.string.expense_today_label
        HabitFrequency.WEEKLY -> R.string.habit_this_week
        HabitFrequency.MONTHLY -> R.string.expense_this_month_label
    })
    val periodStatus = stringResource(R.string.habit_period_status, period, status)
    val streak = if (habit.currentStreak > 0) stringResource(when (habit.frequency) {
        HabitFrequency.DAILY -> R.string.habit_day_streak
        HabitFrequency.WEEKLY -> R.string.habit_week_streak
        HabitFrequency.MONTHLY -> R.string.habit_month_streak
    }, habit.currentStreak) else null
    Box(modifier.fillMaxWidth().testTag("dashboard_habit_${habit.id}")) {
        Row(
            Modifier.fillMaxWidth().clip(DashboardDesign.Shape).background(fill)
                .border(2.dp, DashboardDesign.Ink, DashboardDesign.Shape)
                .toggleable(value = habit.completedToday, role = Role.Checkbox, onValueChange = { onToggle() })
                .heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeoCheck(habit.completedToday)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(habit.title, style = DashboardDesign.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = DashboardDesign.Ink)
                Text(if (streak != null) stringResource(R.string.habit_status_streak, periodStatus, streak) else periodStatus,
                    style = DashboardDesign.Meta, color = DashboardDesign.SecondaryInk)
            }
        }
    }
}

@Composable
internal fun DashboardMoneyOverview(state: DashboardUiState, onMoney: () -> Unit, modifier: Modifier = Modifier) {
    DashboardPanel(modifier.fillMaxWidth().testTag("dashboard_money_overview"), DashboardDesign.Expense) {
        Column {
            Surface(onClick = onMoney, color = DashboardDesign.Expense, contentColor = DashboardDesign.Ink) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).semantics { heading() }) {
                        NeoSticker(stringResource(R.string.dashboard_money_heading,
                            state.date.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))), color = DashboardDesign.Paper)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
                }
            }
            Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DashboardValueRow(stringResource(R.string.dashboard_month_spending), Money.format(state.spentThisMonth),
                    valueStyle = DashboardDesign.Metric)
                HorizontalDivider(color = DashboardDesign.Ink, thickness = 1.dp)
                DashboardValueRow(stringResource(R.string.dashboard_spent_today), Money.format(state.spentToday))
                DashboardValueRow(stringResource(R.string.dashboard_account_balance), Money.format(state.accountBalance))
                DashboardValueRow(stringResource(R.string.dashboard_month_income), Money.format(state.incomeThisMonth))
                if (state.owedToYou > 0 || state.owedByYou > 0) {
                    HorizontalDivider(color = DashboardDesign.Rule)
                    DashboardValueRow(stringResource(R.string.dashboard_people_owe_you), Money.format(state.owedToYou))
                    DashboardValueRow(stringResource(R.string.dashboard_you_owe_people), Money.format(state.owedByYou))
                }
            }
        }
    }
}

/** Measure the real formatted currency: long amounts stack without dropping precision. */
@Composable
internal fun DashboardValueRow(
    label: String, value: String, modifier: Modifier = Modifier,
    labelStyle: TextStyle = DashboardDesign.Meta, valueStyle: TextStyle = DashboardDesign.Amount
) {
    val measurer = rememberTextMeasurer()
    val gapPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val labelWidth = measurer.measure(AnnotatedString(label), labelStyle, softWrap = false).size.width
        val valueWidth = measurer.measure(AnnotatedString(value), valueStyle, softWrap = false).size.width
        if (labelWidth + valueWidth + gapPx <= constraints.maxWidth) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = labelStyle, color = DashboardDesign.SecondaryInk, modifier = Modifier.weight(1f))
                Text(value, style = valueStyle, color = DashboardDesign.Ink)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = labelStyle, color = DashboardDesign.SecondaryInk)
                Text(value, style = valueStyle, color = DashboardDesign.Ink, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun DashboardExpenseRow(expense: Expense, accounts: List<Account>, onClick: () -> Unit) {
    Surface(onClick = onClick, color = DashboardDesign.Paper, contentColor = DashboardDesign.Ink,
        shape = DashboardDesign.Shape, border = BorderStroke(2.dp, DashboardDesign.Ink),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorDot(expense.categoryColorHex, Modifier.padding(top = 6.dp), size = 12)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DashboardValueRow(expense.categoryName, Money.format(expense.amountMinor), labelStyle = DashboardDesign.Label)
                Text(listOfNotNull(accounts.firstOrNull { it.id == expense.accountId }?.name,
                    DateUtils.formatTimestamp(expense.dateTimestamp, "dd MMM")).joinToString(" · "),
                    style = DashboardDesign.Meta, color = DashboardDesign.SecondaryInk)
                if (expense.note.isNotBlank()) Text(expense.note, style = DashboardDesign.Meta, color = DashboardDesign.SecondaryInk)
            }
        }
    }
}

@Composable
internal fun DashboardSpendingEmpty(entryEnabled: Boolean, onAddEntry: () -> Unit, onMoney: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.dashboard_no_spending), style = DashboardDesign.Body, color = DashboardDesign.Ink)
        Text(stringResource(R.string.dashboard_empty_spending_hint), style = DashboardDesign.Meta, color = DashboardDesign.SecondaryInk)
        DashboardTextAction(stringResource(if (entryEnabled) R.string.dashboard_record_expense else R.string.dashboard_set_up_account),
            if (entryEnabled) onAddEntry else onMoney)
    }
}

@Composable
private fun DashboardTextAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = DashboardDesign.Ink)) {
        Text(label, style = DashboardDesign.Label)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
    }
}

@Composable
internal fun DashboardLoading() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.dashboard_loading), style = DashboardDesign.Body, color = DashboardDesign.SecondaryInk,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        // Static placeholders preserve the composition without perpetual decorative motion.
        Box(Modifier.fillMaxWidth().height(148.dp).background(DashboardDesign.Disabled, DashboardDesign.Shape))
        repeat(3) { Box(Modifier.fillMaxWidth().height(72.dp).background(DashboardDesign.Disabled, DashboardDesign.Shape)) }
    }
}
