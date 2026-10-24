package com.vibecheck.lifepulse.ui.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(onDismiss: () -> Unit,
    onConfirm: (String, HabitFrequency, Int?, Int?, Int?, Int?) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var frequencyName by rememberSaveable { mutableStateOf(HabitFrequency.DAILY.name) }
    val frequency = HabitFrequency.valueOf(frequencyName)
    var reminder by rememberSaveable { mutableStateOf(false) }
    var hour by rememberSaveable { mutableStateOf(9) }
    var minute by rememberSaveable { mutableStateOf(0) }
    var weekDay by rememberSaveable { mutableStateOf(DayOfWeek.MONDAY.value) }
    var monthDay by rememberSaveable { mutableStateOf(1) }
    var showTime by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NeoColors.Background,
        shape = NeoFormSheetShape,
        dragHandle = { NeoSheetGrip() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoSheetHeader(stringResource(R.string.habit_dialog_title), onDismiss)
                NeoFormTextField(title, { title = it }, label = stringResource(R.string.habit_title_label),
                    placeholder = stringResource(R.string.habit_dialog_field_title), color = NeoColors.PaleCyan)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NeoSticker(stringResource(R.string.habit_dialog_frequency_label), color = NeoColors.Habit)
                    NeoFormTabs(HabitFrequency.entries.map { it.name to frequencyLabel(it) }, frequencyName, { frequencyName = it })
                }
                val reminderLabel = stringResource(R.string.habit_reminder_enable)
                NeoReminderToggle(reminderLabel, reminder, { reminder = it })
                if (reminder) {
                    if (frequency == HabitFrequency.WEEKLY) {
                        Text(stringResource(R.string.habit_reminder_day_of_week_label), style = NeoTypography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DayOfWeek.entries.size) { index ->
                                val day = DayOfWeek.entries[index]
                                NeoButton(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), { weekDay = day.value },
                                    modifier = Modifier.widthIn(min = 52.dp).semantics {
                                        contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                        selected = weekDay == day.value
                                    }, backgroundColor = if (weekDay == day.value) NeoColors.Yellow else NeoColors.LightViolet,
                                    shadowOffset = if (weekDay == day.value) 3.dp else 0.dp)
                            }
                        }
                    }
                    if (frequency == HabitFrequency.MONTHLY) NeoFormChoice(
                        stringResource(R.string.habit_reminder_day_of_month_label),
                        (1..31).map { it.toString() to it.toString() }, monthDay.toString(), { monthDay = it.toInt() })
                    NeoFormActionRow(stringResource(R.string.habit_reminder_time_label), formatTime12Hour(hour, minute),
                        { showTime = true }, color = NeoColors.BubblegumPink)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                NeoButton(stringResource(R.string.habit_add_action), {
                    onConfirm(title.trim(), frequency, if (reminder) hour else null, if (reminder) minute else null,
                        if (reminder && frequency == HabitFrequency.WEEKLY) weekDay else null,
                        if (reminder && frequency == HabitFrequency.MONTHLY) monthDay else null)
                }, Modifier.weight(1f), enabled = title.isNotBlank(), textStyle = NeoTypography.titleLarge,
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
            }
        }
    }
    if (showTime) NeoTimePickerDialog(hour, minute, onConfirm = { selectedHour, selectedMinute ->
        hour = selectedHour; minute = selectedMinute; showTime = false
    }, onDismiss = { showTime = false })
}

@Preview(name = "Create habit", widthDp = 360, heightDp = 800)
@Preview(name = "Create habit large text", widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Composable
private fun AddHabitPreview() { NeoBrutalismTheme { AddHabitDialog({}) { _, _, _, _, _, _ -> } } }
