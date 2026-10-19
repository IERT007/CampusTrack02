package com.example.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.CustomActivityEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestRecordEntity
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.SlimVernierPrecisionBar
import com.example.ui.dialogs.RetroactiveDatePickerDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    val selectedDateStr by viewModel.selectedDate.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val slots by viewModel.currentDaySlots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val customActivities by viewModel.currentDayActivities.collectAsState()
    val testRecords by viewModel.testRecords.collectAsState()
    val currentHoliday by viewModel.currentHoliday.collectAsState()
    val semesterStart by viewModel.semesterStartDate.collectAsState()
    val semesterEnd by viewModel.semesterEndDate.collectAsState()

    var isCompletedTrayExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExtraEventDialog by remember { mutableStateOf(false) }
    var showSemesterDatesDialog by remember { mutableStateOf(false) }

    val unmarkedSlots = remember(slots) { slots.filter { it.currentLog == null } }
    val markedSlots = remember(slots) { slots.filter { it.currentLog != null } }

    val parsedDate = remember(selectedDateStr) {
        runCatching { LocalDate.parse(selectedDateStr) }.getOrDefault(LocalDate.now())
    }
    val formattedDateLabel = remember(parsedDate) {
        parsedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US)).uppercase(Locale.US)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Telemetry Hero Card with SlimVernierPrecisionBar
        item(key = "telemetry_hero_card") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_hero_card"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SEMESTER TELEMETRY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    letterSpacing = 1.4.sp,
                                    color = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", summary.overallPercentage),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    color = if (summary.overallPercentage >= 75.0) colors.safeZone else colors.bunkDanger
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${summary.totalAttended}/${summary.totalConducted} LECTURES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (summary.criticalSubjectCount == 0) "ALL SUBJECTS SAFE"
                                else "${summary.criticalSubjectCount} BELOW 75% TARGET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    color = if (summary.criticalSubjectCount == 0) colors.safeZone else colors.bunkDanger
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    SlimVernierPrecisionBar(
                        percentage = summary.overallPercentage.toFloat()
                    )
                }
            }
        }

        // 2. Date Stepper & Quick Day Actions
        item(key = "date_stepper_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        viewModel.stepDay(-1)
                    },
                    modifier = Modifier.testTag("prev_day_button")
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141923).copy(alpha = 0.9f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .fluidTouchFeedback(
                            onClick = {
                                CaliperHapticManager.tick(context)
                                showDatePicker = true
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = colors.scheduledLecture,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDateLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                GlassIconButton(
                    icon = Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        viewModel.stepDay(1)
                    },
                    modifier = Modifier.testTag("next_day_button")
                )
            }
        }

        // 3. Batch Action Bar (Mark All Present / Extra Event)
        item(key = "batch_action_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .fluidTouchFeedback(
                            onClick = {
                                CaliperHapticManager.confirmTap(context)
                                CaliperSoundManager.playConfirm(context)
                                viewModel.markWholeDayPresent()
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.safeZone.copy(alpha = 0.14f))
                        .border(1.dp, colors.safeZone.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp)
                        .testTag("mark_all_present_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark All Present",
                        tint = colors.safeZone,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALL PRESENT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.safeZone
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .fluidTouchFeedback(
                            onClick = {
                                CaliperHapticManager.tick(context)
                                showExtraEventDialog = true
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.scheduledLecture.copy(alpha = 0.14f))
                        .border(1.dp, colors.scheduledLecture.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp)
                        .testTag("add_extra_event_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Extra Class",
                        tint = colors.scheduledLecture,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXTRA SLOT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.scheduledLecture
                        )
                    )
                }
            }
        }

        // Holiday Banner if applicable
        currentHoliday?.let { holiday ->
            item(key = "holiday_banner") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { viewModel.applyHolidayToCurrentDay(holiday.title) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GAZETTED HOLIDAY // ${holiday.title.uppercase(Locale.US)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    color = colors.deadlineWarning
                                )
                            )
                            Text(
                                text = "Tap to mark all slots College Off",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }
                }
            }
        }

        // 4. Unmarked Active Schedule Slots
        if (unmarkedSlots.isEmpty() && markedSlots.isEmpty()) {
            item(key = "empty_schedule_card") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO SCHEDULED SLOTS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Zero lectures scheduled for this day.",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
        } else {
            items(unmarkedSlots, key = { "unmarked_${it.slot.id}" }) { item ->
                val slotTest = testRecords.firstOrNull {
                    it.date == selectedDateStr && it.slotId == item.slot.id
                }
                SlotCard(
                    item = item,
                    testRecord = slotTest,
                    selectedDate = selectedDateStr,
                    onEditSlot = {},
                    onMarkAttendance = { status, isProxy, notes ->
                        CaliperHapticManager.confirmTap(context)
                        CaliperSoundManager.playConfirm(context)
                        viewModel.setSlotAttendance(
                            slotId = item.slot.id,
                            subjectId = item.subject.id,
                            status = status,
                            isProxy = isProxy,
                            notes = notes
                        )
                    },
                    onSaveTestRecord = { title, obtained, max ->
                        viewModel.saveSlotTestRecord(
                            subjectId = item.subject.id,
                            date = selectedDateStr,
                            slotId = item.slot.id,
                            title = title,
                            marksObtained = obtained,
                            maxMarks = max
                        )
                    },
                    onDeleteTestRecord = { id -> viewModel.deleteTestRecord(id) }
                )
            }
        }

        // 5. Custom Activities
        items(customActivities, key = { "activity_${it.id}" }) { activity ->
            CustomActivityCard(
                activity = activity,
                onDelete = { viewModel.deleteCustomActivity(activity.id) }
            )
        }

        // 6. Collapsible Completed / Processed Tray
        if (markedSlots.isNotEmpty()) {
            item(key = "completed_tray_pill") {
                val presentCount = markedSlots.count { it.currentLog?.status == "PRESENT" }
                val shape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fluidTouchFeedback(
                            onClick = {
                                CaliperHapticManager.tick(context)
                                isCompletedTrayExpanded = !isCompletedTrayExpanded
                            }
                        )
                        .clip(shape)
                        .background(Color(0xFF0E121B).copy(alpha = 0.85f), shape)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
                            ),
                            shape = shape
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("completed_tray_toggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.safeZone)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "COMPLETED / PROCESSED ($presentCount/${markedSlots.size} PRESENT)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                        )
                    }
                    Icon(
                        imageVector = if (isCompletedTrayExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Completed Slots",
                        tint = colors.textSecondary
                    )
                }
            }

            item(key = "completed_tray_content") {
                AnimatedVisibility(
                    visible = isCompletedTrayExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        markedSlots.forEach { item ->
                            val slotTest = testRecords.firstOrNull {
                                it.date == selectedDateStr && it.slotId == item.slot.id
                            }
                            MarkedSlotCard(
                                item = item,
                                testRecord = slotTest,
                                selectedDate = selectedDateStr,
                                onUndo = {
                                    CaliperHapticManager.tick(context)
                                    viewModel.undoSlotAttendance(item.slot.id)
                                },
                                onEditSlot = {},
                                onMarkAttendance = { status, isProxy, notes ->
                                    viewModel.setSlotAttendance(
                                        slotId = item.slot.id,
                                        subjectId = item.subject.id,
                                        status = status,
                                        isProxy = isProxy,
                                        notes = notes
                                    )
                                },
                                onSaveTestRecord = { title, obtained, max ->
                                    viewModel.saveSlotTestRecord(
                                        subjectId = item.subject.id,
                                        date = selectedDateStr,
                                        slotId = item.slot.id,
                                        title = title,
                                        marksObtained = obtained,
                                        maxMarks = max
                                    )
                                },
                                onDeleteTestRecord = { id -> viewModel.deleteTestRecord(id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        RetroactiveDatePickerDialog(
            initialDate = selectedDateStr,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                viewModel.setSelectedDate(newDate)
                showDatePicker = false
            }
        )
    }

    if (showExtraEventDialog) {
        AddExtraEventDialog(
            subjects = subjects,
            selectedDate = selectedDateStr,
            onDismiss = { showExtraEventDialog = false },
            onSaveExtraClass = { subjId, status, isProxy, notes ->
                viewModel.logExtraClass(subjId, status, isProxy, notes)
                showExtraEventDialog = false
            },
            onSaveCustomActivity = { title, category, start, end, notes ->
                viewModel.saveCustomActivity(title, category, start, end, notes, true)
                showExtraEventDialog = false
            }
        )
    }

    if (showSemesterDatesDialog) {
        SemesterDatesDialog(
            startDate = semesterStart,
            endDate = semesterEnd,
            onDismiss = { showSemesterDatesDialog = false },
            onSave = { start, end ->
                viewModel.setSemesterDates(start, end)
                showSemesterDatesDialog = false
            }
        )
    }
}

@Composable
fun SlotCard(
    item: SlotDisplayItem,
    testRecord: TestRecordEntity?,
    selectedDate: String,
    onEditSlot: () -> Unit,
    onMarkAttendance: (String, Boolean, String?) -> Unit,
    onSaveTestRecord: (String, Double, Double) -> Unit,
    onDeleteTestRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("slot_card_${item.slot.id}"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.slot.startTime} - ${item.slot.endTime}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isOngoing) colors.safeZone else colors.scheduledLecture
                    )
                )
                Text(
                    text = item.slot.roomNumber ?: item.subject.roomNumber,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = colors.textSecondary
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.subject.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = "${item.subject.code} • ${item.subject.facultyName}",
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SlotActionPill(
                    label = "PRESENT",
                    icon = Icons.Default.Check,
                    accent = colors.safeZone,
                    onClick = { onMarkAttendance("PRESENT", false, null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_present_${item.slot.id}")
                )
                SlotActionPill(
                    label = "ABSENT",
                    icon = Icons.Default.Close,
                    accent = colors.bunkDanger,
                    onClick = { onMarkAttendance("ABSENT", false, null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_absent_${item.slot.id}")
                )
                SlotActionPill(
                    label = "OFF",
                    icon = Icons.Default.Remove,
                    accent = colors.deadlineWarning,
                    onClick = { onMarkAttendance("COLLEGE_OFF", false, null) },
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("mark_off_${item.slot.id}")
                )
            }
        }
    }
}

@Composable
private fun SlotActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .fluidTouchFeedback(enabled = true, onClick = onClick)
            .clip(shape)
            .background(accent.copy(alpha = 0.14f), shape)
            .border(1.dp, accent.copy(alpha = 0.38f), shape)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accent
            )
        )
    }
}

@Composable
fun MarkedSlotCard(
    item: SlotDisplayItem,
    testRecord: TestRecordEntity?,
    selectedDate: String,
    onUndo: () -> Unit,
    onEditSlot: () -> Unit,
    onMarkAttendance: (String, Boolean, String?) -> Unit,
    onSaveTestRecord: (String, Double, Double) -> Unit,
    onDeleteTestRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val status = item.currentLog?.status ?: "PRESENT"
    val statusColor = when (status) {
        "PRESENT" -> colors.safeZone
        "ABSENT" -> colors.bunkDanger
        else -> colors.deadlineWarning
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = "${item.slot.startTime} - ${item.slot.endTime} • $status",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = statusColor
                    )
                )
            }
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo Attendance",
                    tint = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun CustomActivityCard(
    activity: CustomActivityEntity,
    onDelete: () -> Unit
) {
    val colors = CaliperTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = "${activity.category} • ${activity.startTime} - ${activity.endTime}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = colors.textSecondary
                    )
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Activity",
                    tint = colors.bunkDanger
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExtraEventDialog(
    subjects: List<SubjectEntity>,
    selectedDate: String,
    onDismiss: () -> Unit,
    onSaveExtraClass: (Long, String, Boolean, String?) -> Unit,
    onSaveCustomActivity: (String, String, String, String, String?) -> Unit
) {
    val colors = CaliperTheme.colors
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var status by remember { mutableStateOf("PRESENT") }
    var notes by remember { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "LOG EXTRA LECTURE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                subjects.take(5).forEach { subj ->
                    FilterChip(
                        selected = selectedSubjectId == subj.id,
                        onClick = { selectedSubjectId = subj.id },
                        label = { Text("${subj.code} • ${subj.name}") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (selectedSubjectId != 0L) {
                                onSaveExtraClass(selectedSubjectId, status, false, notes.ifBlank { null })
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterDatesDialog(
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val colors = CaliperTheme.colors
    var start by remember { mutableStateOf(startDate) }
    var end by remember { mutableStateOf(endDate) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SEMESTER WINDOW",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onSave(start.trim(), end.trim()) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
