package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * Computes the next local date/time a habit reminder should fire, given its frequency and the
 * user-configured time (and, for weekly/monthly habits, the day). Used both to schedule the
 * very first reminder and to reschedule the next one after each firing.
 */
object ReminderTimeCalculator {

    /**
     * @param dayOfWeek ISO day-of-week (1=Monday..7=Sunday), required for [HabitFrequency.WEEKLY].
     * @param dayOfMonth Day of month (1-31), required for [HabitFrequency.MONTHLY]. Values beyond
     * the length of a given month are clamped to that month's last day.
     */
    fun nextTrigger(
        now: LocalDateTime,
        frequency: HabitFrequency,
        hour: Int,
        minute: Int,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null
    ): LocalDateTime = when (frequency) {
        HabitFrequency.DAILY -> nextDaily(now, hour, minute)
        HabitFrequency.WEEKLY -> nextWeekly(now, hour, minute, dayOfWeek ?: DayOfWeek.MONDAY.value)
        HabitFrequency.MONTHLY -> nextMonthly(now, hour, minute, dayOfMonth ?: 1)
    }

    private fun nextDaily(now: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target
    }

    private fun nextWeekly(now: LocalDateTime, hour: Int, minute: Int, isoDayOfWeek: Int): LocalDateTime {
        var candidate = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        repeat(8) {
            if (candidate.dayOfWeek.value == isoDayOfWeek && candidate.isAfter(now)) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextMonthly(now: LocalDateTime, hour: Int, minute: Int, dayOfMonth: Int): LocalDateTime {
        fun triggerFor(yearMonth: YearMonth): LocalDateTime {
            val clampedDay = dayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
            return yearMonth.atDay(clampedDay).atTime(LocalTime.of(hour, minute))
        }

        val thisMonth = triggerFor(YearMonth.from(now))
        if (thisMonth.isAfter(now)) return thisMonth
        return triggerFor(YearMonth.from(now).plusMonths(1))
    }
}

