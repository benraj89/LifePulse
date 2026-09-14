package com.vibecheck.lifepulse.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.neobrutalism.NeoButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoStackedDialogCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoSwitch
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTextField
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTimePickerDialog
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import com.vibecheck.lifepulse.ui.neobrutalism.neoHardShadow
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        frequency: HabitFrequency,
        reminderHour: Int?,
        reminderMinute: Int?,
        reminderDayOfWeek: Int?,
        reminderDayOfMonth: Int?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(HabitFrequency.DAILY) }

    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(9) }
    var reminderMinute by remember { mutableStateOf(0) }
    var reminderDayOfWeek by remember { mutableStateOf(DayOfWeek.MONDAY.value) }
    var reminderDayOfMonth by remember { mutableStateOf(1) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        NeoStackedDialogCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
            backLayerColor = NeoColors.Cyan,
            backRotation = 0f,
            tapeText = "NEW HABIT",
            tapeColor = NeoColors.Orange,
            contentPadding = 20.dp,
            verticalSpacing = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.habit_dialog_title).uppercase(),
                    style = NeoTypography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(NeoColors.Orange, RoundedCornerShape(2.dp))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoLabel("Title")
                NeoTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.habit_dialog_field_title),
                    focusedShadowColor = NeoColors.Cyan,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoLabel(stringResource(R.string.habit_dialog_frequency_label))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HabitFrequency.entries.forEach { option ->
                        FrequencyOptionCard(
                            label = frequencyLabel(option),
                            code = frequencyIcon(option),
                            color = frequencyColor(option),
                            selected = frequency == option,
                            onClick = { frequency = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neoHardShadow(
                        shape = RoundedCornerShape(10.dp),
                        shadowColor = NeoColors.Shadow,
                        offsetX = 5.dp,
                        offsetY = 5.dp
                    )
                    .background(color = NeoColors.Concrete, shape = RoundedCornerShape(10.dp))
                    .border(width = 3.dp, color = NeoColors.Border, shape = RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .background(NeoColors.Yellow, RoundedCornerShape(6.dp))
                                .border(2.dp, NeoColors.Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.habit_reminder_enable).uppercase(),
                                style = NeoTypography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = NeoColors.OnSurface
                            )
                        }
                        NeoSwitch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    if (reminderEnabled) {
                        when (frequency) {
                            HabitFrequency.WEEKLY -> {
                                NeoLabel(stringResource(R.string.habit_reminder_day_of_week_label))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    DayOfWeek.entries.forEach { day ->
                                        val selected = reminderDayOfWeek == day.value
                                        val dayShape = RoundedCornerShape(8.dp)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .neoHardShadow(
                                                    shape = dayShape,
                                                    shadowColor = NeoColors.Shadow,
                                                    offsetX = if (selected) 1.dp else 2.dp,
                                                    offsetY = if (selected) 1.dp else 2.dp
                                                )
                                                .background(
                                                    color = if (selected) NeoColors.Orange else NeoColors.Surface,
                                                    shape = dayShape
                                                )
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.5.dp,
                                                    color = NeoColors.Border,
                                                    shape = dayShape
                                                )
                                                .clickableNoRippleShared { reminderDayOfWeek = day.value },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                                style = NeoTypography.labelLarge,
                                                fontWeight = FontWeight.Black,
                                                color = NeoColors.OnSurface
                                            )
                                        }
                                    }
                                }
                            }
                            HabitFrequency.MONTHLY -> {
                                NeoLabel(stringResource(R.string.habit_reminder_day_of_month_label))
                                MonthlyDayOfMonthPicker(
                                    value = reminderDayOfMonth,
                                    onValueChange = { reminderDayOfMonth = it }
                                )
                            }
                            HabitFrequency.DAILY -> Unit
                        }

                        NeoLabel(stringResource(R.string.habit_reminder_time_label))
                        NeoButton(
                            text = "TIME  " + formatTime12Hour(reminderHour, reminderMinute),
                            onClick = { showTimePicker = true },
                            backgroundColor = NeoColors.Cyan,
                            contentColor = NeoColors.OnSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoButton(
                    text = stringResource(R.string.action_cancel).uppercase(),
                    onClick = onDismiss,
                    backgroundColor = NeoColors.Muted,
                    horizontalPadding = 14.dp
                )
                NeoButton(
                    text = stringResource(R.string.action_add).uppercase() + " HABIT",
                    onClick = {
                        onConfirm(
                            title.trim(),
                            frequency,
                            if (reminderEnabled) reminderHour else null,
                            if (reminderEnabled) reminderMinute else null,
                            if (reminderEnabled && frequency == HabitFrequency.WEEKLY) reminderDayOfWeek else null,
                            if (reminderEnabled && frequency == HabitFrequency.MONTHLY) reminderDayOfMonth else null
                        )
                    },
                    enabled = title.isNotBlank(),
                    backgroundColor = NeoColors.Lime,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showTimePicker) {
        NeoTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            title = stringResource(R.string.habit_time_picker_title),
            confirmText = stringResource(R.string.action_ok),
            cancelText = stringResource(R.string.action_cancel),
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun MonthlyDayOfMonthPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    fun wrapped(next: Int): Int = when {
        next < 1 -> 31
        next > 31 -> 1
        else -> next
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neoHardShadow(
                shape = RoundedCornerShape(10.dp),
                shadowColor = NeoColors.Shadow,
                offsetX = 4.dp,
                offsetY = 4.dp
            )
            .background(NeoColors.Surface, RoundedCornerShape(10.dp))
            .border(3.dp, NeoColors.Border, RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MonthDayButton(
            text = "-",
            backgroundColor = NeoColors.Concrete,
            onClick = { onValueChange(wrapped(value - 1)) }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(NeoColors.Yellow, RoundedCornerShape(8.dp))
                .border(2.5.dp, NeoColors.Border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = NeoTypography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface
            )
        }

        MonthDayButton(
            text = "+",
            backgroundColor = NeoColors.Lime,
            onClick = { onValueChange(wrapped(value + 1)) }
        )
    }
}

@Composable
private fun MonthDayButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(width = 64.dp, height = 56.dp)
            .neoHardShadow(
                shape = buttonShape,
                shadowColor = NeoColors.Shadow,
                offsetX = 3.dp,
                offsetY = 3.dp
            )
            .background(backgroundColor, buttonShape)
            .border(2.5.dp, NeoColors.Border, buttonShape)
            .clickableNoRippleShared(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NeoTypography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = NeoColors.OnSurface
        )
    }
}

@Composable
private fun FrequencyOptionCard(
    label: String,
    code: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .neoHardShadow(
                shape = RoundedCornerShape(10.dp),
                shadowColor = NeoColors.Shadow,
                offsetX = if (selected) 2.dp else 5.dp,
                offsetY = if (selected) 2.dp else 5.dp
            )
            .background(color = if (selected) color else NeoColors.Concrete, shape = RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 4.dp else 2.5.dp,
                color = NeoColors.Border,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (selected) Modifier.offset(x = 2.dp, y = 2.dp) else Modifier
            )
            .clickableNoRippleShared(onClick)
            .heightIn(min = 74.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(if (selected) NeoColors.Surface else color, RoundedCornerShape(6.dp))
                    .border(2.dp, NeoColors.Border, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code,
                    style = NeoTypography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface
                )
            }
            Text(
                text = label.uppercase(),
                style = NeoTypography.labelSmall,
                fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

