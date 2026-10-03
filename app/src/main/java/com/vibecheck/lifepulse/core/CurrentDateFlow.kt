package com.vibecheck.lifepulse.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.Duration

/** Rechecks the clock while collected; also refreshes immediately after resubscription. */
fun currentDateFlow(now: () -> LocalDate = { LocalDate.now() }, pollMillis: Long = 60_000L) = flow {
    while (true) {
        emit(now())
        // Wake at midnight; otherwise check once a minute for manual clock/zone changes.
        val clock = ZonedDateTime.now()
        val midnight = clock.toLocalDate().plusDays(1).atStartOfDay(clock.zone)
        val untilMidnight = Duration.between(clock, midnight).toMillis().coerceAtLeast(1L)
        delay(minOf(pollMillis, untilMidnight))
    }
}.distinctUntilChanged()
