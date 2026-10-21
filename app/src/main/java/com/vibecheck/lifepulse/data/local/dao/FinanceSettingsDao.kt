package com.vibecheck.lifepulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibecheck.lifepulse.data.local.entity.FinanceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceSettingsDao {
    @Query("SELECT * FROM finance_settings WHERE id = 1")
    suspend fun get(): FinanceSettingsEntity?
    @Query("SELECT * FROM finance_settings WHERE id = 1")
    fun observe(): Flow<FinanceSettingsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: FinanceSettingsEntity)
    @Query("SELECT EXISTS(SELECT 1 FROM expenses) OR EXISTS(SELECT 1 FROM accounts WHERE openingMinor != 0)")
    suspend fun hasFinancialData(): Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM expenses) OR EXISTS(SELECT 1 FROM accounts WHERE openingMinor != 0)")
    fun observeHasFinancialData(): Flow<Boolean>
}
