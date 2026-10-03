package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.domain.usecase.CalculateStreakUseCase
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitPeriodTest {
    private val streak = CalculateStreakUseCase()

    @Test fun `weekly period starts Monday even across year boundary`() {
        assertEquals(LocalDate.parse("2025-12-29"),
            HabitPeriod.start(LocalDate.parse("2026-01-04"), HabitFrequency.WEEKLY))
        assertEquals(LocalDate.parse("2026-01-05"),
            HabitPeriod.start(LocalDate.parse("2026-01-05"), HabitFrequency.WEEKLY))
    }

    @Test fun `monthly periods handle leap year and year boundary`() {
        val start = HabitPeriod.start(LocalDate.parse("2028-02-29"), HabitFrequency.MONTHLY)
        assertEquals(LocalDate.parse("2028-02-01"), start)
        assertEquals(LocalDate.parse("2028-03-01"), HabitPeriod.next(start, HabitFrequency.MONTHLY))
        assertEquals(LocalDate.parse("2027-12-01"),
            HabitPeriod.previous(LocalDate.parse("2028-01-01"), HabitFrequency.MONTHLY))
    }

    @Test fun `weekly streak counts periods and deduplicates legacy logs`() {
        assertEquals(3, streak(listOf("2026-09-14", "2026-09-15", "2026-09-09", "2026-09-01"),
            LocalDate.parse("2026-09-20"), HabitFrequency.WEEKLY))
    }

    @Test fun `weekly streak survives unfinished current week`() {
        assertEquals(2, streak(listOf("2026-09-14", "2026-09-07"),
            LocalDate.parse("2026-09-23"), HabitFrequency.WEEKLY))
    }

    @Test fun `missing week breaks streak`() {
        assertEquals(1, streak(listOf("2026-09-23", "2026-09-07"),
            LocalDate.parse("2026-09-23"), HabitFrequency.WEEKLY))
    }

    @Test fun `monthly streak spans unequal months and year boundary`() {
        assertEquals(3, streak(listOf("2027-01-31", "2026-12-05", "2026-11-30"),
            LocalDate.parse("2027-02-01"), HabitFrequency.MONTHLY))
    }

    @Test fun `daily grace behavior remains and ignores invalid and future dates`() {
        assertEquals(2, streak(listOf("2026-09-13", "2026-09-12", "bad", "2026-09-15"),
            LocalDate.parse("2026-09-14")))
        assertEquals(0, streak(listOf("2026-09-12"), LocalDate.parse("2026-09-14")))
    }

    @Test fun `date observation updates across midnight and clock rollback`() = runBlocking {
        val dates = listOf("2026-09-30", "2026-09-30", "2026-10-01", "2026-09-29")
            .map(LocalDate::parse)
        var index = 0
        assertEquals(listOf(dates[0], dates[2], dates[3]),
            currentDateFlow(now = { dates[index++] }, pollMillis = 1).take(3).toList())
    }
}
