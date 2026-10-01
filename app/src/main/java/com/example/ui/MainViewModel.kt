package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.CampusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class SubjectAttendanceStats(
    val subject: SubjectEntity,
    val attended: Int,
    val totalConducted: Int,
    val facultyCancelled: Int,
    val collegeOff: Int,
    val proxyCount: Int,
    val percentage: Double,
    val bunksAvailable: Int,
    val classesNeeded: Int
)

data class GlobalAttendanceSummary(
    val totalAttended: Int,
    val totalConducted: Int,
    val totalFacultyCancelled: Int,
    val totalCollegeOff: Int,
    val totalProxy: Int,
    val overallPercentage: Double,
    val atRiskSubjectCount: Int,
    val criticalSubjectCount: Int
)

data class SlotDisplayItem(
    val slot: TimetableSlotEntity,
    val subject: SubjectEntity?,
    val currentLog: AttendanceLogEntity?,
    val isOngoing: Boolean
)

data class CiaAssessmentBreakdown(
    val attendanceScore: Double, // max 10
    val sessionalsScore: Double, // max 20
    val classTestsScore: Double, // max 10
    val practicalWorkshopScore: Double, // max 10
    val totalInternalScore: Double // max 50
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CampusRepository
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val prefs = application.getSharedPreferences("caliper_settings", android.content.Context.MODE_PRIVATE)

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _hasAutoVault = MutableStateFlow(false)
    val hasAutoVault: StateFlow<Boolean> = _hasAutoVault.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(prefs.getBoolean("audio_effects_enabled", true))
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    private val _isHapticsEnabled = MutableStateFlow(prefs.getBoolean("haptic_feedback_enabled", true))
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("time_format_24h", false))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _selectedTheme = MutableStateFlow(prefs.getString("selected_theme", "AMOLED Dark") ?: "AMOLED Dark")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = CampusRepository(application, db)
        _hasAutoVault.value = repository.hasAutoVault()
        com.example.audio.CaliperSoundManager.init(application)
        com.example.audio.CaliperSoundManager.isAudioEnabled = _isAudioEnabled.value
        com.example.audio.CaliperHapticManager.isHapticsEnabled = _isHapticsEnabled.value
        com.example.notification.NotificationHelper.createNotificationChannels(application)
        com.example.notification.NotificationHelper.scheduleDailyMorningBriefing(application)
        viewModelScope.launch {
            repository.checkAndInitializeDefaultData()
        }
    }

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

    // Current holiday if selected date falls into a declared holiday / institutional off range
    val currentHoliday: StateFlow<HolidayRangeEntity?> = combine(
        selectedDate,
        holidayRanges
    ) { dateStr, holidays ->
        holidays.find { dateStr >= it.startDate && dateStr <= it.endDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Per-subject detailed attendance statistics with the strict mathematical formulas
    val subjectStats: StateFlow<List<SubjectAttendanceStats>> = combine(
        subjects,
        logs
    ) { subjectList, logList ->
        subjectList.map { subject ->
            val subLogs = logList.filter { it.subjectId == subject.id }

            // Logs marked attended (including proxies)
            val logAttended = subLogs.count { it.status == "attended" }
            val proxyCount = subLogs.count { it.status == "attended" && it.isProxy }
            val logBunked = subLogs.count { it.status == "bunked" }
            val facultyCancelled = subLogs.count { it.status == "cancelled_by_faculty" }
            val collegeOff = subLogs.count { it.status == "college_off" }

            // Conducted classes = attended + bunked (strictly excluded: faculty cancelled & college off)
            val baseConducted = logAttended + logBunked

            // Total official = app logs + reconciled offsets
            val totalConducted = max(0, baseConducted + subject.reconciledTotalOffset)
            val attended = max(0, logAttended + subject.reconciledAttendedOffset)

            val percentage = if (totalConducted > 0) {
                (attended.toDouble() / totalConducted.toDouble()) * 100.0
            } else {
                100.0
            }

            // Predictive bunk / catch-up calculation per engineering specification:
            // If % >= 75: Bunks Available = floor((Attended - (0.75 * Total)) / 0.75)
            // If % < 75: Classes Needed = ceil(((0.75 * Total) - Attended) / 0.25)
            val bunksAvailable = if (percentage >= 75.0) {
                val safe = floor((attended.toDouble() - (0.75 * totalConducted.toDouble())) / 0.75).toInt()
                max(0, safe)
            } else {
                0
            }

            val classesNeeded = if (percentage < 75.0) {
                val needed = ceil(((0.75 * totalConducted.toDouble()) - attended.toDouble()) / 0.25).toInt()
                max(1, needed)
            } else {
                0
            }

            SubjectAttendanceStats(
                subject = subject,
                attended = attended,
                totalConducted = totalConducted,
                facultyCancelled = facultyCancelled,
                collegeOff = collegeOff,
                proxyCount = proxyCount,
                percentage = percentage,
                bunksAvailable = bunksAvailable,
                classesNeeded = classesNeeded
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global summary across all subjects
    val globalSummary: StateFlow<GlobalAttendanceSummary> = subjectStats.map { statsList ->
        var totalAtt = 0
        var totalCond = 0
        var totalCan = 0
        var totalOff = 0
        var totalProxy = 0
        var atRisk = 0
        var critical = 0

        for (stat in statsList) {
            totalAtt += stat.attended
            totalCond += stat.totalConducted
            totalCan += stat.facultyCancelled
            totalOff += stat.collegeOff
            totalProxy += stat.proxyCount
            if (stat.percentage < 65.0) {
                critical++
            } else if (stat.percentage < 75.0) {
                atRisk++
            }
        }

        val overallPct = if (totalCond > 0) {
            (totalAtt.toDouble() / totalCond.toDouble()) * 100.0
        } else {
            100.0
        }

        GlobalAttendanceSummary(
            totalAttended = totalAtt,
            totalConducted = totalCond,
            totalFacultyCancelled = totalCan,
            totalCollegeOff = totalOff,
            totalProxy = totalProxy,
            overallPercentage = overallPct,
            atRiskSubjectCount = atRisk,
            criticalSubjectCount = critical
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GlobalAttendanceSummary(0, 0, 0, 0, 0, 100.0, 0, 0)
    )

    // Consecutive collegiate attendance days streak (days attended without bunks)
    val collegeStreak: StateFlow<Int> = combine(
        logs,
        dayStatuses,
        slots,
        holidayRanges
    ) { logList, statusList, slotList, holidayList ->
        calculateAttendanceStreak(logList, statusList, slotList, holidayList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun calculateAttendanceStreak(
        logList: List<AttendanceLogEntity>,
        statusList: List<DailyDayStatusEntity>,
        slotList: List<TimetableSlotEntity>,
        holidayList: List<HolidayRangeEntity>
    ): Int {
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today
        val todayStr = today.format(dateFormatter)
        val todayLogs = logList.filter { it.date == todayStr }

        if (todayLogs.any { it.status == "bunked" }) {
            return 0
        }
        if (todayLogs.any { it.status == "attended" } && !todayLogs.any { it.status == "bunked" }) {
            streak++
            checkDate = checkDate.minusDays(1)
        } else {
            checkDate = checkDate.minusDays(1)
        }

        for (i in 1..90) {
            val dateStr = checkDate.format(dateFormatter)
            val dayOfWeek = checkDate.dayOfWeek.value // 1=Mon..7=Sun

            val isSunday = (dayOfWeek == 7)
            val isHoliday = holidayList.any { h -> dateStr >= h.startDate && dateStr <= h.endDate }
            val dayStatus = statusList.find { it.date == dateStr }
            val isExplicitOff = (dayStatus?.leaveCategory in listOf("college_holiday", "strike"))

            if (isSunday || isHoliday || isExplicitOff) {
                checkDate = checkDate.minusDays(1)
                continue
            }

            val dayLogs = logList.filter { it.date == dateStr }
            val daySlots = slotList.filter { it.dayOfWeek == dayOfWeek }

            if (daySlots.isEmpty() && dayLogs.isEmpty()) {
                checkDate = checkDate.minusDays(1)
                continue
            }

            if (dayLogs.isEmpty()) {
                break
            }

            val hasBunks = dayLogs.any { it.status == "bunked" } || (dayStatus?.leaveCategory == "mass_bunk")
            val hasAttended = dayLogs.any { it.status == "attended" }

            if (hasBunks) {
                break
            } else if (hasAttended) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                val allCancelled = dayLogs.all { it.status == "cancelled_by_faculty" || it.status == "college_off" }
                if (allCancelled) {
                    checkDate = checkDate.minusDays(1)
                    continue
                } else {
                    break
                }
            }
        }
        return streak
    }

    // Today's display slots with live ongoing class indicator
    val currentDaySlots: StateFlow<List<SlotDisplayItem>> = combine(
        selectedDate,
        slots,
        subjects,
        logs
    ) { dateStr, slotList, subList, logList ->
        val localDate = try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val dayOfWeek = localDate.dayOfWeek.value // 1..7

        val isToday = (dateStr == LocalDate.now().format(dateFormatter))
        val nowTime = LocalTime.now()

        val daySlots = slotList.filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { it.startTime }

        val dayLogs = logList.filter { it.date == dateStr }

        daySlots.map { slot ->
            val sub = subList.find { it.id == slot.subjectId }
            val log = dayLogs.find { it.slotId == slot.id }

            var isOngoing = false
            if (isToday) {
                try {
                    val sTime = LocalTime.parse(slot.startTime)
                    val eTime = LocalTime.parse(slot.endTime)
                    isOngoing = (nowTime.isAfter(sTime) || nowTime == sTime) && nowTime.isBefore(eTime)
                } catch (_: Exception) {}
            }

            SlotDisplayItem(
                slot = slot,
                subject = sub,
                currentLog = log,
                isOngoing = isOngoing
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ad-hoc / extra classes for the selected date
    val currentDayExtraClasses: StateFlow<List<Pair<AttendanceLogEntity, SubjectEntity?>>> = combine(
        selectedDate,
        subjects,
        logs
    ) { dateStr, subList, logList ->
        logList.filter { it.date == dateStr && (it.isExtraClass || it.slotId == null) }
            .map { log ->
                val sub = subList.find { it.id == log.subjectId }
                Pair(log, sub)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected day's status (holiday / strike / mass bunk / normal)
    val selectedDayStatus: StateFlow<DailyDayStatusEntity?> = combine(
        selectedDate,
        dayStatuses
    ) { dateStr, statusList ->
        statusList.find { it.date == dateStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Continuous Internal Assessment (CIA) calculation
    val ciaBreakdown: StateFlow<CiaAssessmentBreakdown> = combine(
        globalSummary,
        assessments
    ) { summary, assessmentList ->
        // 1. Attendance component (10 Marks)
        val attScore = when {
            summary.overallPercentage >= 90.0 -> 10.0
            summary.overallPercentage >= 85.0 -> 8.5
            summary.overallPercentage >= 80.0 -> 7.0
            summary.overallPercentage >= 75.0 -> 5.0
            else -> 0.0
        }

        // 2. Sessionals component (20 Marks)
        val sessionalList = assessmentList.filter { it.type.startsWith("sessional") && it.obtainedMarks != null }
        val sessionalScore = if (sessionalList.isNotEmpty()) {
            val totalObtained = sessionalList.sumOf { it.obtainedMarks ?: 0.0 }
            val totalMax = sessionalList.sumOf { it.maxMarks }
            if (totalMax > 0) (totalObtained / totalMax) * 20.0 else 16.0
        } else {
            16.0
        }

        // 3. Class tests & assignments (10 Marks)
        val ctList = assessmentList.filter { (it.type == "ct" || it.type == "assignment") && it.obtainedMarks != null }
        val ctScore = if (ctList.isNotEmpty()) {
            val totalObt = ctList.sumOf { it.obtainedMarks ?: 0.0 }
            val totalMax = ctList.sumOf { it.maxMarks }
            if (totalMax > 0) (totalObt / totalMax) * 10.0 else 8.0
        } else {
            8.0
        }

        // 4. Practical / Workshop / Drawing (10 Marks)
        val practicalList = assessmentList.filter {
            (it.type == "drawing_sheet" || it.type == "workshop_job" || it.type.startsWith("viva"))
        }
        val practicalScore = if (practicalList.isNotEmpty()) {
            val evaluated = practicalList.filter { it.obtainedMarks != null }
            if (evaluated.isNotEmpty()) {
                val totalObt = evaluated.sumOf { it.obtainedMarks ?: 0.0 }
                val totalMax = evaluated.sumOf { it.maxMarks }
                (totalObt / totalMax) * 10.0
            } else {
                8.5
            }
        } else {
            8.5
        }

        val total = attScore + sessionalScore + ctScore + practicalScore

        CiaAssessmentBreakdown(
            attendanceScore = attScore,
            sessionalsScore = sessionalScore,
            classTestsScore = ctScore,
            practicalWorkshopScore = practicalScore,
            totalInternalScore = total
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CiaAssessmentBreakdown(10.0, 16.0, 8.0, 8.5, 42.5)
    )

    // User actions
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        com.example.audio.CaliperSoundManager.playSnap()
        com.example.audio.CaliperHapticManager.tick(getApplication())
    }

    fun stepDay(delta: Long) {
        val current = try {
            LocalDate.parse(_selectedDate.value, dateFormatter)
        } catch (_: Exception) {
            LocalDate.now()
        }
        _selectedDate.value = current.plusDays(delta).format(dateFormatter)
        com.example.audio.CaliperSoundManager.playSnap()
        com.example.audio.CaliperHapticManager.tick(getApplication())
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now().format(dateFormatter)
        com.example.audio.CaliperSoundManager.playSnap()
        com.example.audio.CaliperHapticManager.tick(getApplication())
    }

    fun setSlotAttendance(
        slotId: Long,
        subjectId: Long,
        status: String,
        isProxy: Boolean = false,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.setSlotAttendance(
                date = _selectedDate.value,
                slotId = slotId,
                subjectId = subjectId,
                status = status,
                isProxy = isProxy,
                isExtraClass = false,
                notes = notes
            )
            when (status) {
                "attended" -> {
                    com.example.audio.CaliperSoundManager.playSuccess()
                    com.example.audio.CaliperHapticManager.successClick(getApplication())
                }
                "bunked" -> {
                    com.example.audio.CaliperSoundManager.playThud()
                    com.example.audio.CaliperHapticManager.bunkDoubleTap(getApplication())
                }
                else -> {
                    com.example.audio.CaliperSoundManager.playAlert()
                    com.example.audio.CaliperHapticManager.tick(getApplication())
                }
            }
        }
    }

    fun undoSlotAttendance(slotId: Long) {
        viewModelScope.launch {
            repository.deleteSlotAttendance(_selectedDate.value, slotId)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun updateTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch {
            repository.updateTimetableSlot(slot)
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun deleteTimetableSlot(slotId: Long) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(slotId)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.bunkDoubleTap(getApplication())
        }
    }

    fun setAudioEnabled(enabled: Boolean) {
        _isAudioEnabled.value = enabled
        com.example.audio.CaliperSoundManager.isAudioEnabled = enabled
        prefs.edit().putBoolean("audio_effects_enabled", enabled).apply()
        if (enabled) com.example.audio.CaliperSoundManager.playSuccess()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        com.example.audio.CaliperHapticManager.isHapticsEnabled = enabled
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
        if (enabled) com.example.audio.CaliperHapticManager.successClick(getApplication())
    }

    fun set24HourFormat(enabled: Boolean) {
        _is24HourFormat.value = enabled
        prefs.edit().putBoolean("time_format_24h", enabled).apply()
        com.example.audio.CaliperSoundManager.playSnap()
        com.example.audio.CaliperHapticManager.tick(getApplication())
    }

    fun setSelectedTheme(theme: String) {
        _selectedTheme.value = theme
        prefs.edit().putString("selected_theme", theme).apply()
        com.example.audio.CaliperSoundManager.playSnap()
        com.example.audio.CaliperHapticManager.tick(getApplication())
    }

    fun markWholeDayPresent() {
        viewModelScope.launch {
            val items = currentDaySlots.value
            val slotEntities = items.map { it.slot }
            repository.markWholeDayPresent(_selectedDate.value, slotEntities)
        }
    }

    fun markMassBunkOrOff(category: String) {
        viewModelScope.launch {
            val items = currentDaySlots.value
            val slotEntities = items.map { it.slot }
            repository.markMassBunkOrOff(_selectedDate.value, slotEntities, category)
        }
    }

    fun applyHolidayToCurrentDay(title: String) {
        viewModelScope.launch {
            val items = currentDaySlots.value
            val slotEntities = items.map { it.slot }
            repository.markMassBunkOrOff(_selectedDate.value, slotEntities, "college_holiday")
        }
    }

    fun logExtraClass(subjectId: Long, status: String, isProxy: Boolean, notes: String) {
        viewModelScope.launch {
            repository.logAdHocExtraClass(_selectedDate.value, subjectId, status, isProxy, notes)
        }
    }

    fun reconcileSubject(subjectId: Long, officialAttended: Int, officialTotal: Int) {
        viewModelScope.launch {
            val currentStat = subjectStats.value.find { it.subject.id == subjectId }
            val currentAttended = currentStat?.attended ?: 0
            val currentTotal = currentStat?.totalConducted ?: 0
            repository.reconcileSubject(subjectId, officialAttended, officialTotal, currentAttended, currentTotal)
        }
    }

    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            if (subject.id == 0L) {
                repository.addSubject(subject)
            } else {
                repository.updateSubject(subject)
            }
        }
    }

    fun saveSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch {
            if (slot.id == 0L) {
                repository.addTimetableSlot(slot)
            } else {
                repository.updateTimetableSlot(slot)
            }
        }
    }

    fun deleteSlot(id: Long) {
        viewModelScope.launch {
            repository.deleteTimetableSlotById(id)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun saveAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            if (assessment.id == 0L) {
                repository.addAssessment(assessment)
            } else {
                repository.updateAssessment(assessment)
            }
        }
    }

    fun updateAssessmentStatus(id: Long, newStatus: String) {
        viewModelScope.launch {
            val current = assessments.value.find { it.id == id }
            if (current != null) {
                repository.updateAssessment(current.copy(status = newStatus))
            }
        }
    }

    fun deleteAssessment(id: Long) {
        viewModelScope.launch {
            repository.deleteAssessment(id)
        }
    }

    fun saveMedicalLeave(leave: MedicalLeaveEntity) {
        viewModelScope.launch {
            if (leave.id == 0L) {
                repository.addMedicalLeave(leave)
            } else {
                repository.updateMedicalLeave(leave)
            }
        }
    }

    fun deleteMedicalLeave(id: Long) {
        viewModelScope.launch {
            repository.deleteMedicalLeave(id)
        }
    }

    fun saveHolidayRange(range: HolidayRangeEntity) {
        viewModelScope.launch {
            if (range.id == 0L) {
                repository.addHolidayRange(range)
            } else {
                repository.updateHolidayRange(range)
            }
        }
    }

    fun deleteHolidayRange(id: Long) {
        viewModelScope.launch {
            repository.deleteHolidayRange(id)
        }
    }

    fun saveTask(task: AcademicTaskEntity) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.addTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun toggleTaskCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(id, completed)
        }
    }

    suspend fun exportBackupJson(): String {
        return repository.exportToJson()
    }

    fun exportBackupJson(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportToJson()
                onResult(json)
            } catch (_: Exception) {
                onResult(null)
            }
        }
    }

    fun exportBackupToFile(context: android.content.Context, uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray())
                }
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun importBackupFromFile(context: android.content.Context, uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
                if (json != null) {
                    val success = repository.importFromJson(json)
                    onResult(success)
                } else {
                    onResult(false)
                }
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return repository.importFromJson(jsonString)
    }

    fun preloadDefaultIertData() {
        preloadDefaultData()
    }

    fun restoreFromAutoVault(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreFromAutoVault()
            _hasAutoVault.value = repository.hasAutoVault()
            onResult(success)
        }
    }

    fun scheduleAllLectureAlerts() {
        viewModelScope.launch {
            val allSlots = slots.value
            val allSubjects = subjects.value
            for (slot in allSlots) {
                val sub = allSubjects.find { it.id == slot.subjectId }
                com.example.notification.NotificationHelper.schedulePreLectureAlert(
                    context = getApplication(),
                    slotId = slot.id,
                    subjectName = sub?.name ?: "Lecture",
                    roomNo = slot.roomNo,
                    startTimeStr = slot.startTime,
                    dayOfWeek = slot.dayOfWeek
                )
            }
        }
    }

    fun scheduleAllDeadlineAlerts() {
        viewModelScope.launch {
            val allAssessments = assessments.value
            for (a in allAssessments) {
                if (a.status == "pending") {
                    com.example.notification.NotificationHelper.scheduleDeadlineReminders(
                        context = getApplication(),
                        assessmentId = a.id,
                        title = a.title,
                        dueDateStr = a.dueDate,
                        dueTimeStr = a.dueTime
                    )
                }
            }
        }
    }

    fun preloadDefaultData() {
        viewModelScope.launch {
            repository.preloadDefaultIertData()
            scheduleAllLectureAlerts()
            scheduleAllDeadlineAlerts()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
