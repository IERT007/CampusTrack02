package com.example.data.sample

import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.HolidayRangeEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import java.time.LocalDate

object IertDefaultData {

    val defaultSubjects = listOf(
        SubjectEntity(
            id = 1,
            name = "Manufacturing Process",
            code = "ME-301",
            type = "theory",
            facultyName = "Er. R. P. Singh",
            strictness = "strict",
            colorHex = "#00E5FF"
        ),
        SubjectEntity(
            id = 2,
            name = "Thermal Engineering",
            code = "ME-302",
            type = "theory",
            facultyName = "Dr. S. K. Mishra",
            strictness = "moderate",
            colorHex = "#38BDF8"
        ),
        SubjectEntity(
            id = 3,
            name = "Strength of Materials",
            code = "ME-303",
            type = "theory",
            facultyName = "Er. A. K. Srivastava",
            strictness = "strict",
            colorHex = "#10B981"
        ),
        SubjectEntity(
            id = 4,
            name = "Fluid Mechanics & Hydraulics",
            code = "ME-304",
            type = "theory",
            facultyName = "Er. D. K. Tiwari",
            strictness = "moderate",
            colorHex = "#F59E0B"
        ),
        SubjectEntity(
            id = 5,
            name = "Materials Science & Metallurgy",
            code = "ME-305",
            type = "theory",
            facultyName = "Er. P. K. Pandey",
            strictness = "chill",
            colorHex = "#818CF8"
        ),
        SubjectEntity(
            id = 6,
            name = "Workshop Practice",
            code = "ME-303W",
            type = "workshop",
            facultyName = "Er. V. K. Yadav",
            strictness = "strict",
            colorHex = "#F43F5E"
        ),
        SubjectEntity(
            id = 7,
            name = "M/C Drawing 1",
            code = "ED-301",
            type = "lab",
            facultyName = "Er. N. K. Verma",
            strictness = "moderate",
            colorHex = "#C084FC"
        ),
        SubjectEntity(
            id = 8,
            name = "Thermal & Fluid Lab",
            code = "ME-302L",
            type = "lab",
            facultyName = "Dr. S. K. Mishra",
            strictness = "moderate",
            colorHex = "#2DD4BF"
        )
    )

    fun buildDefaultTimetable(): List<TimetableSlotEntity> {
        val slots = mutableListOf<TimetableSlotEntity>()
        var idCounter = 1L

        // Monday (1)
        slots.add(TimetableSlotEntity(idCounter++, 1, "09:00", "10:00", "L-3", 1))
        slots.add(TimetableSlotEntity(idCounter++, 1, "10:00", "11:00", "L-3", 2))
        slots.add(TimetableSlotEntity(idCounter++, 1, "11:00", "12:00", "L-3", 3))
        slots.add(TimetableSlotEntity(idCounter++, 1, "13:00", "14:00", "L-4", 4))
        slots.add(TimetableSlotEntity(idCounter++, 1, "14:00", "16:00", "Drawing Hall 2", 7))

        // Tuesday (2)
        slots.add(TimetableSlotEntity(idCounter++, 2, "09:00", "10:00", "L-3", 3))
        slots.add(TimetableSlotEntity(idCounter++, 2, "10:00", "11:00", "L-3", 4))
        slots.add(TimetableSlotEntity(idCounter++, 2, "11:00", "12:00", "L-3", 5))
        slots.add(TimetableSlotEntity(idCounter++, 2, "13:00", "14:00", "L-3", 1))
        slots.add(TimetableSlotEntity(idCounter++, 2, "14:00", "16:30", "Machine Shop", 6))

        // Wednesday (3)
        slots.add(TimetableSlotEntity(idCounter++, 3, "09:00", "10:00", "L-3", 2))
        slots.add(TimetableSlotEntity(idCounter++, 3, "10:00", "11:00", "L-3", 1))
        slots.add(TimetableSlotEntity(idCounter++, 3, "11:00", "12:00", "L-3", 4))
        slots.add(TimetableSlotEntity(idCounter++, 3, "13:00", "14:00", "L-4", 5))
        slots.add(TimetableSlotEntity(idCounter++, 3, "14:00", "16:00", "Thermal Lab", 8))

        // Thursday (4)
        slots.add(TimetableSlotEntity(idCounter++, 4, "09:00", "10:00", "L-3", 4))
        slots.add(TimetableSlotEntity(idCounter++, 4, "10:00", "11:00", "L-3", 3))
        slots.add(TimetableSlotEntity(idCounter++, 4, "11:00", "12:00", "L-3", 2))
        slots.add(TimetableSlotEntity(idCounter++, 4, "13:00", "14:00", "L-3", 5))
        slots.add(TimetableSlotEntity(idCounter++, 4, "14:00", "16:00", "Drawing Hall 2", 7))

        // Friday (5)
        slots.add(TimetableSlotEntity(idCounter++, 5, "09:00", "10:00", "L-3", 5))
        slots.add(TimetableSlotEntity(idCounter++, 5, "10:00", "11:00", "L-3", 1))
        slots.add(TimetableSlotEntity(idCounter++, 5, "11:00", "12:00", "L-3", 3))
        slots.add(TimetableSlotEntity(idCounter++, 5, "13:00", "14:00", "L-3", 2))
        slots.add(TimetableSlotEntity(idCounter++, 5, "14:00", "16:30", "Fitting & Welding Shop", 6))

        // Saturday (6)
        slots.add(TimetableSlotEntity(idCounter++, 6, "09:00", "10:00", "LT-4", 1))
        slots.add(TimetableSlotEntity(idCounter++, 6, "10:00", "11:00", "LT-4", 6))
        slots.add(TimetableSlotEntity(idCounter++, 6, "11:00", "12:30", "CAD Lab", 7))
        slots.add(TimetableSlotEntity(idCounter++, 6, "13:00", "15:00", "ME Seminar Hall", 8))

        return slots
    }

    fun buildDefaultAssessments(): List<AssessmentEntity> {
        val today = LocalDate.now()
        return listOf(
            AssessmentEntity(
                id = 1,
                subjectId = 1,
                type = "SESSIONAL",
                title = "First Sessional Exam (ME-301)",
                maxMarks = 20.0,
                obtainedMarks = 16.5,
                status = "CHECKED",
                dueDate = today.minusDays(14).toString(),
                dueTime = "10:00",
                syllabusCoveragePercent = 40
            ),
            AssessmentEntity(
                id = 2,
                subjectId = 2,
                type = "CLASS_TEST",
                title = "Rankine & Otto Cycle Quiz",
                maxMarks = 10.0,
                obtainedMarks = 8.5,
                status = "CHECKED",
                dueDate = today.minusDays(7).toString(),
                dueTime = "11:00",
                syllabusCoveragePercent = 30
            ),
            AssessmentEntity(
                id = 3,
                subjectId = 7,
                type = "DRAWING_SHEET",
                title = "Sheet 04: Knuckle & Cotter Joint Assembly",
                maxMarks = 25.0,
                obtainedMarks = null,
                status = "UPCOMING",
                dueDate = today.plusDays(3).toString(),
                dueTime = "14:00",
                syllabusCoveragePercent = 55
            ),
            AssessmentEntity(
                id = 4,
                subjectId = 6,
                type = "LAB_RECORD",
                title = "Lathe & Shaper Job Submission",
                maxMarks = 25.0,
                obtainedMarks = 22.0,
                status = "CHECKED",
                dueDate = today.minusDays(3).toString(),
                dueTime = "15:00",
                syllabusCoveragePercent = 65
            )
        )
    }

    fun buildDefaultHolidays(): List<HolidayRangeEntity> {
        val year = LocalDate.now().year
        return listOf(
            HolidayRangeEntity(
                id = 1,
                title = "Maha Shivratri",
                startDate = "$year-02-26",
                endDate = "$year-02-26",
                type = "HOLIDAY",
                notes = "Institute Gazette Holiday"
            ),
            HolidayRangeEntity(
                id = 2,
                title = "Holi Recess",
                startDate = "$year-03-13",
                endDate = "$year-03-15",
                type = "HOLIDAY",
                notes = "Spring Festival Break"
            ),
            HolidayRangeEntity(
                id = 3,
                title = "Dr. Ambedkar Jayanti",
                startDate = "$year-04-14",
                endDate = "$year-04-14",
                type = "HOLIDAY",
                notes = "Gazette Holiday"
            )
        )
    }
}
