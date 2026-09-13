package com.vibecheck.lifepulse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["dateTimestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val categoryId: Long,
    val amount: Double,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

