package com.vibecheck.lifepulse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["habitId", "completedDate"], unique = true)
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    /** "YYYY-MM-DD" */
    val completedDate: String
)

