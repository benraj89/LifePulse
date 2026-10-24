package com.vibecheck.lifepulse.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitRow(habit: Habit, accentColor: Color = NeoColors.Habit, onToggle: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val fill by animateColorAsState(if (habit.completedToday) NeoColors.Success else when (habit.frequency) {
        HabitFrequency.DAILY -> NeoColors.Surface
        HabitFrequency.WEEKLY -> NeoColors.Lavender
        HabitFrequency.MONTHLY -> NeoColors.BlushPink
    },
        tween(120), label = "habitCompletion")
    Surface(color = fill, shape = NeoTheme.shapes.medium, border = BorderStroke(2.dp, NeoColors.Ink),
        modifier = Modifier.fillMaxWidth().testTag("habit_" + habit.id)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).toggleable(habit.completedToday, role = Role.Checkbox, onValueChange = { onToggle() })
                .heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoCheck(habit.completedToday)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(habit.title, style = NeoTypography.titleMedium)
                    Text(reminderSummary(habit), style = NeoTypography.bodySmall, color = NeoColors.SecondaryText)
                    if (habit.currentStreak > 0) Text(stringResource(when (habit.frequency) {
                        HabitFrequency.DAILY -> R.string.habit_day_streak
                        HabitFrequency.WEEKLY -> R.string.habit_week_streak
                        HabitFrequency.MONTHLY -> R.string.habit_month_streak
                    }, habit.currentStreak), style = NeoTypography.labelMedium, color = NeoColors.SecondaryText)
                }
            }
            Box(Modifier.padding(end = 4.dp)) {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.habit_actions, habit.title))
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = NeoColors.Danger) },
                        onClick = { menu = false; confirmDelete = true })
                }
            }
        }
    }
    if (confirmDelete) NeoConfirmDialog(stringResource(R.string.habit_delete_title),
        stringResource(R.string.habit_delete_message, habit.title), stringResource(R.string.action_delete),
        { confirmDelete = false }, { confirmDelete = false; onDelete() })
}
@Composable
internal fun frequencyLabel(frequency: HabitFrequency): String = when (frequency) {
    HabitFrequency.DAILY -> stringResource(R.string.frequency_daily)
    HabitFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
    HabitFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
}

@Composable
internal fun dayOfMonthOrdinal(day: Int): String {
    val suffix = if (day in 11..13) R.string.ordinal_other else when (day % 10) {
        1 -> R.string.ordinal_first
        2 -> R.string.ordinal_second
        3 -> R.string.ordinal_third
        else -> R.string.ordinal_other
    }
    return stringResource(suffix, day)
}

@Composable
internal fun reminderSummary(habit: Habit): String {
    if (!habit.hasReminder) return frequencyLabel(habit.frequency)

    val time = formatTime12Hour(habit.reminderHour!!, habit.reminderMinute!!)
    return when (habit.frequency) {
        HabitFrequency.DAILY -> stringResource(R.string.habit_reminder_daily_format, time)
        HabitFrequency.WEEKLY -> {
            val dayName = habit.reminderDayOfWeek
                ?.let { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
                ?: frequencyLabel(HabitFrequency.WEEKLY)
            stringResource(R.string.habit_reminder_day_time_format, dayName, time)
        }
        HabitFrequency.MONTHLY -> {
            val day = habit.reminderDayOfMonth ?: 1
            stringResource(R.string.habit_reminder_day_time_format, dayOfMonthOrdinal(day), time)
        }
    }
}

@Composable
internal fun formatTime12Hour(hour24: Int, minute: Int): String {
    val isPm = hour24 >= 12
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return stringResource(R.string.time_12_hour_format, hour12, minute, stringResource(if (isPm) R.string.time_pm else R.string.time_am))
}

