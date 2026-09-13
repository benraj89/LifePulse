package com.vibecheck.lifepulse.domain.usecase

import com.vibecheck.lifepulse.core.DateUtils.toLocalDate
import java.time.LocalDate
import javax.inject.Inject

/**
 * Counts consecutive completed days ending at [today] (or yesterday, so a streak
 * is not "broken" before the user has had a chance to log today).
 */
class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(completedDates: List<String>, today: LocalDate = LocalDate.now()): Int {
        if (completedDates.isEmpty()) return 0

        val days = completedDates.mapNotNull { runCatching { it.toLocalDate() }.getOrNull() }.toHashSet()

        var cursor = when {
            days.contains(today) -> today
            days.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

