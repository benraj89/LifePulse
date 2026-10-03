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
import com.vibecheck.lifepulse.data.local.dao.AccountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, openingMinor INTEGER NOT NULL)")
            db.execSQL("INSERT INTO accounts (id, name, openingMinor) VALUES (1, 'Cash', 0)")
            db.execSQL("ALTER TABLE categories ADD COLUMN kind TEXT NOT NULL DEFAULT 'EXPENSE'")
            db.execSQL("""CREATE TABLE expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER,
                amountMinor INTEGER NOT NULL, dateTimestamp INTEGER NOT NULL, note TEXT NOT NULL,
                type TEXT NOT NULL, accountId INTEGER NOT NULL, toAccountId INTEGER,
                person TEXT NOT NULL, loanId INTEGER,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(toAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(loanId) REFERENCES expenses(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""")
            db.execSQL("""INSERT INTO expenses_new (id, categoryId, amountMinor, dateTimestamp, note, type, accountId, toAccountId, person, loanId)
                SELECT id, categoryId, CAST(ROUND(amount * 100) AS INTEGER), dateTimestamp, note, 'EXPENSE', 1, NULL, '', NULL FROM expenses""")
            db.execSQL("DROP TABLE expenses")
            db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
            listOf("categoryId", "dateTimestamp", "accountId", "toAccountId", "loanId").forEach {
                db.execSQL("CREATE INDEX index_expenses_$it ON expenses ($it)")
            }
        }
    }

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

    /**
     * Adds an `isDeleted` flag to categories. Deleting a category now soft-deletes it (hidden
     * from pickers/lists) instead of hard-deleting the row, which previously cascade-deleted
     * every expense that referenced it.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifePulseDatabase =
        Room.databaseBuilder(context, LifePulseDatabase::class.java, LifePulseDatabase.NAME)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT OR IGNORE INTO accounts (id, name, openingMinor) VALUES (1, 'Cash', 0)")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideHabitDao(db: LifePulseDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideCategoryDao(db: LifePulseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideExpenseDao(db: LifePulseDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideAccountDao(db: LifePulseDatabase): AccountDao = db.accountDao()
}

