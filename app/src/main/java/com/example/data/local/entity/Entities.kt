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
    val dayOfWeek: Int, // 1 = Monday ... 6 = Saturday
    val startTime: String, // "08:00"
    val endTime: String, // "09:00"
    val roomNo: String, // "LT-4", "Workshop A", "CAD Lab"
    val subjectId: Long
) {
    val slotId: Long get() = id
    val subjectName: String get() = "Lecture"
}

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
    val type: String, // "drawing_sheet", "workshop_job", "lab_file", "sessional_1", "sessional_2", "sessional_3", "ct", "viva_internal", "viva_external", "assignment"
    val title: String,
    val maxMarks: Double,
    val obtainedMarks: Double? = null,
    val status: String, // "pending", "in_progress", "submitted", "appeared", "missed", "cancelled"
    val missedReason: String? = null,
    val dueDate: String, // "YYYY-MM-DD"
    val dueTime: String = "17:00", // "HH:mm" target time
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

@Entity(tableName = "holiday_ranges")
data class HolidayRangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String, // "Mid-Semester Break", "Diwali Vacation", "Institutional Strike"
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String, // "YYYY-MM-DD"
    val type: String = "holiday", // "holiday", "strike", "semester_break", "mass_bunk"
    val notes: String? = null
)

@Entity(tableName = "academic_tasks")
data class AcademicTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val priority: String = "Medium", // "High", "Medium", "Low"
    val category: String = "Assignment", // "Assignment", "Sheet Work", "Exam Prep", "Miscellaneous"
    val dueDate: String, // "YYYY-MM-DD"
    val isCompleted: Boolean = false,
    val subjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
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

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General", // "Lecture", "Lab", "Drawing Sheet", "Viva", "Important", "General"
    val subjectCode: String? = null,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
