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
import com.example.audio.CaliperHardwareEngine
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.*
import com.example.ui.dialogs.AddEditSubjectDialog
import com.example.ui.dialogs.EditSlotDialog
import com.example.ui.dialogs.ReconciliationDialog
import com.example.ui.theme.*
import kotlin.math.floor
import kotlin.math.max

/**
 * SafeZoneScreen — Academic Safe-Zone & Telemetry Dashboard
 * - Slim Vernier Precision Bar (Replaces bulky 180° circular speedometer)
 * - Slim Bunk Buffer Slider (Compact 4.dp track, minimal vertical padding)
 * - Frosted 📅 Calendar/Timetable Action Logo Button in Header (opens slot editor modal bottom sheet)
 * - Real-time statutory 75% cutoff calculation with zero lag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statsList by viewModel.subjectStats.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var filterMode by remember { mutableStateOf("all") } // "all", "risk", "theory", "practical"
    var reconcilingStats by remember { mutableStateOf<SubjectAttendanceStats?>(null) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    // Slot Editor Bottom Sheet State
    var showSlotEditorBottomSheet by remember { mutableStateOf(false) }
    var selectedWeekDay by remember { mutableIntStateOf(1) } // 1 (Mon) .. 6 (Sat)
    var editingSlotItem by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var showCreateSlotDialog by remember { mutableStateOf(false) }

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
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Global Metric Telemetry Vernier Precision Bar Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_metric_ring_card"),
                innerPadding = 12.dp,
                borderColors = listOf(IceBlue.copy(alpha = 0.5f), GlassBorderBottom)
            ) {
                // Header Row: Title & Frosted Calendar/Timetable Logo Button
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
                            text = "SAFE-ZONE TELEMETRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )
                    }

                    // Frosted 📅 Calendar/Timetable Action Logo Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.16f))
                            .border(0.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable {
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)
                                showSlotEditorBottomSheet = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Slots",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Slim Vernier Precision Bar (Replaces bulky 180° circular dial)
                SlimVernierPrecisionBar(
                    percentage = summary.overallPercentage.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mini metrics telemetry bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.totalAttended} / ${summary.totalConducted}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Attended", fontSize = 9.sp, color = ArchitecturalTitanium)
                    }
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.totalProxy}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IceBlue)
                        Text("Proxies", fontSize = 9.sp, color = ArchitecturalTitanium)
                    }
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.totalFacultyCancelled}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedChampagneAmber)
                        Text("Cancelled", fontSize = 9.sp, color = ArchitecturalTitanium)
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(StrictRed.copy(alpha = 0.25f))
                                .border(1.dp, StrictRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StrictRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (summary.criticalSubjectCount > 0) "ADMIT CARD DETENTION RISK!" else "ATTENDANCE SAFE-ZONE WARNING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.criticalSubjectCount > 0) StrictRed else WarningAmber,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${summary.atRiskSubjectCount + summary.criticalSubjectCount} subjects ($atRiskNames) are under the mandatory 75% cutoff.",
                                fontSize = 11.5.sp,
                                color = TextPrimary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Slim Bunk Buffer Slider (Mass Bunk Simulator)
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
                val safe = floor((summary.totalAttended.toDouble() - (0.75 * summary.totalConducted.toDouble())) / 0.75).toInt()
                max(0, safe)
            } else {
                0
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mass_bunk_simulator_card"),
                innerPadding = 10.dp,
                backgroundColor = Color(0x0DFFFFFF),
                borderColors = listOf(IceBlue.copy(alpha = 0.4f), GlassBorderBottom)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MASS BUNK SIMULATOR",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceBlue,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (simulatedPct >= 75.0) SageMint.copy(alpha = 0.18f) else SoftCoral.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (bunksInt == 0) "Buffer: $maxSafeBunks Bunks" else "+$bunksInt Bunks",
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
                            Text("Projected Aggregate", fontSize = 9.5.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f%%", simulatedPct),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (simulatedPct >= 75.0) SageMint else SoftCoral
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (bunksInt > 0) {
                                    val delta = simulatedPct - summary.overallPercentage
                                    Text(
                                        text = String.format("(%.1f%%)", delta),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftCoral
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (simulatedPct >= 75.0) "ELIGIBLE" else "DETAINED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (simulatedPct >= 75.0) SageMint else SoftCoral
                            )
                            Text(
                                text = "$simulatedAttended / $simulatedConducted",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Slim Tactile Slider (Compact 4.dp track)
                    Slider(
                        value = simulatedBunkCount,
                        onValueChange = { newVal ->
                            if (newVal.toInt() != bunksInt) {
                                CaliperHardwareEngine.pulseHaptic(context)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 Bunks", fontSize = 8.5.sp, color = TextMuted)
                        Text("+7 Mass Bunks", fontSize = 8.5.sp, color = TextMuted)
                        Text("+15 Maximum", fontSize = 8.5.sp, color = TextMuted)
                    }
                }
            }
        }

        // 4. Category Filter Chips & Add Subject Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf(
                        "all" to "All (${statsList.size})",
                        "risk" to "Debar Risk (${summary.atRiskSubjectCount + summary.criticalSubjectCount})",
                        "theory" to "Theory",
                        "practical" to "Labs"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = (filterMode == key)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                .border(
                                    0.5.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                    CaliperHardwareEngine.pulseHaptic(context)
                                    filterMode = key
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showAddSubjectDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 5. Individual Subject Attendance Cards
        items(filteredStats, key = { it.subject.id }) { stat ->
            SubjectSafeZoneCard(
                stat = stat,
                onReconcileClick = { reconcilingStats = stat },
                onEditClick = { editingSubject = stat.subject },
                onQuickAdjust = { deltaAtt, deltaTot ->
                    viewModel.reconcileSubject(
                        stat.subject.id,
                        stat.attended + deltaAtt,
                        stat.totalConducted + deltaTot
                    )
                }
            )
        }
    }

    // Modal Bottom Sheet: Weekly Academic Timetable & Slot Editor
    if (showSlotEditorBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlotEditorBottomSheet = false },
            containerColor = Color(0xF2060A12),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WEEKLY ACADEMIC TIMETABLE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Manage recurring lectures, labs & room numbers",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = { showCreateSlotDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Slot", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weekday Selector Chips (Mon - Sat)
                val days = listOf(
                    1 to "Mon", 2 to "Tue", 3 to "Wed",
                    4 to "Thu", 5 to "Fri", 6 to "Sat"
                )
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
                                .background(if (isChosen) NeonCyan.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                .border(0.5.dp, if (isChosen) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                    CaliperHardwareEngine.pulseHaptic(context)
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
                                    color = if (isChosen) NeonCyan else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$slotCount",
                                    fontSize = 9.sp,
                                    color = if (isChosen) NeonCyan else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val daySlots = allSlots.filter { it.dayOfWeek == selectedWeekDay }.sortedBy { it.startTime }

                if (daySlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No slots scheduled for this day.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("Tap '+ Add Slot' to configure a lecture or lab.", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(daySlots, key = { it.id }) { slot ->
                            val sub = subjects.find { it.id == slot.subjectId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF131C2E))
                                    .border(0.5.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
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
                                            color = NeonCyan
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(slot.roomNo, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sub?.name ?: "Unknown Subject",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${sub?.code ?: ""} • ${sub?.facultyName ?: ""}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingSlotItem = slot },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Slot", tint = IceBlue, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            CaliperHardwareEngine.playSound(CaliperHardwareEngine.THUD)
                                            CaliperHardwareEngine.pulseHaptic(context, true)
                                            viewModel.deleteSlot(slot.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Slot", tint = StrictRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
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

    if (reconcilingStats != null) {
        ReconciliationDialog(
            stats = reconcilingStats!!,
            onDismiss = { reconcilingStats = null },
            onConfirm = { officialAtt, officialTot ->
                viewModel.reconcileSubject(reconcilingStats!!.subject.id, officialAtt, officialTot)
                reconcilingStats = null
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

/**
 * Compact, Breathing Individual Subject Card with Statutory Safe Zone Indicators
 */
@Composable
fun SubjectSafeZoneCard(
    stat: SubjectAttendanceStats,
    onReconcileClick: () -> Unit,
    onEditClick: () -> Unit,
    onQuickAdjust: (deltaAttended: Int, deltaConducted: Int) -> Unit
) {
    val context = LocalContext.current
    val isSafe = stat.percentage >= 75.0
    val statusColor = if (isSafe) NeonEmerald else StrictRed

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        innerPadding = 11.dp,
        borderColors = listOf(statusColor.copy(alpha = 0.5f), GlassBorderBottom)
    ) {
        // Top Row: Code, Name, Faculty, and Edit action
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
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = stat.subject.type.uppercase(),
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stat.subject.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stat.subject.facultyName,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.1f%%", stat.percentage),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Subject",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mini metrics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x1100E676))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stat.attended} Present",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonEmerald
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x11EF4444))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stat.bunked} Bunked",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StrictRed
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x11F59E0B))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stat.facultyCancelled} Off",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Buffer Prediction Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSafe) Color(0x1400E676) else Color(0x1DEF4444))
                .border(0.5.dp, if (isSafe) NeonEmerald.copy(alpha = 0.35f) else StrictRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSafe) {
                        "Safe Buffer: ${stat.bunksAvailable} classes safe to miss (≥75% margin)"
                    } else {
                        "Detention Risk! Attend next ${stat.classesNeeded} classes to restore 75%"
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions: Reconcile & Quick 1-tap increments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onReconcileClick,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = IceSky, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync Register", color = IceSky, fontSize = 11.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        CaliperHardwareEngine.playSound(CaliperHardwareEngine.SUCCESS)
                        CaliperHardwareEngine.pulseHaptic(context)
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
                        CaliperHardwareEngine.playSound(CaliperHardwareEngine.THUD)
                        CaliperHardwareEngine.pulseHaptic(context, true)
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
