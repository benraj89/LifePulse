package com.vibecheck.lifepulse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.worker.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LifePulseApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var habitRepository: HabitRepository

    @Inject lateinit var expenseRepository: ExpenseRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Reconcile every habit's reminder work on process start. Idempotent: each habit's
        // unique work request is simply replaced, never duplicated.
        applicationScope.launch {
            habitRepository.getAllHabitsOnce()
                .filter { it.hasReminder }
                .forEach { reminderScheduler.scheduleHabitReminder(it) }
        }

        // Self-healing safety net: guarantees the built-in categories exist (and stay
        // flagged as non-deletable) even if an earlier build left the database without
        // them, regardless of which migration/onCreate path actually ran.
        applicationScope.launch {
            expenseRepository.ensureDefaultCategories()
        }
    }
}

