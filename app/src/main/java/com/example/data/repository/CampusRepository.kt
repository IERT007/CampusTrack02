package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.sample.IertDefaultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CampusRepository(private val db: AppDatabase) {

    val allSubjects: Flow<List<SubjectEntity>> = db.subjectDao().getAllSubjects()
    val allSlots: Flow<List<TimetableSlotEntity>> = db.timetableSlotDao().getAllSlots()
    val allLogs: Flow<List<AttendanceLogEntity>> = db.attendanceLogDao().getAllLogs()
    val allAssessments: Flow<List<AssessmentEntity>> = db.assessmentDao().getAllAssessments()
    val allDayStatuses: Flow<List<DailyDayStatusEntity>> = db.dailyDayStatusDao().getAllDayStatuses()
    val allMedicalLeaves: Flow<List<MedicalLeaveEntity>> = db.medicalLeaveDao().getAllMedicalLeaves()

    fun getLogsForDate(date: String): Flow<List<AttendanceLogEntity>> {
        return db.attendanceLogDao().getLogsForDate(date)
    }

    fun getDayStatus(date: String): Flow<DailyDayStatusEntity?> {
        return db.dailyDayStatusDao().getDayStatus(date)
    }

    suspend fun setSlotAttendance(
        date: String,
        slotId: Long?,
        subjectId: Long,
        status: String,
        isProxy: Boolean = false,
        isExtraClass: Boolean = false,
        notes: String? = null
    ) = withContext(Dispatchers.IO) {
        if (slotId != null) {
            db.attendanceLogDao().deleteLogForSlot(date, slotId)
        }
        val entity = AttendanceLogEntity(
            date = date,
            slotId = slotId,
            subjectId = subjectId,
            status = status,
            isProxy = isProxy,
            isExtraClass = isExtraClass,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        db.attendanceLogDao().insertLog(entity)
    }

    suspend fun markWholeDayPresent(date: String, slots: List<TimetableSlotEntity>) = withContext(Dispatchers.IO) {
        db.dailyDayStatusDao().insertOrUpdate(
            DailyDayStatusEntity(date = date, wentToCollege = true, leaveCategory = "none", notes = "Whole day marked present")
        )
        for (slot in slots) {
            setSlotAttendance(
                date = date,
                slotId = slot.id,
                subjectId = slot.subjectId,
                status = "attended",
                isProxy = false,
                isExtraClass = false,
                notes = "Auto bulk mark"
            )
        }
    }

    suspend fun markMassBunkOrOff(
        date: String,
        slots: List<TimetableSlotEntity>,
        category: String // "college_holiday", "mass_bunk", "strike"
    ) = withContext(Dispatchers.IO) {
        val statusString = if (category == "mass_bunk") "bunked" else "college_off"
        db.dailyDayStatusDao().insertOrUpdate(
            DailyDayStatusEntity(
                date = date,
                wentToCollege = false,
                leaveCategory = category,
                notes = if (category == "mass_bunk") "Department Mass Bunk" else "Institutional Closure / Strike"
            )
        )
        for (slot in slots) {
            setSlotAttendance(
                date = date,
                slotId = slot.id,
                subjectId = slot.subjectId,
                status = statusString,
                isProxy = false,
                isExtraClass = false,
                notes = category
            )
        }
    }

    suspend fun logAdHocExtraClass(
        date: String,
        subjectId: Long,
        status: String,
        isProxy: Boolean,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val entity = AttendanceLogEntity(
            date = date,
            slotId = null,
            subjectId = subjectId,
            status = status,
            isProxy = isProxy,
            isExtraClass = true,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        db.attendanceLogDao().insertLog(entity)
    }

    suspend fun reconcileSubject(
        subjectId: Long,
        officialAttended: Int,
        officialTotal: Int,
        currentAppAttended: Int,
        currentAppTotal: Int
    ) = withContext(Dispatchers.IO) {
        val attendedOffset = officialAttended - currentAppAttended
        val totalOffset = officialTotal - currentAppTotal
        db.subjectDao().updateOffsets(subjectId, attendedOffset, totalOffset)
    }

    suspend fun addSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.subjectDao().insertSubject(subject)
    }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.subjectDao().updateSubject(subject)
    }

    suspend fun deleteSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.subjectDao().deleteSubject(subject)
    }

    suspend fun addAssessment(assessment: AssessmentEntity) = withContext(Dispatchers.IO) {
        db.assessmentDao().insertAssessment(assessment)
    }

    suspend fun updateAssessment(assessment: AssessmentEntity) = withContext(Dispatchers.IO) {
        db.assessmentDao().updateAssessment(assessment)
    }

    suspend fun deleteAssessment(id: Long) = withContext(Dispatchers.IO) {
        db.assessmentDao().deleteAssessmentById(id)
    }

    suspend fun addMedicalLeave(leave: MedicalLeaveEntity) = withContext(Dispatchers.IO) {
        db.medicalLeaveDao().insertMedicalLeave(leave)
    }

    suspend fun updateMedicalLeave(leave: MedicalLeaveEntity) = withContext(Dispatchers.IO) {
        db.medicalLeaveDao().updateMedicalLeave(leave)
    }

    suspend fun deleteMedicalLeave(id: Long) = withContext(Dispatchers.IO) {
        db.medicalLeaveDao().deleteMedicalLeaveById(id)
    }

    suspend fun checkAndInitializeDefaultData() = withContext(Dispatchers.IO) {
        val currentSubjects = db.subjectDao().getAllSubjects().first()
        if (currentSubjects.isEmpty()) {
            preloadDefaultIertData()
        }
    }

    suspend fun preloadDefaultIertData() = withContext(Dispatchers.IO) {
        db.subjectDao().deleteAllSubjects()
        db.timetableSlotDao().deleteAllSlots()
        db.attendanceLogDao().deleteAllLogs()
        db.assessmentDao().deleteAllAssessments()
        db.dailyDayStatusDao().deleteAllDayStatuses()
        db.medicalLeaveDao().deleteAllMedicalLeaves()

        db.subjectDao().insertSubjects(IertDefaultData.defaultSubjects)
        db.timetableSlotDao().insertSlots(IertDefaultData.createDefaultSlots())
        db.assessmentDao().insertAssessments(IertDefaultData.createDefaultAssessments())
        db.medicalLeaveDao().insertMedicalLeave(IertDefaultData.createDefaultMedicalLeaves().first())

        // Populate sample attendance logs for past 5 weekdays
        val today = LocalDate.now()
        val dtf = DateTimeFormatter.ISO_LOCAL_DATE
        val slots = IertDefaultData.createDefaultSlots()

        for (i in 1..5) {
            val pastDate = today.minusDays(i.toLong())
            val dayOfWeekVal = pastDate.dayOfWeek.value
            if (dayOfWeekVal in 1..6) {
                val daySlots = slots.filter { it.dayOfWeek == dayOfWeekVal }
                for ((idx, slot) in daySlots.withIndex()) {
                    val status = when {
                        i == 2 && idx == 1 -> "cancelled_by_faculty"
                        i == 4 && idx == 2 -> "bunked"
                        i == 3 && idx == 0 -> "attended" // proxy example
                        else -> "attended"
                    }
                    val isProxy = (i == 3 && idx == 0)
                    db.attendanceLogDao().insertLog(
                        AttendanceLogEntity(
                            date = pastDate.format(dtf),
                            slotId = slot.id,
                            subjectId = slot.subjectId,
                            status = status,
                            isProxy = isProxy,
                            isExtraClass = false,
                            notes = if (isProxy) "Proxy marked by batchmate" else if (status == "cancelled_by_faculty") "Faculty on CL" else null
                        )
                    )
                }
            }
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.subjectDao().deleteAllSubjects()
        db.timetableSlotDao().deleteAllSlots()
        db.attendanceLogDao().deleteAllLogs()
        db.assessmentDao().deleteAllAssessments()
        db.dailyDayStatusDao().deleteAllDayStatuses()
        db.medicalLeaveDao().deleteAllMedicalLeaves()
    }
}
