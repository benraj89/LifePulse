package com.vibecheck.lifepulse.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.Ledger
import com.vibecheck.lifepulse.core.currentDateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
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
    val spentToday: Long = 0,
    val spentThisMonth: Long = 0,
    val incomeThisMonth: Long = 0,
    val accountBalance: Long = 0,
    val owedToYou: Long = 0,
    val owedByYou: Long = 0,
    val recentExpenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true
) {
    val habitProgressLabel: String get() = "$completedHabits/$totalHabits done"
    val habitProgressFraction: Float
        get() = if (totalHabits == 0) 0f else completedHabits.toFloat() / totalHabits
}

/** Derive actual balances and spending from exact minor units, excluding future dates. */
internal fun dashboardState(today: LocalDate, habits: List<Habit>, transactions: List<Expense>,
                            categories: List<Category>, accounts: List<Account>): DashboardUiState {
    val end = DateUtils.endOfDay(today)
    val current = transactions.filter { it.dateTimestamp <= end }
    val expenses = current.filter { it.type == TransactionType.EXPENSE }
    val monthStart = DateUtils.startOfMonth(today)
    val loans = current.filter { it.type.isLoan }
    return DashboardUiState(
        date = today, habits = habits, completedHabits = habits.count { it.completedToday }, totalHabits = habits.size,
        spentToday = expenses.filter { it.dateTimestamp >= DateUtils.startOfDay(today) }.sumOf { it.amountMinor },
        spentThisMonth = expenses.filter { it.dateTimestamp >= monthStart }.sumOf { it.amountMinor },
        incomeThisMonth = current.filter { it.type == TransactionType.INCOME && it.dateTimestamp >= monthStart }.sumOf { it.amountMinor },
        accountBalance = accounts.sumOf { Ledger.balance(it, current) },
        owedToYou = loans.filter { it.type == TransactionType.LEND }.sumOf { Ledger.outstanding(it, current) },
        owedByYou = loans.filter { it.type == TransactionType.BORROW }.sumOf { Ledger.outstanding(it, current) },
        recentExpenses = expenses.sortedWith(compareByDescending<Expense> { it.dateTimestamp }.thenByDescending { it.id }).take(3),
        categories = categories, accounts = accounts, isLoading = false
    )
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _expenseSave = MutableStateFlow(SaveState())
    val expenseSave = _expenseSave.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun clearMessage() { _message.value = null }
    private val togglingHabits = mutableSetOf<Long>()
    fun resetExpenseSave() { _expenseSave.value = SaveState() }


    /**
     * Single source of truth: every DAO Flow is combined into one immutable
     * UI state, so any DB write instantly re-renders the dashboard.
     */
    val uiState: StateFlow<DashboardUiState> = currentDateFlow().flatMapLatest { today -> combine(
        habitRepository.observeHabitsForDate(today),
        expenseRepository.observeTransactions(),
        expenseRepository.observeCategories(), expenseRepository.observeAccounts()
    ) { habits, transactions, categories, accounts ->
        dashboardState(today, habits, transactions, categories, accounts)
    } }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun toggleHabit(habitId: Long) = viewModelScope.launch {
        if (!togglingHabits.add(habitId)) return@launch
        try { habitRepository.toggleHabit(habitId, LocalDate.now())
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _message.value = e.message ?: "Couldn't update habit. Please try again."
        } finally { togglingHabits.remove(habitId) }
    }

    fun saveTransaction(draft: TransactionDraft) {
        if (_expenseSave.value.saving) return
        _expenseSave.value = SaveState(saving = true)
        writeScope.launch {
            try {
                expenseRepository.saveTransaction(draft)
                _expenseSave.value = SaveState(saved = true)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _expenseSave.value = SaveState(error = e.message ?: "Couldn't save. Please try again.") }
        }
    }

    fun addCategory(name: String, colorHex: String, kind: TransactionType) = writeScope.launch {
        try { expenseRepository.addCategory(name, colorHex, kind)
            _expenseSave.value = _expenseSave.value.copy(error = null)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _expenseSave.value = _expenseSave.value.copy(error = e.message ?: "Couldn't add category.") }
    }

    fun deleteCategory(id: Long) = writeScope.launch {
        try { expenseRepository.deleteCategory(id)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _expenseSave.value = _expenseSave.value.copy(error = e.message ?: "Couldn't delete category.") }
    }
}

