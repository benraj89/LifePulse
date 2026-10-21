package com.vibecheck.lifepulse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibecheck.lifepulse.data.local.dao.CategoryDao
import com.vibecheck.lifepulse.data.local.dao.ExpenseDao
import com.vibecheck.lifepulse.data.local.dao.HabitDao
import com.vibecheck.lifepulse.data.local.dao.AccountDao
import com.vibecheck.lifepulse.data.local.entity.AccountEntity
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import com.vibecheck.lifepulse.data.local.entity.ExpenseEntity
import com.vibecheck.lifepulse.data.local.entity.HabitEntity
import com.vibecheck.lifepulse.data.local.entity.HabitLogEntity

@Database(
    entities = [
        HabitEntity::class,
        AccountEntity::class,
        HabitLogEntity::class,
        CategoryEntity::class,
        ExpenseEntity::class,
        com.vibecheck.lifepulse.data.local.entity.FinanceSettingsEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class LifePulseDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun backupDao(): com.vibecheck.lifepulse.data.local.dao.BackupDao
    abstract fun financeSettingsDao(): com.vibecheck.lifepulse.data.local.dao.FinanceSettingsDao

    companion object {
        const val NAME = "lifepulse.db"
    }
}

