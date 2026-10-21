package com.vibecheck.lifepulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_settings")
data class FinanceSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currencyCode: String?,
    val confirmed: Boolean
)
