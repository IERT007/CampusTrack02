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
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Science
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
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.CustomActivityEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestRecordEntity
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.dialogs.RetroactiveDatePickerDialog
import com.example.ui.dialogs.WeeklyTimetableEditorDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaFamily
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
    val allSlots by viewModel.slots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val customActivities by viewModel.currentDayActivities.collectAsState()
    val testRecords by viewModel.testRecords.collectAsState()
    val currentHoliday by viewModel.currentHoliday.collectAsState()
    val semesterStart by viewModel.semesterStartDate.collectAsState()
    val semesterEnd by viewModel.semesterEndDate.collectAsState()

    var isCompletedTrayExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExtraEventDialog by remember { mutableStateOf(false) }
    var showSemesterDatesDialog by remember { mutableStateOf(false) }
    var showTasksModal by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }
    var showTimetableEditor by remember { mutableStateOf(false) }

    val unmarkedSlots = remember(slots) { slots.filter { it.currentLog == null } }
    val markedSlots = remember(slots) { slots.filter { it.currentLog != null } }

    val parsedDate = remember(selectedDateStr) {
        runCatching { LocalDate.parse(selectedDateStr) }.getOrDefault(LocalDate.now())
    }
    val formattedDateLabel = remember(parsedDate) {
        parsedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US))
    }

    val nextLectureLabel = remember(unmarkedSlots, summary) {
        val next = unmarkedSlots.firstOrNull()
        if (next != null) {
            "Next: ${next.subject.code} at ${next.slot.startTime}"
        } else if (summary.criticalSubjectCount == 0) {
            "All subjects above 75% target"
        } else {
            "${summary.criticalSubjectCount} subject(s) need attention"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Hero Split Bento Row (Architectural Botanical Ring + Deep Work Focus Timer)
        item(key = "telemetry_hero_card") {
            FocusAndProgressBentoRow(
                summary = summary,
                nextLectureLabel = nextLectureLabel
            )
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.containerSurface, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.cardBorderTop, RoundedCornerShape(18.dp))
                        .fluidTouchFeedback(
                            onClick = {
                                CaliperHapticManager.tick(context)
                                showDatePicker = true
                            }
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp),
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
                            fontFamily = PlusJakartaFamily,
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

        // 3. Batch Action Bar (Mark All Present / Extra Slot)
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.safeZone.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .border(1.dp, colors.safeZone.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
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
                        text = "All Present",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaFamily,
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.scheduledLecture.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                        .border(1.dp, colors.scheduledLecture.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
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
                        text = "Extra Slot",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaFamily,
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
                    shape = RoundedCornerShape(22.dp),
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
                                text = "Holiday • ${holiday.title}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
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

        // 4. "Today's Plan" Header
        item(key = "todays_plan_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Plan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = "${unmarkedSlots.size} pending • ${markedSlots.size} logged",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = PlusJakartaFamily,
                        color = colors.textSecondary
                    )
                )
            }
        }

        // 5. Unmarked Active Schedule Slots
        if (unmarkedSlots.isEmpty() && markedSlots.isEmpty()) {
            item(key = "empty_schedule_card") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Lectures Scheduled",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enjoy your self-study & deep work window today.",
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

        // 6. Custom Activities
        items(customActivities, key = { "activity_${it.id}" }) { activity ->
            CustomActivityCard(
                activity = activity,
                onDelete = { viewModel.deleteCustomActivity(activity.id) }
            )
        }

        // 7. Collapsible Completed / Processed Tray
        if (markedSlots.isNotEmpty()) {
            item(key = "completed_tray_pill") {
                val presentCount = markedSlots.count { it.currentLog?.status == "PRESENT" }
                val shape = RoundedCornerShape(20.dp)
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
                        .background(colors.cardFill, shape)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(colors.cardBorderTop, colors.cardBorderBottom)
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
                            text = "Completed Lectures ($presentCount/${markedSlots.size} Present)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaFamily,
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

        // 8. Tasks & Hydration Bento Row (From reference floating cards)
        item(key = "tasks_hydration_bento_row") {
            TasksAndHydrationBentoRow(
                tasks = tasks,
                onOpenTasksModal = { showTasksModal = true }
            )
        }

        // 9. Statistics Wave Trajectory Card (Matching the bottom of the phone in the reference image)
        item(key = "statistics_wave_card") {
            StatisticsWaveBentoCard(
                logs = logs,
                overallPercentage = summary.overallPercentage
            )
        }

        // 10. Quick Actions Bar (Notes, Tasks, Schedule, Extra)
        item(key = "quick_actions_bar") {
            QuickActionsGlassBar(
                onOpenNotes = { showNotesModal = true },
                onOpenTasks = { showTasksModal = true },
                onOpenTimetable = { showTimetableEditor = true },
                onOpenExtraSlot = { showExtraEventDialog = true }
            )
        }

        // 11. Botanical Daily Inspiration Quote Card
        item(key = "botanical_quote_card") {
            BotanicalInspirationQuoteCard()
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

    if (showTasksModal) {
        AcademicTasksModal(
            tasks = tasks,
            onAddTask = { title, due ->
                viewModel.addTask(
                    AcademicTaskEntity(
                        description = title,
                        category = subjects.firstOrNull()?.code ?: "GENERAL",
                        dueDate = selectedDateStr,
                        priority = due,
                        isCompleted = false
                    )
                )
            },
            onToggleTask = { id, completed -> viewModel.toggleTaskCompleted(id, completed) },
            onDeleteTask = { id -> viewModel.deleteTask(id) },
            onDismiss = { showTasksModal = false }
        )
    }

    if (showNotesModal) {
        QuickStudyNotesModal(
            notes = notes,
            onAddNote = { title, content ->
                viewModel.addNote(
                    title = title,
                    content = content,
                    subjectCode = subjects.firstOrNull()?.code ?: "GENERAL",
                    colorHex = "#8B7D6B"
                )
            },
            onDeleteNote = { id -> viewModel.deleteNote(id) },
            onDismiss = { showNotesModal = false }
        )
    }

    if (showTimetableEditor) {
        WeeklyTimetableEditorDialog(
            slots = allSlots,
            subjects = subjects,
            onDismiss = { showTimetableEditor = false },
            onAddSlot = { viewModel.saveSlot(it) },
            onUpdateSlot = { viewModel.updateTimetableSlot(it) },
            onDeleteSlot = { viewModel.deleteSlot(it) }
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
    val isLab = item.subject.type.equals("lab", ignoreCase = true) ||
        item.subject.type.equals("workshop", ignoreCase = true)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("slot_card_${item.slot.id}"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rounded glass icon box matching "Today's Plan" items in the reference image
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardFillSecondary, RoundedCornerShape(14.dp))
                        .border(1.dp, colors.cardBorderTop, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLab) Icons.Outlined.Science else Icons.Outlined.Assignment,
                        contentDescription = item.subject.type,
                        tint = if (item.isOngoing) colors.safeZone else colors.scheduledLecture,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subject.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.slot.startTime} - ${item.slot.endTime} • ${item.subject.code} (${item.slot.roomNumber ?: item.subject.roomNumber})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaFamily,
                            color = colors.textSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SlotActionPill(
                    label = "Present",
                    icon = Icons.Default.Check,
                    accent = colors.safeZone,
                    onClick = { onMarkAttendance("PRESENT", false, null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_present_${item.slot.id}")
                )
                SlotActionPill(
                    label = "Absent",
                    icon = Icons.Default.Close,
                    accent = colors.bunkDanger,
                    onClick = { onMarkAttendance("ABSENT", false, null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_absent_${item.slot.id}")
                )
                SlotActionPill(
                    label = "Off",
                    icon = Icons.Default.Remove,
                    accent = colors.scheduledLecture,
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
            .height(44.dp)
            .fluidTouchFeedback(enabled = true, onClick = onClick)
            .clip(shape)
            .background(accent.copy(alpha = 0.14f), shape)
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(20.dp)
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
                        fontFamily = PlusJakartaFamily,
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
                    imageVector = Icons.AutoMirrored.Filled.Undo,
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
        shape = RoundedCornerShape(20.dp)
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
                        fontFamily = PlusJakartaFamily,
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
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Log Extra Lecture",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaFamily,
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
                    shape = RoundedCornerShape(14.dp),
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
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Semester Window",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaFamily,
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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
