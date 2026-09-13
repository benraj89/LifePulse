package com.vibecheck.lifepulse

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.vibecheck.lifepulse.ui.navigation.LifePulseNavHost
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import com.vibecheck.lifepulse.worker.ReminderSyncWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Reminders may have been suppressed while the permission was missing; re-sync so the
            // next alarm is armed as soon as the user says yes.
            if (granted) ReminderSyncWorker.enqueueOneTimeSync(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
        setContent {
            NeoBrutalismTheme {
                LifePulseNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Cheap self-heal on every foreground: repairs alarms killed by OEM battery managers or a
        // force-stop, and delivers anything missed while the app was away.
        ReminderSyncWorker.enqueueOneTimeSync(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * On API 31/32 `SCHEDULE_EXACT_ALARM` is granted by default but the user (or an OEM cleaner)
     * can revoke it, which silently degrades reminders to inexact. We send them to the system
     * screen at most once so we never nag. On API 33+ `USE_EXACT_ALARM` is auto-granted, so
     * nothing to do.
     */
    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
        ) return

        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        if (alarmManager.canScheduleExactAlarms()) return

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_EXACT_ALARM_ASKED, false)) return
        prefs.edit().putBoolean(KEY_EXACT_ALARM_ASKED, true).apply()

        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }

    companion object {
        private const val PREFS = "reminder_state"
        private const val KEY_EXACT_ALARM_ASKED = "exact_alarm_asked"
    }
}