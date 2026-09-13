package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Computes the next local date/time a habit reminder should fire, given its frequency and the
 * user-configured time (and, for weekly/monthly habits, the day).
 *
 * All calculations are done in *local* wall-clock time and only converted to an epoch instant at
 * the very end, so a habit set for "21:00" always fires at 21:00 local time even across DST
 * transitions or time-zone changes.
 */
object ReminderTimeCalculator {

    /**
     * Grace window used when a reminder fires late (Doze, device off, boot, ...). A trigger up to
     * this many minutes in the past is still treated as "due now" instead of being skipped.
     */
    const val GRACE_MINUTES: Long = 30

    /**
     * @param dayOfWeek ISO day-of-week (1=Monday..7=Sunday), required for [HabitFrequency.WEEKLY].
     * @param dayOfMonth Day of month (1-31), required for [HabitFrequency.MONTHLY]. Values beyond
     * the length of a given month are clamped to that month's last day (31 -> 28/29 in February).
     */
    fun nextTrigger(
        now: LocalDateTime,
        frequency: HabitFrequency,
        hour: Int,
        minute: Int,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null
    ): LocalDateTime {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return when (frequency) {
            HabitFrequency.DAILY -> nextDaily(now, h, m)
            HabitFrequency.WEEKLY ->
                nextWeekly(now, h, m, (dayOfWeek ?: DayOfWeek.MONDAY.value).coerceIn(1, 7))
            HabitFrequency.MONTHLY ->
                nextMonthly(now, h, m, (dayOfMonth ?: 1).coerceIn(1, 31))
        }
    }

    /**
     * The most recent occurrence at or before [now]. Used to detect reminders that were missed
     * while the device was powered off / the alarm was wiped, so they can still be delivered
     * within [GRACE_MINUTES].
     */
    fun previousTrigger(
        now: LocalDateTime,
        frequency: HabitFrequency,
        hour: Int,
        minute: Int,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null
    ): LocalDateTime {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return when (frequency) {
            HabitFrequency.DAILY -> {
                var target = now.toLocalDate().atTime(LocalTime.of(h, m))
                if (target.isAfter(now)) target = target.minusDays(1)
                target
            }

            HabitFrequency.WEEKLY -> {
                val iso = (dayOfWeek ?: DayOfWeek.MONDAY.value).coerceIn(1, 7)
                var candidate = now.toLocalDate().atTime(LocalTime.of(h, m))
                repeat(8) {
                    if (candidate.dayOfWeek.value == iso && !candidate.isAfter(now)) return candidate
                    candidate = candidate.minusDays(1)
                }
                candidate
            }

            HabitFrequency.MONTHLY -> {
                val dom = (dayOfMonth ?: 1).coerceIn(1, 31)
                fun triggerFor(ym: YearMonth): LocalDateTime =
                    ym.atDay(dom.coerceIn(1, ym.lengthOfMonth())).atTime(LocalTime.of(h, m))

                val thisMonth = triggerFor(YearMonth.from(now))
                if (!thisMonth.isAfter(now)) thisMonth
                else triggerFor(YearMonth.from(now).minusMonths(1))
            }
        }
    }

    /** Same as [nextTrigger] but returned as an epoch-millis instant in [zone]. */
    fun nextTriggerEpochMillis(
        now: LocalDateTime,
        frequency: HabitFrequency,
        hour: Int,
        minute: Int,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = toEpochMillis(nextTrigger(now, frequency, hour, minute, dayOfWeek, dayOfMonth), zone)

    /**
     * Converts a local wall-clock reminder time to an instant.
     *
     * DST safety: during a "spring forward" gap the requested wall-clock time does not exist;
     * [ZonedDateTime.of] shifts it forward by the gap length (02:30 -> 03:30) instead of throwing.
     * During a "fall back" overlap the earlier offset is used, so the reminder fires exactly once.
     */
    fun toEpochMillis(local: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        ZonedDateTime.of(local, zone).toInstant().toEpochMilli()

    private fun nextDaily(now: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        // plusDays on the date keeps the wall-clock time stable across DST.
        while (!target.isAfter(now)) target = target.plusDays(1)
        return target
    }

    private fun nextWeekly(
        now: LocalDateTime,
        hour: Int,
        minute: Int,
        isoDayOfWeek: Int
    ): LocalDateTime {
        var candidate = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        // today + the following 7 days always contains the target weekday.
        repeat(8) {
            if (candidate.dayOfWeek.value == isoDayOfWeek && candidate.isAfter(now)) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextMonthly(
        now: LocalDateTime,
        hour: Int,
        minute: Int,
        dayOfMonth: Int
    ): LocalDateTime {
        fun triggerFor(yearMonth: YearMonth): LocalDateTime {
            val clampedDay = dayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
            return yearMonth.atDay(clampedDay).atTime(LocalTime.of(hour, minute))
        }

        var month = YearMonth.from(now)
        repeat(3) {
            val candidate = triggerFor(month)
            if (candidate.isAfter(now)) return candidate
            month = month.plusMonths(1)
        }
        return triggerFor(month)
    }
}

