package com.vibecheck.lifepulse.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.core.ReminderTimeCalculator
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.notification.ReminderNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Safety net for the AlarmManager reminder chain.
 *
 * Exact alarms are reliable but not indestructible: they are wiped by reboots and app updates, and
 * aggressive OEM "battery optimisation" (Xiaomi/MIUI, Huawei/EMUI, Oppo/ColorOS, Samsung, ...) may
 * drop them when the app is force-stopped or put to sleep. WorkManager state, in contrast, is
 * persisted in its own database and is restored by the system after reboot.
 *
 * This worker therefore:
 *  1. re-arms the next alarm for every habit that has a reminder, and
 *  2. delivers any occurrence that was missed within the grace window, exactly once.
 *
 * It runs periodically and is also enqueued on boot / time change / app start.
 */
@HiltWorker
class ReminderSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
    private val notifier: ReminderNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        notifier.ensureChannel()

        val now = LocalDateTime.now()
        val habits = habitRepository.getAllHabitsOnce()
        val completedTodayIds = runCatching {
            habitRepository.observeHabitsForDate(DateUtils.today()).first()
                .filter { it.completedToday }
                .map { it.id }
                .toSet()
        }.getOrDefault(emptySet())

        habits.forEach { habit ->
            if (!habit.hasReminder) {
                reminderScheduler.cancelHabitReminder(habit.id)
                notifier.clearState(habit.id)
                return@forEach
            }

            // 1) Catch up a missed occurrence (device was off, alarm was wiped, ...).
            val previousLocal = ReminderTimeCalculator.previousTrigger(
                now = now,
                frequency = habit.frequency,
                hour = habit.reminderHour!!,
                minute = habit.reminderMinute!!,
                dayOfWeek = habit.reminderDayOfWeek,
                dayOfMonth = habit.reminderDayOfMonth
            )
            val previousMillis = ReminderTimeCalculator.toEpochMillis(previousLocal)
            val withinGrace = Duration.between(previousLocal, now).toMinutes() <
                ReminderTimeCalculator.GRACE_MINUTES
            val notHandledYet = notifier.lastHandledOccurrence(habit.id) < previousMillis
            // A habit created at 21:05 for "21:00 daily" must NOT immediately fire for today's
            // 21:00 slot — that occurrence happened before the habit existed.
            val existedAtOccurrence = previousMillis >= habit.createdAt

            if (withinGrace && notHandledYet && existedAtOccurrence) {
                notifier.markOccurrenceHandled(habit.id, previousMillis)
                if (habit.id !in completedTodayIds) {
                    notifier.showHabitReminder(habit.id, habit.title)
                }
            } else if (notHandledYet) {
                // Too old (or pre-dating the habit) to notify, but record it so it never fires later.
                notifier.markOccurrenceHandled(habit.id, previousMillis)
            }

            // 2) Always (re)arm the next alarm. setAlarmClock replaces any existing one, so this
            //    is idempotent and can never create duplicates.
            reminderScheduler.scheduleHabitReminder(habit, now = now)
        }
        Result.success()
    } catch (t: Throwable) {
        Log.e(TAG, "Reminder sync failed", t)
        Result.retry()
    }

    companion object {
        private const val TAG = "ReminderSyncWorker"
        private const val PERIODIC_WORK_NAME = "reminder_sync_periodic"
        private const val ONE_TIME_WORK_NAME = "reminder_sync_once"

        /** Immediate re-sync (app start, boot, time change, permission change). */
        fun enqueueOneTimeSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReminderSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /**
         * Heartbeat that re-arms alarms even if the user never opens the app. 6h is a good
         * trade-off: frequent enough to repair a wiped alarm well inside the same day, cheap
         * enough for the battery (no constraints, no wakeups of its own).
         */
        fun enqueuePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderSyncWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}


