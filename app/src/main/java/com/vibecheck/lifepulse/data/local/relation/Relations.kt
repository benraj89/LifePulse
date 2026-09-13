package com.vibecheck.lifepulse.data.local.relation

import androidx.room.Embedded
import com.vibecheck.lifepulse.data.local.entity.HabitEntity

/** Projection of a habit + whether it has a log row for the queried date. */
data class HabitWithTodayStatus(
    @Embedded val habit: HabitEntity,
    val completedToday: Boolean
)

/** Projection of an expense joined with its owning category. */
data class ExpenseWithCategory(
    @Embedded val expense: com.vibecheck.lifepulse.data.local.entity.ExpenseEntity,
    val categoryName: String,
    val categoryColorHex: String
)

/** Aggregated spending per category for a date range. */
data class CategoryTotal(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val total: Double
)

