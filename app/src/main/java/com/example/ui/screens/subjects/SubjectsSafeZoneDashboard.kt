package com.example.ui.screens.subjects

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.*
import com.example.ui.dialogs.AddEditSubjectDialog
import com.example.ui.dialogs.EditSlotDialog
import com.example.ui.dialogs.ReconciliationDialog
import com.example.ui.theme.*

@Composable
fun SubjectsSafeZoneDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statsList by viewModel.subjectStats.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var filterMode by remember { mutableStateOf("all") } // "all", "risk", "theory", "practical"
    var reconcilingSubject by remember { mutableStateOf<SubjectAttendanceStats?>(null) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Timetable slot editor state inside SafeZone
    var selectedTimetableDay by remember { mutableStateOf(1) } // 1=Mon..6=Sat
    var showAddSlotBottomSheet by remember { mutableStateOf(false) }
    var editingSlotEntity by remember { mutableStateOf<TimetableSlotEntity?>(null) }

    // Add slot form fields
    var newSlotSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var newSlotStartTime by remember { mutableStateOf("09:00") }
    var newSlotEndTime by remember { mutableStateOf("10:00") }
    var newSlotRoomNo by remember { mutableStateOf("LT-4") }

    val activeDaySlots = remember(allSlots, selectedTimetableDay) {
        allSlots.filter { it.dayOfWeek == selectedTimetableDay }
            .sortedBy { it.startTime }
    }

    val filteredList = remember(statsList, filterMode) {
        when (filterMode) {
            "risk" -> statsList.filter { it.percentage < 75.0 }
            "theory" -> statsList.filter { it.subject.type.lowercase() == "theory" }
            "practical" -> statsList.filter { it.subject.type.lowercase() != "theory" }
            else -> statsList
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Global Metric Telemetry Dial Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_metric_ring_card"),
                borderColors = listOf(IceBlue.copy(alpha = 0.5f), GlassBorderBottom)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = IceBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAFE-ZONE TELEMETRY GAUGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "IERT 75% Rule",
                        fontSize = 11.sp,
                        color = ArchitecturalTitanium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TelemetrySafeZoneGauge(
                    percentage = summary.overallPercentage.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mini metrics telemetry bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.totalAttended} / ${summary.totalConducted}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Attended", fontSize = 10.sp, color = ArchitecturalTitanium)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.totalProxy}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IceBlue)
                        Text("Proxies", fontSize = 10.sp, color = ArchitecturalTitanium)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.totalFacultyCancelled}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedChampagneAmber)
                        Text("Cancelled", fontSize = 10.sp, color = ArchitecturalTitanium)
                    }
                }
            }
        }

        // 1.5 Dedicated Weekly Academic Timetable & Slot Editor Card
        item {
            val days = listOf(
                1 to "Mon",
                2 to "Tue",
                3 to "Wed",
                4 to "Thu",
                5 to "Fri",
                6 to "Sat"
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_timetable_editor_card"),
                borderColors = listOf(NeonCyan.copy(alpha = 0.5f), GlassBorderBottom)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header & Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Weekly Academic Timetable & Slot Editor",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Manage recurring lectures, labs & rooms",
                                    fontSize = 10.sp,
                                    color = ArchitecturalTitanium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (subjects.isNotEmpty() && newSlotSubjectId == 0L) {
                                    newSlotSubjectId = subjects.first().id
                                }
                                showAddSlotBottomSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.22f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_new_slot_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ Add Slot", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 1. Day Selector Chips (Mon - Sat)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.forEach { (dayInt, label) ->
                            val isSelected = (selectedTimetableDay == dayInt)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        triggerHapticFeedback(context, false)
                                        selectedTimetableDay = dayInt
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) NeonCyan else ArchitecturalTitanium,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 2. Active Slots for the Selected Day
                    if (activeDaySlots.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No lecture slots scheduled for ${days.find { it.first == selectedTimetableDay }?.second}. Tap '+ Add Slot'.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeDaySlots.forEach { slot ->
                                val sub = subjects.find { it.id == slot.subjectId }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { editingSlotEntity = slot }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeonCyan.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${slot.startTime} - ${slot.endTime}",
                                                        color = NeonCyan,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Room ${slot.roomNo}",
                                                    color = IceBlue,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = sub?.name ?: "Unknown Subject",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${sub?.code ?: "CODE"} • ${sub?.facultyName ?: "Faculty"}",
                                                color = ArchitecturalTitanium,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingSlotEntity = slot },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Slot",
                                                    tint = ArchitecturalTitanium,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    triggerHapticFeedback(context, true)
                                                    viewModel.deleteSlot(slot.id)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Slot",
                                                    tint = StrictRed.copy(alpha = 0.8f),
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
        }

        // 2. Detained / Admit Card Risk Alert Banner
        if (summary.atRiskSubjectCount > 0 || summary.criticalSubjectCount > 0) {
            item {
                val atRiskNames = statsList.filter { it.percentage < 75.0 }.joinToString { it.subject.code }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detained_risk_alert_banner"),
                    backgroundColor = if (summary.criticalSubjectCount > 0) DangerRed else WarningAmber,
                    borderColors = listOf(StrictRed, GlassBorderBottom)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StrictRed.copy(alpha = 0.25f))
                                .border(1.dp, StrictRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StrictRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (summary.criticalSubjectCount > 0) "ADMIT CARD DETENTION RISK!" else "ATTENDANCE SAFE-ZONE WARNING",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.criticalSubjectCount > 0) StrictRed else WarningAmber,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${summary.atRiskSubjectCount + summary.criticalSubjectCount} subjects ($atRiskNames) are under the mandatory 75% cutoff. Attend subsequent classes immediately to avoid exam hall bar.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // 2.5 Mass Bunk Impact Simulator Card
        item {
            var simulatedBunkCount by remember { mutableFloatStateOf(0f) }
            val bunksInt = simulatedBunkCount.toInt()

            val simulatedConducted = summary.totalConducted + bunksInt
            val simulatedAttended = summary.totalAttended
            val simulatedPct = if (simulatedConducted > 0) {
                (simulatedAttended.toDouble() / simulatedConducted.toDouble()) * 100.0
            } else {
                100.0
            }

            val maxSafeBunks = if (summary.overallPercentage >= 75.0 && summary.totalConducted > 0) {
                val safe = kotlin.math.floor((summary.totalAttended.toDouble() - (0.75 * summary.totalConducted.toDouble())) / 0.75).toInt()
                kotlin.math.max(0, safe)
            } else {
                0
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mass_bunk_simulator_card"),
                backgroundColor = Color(0x0DFFFFFF),
                borderColors = listOf(IceBlue.copy(alpha = 0.4f), GlassBorderBottom)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LinearScale,
                                contentDescription = null,
                                tint = IceBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MASS BUNK IMPACT SIMULATOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceBlue,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (simulatedPct >= 75.0) SageMint.copy(alpha = 0.18f) else SoftCoral.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (bunksInt == 0) "Safe Buffer: $maxSafeBunks Bunks" else "Simulating +$bunksInt Bunks",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (simulatedPct >= 75.0) SageMint else SoftCoral
                            )
                        }
                    }

                    // Simulation Metrics Projection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Projected Aggregate", fontSize = 11.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f%%", simulatedPct),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (simulatedPct >= 75.0) SageMint else SoftCoral
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (bunksInt > 0) {
                                    val delta = simulatedPct - summary.overallPercentage
                                    Text(
                                        text = String.format("(%.1f%%)", delta),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftCoral
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (simulatedPct >= 75.0) "ELIGIBLE FOR EXAMS" else "DETAINED FROM EXAMS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (simulatedPct >= 75.0) SageMint else SoftCoral
                            )
                            Text(
                                text = "$simulatedAttended / $simulatedConducted Conducted",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Tactile Slider
                    Slider(
                        value = simulatedBunkCount,
                        onValueChange = { newVal ->
                            if (newVal.toInt() != bunksInt) {
                                com.example.audio.CaliperHapticManager.tick(context)
                            }
                            simulatedBunkCount = newVal
                        },
                        valueRange = 0f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = if (simulatedPct >= 75.0) IceBlue else SoftCoral,
                            activeTrackColor = if (simulatedPct >= 75.0) IceBlue else SoftCoral,
                            inactiveTrackColor = Color(0x22FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 Bunks", fontSize = 10.sp, color = TextMuted)
                        Text("+7 Mass Bunks", fontSize = 10.sp, color = TextMuted)
                        Text("+15 Maximum", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        // 3. Category Filter Chips & Add Subject Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf(
                        "all" to "All (${statsList.size})",
                        "risk" to "At Risk (${statsList.count { it.percentage < 75.0 }})",
                        "theory" to "Theory",
                        "practical" to "Lab & Workshop"
                    )

                    filters.forEach { (mode, label) ->
                        val isSelected = (filterMode == mode)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    triggerHapticFeedback(context, false)
                                    filterMode = mode
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Subject",
                        tint = NeonCyan
                    )
                }
            }
        }

        // 4. Subject-Specific Glass Cards
        items(filteredList, key = { it.subject.id }) { stat ->
            SubjectGlassCard(
                stat = stat,
                onReconcileClick = { reconcilingSubject = stat },
                onEditClick = { editingSubject = stat.subject },
                onQuickAdjust = { deltaAtt, deltaCond ->
                    viewModel.reconcileSubject(
                        subjectId = stat.subject.id,
                        officialAttended = stat.attended + deltaAtt,
                        officialTotal = stat.totalConducted + deltaCond
                    )
                }
            )
        }
    }

    // Reconciliation Dialog
    reconcilingSubject?.let { stat ->
        ReconciliationDialog(
            stats = stat,
            onDismiss = { reconcilingSubject = null },
            onConfirm = { offAtt, offTot ->
                viewModel.reconcileSubject(stat.subject.id, offAtt, offTot)
            }
        )
    }

    // Add / Edit Subject Dialog
    if (showAddDialog || editingSubject != null) {
        AddEditSubjectDialog(
            initialSubject = editingSubject,
            onDismiss = {
                showAddDialog = false
                editingSubject = null
            },
            onSave = { subject ->
                viewModel.saveSubject(subject)
            }
        )
    }

    // Timetable Slot Bottom Sheet / Dialog for adding new slots
    if (showAddSlotBottomSheet) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddSlotBottomSheet = false }
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                backgroundColor = Color(0xF20C0E14),
                borderColors = listOf(NeonCyan.copy(alpha = 0.6f), GlassBorderBottom)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Slot: ${listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat").find { it.first == selectedTimetableDay }?.second}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        IconButton(onClick = { showAddSlotBottomSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ArchitecturalTitanium)
                        }
                    }

                    // Subject dropdown / chips
                    Text("Subject", fontSize = 11.sp, color = ArchitecturalTitanium)
                    if (subjects.isEmpty()) {
                        Text("No subjects found. Create a subject first.", color = SoftCoral, fontSize = 12.sp)
                    } else {
                        var expandedSubDropdown by remember { mutableStateOf(false) }
                        val curSelectedSub = subjects.find { it.id == newSlotSubjectId } ?: subjects.first()
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedSubDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${curSelectedSub.code} - ${curSelectedSub.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan)
                            }

                            DropdownMenu(
                                expanded = expandedSubDropdown,
                                onDismissRequest = { expandedSubDropdown = false },
                                modifier = Modifier.background(Color(0xFF141923))
                            ) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text("${sub.code} - ${sub.name}", color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            newSlotSubjectId = sub.id
                                            expandedSubDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Start & End Time TextFields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSlotStartTime,
                            onValueChange = { newSlotStartTime = it },
                            label = { Text("Start (e.g. 10:00)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSlotEndTime,
                            onValueChange = { newSlotEndTime = it },
                            label = { Text("End (e.g. 11:00)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Room No TextField
                    OutlinedTextField(
                        value = newSlotRoomNo,
                        onValueChange = { newSlotRoomNo = it },
                        label = { Text("Room No / Venue (e.g. LT-4, Workshop)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (newSlotSubjectId > 0 && newSlotStartTime.isNotBlank() && newSlotEndTime.isNotBlank()) {
                                val slot = TimetableSlotEntity(
                                    dayOfWeek = selectedTimetableDay,
                                    startTime = newSlotStartTime.trim(),
                                    endTime = newSlotEndTime.trim(),
                                    roomNo = newSlotRoomNo.trim().ifEmpty { "LT-1" },
                                    subjectId = newSlotSubjectId
                                )
                                viewModel.saveSlot(slot)
                                triggerHapticFeedback(context, false)
                                showAddSlotBottomSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Slot to Timetable", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Edit Slot Dialog
    editingSlotEntity?.let { slot ->
        EditSlotDialog(
            slot = slot,
            subjects = subjects,
            onDismissRequest = { editingSlotEntity = null },
            onSaveSlot = { updatedSlot ->
                viewModel.saveSlot(updatedSlot)
                editingSlotEntity = null
            },
            onDeleteSlot = { slotId ->
                viewModel.deleteSlot(slotId)
                editingSlotEntity = null
            }
        )
    }
}

@Composable
fun SubjectGlassCard(
    stat: SubjectAttendanceStats,
    onReconcileClick: () -> Unit,
    onEditClick: () -> Unit,
    onQuickAdjust: (deltaAttended: Int, deltaTotal: Int) -> Unit
) {
    val context = LocalContext.current
    val sub = stat.subject
    val percentage = stat.percentage
    val isSafe = percentage >= 75.0

    val statusColor = when {
        percentage >= 75.0 -> NeonEmerald
        percentage >= 65.0 -> WarningAmber
        else -> StrictRed
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(
            if (isSafe) GlassBorderTop else statusColor.copy(alpha = 0.7f),
            GlassBorderBottom
        )
    ) {
        // Row 1: Code, Type, Strictness & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x2200E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(sub.code, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                TypeBadge(sub.type)
                Spacer(modifier = Modifier.width(6.dp))
                StrictnessBadge(sub.strictness)
            }

            Row {
                IconButton(onClick = onReconcileClick, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Reconcile with Register",
                        tint = IceSky,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Subject",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Subject Title & Faculty
        Text(
            text = sub.name,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Faculty: ${sub.facultyName}",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: Progress Bar & Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${stat.attended} Attended / ${stat.totalConducted} Conducted",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f%%", percentage),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom Frosted Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33FFFFFF))
        ) {
            val progressFraction = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progressFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(statusColor.copy(alpha = 0.7f), statusColor)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Granular Breakdown Badges: Attended, Bunked, Faculty Cancelled, College Off
        val bunkedCount = kotlin.math.max(0, stat.totalConducted - stat.attended)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A00E676))
                    .border(0.5.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stat.attended}", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Attended", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AEF4444))
                    .border(0.5.dp, StrictRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$bunkedCount", color = StrictRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Bunked", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AF59E0B))
                    .border(0.5.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stat.facultyCancelled}", color = WarningAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Cancelled", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.5.dp, GlassBorderTop, RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stat.collegeOff}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("College Off", color = TextSecondary, fontSize = 9.sp)
                }
            }
        }

        val totalExcluded = stat.facultyCancelled + stat.collegeOff
        if (totalExcluded > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ℹ️ $totalExcluded cancelled/off lectures excluded from total conducted (0% penalty on 75% rule).",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 4: Predictive Bunk / Catch-up Engine Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSafe) Color(0x1400E676) else Color(0x1DEF4444))
                .border(
                    0.5.dp,
                    if (isSafe) NeonEmerald.copy(alpha = 0.4f) else StrictRed.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.BeachAccess else Icons.Default.RunningWithErrors,
                    contentDescription = null,
                    tint = if (isSafe) NeonEmerald else StrictRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (isSafe) {
                        Text(
                            text = if (stat.bunksAvailable > 0) {
                                "Safe Zone: You can skip ${stat.bunksAvailable} consecutive classes and stay ≥ 75%"
                            } else {
                                "On The Edge: Don't miss next lecture (0 bunks available)"
                            },
                            color = NeonEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Detention Risk: Attend next ${stat.classesNeeded} consecutive classes to reach 75%!",
                            color = StrictRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Real-time sync based on current 75% statutory rule.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 5: Quick Adjust buttons for instant register correction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReconcileClick) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = IceSky, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Reconcile", color = IceSky, fontSize = 11.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        onQuickAdjust(1, 1)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald)
                ) {
                    Text("+1 Present", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        onQuickAdjust(0, 1)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StrictRed)
                ) {
                    Text("+1 Bunk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
