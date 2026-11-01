package com.vibecheck.lifepulse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.ui.navigation.LifePulseNavHost
import com.vibecheck.lifepulse.ui.expenses.CurrencyGate
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import com.vibecheck.lifepulse.worker.ReminderSyncWorker
import com.vibecheck.lifepulse.update.InAppUpdateController
import com.vibecheck.lifepulse.update.UpdateSnackbarHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var appUpdates: InAppUpdateController? = null
    private var notificationPermissionPending = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionPending = false
            // Reminders may have been suppressed while the permission was missing; re-sync so the
            // next alarm is armed as soon as the user says yes.
            if (granted) ReminderSyncWorker.enqueueOneTimeSync(this)
            appUpdates?.checkForUpdates()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val updates = InAppUpdateController(this, canPrompt = { !notificationPermissionPending })
        appUpdates = updates
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            NeoBrutalismTheme {
                CurrencyGate {
                    DisposableEffect(updates) {
                        updates.setUiReady(true)
                        onDispose { updates.setUiReady(false) }
                    }
                    val updateNotice by updates.notice.collectAsStateWithLifecycle()
                    LifePulseNavHost(modifier = Modifier.fillMaxSize(), snackbarHost = {
                        UpdateSnackbarHost(updateNotice, updates::completeUpdate, updates::dismissNotice)
                    })
                }
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
            notificationPermissionPending = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

}
