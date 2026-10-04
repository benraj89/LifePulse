package com.vibecheck.lifepulse.ui.expenses

import com.vibecheck.lifepulse.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class SpendingInsightsTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 3)
    private val month = YearMonth.from(today)
    private fun entry(id: Long, date: String, amount: Long, type: TransactionType = TransactionType.EXPENSE,
                      account: Long = 1, category: Long = 1) = Expense(id, category, "Category $category", "#FFD400", amount,
        LocalDate.parse(date).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(), "", type, account)
    private fun insights(entries: List<Expense>, match: Boolean = true, account: Long? = null) =
        spendingInsights(entries, month, month.minusMonths(1), account, match, today, zone)

    @Test fun comparesElapsedDaysWithoutHidingFullMonthTotals() {
        val entries = listOf(entry(1, "2026-10-02", 1000), entry(2, "2026-09-02", 500), entry(3, "2026-09-20", 9000))
        val matched = insights(entries)
        assertEquals(3, matched.comparedDays)
        assertEquals(500L, matched.previousSpent)
        assertEquals(100.0, matched.changePercent!!, 0.001)
        val full = insights(entries, match = false)
        assertEquals(9500L, full.previousSpent)
        assertEquals(30, full.comparedDays)
        assertEquals(matched.spent, full.spent)
    }

    @Test fun shorterMonthsUseEqualDayCountsAndKeepCategoryChartComplete() {
        val march = YearMonth.of(2024, 3)
        val data = spendingInsights(listOf(entry(1, "2024-03-29", 100), entry(2, "2024-03-31", 200), entry(3, "2024-02-29", 50)),
            march, march.minusMonths(1), null, true, LocalDate.of(2024, 4, 5), zone)
        assertEquals(29, data.comparisonDays)
        assertEquals(29, data.comparedDays)
        assertEquals(100L, data.comparisonSpent)
        assertEquals(300L, data.spent)
        assertEquals(300L, data.categories.single().amount)
        assertEquals(100L, data.changes.single().amount)
        assertEquals(300L, data.trend.last().amount)
    }

    @Test fun expensesOnlyAndAccountFilterApplyToEveryChart() {
        val entries = listOf(entry(1, "2026-10-01", 10), entry(2, "2026-10-01", 20),
            entry(3, "2026-10-01", 200, TransactionType.INCOME), entry(4, "2026-10-01", 999, TransactionType.TRANSFER),
            entry(5, "2026-10-01", 800, TransactionType.LEND), entry(6, "2026-10-01", 700, TransactionType.REPAYMENT_RECEIVED),
            entry(7, "2026-10-01", 400, account = 2), entry(8, "2026-09-01", 5))
        val data = insights(entries, account = 1)
        assertEquals(30L, data.spent)
        assertEquals(200L, data.income)
        assertEquals(2, data.count)
        assertEquals(10L, data.averagePerDay)
        assertEquals(30L, data.categories.single().amount)
        assertEquals(30L, data.trend.last().amount)
        assertEquals(5L, data.previousSpent)
    }

    @Test fun futureEntriesAreExcludedAndEmptyComparisonHasNoInfinitePercentage() {
        val data = insights(listOf(entry(1, "2026-10-04", 1000), entry(2, "2026-10-01", 5)))
        assertEquals(5L, data.spent)
        assertNull(data.changePercent)
        val empty = insights(emptyList())
        assertEquals(0L, empty.spent)
        assertNull(empty.changePercent)
        assertTrue(empty.slices.isEmpty())
        assertEquals(6, empty.trend.size)
        assertTrue(empty.trend.all { it.amount == 0L })
    }

    @Test fun smallCategoriesAreGroupedWithoutLosingMoneyAndPreviousOnlyCategoriesRemain() {
        val entries = (1L..8L).map { entry(it, "2026-10-01", it * 100, category = it) } +
            entry(9, "2026-09-01", 2000, category = 9)
        val data = insights(entries)
        assertEquals(6, data.slices.size)
        assertEquals(data.spent, data.slices.sumOf { it.amount })
        assertEquals(600L, data.slices.last().amount)
        assertEquals(9, data.changes.size)
        assertEquals(0L, data.changes.first().amount)
        assertEquals(2000L, data.changes.first().previous)
    }

    @Test fun trendIncludesYearBoundaryAndZeroMonthsInOrder() {
        val data = spendingInsights(listOf(entry(1, "2025-12-31", 123)), YearMonth.of(2026, 2), YearMonth.of(2026, 1),
            null, false, today, zone)
        assertEquals(YearMonth.of(2025, 9), data.trend.first().month)
        assertEquals(YearMonth.of(2026, 2), data.trend.last().month)
        assertEquals(123L, data.trend.single { it.month == YearMonth.of(2025, 12) }.amount)
        assertEquals(5, data.trend.count { it.amount == 0L })
    }
}
