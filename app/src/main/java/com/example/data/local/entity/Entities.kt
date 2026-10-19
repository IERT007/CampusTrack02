package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val type: String = "theory", // "theory", "lab", "workshop"
    val facultyName: String = "",
    val strictness: String = "moderate", // "strict", "moderate", "chill"
    val colorHex: String = "#00E5FF",
    val reconciledAttendedOffset: Int = 0,
    val reconciledTotalOffset: Int = 0
) {
    val roomNumber: String get() = if (type.equals("lab", ignoreCase = true)) "Comp Lab" else "Room 301"
    val targetPercentage: Double get() = 75.0
    val attendedOffset: Int get() = reconciledAttendedOffset
    val totalOffset: Int get() = reconciledTotalOffset
    val credits: Int get() = if (type.equals("lab", ignoreCase = true)) 2 else 4
}

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startTime: String, // "09:00"
    val endTime: String,   // "10:00"
    val roomNo: String,
    val subjectId: Long
) {
    val slotId: Long get() = id
    val subjectName: String get() = ""
    val roomNumber: String? get() = roomNo.ifBlank { null }
}

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val slotId: Long? = null,
    val subjectId: Long,
    val status: String, // "PRESENT", "ABSENT", "FACULTY_CANCELLED", "COLLEGE_OFF", "MEDICAL_LEAVE"
    val isProxy: Boolean = false,
    val isExtraClass: Boolean = false,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val type: String, // "SESSIONAL", "CLASS_TEST", "QUIZ", "LAB_RECORD", "DRAWING_SHEET"
    val title: String,
    val maxMarks: Double = 20.0,
    val obtainedMarks: Double? = null,
    val status: String = "UPCOMING", // "UPCOMING", "COMPLETED", "MISSED", "SUBMITTED", "CHECKED"
    val missedReason: String? = null,
    val dueDate: String, // YYYY-MM-DD
    val dueTime: String = "10:00",
    val syllabusCoveragePercent: Int = 0
) {
    val category: String get() = type
}

@Entity(tableName = "daily_day_status")
data class DailyDayStatusEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val wentToCollege: Boolean = true,
    val leaveCategory: String = "NONE", // "NONE", "MASS_BUNK", "PERSONAL_LEAVE", "MEDICAL", "HOLIDAY"
    val notes: String? = null
) {
    val status: String get() = leaveCategory
}

@Entity(tableName = "medical_leaves")
data class MedicalLeaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,
    val endDate: String,
    val reason: String,
    val doctorName: String = "",
    val submittedTo: String = "HOD Mechanical",
    val status: String = "PENDING", // "PENDING", "SUBMITTED", "APPROVED"
    val refNo: String = "",
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val classesMissedCount: Int get() = 4
}

@Entity(tableName = "holiday_ranges")
data class HolidayRangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startDate: String, // YYYY-MM-DD
    val endDate: String,   // YYYY-MM-DD
    val type: String = "HOLIDAY", // "HOLIDAY", "SEMESTER_BREAK", "EXAM_PREP"
    val notes: String? = null
)

@Entity(tableName = "academic_tasks")
data class AcademicTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val category: String = "GENERAL", // "SHEET", "ASSIGNMENT", "LAB_MANUAL", "GENERAL"
    val dueDate: String, // YYYY-MM-DD
    val isCompleted: Boolean = false,
    val subjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "LECTURE",
    val subjectCode: String? = null,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "test_records")
data class TestRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val date: String, // YYYY-MM-DD
    val slotId: Long? = null,
    val testName: String,
    val marksObtained: Double,
    val maxMarks: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val title: String get() = testName
}

@Entity(tableName = "custom_activities")
data class CustomActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val title: String,
    val startTime: String, // "14:00"
    val endTime: String,   // "15:00"
    val location: String = "",
    val status: String = "SCHEDULED", // "SCHEDULED", "PRESENT", "ABSENT"
    val countsTowardsAttendance: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val category: String get() = location.ifBlank { "ACTIVITY" }
}
