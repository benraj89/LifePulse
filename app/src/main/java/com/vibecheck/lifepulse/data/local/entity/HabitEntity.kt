package com.vibecheck.lifepulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    /** Stored as the name of [com.vibecheck.lifepulse.domain.model.HabitFrequency]. */
    val frequency: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Hour (0-23) the reminder should fire, or null when no reminder is configured. */
    val reminderHour: Int? = null,
    /** Minute (0-59) the reminder should fire. */
    val reminderMinute: Int? = null,
    /** ISO day-of-week (1=Monday..7=Sunday), only used for weekly habits. */
    val reminderDayOfWeek: Int? = null,
    /** Day of month (1-31), only used for monthly habits. */
    val reminderDayOfMonth: Int? = null
)

