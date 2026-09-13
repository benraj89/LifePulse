package com.vibecheck.lifepulse.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Central date helpers so every layer speaks the same "YYYY-MM-DD" language. */
object DateUtils {

    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun todayKey(): String = today().format(ISO_DATE)

    fun LocalDate.toKey(): String = format(ISO_DATE)

    fun String.toLocalDate(): LocalDate = LocalDate.parse(this, ISO_DATE)

    /** Epoch millis for 00:00:00.000 of the given date. */
    fun startOfDay(date: LocalDate = today()): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Epoch millis for 23:59:59.999 of the given date. */
    fun endOfDay(date: LocalDate = today()): Long =
        startOfDay(date.plusDays(1)) - 1

    fun startOfMonth(date: LocalDate = today()): Long =
        startOfDay(date.withDayOfMonth(1))

    fun endOfMonth(date: LocalDate = today()): Long =
        endOfDay(date.withDayOfMonth(date.lengthOfMonth()))

    fun formatTimestamp(millis: Long, pattern: String = "dd MMM, HH:mm"): String =
        DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(millis))
}

