package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Calendar periods: days, Monday–Sunday weeks, and calendar months. */
object HabitPeriod {
    fun start(date: LocalDate, frequency: HabitFrequency): LocalDate = when (frequency) {
        HabitFrequency.DAILY -> date
        HabitFrequency.WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        HabitFrequency.MONTHLY -> date.withDayOfMonth(1)
    }

    fun next(start: LocalDate, frequency: HabitFrequency): LocalDate = when (frequency) {
        HabitFrequency.DAILY -> start.plusDays(1)
        HabitFrequency.WEEKLY -> start.plusWeeks(1)
        HabitFrequency.MONTHLY -> start.plusMonths(1)
    }

    fun previous(start: LocalDate, frequency: HabitFrequency): LocalDate = when (frequency) {
        HabitFrequency.DAILY -> start.minusDays(1)
        HabitFrequency.WEEKLY -> start.minusWeeks(1)
        HabitFrequency.MONTHLY -> start.minusMonths(1)
    }
}
