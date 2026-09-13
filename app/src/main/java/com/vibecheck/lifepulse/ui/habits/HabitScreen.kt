package com.vibecheck.lifepulse.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.components.EmptyState
import com.vibecheck.lifepulse.ui.neobrutalism.NeoButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoFab
import com.vibecheck.lifepulse.ui.neobrutalism.NeoIconButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoSectionHeader
import com.vibecheck.lifepulse.ui.neobrutalism.NeoStackedDialogCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoSwitch
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTextField
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTimePickerDialog
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import com.vibecheck.lifepulse.ui.neobrutalism.neoHardShadow
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    modifier: Modifier = Modifier,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = NeoColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.habit_title),
                        style = NeoTypography.headlineMedium,
                        color = NeoColors.OnSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeoColors.Background,
                    titleContentColor = NeoColors.OnSurface
                ),
                actions = {
                    Text(
                        text = stringResource(
                            R.string.habit_progress_format,
                            state.completedCount,
                            state.habits.size
                        ),
                        style = NeoTypography.titleMedium,
                        color = NeoColors.OnSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            NeoFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.habit_add_content_description),
                onClick = { showAddDialog = true }
            )
        }
    ) { innerPadding ->
        if (state.habits.isEmpty() && !state.isLoading) {
            EmptyState(
                title = stringResource(R.string.habit_empty_title),
                subtitle = stringResource(R.string.habit_empty_subtitle),
                imageRes = R.drawable.empty_hobbie,
                imageSize = 350.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeoColors.Background)
                    .padding(innerPadding)
            )
        } else {
            val grouped = state.habits.groupBy { it.frequency }
            val sections = listOf(
                HabitSection(HabitFrequency.DAILY, stringResource(R.string.frequency_daily), NeoColors.Lime, "D", -1.5f),
                HabitSection(HabitFrequency.WEEKLY, stringResource(R.string.frequency_weekly), NeoColors.Cyan, "W", 1.5f),
                HabitSection(HabitFrequency.MONTHLY, stringResource(R.string.frequency_monthly), NeoColors.Orange, "M", -1.5f)
            ).filter { !grouped[it.frequency].isNullOrEmpty() }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeoColors.Background)
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sections.forEach { section ->
                    val habitsInGroup = grouped[section.frequency].orEmpty()
                    item(key = "header_${section.frequency.name}") {
                        NeoSectionHeader(
                            title = section.label,
                            color = section.color,
                            icon = section.icon,
                            count = habitsInGroup.size,
                            rotationDegrees = section.rotation,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                    }
                    items(habitsInGroup, key = { it.id }) { habit ->
                        HabitRow(
                            habit = habit,
                            accentColor = section.color,
                            onToggle = { viewModel.toggleHabit(habit.id) },
                            onDelete = { viewModel.deleteHabit(habit.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, frequency, hour, minute, dayOfWeek, dayOfMonth ->
                viewModel.addHabit(title, frequency, hour, minute, dayOfWeek, dayOfMonth)
                showAddDialog = false
            }
        )
    }
}

private data class HabitSection(
    val frequency: HabitFrequency,
    val label: String,
    val color: Color,
    val icon: String,
    val rotation: Float
)

@Composable
private fun HabitRow(habit: Habit, accentColor: Color = NeoColors.Primary, onToggle: () -> Unit, onDelete: () -> Unit) {
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
                        stringResource(
                            R.string.habit_streak_format,
                            frequencyLabel(habit.frequency),
                            habit.currentStreak
                        ),
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
private fun frequencyLabel(frequency: HabitFrequency): String = when (frequency) {
    HabitFrequency.DAILY -> stringResource(R.string.frequency_daily)
    HabitFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
    HabitFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
}

private fun formatTime12Hour(hour24: Int, minute: Int): String {
    val isPm = hour24 >= 12
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return "%d:%02d %s".format(hour12, minute, if (isPm) "PM" else "AM")
}

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

private fun Modifier.clickableNoRippleShared(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun NeoLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 8.dp)
                .background(NeoColors.Cyan, RoundedCornerShape(2.dp))
        )
        Text(
            text = text.uppercase(),
            style = NeoTypography.labelLarge,
            fontWeight = FontWeight.Black,
            color = NeoColors.OnSurface
        )
    }
}

private fun frequencyColor(frequency: HabitFrequency): Color = when (frequency) {
    HabitFrequency.DAILY -> NeoColors.Lime
    HabitFrequency.WEEKLY -> NeoColors.Cyan
    HabitFrequency.MONTHLY -> NeoColors.Orange
}

private fun frequencyIcon(frequency: HabitFrequency): String = when (frequency) {
    HabitFrequency.DAILY -> "D"
    HabitFrequency.WEEKLY -> "W"
    HabitFrequency.MONTHLY -> "M"
}

