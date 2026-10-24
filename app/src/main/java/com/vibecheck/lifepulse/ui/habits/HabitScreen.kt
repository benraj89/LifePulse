package com.vibecheck.lifepulse.ui.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.neobrutalism.*

@Composable
fun HabitScreen(modifier: Modifier = Modifier, viewModel: HabitViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    HabitContent(state, { showAdd = true }, viewModel::toggleHabit, viewModel::deleteHabit, modifier)
    if (showAdd) AddHabitDialog({ showAdd = false }) { title, frequency, hour, minute, weekDay, monthDay ->
        viewModel.addHabit(title, frequency, hour, minute, weekDay, monthDay)
        showAdd = false
    }
}

@Composable
internal fun HabitContent(state: HabitUiState, onAdd: () -> Unit, onToggle: (Long) -> Unit,
                          onDelete: (Long) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier, containerColor = NeoColors.Background, contentWindowInsets = WindowInsets(0),
        topBar = { NeoPageHeader(stringResource(R.string.habit_title)) {
            NeoButton(stringResource(R.string.habit_add_content_description), onAdd, enabled = !state.isLoading,
                leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
        } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("habits"),
                contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isLoading) item { Text(stringResource(R.string.habits_loading), style = NeoTypography.bodyLarge) }
                else if (state.habits.isEmpty()) item {
                    NeoFeatureCard(stringResource(R.string.habit_empty_title), NeoColors.Habit) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(R.drawable.empty_hobbie), null, Modifier.size(88.dp))
                            Text(stringResource(R.string.habit_empty_subtitle), style = NeoTypography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    item {
                        NeoFeatureCard(stringResource(R.string.dashboard_habit_progress), NeoColors.Habit) {
                            Text(stringResource(R.string.dashboard_habit_progress_done, state.completedCount, state.habits.size),
                                style = NeoTypography.headlineLarge)
                            val progress by animateFloatAsState(state.completedCount.toFloat() / state.habits.size, tween(180), label = "habitProgress")
                            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(8.dp),
                                color = NeoColors.Ink, trackColor = NeoColors.Surface, strokeCap = StrokeCap.Butt,
                                gapSize = 0.dp, drawStopIndicator = {})
                        }
                    }
                    HabitFrequency.entries.forEach { frequency ->
                        val group = state.habits.filter { it.frequency == frequency }
                        if (group.isNotEmpty()) {
                            item(key = "frequency_$frequency") {
                                NeoSticker(frequencyLabel(frequency), color = when (frequency) {
                                    HabitFrequency.DAILY -> NeoColors.Yellow
                                    HabitFrequency.WEEKLY -> NeoColors.LightViolet
                                    HabitFrequency.MONTHLY -> NeoColors.BubblegumPink
                                }, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp).semantics { heading() })
                            }
                            items(group, key = { it.id }) { habit -> HabitRow(habit,
                                onToggle = { onToggle(habit.id) }, onDelete = { onDelete(habit.id) }) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Habits", widthDp = 360, heightDp = 800)
@Preview(name = "Habits • enlarged text", widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Composable
private fun HabitsPreview() {
    NeoBrutalismTheme { HabitContent(HabitUiState(habits = listOf(
        Habit(1, "Make time for a walk around the neighbourhood", HabitFrequency.DAILY, 0),
        Habit(2, "Plan the week", HabitFrequency.WEEKLY, 0, completedToday = true, currentStreak = 3)
    ), isLoading = false), {}, {}, {}) }
}
