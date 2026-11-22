package com.example.ui.screens.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHardwareEngine
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.GlassCard
import com.example.ui.components.SlimVernierPrecisionBar
import com.example.ui.dialogs.AddEditSubjectDialog
import com.example.ui.dialogs.EditSlotDialog
import com.example.ui.theme.CaliperTheme

/**
 * SafeZoneScreen — CALIPER v2.0 Refined Telemetry & On-Demand Bunk Simulator
 * 1. Refined Overall Telemetry Card: Large % + SAFE ZONE/AT RISK pill, compact [Slots] & [Simulator] actions,
 *    Slim Vernier 75% cutoff bar, and 3-column metrics (Attended | Safe Margin | Saved).
 * 2. On-Demand Bunk Simulator ModalBottomSheet with Overall & Subject-Wise projection modes.
 * 3. Curriculum Subject List with filtering, Edit, and Cascading Deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors

    val statsList by viewModel.subjectStats.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var filterMode by remember { mutableStateOf("all") }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    // On-Demand Modal Bottom Sheets
    var showSlotEditorBottomSheet by remember { mutableStateOf(false) }
    var showBunkSimulatorSheet by remember { mutableStateOf(false) }
    var selectedWeekDay by remember { mutableIntStateOf(1) }
    var editingSlotItem by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var showCreateSlotDialog by remember { mutableStateOf(false) }

    val totalSafeBunks = remember(statsList) { statsList.sumOf { it.bunksAvailable } }
    val totalSavedClasses = summary.totalFacultyCancelled + summary.totalCollegeOff
    val isOverallSafe = summary.overallPercentage >= 75.0
    val statusColor = if (isOverallSafe) colors.safeGreen else colors.dangerRed

    val filteredStats = remember(statsList, filterMode) {
        when (filterMode) {
            "risk" -> statsList.filter { it.percentage < 75.0 }
            "theory" -> statsList.filter { it.subject.type.equals("theory", ignoreCase = true) }
            "practical" -> statsList.filter { !it.subject.type.equals("theory", ignoreCase = true) }
            else -> statsList
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // SECTION 1: REFINED OVERALL TELEMETRY CARD
        // ==========================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overall_telemetry_header_card"),
                innerPadding = 14.dp,
                borderColors = listOf(colors.primaryAccent.copy(alpha = 0.45f), Color.Transparent)
            ) {
                // Top Row: Large % + Status Pill on Left | [Slots] & [Simulator] Action Chips on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format("%.1f%%", summary.overallPercentage),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.16f))
                                .border(0.5.dp, statusColor.copy(alpha = 0.55f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isOverallSafe) "SAFE ZONE" else "AT RISK",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Compact [Slots] Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.primaryAccent.copy(alpha = 0.14f))
                                .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    showSlotEditorBottomSheet = true
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("open_slots_sheet_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Slots",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Slots",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAccent
                            )
                        }

                        // Compact [Simulator] Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.warnAmber.copy(alpha = 0.14f))
                                .border(0.5.dp, colors.warnAmber.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    showBunkSimulatorSheet = true
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("open_bunk_simulator_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Simulator",
                                tint = colors.warnAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Simulator",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.warnAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Middle: Slim Vernier Precision Bar with 75% Statutory Cutoff Tick Mark
                SlimVernierPrecisionBar(
                    percentage = summary.overallPercentage.toFloat(),
                    showHeader = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%", fontSize = 9.sp, color = colors.textMuted)
                    Text("75% Statutory Cutoff", fontSize = 9.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                    Text("100%", fontSize = 9.sp, color = colors.textMuted)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom 3-Column Metrics: Attended (X/Y) | Safe Margin (+N) | Saved (Cancelled + Off)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Attended", fontSize = 9.5.sp, color = colors.textSecondary)
                        Text(
                            text = "${summary.totalAttended} / ${summary.totalConducted}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Safe Margin", fontSize = 9.5.sp, color = colors.textSecondary)
                        Text(
                            text = if (totalSafeBunks > 0) "+$totalSafeBunks Bunks" else "0 Margin",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSafeBunks > 0) colors.safeGreen else colors.dangerRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saved (C+Off)", fontSize = 9.5.sp, color = colors.textSecondary)
                        Text(
                            text = "$totalSavedClasses Classes",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.warnAmber
                        )
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: CURRICULUM SUBJECT LIST
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf(
                        "all" to "All (${statsList.size})",
                        "risk" to "Risk (${summary.atRiskSubjectCount + summary.criticalSubjectCount})",
                        "theory" to "Theory",
                        "practical" to "Labs"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = (filterMode == key)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.2f) else colors.surfaceVariant)
                                .border(
                                    0.5.dp,
                                    if (isSelected) colors.primaryAccent else colors.border,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    filterMode = key
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.primaryAccent else colors.textSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        CaliperSoundManager.playSnap(view)
                        showAddSubjectDialog = true
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primaryAccent.copy(alpha = 0.18f))
                        .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.45f), CircleShape)
                        .testTag("add_subject_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        items(filteredStats, key = { it.subject.id }) { stat ->
            SubjectCardWithMenu(
                stat = stat,
                onEdit = { editingSubject = stat.subject },
                onDelete = { subjectToDelete = stat.subject }
            )
        }
    }

    // =========================================================================
    // ON-DEMAND BUNK SIMULATOR MODAL BOTTOM SHEET (Overall + Subject-Wise Modes)
    // =========================================================================
    if (showBunkSimulatorSheet) {
        var simulatorMode by remember { mutableStateOf("overall") } // "overall" or "subject"
        var selectedSubjectId by remember(statsList) {
            mutableLongStateOf(statsList.firstOrNull()?.subject?.id ?: -1L)
        }
        var simulatedBunkCount by remember { mutableFloatStateOf(0f) }

        val activeStat = statsList.find { it.subject.id == selectedSubjectId } ?: statsList.firstOrNull()
        val baseAttended = if (simulatorMode == "overall") summary.totalAttended else (activeStat?.attended ?: 0)
        val baseConducted = if (simulatorMode == "overall") summary.totalConducted else (activeStat?.totalConducted ?: 0)
        val basePct = if (simulatorMode == "overall") summary.overallPercentage else (activeStat?.percentage ?: 100.0)

        val bunksInt = simulatedBunkCount.toInt()
        val projectedConducted = baseConducted + bunksInt
        val projectedPct = if (projectedConducted > 0) {
            (baseAttended.toDouble() / projectedConducted.toDouble()) * 100.0
        } else 100.0
        val projectedSafe = projectedPct >= 75.0
        val simAccent = if (projectedSafe) colors.safeGreen else colors.dangerRed

        ModalBottomSheet(
            onDismissRequest = { showBunkSimulatorSheet = false },
            containerColor = colors.surfaceVariant,
            scrimColor = Color.Black.copy(alpha = 0.72f),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.offSlate) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
                    .testTag("bunk_simulator_sheet")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.warnAmber.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bunk Simulator",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Project exact post-bunk attendance impact",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    IconButton(onClick = { showBunkSimulatorSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2-Mode Segmented Switch: [Overall | Subject-Wise]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    listOf("overall" to "Overall Aggregate", "subject" to "Subject-Wise").forEach { (modeKey, modeLabel) ->
                        val isActive = (simulatorMode == modeKey)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isActive) colors.primaryAccent else Color.Transparent)
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    simulatorMode = modeKey
                                    simulatedBunkCount = 0f
                                }
                                .padding(vertical = 8.dp)
                                .testTag("sim_mode_$modeKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeLabel,
                                color = if (isActive) colors.onPrimaryAccent else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Subject Selector Chips when in Subject-Wise mode
                if (simulatorMode == "subject" && statsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statsList.forEach { stat ->
                            val isChosen = (stat.subject.id == activeStat?.subject?.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) colors.primaryAccent.copy(alpha = 0.2f) else colors.surface)
                                    .border(
                                        0.75.dp,
                                        if (isChosen) colors.primaryAccent else colors.border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        CaliperSoundManager.playSnap(view)
                                        CaliperHapticManager.tick(context)
                                        selectedSubjectId = stat.subject.id
                                        simulatedBunkCount = 0f
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stat.subject.code,
                                        color = if (isChosen) colors.primaryAccent else colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format("%.1f", stat.percentage)}%",
                                        color = if (stat.percentage >= 75.0) colors.safeGreen else colors.dangerRed,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Projection Readout Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(0.75.dp, simAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (simulatorMode == "overall") "Projected Aggregate" else (activeStat?.subject?.name ?: "Subject"),
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.primaryAccent.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "+$bunksInt Bunk${if (bunksInt == 1) "" else "s"}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f%%", projectedPct),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = simAccent
                                )
                                if (bunksInt > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val delta = projectedPct - basePct
                                    Text(
                                        text = String.format("(%.1f%%)", delta),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.dangerRed,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (projectedSafe) "ELIGIBLE (≥ 75%)" else "DEBARRED (< 75%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = simAccent
                                )
                                Text(
                                    text = "$baseAttended / $projectedConducted conducted",
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SlimVernierPrecisionBar(
                            percentage = projectedPct.toFloat(),
                            showHeader = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Bunk Slider
                Slider(
                    value = simulatedBunkCount,
                    onValueChange = { newVal ->
                        if (newVal.toInt() != bunksInt) {
                            CaliperHapticManager.tick(context)
                        }
                        simulatedBunkCount = newVal
                    },
                    valueRange = 0f..15f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = simAccent,
                        activeTrackColor = simAccent,
                        inactiveTrackColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0 Bunks", fontSize = 10.sp, color = colors.textSecondary)
                    Text("+7 Lectures", fontSize = 10.sp, color = colors.textSecondary)
                    Text("+15 Max", fontSize = 10.sp, color = colors.textSecondary)
                }
            }
        }
    }

    // Weekday Slot Editor Bottom Sheet
    if (showSlotEditorBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlotEditorBottomSheet = false },
            containerColor = colors.surfaceVariant,
            scrimColor = Color.Black.copy(alpha = 0.72f),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.offSlate) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
                    .heightIn(min = 360.dp, max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Timetable Slot Editor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Manage weekly slots and room mappings",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    Button(
                        onClick = { showCreateSlotDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Slot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (dayNum, dayLabel) ->
                        val isChosen = (selectedWeekDay == dayNum)
                        val slotCount = allSlots.count { it.dayOfWeek == dayNum }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) colors.primaryAccent.copy(alpha = 0.2f) else colors.surface)
                                .border(0.5.dp, if (isChosen) colors.primaryAccent else colors.border, RoundedCornerShape(8.dp))
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    selectedWeekDay = dayNum
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChosen) colors.primaryAccent else colors.textSecondary
                                )
                                Text(
                                    text = "$slotCount",
                                    fontSize = 9.sp,
                                    color = if (isChosen) colors.primaryAccent else colors.textMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val daySlots = allSlots.filter { it.dayOfWeek == selectedWeekDay }.sortedBy { it.startTime }

                if (daySlots.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = colors.offSlate, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No slots scheduled for this day.", color = colors.textSecondary, fontSize = 13.sp)
                            Text("Tap 'Add Slot' to configure a lecture or lab.", color = colors.textMuted, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(daySlots, key = { it.id }) { slot ->
                            val sub = subjects.find { it.id == slot.subjectId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${slot.startTime} – ${slot.endTime}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.surfaceVariant)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(slot.roomNo, fontSize = 10.sp, color = colors.textSecondary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sub?.name ?: "Unknown Subject",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${sub?.code ?: ""} • ${sub?.facultyName ?: ""}",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingSlotItem = slot },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Slot", tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            CaliperHardwareEngine.playSound(CaliperHardwareEngine.THUD)
                                            CaliperHardwareEngine.pulseHaptic(context, true)
                                            viewModel.deleteSlot(slot.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Slot", tint = colors.dangerRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AddEditSubjectDialog(
            initialSubject = null,
            onDismiss = { showAddSubjectDialog = false },
            onSave = { newSub ->
                viewModel.saveSubject(newSub)
                showAddSubjectDialog = false
            }
        )
    }

    if (editingSubject != null) {
        AddEditSubjectDialog(
            initialSubject = editingSubject,
            onDismiss = { editingSubject = null },
            onSave = { updatedSub ->
                viewModel.saveSubject(updatedSub)
                editingSubject = null
            }
        )
    }

    if (subjectToDelete != null) {
        val sub = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            containerColor = colors.surfaceVariant,
            title = {
                Text(
                    text = "Delete ${sub.code}?",
                    fontWeight = FontWeight.Bold,
                    color = colors.dangerRed
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${sub.name}?\n\nWarning: This will permanently remove all associated timetable slots, attendance records, and test marks for this subject. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubjectCascade(sub)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.dangerRed)
                ) {
                    Text("Delete Subject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (editingSlotItem != null) {
        val targetSlot = editingSlotItem!!
        EditSlotDialog(
            slot = targetSlot,
            subjects = subjects,
            onDismissRequest = { editingSlotItem = null },
            onSaveSlot = { updatedSlot ->
                viewModel.saveSlot(updatedSlot)
                editingSlotItem = null
            },
            onDeleteSlot = { slotId ->
                viewModel.deleteSlot(slotId)
                editingSlotItem = null
            }
        )
    }

    if (showCreateSlotDialog) {
        val initialSlot = remember(selectedWeekDay, subjects) {
            TimetableSlotEntity(
                id = 0L,
                dayOfWeek = selectedWeekDay,
                startTime = "08:00",
                endTime = "09:00",
                roomNo = "LT-4",
                subjectId = subjects.firstOrNull()?.id ?: 1L
            )
        }
        EditSlotDialog(
            slot = initialSlot,
            subjects = subjects,
            onDismissRequest = { showCreateSlotDialog = false },
            onSaveSlot = { newSlot ->
                viewModel.saveSlot(newSlot)
                showCreateSlotDialog = false
            },
            onDeleteSlot = {
                showCreateSlotDialog = false
            }
        )
    }
}

@Composable
fun SubjectCardWithMenu(
    stat: SubjectAttendanceStats,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = CaliperTheme.colors
    val isSafe = stat.percentage >= 75.0
    val statusColor = if (isSafe) colors.safeGreen else colors.dangerRed
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        innerPadding = 11.dp,
        borderColors = listOf(statusColor.copy(alpha = 0.45f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stat.subject.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surface)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = stat.subject.type.uppercase(),
                            fontSize = 8.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stat.subject.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = stat.subject.facultyName,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f%%", stat.percentage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Subject Options",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = colors.surfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details", color = colors.textPrimary, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Subject", color = colors.dangerRed, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.dangerRed, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Attended", fontSize = 9.sp, color = colors.textSecondary)
                Text(
                    text = "${stat.attended} Classes",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.safeGreen
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Conducted", fontSize = 9.sp, color = colors.textSecondary)
                Text(
                    text = "${stat.totalConducted} Classes",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Safe Buffer", fontSize = 9.sp, color = colors.textSecondary)
                Text(
                    text = if (stat.bunksAvailable > 0) "+${stat.bunksAvailable} Bunks Safe" else "Need ${stat.classesNeeded} Classes",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stat.bunksAvailable > 0) colors.safeGreen else colors.dangerRed
                )
            }
        }
    }
}
