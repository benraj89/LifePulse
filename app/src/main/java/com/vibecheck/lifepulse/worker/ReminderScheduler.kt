package com.vibecheck.lifepulse.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vibecheck.lifepulse.core.ReminderTimeCalculator
import com.vibecheck.lifepulse.domain.model.Habit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Enqueues (or replaces) a single reminder for [habit], based on its own frequency/time. */
    fun scheduleHabitReminder(habit: Habit) {
        if (!habit.hasReminder) {
            cancelHabitReminder(habit.id)
            return
        }
        val now = LocalDateTime.now()
        val next = ReminderTimeCalculator.nextTrigger(
            now = now,
            frequency = habit.frequency,
            hour = habit.reminderHour!!,
            minute = habit.reminderMinute!!,
            dayOfWeek = habit.reminderDayOfWeek,
            dayOfMonth = habit.reminderDayOfMonth
        )
        val delay = Duration.between(now, next)
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf(HabitReminderWorker.KEY_HABIT_ID to habit.id))
            .addTag(workName(habit.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(habit.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelHabitReminder(habitId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(habitId))
    }

    companion object {
        private fun workName(habitId: Long) = "habit_reminder_$habitId"
    }
}



