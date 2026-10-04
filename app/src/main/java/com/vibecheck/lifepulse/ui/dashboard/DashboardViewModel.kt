package com.vibecheck.lifepulse.ui.dashboard

import com.vibecheck.lifepulse.R
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.currentDateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.vibecheck.lifepulse.ui.expenses.SaveState
import com.vibecheck.lifepulse.domain.model.TransactionType
import com.vibecheck.lifepulse.domain.model.Account
import com.vibecheck.lifepulse.domain.model.TransactionDraft
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
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true
) {
    val habitProgressFraction: Float
        get() = if (totalHabits == 0) 0f else completedHabits.toFloat() / totalHabits
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _expenseSave = MutableStateFlow(SaveState())
    val expenseSave = _expenseSave.asStateFlow()
    fun resetExpenseSave() { _expenseSave.value = SaveState() }


    /**
     * Single source of truth: every DAO Flow is combined into one immutable
     * UI state, so any DB write instantly re-renders the dashboard.
     */
    val uiState: StateFlow<DashboardUiState> = currentDateFlow().flatMapLatest { today -> combine(
        habitRepository.observeHabitsForDate(today),
        expenseRepository.observeTotalInRange(DateUtils.startOfDay(today), DateUtils.endOfDay(today)),
        expenseRepository.observeTotalInRange(DateUtils.startOfMonth(today), DateUtils.endOfMonth(today)),
        expenseRepository.observeRecentExpenses(limit = 5),
        combine(expenseRepository.observeCategories(), expenseRepository.observeAccounts()) { categories, accounts -> categories to accounts }
    ) { habits, spentToday, spentMonth, recent, categoryAccounts ->
        DashboardUiState(
            date = today,
            habits = habits,
            completedHabits = habits.count { it.completedToday },
            totalHabits = habits.size,
            spentToday = spentToday,
            spentThisMonth = spentMonth,
            recentExpenses = recent,
            categories = categoryAccounts.first.filter { it.kind == TransactionType.EXPENSE },
            accounts = categoryAccounts.second,
            isLoading = false
        )
    } }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun toggleHabit(habitId: Long) = viewModelScope.launch {
        habitRepository.toggleHabit(habitId, LocalDate.now())
    }

    fun addExpense(
        accountId: Long,
        categoryId: Long,
        amountMinor: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (_expenseSave.value.saving) return
        _expenseSave.value = SaveState(saving = true)
        writeScope.launch {
            try {
                expenseRepository.saveTransaction(TransactionDraft(accountId = accountId, categoryId = categoryId,
                    amountMinor = amountMinor, note = note, dateTimestamp = timestamp))
                _expenseSave.value = SaveState(saved = true)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _expenseSave.value = SaveState(error = e.message ?: context.getString(R.string.error_save_retry)) }
        }
    }

    fun addCategory(name: String, colorHex: String) = writeScope.launch {
        try { expenseRepository.addCategory(name, colorHex)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _expenseSave.value = SaveState(error = e.message ?: context.getString(R.string.error_add_category)) }
    }

    fun deleteCategory(id: Long) = writeScope.launch {
        try { expenseRepository.deleteCategory(id)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _expenseSave.value = SaveState(error = e.message ?: context.getString(R.string.error_delete_category)) }
    }
}

