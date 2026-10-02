package com.vibecheck.lifepulse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.worker.ReminderScheduler
import com.vibecheck.lifepulse.worker.ReminderSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Re-arms every habit alarm after events that wipe or invalidate pending alarms:
 *
 *  - `BOOT_COMPLETED` / `QUICKBOOT_POWERON` (OEM variant): AlarmManager alarms do **not** survive
 *    a reboot, so without this every reminder would be silently lost after a restart.
 *  - `MY_PACKAGE_REPLACED`: alarms are cleared when the app is updated.
 *  - `TIME_SET` / `TIMEZONE_CHANGED`: a stored RTC instant no longer maps to the user's chosen
 *    wall-clock time; recomputing keeps "21:00" meaning 21:00 in the new zone.
 *  - `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (API 31+): the user just granted/revoked the
 *    exact-alarm permission, so alarms must be re-armed with the right precision.
 *
 * Alarms are restored with bounded asynchronous database work; [ReminderSyncWorker] provides
 * catch-up and retries if the direct restoration fails.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON,
            ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                restoreAlarms(context)
        }
    }

    private fun restoreAlarms(context: Context) {
        // Boot/time changes must re-arm promptly; WorkManager can be deferred in Doze.
        val pendingResult = goAsync()
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LifePulse:restore-reminders")
            .apply { acquire(10_000L) }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000L) {
                    habitRepository.getAllHabitsOnce().forEach {
                        reminderScheduler.scheduleHabitReminder(it)
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderBootReceiver", "Unable to restore all alarms", e)
            } finally {
                try {
                    ReminderSyncWorker.enqueueOneTimeSync(context)
                } finally {
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}

