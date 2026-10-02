package com.vibecheck.lifepulse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.ReminderTimeCalculator
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.worker.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
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
 *  - The next occurrence is armed before posting. If database access fails or times out,
 *    a sync worker is queued to repair the chain.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HABIT_REMINDER) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        runReminderWork(context, "ReminderAlarmReceiver") { handle(habitId) }
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

        val alreadyDone = habitRepository.isHabitCompleted(habit, DateUtils.today())

        if (occurrence >= habit.createdAt) {
            notifier.deliverOccurrence(habit.id, habit.title, occurrence, alreadyDone)
        }
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.vibecheck.lifepulse.action.HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}



