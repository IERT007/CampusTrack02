package com.example.data.repository

import android.content.Context
import com.example.cloud.CloudSyncWorker
import com.example.cloud.SupabaseSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.sample.IertDefaultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CampusRepository(
    private val context: Context? = null,
    private val db: AppDatabase
) {

    constructor(db: AppDatabase) : this(null, db)

    val allSemesters: Flow<List<SemesterEntity>> = db.semesterDao().getAllSemesters()
    val activeSemester: Flow<SemesterEntity?> = db.semesterDao().getActiveSemester()
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
    val allExtraSlots: Flow<List<ExtraSlotEntity>> = db.extraSlotDao().getAllExtraSlots()
    val allOverrides: Flow<List<RescheduleOverrideEntity>> = db.rescheduleOverrideDao().getAllOverrides()
    val allTodoTasks: Flow<List<TodoTaskEntity>> = db.todoDao().getAllTodoTasks()

    // --- Multi-Semester Profile Operations ---
    suspend fun ensureDefaultSemesterExists() = withContext(Dispatchers.IO) {
        val existing = db.semesterDao().getAllSemestersDirect()
        if (existing.isEmpty()) {
            db.semesterDao().insertSemester(
                SemesterEntity(
                    id = 1L,
                    name = "Semester 3 • Tool Engg",
                    startDate = "2026-09-19",
                    endDate = "2026-12-15",
                    isActive = true,
                    isArchived = false
                )
            )
        } else if (existing.none { it.isActive }) {
            db.semesterDao().switchActiveSemester(existing.first().id)
        }
    }

    suspend fun createSemester(
        name: String,
        startDate: String,
        endDate: String,
        activate: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val newId = db.semesterDao().insertSemester(
            SemesterEntity(
                name = name.trim(),
                startDate = startDate,
                endDate = endDate,
                isActive = false,
                isArchived = false
            )
        )
        if (activate) {
            db.semesterDao().switchActiveSemester(newId)
        }
        triggerSilentAutoBackup()
        newId
    }

    suspend fun switchActiveSemester(semesterId: Long) = withContext(Dispatchers.IO) {
        db.semesterDao().switchActiveSemester(semesterId)
        triggerSilentAutoBackup()
    }

    suspend fun setSemesterArchived(semesterId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        db.semesterDao().setSemesterArchived(semesterId, isArchived)
        triggerSilentAutoBackup()
    }

    suspend fun updateSemesterDates(semesterId: Long, startDate: String, endDate: String) = withContext(Dispatchers.IO) {
        db.semesterDao().updateSemesterDates(semesterId, startDate, endDate)
        triggerSilentAutoBackup()
    }

    fun getExtraSlotsForDate(date: String): Flow<List<ExtraSlotEntity>> = db.extraSlotDao().getExtraSlotsForDate(date)
    fun getOverridesForDate(date: String): Flow<List<RescheduleOverrideEntity>> = db.rescheduleOverrideDao().getOverridesForDate(date)

    fun getActivitiesForDate(date: String): Flow<List<CustomActivityEntity>> {
        return db.customActivityDao().getActivitiesForDate(date)
    }

    fun searchSubjects(query: String): Flow<List<SubjectEntity>> {
        return db.subjectDao().searchSubjects("%$query%")
    }

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
        notes: String? = null,
        semesterId: Long = 1L
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
            timestamp = System.currentTimeMillis(),
            semesterId = semesterId
        )
        db.attendanceLogDao().insertLog(entity)
        triggerSilentAutoBackup()
    }

    suspend fun deleteSlotAttendance(date: String, slotId: Long) = withContext(Dispatchers.IO) {
        db.attendanceLogDao().deleteLogForSlot(date, slotId)
        triggerSilentAutoBackup()
    }

    suspend fun deleteAttendanceLog(log: AttendanceLogEntity) = withContext(Dispatchers.IO) {
        db.attendanceLogDao().deleteAttendanceLog(log)
        triggerSilentAutoBackup()
    }

    suspend fun deleteAttendanceLogById(logId: Long) = withContext(Dispatchers.IO) {
        db.attendanceLogDao().deleteLogById(logId)
        triggerSilentAutoBackup()
    }

    suspend fun markWholeDayPresent(
        date: String,
        slots: List<TimetableSlotEntity>,
        semesterId: Long = 1L
    ) = withContext(Dispatchers.IO) {
        db.dailyDayStatusDao().insertOrUpdate(
            DailyDayStatusEntity(date = date, wentToCollege = true, leaveCategory = "none", notes = "Whole day marked present")
        )
        for (slot in slots) {
            db.attendanceLogDao().deleteLogForSlot(date, slot.id)
            db.attendanceLogDao().insertLog(
                AttendanceLogEntity(
                    date = date,
                    slotId = slot.id,
                    subjectId = slot.subjectId,
                    status = "attended",
                    isProxy = false,
                    isExtraClass = false,
                    notes = "Auto bulk mark",
                    timestamp = System.currentTimeMillis(),
                    semesterId = semesterId
                )
            )
        }
        triggerSilentAutoBackup()
    }

    suspend fun markMassBunkOrOff(
        date: String,
        slots: List<TimetableSlotEntity>,
        category: String, // "college_holiday", "mass_bunk", "strike"
        semesterId: Long = 1L
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
            db.attendanceLogDao().deleteLogForSlot(date, slot.id)
            db.attendanceLogDao().insertLog(
                AttendanceLogEntity(
                    date = date,
                    slotId = slot.id,
                    subjectId = slot.subjectId,
                    status = statusString,
                    isProxy = false,
                    isExtraClass = false,
                    notes = category,
                    timestamp = System.currentTimeMillis(),
                    semesterId = semesterId
                )
            )
        }
        triggerSilentAutoBackup()
    }

    suspend fun logAdHocExtraClass(
        date: String,
        subjectId: Long,
        status: String,
        isProxy: Boolean,
        notes: String,
        semesterId: Long = 1L
    ) = withContext(Dispatchers.IO) {
        val entity = AttendanceLogEntity(
            date = date,
            slotId = null,
            subjectId = subjectId,
            status = status,
            isProxy = isProxy,
            isExtraClass = true,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            semesterId = semesterId
        )
        db.attendanceLogDao().insertLog(entity)
        triggerSilentAutoBackup()
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
        triggerSilentAutoBackup()
    }

    suspend fun addSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.subjectDao().insertSubject(subject)
        triggerSilentAutoBackup()
    }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.subjectDao().updateSubject(subject)
        triggerSilentAutoBackup()
    }

    suspend fun deleteSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.subjectDao().deleteSubject(subject)
        triggerSilentAutoBackup()
    }

    suspend fun deleteSubjectCascade(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        db.attendanceLogDao().deleteLogsForSubject(subject.id)
        db.timetableSlotDao().deleteSlotsForSubject(subject.id)
        db.testRecordDao().deleteTestRecordsForSubject(subject.id)
        db.subjectDao().deleteSubject(subject)
        triggerSilentAutoBackup()
    }

    suspend fun saveTestRecord(record: TestRecordEntity): Long = withContext(Dispatchers.IO) {
        val id = db.testRecordDao().insertTestRecord(record)
        triggerSilentAutoBackup()
        id
    }

    suspend fun deleteTestRecord(id: Long) = withContext(Dispatchers.IO) {
        db.testRecordDao().deleteTestRecordById(id)
        triggerSilentAutoBackup()
    }

    suspend fun updateTestRecordMarks(id: Long, marks: Double?) = withContext(Dispatchers.IO) {
        db.testRecordDao().updateMarksObtained(id, marks)
        triggerSilentAutoBackup()
    }

    suspend fun addExtraSlot(slot: ExtraSlotEntity): Long = withContext(Dispatchers.IO) {
        val id = db.extraSlotDao().insertExtraSlot(slot)
        triggerSilentAutoBackup()
        id
    }

    suspend fun deleteExtraSlot(id: Long) = withContext(Dispatchers.IO) {
        db.extraSlotDao().deleteExtraSlotById(id)
        triggerSilentAutoBackup()
    }

    /**
     * Clean 2-option Extra Class deletion:
     * - deleteAttendanceLog = true  -> removes ExtraSlotEntity AND its recorded AttendanceLogEntity
     * - deleteAttendanceLog = false -> removes ExtraSlotEntity ONLY, preserving the AttendanceLogEntity
     *   so it remains visible as an orphan extra log in both Home (Marked Periods) and Audit.
     */
    suspend fun deleteExtraSlotWithAttendanceOption(
        extraSlotId: Long,
        date: String,
        deleteAttendanceLog: Boolean
    ) = withContext(Dispatchers.IO) {
        val syntheticSlotId = -extraSlotId
        db.extraSlotDao().deleteExtraSlotById(extraSlotId)
        if (deleteAttendanceLog) {
            db.attendanceLogDao().deleteLogForSlot(date, syntheticSlotId)
            val dateLogs = db.attendanceLogDao().getLogsForDate(date).first()
            dateLogs.filter {
                it.slotId == syntheticSlotId ||
                    (it.isExtraClass && it.notes?.contains("extra_$syntheticSlotId") == true)
            }.forEach { log ->
                db.attendanceLogDao().deleteAttendanceLog(log)
            }
        }
        triggerSilentAutoBackup()
    }

    /**
     * Collects all AttendanceLogEntity rows for a given date where `isExtraClass == true`
     * that do NOT have a matching active ExtraSlotEntity (orphan logs from deleted slots
     * or legacy builds), ensuring Home (Today) and Audit always stay 100% synchronized.
     */
    fun findOrphanExtraLogsForDate(
        date: String,
        allLogs: List<AttendanceLogEntity>,
        dateExtraSlots: List<ExtraSlotEntity>,
        activeRegularSlotIds: Set<Long> = emptySet()
    ): List<AttendanceLogEntity> {
        val activeSyntheticIds = dateExtraSlots.map { -it.id }.toSet()
        return allLogs.filter { log ->
            if (log.date != date) return@filter false
            val isExtra = log.isExtraClass || (log.slotId != null && log.slotId < 0L)
            if (!isExtra) return@filter false

            val matchesActiveRegular = log.slotId != null && log.slotId > 0L && log.slotId in activeRegularSlotIds
            if (matchesActiveRegular) return@filter false

            val matchesActiveExtra = dateExtraSlots.any { extra ->
                val syntheticId = -extra.id
                log.slotId == syntheticId ||
                    (log.isExtraClass && log.subjectId == extra.subjectId && log.notes?.contains("extra_$syntheticId") == true)
            }
            val matchesSyntheticId = log.slotId != null && log.slotId in activeSyntheticIds

            !matchesActiveExtra && !matchesSyntheticId
        }.sortedByDescending { it.timestamp }
    }

    suspend fun saveRescheduleOverride(override: RescheduleOverrideEntity): Long = withContext(Dispatchers.IO) {
        val id = db.rescheduleOverrideDao().insertOverride(override)
        triggerSilentAutoBackup()
        id
    }

    suspend fun deleteRescheduleOverride(slotId: Long, date: String) = withContext(Dispatchers.IO) {
        db.rescheduleOverrideDao().deleteOverrideForSlot(slotId, date)
        triggerSilentAutoBackup()
    }

    suspend fun saveCustomActivity(activity: CustomActivityEntity): Long = withContext(Dispatchers.IO) {
        val id = db.customActivityDao().insertActivity(activity)
        triggerSilentAutoBackup()
        id
    }

    suspend fun deleteCustomActivity(id: Long) = withContext(Dispatchers.IO) {
        db.customActivityDao().deleteActivityById(id)
        triggerSilentAutoBackup()
    }

    suspend fun addAssessment(assessment: AssessmentEntity) = withContext(Dispatchers.IO) {
        db.assessmentDao().insertAssessment(assessment)
        triggerSilentAutoBackup()
    }

    suspend fun updateAssessment(assessment: AssessmentEntity) = withContext(Dispatchers.IO) {
        db.assessmentDao().updateAssessment(assessment)
        triggerSilentAutoBackup()
    }

    suspend fun deleteAssessment(id: Long) = withContext(Dispatchers.IO) {
        db.assessmentDao().deleteAssessmentById(id)
        triggerSilentAutoBackup()
    }

    suspend fun addMedicalLeave(leave: MedicalLeaveEntity) = withContext(Dispatchers.IO) {
        db.medicalLeaveDao().insertMedicalLeave(leave)
        triggerSilentAutoBackup()
    }

    suspend fun updateMedicalLeave(leave: MedicalLeaveEntity) = withContext(Dispatchers.IO) {
        db.medicalLeaveDao().updateMedicalLeave(leave)
        triggerSilentAutoBackup()
    }

    suspend fun deleteMedicalLeave(id: Long) = withContext(Dispatchers.IO) {
        db.medicalLeaveDao().deleteMedicalLeaveById(id)
        triggerSilentAutoBackup()
    }

    suspend fun addTimetableSlot(slot: TimetableSlotEntity) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().insertSlot(slot)
        triggerSilentAutoBackup()
    }

    suspend fun updateTimetableSlot(slot: TimetableSlotEntity) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().updateSlot(slot)
        triggerSilentAutoBackup()
    }

    suspend fun deleteTimetableSlot(slot: TimetableSlotEntity) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().deleteSlot(slot)
        triggerSilentAutoBackup()
    }

    suspend fun deleteTimetableSlotById(id: Long) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().deleteSlotById(id)
        triggerSilentAutoBackup()
    }

    suspend fun addHolidayRange(range: HolidayRangeEntity) = withContext(Dispatchers.IO) {
        db.holidayRangeDao().insertHolidayRange(range)
        triggerSilentAutoBackup()
    }

    suspend fun updateHolidayRange(range: HolidayRangeEntity) = withContext(Dispatchers.IO) {
        db.holidayRangeDao().updateHolidayRange(range)
        triggerSilentAutoBackup()
    }

    suspend fun deleteHolidayRange(id: Long) = withContext(Dispatchers.IO) {
        db.holidayRangeDao().deleteHolidayRangeById(id)
        triggerSilentAutoBackup()
    }

    suspend fun deleteTimetableSlot(id: Long) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().deleteSlotById(id)
        triggerSilentAutoBackup()
    }

    suspend fun addTask(task: AcademicTaskEntity) = withContext(Dispatchers.IO) {
        db.academicTaskDao().insertTask(task)
        triggerSilentAutoBackup()
    }

    suspend fun updateTask(task: AcademicTaskEntity) = withContext(Dispatchers.IO) {
        db.academicTaskDao().updateTask(task)
        triggerSilentAutoBackup()
    }

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        db.academicTaskDao().deleteTaskById(id)
        triggerSilentAutoBackup()
    }

    suspend fun toggleTaskCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        db.academicTaskDao().updateTaskCompletion(id, completed)
        triggerSilentAutoBackup()
    }

    // --- Smart Auto-Prioritized To-Do CRUD ---
    suspend fun addTodoTask(task: TodoTaskEntity): Long = withContext(Dispatchers.IO) {
        val id = db.todoDao().insertTodoTask(task)
        triggerSilentAutoBackup()
        id
    }

    suspend fun updateTodoTask(task: TodoTaskEntity) = withContext(Dispatchers.IO) {
        db.todoDao().updateTodoTask(task)
        triggerSilentAutoBackup()
    }

    suspend fun toggleTodoTaskCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        db.todoDao().updateTodoCompletion(id, completed)
        triggerSilentAutoBackup()
    }

    suspend fun deleteTodoTask(id: Long) = withContext(Dispatchers.IO) {
        db.todoDao().deleteTodoTaskById(id)
        triggerSilentAutoBackup()
    }

    suspend fun addNote(note: QuickNoteEntity) = withContext(Dispatchers.IO) {
        db.quickNoteDao().insertNote(note)
        triggerSilentAutoBackup()
    }

    suspend fun updateNote(note: QuickNoteEntity) = withContext(Dispatchers.IO) {
        db.quickNoteDao().updateNote(note)
        triggerSilentAutoBackup()
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        db.quickNoteDao().deleteNoteById(id)
        triggerSilentAutoBackup()
    }

    suspend fun toggleNotePin(id: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        db.quickNoteDao().togglePin(id, pinned)
        triggerSilentAutoBackup()
    }

    suspend fun checkAndInitializeDefaultData() = withContext(Dispatchers.IO) {
        ensureDefaultSemesterExists()
    }

    suspend fun preloadDefaultIertData(semesterId: Long = 1L) = withContext(Dispatchers.IO) {
        ensureDefaultSemesterExists()
        clearAllData(preserveSemesters = true)

        db.subjectDao().insertSubjects(IertDefaultData.defaultSubjects.map { it.copy(semesterId = semesterId) })
        db.timetableSlotDao().insertSlots(IertDefaultData.createDefaultSlots().map { it.copy(semesterId = semesterId) })
        db.assessmentDao().insertAssessments(IertDefaultData.createDefaultAssessments().map { it.copy(semesterId = semesterId) })
        triggerSilentAutoBackup()
    }

    suspend fun clearAllData(preserveSemesters: Boolean = false) = withContext(Dispatchers.IO) {
        db.subjectDao().deleteAllSubjects()
        db.timetableSlotDao().deleteAllSlots()
        db.attendanceLogDao().deleteAllLogs()
        db.assessmentDao().deleteAllAssessments()
        db.dailyDayStatusDao().deleteAllDayStatuses()
        db.medicalLeaveDao().deleteAllMedicalLeaves()
        db.holidayRangeDao().deleteAllHolidayRanges()
        db.academicTaskDao().deleteAllTasks()
        db.todoDao().deleteAllTodoTasks()
        db.extraSlotDao().deleteAllExtraSlots()
        db.rescheduleOverrideDao().deleteAllOverrides()
        db.testRecordDao().deleteAllTestRecords()
        if (!preserveSemesters) {
            db.semesterDao().deleteAllSemesters()
        }
    }

    // --- Silent Auto-Backup Engine (local vault + debounced Supabase WorkManager Cloud Sync) ---
    suspend fun triggerSilentAutoBackup(enqueueCloudSync: Boolean = true) = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        try {
            val jsonStr = exportToJson()
            val vaultFile = File(ctx.filesDir, "caliper_vault_snapshot.json")
            val tempFile = File(ctx.filesDir, "caliper_vault_snapshot.tmp")
            tempFile.writeText(jsonStr)
            if (tempFile.exists()) {
                tempFile.renameTo(vaultFile)
            }
        } catch (_: Exception) {}

        if (enqueueCloudSync) {
            try {
                if (SupabaseSyncManager(ctx).isConfigured()) {
                    CloudSyncWorker.enqueueDebouncedSync(ctx)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun restoreFromAutoVault(): Boolean = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext false
        try {
            var vaultFile = File(ctx.filesDir, "caliper_vault_snapshot.json")
            if (!vaultFile.exists() || vaultFile.length() == 0L) {
                vaultFile = File(ctx.filesDir, "CampusTrack_AutoVault.json")
            }
            if (!vaultFile.exists() || vaultFile.length() == 0L) return@withContext false
            val json = vaultFile.readText()
            importFromJson(json)
        } catch (_: Exception) {
            false
        }
    }

    fun hasAutoVault(): Boolean {
        val ctx = context ?: return false
        val vaultFile = File(ctx.filesDir, "caliper_vault_snapshot.json")
        val legacyVaultFile = File(ctx.filesDir, "CampusTrack_AutoVault.json")
        return (vaultFile.exists() && vaultFile.length() > 0) || (legacyVaultFile.exists() && legacyVaultFile.length() > 0)
    }

    // --- JSON Backup & Restore Engine (Local File Storage & Supabase Cloud Vault) ---
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Caliper")
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())

        // 0. Semesters
        val semestersList = db.semesterDao().getAllSemestersDirect()
        val semestersArray = JSONArray()
        for (sem in semestersList) {
            val obj = JSONObject()
            obj.put("id", sem.id)
            obj.put("name", sem.name)
            obj.put("startDate", sem.startDate)
            obj.put("endDate", sem.endDate)
            obj.put("isActive", sem.isActive)
            obj.put("isArchived", sem.isArchived)
            semestersArray.put(obj)
        }
        root.put("semesters", semestersArray)

        // 1. Subjects
        val subjectsList = db.subjectDao().getAllSubjects().first()
        val subjectsArray = JSONArray()
        for (s in subjectsList) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("code", s.code)
            obj.put("type", s.type)
            obj.put("facultyName", s.facultyName)
            obj.put("strictness", s.strictness)
            obj.put("colorHex", s.colorHex)
            obj.put("reconciledAttendedOffset", s.reconciledAttendedOffset)
            obj.put("reconciledTotalOffset", s.reconciledTotalOffset)
            obj.put("semesterId", s.semesterId)
            subjectsArray.put(obj)
        }
        root.put("subjects", subjectsArray)

        // 2. Timetable Slots
        val slotsList = db.timetableSlotDao().getAllSlots().first()
        val slotsArray = JSONArray()
        for (slot in slotsList) {
            val obj = JSONObject()
            obj.put("id", slot.id)
            obj.put("dayOfWeek", slot.dayOfWeek)
            obj.put("startTime", slot.startTime)
            obj.put("endTime", slot.endTime)
            obj.put("roomNo", slot.roomNo)
            obj.put("subjectId", slot.subjectId)
            obj.put("semesterId", slot.semesterId)
            slotsArray.put(obj)
        }
        root.put("timetable_slots", slotsArray)

        // 3. Attendance Logs
        val logsList = db.attendanceLogDao().getAllLogs().first()
        val logsArray = JSONArray()
        for (log in logsList) {
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("date", log.date)
            obj.put("slotId", log.slotId ?: JSONObject.NULL)
            obj.put("subjectId", log.subjectId)
            obj.put("status", log.status)
            obj.put("isProxy", log.isProxy)
            obj.put("isExtraClass", log.isExtraClass)
            obj.put("notes", log.notes ?: JSONObject.NULL)
            obj.put("timestamp", log.timestamp)
            obj.put("semesterId", log.semesterId)
            logsArray.put(obj)
        }
        root.put("attendance_logs", logsArray)

        // 4. Assessments & Deadlines
        val assessmentsList = db.assessmentDao().getAllAssessments().first()
        val assessmentsArray = JSONArray()
        for (a in assessmentsList) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("subjectId", a.subjectId)
            obj.put("type", a.type)
            obj.put("title", a.title)
            obj.put("maxMarks", a.maxMarks)
            obj.put("obtainedMarks", a.obtainedMarks ?: JSONObject.NULL)
            obj.put("status", a.status)
            obj.put("missedReason", a.missedReason ?: JSONObject.NULL)
            obj.put("dueDate", a.dueDate)
            obj.put("dueTime", a.dueTime)
            obj.put("syllabusCoveragePercent", a.syllabusCoveragePercent)
            obj.put("semesterId", a.semesterId)
            assessmentsArray.put(obj)
        }
        root.put("assessments", assessmentsArray)

        // 5. Daily Day Statuses
        val statusesList = db.dailyDayStatusDao().getAllDayStatuses().first()
        val statusesArray = JSONArray()
        for (st in statusesList) {
            val obj = JSONObject()
            obj.put("date", st.date)
            obj.put("wentToCollege", st.wentToCollege)
            obj.put("leaveCategory", st.leaveCategory)
            obj.put("notes", st.notes ?: JSONObject.NULL)
            statusesArray.put(obj)
        }
        root.put("daily_day_status", statusesArray)

        // 6. Holiday Ranges
        val holidaysList = db.holidayRangeDao().getAllHolidayRanges().first()
        val holidaysArray = JSONArray()
        for (h in holidaysList) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("title", h.title)
            obj.put("startDate", h.startDate)
            obj.put("endDate", h.endDate)
            obj.put("type", h.type)
            obj.put("notes", h.notes ?: JSONObject.NULL)
            holidaysArray.put(obj)
        }
        root.put("holiday_ranges", holidaysArray)

        // 7. Medical Leaves
        val leavesList = db.medicalLeaveDao().getAllMedicalLeaves().first()
        val leavesArray = JSONArray()
        for (m in leavesList) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("startDate", m.startDate)
            obj.put("endDate", m.endDate)
            obj.put("reason", m.reason)
            obj.put("doctorName", m.doctorName)
            obj.put("submittedTo", m.submittedTo)
            obj.put("status", m.status)
            obj.put("refNo", m.refNo)
            obj.put("notes", m.notes ?: JSONObject.NULL)
            obj.put("timestamp", m.timestamp)
            leavesArray.put(obj)
        }
        root.put("medical_leaves", leavesArray)

        // 8. Academic Tasks
        val tasksList = db.academicTaskDao().getAllTasks().first()
        val tasksArray = JSONArray()
        for (t in tasksList) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("description", t.description)
            obj.put("priority", t.priority)
            obj.put("category", t.category)
            obj.put("dueDate", t.dueDate)
            obj.put("isCompleted", t.isCompleted)
            obj.put("subjectId", t.subjectId ?: JSONObject.NULL)
            obj.put("createdAt", t.createdAt)
            tasksArray.put(obj)
        }
        root.put("academic_tasks", tasksArray)

        // 9. Smart Auto-Prioritized To-Do Tasks
        val todoList = db.todoDao().getAllTodoTasks().first()
        val todoArray = JSONArray()
        for (todo in todoList) {
            val obj = JSONObject()
            obj.put("id", todo.id)
            obj.put("title", todo.title)
            obj.put("subjectId", todo.subjectId ?: JSONObject.NULL)
            obj.put("dueDateTimeMillis", todo.dueDateTimeMillis)
            obj.put("isCompleted", todo.isCompleted)
            obj.put("createdAt", todo.createdAt)
            obj.put("semesterId", todo.semesterId)
            todoArray.put(obj)
        }
        root.put("todo_tasks", todoArray)

        // 10. Date-Specific Extra Slots
        val extraSlotsList = db.extraSlotDao().getAllExtraSlots().first()
        val extraSlotsArray = JSONArray()
        for (ex in extraSlotsList) {
            val obj = JSONObject()
            obj.put("id", ex.id)
            obj.put("date", ex.date)
            obj.put("subjectId", ex.subjectId)
            obj.put("startTime", ex.startTime)
            obj.put("endTime", ex.endTime)
            obj.put("roomNo", ex.roomNo)
            obj.put("tag", ex.tag)
            obj.put("semesterId", ex.semesterId)
            extraSlotsArray.put(obj)
        }
        root.put("extra_slots", extraSlotsArray)

        // 11. Single-Day Reschedule Overrides
        val overridesList = db.rescheduleOverrideDao().getAllOverrides().first()
        val overridesArray = JSONArray()
        for (ov in overridesList) {
            val obj = JSONObject()
            obj.put("id", ov.id)
            obj.put("slotId", ov.slotId)
            obj.put("date", ov.date)
            obj.put("newStartTime", ov.newStartTime)
            obj.put("newEndTime", ov.newEndTime)
            obj.put("newRoomNo", ov.newRoomNo ?: JSONObject.NULL)
            obj.put("semesterId", ov.semesterId)
            overridesArray.put(obj)
        }
        root.put("reschedule_overrides", overridesArray)

        // 12. Test Records
        val testsList = db.testRecordDao().getAllTestRecords().first()
        val testsArray = JSONArray()
        for (tr in testsList) {
            val obj = JSONObject()
            obj.put("id", tr.id)
            obj.put("subjectId", tr.subjectId)
            obj.put("date", tr.date)
            obj.put("slotId", tr.slotId ?: JSONObject.NULL)
            obj.put("testName", tr.testName)
            obj.put("marksObtained", tr.marksObtained ?: JSONObject.NULL)
            obj.put("maxMarks", tr.maxMarks)
            obj.put("timestamp", tr.timestamp)
            testsArray.put(obj)
        }
        root.put("test_records", testsArray)

        root.toString(2)
    }

    suspend fun importFromJson(
        jsonString: String,
        triggerCloudSync: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("subjects") && !root.has("timetable_slots") && !root.has("semesters")) {
                return@withContext false
            }

            clearAllData(preserveSemesters = false)

            // 0. Semesters
            if (root.has("semesters")) {
                val array = root.getJSONArray("semesters")
                val list = mutableListOf<SemesterEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SemesterEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", "Semester 3 • Tool Engg"),
                            startDate = obj.optString("startDate", "2026-09-19"),
                            endDate = obj.optString("endDate", "2026-12-15"),
                            isActive = obj.optBoolean("isActive", i == 0),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                    )
                }
                if (list.isNotEmpty()) db.semesterDao().insertSemesters(list)
            }
            ensureDefaultSemesterExists()

            // 1. Subjects
            if (root.has("subjects")) {
                val array = root.getJSONArray("subjects")
                val list = mutableListOf<SubjectEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SubjectEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            code = obj.getString("code"),
                            type = obj.optString("type", "theory"),
                            facultyName = obj.optString("facultyName", "Faculty"),
                            strictness = obj.optString("strictness", "moderate"),
                            colorHex = obj.optString("colorHex", "#00E5FF"),
                            reconciledAttendedOffset = obj.optInt("reconciledAttendedOffset", 0),
                            reconciledTotalOffset = obj.optInt("reconciledTotalOffset", 0),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.subjectDao().insertSubjects(list)
            }

            // 2. Timetable Slots
            if (root.has("timetable_slots")) {
                val array = root.getJSONArray("timetable_slots")
                val list = mutableListOf<TimetableSlotEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TimetableSlotEntity(
                            id = obj.optLong("id", 0L),
                            dayOfWeek = obj.getInt("dayOfWeek"),
                            startTime = obj.getString("startTime"),
                            endTime = obj.getString("endTime"),
                            roomNo = obj.getString("roomNo"),
                            subjectId = obj.getLong("subjectId"),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.timetableSlotDao().insertSlots(list)
            }

            // 3. Attendance Logs
            if (root.has("attendance_logs")) {
                val array = root.getJSONArray("attendance_logs")
                val list = mutableListOf<AttendanceLogEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AttendanceLogEntity(
                            id = obj.optLong("id", 0L),
                            date = obj.getString("date"),
                            slotId = if (obj.isNull("slotId")) null else obj.optLong("slotId"),
                            subjectId = obj.getLong("subjectId"),
                            status = obj.getString("status"),
                            isProxy = obj.optBoolean("isProxy", false),
                            isExtraClass = obj.optBoolean("isExtraClass", false),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.attendanceLogDao().insertLogs(list)
            }

            // 4. Assessments
            if (root.has("assessments")) {
                val array = root.getJSONArray("assessments")
                val list = mutableListOf<AssessmentEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AssessmentEntity(
                            id = obj.optLong("id", 0L),
                            subjectId = obj.getLong("subjectId"),
                            type = obj.getString("type"),
                            title = obj.getString("title"),
                            maxMarks = obj.getDouble("maxMarks"),
                            obtainedMarks = if (obj.isNull("obtainedMarks")) null else obj.optDouble("obtainedMarks"),
                            status = obj.optString("status", "pending"),
                            missedReason = if (obj.isNull("missedReason")) null else obj.optString("missedReason"),
                            dueDate = obj.getString("dueDate"),
                            dueTime = obj.optString("dueTime", "17:00"),
                            syllabusCoveragePercent = obj.optInt("syllabusCoveragePercent", 0),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.assessmentDao().insertAssessments(list)
            }

            // 5. Daily Day Statuses
            if (root.has("daily_day_status")) {
                val array = root.getJSONArray("daily_day_status")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.dailyDayStatusDao().insertOrUpdate(
                        DailyDayStatusEntity(
                            date = obj.getString("date"),
                            wentToCollege = obj.optBoolean("wentToCollege", true),
                            leaveCategory = obj.optString("leaveCategory", "none"),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes")
                        )
                    )
                }
            }

            // 6. Holiday Ranges
            if (root.has("holiday_ranges")) {
                val array = root.getJSONArray("holiday_ranges")
                val list = mutableListOf<HolidayRangeEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HolidayRangeEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            startDate = obj.getString("startDate"),
                            endDate = obj.getString("endDate"),
                            type = obj.optString("type", "holiday"),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes")
                        )
                    )
                }
                if (list.isNotEmpty()) db.holidayRangeDao().insertHolidayRanges(list)
            }

            // 7. Medical Leaves
            if (root.has("medical_leaves")) {
                val array = root.getJSONArray("medical_leaves")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.medicalLeaveDao().insertMedicalLeave(
                        MedicalLeaveEntity(
                            id = obj.optLong("id", 0L),
                            startDate = obj.getString("startDate"),
                            endDate = obj.getString("endDate"),
                            reason = obj.getString("reason"),
                            doctorName = obj.optString("doctorName", ""),
                            submittedTo = obj.optString("submittedTo", "HOD Mechanical Engineering"),
                            status = obj.optString("status", "submitted"),
                            refNo = obj.optString("refNo", "MED-001"),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 8. Academic Tasks
            if (root.has("academic_tasks")) {
                val array = root.getJSONArray("academic_tasks")
                val list = mutableListOf<AcademicTaskEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AcademicTaskEntity(
                            id = obj.optLong("id", 0L),
                            description = obj.getString("description"),
                            priority = obj.optString("priority", "Medium"),
                            category = obj.optString("category", "Assignment"),
                            dueDate = obj.getString("dueDate"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            subjectId = if (obj.isNull("subjectId")) null else obj.optLong("subjectId"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) db.academicTaskDao().insertTasks(list)
            }

            // 9. Smart Auto-Prioritized To-Do Tasks
            if (root.has("todo_tasks")) {
                val array = root.getJSONArray("todo_tasks")
                val list = mutableListOf<TodoTaskEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TodoTaskEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            subjectId = if (obj.isNull("subjectId")) null else obj.optLong("subjectId"),
                            dueDateTimeMillis = obj.optLong("dueDateTimeMillis", System.currentTimeMillis()),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.todoDao().insertTodoTasks(list)
            }

            // 10. Extra Slots
            if (root.has("extra_slots")) {
                val array = root.getJSONArray("extra_slots")
                val list = mutableListOf<ExtraSlotEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ExtraSlotEntity(
                            id = obj.optLong("id", 0L),
                            date = obj.getString("date"),
                            subjectId = obj.getLong("subjectId"),
                            startTime = obj.getString("startTime"),
                            endTime = obj.getString("endTime"),
                            roomNo = obj.optString("roomNo", "Room"),
                            tag = obj.optString("tag", "Extra Class"),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.extraSlotDao().insertExtraSlots(list)
            }

            // 11. Reschedule Overrides
            if (root.has("reschedule_overrides")) {
                val array = root.getJSONArray("reschedule_overrides")
                val list = mutableListOf<RescheduleOverrideEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RescheduleOverrideEntity(
                            id = obj.optLong("id", 0L),
                            slotId = obj.getLong("slotId"),
                            date = obj.getString("date"),
                            newStartTime = obj.getString("newStartTime"),
                            newEndTime = obj.getString("newEndTime"),
                            newRoomNo = if (obj.isNull("newRoomNo")) null else obj.optString("newRoomNo"),
                            semesterId = obj.optLong("semesterId", 1L)
                        )
                    )
                }
                if (list.isNotEmpty()) db.rescheduleOverrideDao().insertOverrides(list)
            }

            // 12. Test Records
            if (root.has("test_records")) {
                val array = root.getJSONArray("test_records")
                val list = mutableListOf<TestRecordEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TestRecordEntity(
                            id = obj.optLong("id", 0L),
                            subjectId = obj.getLong("subjectId"),
                            date = obj.getString("date"),
                            slotId = if (obj.isNull("slotId")) null else obj.optLong("slotId"),
                            testName = obj.optString("testName", "Class Test"),
                            marksObtained = if (obj.isNull("marksObtained")) null else obj.optDouble("marksObtained"),
                            maxMarks = obj.optDouble("maxMarks", 20.0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) db.testRecordDao().insertTestRecords(list)
            }

            triggerSilentAutoBackup(enqueueCloudSync = triggerCloudSync)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearAttendanceLogs() = withContext(Dispatchers.IO) {
        db.attendanceLogDao().deleteAllLogs()
        db.dailyDayStatusDao().deleteAllDayStatuses()
        triggerSilentAutoBackup()
    }
}
