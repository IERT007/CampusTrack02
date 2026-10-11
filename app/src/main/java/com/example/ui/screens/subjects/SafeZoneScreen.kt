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
import com.example.ui.theme.*
import kotlin.math.floor
import kotlin.math.max

/**
 * SafeZoneScreen — Academic Safe-Zone & Telemetry Dashboard
 * 3 Clear Vertical Sections:
 * 1. Overall Telemetry Header: Total Classes Attended/Conducted and Total Safe Bunks Available.
 * 2. Bunk Simulator: Minimal slider calculating aggregate projection.
 * 3. Curriculum Subject List: Each card clearly displays Attended, Total Conducted, and Safe Bunk Buffer,
 *    plus a 3-dots menu providing Edit Subject details and Cascading Subject Deletion.
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
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    // Slot Editor Bottom Sheet State
    var showSlotEditorBottomSheet by remember { mutableStateOf(false) }
    var selectedWeekDay by remember { mutableIntStateOf(1) } // 1 (Mon) .. 6 (Sat)
    var editingSlotItem by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var showCreateSlotDialog by remember { mutableStateOf(false) }

    // Calculate total aggregate safe bunks available
    val totalSafeBunks = remember(statsList) {
        statsList.sumOf { it.bunksAvailable }
    }

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
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // SECTION 1: OVERALL TELEMETRY HEADER
        // ==========================================
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overall_telemetry_header_card"),
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
                            text = "OVERALL TELEMETRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )
                    }

                    // Frosted 📅 Calendar/Slots Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.16f))
                            .border(0.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)
                                showSlotEditorBottomSheet = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 11.sp)
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

                // Slim Vernier Precision Bar with 75% tick mark
                SlimVernierPrecisionBar(
                    percentage = summary.overallPercentage.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Telemetry Metrics Row: Attended/Conducted & Total Safe Bunks Available
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Classes Attended", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = "${summary.totalAttended} / ${summary.totalConducted}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Safe Bunk Margin", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = if (totalSafeBunks > 0) "+$totalSafeBunks Safe Bunks" else "0 Bunks Remaining",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSafeBunks > 0) SageMint else SoftCoral
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Aggregate Status", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = if (summary.overallPercentage >= 75.0) "SAFE ZONE" else "DEBAR RISK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.overallPercentage >= 75.0) SageMint else SoftCoral
                        )
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: BUNK SIMULATOR
        // ==========================================
        item {
            var simulatedBunkCount by remember { mutableFloatStateOf(0f) }
            val bunksInt = simulatedBunkCount.toInt()

            val simulatedConducted = summary.totalConducted + bunksInt
            val simulatedAttended = summary.totalAttended
            val simulatedPct = if (simulatedConducted > 0) {
                (simulatedAttended.toDouble() / simulatedConducted.toDouble()) * 100.0
            } else 100.0

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bunk_simulator_card"),
                innerPadding = 12.dp,
                borderColors = listOf(NeonCyan.copy(alpha = 0.35f), GlassBorderBottom)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧮", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BUNK SIMULATOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceSky,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+$bunksInt Bunk${if (bunksInt == 1) "" else "s"} Projected",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Projected Aggregate", fontSize = 9.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f%%", simulatedPct),
                                    fontSize = 17.sp,
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
                                text = if (simulatedPct >= 75.0) "ELIGIBLE (≥ 75%)" else "DEBARRED (< 75%)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (simulatedPct >= 75.0) SageMint else SoftCoral
                            )
                            Text(
                                text = "$simulatedAttended / $simulatedConducted total",
                                fontSize = 9.5.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Minimal slider track
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
                        Text("+7 Buffer", fontSize = 8.5.sp, color = TextMuted)
                        Text("+15 Max", fontSize = 8.5.sp, color = TextMuted)
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3: CURRICULUM SUBJECT LIST
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
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Individual Subject Cards with 3-Dots Menu & Cascade Deletion
        items(filteredStats, key = { it.subject.id }) { stat ->
            SubjectCardWithMenu(
                stat = stat,
                onEdit = { editingSubject = stat.subject },
                onDelete = { subjectToDelete = stat.subject }
            )
        }
    }

    // Weekday Slot Editor Bottom Sheet
    if (showSlotEditorBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlotEditorBottomSheet = false },
            containerColor = Color(0xFF0A0F18),
            scrimColor = Color.Black.copy(alpha = 0.7f),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF475569)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .heightIn(min = 360.dp, max = 560.dp)
            ) {
                // Header
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
                            color = Color.White
                        )
                        Text(
                            text = "Manage weekly slots and room mappings",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = { showCreateSlotDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Slot", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // Add Subject Dialog
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

    // Edit Subject Dialog
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

    // Confirmation Dialog for Cascading Subject Deletion
    if (subjectToDelete != null) {
        val sub = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = {
                Text(
                    text = "Delete ${sub.code}?",
                    fontWeight = FontWeight.Bold,
                    color = StrictRed
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${sub.name}?\n\nWarning: This will permanently remove all associated timetable slots, attendance records, and test marks for this subject. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubjectCascade(sub)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StrictRed)
                ) {
                    Text("Delete Subject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel", color = Color.White)
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

/**
 * Subject Card with 3-Dots Menu & Cascading Deletion
 * Clearly displays: Attended, Total Conducted, and Safe Bunk Buffer.
 */
@Composable
fun SubjectCardWithMenu(
    stat: SubjectAttendanceStats,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isSafe = stat.percentage >= 75.0
    val statusColor = if (isSafe) NeonEmerald else StrictRed
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        innerPadding = 11.dp,
        borderColors = listOf(statusColor.copy(alpha = 0.5f), GlassBorderBottom)
    ) {
        // Header Row: Code, Name, Percentage, and 3-Dots Menu
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
                            fontSize = 8.5.sp,
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
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = Color(0xFF0F172A)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details", color = Color.White, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Subject", color = StrictRed, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StrictRed, modifier = Modifier.size(16.dp)) },
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

        // 3 Key Metrics: Attended, Total Conducted, and Safe Bunk Buffer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Attended", fontSize = 9.sp, color = TextMuted)
                Text(
                    text = "${stat.attended} Classes",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonEmerald
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Conducted", fontSize = 9.sp, color = TextMuted)
                Text(
                    text = "${stat.totalConducted} Classes",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Safe Buffer", fontSize = 9.sp, color = TextMuted)
                Text(
                    text = if (stat.bunksAvailable > 0) "+${stat.bunksAvailable} Bunks Safe" else "Need ${stat.classesNeeded} Classes",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stat.bunksAvailable > 0) SageMint else SoftCoral
                )
            }
        }
    }
}
