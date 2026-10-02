package com.vibecheck.lifepulse.domain.usecase

import com.vibecheck.lifepulse.core.DateUtils.toLocalDate
import com.vibecheck.lifepulse.core.HabitPeriod
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import java.time.LocalDate
import javax.inject.Inject

/**
 * Counts consecutive completed calendar periods. An unfinished current period does not
 * break the streak until that period ends.
 */
class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(
        completedDates: List<String>,
        today: LocalDate = LocalDate.now(),
        frequency: HabitFrequency = HabitFrequency.DAILY
    ): Int {
        if (completedDates.isEmpty()) return 0

        val days = completedDates.mapNotNull {
            runCatching { it.toLocalDate() }.getOrNull()?.takeUnless { date -> date.isAfter(today) }
                ?.let { date -> HabitPeriod.start(date, frequency) }
        }.toHashSet()
        val current = HabitPeriod.start(today, frequency)
        val previous = HabitPeriod.previous(current, frequency)

        var cursor = when {
            days.contains(current) -> current
            days.contains(previous) -> previous
            else -> return 0
        }

        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor = HabitPeriod.previous(cursor, frequency)
        }
        return streak
    }
}

