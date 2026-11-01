package com.vibecheck.lifepulse.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vibecheck.lifepulse.R
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifePulseDatabase =
        Room.databaseBuilder(context, LifePulseDatabase::class.java, LifePulseDatabase.NAME)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT OR IGNORE INTO accounts (id, name, openingMinor) VALUES (1, ?, 0)",
                        arrayOf(context.getString(R.string.default_account_cash)))
                    // Seed a few default expense categories on first launch. These are flagged
                    // isDefault = 1 so the UI can prevent the user from deleting them.
                    DefaultCategories.all(context).forEach { (name, color) ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO categories (name, colorHex, isDefault, isDeleted, kind) VALUES (?, ?, 1, 0, 'EXPENSE')",
                            arrayOf(name, color)
                        )
                    }
                }
            })
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

