package com.vibecheck.lifepulse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: String,
    /** True for the built-in categories seeded on first launch; these cannot be deleted. */
    val isDefault: Boolean = false
)

