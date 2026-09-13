package com.vibecheck.lifepulse.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vibecheck.lifepulse.MainActivity
import com.vibecheck.lifepulse.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place that owns the reminder notification channel and posts habit reminders.
 *
 * Channel creation is idempotent (`createNotificationChannel` is an upsert) and is done eagerly on
 * app start as well as defensively right before posting, so a notification can never be dropped
 * because the channel did not exist yet.
 */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs by lazy {
        context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
    }

    /**
     * Epoch-millis of the last occurrence that was already delivered (or deliberately skipped)
     * for [habitId]. Used by the catch-up logic to guarantee "exactly once" per occurrence.
     */
    fun lastHandledOccurrence(habitId: Long): Long = prefs.getLong(keyFor(habitId), 0L)

    fun markOccurrenceHandled(habitId: Long, occurrenceMillis: Long) {
        // commit(): this is called from receivers/workers whose process can die immediately after.
        prefs.edit().putLong(keyFor(habitId), occurrenceMillis).commit()
    }

    fun clearState(habitId: Long) {
        prefs.edit().remove(keyFor(habitId)).apply()
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // Do not recreate an existing channel: the user may have tweaked its settings and
        // recreating would not reset them anyway, but this avoids useless binder calls.
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            // HIGH so reminders can heads-up and make a sound. DEFAULT would be silent-ish on
            // many OEM skins and is easy to miss for a time-critical reminder.
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Posts the reminder for [habitId]. No-ops (without crashing) when notifications are off. */
    fun showHabitReminder(habitId: Long, habitTitle: String) {
        ensureChannel()
        if (!canPostNotifications()) return

        val notificationId = notificationIdFor(habitId)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = context.getString(R.string.reminder_habit_notification_message, habitTitle)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post — nothing to do.
        }
    }

    companion object {
        const val CHANNEL_ID = "lifepulse_habit_reminders"
        private const val NOTIFICATION_ID_BASE = 100_000

        private fun keyFor(habitId: Long) = "last_handled_$habitId"

        /**
         * Stable, collision-free notification id per habit. Using `habitId.toInt()` directly would
         * collide once row ids exceed Int range; hashing the Long keeps it stable and bounded.
         */
        fun notificationIdFor(habitId: Long): Int =
            NOTIFICATION_ID_BASE + (habitId % 1_000_000L).toInt()
    }
}



