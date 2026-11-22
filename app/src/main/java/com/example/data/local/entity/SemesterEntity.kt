package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SemesterEntity — Multi-Semester Academic Profile
 * Scopes subjects, timetable slots, attendance logs, extra slots, overrides,
 * assessments, and to-dos under an isolated semester profile.
 * Archived semesters become read-only to prevent accidental modifications.
 */
@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String,   // "YYYY-MM-DD"
    val isActive: Boolean = false,
    val isArchived: Boolean = false
)
