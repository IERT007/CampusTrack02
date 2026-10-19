package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.DataStoreManager
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
import com.example.data.repository.CampusRepository
import com.example.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

data class SlotDisplayItem(
    val slot: TimetableSlotEntity,
    val subject: SubjectEntity,
    val currentLog: AttendanceLogEntity?,
    val isOngoing: Boolean
)

data class SubjectAttendanceStats(
    val subject: SubjectEntity,
    val attended: Int,
    val totalConducted: Int,
    val bunked: Int,
    val facultyCancelled: Int,
    val collegeOff: Int,
    val percentage: Double,
    val bunksAvailable: Int,
    val classesNeeded: Int,
    val proxyCount: Int = 0
)

data class GlobalAttendanceSummary(
    val totalAttended: Int,
    val totalConducted: Int,
    val totalFacultyCancelled: Int,
    val totalCollegeOff: Int,
    val totalProxy: Int,
    val overallPercentage: Double,
    val criticalSubjectCount: Int,
    val atRiskSubjectCount: Int
)

data class CiaAssessmentBreakdown(
    val attendanceScore: Double,
    val classTestsScore: Double,
    val sessionalsScore: Double,
    val practicalWorkshopScore: Double,
    val totalInternalScore: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = CampusRepository(application, db)
    private val dataStoreManager = DataStoreManager(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("caliper_os_prefs", Context.MODE_PRIVATE)
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isMonochromeMode = MutableStateFlow(false)
    val isMonochromeMode: StateFlow<Boolean> = _isMonochromeMode.asStateFlow()

    private val _selectedTheme = MutableStateFlow("Obsidian")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _isHapticsEnabled = MutableStateFlow(true)
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    private val _semesterStartDate = MutableStateFlow("2026-01-19")
    val semesterStartDate: StateFlow<String> = _semesterStartDate.asStateFlow()

    private val _semesterEndDate = MutableStateFlow("2026-05-15")
    val semesterEndDate: StateFlow<String> = _semesterEndDate.asStateFlow()

    private val _hasAutoVault = MutableStateFlow(false)
    val hasAutoVault: StateFlow<Boolean> = _hasAutoVault.asStateFlow()

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val slots: StateFlow<List<TimetableSlotEntity>> = repository.allSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<AttendanceLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assessments: StateFlow<List<AssessmentEntity>> = repository.allAssessments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayStatuses: StateFlow<List<DailyDayStatusEntity>> = repository.allDayStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicalLeaves: StateFlow<List<MedicalLeaveEntity>> = repository.allMedicalLeaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val holidayRanges: StateFlow<List<HolidayRangeEntity>> = repository.allHolidayRanges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<AcademicTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<QuickNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testRecords: StateFlow<List<TestRecordEntity>> = repository.allTestRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayStatus: StateFlow<DailyDayStatusEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getDayStatus(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentDayActivities: StateFlow<List<CustomActivityEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getActivitiesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentHoliday: StateFlow<HolidayRangeEntity?> = combine(
        _selectedDate,
        holidayRanges
    ) { date, holidays ->
        holidays.firstOrNull { date >= it.startDate && date <= it.endDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentDaySlots: StateFlow<List<SlotDisplayItem>> = combine(
        _selectedDate,
        subjects,
        slots,
        logs
    ) { dateStr, subjectList, slotList, logList ->
        val date = runCatching { LocalDate.parse(dateStr, dateFormatter) }.getOrDefault(LocalDate.now())
        val dayOfWeek = date.dayOfWeek.value
        val daySlots = slotList.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
        val dayLogs = logList.filter { it.date == dateStr && !it.isExtraClass }
        val subjectMap = subjectList.associateBy { it.id }
        val now = LocalTime.now()
        val isToday = date == LocalDate.now()

        daySlots.mapNotNull { slot ->
            val subj = subjectMap[slot.subjectId] ?: return@mapNotNull null
            val currentLog = dayLogs.firstOrNull { it.slotId == slot.id }
            val ongoing = if (isToday) {
                runCatching {
                    val start = LocalTime.parse(slot.startTime)
                    val end = LocalTime.parse(slot.endTime)
                    !now.isBefore(start) && !now.isAfter(end)
                }.getOrDefault(false)
            } else false

            SlotDisplayItem(
                slot = slot,
                subject = subj,
                currentLog = currentLog,
                isOngoing = ongoing
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySlots: StateFlow<List<SlotDisplayItem>> get() = currentDaySlots

    val currentDayExtraClasses: StateFlow<List<Pair<AttendanceLogEntity, SubjectEntity>>> = combine(
        _selectedDate,
        subjects,
        logs
    ) { dateStr, subjectList, logList ->
        val subjectMap = subjectList.associateBy { it.id }
        logList
            .filter { it.date == dateStr && it.isExtraClass }
            .mapNotNull { log ->
                val subj = subjectMap[log.subjectId] ?: return@mapNotNull null
                log to subj
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectStats: StateFlow<List<SubjectAttendanceStats>> = combine(
        subjects,
        logs
    ) { subjectList, logList ->
        subjectList.map { subj ->
            val subjLogs = logList.filter { it.subjectId == subj.id }
            val attendedLogs = subjLogs.count { it.status == "PRESENT" }
            val bunkedLogs = subjLogs.count { it.status == "ABSENT" }
            val facultyCancelled = subjLogs.count { it.status == "FACULTY_ABSENT" }
            val collegeOff = subjLogs.count { it.status == "COLLEGE_OFF" }
            val proxyCount = subjLogs.count { it.status == "PRESENT" && it.isProxy }

            val attended = (attendedLogs + subj.attendedOffset).coerceAtLeast(0)
            val totalConducted = (attendedLogs + bunkedLogs + subj.totalOffset).coerceAtLeast(attended)
            val bunked = (totalConducted - attended).coerceAtLeast(0)

            val percentage = if (totalConducted > 0) {
                (attended.toDouble() / totalConducted.toDouble()) * 100.0
            } else {
                100.0
            }

            val targetRatio = subj.targetPercentage / 100.0
            val bunksAvailable = if (totalConducted > 0 && percentage >= subj.targetPercentage) {
                floor((attended - targetRatio * totalConducted) / targetRatio).toInt().coerceAtLeast(0)
            } else {
                0
            }

            val classesNeeded = if (totalConducted > 0 && percentage < subj.targetPercentage) {
                ceil((targetRatio * totalConducted - attended) / (1.0 - targetRatio)).toInt().coerceAtLeast(0)
            } else {
                0
            }

            SubjectAttendanceStats(
                subject = subj,
                attended = attended,
                totalConducted = totalConducted,
                bunked = bunked,
                facultyCancelled = facultyCancelled,
                collegeOff = collegeOff,
                percentage = percentage,
                bunksAvailable = bunksAvailable,
                classesNeeded = classesNeeded,
                proxyCount = proxyCount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalSummary: StateFlow<GlobalAttendanceSummary> = combine(
        subjectStats,
        logs
    ) { statsList, logList ->
        val totalAttended = statsList.sumOf { it.attended }
        val totalConducted = statsList.sumOf { it.totalConducted }
        val totalFacultyCancelled = logList.count { it.status == "FACULTY_ABSENT" }
        val totalCollegeOff = logList.count { it.status == "COLLEGE_OFF" }
        val totalProxy = logList.count { it.status == "PRESENT" && it.isProxy }
        val overallPercentage = if (totalConducted > 0) {
            (totalAttended.toDouble() / totalConducted.toDouble()) * 100.0
        } else {
            100.0
        }
        val criticalCount = statsList.count { it.totalConducted > 0 && it.percentage < 75.0 }
        val atRiskCount = statsList.count { it.totalConducted > 0 && it.percentage in 75.0..79.99 }

        GlobalAttendanceSummary(
            totalAttended = totalAttended,
            totalConducted = totalConducted,
            totalFacultyCancelled = totalFacultyCancelled,
            totalCollegeOff = totalCollegeOff,
            totalProxy = totalProxy,
            overallPercentage = overallPercentage,
            criticalSubjectCount = criticalCount,
            atRiskSubjectCount = atRiskCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GlobalAttendanceSummary(0, 0, 0, 0, 0, 100.0, 0, 0)
    )

    val collegeStreak: StateFlow<Int> = combine(
        logs,
        slots,
        dayStatuses,
        holidayRanges
    ) { logList, slotList, statusList, holidays ->
        calculateAttendanceStreak(logList, slotList, statusList, holidays)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val ciaBreakdown: StateFlow<CiaAssessmentBreakdown> = combine(
        globalSummary,
        testRecords
    ) { summary, records ->
        val attendanceScore = when {
            summary.overallPercentage >= 90.0 -> 10.0
            summary.overallPercentage >= 85.0 -> 9.0
            summary.overallPercentage >= 80.0 -> 8.0
            summary.overallPercentage >= 75.0 -> 7.5
            summary.overallPercentage >= 70.0 -> 6.0
            else -> 4.0
        }
        val avgTestRatio = if (records.isNotEmpty()) {
            records.map { if (it.maxMarks > 0) (it.marksObtained / it.maxMarks).coerceIn(0.0, 1.0) else 0.8 }.average()
        } else 0.82
        val classTestsScore = avgTestRatio * 10.0
        val sessionalsScore = avgTestRatio * 20.0
        val practicalWorkshopScore = 9.0
        val totalInternalScore = attendanceScore + classTestsScore + sessionalsScore + practicalWorkshopScore

        CiaAssessmentBreakdown(
            attendanceScore = attendanceScore,
            classTestsScore = classTestsScore,
            sessionalsScore = sessionalsScore,
            practicalWorkshopScore = practicalWorkshopScore,
            totalInternalScore = totalInternalScore
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CiaAssessmentBreakdown(8.0, 8.2, 16.4, 9.0, 41.6)
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataLoaded()
            val vaultFile = File(getApplication<Application>().filesDir, "caliper_auto_vault.json")
            _hasAutoVault.value = vaultFile.exists()
        }
        viewModelScope.launch {
            dataStoreManager.isMonochromeFlow.collect { _isMonochromeMode.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.selectedThemeFlow.collect { _selectedTheme.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.is24HourFormatFlow.collect { _is24HourFormat.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.isHapticsEnabledFlow.collect {
                _isHapticsEnabled.value = it
                CaliperHapticManager.isHapticsEnabled = it
            }
        }
        viewModelScope.launch {
            dataStoreManager.isAudioEnabledFlow.collect {
                _isAudioEnabled.value = it
                CaliperSoundManager.isAudioEnabled = it
            }
        }
        viewModelScope.launch {
            dataStoreManager.semesterStartDateFlow.collect { _semesterStartDate.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.semesterEndDateFlow.collect { _semesterEndDate.value = it }
        }
    }

    private fun calculateAttendanceStreak(
        logList: List<AttendanceLogEntity>,
        slotList: List<TimetableSlotEntity>,
        statusList: List<DailyDayStatusEntity>,
        holidays: List<HolidayRangeEntity>
    ): Int {
        if (logList.isEmpty()) return 0
        val logsByDate = logList.groupBy { it.date }
        var streak = 0
        var cursor = LocalDate.now()
        for (i in 0 until 120) {
            val dateStr = cursor.format(dateFormatter)
            val isWeekend = cursor.dayOfWeek.value >= 6
            val isHoliday = holidays.any { dateStr >= it.startDate && dateStr <= it.endDate }
            val dayStatus = statusList.firstOrNull { it.date == dateStr }
            val dayLogs = logsByDate[dateStr].orEmpty()

            if (dayStatus?.status == "MASS_BUNK") {
                break
            } else if (dayLogs.isNotEmpty()) {
                val anyAbsent = dayLogs.any { it.status == "ABSENT" }
                val anyPresent = dayLogs.any { it.status == "PRESENT" }
                if (anyAbsent) {
                    break
                } else if (anyPresent) {
                    streak++
                }
            } else if (!isWeekend && !isHoliday && dayStatus == null && i > 0) {
                val hasScheduled = slotList.any { it.dayOfWeek == cursor.dayOfWeek.value }
                if (hasScheduled) break
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun stepDay(daysDelta: Long) {
        val current = runCatching { LocalDate.parse(_selectedDate.value, dateFormatter) }
            .getOrDefault(LocalDate.now())
        _selectedDate.value = current.plusDays(daysDelta).format(dateFormatter)
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now().format(dateFormatter)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMonochromeMode(enabled: Boolean) {
        _isMonochromeMode.value = enabled
        viewModelScope.launch { dataStoreManager.setMonochromeMode(enabled) }
    }

    fun setSelectedTheme(theme: String) {
        _selectedTheme.value = theme
        viewModelScope.launch { dataStoreManager.setSelectedTheme(theme) }
    }

    fun set24HourFormat(enabled: Boolean) {
        _is24HourFormat.value = enabled
        viewModelScope.launch { dataStoreManager.set24HourFormat(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        CaliperHapticManager.isHapticsEnabled = enabled
        viewModelScope.launch { dataStoreManager.setHapticsEnabled(enabled) }
    }

    fun setAudioEnabled(enabled: Boolean) {
        _isAudioEnabled.value = enabled
        CaliperSoundManager.isAudioEnabled = enabled
        viewModelScope.launch { dataStoreManager.setAudioEnabled(enabled) }
    }

    fun setSemesterDates(start: String, end: String) {
        _semesterStartDate.value = start
        _semesterEndDate.value = end
        viewModelScope.launch { dataStoreManager.setSemesterDates(start, end) }
    }

    fun setSlotAttendance(
        slotId: Long,
        subjectId: Long,
        status: String,
        isProxy: Boolean = false,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.logSlotAttendance(date, slotId, subjectId, status, isProxy, notes)
            triggerAutoBackup()
        }
    }

    fun undoSlotAttendance(slotId: Long) {
        viewModelScope.launch {
            repository.undoSlotAttendance(_selectedDate.value, slotId)
            triggerAutoBackup()
        }
    }

    fun markWholeDayPresent() {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.setDailyDayStatus(DailyDayStatusEntity(date = date, wentToCollege = true, leaveCategory = "NONE"))
            currentDaySlots.value.forEach { item ->
                repository.logSlotAttendance(
                    date = date,
                    slotId = item.slot.id,
                    subjectId = item.subject.id,
                    status = "PRESENT",
                    isProxy = false,
                    notes = item.currentLog?.notes
                )
            }
            triggerAutoBackup()
        }
    }

    fun markMassBunkOrOff(mode: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val statusStr = if (mode == "MASS_BUNK") "ABSENT" else "COLLEGE_OFF"
            repository.setDailyDayStatus(DailyDayStatusEntity(date = date, wentToCollege = false, leaveCategory = mode))
            currentDaySlots.value.forEach { item ->
                repository.logSlotAttendance(
                    date = date,
                    slotId = item.slot.id,
                    subjectId = item.subject.id,
                    status = statusStr,
                    isProxy = false,
                    notes = mode
                )
            }
            triggerAutoBackup()
        }
    }

    fun applyHolidayToCurrentDay(holidayTitle: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.setDailyDayStatus(
                DailyDayStatusEntity(date = date, wentToCollege = false, leaveCategory = "HOLIDAY", notes = holidayTitle)
            )
            currentDaySlots.value.forEach { item ->
                repository.logSlotAttendance(
                    date = date,
                    slotId = item.slot.id,
                    subjectId = item.subject.id,
                    status = "COLLEGE_OFF",
                    isProxy = false,
                    notes = holidayTitle
                )
            }
            triggerAutoBackup()
        }
    }

    fun logExtraClass(
        subjectId: Long,
        status: String,
        isProxy: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.logExtraClass(_selectedDate.value, subjectId, status, isProxy, notes)
            triggerAutoBackup()
        }
    }

    fun saveCustomActivity(
        title: String,
        category: String,
        startTime: String,
        endTime: String,
        notes: String?,
        countsAsAttendance: Boolean
    ) {
        viewModelScope.launch {
            repository.saveCustomActivity(
                CustomActivityEntity(
                    date = _selectedDate.value,
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    location = category,
                    status = notes ?: "SCHEDULED",
                    countsTowardsAttendance = countsAsAttendance
                )
            )
            triggerAutoBackup()
        }
    }

    fun deleteCustomActivity(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomActivity(id)
            triggerAutoBackup()
        }
    }

    fun saveSlotTestRecord(
        subjectId: Long,
        date: String,
        slotId: Long?,
        title: String,
        marksObtained: Double,
        maxMarks: Double
    ) {
        viewModelScope.launch {
            repository.saveTestRecord(
                TestRecordEntity(
                    subjectId = subjectId,
                    date = date,
                    slotId = slotId,
                    testName = title,
                    marksObtained = marksObtained,
                    maxMarks = maxMarks
                )
            )
            triggerAutoBackup()
        }
    }

    fun deleteTestRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteTestRecord(id)
            triggerAutoBackup()
        }
    }

    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.saveSubject(subject)
            triggerAutoBackup()
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            triggerAutoBackup()
        }
    }

    fun deleteSubjectCascade(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubjectCascade(subject)
            triggerAutoBackup()
        }
    }

    fun reconcileSubject(subjectId: Long, targetAttended: Int, targetConducted: Int) {
        viewModelScope.launch {
            val subjLogs = logs.value.filter { it.subjectId == subjectId }
            val loggedAttended = subjLogs.count { it.status == "PRESENT" }
            val loggedConducted = subjLogs.count { it.status == "PRESENT" || it.status == "ABSENT" }
            val attendedOffset = targetAttended - loggedAttended
            val totalOffset = targetConducted - loggedConducted
            repository.reconcileSubject(subjectId, attendedOffset, totalOffset)
            triggerAutoBackup()
        }
    }

    fun saveSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch {
            repository.saveSlot(slot)
            scheduleAllLectureAlerts()
            triggerAutoBackup()
        }
    }

    fun updateTimetableSlot(slot: TimetableSlotEntity) {
        saveSlot(slot)
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch {
            repository.deleteSlot(slotId)
            triggerAutoBackup()
        }
    }

    fun deleteTimetableSlot(slotId: Long) {
        deleteSlot(slotId)
    }

    fun saveAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            repository.saveAssessment(assessment)
            scheduleAllDeadlineAlerts()
            triggerAutoBackup()
        }
    }

    fun updateAssessmentStatus(id: Long, status: String) {
        viewModelScope.launch {
            val existing = assessments.value.firstOrNull { it.id == id } ?: return@launch
            repository.saveAssessment(existing.copy(status = status))
            triggerAutoBackup()
        }
    }

    fun deleteAssessment(id: Long) {
        viewModelScope.launch {
            repository.deleteAssessment(id)
            triggerAutoBackup()
        }
    }

    fun saveMedicalLeave(leave: MedicalLeaveEntity) {
        viewModelScope.launch {
            repository.saveMedicalLeave(leave)
            triggerAutoBackup()
        }
    }

    fun deleteMedicalLeave(id: Long) {
        viewModelScope.launch {
            repository.deleteMedicalLeave(id)
            triggerAutoBackup()
        }
    }

    fun saveHolidayRange(holiday: HolidayRangeEntity) {
        viewModelScope.launch {
            repository.saveHolidayRange(holiday)
            triggerAutoBackup()
        }
    }

    fun deleteHolidayRange(id: Long) {
        viewModelScope.launch {
            repository.deleteHolidayRange(id)
            triggerAutoBackup()
        }
    }

    fun saveTask(task: AcademicTaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task)
            triggerAutoBackup()
        }
    }

    fun addTask(task: AcademicTaskEntity) {
        saveTask(task)
    }

    fun toggleTaskCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(id, completed)
            triggerAutoBackup()
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
            triggerAutoBackup()
        }
    }

    fun addNote(title: String, content: String, subjectCode: String?, colorHex: String) {
        viewModelScope.launch {
            repository.saveNote(
                QuickNoteEntity(
                    title = title,
                    content = content,
                    category = colorHex,
                    subjectCode = subjectCode
                )
            )
            triggerAutoBackup()
        }
    }

    fun updateNote(note: QuickNoteEntity) {
        viewModelScope.launch {
            repository.saveNote(note)
            triggerAutoBackup()
        }
    }

    fun toggleNotePin(id: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.toggleNotePin(id, isPinned)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            triggerAutoBackup()
        }
    }

    fun preloadDefaultData() {
        viewModelScope.launch {
            repository.seedDefaultData()
            triggerAutoBackup()
        }
    }

    fun preloadDefaultIertData() {
        preloadDefaultData()
    }

    fun clearAttendanceLogs() {
        viewModelScope.launch {
            repository.clearAllAttendanceLogs()
            triggerAutoBackup()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            triggerAutoBackup()
        }
    }

    fun scheduleAllLectureAlerts() {
        val context = getApplication<Application>()
        val subjMap = subjects.value.associateBy { it.id }
        slots.value.forEach { slot ->
            val subj = subjMap[slot.subjectId] ?: return@forEach
            NotificationHelper.scheduleWeeklyPreLectureAlert(
                context = context,
                slotId = slot.id,
                dayOfWeek = slot.dayOfWeek,
                startTime = slot.startTime,
                subjectName = subj.name,
                room = slot.roomNumber ?: subj.roomNumber
            )
        }
    }

    fun scheduleAllDeadlineAlerts() {
        val context = getApplication<Application>()
        assessments.value.forEach { assessment ->
            NotificationHelper.scheduleAssessmentDeadlineAlerts(
                context = context,
                assessmentId = assessment.id,
                title = assessment.title,
                category = assessment.category,
                dueDate = assessment.dueDate
            )
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 4)
        root.put("semesterStartDate", _semesterStartDate.value)
        root.put("semesterEndDate", _semesterEndDate.value)

        val subjectsArr = JSONArray()
        subjects.value.forEach { s ->
            subjectsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("code", s.code)
                    put("name", s.name)
                    put("facultyName", s.facultyName)
                    put("roomNumber", s.roomNumber)
                    put("type", s.type)
                    put("strictness", s.strictness)
                    put("targetPercentage", s.targetPercentage)
                    put("attendedOffset", s.attendedOffset)
                    put("totalOffset", s.totalOffset)
                    put("credits", s.credits)
                }
            )
        }
        root.put("subjects", subjectsArr)

        val logsArr = JSONArray()
        logs.value.forEach { l ->
            logsArr.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("date", l.date)
                    put("slotId", l.slotId ?: -1L)
                    put("subjectId", l.subjectId)
                    put("status", l.status)
                    put("isProxy", l.isProxy)
                    put("isExtraClass", l.isExtraClass)
                    put("notes", l.notes ?: "")
                }
            )
        }
        root.put("logs", logsArr)
        root.toString(2)
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(exportBackupJson())
        }
    }

    fun exportBackupToFile(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val json = exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    suspend fun importBackupJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonStr)
            val start = root.optString("semesterStartDate", _semesterStartDate.value)
            val end = root.optString("semesterEndDate", _semesterEndDate.value)
            setSemesterDates(start, end)
            true
        }.getOrDefault(false)
    }

    fun importBackupFromFile(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@runCatching false
                importBackupJson(json)
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    fun triggerAutoBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = exportBackupJson()
                val vaultFile = File(getApplication<Application>().filesDir, "caliper_auto_vault.json")
                vaultFile.writeText(json)
                _hasAutoVault.value = true
            }
        }
    }

    fun restoreFromAutoVault(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val vaultFile = File(getApplication<Application>().filesDir, "caliper_auto_vault.json")
                if (!vaultFile.exists()) return@runCatching false
                importBackupJson(vaultFile.readText())
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }
}
