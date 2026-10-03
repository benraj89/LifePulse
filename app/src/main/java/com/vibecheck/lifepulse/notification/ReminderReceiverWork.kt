package com.vibecheck.lifepulse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.vibecheck.lifepulse.worker.ReminderSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Keeps receiver work alive and awake, with bounded execution and recovery on failure. */
internal fun BroadcastReceiver.runReminderWork(
    context: Context,
    tag: String,
    syncAfterwards: Boolean = false,
    work: suspend () -> Unit
) {
    val pendingResult = goAsync()
    val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LifePulse:$tag")
    try {
        wakeLock.acquire(10_000L)
    } catch (e: Exception) {
        pendingResult.finish()
        throw e
    }
    CoroutineScope(Dispatchers.IO).launch {
        var needsSync = syncAfterwards
        try {
            withTimeout(8_000L) { work() }
        } catch (e: Exception) {
            Log.e(tag, "Reminder receiver work failed; queuing recovery", e)
            needsSync = true
        } finally {
            try {
                if (needsSync) ReminderSyncWorker.enqueueOneTimeSync(context)
            } finally {
                try {
                    if (wakeLock.isHeld) wakeLock.release()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
