package com.vibecheck.lifepulse.ui.dashboard

import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DashboardStateTest {
    private val today = LocalDate.of(2026, 10, 3)
    private fun entry(id: Long, type: TransactionType, amount: Long, date: LocalDate = today,
                      account: Long = 1, destination: Long? = null, loan: Long? = null) =
        Expense(id, if (type.needsCategory) 1 else null, "Food", "#FFD400", amount,
            DateUtils.startOfDay(date), "", type, account, destination, loanId = loan)

    @Test fun exactTotalsSeparateSpendingIncomeTransfersAndLoans() {
        val transactions = listOf(
            entry(1, TransactionType.INCOME, 10000),
            entry(2, TransactionType.EXPENSE, 10), entry(3, TransactionType.EXPENSE, 20),
            entry(4, TransactionType.TRANSFER, 2000, destination = 2),
            entry(5, TransactionType.LEND, 1000), entry(6, TransactionType.REPAYMENT_RECEIVED, 250, loan = 5),
            entry(7, TransactionType.BORROW, 500), entry(8, TransactionType.REPAYMENT_PAID, 100, loan = 7)
        )
        val state = dashboardState(today, emptyList(), transactions, emptyList(),
            listOf(Account(1, "Cash", 100), Account(2, "Bank", 200)))
        assertEquals(30L, state.spentToday)
        assertEquals(30L, state.spentThisMonth)
        assertEquals(10000L, state.incomeThisMonth)
        assertEquals(9920L, state.accountBalance)
        assertEquals(750L, state.owedToYou)
        assertEquals(400L, state.owedByYou)
        assertEquals(listOf(3L, 2L), state.recentExpenses.map { it.id })
    }

    @Test fun futureEntriesAndRepaymentsDoNotChangeActualBalancesOrRecentSpending() {
        val state = dashboardState(today, emptyList(), listOf(
            entry(1, TransactionType.LEND, 500),
            entry(2, TransactionType.REPAYMENT_RECEIVED, 500, today.plusDays(1), loan = 1),
            entry(3, TransactionType.EXPENSE, 100, today.plusDays(1)),
            entry(4, TransactionType.INCOME, 999, today.plusMonths(1))
        ), emptyList(), listOf(Account(1, "Cash", 1000)))
        assertEquals(500L, state.accountBalance)
        assertEquals(500L, state.owedToYou)
        assertEquals(0L, state.spentToday)
        assertEquals(0L, state.spentThisMonth)
        assertEquals(0L, state.incomeThisMonth)
        assertTrue(state.recentExpenses.isEmpty())
    }

    @Test fun monthRolloverResetsSummaryWhilePreservingRecentExpensesAndBalance() {
        val transactions = listOf(entry(1, TransactionType.EXPENSE, 123, today.withDayOfMonth(1).minusDays(1)),
            entry(2, TransactionType.EXPENSE, 456, today.minusDays(1)))
        val accounts = listOf(Account(1, "Cash", 1000))
        val state = dashboardState(today, emptyList(), transactions, emptyList(), accounts)
        assertEquals(0L, state.spentToday)
        assertEquals(456L, state.spentThisMonth)
        assertEquals(421L, state.accountBalance)
        val nextMonth = dashboardState(today.plusMonths(1), emptyList(), transactions, emptyList(), accounts)
        assertEquals(0L, nextMonth.spentThisMonth)
        assertEquals(state.recentExpenses, nextMonth.recentExpenses)
    }

    @Test fun weeklyAndMonthlyCompletionCountsOnceInHabitProgress() {
        val habits = listOf(Habit(1, "Walk", HabitFrequency.DAILY, 0),
            Habit(2, "Clean", HabitFrequency.WEEKLY, 0, completedToday = true),
            Habit(3, "Review", HabitFrequency.MONTHLY, 0, completedToday = true))
        val state = dashboardState(today, habits, emptyList(), emptyList(), emptyList())
        assertEquals(2, state.completedHabits)
        assertEquals(3, state.totalHabits)
        assertEquals(2f / 3f, state.habitProgressFraction, 0.001f)
        assertEquals(0f, DashboardUiState().habitProgressFraction, 0f)
    }
}
