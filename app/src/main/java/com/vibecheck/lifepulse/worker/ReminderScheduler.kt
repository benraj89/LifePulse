package com.vibecheck.lifepulse.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vibecheck.lifepulse.core.ReminderTimeCalculator
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.notification.ReminderAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules habit reminders with [AlarmManager].
 *
 * Why AlarmManager and not WorkManager: WorkManager's `setInitialDelay` is explicitly *inexact*.
 * Under Doze / App Standby a job can be deferred by many minutes up to hours, which is
 * unacceptable for "remind me every Monday at 21:00". `setExactAndAllowWhileIdle` is the only API
 * that fires at a precise wall-clock time even in Doze.
 *
 * One alarm is kept alive per habit at a time (the next occurrence only); when it fires,
 * [ReminderAlarmReceiver] schedules the following one. A periodic [ReminderSyncWorker] plus a
 * boot/time-change receiver re-arm everything, so the chain is self-healing and can never be lost.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /** (Re)arms the next reminder for [habit]; cancels it when the habit has no reminder. */
    fun scheduleHabitReminder(habit: Habit, now: LocalDateTime = LocalDateTime.now()) {
        if (!habit.hasReminder) {
            cancelHabitReminder(habit.id)
            return
        }
        val triggerAtMillis = ReminderTimeCalculator.nextTriggerEpochMillis(
            now = now,
            frequency = habit.frequency,
            hour = habit.reminderHour!!,
            minute = habit.reminderMinute!!,
            dayOfWeek = habit.reminderDayOfWeek,
            dayOfMonth = habit.reminderDayOfMonth
        )
        scheduleAt(habit.id, triggerAtMillis)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun scheduleAt(habitId: Long, triggerAtMillis: Long) {
        val am = alarmManager ?: return
        val pendingIntent = alarmPendingIntent(habitId, mutableFlagsForUpdate = true) ?: return

        try {
            if (canScheduleExactAlarms()) {
                // setAlarmClock is the highest-priority alarm type: it is never deferred by Doze,
                // App Standby buckets or battery saver, and it survives aggressive OEM policies
                // better than setExactAndAllowWhileIdle.
                val showIntent = PendingIntent.getActivity(
                    context,
                    SHOW_INTENT_REQUEST_BASE + (habitId % 1_000_000L).toInt(),
                    Intent(context, com.vibecheck.lifepulse.MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    pendingIntent
                )
            } else {
                // API 31+ without SCHEDULE_EXACT_ALARM granted. This is the best we can legally do:
                // it still fires in Doze, just within a maintenance window (usually minutes late).
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            Log.w(TAG, "Exact alarm denied for habit $habitId, falling back to inexact", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelHabitReminder(habitId: Long) {
        val am = alarmManager ?: return
        // NO_CREATE so we only touch an already-registered alarm; then cancel *and* release it.
        alarmPendingIntent(habitId, mutableFlagsForUpdate = false)?.let {
            am.cancel(it)
            it.cancel()
        }
    }

    /**
     * True when the app is allowed to post exact alarms. Below API 31 exact alarms are always
     * allowed; from API 31 the user (or an OEM) can revoke `SCHEDULE_EXACT_ALARM`.
     */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun alarmPendingIntent(habitId: Long, mutableFlagsForUpdate: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_HABIT_REMINDER
            // The data URI makes each habit's PendingIntent unique: filterEquals() ignores extras,
            // so without it every habit would overwrite the previous habit's alarm.
            data = android.net.Uri.parse("lifepulse://habit-reminder/$habitId")
            putExtra(ReminderAlarmReceiver.EXTRA_HABIT_ID, habitId)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (mutableFlagsForUpdate) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(habitId),
            intent,
            flags
        )
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val REQUEST_CODE_BASE = 200_000
        private const val SHOW_INTENT_REQUEST_BASE = 300_000

        fun requestCodeFor(habitId: Long): Int =
            REQUEST_CODE_BASE + (habitId % 1_000_000L).toInt()
    }
}

