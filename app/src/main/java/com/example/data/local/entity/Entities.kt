package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val type: String, // "theory", "lab", "workshop"
    val facultyName: String,
    val strictness: String, // "strict", "moderate", "chill"
    val colorHex: String,
    val reconciledAttendedOffset: Int = 0,
    val reconciledTotalOffset: Int = 0
)

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday
    val startTime: String, // "09:00"
    val endTime: String, // "10:00"
    val roomNo: String, // "LT-4", "Workshop A", "CAD Lab"
    val subjectId: Long
)

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val slotId: Long? = null,
    val subjectId: Long,
    val status: String, // "attended", "bunked", "cancelled_by_faculty", "college_off"
    val isProxy: Boolean = false,
    val isExtraClass: Boolean = false,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val type: String, // "sessional_1", "sessional_2", "sessional_3", "ct", "viva_internal", "viva_external", "assignment", "drawing_sheet", "workshop_job"
    val title: String,
    val maxMarks: Double,
    val obtainedMarks: Double? = null,
    val status: String, // "appeared", "missed", "cancelled", "submitted", "in_progress"
    val missedReason: String? = null,
    val dueDate: String, // "YYYY-MM-DD"
    val syllabusCoveragePercent: Int = 0
)

@Entity(tableName = "daily_day_status")
data class DailyDayStatusEntity(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD"
    val wentToCollege: Boolean = true,
    val leaveCategory: String = "none", // "none", "college_holiday", "personal_bunk", "sick_leave", "mass_bunk", "strike"
    val notes: String? = null
)

@Entity(tableName = "medical_leaves")
data class MedicalLeaveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: String,
    val endDate: String,
    val reason: String,
    val doctorName: String,
    val submittedTo: String, // "HOD Mechanical Engineering"
    val status: String, // "submitted", "approved", "pending", "rejected"
    val refNo: String,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
