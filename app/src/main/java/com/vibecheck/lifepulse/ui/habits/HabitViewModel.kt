package com.vibecheck.lifepulse.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitUiState(
    val date: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = true
) {
    val completedCount: Int get() = habits.count { it.completedToday }
    val bestStreak: Int get() = habits.maxOfOrNull { it.currentStreak } ?: 0
}

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<HabitUiState> = habitRepository.observeHabitsForDate(today)
        .map { HabitUiState(date = today, habits = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitUiState())

    fun addHabit(
        title: String,
        frequency: HabitFrequency,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        reminderDayOfWeek: Int? = null,
        reminderDayOfMonth: Int? = null
    ) = viewModelScope.launch {
        if (title.isNotBlank()) {
            val id = habitRepository.addHabit(
                title, frequency, reminderHour, reminderMinute, reminderDayOfWeek, reminderDayOfMonth
            )
            habitRepository.getHabitById(id)?.let { reminderScheduler.scheduleHabitReminder(it) }
        }
    }

    fun toggleHabit(habitId: Long) = viewModelScope.launch {
        habitRepository.toggleHabit(habitId, today)
    }

    fun deleteHabit(habitId: Long) = viewModelScope.launch {
        habitRepository.deleteHabit(habitId)
        reminderScheduler.cancelHabitReminder(habitId)
    }
}

