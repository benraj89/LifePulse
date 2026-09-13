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
     * Streams every habit joined with a flag telling whether it was completed on [date].
     * Emits again automatically whenever habits OR habit_logs change.
     */
    @Transaction
    @Query(
        """
        SELECT h.*, 
               (SELECT COUNT(*) FROM habit_logs l 
                 WHERE l.habitId = h.id AND l.completedDate = :date) > 0 AS completedToday
        FROM habits h
        ORDER BY h.createdAt DESC
        """
    )
    fun observeHabitsWithStatus(date: String): Flow<List<HabitWithTodayStatus>>

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

    /** Idempotent check/uncheck of a habit for a given day. */
    @Transaction
    suspend fun toggleCompletion(habitId: Long, date: String) {
        if (isCompleted(habitId, date) > 0) {
            deleteLog(habitId, date)
        } else {
            insertLog(HabitLogEntity(habitId = habitId, completedDate = date))
        }
    }
}

