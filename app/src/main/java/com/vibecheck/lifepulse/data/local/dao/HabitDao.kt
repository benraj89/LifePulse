package com.vibecheck.lifepulse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vibecheck.lifepulse.data.local.entity.HabitEntity
import com.vibecheck.lifepulse.data.local.entity.HabitLogEntity
import com.vibecheck.lifepulse.data.local.relation.HabitWithTodayStatus
import com.vibecheck.lifepulse.core.HabitPeriod
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    /** All habits once, used to reconcile reminders on app startup. */
    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    /**
     * Streams habits with completion in the current day, week, or month through [date].
     * Emits again automatically whenever habits OR habit_logs change.
     */
    @Transaction
    @Query(
        """
        SELECT h.*, 
               (SELECT COUNT(*) FROM habit_logs l 
                 WHERE l.habitId = h.id AND l.completedDate BETWEEN
                     CASE UPPER(h.frequency) WHEN 'WEEKLY' THEN :weekStart
                         WHEN 'MONTHLY' THEN :monthStart ELSE :date END
                     AND :date) > 0 AS completedToday
        FROM habits h
        ORDER BY h.createdAt DESC
        """
    )
    fun observeHabitsWithStatus(date: String, weekStart: String, monthStart: String): Flow<List<HabitWithTodayStatus>>

    @Query("SELECT COUNT(*) FROM habits")
    fun observeHabitCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_logs WHERE completedDate = :date")
    fun observeCompletedCount(date: String): Flow<Int>

    /** All completion dates for a habit, newest first — used for streak calculation. */
    @Query("SELECT completedDate FROM habit_logs WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun observeCompletionDates(habitId: Long): Flow<List<String>>

    @Query("SELECT completedDate FROM habit_logs WHERE habitId = :habitId ORDER BY completedDate DESC")
    suspend fun getCompletionDates(habitId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteLog(habitId: Long, date: String)

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND completedDate = :date")
    suspend fun isCompleted(habitId: Long, date: String): Int

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND completedDate BETWEEN :start AND :end")
    suspend fun completionsInPeriod(habitId: Long, start: String, end: String): Int

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND completedDate BETWEEN :start AND :end")
    suspend fun deletePeriodLogs(habitId: Long, start: String, end: String)

    /** Transactional check/uncheck of a habit for its current calendar period. */
    @Transaction
    suspend fun toggleCompletion(habitId: Long, date: String) {
        val habit = getHabitById(habitId) ?: return
        val frequency = HabitFrequency.fromRaw(habit.frequency)
        val start = HabitPeriod.start(LocalDate.parse(date), frequency).toString()
        if (completionsInPeriod(habitId, start, date) > 0) {
            // Clear legacy multiple logs too, so unchecking clears the entire current period.
            deletePeriodLogs(habitId, start, date)
        } else {
            insertLog(HabitLogEntity(habitId = habitId, completedDate = date))
        }
    }
}

