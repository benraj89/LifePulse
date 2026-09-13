package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.HabitFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * Covers the exact scheduling contract:
 *  - DAILY: every day at the chosen time, forever.
 *  - WEEKLY: every chosen weekday at the chosen time, forever.
 *  - MONTHLY: the chosen day of every month at the chosen time, forever.
 */
class ReminderTimeCalculatorTest {

    private fun next(
        now: String,
        frequency: HabitFrequency,
        hour: Int,
        minute: Int,
        dow: Int? = null,
        dom: Int? = null
    ) = ReminderTimeCalculator.nextTrigger(
        LocalDateTime.parse(now), frequency, hour, minute, dow, dom
    )

    // ---------- DAILY ----------

    @Test
    fun `daily before time fires today`() {
        assertEquals(
            LocalDateTime.parse("2026-09-14T21:00"),
            next("2026-09-14T08:30", HabitFrequency.DAILY, 21, 0)
        )
    }

    @Test
    fun `daily after time fires tomorrow`() {
        assertEquals(
            LocalDateTime.parse("2026-09-15T21:00"),
            next("2026-09-14T21:30", HabitFrequency.DAILY, 21, 0)
        )
    }

    @Test
    fun `daily exactly at trigger time rolls to tomorrow so it never loops`() {
        assertEquals(
            LocalDateTime.parse("2026-09-15T21:00"),
            next("2026-09-14T21:00", HabitFrequency.DAILY, 21, 0)
        )
    }

    @Test
    fun `daily crosses month and year boundaries`() {
        assertEquals(
            LocalDateTime.parse("2027-01-01T09:00"),
            next("2026-12-31T10:00", HabitFrequency.DAILY, 9, 0)
        )
    }

    // ---------- WEEKLY ----------

    @Test
    fun `weekly monday before time fires same monday`() {
        // 2026-09-14 is a Monday.
        assertEquals(
            LocalDateTime.parse("2026-09-14T21:00"),
            next("2026-09-14T10:00", HabitFrequency.WEEKLY, 21, 0, dow = 1)
        )
    }

    @Test
    fun `weekly monday after time fires next monday`() {
        assertEquals(
            LocalDateTime.parse("2026-09-21T21:00"),
            next("2026-09-14T21:05", HabitFrequency.WEEKLY, 21, 0, dow = 1)
        )
    }

    @Test
    fun `weekly from mid week fires on the upcoming target weekday`() {
        // Wednesday 2026-09-16 -> next Monday is 2026-09-21.
        assertEquals(
            LocalDateTime.parse("2026-09-21T21:00"),
            next("2026-09-16T23:00", HabitFrequency.WEEKLY, 21, 0, dow = 1)
        )
    }

    @Test
    fun `weekly sunday is supported as iso day 7`() {
        assertEquals(
            LocalDateTime.parse("2026-09-20T21:00"),
            next("2026-09-14T10:00", HabitFrequency.WEEKLY, 21, 0, dow = 7)
        )
    }

    // ---------- MONTHLY ----------

    @Test
    fun `monthly second of month before time fires this month`() {
        assertEquals(
            LocalDateTime.parse("2026-09-02T21:00"),
            next("2026-09-01T12:00", HabitFrequency.MONTHLY, 21, 0, dom = 2)
        )
    }

    @Test
    fun `monthly second of month after time fires next month`() {
        assertEquals(
            LocalDateTime.parse("2026-10-02T21:00"),
            next("2026-09-02T21:01", HabitFrequency.MONTHLY, 21, 0, dom = 2)
        )
    }

    @Test
    fun `monthly day 31 clamps to last day of short months`() {
        assertEquals(
            LocalDateTime.parse("2027-02-28T21:00"),
            next("2027-02-01T00:00", HabitFrequency.MONTHLY, 21, 0, dom = 31)
        )
    }

    @Test
    fun `monthly day 30 in february leap year clamps to 29`() {
        assertEquals(
            LocalDateTime.parse("2028-02-29T21:00"),
            next("2028-02-10T00:00", HabitFrequency.MONTHLY, 21, 0, dom = 30)
        )
    }

    @Test
    fun `monthly crosses year boundary`() {
        assertEquals(
            LocalDateTime.parse("2027-01-02T21:00"),
            next("2026-12-02T22:00", HabitFrequency.MONTHLY, 21, 0, dom = 2)
        )
    }

    // ---------- previousTrigger (catch-up) ----------

    @Test
    fun `previous daily is today when time already passed`() {
        assertEquals(
            LocalDateTime.parse("2026-09-14T21:00"),
            ReminderTimeCalculator.previousTrigger(
                LocalDateTime.parse("2026-09-14T21:10"), HabitFrequency.DAILY, 21, 0
            )
        )
    }

    @Test
    fun `previous weekly walks back to the last matching weekday`() {
        assertEquals(
            LocalDateTime.parse("2026-09-14T21:00"),
            ReminderTimeCalculator.previousTrigger(
                LocalDateTime.parse("2026-09-17T08:00"), HabitFrequency.WEEKLY, 21, 0, dayOfWeek = 1
            )
        )
    }

    @Test
    fun `previous monthly goes back a month when the day has not arrived`() {
        assertEquals(
            LocalDateTime.parse("2026-08-02T21:00"),
            ReminderTimeCalculator.previousTrigger(
                LocalDateTime.parse("2026-09-01T08:00"), HabitFrequency.MONTHLY, 21, 0, dayOfMonth = 2
            )
        )
    }

    // ---------- robustness ----------

    @Test
    fun `next trigger is always strictly in the future for every frequency`() {
        val now = LocalDateTime.parse("2026-09-14T21:00")
        HabitFrequency.entries.forEach { frequency ->
            val result = next("2026-09-14T21:00", frequency, 21, 0, dow = 1, dom = 14)
            assert(result.isAfter(now)) { "$frequency produced a non-future trigger: $result" }
        }
    }

    @Test
    fun `out of range inputs are clamped instead of throwing`() {
        // Would throw in LocalTime_of / YearMonth_atDay without clamping.
        next("2026-09-14T10:00", HabitFrequency.DAILY, 25, 99)
        next("2026-09-14T10:00", HabitFrequency.WEEKLY, 21, 0, dow = 0)
        next("2026-09-14T10:00", HabitFrequency.MONTHLY, 21, 0, dom = 99)
    }
}

