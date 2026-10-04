package com.vibecheck.lifepulse.ui.expenses

import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal data class CategoryInsight(val key: String, val name: String, val amount: Long, val previous: Long = 0)
internal data class MonthInsight(val month: YearMonth, val amount: Long)
internal data class SpendingInsights(
    val month: YearMonth,
    val comparedMonth: YearMonth,
    val days: Int,
    val comparisonDays: Int,
    val comparedDays: Int,
    val spent: Long,
    val comparisonSpent: Long,
    val previousSpent: Long,
    val income: Long,
    val count: Int,
    val categories: List<CategoryInsight>,
    val changes: List<CategoryInsight>,
    val trend: List<MonthInsight>
) {
    val averagePerDay: Long get() = if (days > 0) spent / days else 0
    val changePercent: Double? get() = if (previousSpent > 0) (comparisonSpent - previousSpent).toDouble() / previousSpent * 100 else null

    // Keep the donut readable; the complete category list remains available below it.
    val slices: List<CategoryInsight> get() = if (categories.size <= 6) categories else
        categories.take(5) + CategoryInsight("other", "Other categories", categories.drop(5).sumOf { it.amount })
}

/** Money stays in minor units; floating point is used only to draw ratios and percentages. */
internal fun spendingInsights(transactions: List<Expense>, month: YearMonth, comparedMonth: YearMonth,
                              accountId: Long?, matchDays: Boolean, today: LocalDate,
                              zone: ZoneId = ZoneId.systemDefault()): SpendingInsights {
    fun elapsedDays(value: YearMonth): Int = when {
        value > YearMonth.from(today) -> 0
        value == YearMonth.from(today) -> today.dayOfMonth
        else -> value.lengthOfMonth()
    }
    val days = elapsedDays(month)
    val comparisonDays = if (matchDays) minOf(days, elapsedDays(comparedMonth)) else days
    val comparedDays = if (matchDays) comparisonDays else elapsedDays(comparedMonth)
    val dated = transactions.asSequence()
        .filter { it.type.needsCategory && (accountId == null || it.accountId == accountId) }
        .map { it to Instant.ofEpochMilli(it.dateTimestamp).atZone(zone).toLocalDate() }
        .filter { (_, date) -> date <= today }
        .toList()
    fun period(value: YearMonth, limit: Int) = dated.filter { (_, date) -> YearMonth.from(date) == value && date.dayOfMonth <= limit }.map { it.first }
    fun key(entry: Expense) = entry.categoryId?.let { "category_$it" } ?: "name_${entry.categoryName}"
    fun categories(entries: List<Expense>) = entries.filter { it.type == TransactionType.EXPENSE }.groupBy(::key)
        .mapValues { (_, group) -> CategoryInsight(key(group.first()), group.first().categoryName, group.sumOf { it.amountMinor }) }
    val current = period(month, days)
    val comparisonCurrent = period(month, comparisonDays)
    val previous = period(comparedMonth, comparedDays)
    val currentCategories = categories(current)
    val comparedCurrentCategories = categories(comparisonCurrent)
    val previousCategories = categories(previous)
    val changes = (comparedCurrentCategories.keys + previousCategories.keys).map { key ->
        CategoryInsight(key, (comparedCurrentCategories[key] ?: previousCategories.getValue(key)).name,
            comparedCurrentCategories[key]?.amount ?: 0, previousCategories[key]?.amount ?: 0)
    }.sortedByDescending { kotlin.math.abs(it.amount - it.previous) }
    return SpendingInsights(month, comparedMonth, days, comparisonDays, comparedDays,
        current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor },
        comparisonCurrent.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor },
        previous.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor },
        current.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
        current.count { it.type == TransactionType.EXPENSE },
        currentCategories.values.sortedByDescending { it.amount }, changes,
        (5 downTo 0).map { offset ->
            val target = month.minusMonths(offset.toLong())
            MonthInsight(target, period(target, elapsedDays(target)).filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor })
        })
}
