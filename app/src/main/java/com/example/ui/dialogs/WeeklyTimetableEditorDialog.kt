package com.example.ui.dialogs

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*

@Composable
fun WeeklyTimetableEditorDialog(
    slots: List<TimetableSlotEntity>,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSaveSlot: (TimetableSlotEntity) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    onAddNewSubject: (SubjectEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedDayOfWeek by remember { mutableStateOf(1) } // 1 = Monday to 6 = Saturday
    var showAddForm by remember { mutableStateOf(false) }
    var showNewSubjectDialog by remember { mutableStateOf(false) }

    // Form fields
    var formSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var formStartTime by remember { mutableStateOf("09:00") }
    var formEndTime by remember { mutableStateOf("10:00") }
    var formRoomNo by remember { mutableStateOf("LT-4") }

    val daysList = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat"
    )

    val currentDaySlots = remember(slots, selectedDayOfWeek) {
        slots.filter { it.dayOfWeek == selectedDayOfWeek }
            .sortedBy { it.startTime }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBackground.copy(alpha = 0.95f))
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AmoledCardSurface),
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, GlassBorderTop, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Weekly Timetable Manager",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Configure recurring slots for Mon–Sat",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Day of Week Selector Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x221F293D))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysList.forEach { (dayInt, label) ->
                            val isSelected = (selectedDayOfWeek == dayInt)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.22f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        triggerHapticFeedback(context, false)
                                        selectedDayOfWeek = dayInt
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action bar: Slots count & Add Slot button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${daysList.find { it.first == selectedDayOfWeek }?.second} Schedule (${currentDaySlots.size} slots)",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Button(
                            onClick = {
                                if (subjects.isEmpty()) {
                                    showNewSubjectDialog = true
                                } else {
                                    if (formSubjectId == 0L) {
                                        formSubjectId = subjects.first().id
                                    }
                                    showAddForm = !showAddForm
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showAddForm) StrictRed.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (showAddForm) StrictRed else NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("toggle_add_slot_form_btn")
                        ) {
                            Icon(
                                imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (showAddForm) StrictRed else NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showAddForm) "Cancel" else "+ Add Slot",
                                color = if (showAddForm) StrictRed else NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slot list & inline add form inside a scrollable column
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Inline Add Slot Form
                        if (showAddForm) {
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColors = listOf(NeonCyan, GlassBorderBottom)
                                ) {
                                    Text(
                                        text = "Add New Timetable Slot",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Subject selection
                                    Text("Select Subject", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (subjects.isEmpty()) {
                                        Button(
                                            onClick = { showNewSubjectDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("+ Create Subject First", color = NeonEmerald, fontSize = 12.sp)
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            subjects.forEach { sub ->
                                                val isChosen = (formSubjectId == sub.id)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isChosen) NeonCyan.copy(alpha = 0.18f) else Color(0x1AFFFFFF))
                                                        .border(0.5.dp, if (isChosen) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable { formSubjectId = sub.id }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isChosen,
                                                        onClick = { formSubjectId = sub.id },
                                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(sub.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text("${sub.code} • ${sub.facultyName}", color = TextSecondary, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Time Presets
                                    Text("Start & End Time (24h or HH:mm)", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = formStartTime,
                                            onValueChange = { formStartTime = it },
                                            label = { Text("Start Time", color = TextSecondary, fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = GlassBorderTop
                                            ),
                                            modifier = Modifier.weight(1f).testTag("slot_start_time_input")
                                        )

                                        OutlinedTextField(
                                            value = formEndTime,
                                            onValueChange = { formEndTime = it },
                                            label = { Text("End Time", color = TextSecondary, fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = GlassBorderTop
                                            ),
                                            modifier = Modifier.weight(1f).testTag("slot_end_time_input")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Quick time chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val timePresets = listOf(
                                            "09:00" to "10:00",
                                            "10:00" to "11:00",
                                            "11:15" to "12:15",
                                            "12:15" to "13:15",
                                            "14:00" to "16:00"
                                        )
                                        timePresets.forEach { (st, et) ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x22FFFFFF))
                                                    .clickable {
                                                        formStartTime = st
                                                        formEndTime = et
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text("$st-$et", color = IceSky, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Room No Input
                                    OutlinedTextField(
                                        value = formRoomNo,
                                        onValueChange = { formRoomNo = it },
                                        label = { Text("Room No / Venue (e.g. LT-4, CAD Lab)", color = TextSecondary, fontSize = 11.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = GlassBorderTop
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("slot_room_no_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (formSubjectId > 0 && formStartTime.isNotBlank() && formEndTime.isNotBlank()) {
                                                val newSlot = TimetableSlotEntity(
                                                    dayOfWeek = selectedDayOfWeek,
                                                    startTime = formStartTime.trim(),
                                                    endTime = formEndTime.trim(),
                                                    roomNo = formRoomNo.trim().ifEmpty { "Room 101" },
                                                    subjectId = formSubjectId
                                                )
                                                onSaveSlot(newSlot)
                                                showAddForm = false
                                                triggerHapticFeedback(context, false)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("save_new_slot_btn")
                                    ) {
                                        Text("Save Recurring Slot", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Empty State if no slots for this day
                        if (currentDaySlots.isEmpty() && !showAddForm) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = IceSky,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No Slots on ${daysList.find { it.first == selectedDayOfWeek }?.second}",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Tap '+ Add Slot' to configure a lecture or lab period for this day.",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // List of Existing Slots for Selected Day
                        items(currentDaySlots, key = { it.id }) { slot ->
                            val sub = subjects.find { it.id == slot.subjectId }
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${slot.startTime} - ${slot.endTime}",
                                                color = NeonCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x22FFFFFF))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(slot.roomNo, color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = sub?.name ?: "Subject #${slot.subjectId}",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${sub?.code ?: ""} • ${sub?.facultyName ?: ""}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            triggerHapticFeedback(context, true)
                                            onDeleteSlot(slot.id)
                                        },
                                        modifier = Modifier.testTag("delete_slot_${slot.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Slot",
                                            tint = StrictRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Done Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done / Close", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNewSubjectDialog) {
        AddEditSubjectDialog(
            onDismiss = { showNewSubjectDialog = false },
            onSave = { newSub ->
                onAddNewSubject(newSub)
                showNewSubjectDialog = false
            }
        )
    }
}
