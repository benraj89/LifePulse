package com.vibecheck.lifepulse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibecheck.lifepulse.data.local.dao.CategoryDao
import com.vibecheck.lifepulse.data.local.dao.ExpenseDao
import com.vibecheck.lifepulse.data.local.dao.HabitDao
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import com.vibecheck.lifepulse.data.local.entity.ExpenseEntity
import com.vibecheck.lifepulse.data.local.entity.HabitEntity
import com.vibecheck.lifepulse.data.local.entity.HabitLogEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        CategoryEntity::class,
        ExpenseEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class LifePulseDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME = "lifepulse.db"
    }
}

