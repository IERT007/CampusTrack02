package com.example.data.sample

import com.example.data.local.entity.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IertDefaultData {

    val defaultSubjects = listOf(
        SubjectEntity(
            id = 1,
            name = "Thermodynamics & Heat Engines",
            code = "ME-301",
            type = "theory",
            facultyName = "Dr. S. K. Mishra",
            strictness = "strict",
            colorHex = "#00E5FF",
            reconciledAttendedOffset = 18,
            reconciledTotalOffset = 22
        ),
        SubjectEntity(
            id = 2,
            name = "Manufacturing Tech & Machine Tools",
            code = "ME-302",
            type = "theory",
            facultyName = "Er. R. P. Singh",
            strictness = "moderate",
            colorHex = "#38BDF8",
            reconciledAttendedOffset = 20,
            reconciledTotalOffset = 24
        ),
        SubjectEntity(
            id = 3,
            name = "Fluid Mechanics & Hydraulic Machines",
            code = "ME-303",
            type = "theory",
            facultyName = "Prof. A. K. Srivastava",
            strictness = "strict",
            colorHex = "#818CF8",
            reconciledAttendedOffset = 14,
            reconciledTotalOffset = 21
        ),
        SubjectEntity(
            id = 4,
            name = "Workshop Practice (Lathe & Fitting)",
            code = "ME-304",
            type = "workshop",
            facultyName = "Er. V. K. Yadav",
            strictness = "strict",
            colorHex = "#F43F5E",
            reconciledAttendedOffset = 11,
            reconciledTotalOffset = 12
        ),
        SubjectEntity(
            id = 5,
            name = "Machine Drawing & CAD Drafting",
            code = "ED-301",
            type = "lab",
            facultyName = "Er. N. K. Verma",
            strictness = "moderate",
            colorHex = "#C084FC",
            reconciledAttendedOffset = 10,
            reconciledTotalOffset = 12
        ),
        SubjectEntity(
            id = 6,
            name = "Applied Mathematics - II",
            code = "AM-301",
            type = "theory",
            facultyName = "Dr. P. K. Tripathi",
            strictness = "chill",
            colorHex = "#10B981",
            reconciledAttendedOffset = 22,
            reconciledTotalOffset = 24
        ),
        SubjectEntity(
            id = 7,
            name = "Industrial Management & Entrepreneurship",
            code = "HU-301",
            type = "theory",
            facultyName = "Mrs. R. Gupta",
            strictness = "chill",
            colorHex = "#F59E0B",
            reconciledAttendedOffset = 12,
            reconciledTotalOffset = 14
        )
    )

    fun createDefaultSlots(): List<TimetableSlotEntity> {
        val slots = mutableListOf<TimetableSlotEntity>()
        var idCounter = 1L

        // Day 1: Monday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "11:15", endTime = "12:15", roomNo = "LT-2", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "12:15", endTime = "01:15", roomNo = "LT-4", subjectId = 3))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "14:00", endTime = "16:00", roomNo = "CAD Lab", subjectId = 5))

        // Day 2: Tuesday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 3))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "11:15", endTime = "12:15", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "12:15", endTime = "01:15", roomNo = "LT-1", subjectId = 7))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "14:00", endTime = "17:00", roomNo = "Fitting Shop", subjectId = 4))

        // Day 3: Wednesday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "09:00", endTime = "10:00", roomNo = "LT-2", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 3))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "11:15", endTime = "12:15", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "12:15", endTime = "01:15", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "14:00", endTime = "16:00", roomNo = "Drawing Hall-3", subjectId = 5))

        // Day 4: Thursday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "10:00", endTime = "11:00", roomNo = "LT-2", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "11:15", endTime = "12:15", roomNo = "LT-4", subjectId = 3))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "12:15", endTime = "01:15", roomNo = "LT-1", subjectId = 7))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "14:00", endTime = "17:00", roomNo = "Machine Shop", subjectId = 4))

        // Day 5: Friday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "10:00", endTime = "11:00", roomNo = "LT-4", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "11:15", endTime = "12:15", roomNo = "LT-2", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "12:15", endTime = "01:15", roomNo = "LT-4", subjectId = 3))

        // Day 6: Saturday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "09:00", endTime = "10:00", roomNo = "LT-4", subjectId = 3))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "10:00", endTime = "11:00", roomNo = "LT-1", subjectId = 7))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "11:15", endTime = "12:15", roomNo = "LT-4", subjectId = 1))

        return slots
    }

    fun createDefaultAssessments(): List<AssessmentEntity> {
        val today = LocalDate.now()
        val dtf = DateTimeFormatter.ISO_LOCAL_DATE
        return listOf(
            AssessmentEntity(
                id = 1,
                subjectId = 1,
                type = "sessional_1",
                title = "Sessional Exam 1 - First & Second Laws of Thermo",
                maxMarks = 30.0,
                obtainedMarks = 24.5,
                status = "appeared",
                dueDate = today.minusDays(18).format(dtf),
                syllabusCoveragePercent = 100
            ),
            AssessmentEntity(
                id = 2,
                subjectId = 3,
                type = "sessional_1",
                title = "Sessional Exam 1 - Fluid Kinematics & Bernoulli",
                maxMarks = 30.0,
                obtainedMarks = 19.0,
                status = "appeared",
                dueDate = today.minusDays(16).format(dtf),
                syllabusCoveragePercent = 100
            ),
            AssessmentEntity(
                id = 3,
                subjectId = 2,
                type = "sessional_1",
                title = "Sessional Exam 1 - Lathe & Shaper Mechanisms",
                maxMarks = 30.0,
                obtainedMarks = 26.0,
                status = "appeared",
                dueDate = today.minusDays(14).format(dtf),
                syllabusCoveragePercent = 100
            ),
            AssessmentEntity(
                id = 4,
                subjectId = 1,
                type = "sessional_2",
                title = "Sessional Exam 2 - Steam Turbines & Rankine Cycle",
                maxMarks = 30.0,
                obtainedMarks = null,
                status = "in_progress",
                dueDate = today.plusDays(12).format(dtf),
                syllabusCoveragePercent = 65
            ),
            AssessmentEntity(
                id = 5,
                subjectId = 3,
                type = "sessional_2",
                title = "Sessional Exam 2 - Dimensional Analysis & Boundary Layer",
                maxMarks = 30.0,
                obtainedMarks = null,
                status = "in_progress",
                dueDate = today.plusDays(14).format(dtf),
                syllabusCoveragePercent = 50
            ),
            AssessmentEntity(
                id = 6,
                subjectId = 1,
                type = "ct",
                title = "Surprise Class Test: Entropy & Carnot Efficiency",
                maxMarks = 10.0,
                obtainedMarks = 8.5,
                status = "appeared",
                dueDate = today.minusDays(6).format(dtf)
            ),
            AssessmentEntity(
                id = 7,
                subjectId = 5,
                type = "drawing_sheet",
                title = "Sheet #4: Knuckle & Gib-Cotter Assembly",
                maxMarks = 25.0,
                obtainedMarks = null,
                status = "in_progress",
                dueDate = today.plusDays(4).format(dtf)
            ),
            AssessmentEntity(
                id = 8,
                subjectId = 4,
                type = "workshop_job",
                title = "Workshop Job: Mild Steel Single V-Butt Welding",
                maxMarks = 20.0,
                obtainedMarks = 17.5,
                status = "submitted",
                dueDate = today.minusDays(5).format(dtf)
            ),
            AssessmentEntity(
                id = 9,
                subjectId = 4,
                type = "workshop_job",
                title = "Workshop Job: Step Turning & Taper Turning on Lathe",
                maxMarks = 20.0,
                obtainedMarks = null,
                status = "in_progress",
                dueDate = today.plusDays(6).format(dtf)
            )
        )
    }

    fun createDefaultMedicalLeaves(): List<MedicalLeaveEntity> {
        val today = LocalDate.now()
        val dtf = DateTimeFormatter.ISO_LOCAL_DATE
        return listOf(
            MedicalLeaveEntity(
                id = 1,
                startDate = today.minusDays(20).format(dtf),
                endDate = today.minusDays(18).format(dtf),
                reason = "Acute Viral Fever & Throat Infection",
                doctorName = "Dr. R. C. Maurya, Tej Bahadur Sapru Hospital (Beli), Prayagraj",
                submittedTo = "HOD Mechanical Engineering, IERT",
                status = "approved",
                refNo = "IERT/ME/MED/2026/089",
                notes = "Countersigned by proctor; 3 days attendance benefit credited."
            )
        )
    }
}
