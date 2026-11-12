package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RescheduleOverrideEntity — Single-Day Time Shift Override
 * Prepones or postpones a recurring timetable slot on a specific date only.
 * Leaves master recurring weekly timetable completely intact.
 */
@Entity(tableName = "reschedule_overrides")
data class RescheduleOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val slotId: Long,
    val date: String, // "YYYY-MM-DD"
    val newStartTime: String, // "10:00"
    val newEndTime: String, // "11:00"
    val newRoomNo: String? = null
)
