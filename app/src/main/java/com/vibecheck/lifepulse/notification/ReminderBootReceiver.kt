package com.vibecheck.lifepulse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vibecheck.lifepulse.worker.ReminderSyncWorker

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
 * Work is delegated to [ReminderSyncWorker] because a receiver may not do long database I/O and on
 * boot the app process is cold.
 */
class ReminderBootReceiver : BroadcastReceiver() {

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
                ReminderSyncWorker.enqueueOneTimeSync(context)
        }
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}

