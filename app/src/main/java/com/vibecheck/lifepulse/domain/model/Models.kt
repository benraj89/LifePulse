package com.vibecheck.lifepulse.domain.model

enum class HabitFrequency {
    DAILY, WEEKLY, MONTHLY;

    companion object {
        fun fromRaw(raw: String): HabitFrequency =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
    }
}

data class Habit(
    val id: Long,
    val title: String,
    val frequency: HabitFrequency,
    val createdAt: Long,
    val completedToday: Boolean = false,
    val currentStreak: Int = 0,
    /** Hour (0-23) the reminder should fire, or null when no reminder is configured. */
    val reminderHour: Int? = null,
    /** Minute (0-59) the reminder should fire. */
    val reminderMinute: Int? = null,
    /** ISO day-of-week (1=Monday..7=Sunday), only meaningful for [HabitFrequency.WEEKLY]. */
    val reminderDayOfWeek: Int? = null,
    /** Day of month (1-31), only meaningful for [HabitFrequency.MONTHLY]. Values beyond the
     *  length of a given month are clamped to that month's last day. */
    val reminderDayOfMonth: Int? = null
) {
    val hasReminder: Boolean get() = reminderHour != null && reminderMinute != null
}

data class Category(
    val id: Long,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

data class Expense(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColorHex: String,
    val amount: Double,
    val dateTimestamp: Long,
    val note: String
)

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val total: Double
)

