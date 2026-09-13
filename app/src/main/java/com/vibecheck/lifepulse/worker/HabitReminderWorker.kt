package com.vibecheck.lifepulse.worker

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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vibecheck.lifepulse.MainActivity
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Fires a single habit's reminder notification (skipping it if the habit was already completed
 * today), then re-enqueues itself for the habit's next occurrence so the reminder keeps
 * recurring according to the habit's own frequency/time/day configuration.
 */
@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong(KEY_HABIT_ID, -1L)
        if (habitId == -1L) return Result.success()

        return try {
            val habit = habitRepository.getHabitById(habitId)
            if (habit != null && habit.hasReminder) {
                val today = DateUtils.today()
                val completed = habitRepository.observeHabitsForDate(today).first()
                    .firstOrNull { it.id == habitId }?.completedToday ?: false

                if (!completed) {
                    showNotification(
                        title = applicationContext.getString(R.string.reminder_notification_title),
                        message = applicationContext.getString(
                            R.string.reminder_habit_notification_message,
                            habit.title
                        ),
                        notificationId = NOTIFICATION_ID_BASE + habitId.toInt()
                    )
                }
                // Reschedule for the next occurrence so the reminder keeps recurring.
                reminderScheduler.scheduleHabitReminder(habit)
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val context = applicationContext
        createChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.reminder_channel_description) }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val CHANNEL_ID = "lifepulse_habit_reminders"
        private const val NOTIFICATION_ID_BASE = 100_000
    }
}




