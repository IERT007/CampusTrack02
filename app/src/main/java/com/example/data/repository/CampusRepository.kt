package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.sample.IertDefaultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CampusRepository(private val db: AppDatabase) {

    val allSubjects: Flow<List<SubjectEntity>> = db.subjectDao().getAllSubjects()
    val allSlots: Flow<List<TimetableSlotEntity>> = db.timetableSlotDao().getAllSlots()
    val allLogs: Flow<List<AttendanceLogEntity>> = db.attendanceLogDao().getAllLogs()
    val allAssessments: Flow<List<AssessmentEntity>> = db.assessmentDao().getAllAssessments()
    val allDayStatuses: Flow<List<DailyDayStatusEntity>> = db.dailyDayStatusDao().getAllDayStatuses()
    val allMedicalLeaves: Flow<List<MedicalLeaveEntity>> = db.medicalLeaveDao().getAllMedicalLeaves()
    val allHolidayRanges: Flow<List<HolidayRangeEntity>> = db.holidayRangeDao().getAllHolidayRanges()
    val allTasks: Flow<List<AcademicTaskEntity>> = db.academicTaskDao().getAllTasks()

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

    suspend fun deleteSlotAttendance(date: String, slotId: Long) = withContext(Dispatchers.IO) {
        db.attendanceLogDao().deleteLogForSlot(date, slotId)
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

    suspend fun addTimetableSlot(slot: TimetableSlotEntity) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().insertSlot(slot)
    }

    suspend fun updateTimetableSlot(slot: TimetableSlotEntity) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().updateSlot(slot)
    }

    suspend fun deleteTimetableSlot(slot: TimetableSlotEntity) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().deleteSlot(slot)
    }

    suspend fun deleteTimetableSlotById(id: Long) = withContext(Dispatchers.IO) {
        db.timetableSlotDao().deleteSlotById(id)
    }

    suspend fun addHolidayRange(range: HolidayRangeEntity) = withContext(Dispatchers.IO) {
        db.holidayRangeDao().insertHolidayRange(range)
    }

    suspend fun updateHolidayRange(range: HolidayRangeEntity) = withContext(Dispatchers.IO) {
        db.holidayRangeDao().updateHolidayRange(range)
    }

    suspend fun deleteHolidayRange(id: Long) = withContext(Dispatchers.IO) {
        db.holidayRangeDao().deleteHolidayRangeById(id)
    }

    suspend fun addTask(task: AcademicTaskEntity) = withContext(Dispatchers.IO) {
        db.academicTaskDao().insertTask(task)
    }

    suspend fun updateTask(task: AcademicTaskEntity) = withContext(Dispatchers.IO) {
        db.academicTaskDao().updateTask(task)
    }

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        db.academicTaskDao().deleteTaskById(id)
    }

    suspend fun toggleTaskCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        db.academicTaskDao().updateTaskCompletion(id, completed)
    }

    suspend fun checkAndInitializeDefaultData() = withContext(Dispatchers.IO) {
        // App starts fresh with 0 historical logs, 0 attendance percentage, 0 fake assessments per specification
    }

    suspend fun preloadDefaultIertData() = withContext(Dispatchers.IO) {
        clearAllData()

        db.subjectDao().insertSubjects(IertDefaultData.defaultSubjects)
        db.timetableSlotDao().insertSlots(IertDefaultData.createDefaultSlots())
        db.assessmentDao().insertAssessments(IertDefaultData.createDefaultAssessments())
        // Clean start: 0 logs, 0 attendance records, 0 days status
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.subjectDao().deleteAllSubjects()
        db.timetableSlotDao().deleteAllSlots()
        db.attendanceLogDao().deleteAllLogs()
        db.assessmentDao().deleteAllAssessments()
        db.dailyDayStatusDao().deleteAllDayStatuses()
        db.medicalLeaveDao().deleteAllMedicalLeaves()
        db.holidayRangeDao().deleteAllHolidayRanges()
        db.academicTaskDao().deleteAllTasks()
    }

    // --- JSON Backup & Restore Engine (Local File Storage) ---
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "CampusTrack IERT")
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

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

        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("subjects") && !root.has("timetable_slots")) {
                return@withContext false
            }

            clearAllData()

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
                            reconciledTotalOffset = obj.optInt("reconciledTotalOffset", 0)
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
                            subjectId = obj.getLong("subjectId")
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
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
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
                            syllabusCoveragePercent = obj.optInt("syllabusCoveragePercent", 0)
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

            true
        } catch (_: Exception) {
            false
        }
    }
}
