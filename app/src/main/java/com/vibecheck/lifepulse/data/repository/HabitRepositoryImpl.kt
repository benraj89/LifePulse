package com.vibecheck.lifepulse.data.repository

import com.vibecheck.lifepulse.core.DateUtils.toKey
import com.vibecheck.lifepulse.data.local.dao.HabitDao
import com.vibecheck.lifepulse.data.local.entity.HabitEntity
import com.vibecheck.lifepulse.domain.model.Habit
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.domain.repository.HabitRepository
import com.vibecheck.lifepulse.domain.usecase.CalculateStreakUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val calculateStreak: CalculateStreakUseCase
) : HabitRepository {

    override fun observeHabitsForDate(date: LocalDate): Flow<List<Habit>> =
        habitDao.observeHabitsWithStatus(date.toKey()).map { rows ->
            rows.map { row ->
                val dates = habitDao.getCompletionDates(row.habit.id)
                Habit(
                    id = row.habit.id,
                    title = row.habit.title,
                    frequency = HabitFrequency.fromRaw(row.habit.frequency),
                    createdAt = row.habit.createdAt,
                    completedToday = row.completedToday,
                    currentStreak = calculateStreak(dates, date),
                    reminderHour = row.habit.reminderHour,
                    reminderMinute = row.habit.reminderMinute,
                    reminderDayOfWeek = row.habit.reminderDayOfWeek,
                    reminderDayOfMonth = row.habit.reminderDayOfMonth
                )
            }
        }

    override fun observeTotalHabitCount(): Flow<Int> = habitDao.observeHabitCount()

    override fun observeCompletedCount(date: LocalDate): Flow<Int> =
        habitDao.observeCompletedCount(date.toKey())

    override suspend fun addHabit(
        title: String,
        frequency: HabitFrequency,
        reminderHour: Int?,
        reminderMinute: Int?,
        reminderDayOfWeek: Int?,
        reminderDayOfMonth: Int?
    ): Long =
        habitDao.insertHabit(
            HabitEntity(
                title = title.trim(),
                frequency = frequency.name,
                createdAt = System.currentTimeMillis(),
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderDayOfWeek = reminderDayOfWeek,
                reminderDayOfMonth = reminderDayOfMonth
            )
        )

    override suspend fun getHabitById(habitId: Long): Habit? =
        habitDao.getHabitById(habitId)?.toDomain()

    override suspend fun getAllHabitsOnce(): List<Habit> =
        habitDao.getAllHabitsOnce().map { it.toDomain() }

    private fun HabitEntity.toDomain(): Habit = Habit(
        id = id,
        title = title,
        frequency = HabitFrequency.fromRaw(frequency),
        createdAt = createdAt,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderDayOfWeek = reminderDayOfWeek,
        reminderDayOfMonth = reminderDayOfMonth
    )

    override suspend fun toggleHabit(habitId: Long, date: LocalDate) =
        habitDao.toggleCompletion(habitId, date.toKey())

    override suspend fun deleteHabit(habitId: Long) = habitDao.deleteHabitById(habitId)
}

