package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ExtraSlotEntity — Date-Locked Single-Day Extra Classes
 * Strictly bound to an explicit ISO-date key (e.g. "2026-11-10").
 * NEVER writes into or modifies the recurring weekly TimetableSlotEntity template.
 */
@Entity(tableName = "extra_slots")
data class ExtraSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val subjectId: Long,
    val startTime: String, // "09:00"
    val endTime: String, // "10:00"
    val roomNo: String, // "LT-4", "Room 301"
    val tag: String = "Extra Class" // "Extra Class", "Theory", "Lab", "Tutorial"
)
