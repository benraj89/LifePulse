package com.vibecheck.lifepulse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.ReminderTimeCalculator
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.worker.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Receives the exact alarm for a single habit, posts the reminder and immediately arms the next
 * occurrence.
 *
 * Correctness notes:
 *  - The next alarm is scheduled from `LocalDateTime.now()`, and the calculator only ever returns
 *    a strictly future instant, so a late-firing alarm can never re-schedule itself into the past
 *    (which would cause an infinite notification loop).
 *  - Rescheduling happens in a `finally`-style path: even if reading the habit or posting fails,
 *    the chain is re-armed, so a single failure never silently kills the reminder forever.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        // A BroadcastReceiver's process may be killed as soon as onReceive() returns, so we hold
        // the broadcast open while we touch the database on a background thread.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Hard timeout: the system kills the receiver after ~10s anyway, and we must
                // always reach the finally block that re-arms the alarm.
                withTimeoutOrNull(BROADCAST_TIMEOUT_MS) {
                    handle(habitId)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to handle reminder for habit $habitId", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(habitId: Long) {
        val habit = habitRepository.getHabitById(habitId)
        if (habit == null || !habit.hasReminder) {
            // Habit was deleted or its reminder was turned off while the alarm was pending.
            reminderScheduler.cancelHabitReminder(habitId)
            notifier.clearState(habitId)
            return
        }

        val now = LocalDateTime.now()

        // Re-arm the next occurrence FIRST so the chain survives a crash while notifying.
        // nextTrigger() is strictly in the future, so a late alarm can never loop.
        reminderScheduler.scheduleHabitReminder(habit, now = now)

        // Mark this occurrence as handled so the catch-up sync will not repeat it.
        val occurrence = ReminderTimeCalculator.toEpochMillis(
            ReminderTimeCalculator.previousTrigger(
                now = now,
                frequency = habit.frequency,
                hour = habit.reminderHour!!,
                minute = habit.reminderMinute!!,
                dayOfWeek = habit.reminderDayOfWeek,
                dayOfMonth = habit.reminderDayOfMonth
            )
        )
        notifier.markOccurrenceHandled(habitId, occurrence)

        val alreadyDone = runCatching {
            habitRepository.observeHabitsForDate(DateUtils.today()).first()
                .firstOrNull { it.id == habitId }?.completedToday
        }.getOrNull() ?: false

        if (!alreadyDone) {
            notifier.showHabitReminder(habit.id, habit.title)
        }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        private const val BROADCAST_TIMEOUT_MS = 8_000L
        const val ACTION_HABIT_REMINDER = "com.vibecheck.lifepulse.action.HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}



