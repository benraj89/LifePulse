package com.vibecheck.lifepulse.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.components.EmptyState
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoFab
import com.vibecheck.lifepulse.ui.neobrutalism.NeoSectionHeader
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography

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

