package com.example.data.sample

import com.example.data.local.entity.*

object IertDefaultData {
    val defaultSubjects = listOf(
        SubjectEntity(
            id = 1,
            name = "Manufacturing Process",
            code = "ME-301",
            type = "theory",
            facultyName = "Faculty In-Charge",
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
            facultyName = "Faculty In-Charge",
            strictness = "strict",
            colorHex = "#38BDF8",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 3,
            name = "Workshop Practice",
            code = "ME-303W",
            type = "workshop",
            facultyName = "Workshop Superintendent",
            strictness = "strict",
            colorHex = "#F59E0B",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 4,
            name = "M/C Drawing 1",
            code = "ED-301",
            type = "lab",
            facultyName = "Faculty In-Charge",
            strictness = "moderate",
            colorHex = "#818CF8",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 5,
            name = "Computer Lecture",
            code = "CS-301",
            type = "theory",
            facultyName = "Faculty In-Charge",
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
            facultyName = "Faculty In-Charge",
            strictness = "moderate",
            colorHex = "#C084FC",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        ),
        SubjectEntity(
            id = 7,
            name = "Computer Lab / Thermal Engg Lab",
            code = "ME-305L",
            type = "lab",
            facultyName = "Lab In-Charge",
            strictness = "strict",
            colorHex = "#F43F5E",
            reconciledAttendedOffset = 0,
            reconciledTotalOffset = 0
        )
    )

    fun createDefaultSlots(): List<TimetableSlotEntity> {
        val slots = mutableListOf<TimetableSlotEntity>()
        var idCounter = 1L

        // Day 1: Monday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "09:30", endTime = "11:00", roomNo = "LT-Class", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "11:00", endTime = "12:30", roomNo = "LT-Class", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 1, startTime = "13:00", endTime = "16:00", roomNo = "Workshop", subjectId = 3))

        // Day 2: Tuesday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "08:00", endTime = "12:30", roomNo = "Drawing Hall", subjectId = 4))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 2, startTime = "13:00", endTime = "14:30", roomNo = "Comp Room", subjectId = 5))

        // Day 3: Wednesday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "08:00", endTime = "09:30", roomNo = "LT-Class", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "09:30", endTime = "11:00", roomNo = "LT-Class", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "11:00", endTime = "12:30", roomNo = "LT-Class", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 3, startTime = "13:00", endTime = "16:00", roomNo = "Lab/Shop", subjectId = 7))

        // Day 4: Thursday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "09:30", endTime = "12:30", roomNo = "Drawing Hall", subjectId = 4))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 4, startTime = "13:00", endTime = "14:30", roomNo = "Comp Room", subjectId = 5))

        // Day 5: Friday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "08:00", endTime = "09:30", roomNo = "LT-Class", subjectId = 6))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "09:30", endTime = "11:00", roomNo = "LT-Class", subjectId = 1))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "11:00", endTime = "12:30", roomNo = "LT-Class", subjectId = 2))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 5, startTime = "13:00", endTime = "16:00", roomNo = "Workshop", subjectId = 3))

        // Day 6: Saturday
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "09:30", endTime = "12:30", roomNo = "Drawing Hall", subjectId = 4))
        slots.add(TimetableSlotEntity(id = idCounter++, dayOfWeek = 6, startTime = "13:00", endTime = "14:30", roomNo = "LT-Class", subjectId = 6))

        return slots
    }

    fun createDefaultAssessments(): List<AssessmentEntity> = emptyList()
    fun createDefaultMedicalLeaves(): List<MedicalLeaveEntity> = emptyList()
}
