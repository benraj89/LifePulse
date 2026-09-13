package com.vibecheck.lifepulse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import com.vibecheck.lifepulse.notification.ReminderNotifier
import com.vibecheck.lifepulse.worker.ReminderSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LifePulseApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reminderNotifier: ReminderNotifier

    @Inject lateinit var expenseRepository: ExpenseRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // The channel must exist before the very first notification is posted, and creating it
        // eagerly also makes the app's notification settings visible to the user right away.
        reminderNotifier.ensureChannel()

        // Re-arm every habit alarm and deliver anything missed while the app was not running.
        // Idempotent: alarms are replaced, never duplicated.
        ReminderSyncWorker.enqueueOneTimeSync(this)

        // Heartbeat that keeps alarms alive even if the user never opens the app again
        // (OEM battery managers, force-stop, app-update, ...).
        ReminderSyncWorker.enqueuePeriodicSync(this)

        // Self-healing safety net: guarantees the built-in categories exist (and stay
        // flagged as non-deletable) even if an earlier build left the database without
        // them, regardless of which migration/onCreate path actually ran.
        applicationScope.launch {
            expenseRepository.ensureDefaultCategories()
        }
    }
}

