package com.vibecheck.lifepulse.domain.usecase

import android.content.Context
import android.util.Log
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.worker.ReminderScheduler
import com.vibecheck.lifepulse.worker.ReminderSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Accepted saves survive navigation and ViewModel destruction. */
@Singleton
class HabitActions @Inject constructor(
    private val repository: HabitRepository,
    private val scheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun add(
        title: String, frequency: HabitFrequency, hour: Int?, minute: Int?,
        dayOfWeek: Int?, dayOfMonth: Int?
    ) = scope.launch {
        if (title.isBlank()) return@launch
        try {
            val id = repository.addHabit(title, frequency, hour, minute, dayOfWeek, dayOfMonth)
            repository.getHabitById(id)?.let { scheduler.scheduleHabitReminder(it) }
        } catch (e: Exception) {
            Log.e("HabitActions", "Unable to finish saving habit and scheduling its reminder", e)
            ReminderSyncWorker.enqueueOneTimeSync(context)
        }
    }
}
