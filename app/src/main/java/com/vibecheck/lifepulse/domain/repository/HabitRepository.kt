package com.vibecheck.lifepulse.domain.repository

import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {

    /** Habits enriched with today's completion flag and their current streak. */
    fun observeHabitsForDate(date: LocalDate): Flow<List<Habit>>

    fun observeTotalHabitCount(): Flow<Int>

    fun observeCompletedCount(date: LocalDate): Flow<Int>

    suspend fun addHabit(
        title: String,
        frequency: HabitFrequency,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        reminderDayOfWeek: Int? = null,
        reminderDayOfMonth: Int? = null
    ): Long

    suspend fun getHabitById(habitId: Long): Habit?

    suspend fun getAllHabitsOnce(): List<Habit>

    suspend fun toggleHabit(habitId: Long, date: LocalDate)

    suspend fun deleteHabit(habitId: Long)
}

