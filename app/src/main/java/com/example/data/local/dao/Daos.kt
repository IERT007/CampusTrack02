package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY code ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectById(id: Long): Flow<SubjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET reconciledAttendedOffset = :attendedOffset, reconciledTotalOffset = :totalOffset WHERE id = :subjectId")
    suspend fun updateOffsets(subjectId: Long, attendedOffset: Int, totalOffset: Int)

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}

@Dao
interface TimetableSlotDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getSlotsByDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getSlotsForDayDirect(dayOfWeek: Int): List<TimetableSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Update
    suspend fun updateSlot(slot: TimetableSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlotEntity)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteSlotById(id: Long)

    @Query("DELETE FROM timetable_slots")
    suspend fun deleteAllSlots()
}

@Dao
interface AttendanceLogDao {
    @Query("SELECT * FROM attendance_logs ORDER BY date DESC, timestamp DESC")
    fun getAllLogs(): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId")
    fun getLogsForSubject(subjectId: Long): Flow<List<AttendanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AttendanceLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AttendanceLogEntity>)

    @Query("DELETE FROM attendance_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM attendance_logs WHERE date = :date")
    suspend fun deleteLogsForDate(date: String)

    @Query("DELETE FROM attendance_logs WHERE date = :date AND slotId = :slotId")
    suspend fun deleteLogForSlot(date: String, slotId: Long)

    @Query("DELETE FROM attendance_logs")
    suspend fun deleteAllLogs()
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessments ORDER BY dueDate ASC")
    fun getAllAssessments(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE subjectId = :subjectId ORDER BY dueDate ASC")
    fun getAssessmentsForSubject(subjectId: Long): Flow<List<AssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<AssessmentEntity>)

    @Update
    suspend fun updateAssessment(assessment: AssessmentEntity)

    @Query("DELETE FROM assessments WHERE id = :id")
    suspend fun deleteAssessmentById(id: Long)

    @Query("DELETE FROM assessments")
    suspend fun deleteAllAssessments()
}

@Dao
interface DailyDayStatusDao {
    @Query("SELECT * FROM daily_day_status")
    fun getAllDayStatuses(): Flow<List<DailyDayStatusEntity>>

    @Query("SELECT * FROM daily_day_status WHERE date = :date LIMIT 1")
    fun getDayStatus(date: String): Flow<DailyDayStatusEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: DailyDayStatusEntity)

    @Query("DELETE FROM daily_day_status WHERE date = :date")
    suspend fun deleteDayStatus(date: String)

    @Query("DELETE FROM daily_day_status")
    suspend fun deleteAllDayStatuses()
}

@Dao
interface MedicalLeaveDao {
    @Query("SELECT * FROM medical_leaves ORDER BY startDate DESC")
    fun getAllMedicalLeaves(): Flow<List<MedicalLeaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalLeave(leave: MedicalLeaveEntity): Long

    @Update
    suspend fun updateMedicalLeave(leave: MedicalLeaveEntity)

    @Query("DELETE FROM medical_leaves WHERE id = :id")
    suspend fun deleteMedicalLeaveById(id: Long)

    @Query("DELETE FROM medical_leaves")
    suspend fun deleteAllMedicalLeaves()
}

@Dao
interface HolidayRangeDao {
    @Query("SELECT * FROM holiday_ranges ORDER BY startDate ASC")
    fun getAllHolidayRanges(): Flow<List<HolidayRangeEntity>>

    @Query("SELECT * FROM holiday_ranges WHERE :dateStr >= startDate AND :dateStr <= endDate LIMIT 1")
    suspend fun getHolidayForDate(dateStr: String): HolidayRangeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidayRange(range: HolidayRangeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidayRanges(ranges: List<HolidayRangeEntity>)

    @Update
    suspend fun updateHolidayRange(range: HolidayRangeEntity)

    @Query("DELETE FROM holiday_ranges WHERE id = :id")
    suspend fun deleteHolidayRangeById(id: Long)

    @Query("DELETE FROM holiday_ranges")
    suspend fun deleteAllHolidayRanges()
}

@Dao
interface AcademicTaskDao {
    @Query("SELECT * FROM academic_tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<AcademicTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AcademicTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<AcademicTaskEntity>)

    @Update
    suspend fun updateTask(task: AcademicTaskEntity)

    @Query("UPDATE academic_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, completed: Boolean)

    @Query("DELETE FROM academic_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM academic_tasks")
    suspend fun deleteAllTasks()
}

