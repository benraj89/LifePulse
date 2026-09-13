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
    val isDefault: Boolean = false,
    /**
     * Soft-delete flag. When a user "deletes" a category we no longer remove the row so that
     * past expenses referencing it keep their name/color instead of being cascade-deleted.
     * Deleted categories are hidden from the category picker/list.
     */
    val isDeleted: Boolean = false
)

