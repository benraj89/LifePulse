package com.vibecheck.lifepulse.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.domain.model.CategorySpending
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** A single day of the selected month that actually has expenses logged against it. */
data class ExpenseDayGroup(
    val date: LocalDate,
    val expenses: List<Expense>,
    val total: Double
)

data class ExpenseUiState(
    val categories: List<Category> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val expensesByDay: List<ExpenseDayGroup> = emptyList(),
    val totalToday: Double = 0.0,
    val totalSelectedMonth: Double = 0.0,
    val byCategory: List<CategorySpending> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private fun monthStart(month: YearMonth): Long = DateUtils.startOfDay(month.atDay(1))
    private fun monthEnd(month: YearMonth): Long = DateUtils.endOfDay(month.atEndOfMonth())

    val uiState: StateFlow<ExpenseUiState> = combine(
        expenseRepository.observeCategories(),
        expenseRepository.observeTotalInRange(DateUtils.startOfDay(today), DateUtils.endOfDay(today)),
        selectedMonth.flatMapLatest { month ->
            combine(
                expenseRepository.observeExpensesInRange(monthStart(month), monthEnd(month)),
                expenseRepository.observeTotalInRange(monthStart(month), monthEnd(month)),
                expenseRepository.observeTotalsByCategory(monthStart(month), monthEnd(month))
            ) { expenses, total, byCategory -> Triple(expenses, total, byCategory) }
        }
    ) { categories, totalToday, monthData ->
        val (expenses, totalMonth, byCategory) = monthData
        val zone = ZoneId.systemDefault()
        val groups = expenses
            .groupBy { Instant.ofEpochMilli(it.dateTimestamp).atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, list) -> ExpenseDayGroup(date, list, list.sumOf { it.amount }) }
        ExpenseUiState(
            categories = categories,
            selectedMonth = selectedMonth.value,
            expensesByDay = groups,
            totalToday = totalToday,
            totalSelectedMonth = totalMonth,
            byCategory = byCategory,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
    }

    fun addExpense(
        categoryId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        expenseRepository.addExpense(categoryId, amount, note, timestamp)
    }

    fun addCategory(name: String, colorHex: String) = viewModelScope.launch {
        expenseRepository.addCategory(name, colorHex)
    }

    fun deleteCategory(id: Long) = viewModelScope.launch {
        expenseRepository.deleteCategory(id)
    }

    fun deleteExpense(id: Long) = viewModelScope.launch {
        expenseRepository.deleteExpense(id)
    }
}

