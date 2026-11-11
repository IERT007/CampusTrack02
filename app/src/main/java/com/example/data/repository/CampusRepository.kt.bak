package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.local.entity.CustomActivityEntity
import com.example.data.local.entity.DailyDayStatusEntity
import com.example.data.local.entity.HolidayRangeEntity
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.data.local.entity.QuickNoteEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestRecordEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.data.sample.IertDefaultData
import kotlinx.coroutines.flow.Flow

class CampusRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    val allSubjects: Flow<List<SubjectEntity>> = db.subjectDao().getAllSubjects()
    val allSlots: Flow<List<TimetableSlotEntity>> = db.timetableSlotDao().getAllSlots()
    val allLogs: Flow<List<AttendanceLogEntity>> = db.attendanceLogDao().getAllLogs()
    val allAssessments: Flow<List<AssessmentEntity>> = db.assessmentDao().getAllAssessments()
    val allDayStatuses: Flow<List<DailyDayStatusEntity>> = db.dailyDayStatusDao().getAllDayStatuses()
    val allMedicalLeaves: Flow<List<MedicalLeaveEntity>> = db.medicalLeaveDao().getAllMedicalLeaves()
    val allHolidayRanges: Flow<List<HolidayRangeEntity>> = db.holidayRangeDao().getAllHolidayRanges()
    val allTasks: Flow<List<AcademicTaskEntity>> = db.academicTaskDao().getAllTasks()
    val allNotes: Flow<List<QuickNoteEntity>> = db.quickNoteDao().getAllNotes()
    val allTestRecords: Flow<List<TestRecordEntity>> = db.testRecordDao().getAllTestRecords()

    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>> =
        db.timetableSlotDao().getSlotsByDay(dayOfWeek)

    fun getLogsForDate(date: String): Flow<List<AttendanceLogEntity>> =
        db.attendanceLogDao().getLogsForDate(date)

    fun getDayStatus(date: String): Flow<DailyDayStatusEntity?> =
        db.dailyDayStatusDao().getDayStatus(date)

    fun getActivitiesForDate(date: String): Flow<List<CustomActivityEntity>> =
        db.customActivityDao().getActivitiesForDate(date)

    suspend fun getHolidayForDate(date: String): HolidayRangeEntity? =
        db.holidayRangeDao().getHolidayForDate(date)

    suspend fun ensureDefaultDataLoaded() {
        val current = db.subjectDao().getAllSubjectsList()
        if (current.isEmpty()) {
            seedDefaultData()
        }
    }

    suspend fun seedDefaultData() {
        db.subjectDao().deleteAllSubjects()
        db.timetableSlotDao().deleteAllSlots()
        db.assessmentDao().deleteAllAssessments()
        db.holidayRangeDao().deleteAllHolidayRanges()

        db.subjectDao().insertSubjects(IertDefaultData.defaultSubjects)
        db.timetableSlotDao().insertSlots(IertDefaultData.buildDefaultTimetable())
        db.assessmentDao().insertAssessments(IertDefaultData.buildDefaultAssessments())
        db.holidayRangeDao().insertHolidayRanges(IertDefaultData.buildDefaultHolidays())
    }

    suspend fun saveSubject(subject: SubjectEntity) {
        if (subject.id == 0L) {
            db.subjectDao().insertSubject(subject)
        } else {
            db.subjectDao().updateSubject(subject)
        }
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        db.subjectDao().deleteSubject(subject)
    }

    suspend fun deleteSubjectCascade(subject: SubjectEntity) {
        db.timetableSlotDao().deleteSlotsForSubject(subject.id)
        db.attendanceLogDao().deleteLogsForSubject(subject.id)
        db.testRecordDao().deleteTestRecordsForSubject(subject.id)
        db.subjectDao().deleteSubject(subject)
    }

    suspend fun reconcileSubject(subjectId: Long, attendedOffset: Int, totalOffset: Int) {
        db.subjectDao().updateOffsets(subjectId, attendedOffset, totalOffset)
    }

    suspend fun saveSlot(slot: TimetableSlotEntity) {
        if (slot.id == 0L) {
            db.timetableSlotDao().insertSlot(slot)
        } else {
            db.timetableSlotDao().updateSlot(slot)
        }
    }

    suspend fun deleteSlot(slotId: Long) {
        db.timetableSlotDao().deleteSlotById(slotId)
    }

    suspend fun logSlotAttendance(
        date: String,
        slotId: Long,
        subjectId: Long,
        status: String,
        isProxy: Boolean,
        notes: String?
    ) {
        db.attendanceLogDao().deleteLogForSlot(date, slotId)
        db.attendanceLogDao().insertLog(
            AttendanceLogEntity(
                date = date,
                slotId = slotId,
                subjectId = subjectId,
                status = status,
                isProxy = isProxy,
                isExtraClass = false,
                notes = notes
            )
        )
    }

    suspend fun undoSlotAttendance(date: String, slotId: Long) {
        db.attendanceLogDao().deleteLogForSlot(date, slotId)
    }

    suspend fun logExtraClass(
        date: String,
        subjectId: Long,
        status: String,
        isProxy: Boolean,
        notes: String?
    ) {
        db.attendanceLogDao().insertLog(
            AttendanceLogEntity(
                date = date,
                slotId = null,
                subjectId = subjectId,
                status = status,
                isProxy = isProxy,
                isExtraClass = true,
                notes = notes
            )
        )
    }

    suspend fun deleteLogById(logId: Long) {
        db.attendanceLogDao().deleteLogById(logId)
    }

    suspend fun setDailyDayStatus(status: DailyDayStatusEntity) {
        db.dailyDayStatusDao().insertOrUpdate(status)
    }

    suspend fun deleteLogsForDate(date: String) {
        db.attendanceLogDao().deleteLogsForDate(date)
    }

    suspend fun saveAssessment(assessment: AssessmentEntity) {
        db.assessmentDao().insertAssessment(assessment)
    }

    suspend fun deleteAssessment(id: Long) {
        db.assessmentDao().deleteAssessmentById(id)
    }

    suspend fun saveMedicalLeave(leave: MedicalLeaveEntity) {
        db.medicalLeaveDao().insertMedicalLeave(leave)
    }

    suspend fun deleteMedicalLeave(id: Long) {
        db.medicalLeaveDao().deleteMedicalLeaveById(id)
    }

    suspend fun saveHolidayRange(holiday: HolidayRangeEntity) {
        db.holidayRangeDao().insertHolidayRange(holiday)
    }

    suspend fun deleteHolidayRange(id: Long) {
        db.holidayRangeDao().deleteHolidayRangeById(id)
    }

    suspend fun saveTask(task: AcademicTaskEntity) {
        db.academicTaskDao().insertTask(task)
    }

    suspend fun toggleTaskCompleted(id: Long, completed: Boolean) {
        db.academicTaskDao().updateTaskCompletion(id, completed)
    }

    suspend fun deleteTask(id: Long) {
        db.academicTaskDao().deleteTaskById(id)
    }

    suspend fun saveNote(note: QuickNoteEntity) {
        if (note.id == 0L) {
            db.quickNoteDao().insertNote(note)
        } else {
            db.quickNoteDao().updateNote(note)
        }
    }

    suspend fun toggleNotePin(id: Long, isPinned: Boolean) {
        db.quickNoteDao().togglePin(id, isPinned)
    }

    suspend fun deleteNote(id: Long) {
        db.quickNoteDao().deleteNoteById(id)
    }

    suspend fun saveTestRecord(record: TestRecordEntity) {
        db.testRecordDao().insertTestRecord(record)
    }

    suspend fun deleteTestRecord(id: Long) {
        db.testRecordDao().deleteTestRecordById(id)
    }

    suspend fun saveCustomActivity(activity: CustomActivityEntity) {
        db.customActivityDao().insertActivity(activity)
    }

    suspend fun deleteCustomActivity(id: Long) {
        db.customActivityDao().deleteActivityById(id)
    }

    suspend fun clearAllAttendanceLogs() {
        db.attendanceLogDao().deleteAllLogs()
        db.dailyDayStatusDao().deleteAllDayStatuses()
    }

    suspend fun clearAllData() {
        db.attendanceLogDao().deleteAllLogs()
        db.dailyDayStatusDao().deleteAllDayStatuses()
        db.academicTaskDao().deleteAllTasks()
        db.quickNoteDao().deleteAllNotes()
        db.medicalLeaveDao().deleteAllMedicalLeaves()
        seedDefaultData()
    }
}
