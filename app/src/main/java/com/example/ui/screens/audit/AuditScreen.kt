package com.example.ui.screens.audit

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.dialogs.AddMedicalLeaveDialog
import com.example.ui.dialogs.HolidayManagerDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.fluidTouchFeedback
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * AuditScreen — CALIPER v2.0 Control Center Bento Header & Academic History Engine
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    viewModel: MainViewModel,
    onNavigateToDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    val coroutineScope = rememberCoroutineScope()

    val logs by viewModel.logs.collectAsState()
    val slots by viewModel.slots.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()
    val medicalLeaves by viewModel.medicalLeaves.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val holidayRanges by viewModel.holidayRanges.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val testRecords by viewModel.testRecords.collectAsState()
    val todoTasks by viewModel.todoTasks.collectAsState()
    val extraSlots by viewModel.extraSlots.collectAsState()
    val rescheduleOverrides by viewModel.rescheduleOverrides.collectAsState()
    val semesterStartDate by viewModel.semesterStartDate.collectAsState()
    val semesterEndDate by viewModel.semesterEndDate.collectAsState()

    var showMedicalDialog by remember { mutableStateOf(false) }
    var showHolidayDialog by remember { mutableStateOf(false) }
    var showCancelledClassesSheet by remember { mutableStateOf(false) }
    var showCollegeOffSheet by remember { mutableStateOf(false) }
    var showTodoBottomSheet by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var enteringMarksRecord by remember { mutableStateOf<com.example.data.local.entity.TestRecordEntity?>(null) }

    // Synced Calendar Month & Bento Donut Scope ("month" vs "all")
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var donutScope by remember { mutableStateOf("month") } // "month" or "all"

    val todayDate = remember { LocalDate.now() }
    val todayDateStr = remember { todayDate.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var selectedAuditDate by remember { mutableStateOf(todayDate) }
    val selectedAuditDateStr = remember(selectedAuditDate) {
        selectedAuditDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // Compute Bento Donut Metrics
    val monthPrefix = remember(displayedMonth) {
        displayedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }
    val scopedLogs = remember(logs, donutScope, monthPrefix) {
        if (donutScope == "month") logs.filter { it.date.startsWith(monthPrefix) } else logs
    }

    val donutPresent = remember(scopedLogs, donutScope, summary) {
        if (donutScope == "all") summary.totalAttended else scopedLogs.count { it.status == "attended" }
    }
    val donutBunked = remember(scopedLogs, donutScope, summary) {
        if (donutScope == "all") (summary.totalConducted - summary.totalAttended).coerceAtLeast(0)
        else scopedLogs.count { it.status == "bunked" }
    }
    val donutCancelled = remember(scopedLogs, donutScope, summary) {
        if (donutScope == "all") summary.totalFacultyCancelled else scopedLogs.count { it.status == "cancelled_by_faculty" }
    }
    val donutOff = remember(scopedLogs, donutScope, summary) {
        if (donutScope == "all") summary.totalCollegeOff else scopedLogs.count { it.status == "college_off" }
    }
    val donutPercentage = remember(donutPresent, donutBunked, donutScope, summary) {
        if (donutScope == "all") {
            summary.overallPercentage
        } else {
            val conducted = donutPresent + donutBunked
            if (conducted > 0) (donutPresent.toDouble() / conducted.toDouble()) * 100.0 else 100.0
        }
    }

    // Compute Semester Elapsed Percentage (Bar 1)
    val semesterProgress = remember(semesterStartDate, semesterEndDate, todayDate) {
        try {
            val start = LocalDate.parse(semesterStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val end = LocalDate.parse(semesterEndDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val totalDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1L)
            val elapsedDays = ChronoUnit.DAYS.between(start, todayDate).coerceIn(0L, totalDays)
            (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
        } catch (_: Exception) {
            0f
        }
    }

    // Compute Smart To-Do Progress (Bar 2)
    val completedTasksCount = remember(todoTasks) { todoTasks.count { it.isCompleted } }
    val totalTasksCount = todoTasks.size
    val todoProgress = remember(completedTasksCount, totalTasksCount) {
        if (totalTasksCount > 0) (completedTasksCount.toFloat() / totalTasksCount.toFloat()).coerceIn(0f, 1f) else 0f
    }

    // SAF Launchers for JSON Backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    CaliperSoundManager.playSuccess()
                    CaliperHapticManager.successClick(context)
                    statusMessage = "Backup exported successfully to JSON file!"
                    Toast.makeText(context, "Export complete", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    statusMessage = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { ins ->
                        ins.bufferedReader().readText()
                    } ?: ""
                    val success = viewModel.importBackupJson(json)
                    if (success) {
                        CaliperSoundManager.playSuccess()
                        CaliperHapticManager.successClick(context)
                        statusMessage = "Backup restored successfully from JSON!"
                        Toast.makeText(context, "Restore complete", Toast.LENGTH_SHORT).show()
                    } else {
                        CaliperSoundManager.playThud()
                        statusMessage = "Invalid backup format or empty file."
                    }
                } catch (e: Exception) {
                    statusMessage = "Restore failed: ${e.localizedMessage}"
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (statusMessage != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.5f), Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusMessage ?: "", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1. CONTROL CENTER-STYLE BENTO HEADER (Donut Tile + 2 Vertical Fill-Pills)
        // =========================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .testTag("audit_bento_header"),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT SQUARE TILE (52% width): Multi-Segment Audit Donut
                GlassCard(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                        .testTag("audit_donut_bento_tile"),
                    shape = RoundedCornerShape(24.dp),
                    innerPadding = 12.dp,
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.4f), Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Row: Title "Audit" & Compact [Month | All] Toggle Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audit",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border, CircleShape)
                                    .padding(2.dp)
                            ) {
                                listOf("month" to "Month", "all" to "All").forEach { (key, label) ->
                                    val active = (donutScope == key)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (active) colors.primaryAccent else Color.Transparent)
                                            .clickable {
                                                CaliperSoundManager.playSnap(view)
                                                CaliperHapticManager.tick(context)
                                                donutScope = key
                                            }
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                            .testTag("donut_toggle_$key")
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (active) colors.onPrimaryAccent else colors.textSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Center: Multi-Segment Circular Donut Canvas
                        AuditMultiSegmentDonut(
                            present = donutPresent,
                            cancelled = donutCancelled,
                            bunked = donutBunked,
                            off = donutOff,
                            percentage = donutPercentage,
                            scopeLabel = if (donutScope == "month") {
                                displayedMonth.format(DateTimeFormatter.ofPattern("MMM"))
                            } else "All",
                            modifier = Modifier.size(94.dp)
                        )

                        // Bottom: Compact Inline Counts (P • C • B • Off)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DonutInlineStat("${donutPresent}P", colors.safeGreen)
                            Text("•", color = colors.textMuted, fontSize = 9.sp)
                            DonutInlineStat("${donutCancelled}C", colors.warnAmber)
                            Text("•", color = colors.textMuted, fontSize = 9.sp)
                            DonutInlineStat("${donutBunked}B", colors.dangerRed)
                            Text("•", color = colors.textMuted, fontSize = 9.sp)
                            DonutInlineStat("${donutOff}O", colors.offSlate)
                        }
                    }
                }

                // RIGHT SIDE (48% width): Two OxygenOS-style Vertical Fill-Pill Capsules
                Row(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bar 1: Semester Progress Capsule
                    VerticalFillPillBar(
                        progress = semesterProgress,
                        valueText = "${(semesterProgress * 100).toInt()}%",
                        label = "Term",
                        icon = Icons.Default.CalendarMonth,
                        fillColor = colors.primaryAccent,
                        onClick = {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            Toast.makeText(
                                context,
                                "Term: $semesterStartDate → $semesterEndDate (${(semesterProgress * 100).toInt()}% elapsed)",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("semester_progress_pill")
                    )

                    // Bar 2: To-Do Progress Capsule (Opens To-Do Bottom Sheet)
                    VerticalFillPillBar(
                        progress = todoProgress,
                        valueText = if (totalTasksCount > 0) "$completedTasksCount/$totalTasksCount" else "0/0",
                        label = "To-Do",
                        icon = Icons.Default.Checklist,
                        fillColor = colors.safeGreen,
                        onClick = {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            showTodoBottomSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("todo_progress_pill")
                    )
                }
            }
        }

        // 2. Dynamic Interactive Calendar Month Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interactive_calendar_card"),
                borderColors = listOf(colors.primaryAccent.copy(alpha = 0.4f), Color.Transparent),
                innerPadding = 12.dp
            ) {
                InteractiveAuditCalendar(
                    viewModel = viewModel,
                    selectedDate = selectedAuditDate,
                    onDateSelected = { date ->
                        selectedAuditDate = date
                    },
                    onNavigateToTodayView = { dateStr ->
                        onNavigateToDate(dateStr)
                    },
                    onMonthChanged = { ym ->
                        displayedMonth = ym
                    }
                )
            }
        }

        // 3. Academic Test Performance Ledger Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academic_test_performance_card"),
                borderColors = listOf(colors.primaryAccent.copy(alpha = 0.45f), Color.Transparent),
                innerPadding = 14.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACADEMIC TEST PERFORMANCE",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAccent,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primaryAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${testRecords.size} Tests Logged",
                                color = colors.primaryAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (testRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No class tests recorded yet. Mark tests directly on lecture slot cards in Today's schedule.",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val testsBySubject = testRecords.groupBy { it.subjectId }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            testsBySubject.forEach { (subId, records) ->
                                val sub = subjects.find { it.id == subId }
                                val evaluatedRecords = records.filter { it.marksObtained != null }
                                val avgPct = if (evaluatedRecords.isNotEmpty()) {
                                    evaluatedRecords.map {
                                        if (it.maxMarks > 0) ((it.marksObtained ?: 0.0) / it.maxMarks) * 100.0 else 0.0
                                    }.average()
                                } else null

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.border, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${sub?.code ?: "SUB"} — ${sub?.name ?: "Subject"}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "${records.size} Evaluation${if (records.size == 1) "" else "s"}",
                                                fontSize = 10.5.sp,
                                                color = colors.textSecondary
                                            )
                                        }

                                        val badgeColor = when {
                                            avgPct == null -> colors.warnAmber
                                            avgPct >= 75.0 -> colors.safeGreen
                                            avgPct >= 50.0 -> colors.warnAmber
                                            else -> colors.dangerRed
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(badgeColor.copy(alpha = 0.16f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (avgPct != null) "Avg: ${String.format("%.1f", avgPct)}%" else "Pending",
                                                color = badgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    records.forEach { record ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "• ${record.testName}",
                                                    fontSize = 11.5.sp,
                                                    color = colors.textPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${record.date})",
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (record.marksObtained == null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colors.warnAmber.copy(alpha = 0.15f))
                                                            .border(0.5.dp, colors.warnAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Pending",
                                                            color = colors.warnAmber,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    OutlinedButton(
                                                        onClick = { enteringMarksRecord = record },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryAccent),
                                                        border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.5f)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("Enter Marks", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    val testPct = if (record.maxMarks > 0) ((record.marksObtained ?: 0.0) / record.maxMarks) * 100.0 else 0.0
                                                    Text(
                                                        text = "${record.marksObtained}/${record.maxMarks} (${String.format("%.1f", testPct)}%)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (testPct >= 75.0) colors.safeGreen else colors.dangerRed
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Edit",
                                                        color = colors.primaryAccent,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .clickable { enteringMarksRecord = record }
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteTestRecord(record.id) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete",
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Smooth Transition Filtered Day Detail Inspector Card
        item {
            AnimatedContent(
                targetState = selectedAuditDate,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "day_detail_transition"
            ) { inspectDate ->
                val dateStr = inspectDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isFutureInspect = inspectDate.isAfter(todayDate)
                val inspectLogs = logs.filter { it.date == dateStr }
                val inspectStatus = dayStatuses.find { it.date == dateStr }
                val holidayMatch = holidayRanges.find { dateStr >= it.startDate && dateStr <= it.endDate }
                val isHoliday = holidayMatch != null
                val isSunday = inspectDate.dayOfWeek.value == 7
                val isOutsideSemester = dateStr < semesterStartDate || dateStr > semesterEndDate
                val isFutureOff = isFutureInspect && (isHoliday || isSunday || isOutsideSemester || inspectStatus?.leaveCategory in listOf("college_holiday", "strike"))

                val upcomingFutureSlots = remember(inspectDate, slots, extraSlots, rescheduleOverrides, subjects, isFutureOff, isOutsideSemester) {
                    if (isFutureInspect) {
                        resolveScheduledClassesForDate(
                            date = inspectDate,
                            allSlots = slots,
                            extraSlots = extraSlots,
                            overrides = rescheduleOverrides,
                            subjects = subjects,
                            includeRegularSlots = !isFutureOff && !isOutsideSemester
                        )
                    } else {
                        emptyList()
                    }
                }

                val parsedDateLabel = remember(inspectDate) {
                    inspectDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.35f), Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFutureInspect) "UPCOMING SCHEDULE PREVIEW" else "DAY AUDIT DOSSIER",
                                    color = colors.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = parsedDateLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                onNavigateToDate(dateStr)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.18f)),
                            border = BorderStroke(0.5.dp, colors.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Open in Today →", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isHoliday) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.warnAmber.copy(alpha = 0.12f))
                                .border(0.5.dp, colors.warnAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text("Declared Holiday / Off • ${holidayMatch?.title ?: "Official Break"}", color = colors.warnAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (inspectStatus != null && inspectStatus.leaveCategory != "none") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.dangerRed.copy(alpha = 0.12f))
                                .border(0.5.dp, colors.dangerRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Day Level Event: ${inspectStatus.notes ?: inspectStatus.leaveCategory.replace('_', ' ').uppercase()}",
                                color = colors.dangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isFutureInspect) {
                        if (isFutureOff && upcomingFutureSlots.isEmpty()) {
                            Text(
                                text = when {
                                    isHoliday -> "Declared Holiday / Off (${holidayMatch?.title ?: "Campus Closed"})."
                                    isSunday -> "Declared Holiday / Off (Sunday)."
                                    dateStr < semesterStartDate -> "Declared Holiday / Off (Pre-Semester, starts $semesterStartDate)."
                                    dateStr > semesterEndDate -> "Declared Holiday / Off (Semester Concluded on $semesterEndDate)."
                                    else -> "Declared Holiday / Off."
                                },
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else if (upcomingFutureSlots.isEmpty()) {
                            Text(
                                text = "No scheduled classes for this upcoming date.",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                upcomingFutureSlots.forEach { slot ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.surface)
                                            .border(0.5.dp, colors.border, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = slot.subjectName,
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${slot.startTime} - ${slot.endTime} • ${slot.roomNo}",
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        val pillColor = when {
                                            slot.isExtra -> colors.safeGreen
                                            slot.isRescheduled -> colors.warnAmber
                                            else -> colors.primaryAccent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(pillColor.copy(alpha = 0.15f))
                                                .border(0.5.dp, pillColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (slot.isExtra) "EXTRA" else if (slot.isRescheduled) "SHIFTED" else "SCHEDULED",
                                                color = pillColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (inspectLogs.isEmpty()) {
                        Text(
                            text = "No period attendance recorded for this date.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            inspectLogs.forEach { log ->
                                val sub = subjects.find { it.id == log.subjectId }
                                val (statusText, statusColor) = when (log.status) {
                                    "attended" -> (if (log.isProxy) "PROXY PRESENT" else "PRESENT") to colors.safeGreen
                                    "bunked" -> "BUNKED" to colors.dangerRed
                                    "cancelled_by_faculty" -> "FACULTY CANCELLED" to colors.warnAmber
                                    "college_off" -> "COLLEGE OFF" to colors.offSlate
                                    else -> log.status.uppercase() to colors.primaryAccent
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.border, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sub?.name ?: "Subject #${log.subjectId}",
                                            color = colors.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${sub?.code ?: ""} ${if (log.notes != null) "• ${log.notes}" else ""}",
                                            color = colors.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusColor.copy(alpha = 0.15f))
                                                .border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = statusColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                CaliperHapticManager.bunkDoubleTap(context)
                                                CaliperSoundManager.playThud()
                                                viewModel.deleteAttendanceLog(log)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_log_${log.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Attendance Log",
                                                tint = colors.dangerRed.copy(alpha = 0.85f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Global Cumulative Metrics Tally
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ACADEMIC AGGREGATE SUMMARY",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditMetricBox("Attended", "${summary.totalAttended}", colors.safeGreen, Modifier.weight(1f))
                    AuditMetricBox("Conducted", "${summary.totalConducted}", colors.primaryAccent, Modifier.weight(1f))
                    AuditMetricBox(
                        title = "Cancelled",
                        value = "${summary.totalFacultyCancelled}",
                        color = colors.warnAmber,
                        modifier = Modifier.weight(1f),
                        isClickable = true,
                        onClick = { showCancelledClassesSheet = true }
                    )
                    AuditMetricBox(
                        title = "College Off",
                        value = "${summary.totalCollegeOff}",
                        color = colors.offSlate,
                        modifier = Modifier.weight(1f),
                        isClickable = true,
                        onClick = { showCollegeOffSheet = true }
                    )
                }
            }
        }

        // 6. Institutional Holiday / College Off Manager Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BeachAccess, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HOLIDAYS & BREAK RANGES", color = colors.warnAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Text(
                            text = "${holidayRanges.size} Institutional Ranges Declared",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text("Suppresses attendance penalties during closures", color = colors.textSecondary, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            showHolidayDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.warnAmber.copy(alpha = 0.2f)),
                        border = BorderStroke(0.5.dp, colors.warnAmber),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Manage", color = colors.warnAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 7. Medical Leave Archive
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEDICAL LEAVE & CONDONATION ARCHIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showMedicalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.safeGreen.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Slip", color = colors.safeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (medicalLeaves.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No Medical Certificates Archived", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Certificates submitted to HOD Mechanical for attendance condonation.", color = colors.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        items(medicalLeaves, key = { it.id }) { leave ->
            MedicalLeaveCard(leave = leave)
        }

        // 8. JSON Backup & Restore Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "JSON BACKUP & RESTORE HUB",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            val filename = "caliper_backup_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
                            exportLauncher.launch(filename)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, colors.primaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            importLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.safeGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, colors.safeGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore JSON", color = colors.safeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showMedicalDialog) {
        AddMedicalLeaveDialog(
            onDismiss = { showMedicalDialog = false },
            onSave = { leave -> viewModel.saveMedicalLeave(leave) }
        )
    }

    if (showHolidayDialog) {
        HolidayManagerDialog(
            holidayRanges = holidayRanges,
            onDismiss = { showHolidayDialog = false },
            onSaveHoliday = { range -> viewModel.saveHolidayRange(range) },
            onDeleteHoliday = { id -> viewModel.deleteHolidayRange(id) }
        )
    }

    enteringMarksRecord?.let { record ->
        val sub = subjects.find { it.id == record.subjectId }
        com.example.ui.screens.today.EnterMarksDialog(
            testRecord = record,
            subjectName = sub?.name ?: "Subject",
            onDismiss = { enteringMarksRecord = null },
            onSaveMarks = { marks ->
                viewModel.updateTestRecordMarks(record.id, marks)
                enteringMarksRecord = null
            }
        )
    }

    // Smart Auto-Prioritized To-Do Bottom Sheet (Triggered by Vertical To-Do Fill-Pill Bar 2)
    if (showTodoBottomSheet) {
        TodoBottomSheet(
            tasks = todoTasks,
            subjects = subjects,
            onDismiss = { showTodoBottomSheet = false },
            onAddTask = { title, subjectId, dueMillis ->
                viewModel.addTodoTask(title, subjectId, dueMillis)
            },
            onToggleTask = { id, completed ->
                viewModel.toggleTodoTaskCompleted(id, completed)
            },
            onDeleteTask = { id ->
                viewModel.deleteTodoTask(id)
            }
        )
    }

    // Historical Faculty Cancellations Drill-Down Bottom Sheet
    if (showCancelledClassesSheet) {
        val cancelledLogs = remember(logs) {
            logs.filter { it.status == "cancelled_by_faculty" }.sortedByDescending { it.date }
        }
        ModalBottomSheet(
            onDismissRequest = { showCancelledClassesSheet = false },
            containerColor = colors.surfaceVariant,
            scrimColor = Color.Black.copy(alpha = 0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.warnAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Faculty Cancelled Classes", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("${cancelledLogs.size} Historical Cancellations by Faculty", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = { showCancelledClassesSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (cancelledLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No faculty cancellations recorded in academic history.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cancelledLogs, key = { it.id }) { log ->
                            val sub = subjects.find { it.id == log.subjectId }
                            val slot = slots.find { it.id == log.slotId }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.warnAmber.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${sub?.code ?: "SUB"} — ${sub?.name ?: "Curriculum Lecture"}",
                                            color = colors.textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Faculty: ${sub?.facultyName ?: "Instructor"} • Slot: ${slot?.startTime ?: "Period"} (${slot?.roomNo ?: "LT-TBD"})",
                                            color = colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                        if (!log.notes.isNullOrBlank()) {
                                            Text("Note: ${log.notes}", color = colors.warnAmber, fontSize = 10.5.sp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.warnAmber.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(log.date, color = colors.warnAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Recorded Holidays and College Off Days Drill-Down Bottom Sheet
    if (showCollegeOffSheet) {
        val offDayStatuses = remember(dayStatuses) {
            dayStatuses.filter { it.leaveCategory != "none" || !it.wentToCollege }
        }
        val collegeOffLogs = remember(logs) {
            logs.filter { it.status == "college_off" }
        }

        ModalBottomSheet(
            onDismissRequest = { showCollegeOffSheet = false },
            containerColor = colors.surfaceVariant,
            scrimColor = Color.Black.copy(alpha = 0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.offSlate.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BeachAccess, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Recorded Holidays & College Off Days", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${holidayRanges.size} Declared Ranges • ${offDayStatuses.size + collegeOffLogs.size} Event Logs",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = { showCollegeOffSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (holidayRanges.isNotEmpty()) {
                        item {
                            Text(
                                text = "INSTITUTIONAL HOLIDAY RANGES",
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        items(holidayRanges, key = { "range_${it.id}" }) { range ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(range.title, color = colors.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                        Text("Period: ${range.startDate} to ${range.endDate}", color = colors.textSecondary, fontSize = 11.sp)
                                        if (!range.notes.isNullOrBlank()) {
                                            Text(range.notes, color = colors.textSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.surfaceVariant)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(range.type.uppercase(), color = colors.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (offDayStatuses.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "LOGGED DAILY OFF EVENTS",
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        items(offDayStatuses, key = { "status_${it.date}" }) { status ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (status.leaveCategory == "mass_bunk") "Mass Bunk Declared"
                                            else if (status.leaveCategory == "college_holiday") "College Holiday"
                                            else "College Off",
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (!status.notes.isNullOrBlank()) {
                                            Text(status.notes, color = colors.textSecondary, fontSize = 10.5.sp)
                                        }
                                    }
                                    Text(status.date, color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (holidayRanges.isEmpty() && offDayStatuses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No holidays or institutional off days recorded.", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Multi-Segment Circular Donut Graph matching exact calendar colors:
 * Present (safeGreen), Cancelled (warnAmber), Bunked (dangerRed), Off/Holiday (offSlate).
 */
@Composable
private fun AuditMultiSegmentDonut(
    present: Int,
    cancelled: Int,
    bunked: Int,
    off: Int,
    percentage: Double,
    scopeLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val total = (present + cancelled + bunked + off).coerceAtLeast(0)

    val animFraction by animateFloatAsState(
        targetValue = if (total > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "donut_sweep_anim"
    )

    val segments = remember(present, cancelled, bunked, off, colors) {
        listOf(
            present to colors.safeGreen,
            cancelled to colors.warnAmber,
            bunked to colors.dangerRed,
            off to colors.offSlate
        ).filter { it.first > 0 }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val strokePx = 9.dp.toPx()
            val diameter = size.minDimension
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Base ring track
            drawArc(
                color = colors.border.copy(alpha = 0.45f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (total > 0 && segments.isNotEmpty()) {
                val gapDeg = if (segments.size > 1) 4f else 0f
                val totalGap = gapDeg * segments.size
                val availableDeg = (360f - totalGap).coerceAtLeast(1f)
                var startAngle = -90f

                segments.forEach { (count, segColor) ->
                    val rawSweep = (count.toFloat() / total.toFloat()) * availableDeg
                    val sweep = (rawSweep * animFraction).coerceAtLeast(1.5f)
                    drawArc(
                        color = segColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep + gapDeg
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > 0) "${String.format("%.0f", percentage)}%" else "—",
                color = if (total == 0) colors.textSecondary else if (percentage >= 75.0) colors.safeGreen else colors.dangerRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = scopeLabel.uppercase(),
                color = colors.textSecondary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DonutInlineStat(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

/**
 * OxygenOS Control Center-style Vertical Fill-Pill Capsule (RoundedCornerShape(24.dp))
 * Fills smoothly from bottom to top with percentage/ratio and minimal icon anchored cleanly.
 */
@Composable
private fun VerticalFillPillBar(
    progress: Float,
    valueText: String,
    label: String,
    icon: ImageVector,
    fillColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val animatedFill by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "vertical_pill_fill"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.90f))
            .border(0.5.dp, colors.border, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        // Bottom-to-Top Smooth Animated Fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = animatedFill.coerceAtLeast(0.06f))
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            fillColor.copy(alpha = 0.55f),
                            fillColor.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        // Overlay Telemetry Labels & Bottom Minimal Icon
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueText,
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = label,
                    color = colors.textSecondary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(colors.surface.copy(alpha = 0.72f))
                    .border(0.5.dp, fillColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fillColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AuditMetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isClickable) color.copy(alpha = 0.10f) else colors.surface)
            .border(
                if (isClickable) 1.dp else 0.5.dp,
                color.copy(alpha = if (isClickable) 0.55f else 0.30f),
                RoundedCornerShape(10.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .fluidTouchFeedback {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            onClick()
                        }
                        .clickable {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            onClick()
                        }
                } else Modifier
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    color = if (isClickable) color else colors.textSecondary,
                    maxLines = 1,
                    fontWeight = if (isClickable) FontWeight.Bold else FontWeight.Normal
                )
                if (isClickable) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("▾", fontSize = 8.sp, color = color)
                }
            }
        }
    }
}

@Composable
private fun MedicalLeaveCard(leave: MedicalLeaveEntity) {
    val colors = CaliperTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(colors.safeGreen.copy(alpha = 0.35f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${leave.startDate} to ${leave.endDate}",
                    color = colors.safeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = leave.reason,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ref: ${leave.refNo} • Dr. ${leave.doctorName} • Submitted to ${leave.submittedTo}",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.safeGreen.copy(alpha = 0.15f))
                    .border(0.5.dp, colors.safeGreen, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = leave.status.uppercase(),
                    color = colors.safeGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
