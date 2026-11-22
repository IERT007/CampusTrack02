package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloud.SupabaseSyncManager
import com.example.data.DataStoreManager
import com.example.data.SupabaseSession
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.CampusRepository
import com.example.update.OtaCheckResult
import com.example.update.OtaUpdateManager
import com.example.update.ReleaseInfo
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
    val classesNeeded: Int,
    val bunked: Int = max(0, totalConducted - attended)
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
    val isOngoing: Boolean,
    val isRescheduled: Boolean = false,
    val originalTime: String? = null,
    val isDateSpecificExtra: Boolean = false,
    val extraSlotId: Long? = null
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
    private val dataStoreManager = DataStoreManager(application)
    private val supabaseSyncManager = SupabaseSyncManager(application, dataStoreManager)
    private val otaUpdateManager = OtaUpdateManager(application, dataStoreManager)

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _hasAutoVault = MutableStateFlow(false)
    val hasAutoVault: StateFlow<Boolean> = _hasAutoVault.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(prefs.getBoolean("audio_effects_enabled", true))
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    private val _isHapticsEnabled = MutableStateFlow(prefs.getBoolean("haptic_feedback_enabled", true))
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("time_format_24h", false))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _isMonochromeMode = MutableStateFlow(prefs.getBoolean("is_monochrome_mode", false))
    val isMonochromeMode: StateFlow<Boolean> = _isMonochromeMode.asStateFlow()

    private val _selectedTheme = MutableStateFlow(prefs.getString("selected_theme", "Normal") ?: "Normal")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _themePreset = MutableStateFlow(
        prefs.getString("theme_preset_key", DataStoreManager.DEFAULT_THEME_PRESET)
            ?: DataStoreManager.DEFAULT_THEME_PRESET
    )
    val themePreset: StateFlow<String> = _themePreset.asStateFlow()

    // --- Supabase Cloud Sync UI State ---
    val isSupabaseConfigured: Boolean = supabaseSyncManager.isConfigured()

    val supabaseSession: StateFlow<SupabaseSession?> = dataStoreManager.supabaseSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cloudLastSyncedAt: StateFlow<String> = dataStoreManager.supabaseLastSyncedAtFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _cloudSyncBusy = MutableStateFlow(false)
    val cloudSyncBusy: StateFlow<Boolean> = _cloudSyncBusy.asStateFlow()

    private val _cloudSyncStatusMessage = MutableStateFlow<String?>(null)
    val cloudSyncStatusMessage: StateFlow<String?> = _cloudSyncStatusMessage.asStateFlow()

    // --- GitHub Releases OTA Updater UI State ---
    private val _availableUpdate = MutableStateFlow<ReleaseInfo?>(null)
    val availableUpdate: StateFlow<ReleaseInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow(0f)
    val updateDownloadProgress: StateFlow<Float> = _updateDownloadProgress.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow<String?>(null)
    val updateStatusMessage: StateFlow<String?> = _updateStatusMessage.asStateFlow()

    private val _semesterStartDate = MutableStateFlow(prefs.getString("semester_start_date", DataStoreManager.DEFAULT_START_DATE) ?: DataStoreManager.DEFAULT_START_DATE)
    val semesterStartDate: StateFlow<String> = _semesterStartDate.asStateFlow()

    private val _semesterEndDate = MutableStateFlow(prefs.getString("semester_end_date", DataStoreManager.DEFAULT_END_DATE) ?: DataStoreManager.DEFAULT_END_DATE)
    val semesterEndDate: StateFlow<String> = _semesterEndDate.asStateFlow()

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
        viewModelScope.launch {
            repository.activeSemester.collect { activeSem ->
                if (activeSem != null) {
                    if (_semesterStartDate.value != activeSem.startDate) {
                        _semesterStartDate.value = activeSem.startDate
                    }
                    if (_semesterEndDate.value != activeSem.endDate) {
                        _semesterEndDate.value = activeSem.endDate
                    }
                }
            }
        }
        viewModelScope.launch {
            dataStoreManager.themePresetFlow.collect { presetKey ->
                if (_themePreset.value != presetKey) {
                    _themePreset.value = presetKey
                    prefs.edit().putString("theme_preset_key", presetKey).apply()
                    _isMonochromeMode.value = (presetKey == "pure_monochrome")
                }
            }
        }
        // Throttled 12-hour OTA update check on app launch
        checkForUpdates(force = false)
    }

    // --- Multi-Semester StateFlows ---
    val semesters: StateFlow<List<SemesterEntity>> = repository.allSemesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSemester: StateFlow<SemesterEntity?> = repository.activeSemester
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSemesterId: StateFlow<Long> = activeSemester
        .map { it?.id ?: 1L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1L)

    val isActiveSemesterArchived: StateFlow<Boolean> = activeSemester
        .map { it?.isArchived == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Semester-Scoped Core Flows (Today, Safe-Zone, Audit, To-Do automatically scope to active semester) ---
    val subjects: StateFlow<List<SubjectEntity>> = combine(
        repository.allSubjects,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val slots: StateFlow<List<TimetableSlotEntity>> = combine(
        repository.allSlots,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extraSlots: StateFlow<List<ExtraSlotEntity>> = combine(
        repository.allExtraSlots,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rescheduleOverrides: StateFlow<List<RescheduleOverrideEntity>> = combine(
        repository.allOverrides,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<AttendanceLogEntity>> = combine(
        repository.allLogs,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assessments: StateFlow<List<AssessmentEntity>> = combine(
        repository.allAssessments,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todoTasks: StateFlow<List<TodoTaskEntity>> = combine(
        repository.allTodoTasks,
        activeSemesterId
    ) { list, semId ->
        list.filter { it.semesterId == semId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private fun guardWritable(): Boolean {
        if (isActiveSemesterArchived.value) {
            Toast.makeText(
                getApplication(),
                "Archived semester is Read-Only. Unarchive in Settings to edit.",
                Toast.LENGTH_SHORT
            ).show()
            com.example.audio.CaliperSoundManager.playAlert()
            return false
        }
        return true
    }

    // --- Multi-Semester Profile Actions ---
    fun createSemester(name: String, startDate: String, endDate: String, activate: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createSemester(name = name, startDate = startDate, endDate = endDate, activate = activate)
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun switchActiveSemester(semesterId: Long) {
        viewModelScope.launch {
            repository.switchActiveSemester(semesterId)
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun setSemesterArchived(semesterId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setSemesterArchived(semesterId, isArchived)
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun setSemesterDates(startDate: String, endDate: String) {
        if (!guardWritable()) return
        _semesterStartDate.value = startDate
        _semesterEndDate.value = endDate
        prefs.edit()
            .putString("semester_start_date", startDate)
            .putString("semester_end_date", endDate)
            .apply()
        viewModelScope.launch {
            dataStoreManager.setSemesterDates(startDate, endDate)
            repository.updateSemesterDates(activeSemesterId.value, startDate, endDate)
        }
    }

    // --- Supabase Cloud Sync Actions ---
    fun signInCloud(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            _cloudSyncStatusMessage.value = "Signing in..."
            val result = supabaseSyncManager.signIn(email, password)
            _cloudSyncBusy.value = false
            result.fold(
                onSuccess = { msg ->
                    _cloudSyncStatusMessage.value = msg
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Sign in failed"
                    _cloudSyncStatusMessage.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    fun signUpCloud(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            _cloudSyncStatusMessage.value = "Creating account..."
            val result = supabaseSyncManager.signUp(email, password)
            _cloudSyncBusy.value = false
            result.fold(
                onSuccess = { msg ->
                    _cloudSyncStatusMessage.value = msg
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Sign up failed"
                    _cloudSyncStatusMessage.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    fun signOutCloud(onResult: () -> Unit = {}) {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            supabaseSyncManager.signOut()
            _cloudSyncBusy.value = false
            _cloudSyncStatusMessage.value = "Signed out from Supabase Cloud"
            onResult()
        }
    }

    fun syncNowCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            _cloudSyncStatusMessage.value = "Uploading backup..."
            val result = supabaseSyncManager.pushBackupToCloud(repository)
            _cloudSyncBusy.value = false
            result.fold(
                onSuccess = { ts ->
                    val msg = "Synced to cloud ($ts)"
                    _cloudSyncStatusMessage.value = msg
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Sync failed"
                    _cloudSyncStatusMessage.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    fun restoreFromCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _cloudSyncBusy.value = true
            _cloudSyncStatusMessage.value = "Downloading backup..."
            val result = supabaseSyncManager.restoreBackupFromCloud(repository)
            _cloudSyncBusy.value = false
            result.fold(
                onSuccess = { ts ->
                    val msg = "Restored cloud backup ($ts)"
                    _cloudSyncStatusMessage.value = msg
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Restore failed"
                    _cloudSyncStatusMessage.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    // --- GitHub Releases OTA Updater Actions ---
    fun checkForUpdates(force: Boolean = false, onFeedback: ((String) -> Unit)? = null) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            if (force) _updateStatusMessage.value = "Checking GitHub Releases..."
            when (val result = otaUpdateManager.checkForUpdate(force = force)) {
                is OtaCheckResult.UpdateAvailable -> {
                    _availableUpdate.value = result.release
                    val msg = "Update v${result.release.versionName} is available!"
                    _updateStatusMessage.value = msg
                    onFeedback?.invoke(msg)
                }
                is OtaCheckResult.UpToDate -> {
                    if (force) {
                        val msg = "You're up to date (v${result.currentVersion})"
                        _updateStatusMessage.value = msg
                        onFeedback?.invoke(msg)
                    }
                }
                is OtaCheckResult.Throttled -> {
                    // Silent skip during 12-hour window
                }
                is OtaCheckResult.Error -> {
                    if (force) {
                        _updateStatusMessage.value = result.message
                        onFeedback?.invoke(result.message)
                    }
                }
            }
            _isCheckingUpdate.value = false
        }
    }

    fun downloadAndInstallUpdate(onError: (String) -> Unit = {}) {
        val release = _availableUpdate.value ?: return
        if (_isDownloadingUpdate.value) return
        viewModelScope.launch {
            _isDownloadingUpdate.value = true
            _updateDownloadProgress.value = 0f
            _updateStatusMessage.value = "Downloading v${release.versionName}..."
            val downloadResult = otaUpdateManager.downloadApk(release.apkDownloadUrl) { progress ->
                _updateDownloadProgress.value = progress
            }
            _isDownloadingUpdate.value = false

            downloadResult.fold(
                onSuccess = { apkFile ->
                    _updateStatusMessage.value = "Launching Android installer..."
                    val installResult = otaUpdateManager.launchApkInstaller(apkFile)
                    installResult.onFailure { err ->
                        val msg = err.message ?: "Unable to open installer"
                        _updateStatusMessage.value = msg
                        onError(msg)
                    }
                },
                onFailure = { err ->
                    val msg = err.message ?: "APK download failed"
                    _updateStatusMessage.value = msg
                    onError(msg)
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        if (!_isDownloadingUpdate.value) {
            _availableUpdate.value = null
        }
    }

    val testRecords: StateFlow<List<TestRecordEntity>> = repository.allTestRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentDayActivities: StateFlow<List<CustomActivityEntity>> = selectedDate.flatMapLatest { dateStr ->
        repository.getActivitiesForDate(dateStr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val extraAndOverrides = combine(
        extraSlots,
        rescheduleOverrides,
        semesterStartDate,
        semesterEndDate
    ) { extras, overrides, semStart, semEnd ->
        Quadruple(extras, overrides, semStart, semEnd)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // Today's display slots with strict semester bounds, live ongoing class indicator, date-specific extra classes, and time shifts
    val currentDaySlots: StateFlow<List<SlotDisplayItem>> = combine(
        selectedDate,
        slots,
        subjects,
        logs,
        extraAndOverrides
    ) { dateStr, slotList, subList, logList, meta ->
        val (extraList, overrideList, semStart, semEnd) = meta
        val localDate = try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val dayOfWeek = localDate.dayOfWeek.value // 1..7

        val isToday = (dateStr == LocalDate.now().format(dateFormatter))
        val nowTime = LocalTime.now()
        val isOutsideSemester = (dateStr < semStart || dateStr > semEnd)

        val dayLogs = logList.filter { it.date == dateStr }

        // 1. Regular weekly slots (strictly suppressed outside semester boundaries)
        val regularItems = if (isOutsideSemester) {
            emptyList()
        } else {
            slotList.filter { it.dayOfWeek == dayOfWeek }.map { slot ->
                val sub = subList.find { it.id == slot.subjectId }
                val log = dayLogs.find { it.slotId == slot.id }

                val override = overrideList.find { it.slotId == slot.id && it.date == dateStr }
                val activeSlot = if (override != null) {
                    slot.copy(
                        startTime = override.newStartTime,
                        endTime = override.newEndTime,
                        roomNo = override.newRoomNo ?: slot.roomNo
                    )
                } else {
                    slot
                }

                var isOngoing = false
                if (isToday) {
                    try {
                        val sTime = LocalTime.parse(activeSlot.startTime)
                        val eTime = LocalTime.parse(activeSlot.endTime)
                        isOngoing = (nowTime.isAfter(sTime) || nowTime == sTime) && nowTime.isBefore(eTime)
                    } catch (_: Exception) {}
                }

                SlotDisplayItem(
                    slot = activeSlot,
                    subject = sub,
                    currentLog = log,
                    isOngoing = isOngoing,
                    isRescheduled = (override != null),
                    originalTime = if (override != null) "${slot.startTime} - ${slot.endTime}" else null,
                    isDateSpecificExtra = false,
                    extraSlotId = null
                )
            }
        }

        // 2. Strict Date-Locked Extra Classes for this exact ISO date
        val dateExtraItems = extraList.filter { it.date == dateStr }.map { extra ->
            val sub = subList.find { it.id == extra.subjectId }
            val syntheticSlotId = -extra.id
            val log = dayLogs.find { it.slotId == syntheticSlotId || (it.isExtraClass && it.subjectId == extra.subjectId && it.notes?.contains("extra_$syntheticSlotId") == true) }

            val syntheticSlot = TimetableSlotEntity(
                id = syntheticSlotId,
                dayOfWeek = dayOfWeek,
                startTime = extra.startTime,
                endTime = extra.endTime,
                roomNo = "${extra.roomNo} • ${extra.tag}",
                subjectId = extra.subjectId,
                semesterId = extra.semesterId
            )

            var isOngoing = false
            if (isToday) {
                try {
                    val sTime = LocalTime.parse(extra.startTime)
                    val eTime = LocalTime.parse(extra.endTime)
                    isOngoing = (nowTime.isAfter(sTime) || nowTime == sTime) && nowTime.isBefore(eTime)
                } catch (_: Exception) {}
            }

            SlotDisplayItem(
                slot = syntheticSlot,
                subject = sub,
                currentLog = log,
                isOngoing = isOngoing,
                isRescheduled = false,
                originalTime = null,
                isDateSpecificExtra = true,
                extraSlotId = extra.id
            )
        }

        (regularItems + dateExtraItems).sortedBy { it.slot.startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySlots: StateFlow<List<SlotDisplayItem>> get() = currentDaySlots

    // Orphan extra-class attendance logs for the selected date (logs with isExtraClass=true and no matching active ExtraSlotEntity)
    val currentDayOrphanExtraLogs: StateFlow<List<Pair<AttendanceLogEntity, SubjectEntity?>>> = combine(
        selectedDate,
        subjects,
        logs,
        extraSlots,
        currentDaySlots
    ) { dateStr, subList, logList, extraList, activeDaySlots ->
        val dateExtraSlots = extraList.filter { it.date == dateStr }
        val activeRegularIds = activeDaySlots.filter { !it.isDateSpecificExtra }.map { it.slot.id }.toSet()
        repository.findOrphanExtraLogsForDate(
            date = dateStr,
            allLogs = logList,
            dateExtraSlots = dateExtraSlots,
            activeRegularSlotIds = activeRegularIds
        ).map { log ->
            val sub = subList.find { it.id == log.subjectId }
            Pair(log, sub)
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
        isExtraClass: Boolean = false,
        notes: String? = null
    ) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            val isExtra = isExtraClass || slots.value.find { it.id == slotId }?.roomNo?.contains("Extra", ignoreCase = true) == true
            repository.setSlotAttendance(
                date = _selectedDate.value,
                slotId = slotId,
                subjectId = subjectId,
                status = status,
                isProxy = isProxy,
                isExtraClass = isExtra,
                notes = notes,
                semesterId = semId
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
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteSlotAttendance(_selectedDate.value, slotId)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun deleteAttendanceLog(log: AttendanceLogEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteAttendanceLog(log)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.bunkDoubleTap(getApplication())
        }
    }

    fun deleteAttendanceLogById(logId: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteAttendanceLogById(logId)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.bunkDoubleTap(getApplication())
        }
    }

    fun updateTimetableSlot(slot: TimetableSlotEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.updateTimetableSlot(slot.copy(semesterId = activeSemesterId.value))
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun deleteTimetableSlot(slotId: Long) {
        if (!guardWritable()) return
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

    fun setThemePreset(presetKey: String) {
        _themePreset.value = presetKey
        val isMono = (presetKey == "pure_monochrome")
        _isMonochromeMode.value = isMono
        _selectedTheme.value = presetKey
        prefs.edit()
            .putString("theme_preset_key", presetKey)
            .putBoolean("is_monochrome_mode", isMono)
            .putString("selected_theme", presetKey)
            .apply()
        viewModelScope.launch {
            dataStoreManager.setThemePreset(presetKey)
        }
        com.example.audio.CaliperSoundManager.playSnap()
        com.example.audio.CaliperHapticManager.tick(getApplication())
    }

    fun markWholeDayPresent() {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            val items = currentDaySlots.value
            val slotEntities = items.map { it.slot }
            repository.markWholeDayPresent(_selectedDate.value, slotEntities, semesterId = semId)
        }
    }

    fun markMassBunkOrOff(category: String) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            val items = currentDaySlots.value
            val slotEntities = items.map { it.slot }
            repository.markMassBunkOrOff(_selectedDate.value, slotEntities, category, semesterId = semId)
        }
    }

    fun applyHolidayToCurrentDay(title: String) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            val items = currentDaySlots.value
            val slotEntities = items.map { it.slot }
            repository.markMassBunkOrOff(_selectedDate.value, slotEntities, "college_holiday", semesterId = semId)
        }
    }

    fun logExtraClass(subjectId: Long, status: String, isProxy: Boolean, notes: String) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            repository.logAdHocExtraClass(_selectedDate.value, subjectId, status, isProxy, notes, semesterId = semId)
        }
    }

    fun reconcileSubject(subjectId: Long, officialAttended: Int, officialTotal: Int) {
        if (!guardWritable()) return
        viewModelScope.launch {
            val currentStat = subjectStats.value.find { it.subject.id == subjectId }
            val currentAttended = currentStat?.attended ?: 0
            val currentTotal = currentStat?.totalConducted ?: 0
            repository.reconcileSubject(subjectId, officialAttended, officialTotal, currentAttended, currentTotal)
        }
    }

    fun saveSubject(subject: SubjectEntity) {
        if (!guardWritable()) return
        val scopedSubject = subject.copy(semesterId = activeSemesterId.value)
        viewModelScope.launch {
            if (scopedSubject.id == 0L) {
                repository.addSubject(scopedSubject)
            } else {
                repository.updateSubject(scopedSubject)
            }
        }
    }

    fun saveSlot(slot: TimetableSlotEntity) {
        if (!guardWritable()) return
        val scopedSlot = slot.copy(semesterId = activeSemesterId.value)
        viewModelScope.launch {
            if (scopedSlot.id == 0L) {
                repository.addTimetableSlot(scopedSlot)
            } else {
                repository.updateTimetableSlot(scopedSlot)
            }
        }
    }

    fun deleteSlot(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteTimetableSlotById(id)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun saveAssessment(assessment: AssessmentEntity) {
        if (!guardWritable()) return
        val scoped = assessment.copy(semesterId = activeSemesterId.value)
        viewModelScope.launch {
            if (scoped.id == 0L) {
                repository.addAssessment(scoped)
            } else {
                repository.updateAssessment(scoped)
            }
        }
    }

    fun updateAssessmentStatus(id: Long, newStatus: String) {
        if (!guardWritable()) return
        viewModelScope.launch {
            val current = assessments.value.find { it.id == id }
            if (current != null) {
                repository.updateAssessment(current.copy(status = newStatus))
            }
        }
    }

    fun deleteAssessment(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteAssessment(id)
        }
    }

    fun saveMedicalLeave(leave: MedicalLeaveEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            if (leave.id == 0L) {
                repository.addMedicalLeave(leave)
            } else {
                repository.updateMedicalLeave(leave)
            }
        }
    }

    fun deleteMedicalLeave(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteMedicalLeave(id)
        }
    }

    fun saveHolidayRange(range: HolidayRangeEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            if (range.id == 0L) {
                repository.addHolidayRange(range)
            } else {
                repository.updateHolidayRange(range)
            }
        }
    }

    fun deleteHolidayRange(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteHolidayRange(id)
        }
    }

    fun addTask(task: AcademicTaskEntity) = saveTask(task)

    fun saveTask(task: AcademicTaskEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.addTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun deleteTask(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun toggleTaskCompleted(id: Long, completed: Boolean) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.toggleTaskCompleted(id, completed)
        }
    }

    // --- Smart Auto-Prioritized To-Do Actions ---
    fun addTodoTask(
        title: String,
        subjectId: Long?,
        dueDateTimeMillis: Long
    ) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            val trimmed = title.trim()
            if (trimmed.isNotEmpty()) {
                repository.addTodoTask(
                    TodoTaskEntity(
                        title = trimmed,
                        subjectId = subjectId,
                        dueDateTimeMillis = dueDateTimeMillis,
                        isCompleted = false,
                        createdAt = System.currentTimeMillis(),
                        semesterId = semId
                    )
                )
                com.example.audio.CaliperSoundManager.playSuccess()
                com.example.audio.CaliperHapticManager.tick(getApplication())
            }
        }
    }

    fun toggleTodoTaskCompleted(id: Long, completed: Boolean) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.toggleTodoTaskCompleted(id, completed)
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun deleteTodoTask(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteTodoTask(id)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.tick(getApplication())
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
        // Obsolete deadline alarm engine purged per engineering specification
    }

    fun preloadDefaultData() {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            repository.preloadDefaultIertData(semesterId = semId)
            scheduleAllLectureAlerts()
            scheduleAllDeadlineAlerts()
        }
    }

    fun clearAllData() {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.clearAllData(preserveSemesters = true)
        }
    }

    fun triggerAutoBackup() {
        viewModelScope.launch {
            repository.triggerSilentAutoBackup()
            _hasAutoVault.value = repository.hasAutoVault()
        }
    }

    fun clearAttendanceLogs() {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.clearAttendanceLogs()
        }
    }

    fun addNote(title: String, content: String, category: String = "General", subjectCode: String? = null) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.addNote(
                QuickNoteEntity(
                    title = title.trim(),
                    content = content.trim(),
                    category = category,
                    subjectCode = subjectCode?.ifBlank { null }
                )
            )
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun updateNote(note: QuickNoteEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.updateNote(note)
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun deleteNote(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteNote(id)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.bunkDoubleTap(getApplication())
        }
    }

    fun toggleNotePin(id: Long, currentPinned: Boolean) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.toggleNotePin(id, !currentPinned)
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun setMonochromeMode(enabled: Boolean) {
        _isMonochromeMode.value = enabled
        prefs.edit().putBoolean("is_monochrome_mode", enabled).apply()
        _selectedTheme.value = if (enabled) "Monochrome Dark" else "Normal"
        prefs.edit().putString("selected_theme", _selectedTheme.value).apply()
    }

    fun saveSlotTestRecord(
        subjectId: Long,
        date: String,
        slotId: Long?,
        testName: String,
        marksObtained: Double?,
        maxMarks: Double
    ) {
        if (!guardWritable()) return
        viewModelScope.launch {
            val record = TestRecordEntity(
                subjectId = subjectId,
                date = date,
                slotId = slotId,
                testName = testName,
                marksObtained = marksObtained,
                maxMarks = maxMarks
            )
            repository.saveTestRecord(record)
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun updateTestRecordMarks(id: Long, marksObtained: Double?) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.updateTestRecordMarks(id, marksObtained)
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun saveDateSpecificExtraSlot(
        date: String,
        subjectId: Long,
        startTime: String,
        endTime: String,
        roomNo: String,
        tag: String
    ) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            val insertedId = repository.addExtraSlot(
                ExtraSlotEntity(
                    date = date,
                    subjectId = subjectId,
                    startTime = startTime,
                    endTime = endTime,
                    roomNo = roomNo,
                    tag = tag,
                    semesterId = semId
                )
            )
            if (date == LocalDate.now().format(dateFormatter)) {
                val subName = subjects.value.find { it.id == subjectId }?.name ?: "Extra Class"
                com.example.notification.NotificationHelper.scheduleExactPreLectureForToday(
                    context = getApplication(),
                    slotId = -insertedId,
                    subjectName = subName,
                    roomNo = roomNo,
                    startTimeStr = startTime,
                    endTimeStr = endTime
                )
            }
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun deleteDateSpecificExtraSlot(extraSlotId: Long, deleteAttendanceLog: Boolean = true) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteExtraSlotWithAttendanceOption(
                extraSlotId = extraSlotId,
                date = _selectedDate.value,
                deleteAttendanceLog = deleteAttendanceLog
            )
            com.example.notification.NotificationHelper.cancelPreLectureAlert(getApplication(), -extraSlotId)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.bunkDoubleTap(getApplication())
        }
    }

    fun rescheduleSlotForDay(
        slotId: Long,
        date: String,
        newStartTime: String,
        newEndTime: String,
        newRoomNo: String? = null
    ) {
        if (!guardWritable()) return
        val semId = activeSemesterId.value
        viewModelScope.launch {
            repository.saveRescheduleOverride(
                RescheduleOverrideEntity(
                    slotId = slotId,
                    date = date,
                    newStartTime = newStartTime,
                    newEndTime = newEndTime,
                    newRoomNo = newRoomNo,
                    semesterId = semId
                )
            )
            if (date == LocalDate.now().format(dateFormatter)) {
                val slot = slots.value.find { it.id == slotId }
                val subName = slot?.let { s -> subjects.value.find { it.id == s.subjectId }?.name } ?: "Lecture"
                com.example.notification.NotificationHelper.scheduleExactPreLectureForToday(
                    context = getApplication(),
                    slotId = slotId,
                    subjectName = subName,
                    roomNo = newRoomNo ?: slot?.roomNo ?: "Room",
                    startTimeStr = newStartTime,
                    endTimeStr = newEndTime
                )
            }
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun revertSlotReschedule(slotId: Long, date: String) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteRescheduleOverride(slotId, date)
            com.example.audio.CaliperSoundManager.playSnap()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun deleteTestRecord(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteTestRecord(id)
            com.example.audio.CaliperSoundManager.playPip()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun saveCustomActivity(
        date: String,
        title: String,
        startTime: String,
        endTime: String = "",
        location: String = "",
        countsTowardsAttendance: Boolean = false
    ) {
        if (!guardWritable()) return
        viewModelScope.launch {
            val act = CustomActivityEntity(
                date = date,
                title = title,
                startTime = startTime,
                endTime = endTime,
                location = location,
                status = "attended",
                countsTowardsAttendance = countsTowardsAttendance
            )
            repository.saveCustomActivity(act)
            com.example.audio.CaliperSoundManager.playSuccess()
            com.example.audio.CaliperHapticManager.successClick(getApplication())
        }
    }

    fun deleteCustomActivity(id: Long) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteCustomActivity(id)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }

    fun deleteSubjectCascade(subject: SubjectEntity) {
        if (!guardWritable()) return
        viewModelScope.launch {
            repository.deleteSubjectCascade(subject)
            com.example.audio.CaliperSoundManager.playThud()
            com.example.audio.CaliperHapticManager.tick(getApplication())
        }
    }
}
