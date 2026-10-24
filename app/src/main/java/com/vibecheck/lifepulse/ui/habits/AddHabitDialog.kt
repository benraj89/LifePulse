package com.vibecheck.lifepulse.ui.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NeoColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoSheetHeader(stringResource(R.string.habit_dialog_title), onDismiss)
                Text(stringResource(R.string.habit_title_label), style = NeoTypography.labelLarge)
                NeoTextField(title, { title = it }, placeholder = stringResource(R.string.habit_dialog_field_title),
                    label = stringResource(R.string.habit_title_label), modifier = Modifier.fillMaxWidth())
                NeoSticker(stringResource(R.string.habit_dialog_frequency_label), color = NeoColors.Habit)
                NeoTextTabs(HabitFrequency.entries.map { it.name to frequencyLabel(it) }, frequencyName, { frequencyName = it })
                HorizontalDivider(color = NeoColors.Rule)
                val reminderLabel = stringResource(R.string.habit_reminder_enable)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(reminderLabel, style = NeoTypography.titleMedium, modifier = Modifier.weight(1f))
                    NeoSwitch(reminder, { reminder = it }, Modifier.semantics { contentDescription = reminderLabel })
                }
                if (reminder) {
                    if (frequency == HabitFrequency.WEEKLY) {
                        Text(stringResource(R.string.habit_reminder_day_of_week_label), style = NeoTypography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DayOfWeek.entries.size) { index ->
                                val day = DayOfWeek.entries[index]
                                FilterChip(selected = weekDay == day.value, onClick = { weekDay = day.value },
                                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                                        contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                    }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeoColors.Habit))
                            }
                        }
                    }
                    if (frequency == HabitFrequency.MONTHLY) NeoInlineChoice(
                        stringResource(R.string.habit_reminder_day_of_month_label),
                        (1..31).map { it.toString() to it.toString() }, monthDay.toString(), { monthDay = it.toInt() })
                    Text(stringResource(R.string.habit_reminder_time_label), style = NeoTypography.labelLarge)
                    NeoButton(stringResource(R.string.habit_reminder_time_format, formatTime12Hour(hour, minute)),
                        { showTime = true }, Modifier.fillMaxWidth(), backgroundColor = NeoColors.Habit, shadowOffset = 0.dp)
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                NeoButton(stringResource(R.string.habit_add_action), {
                    onConfirm(title.trim(), frequency, if (reminder) hour else null, if (reminder) minute else null,
                        if (reminder && frequency == HabitFrequency.WEEKLY) weekDay else null,
                        if (reminder && frequency == HabitFrequency.MONTHLY) monthDay else null)
                }, Modifier.fillMaxWidth(), enabled = title.isNotBlank())
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    }
    if (showTime) NeoTimePickerDialog(hour, minute, onConfirm = { selectedHour, selectedMinute ->
        hour = selectedHour; minute = selectedMinute; showTime = false
    }, onDismiss = { showTime = false })
}

@Preview(name = "Create habit", widthDp = 360, heightDp = 800)
@Composable
private fun AddHabitPreview() { NeoBrutalismTheme { AddHabitDialog({}) { _, _, _, _, _, _ -> } } }
