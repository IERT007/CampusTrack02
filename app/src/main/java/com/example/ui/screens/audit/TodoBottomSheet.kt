package com.example.ui.screens.audit

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TodoTaskEntity
import com.example.ui.theme.CaliperTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class TodoUrgencyBucket(val sortOrder: Int) {
    OVERDUE(0),
    DUE_TODAY(1),
    UPCOMING(2),
    COMPLETED(3)
}

data class PrioritizedTodoItem(
    val task: TodoTaskEntity,
    val bucket: TodoUrgencyBucket,
    val badgeText: String,
    val formattedDue: String
)

/**
 * Auto-Urgency Sorting Engine:
 * 1. Overdue & Incomplete (oldest overdue first) -> "Overdue"
 * 2. Due Today & Incomplete (soonest time first) -> "Due in Xh" / "Due in Xm"
 * 3. Upcoming & Incomplete (chronological by dueDateTimeMillis) -> formatted date/time
 * 4. Completed (pushed to bottom with strikethrough)
 */
fun prioritizeTodoTasks(
    tasks: List<TodoTaskEntity>,
    nowMillis: Long = System.currentTimeMillis()
): List<PrioritizedTodoItem> {
    val zone = ZoneId.systemDefault()
    val nowDateTime = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
    val today = nowDateTime.toLocalDate()
    val tomorrow = today.plusDays(1)
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM • hh:mm a")

    return tasks.map { task ->
        val dueDateTime = Instant.ofEpochMilli(task.dueDateTimeMillis).atZone(zone).toLocalDateTime()
        val dueDate = dueDateTime.toLocalDate()
        val formattedDue = when {
            dueDate.isEqual(today) -> "Today • ${dueDateTime.format(timeFormatter)}"
            dueDate.isEqual(tomorrow) -> "Tomorrow • ${dueDateTime.format(timeFormatter)}"
            else -> dueDateTime.format(dateTimeFormatter)
        }

        val (bucket, badge) = when {
            task.isCompleted -> TodoUrgencyBucket.COMPLETED to "Done"
            task.dueDateTimeMillis < nowMillis -> TodoUrgencyBucket.OVERDUE to "Overdue"
            dueDate.isEqual(today) -> {
                val minsLeft = ChronoUnit.MINUTES.between(nowDateTime, dueDateTime).coerceAtLeast(1L)
                val hoursLeft = minsLeft / 60L
                val label = if (hoursLeft >= 1L) "Due in ${hoursLeft}h" else "Due in ${minsLeft}m"
                TodoUrgencyBucket.DUE_TODAY to label
            }
            else -> {
                val daysLeft = ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(1L)
                val label = if (daysLeft == 1L) "Tomorrow" else "In ${daysLeft}d"
                TodoUrgencyBucket.UPCOMING to label
            }
        }

        PrioritizedTodoItem(
            task = task,
            bucket = bucket,
            badgeText = badge,
            formattedDue = formattedDue
        )
    }.sortedWith(
        compareBy<PrioritizedTodoItem> { it.bucket.sortOrder }
            .thenBy { if (it.bucket == TodoUrgencyBucket.COMPLETED) -it.task.dueDateTimeMillis else it.task.dueDateTimeMillis }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoBottomSheet(
    tasks: List<TodoTaskEntity>,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onAddTask: (title: String, subjectId: Long?, dueDateTimeMillis: Long) -> Unit,
    onToggleTask: (id: Long, completed: Boolean) -> Unit,
    onDeleteTask: (id: Long) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    val zone = remember { ZoneId.systemDefault() }

    val prioritizedItems = remember(tasks) {
        prioritizeTodoTasks(tasks, System.currentTimeMillis())
    }
    val completedCount = remember(tasks) { tasks.count { it.isCompleted } }
    val overdueCount = remember(prioritizedItems) { prioritizedItems.count { it.bucket == TodoUrgencyBucket.OVERDUE } }

    var titleInput by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedDueDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedDueTime by remember { mutableStateOf(LocalTime.of(18, 0)) }
    var showComposer by remember { mutableStateOf(tasks.isEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        scrimColor = Color.Black.copy(alpha = 0.74f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.offSlate) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("smart_todo_bottom_sheet")
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.safeGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = colors.safeGreen,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Auto-Prioritized To-Do",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildString {
                                append("$completedCount of ${tasks.size} completed")
                                if (overdueCount > 0) append(" • $overdueCount overdue")
                            },
                            color = if (overdueCount > 0) colors.dangerRed else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (overdueCount > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            showComposer = !showComposer
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.primaryAccent.copy(alpha = 0.18f),
                            contentColor = colors.primaryAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("todo_toggle_composer_btn")
                    ) {
                        Icon(
                            imageVector = if (showComposer) Icons.Default.ExpandLess else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showComposer) "Hide" else "New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Task Composer
            AnimatedVisibility(visible = showComposer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(0.75.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Task Title", fontSize = 11.sp) },
                        placeholder = { Text("Submit CAD sheet, Lab viva prep, Assignment...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("todo_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAccent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.primaryAccent,
                            unfocusedLabelColor = colors.textSecondary
                        )
                    )

                    // Subject Chip Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SUBJECT TAG (OPTIONAL)",
                            color = colors.textSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val generalSelected = (selectedSubjectId == null)
                            SubjectFilterChip(
                                label = "General",
                                selected = generalSelected,
                                onClick = {
                                    CaliperSoundManager.playSnap(view)
                                    selectedSubjectId = null
                                }
                            )
                            subjects.forEach { subject ->
                                SubjectFilterChip(
                                    label = subject.code.ifBlank { subject.name },
                                    selected = (selectedSubjectId == subject.id),
                                    onClick = {
                                        CaliperSoundManager.playSnap(view)
                                        selectedSubjectId = subject.id
                                    }
                                )
                            }
                        }
                    }

                    // DatePicker & TimePicker Trigger Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                showDatePicker = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("todo_pick_date_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.75.dp, colors.border),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedDueDate.format(dateFormatter),
                                color = colors.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                showTimePicker = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("todo_pick_time_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.75.dp, colors.border),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedDueTime.format(timeFormatter),
                                color = colors.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val trimmed = titleInput.trim()
                            if (trimmed.isNotEmpty()) {
                                val dueMillis = LocalDateTime.of(selectedDueDate, selectedDueTime)
                                    .atZone(zone)
                                    .toInstant()
                                    .toEpochMilli()
                                onAddTask(trimmed, selectedSubjectId, dueMillis)
                                CaliperSoundManager.playSuccess()
                                CaliperHapticManager.tick(context)
                                titleInput = ""
                                selectedSubjectId = null
                                showComposer = false
                            }
                        },
                        enabled = titleInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("todo_add_submit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.safeGreen,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Priority Queue", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Task List
            if (prioritizedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = colors.safeGreen.copy(alpha = 0.7f),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All clear! No pending tasks.",
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add deadlines, drawing sheets, or viva prep — Caliper ranks them automatically by urgency.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prioritizedItems, key = { it.task.id }) { item ->
                        val task = item.task
                        val subject = subjects.find { it.id == task.subjectId }
                        val badgeColor = when (item.bucket) {
                            TodoUrgencyBucket.OVERDUE -> colors.dangerRed
                            TodoUrgencyBucket.DUE_TODAY -> colors.warnAmber
                            TodoUrgencyBucket.UPCOMING -> colors.primaryAccent
                            TodoUrgencyBucket.COMPLETED -> colors.offSlate
                        }
                        val rowBorder = when (item.bucket) {
                            TodoUrgencyBucket.OVERDUE -> colors.dangerRed.copy(alpha = 0.50f)
                            TodoUrgencyBucket.DUE_TODAY -> colors.warnAmber.copy(alpha = 0.45f)
                            TodoUrgencyBucket.UPCOMING -> colors.border
                            TodoUrgencyBucket.COMPLETED -> colors.border.copy(alpha = 0.5f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface.copy(alpha = if (task.isCompleted) 0.65f else 1f))
                                .border(0.75.dp, rowBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    onToggleTask(task.id, !task.isCompleted)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("todo_item_${task.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        CaliperSoundManager.playSnap(view)
                                        CaliperHapticManager.tick(context)
                                        onToggleTask(task.id, checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.safeGreen,
                                        uncheckedColor = badgeColor,
                                        checkmarkColor = colors.onPrimaryAccent
                                    ),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        color = if (task.isCompleted) colors.textSecondary else colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(badgeColor.copy(alpha = 0.16f))
                                                .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = item.badgeText,
                                                color = badgeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        if (subject != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(colors.primaryAccent.copy(alpha = 0.12f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = subject.code.ifBlank { subject.name },
                                                    color = colors.primaryAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.formattedDue,
                                            color = colors.textSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    CaliperSoundManager.playThud()
                                    CaliperHapticManager.tick(context)
                                    onDeleteTask(task.id)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("todo_delete_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Task",
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

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val initialMillis = remember(selectedDueDate) {
            selectedDueDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        }
                        CaliperSoundManager.playSnap(view)
                        showDatePicker = false
                    }
                ) {
                    Text("Set Date", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.surfaceVariant)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 TimePicker inside AlertDialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDueTime.hour,
            initialMinute = selectedDueTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = colors.surfaceVariant,
            title = {
                Text("Select Due Time", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDueTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        CaliperSoundManager.playSnap(view)
                        showTimePicker = false
                    }
                ) {
                    Text("Set Time", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SubjectFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = CaliperTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primaryAccent.copy(alpha = 0.22f) else colors.surfaceVariant)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = if (selected) colors.primaryAccent else colors.border,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) colors.primaryAccent else colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
