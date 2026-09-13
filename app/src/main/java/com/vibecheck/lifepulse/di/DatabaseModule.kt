package com.vibecheck.lifepulse.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vibecheck.lifepulse.data.local.DefaultCategories
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.dao.CategoryDao
import com.vibecheck.lifepulse.data.local.dao.ExpenseDao
import com.vibecheck.lifepulse.data.local.dao.HabitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Adds the `isDefault` column introduced for category deletion support. Uses a real
     * migration (instead of destructive fallback) so existing habits/expenses/categories
     * survive the upgrade. Any pre-existing category whose name matches one of the built-in
     * defaults is retroactively flagged as isDefault = 1.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
            DefaultCategories.ALL.forEach { (name, _) ->
                db.execSQL(
                    "UPDATE categories SET isDefault = 1 WHERE name = ? COLLATE NOCASE",
                    arrayOf(name)
                )
            }
        }
    }

    /**
     * Adds per-habit reminder configuration: a fire time (hour/minute) plus an optional
     * day-of-week (weekly habits) or day-of-month (monthly habits). All columns are nullable
     * so existing habits simply have reminders disabled after the upgrade.
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE habits ADD COLUMN reminderHour INTEGER")
            db.execSQL("ALTER TABLE habits ADD COLUMN reminderMinute INTEGER")
            db.execSQL("ALTER TABLE habits ADD COLUMN reminderDayOfWeek INTEGER")
            db.execSQL("ALTER TABLE habits ADD COLUMN reminderDayOfMonth INTEGER")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifePulseDatabase =
        Room.databaseBuilder(context, LifePulseDatabase::class.java, LifePulseDatabase.NAME)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed a few default expense categories on first launch. These are flagged
                    // isDefault = 1 so the UI can prevent the user from deleting them.
                    DefaultCategories.ALL.forEach { (name, color) ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO categories (name, colorHex, isDefault) VALUES (?, ?, 1)",
                            arrayOf(name, color)
                        )
                    }
                }
            })
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHabitDao(db: LifePulseDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideCategoryDao(db: LifePulseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideExpenseDao(db: LifePulseDatabase): ExpenseDao = db.expenseDao()
}

