package com.example.ui.screens.audit

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.AddMedicalLeaveDialog
import com.example.ui.dialogs.HolidayManagerDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.tactilePressEffect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * AuditScreen — Interactive Google Calendar & Academic History Engine
 * Completely replaces static calendar with a 2-way dynamic month navigation engine,
 * interactive day cells with attendance indicators, smooth day detail transitions,
 * Bezier trajectory wave, and engineering milestone shields.
 */
@Composable
fun AuditScreen(
    viewModel: MainViewModel,
    onNavigateToDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val coroutineScope = rememberCoroutineScope()

    val logs by viewModel.logs.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()
    val medicalLeaves by viewModel.medicalLeaves.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val holidayRanges by viewModel.holidayRanges.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showMedicalDialog by remember { mutableStateOf(false) }
    var showHolidayDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Interactive Google Calendar Month State
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val todayDate = remember { LocalDate.now() }
    val todayDateStr = remember { todayDate.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    // Selected date defaults to today or first day
    var selectedAuditDate by remember { mutableStateOf<String?>(todayDateStr) }

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

    // Dynamic month calculations
    val daysInMonth = remember(displayedMonth) { displayedMonth.lengthOfMonth() }
    val firstDayOffset = remember(displayedMonth) {
        displayedMonth.atDay(1).dayOfWeek.value - 1 // 0 (Mon) .. 6 (Sun)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Message Banner
        if (statusMessage != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.5f), colors.cardBorderBottom)
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

        // 1. Attendance Trajectory Quadratic Bezier Wave Chart
        item {
            AttendanceTrajectoryWaveChart(
                logs = logs,
                overallPercentage = summary.overallPercentage,
                height = 130.dp
            )
        }

        // 3. Dynamic Interactive Google Calendar Month Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interactive_calendar_card"),
                borderColors = listOf(colors.primaryAccent.copy(alpha = 0.4f), colors.cardBorderBottom)
            ) {
                // Calendar Top Bar: Month Switcher Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable Month-Year title with dropdown arrow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                CaliperSoundManager.playSnap()
                                CaliperHapticManager.tick(context)
                                showMonthPicker = true
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Month",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Tap to switch semester month",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    // Navigation Chevrons: < and >
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (displayedMonth != YearMonth.now()) {
                            TextButton(
                                onClick = {
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    displayedMonth = YearMonth.now()
                                    selectedAuditDate = todayDateStr
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Current", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = {
                                CaliperSoundManager.playSnap()
                                CaliperHapticManager.tick(context)
                                displayedMonth = displayedMonth.minusMonths(1)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                CaliperSoundManager.playSnap()
                                CaliperHapticManager.tick(context)
                                displayedMonth = displayedMonth.plusMonths(1)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of week header (Mon - Sun)
                val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayHeaders.forEachIndexed { idx, day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                color = if (idx == 6) colors.bunkDanger else colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Month Grid
                val totalCells = firstDayOffset + daysInMonth
                val rowCount = (totalCells + 6) / 7

                for (r in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val cellDate = displayedMonth.atDay(dayNum)
                                val dateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isSelected = (selectedAuditDate == dateStr)
                                val isToday = (dateStr == todayDateStr)

                                val dayLogs = logs.filter { it.date == dateStr }
                                val dayStatus = dayStatuses.find { it.date == dateStr }
                                val isHoliday = holidayRanges.any { dateStr >= it.startDate && dateStr <= it.endDate }

                                val attended = dayLogs.count { it.status == "attended" }
                                val bunked = dayLogs.count { it.status == "bunked" }
                                val cancelled = dayLogs.count { it.status == "cancelled_by_faculty" }

                                // Status dot color
                                val dotColor = when {
                                    isHoliday || dayStatus?.leaveCategory == "college_holiday" -> colors.textSecondary
                                    dayStatus?.leaveCategory == "mass_bunk" -> colors.bunkDanger
                                    attended > 0 && bunked == 0 -> colors.safeZone
                                    attended > 0 && bunked > 0 -> colors.deadlineWarning
                                    bunked > 0 -> colors.bunkDanger
                                    cancelled > 0 -> colors.scheduledLecture
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> colors.primaryAccent.copy(alpha = 0.20f)
                                                isToday -> colors.primaryAccent.copy(alpha = 0.08f)
                                                else -> Color.White.copy(alpha = 0.02f)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.2.dp else if (isToday) 0.8.dp else 0.5.dp,
                                            color = if (isSelected) colors.primaryAccent else if (isToday) colors.primaryAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.04f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            CaliperSoundManager.playSnap()
                                            CaliperHapticManager.tick(context)
                                            selectedAuditDate = dateStr
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            color = if (isSelected) colors.primaryAccent else if (isToday) Color.White else colors.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )

                                        // Status dot indicator
                                        if (dotColor != Color.Transparent) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).padding(2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CalendarLegendDot("Present", colors.safeZone)
                    CalendarLegendDot("Partial", colors.deadlineWarning)
                    CalendarLegendDot("Bunked", colors.bunkDanger)
                    CalendarLegendDot("Off/Holiday", colors.textSecondary)
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
            ) { dateStr ->
                val inspectLogs = logs.filter { it.date == dateStr }
                val inspectStatus = dayStatuses.find { it.date == dateStr }
                val isHoliday = holidayRanges.any { dateStr != null && dateStr >= it.startDate && dateStr <= it.endDate }

                val parsedDateLabel = remember(dateStr) {
                    try {
                        dateStr?.let {
                            LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
                        } ?: "Selected Date"
                    } catch (_: Exception) {
                        dateStr ?: "Selected Date"
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.35f), colors.cardBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DAY AUDIT DOSSIER",
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

                        dateStr?.let { d ->
                            Button(
                                onClick = {
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    onNavigateToDate(d)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.18f)),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.primaryAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Open in Today →", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isHoliday) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.deadlineWarning.copy(alpha = 0.12f))
                                .border(0.5.dp, colors.deadlineWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text("Official Institutional Holiday / Break Window Declared", color = colors.deadlineWarning, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (inspectStatus != null && inspectStatus.leaveCategory != "none") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.bunkDanger.copy(alpha = 0.12f))
                                .border(0.5.dp, colors.bunkDanger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Day Level Event: ${inspectStatus.notes ?: inspectStatus.leaveCategory.replace('_', ' ').uppercase()}",
                                color = colors.bunkDanger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (inspectLogs.isEmpty()) {
                        Text(
                            text = "No period attendance recorded for this date.",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            inspectLogs.forEach { log ->
                                val sub = subjects.find { it.id == log.subjectId }
                                val (statusText, statusColor) = when (log.status) {
                                    "attended" -> (if (log.isProxy) "PROXY PRESENT" else "PRESENT") to colors.safeZone
                                    "bunked" -> "BUNKED" to colors.bunkDanger
                                    "cancelled_by_faculty" -> "FACULTY CANCELLED" to colors.deadlineWarning
                                    "college_off" -> "COLLEGE OFF" to colors.textSecondary
                                    else -> log.status.uppercase() to colors.primaryAccent
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.02f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
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
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditMetricBox("Attended", "${summary.totalAttended}", colors.safeZone, Modifier.weight(1f))
                    AuditMetricBox("Conducted", "${summary.totalConducted}", colors.scheduledLecture, Modifier.weight(1f))
                    AuditMetricBox("Cancelled", "${summary.totalFacultyCancelled}", colors.deadlineWarning, Modifier.weight(1f))
                    AuditMetricBox("College Off", "${summary.totalCollegeOff}", colors.textSecondary, Modifier.weight(1f))
                }
            }
        }

        // 6. Institutional Holiday / College Off Manager Button
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BeachAccess, contentDescription = null, tint = colors.deadlineWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HOLIDAYS & BREAK RANGES", color = colors.deadlineWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                            CaliperSoundManager.playSnap()
                            CaliperHapticManager.tick(context)
                            showHolidayDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.deadlineWarning.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.deadlineWarning),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Manage", color = colors.deadlineWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    color = colors.textMuted,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showMedicalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.safeZone.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.safeZone, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Slip", color = colors.safeZone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Text("Certificates submitted to HOD Mechanical for attendance condonation.", color = colors.textSecondary, fontSize = 11.sp)
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
                    color = colors.textMuted,
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
                            CaliperSoundManager.playSnap()
                            CaliperHapticManager.tick(context)
                            val filename = "caliper_backup_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
                            exportLauncher.launch(filename)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.primaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            CaliperSoundManager.playSnap()
                            CaliperHapticManager.tick(context)
                            importLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.safeZone.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.safeZone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = colors.safeZone, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore JSON", color = colors.safeZone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Quick Semester Month Selector Dialog
    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            containerColor = colors.containerSurface,
            title = {
                Text(
                    text = "Select Semester Month",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val currentYear = remember { LocalDate.now().year }
                    val semesterMonths = listOf(
                        YearMonth.of(currentYear, 7),
                        YearMonth.of(currentYear, 8),
                        YearMonth.of(currentYear, 9),
                        YearMonth.of(currentYear, 10),
                        YearMonth.of(currentYear, 11),
                        YearMonth.of(currentYear, 12)
                    )

                    semesterMonths.forEach { ym ->
                        val isChosen = (ym == displayedMonth)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) colors.primaryAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(0.5.dp, if (isChosen) colors.primaryAccent else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable {
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    displayedMonth = ym
                                    showMonthPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                color = if (isChosen) colors.primaryAccent else colors.textPrimary,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("Close", color = colors.primaryAccent)
                }
            }
        )
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
}

@Composable
private fun CalendarLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = CaliperTheme.colors.textSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun AuditMetricBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(title, fontSize = 9.sp, color = CaliperTheme.colors.textMuted, maxLines = 1)
        }
    }
}

@Composable
private fun MedicalLeaveCard(leave: MedicalLeaveEntity) {
    val colors = CaliperTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(colors.safeZone.copy(alpha = 0.35f), colors.cardBorderBottom)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${leave.startDate} to ${leave.endDate}",
                    color = colors.safeZone,
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
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.safeZone.copy(alpha = 0.15f))
                    .border(0.5.dp, colors.safeZone, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = leave.status.uppercase(),
                    color = colors.safeZone,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
