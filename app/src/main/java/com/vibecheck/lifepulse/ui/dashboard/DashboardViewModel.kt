package com.vibecheck.lifepulse.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val date: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val spentToday: Double = 0.0,
    val spentThisMonth: Double = 0.0,
    val recentExpenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
) {
    val habitProgressLabel: String get() = "$completedHabits/$totalHabits Completed"
    val habitProgressFraction: Float
        get() = if (totalHabits == 0) 0f else completedHabits.toFloat() / totalHabits
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val today = LocalDate.now()

    /**
     * Single source of truth: every DAO Flow is combined into one immutable
     * UI state, so any DB write instantly re-renders the dashboard.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        habitRepository.observeHabitsForDate(today),
        expenseRepository.observeTotalInRange(DateUtils.startOfDay(today), DateUtils.endOfDay(today)),
        expenseRepository.observeTotalInRange(DateUtils.startOfMonth(today), DateUtils.endOfMonth(today)),
        expenseRepository.observeRecentExpenses(limit = 5),
        expenseRepository.observeCategories()
    ) { habits, spentToday, spentMonth, recent, categories ->
        DashboardUiState(
            date = today,
            habits = habits,
            completedHabits = habits.count { it.completedToday },
            totalHabits = habits.size,
            spentToday = spentToday,
            spentThisMonth = spentMonth,
            recentExpenses = recent,
            categories = categories,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun toggleHabit(habitId: Long) = viewModelScope.launch {
        habitRepository.toggleHabit(habitId, today)
    }

    fun addExpense(
        categoryId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        expenseRepository.addExpense(
            categoryId = categoryId,
            amount = amount,
            note = note,
            timestamp = timestamp
        )
    }

    fun addCategory(name: String, colorHex: String) = viewModelScope.launch {
        expenseRepository.addCategory(name, colorHex)
    }

    fun deleteCategory(id: Long) = viewModelScope.launch {
        expenseRepository.deleteCategory(id)
    }
}

