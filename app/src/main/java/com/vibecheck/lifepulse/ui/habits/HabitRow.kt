package com.vibecheck.lifepulse.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoIconButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitRow(habit: Habit, accentColor: Color = NeoColors.Primary, onToggle: () -> Unit, onDelete: () -> Unit) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeoColors.Surface,
        shape = RoundedCornerShape(12.dp),
        borderWidth = 2.dp,
        shadowOffsetX = 4.dp,
        shadowOffsetY = 4.dp,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chunky color-coded accent stripe matching the section's frequency color.
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(accentColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
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
                        text = reminderSummary(habit),
                        style = NeoTypography.bodyMedium,
                        color = NeoColors.OnSurface.copy(alpha = 0.7f)
                    )
                }
                if (habit.currentStreak > 0) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .background(color = NeoColors.Accent, shape = RoundedCornerShape(50))
                            .border(width = 2.dp, color = NeoColors.Border, shape = RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔥${habit.currentStreak}",
                            style = NeoTypography.labelLarge,
                            color = NeoColors.OnSurface
                        )
                    }
                }
                NeoIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.habit_delete_content_description),
                    onClick = onDelete,
                    backgroundColor = NeoColors.Danger,
                    iconTint = NeoColors.White,
                    size = 30.dp,
                    borderWidth = 1.5.dp,
                    shadowOffset = 2.dp
                )
            }
        }
    }
}

@Composable
internal fun frequencyLabel(frequency: HabitFrequency): String = when (frequency) {
    HabitFrequency.DAILY -> stringResource(R.string.frequency_daily)
    HabitFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
    HabitFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
}

internal fun dayOfMonthOrdinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

@Composable
internal fun reminderSummary(habit: Habit): String {
    if (!habit.hasReminder) return frequencyLabel(habit.frequency)

    val time = formatTime12Hour(habit.reminderHour!!, habit.reminderMinute!!)
    return when (habit.frequency) {
        HabitFrequency.DAILY -> "⏰ $time daily"
        HabitFrequency.WEEKLY -> {
            val dayName = habit.reminderDayOfWeek
                ?.let { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
                ?: frequencyLabel(HabitFrequency.WEEKLY)
            "⏰ $dayName @ $time"
        }
        HabitFrequency.MONTHLY -> {
            val day = habit.reminderDayOfMonth ?: 1
            "⏰ ${dayOfMonthOrdinal(day)} @ $time"
        }
    }
}

internal fun formatTime12Hour(hour24: Int, minute: Int): String {
    val isPm = hour24 >= 12
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return "%d:%02d %s".format(hour12, minute, if (isPm) "PM" else "AM")
}

