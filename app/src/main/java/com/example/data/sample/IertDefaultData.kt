package com.example.data.sample

import com.example.data.local.entity.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IertDefaultData {

    // Strictly real 7 subjects for IERT Mechanical (Tool Engg) 3rd Sem
    val defaultSubjects = listOf(
        SubjectEntity(
            id = 1,
            name = "Manufacturing Process",
            code = "ME-301",
            type = "theory",
            facultyName = "Er. R. P. Singh",
            strictness = "strict",
            colorHex = "#00E5FF",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 2,
            name = "Thermal Engineering",
            code = "ME-302",
            type = "theory",
            facultyName = "Dr. S. K. Mishra",
            strictness = "moderate",
            colorHex = "#38BDF8",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 3,
            name = "Workshop Practice",
            code = "ME-303W",
            type = "workshop",
            facultyName = "Er. V. K. Yadav",
            strictness = "strict",
            colorHex = "#F43F5E",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 4,
            name = "M/C Drawing 1",
            code = "ED-301",
            type = "lab",
            facultyName = "Er. N. K. Verma",
            strictness = "moderate",
            colorHex = "#C084FC",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 5,
            name = "Computer Lecture",
            code = "CS-301",
            type = "theory",
            facultyName = "Prof. Amit Patel",
            strictness = "chill",
            colorHex = "#10B981",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 6,
            name = "Material Science",
            code = "ME-304",
            type = "theory",
            facultyName = "Dr. P. K. Tripathi",
            strictness = "moderate",
            colorHex = "#F59E0B",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 7,
            name = "Computer Lab / Thermal Engineering Lab",
            code = "ME-305L",
            type = "lab",
            facultyName = "Er. S. C. Sharma",
            strictness = "strict",
            colorHex = "#818CF8",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        )
    )

    // Monday to Saturday schedule (08:00 AM - 04:00 PM with lunch break 12:30 - 01:00 PM)
    fun createDefaultSlots(): List<TimetableSlotEntity> {
        val slots = mutableListOf<TimetableSlotEntity>()
        var idCounter = 1L

        // Day 1: Monday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "08:00", endTime = "09:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "11:00", endTime = "12:00", roomNo = "LT-2", subjectId = 5))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "12:00", endTime = "12:30", roomNo = "LT-4", subjectId = 1))
        // 12:30 - 13:00 Lunch Break
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "13:00", endTime = "16:00", roomNo = "Machine Shop", subjectId = 3))

        // Day 2: Tuesday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "08:00", endTime = "09:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "11:00", endTime = "12:00", roomNo = "LT-2", subjectId = 5))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "12:00", endTime = "12:30", roomNo = "LT-4", subjectId = 2))
        // 12:30 - 13:00 Lunch Break
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "13:00", endTime = "16:00", roomNo = "Drafting Hall 3", subjectId = 4))

        // Day 3: Wednesday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "08:00", endTime = "09:00", roomNo = "LT-4", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "11:00", endTime = "12:00", roomNo = "LT-2", subjectId = 5))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "12:00", endTime = "12:30", roomNo = "LT-4", subjectId = 6))
        // 12:30 - 13:00 Lunch Break
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "13:00", endTime = "16:00", roomNo = "Thermal Lab", subjectId = 7))

        // Day 4: Thursday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "08:00", endTime = "09:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "11:00", endTime = "12:00", roomNo = "LT-2", subjectId = 5))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "12:00", endTime = "12:30", roomNo = "LT-4", subjectId = 1))
        // 12:30 - 13:00 Lunch Break
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "13:00", endTime = "16:00", roomNo = "Fitting Shop", subjectId = 3))

        // Day 5: Friday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "08:00", endTime = "09:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "11:00", endTime = "12:00", roomNo = "LT-2", subjectId = 5))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "12:00", endTime = "12:30", roomNo = "LT-2", subjectId = 5))
        // 12:30 - 13:00 Lunch Break
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "13:00", endTime = "16:00", roomNo = "Drafting Hall 3", subjectId = 4))

        // Day 6: Saturday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "08:00", endTime = "09:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "11:00", endTime = "12:30", roomNo = "CAD Lab", subjectId = 7))
        // 12:30 - 13:00 Lunch Break
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "13:00", endTime = "15:00", roomNo = "ME Seminar Hall", subjectId = 6))

        return slots
    }

    // Default clean submission deadlines for Machine Drawing sheets, Workshop jobs, and Lab files
    fun createDefaultAssessments(): List<AssessmentEntity> {
        val today = LocalDate.now()
        val dtf = DateTimeFormatter.ISO_LOCAL_DATE
        return listOf(
            AssessmentEntity(
                id = 1,
                subjectId = 4, // M/C Drawing 1
                type = "drawing_sheet",
                title = "Sheet 1: Orthographic Projections of Machine Parts",
                maxMarks = 10.0,
                obtainedMarks = null,
                status = "pending",
                dueDate = today.plusDays(3).format(dtf),
                dueTime = "16:00",
                syllabusCoveragePercent = 20
            ),
            AssessmentEntity(
                id = 2,
                subjectId = 3, // Workshop Practice
                type = "workshop_job",
                title = "Job 1: Mild Steel Step Turning on Lathe",
                maxMarks = 10.0,
                obtainedMarks = null,
                status = "pending",
                dueDate = today.plusDays(5).format(dtf),
                dueTime = "17:00",
                syllabusCoveragePercent = 25
            ),
            AssessmentEntity(
                id = 3,
                subjectId = 7, // Lab
                type = "lab_file",
                title = "Thermal Lab File: Study of 2-Stroke & 4-Stroke Engines",
                maxMarks = 10.0,
                obtainedMarks = null,
                status = "pending",
                dueDate = today.plusDays(7).format(dtf),
                dueTime = "15:00",
                syllabusCoveragePercent = 15
            ),
            AssessmentEntity(
                id = 4,
                subjectId = 4, // M/C Drawing 1
                type = "drawing_sheet",
                title = "Sheet 2: Fasteners & Thread Profiles (Square & Acme)",
                maxMarks = 10.0,
                obtainedMarks = null,
                status = "pending",
                dueDate = today.plusDays(12).format(dtf),
                dueTime = "16:00",
                syllabusCoveragePercent = 40
            ),
            AssessmentEntity(
                id = 5,
                subjectId = 1, // Manufacturing Process
                type = "sessional_1",
                title = "Sessional Exam 1 - Foundry & Casting Processes",
                maxMarks = 30.0,
                obtainedMarks = null,
                status = "pending",
                dueDate = today.plusDays(20).format(dtf),
                dueTime = "10:00",
                syllabusCoveragePercent = 50
            ),
            AssessmentEntity(
                id = 6,
                subjectId = 2, // Thermal Engineering
                type = "sessional_1",
                title = "Sessional Exam 1 - First & Second Laws of Thermo",
                maxMarks = 30.0,
                obtainedMarks = null,
                status = "pending",
                dueDate = today.plusDays(21).format(dtf),
                dueTime = "10:00",
                syllabusCoveragePercent = 50
            )
        )
    }

    fun createDefaultMedicalLeaves(): List<MedicalLeaveEntity> {
        return emptyList()
    }
}
