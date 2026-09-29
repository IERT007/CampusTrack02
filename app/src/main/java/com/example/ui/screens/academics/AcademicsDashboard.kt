package com.example.ui.screens.academics

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
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.AddAssessmentDialog
import com.example.ui.dialogs.AddDeadlineDialog
import com.example.ui.theme.*
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun AcademicsDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assessments by viewModel.assessments.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val ciaBreakdown by viewModel.ciaBreakdown.collectAsState()

    var activeTab by remember { mutableStateOf("deadlines") } // "deadlines", "sessionals", "practical"
    var showAddTestDialog by remember { mutableStateOf(false) }
    var showAddDeadlineDialog by remember { mutableStateOf(false) }

    val sessionals = remember(assessments) {
        assessments.filter { it.type.startsWith("sessional") }
    }
    val classTests = remember(assessments) {
        assessments.filter { it.type == "ct" || it.type == "assignment" }
    }
    val submissions = remember(assessments) {
        assessments.filter {
            it.type in listOf("drawing_sheet", "workshop_job", "lab_file")
        }
    }
    val allDeadlines = remember(assessments) {
        assessments.sortedBy { it.dueDate }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Continuous Internal Assessment (CIA) Calculator Hero Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cia_calculator_card"),
                borderColors = listOf(Color(0x668B5CF6), GlassBorderBottom)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CIA INTERNAL ASSESSMENT ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Internal Score Projection",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "50-Marks Institutional Weightage Formula",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x288B5CF6))
                            .border(1.dp, ElectricViolet, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f", ciaBreakdown.totalInternalScore),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricViolet
                            )
                            Text("/ 50 Marks", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown components: Attendance (10), Sessionals (20), CTs (10), Practical (10)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CiaMetricPill(
                        label = "Attendance",
                        obtained = ciaBreakdown.attendanceScore,
                        max = 10.0,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    CiaMetricPill(
                        label = "Sessionals",
                        obtained = ciaBreakdown.sessionalsScore,
                        max = 20.0,
                        color = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                    CiaMetricPill(
                        label = "CTs & Files",
                        obtained = ciaBreakdown.classTestsScore,
                        max = 10.0,
                        color = IceSky,
                        modifier = Modifier.weight(1f)
                    )
                    CiaMetricPill(
                        label = "Workshop/Lab",
                        obtained = ciaBreakdown.practicalWorkshopScore,
                        max = 10.0,
                        color = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Tab Navigation for Academics Submodules
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1A1F293D))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    "deadlines" to "Deadlines (${allDeadlines.count { it.status == "pending" }})",
                    "sessionals" to "Sessionals & CTs",
                    "practical" to "Sheets & Jobs"
                )

                tabs.forEach { (tabId, label) ->
                    val isSelected = (activeTab == tabId)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                triggerHapticFeedback(context, false)
                                activeTab = tabId
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
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
        }

        // 3. Tab Content
        when (activeTab) {
            "deadlines" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SUBMISSION DEADLINES & COUNTDOWN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Drawing sheets, workshop jobs & lab records",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { showAddDeadlineDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Deadline", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (allDeadlines.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No Pending Submissions", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Tap '+ Deadline' to track Drawing Sheets or Workshop Jobs with live countdowns.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                items(allDeadlines, key = { it.id }) { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    SubmissionDeadlineItemCard(
                        assessment = item,
                        subject = subject,
                        onToggleStatus = {
                            val newStatus = if (item.status == "submitted") "pending" else "submitted"
                            viewModel.updateAssessmentStatus(item.id, newStatus)
                        },
                        onDelete = {
                            viewModel.deleteAssessment(item.id)
                        }
                    )
                }
            }

            "sessionals" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IERT SESSIONAL EXAMINATIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = { showAddTestDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Test", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(sessionals, key = { it.id }) { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    AssessmentCard(
                        assessment = item,
                        subject = subject,
                        onDelete = { viewModel.deleteAssessment(item.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SURPRISE CLASS TESTS (CT)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                items(classTests, key = { it.id }) { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    AssessmentCard(
                        assessment = item,
                        subject = subject,
                        onDelete = { viewModel.deleteAssessment(item.id) }
                    )
                }
            }

            "practical" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DRAWING SHEETS & WORKSHOP JOBS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = { showAddDeadlineDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(submissions, key = { it.id }) { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    SubmissionDeadlineItemCard(
                        assessment = item,
                        subject = subject,
                        onToggleStatus = {
                            val newStatus = if (item.status == "submitted") "pending" else "submitted"
                            viewModel.updateAssessmentStatus(item.id, newStatus)
                        },
                        onDelete = {
                            viewModel.deleteAssessment(item.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddTestDialog) {
        AddAssessmentDialog(
            subjects = subjects,
            onDismiss = { showAddTestDialog = false },
            onSave = { entity ->
                viewModel.saveAssessment(entity)
            }
        )
    }

    if (showAddDeadlineDialog) {
        AddDeadlineDialog(
            subjects = subjects,
            onDismiss = { showAddDeadlineDialog = false },
            onSave = { entity ->
                viewModel.saveAssessment(entity)
            }
        )
    }
}

@Composable
fun SubmissionDeadlineItemCard(
    assessment: AssessmentEntity,
    subject: SubjectEntity?,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val (countdownText, countdownColor) = remember(assessment.dueDate, assessment.dueTime) {
        calculateCountdown(assessment.dueDate, assessment.dueTime)
    }

    val isSubmitted = (assessment.status == "submitted")

    val typeLabel = when (assessment.type) {
        "drawing_sheet" -> "Drawing Sheet"
        "workshop_job" -> "Workshop Job"
        "lab_file" -> "Lab Record"
        "assignment" -> "Assignment"
        else -> assessment.type.replace("_", " ").uppercase()
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(
            if (isSubmitted) NeonEmerald.copy(alpha = 0.5f) else countdownColor.copy(alpha = 0.6f),
            GlassBorderBottom
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2200E5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(typeLabel, color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${subject?.code ?: "ME"} • ${subject?.name ?: "Subject"}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = assessment.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Target: ${assessment.dueDate} at ${assessment.dueTime}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Countdown Badge
                if (!isSubmitted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(countdownColor.copy(alpha = 0.18f))
                            .border(1.dp, countdownColor.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdownText,
                            color = countdownColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonEmerald.copy(alpha = 0.18f))
                            .border(1.dp, NeonEmerald, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("SUBMITTED", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row {
                    TextButton(
                        onClick = {
                            triggerHapticFeedback(context, false)
                            onToggleStatus()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isSubmitted) "Mark Pending" else "Mark Done",
                            color = if (isSubmitted) WarningAmber else NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

fun calculateCountdown(dueDateStr: String, dueTimeStr: String): Pair<String, Color> {
    try {
        val dueTime = if (dueTimeStr.contains(":")) dueTimeStr else "$dueTimeStr:00"
        val dueDateTime = LocalDateTime.parse("${dueDateStr}T${dueTime}")
        val now = LocalDateTime.now()
        val duration = Duration.between(now, dueDateTime)

        if (duration.isNegative) {
            return "Overdue" to DangerRed
        }

        val totalHours = duration.toHours()
        val days = duration.toDays()
        val remainingHours = totalHours % 24

        val text = when {
            days > 0 -> "Due in ${days}d ${remainingHours}h"
            totalHours > 0 -> "Due in ${totalHours}h"
            else -> "Due in < 1h"
        }

        val color = when {
            totalHours < 24 -> DangerRed
            totalHours < 72 -> WarningAmber
            else -> NeonCyan
        }

        return text to color
    } catch (_: Exception) {
        return "Upcoming" to NeonCyan
    }
}

@Composable
fun CiaMetricPill(
    label: String,
    obtained: Double,
    max: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", obtained),
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "/ ${max.toInt()}",
                color = TextSecondary,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AssessmentCard(
    assessment: AssessmentEntity,
    subject: SubjectEntity?,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2200E5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = assessment.type.uppercase().replace("_", " "),
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subject?.code ?: "ME",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = assessment.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (assessment.syllabusCoveragePercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Syllabus Coverage: ${assessment.syllabusCoveragePercent}%",
                        color = IceSky,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (assessment.obtainedMarks != null) {
                    Text(
                        text = "${assessment.obtainedMarks} / ${assessment.maxMarks.toInt()}",
                        color = NeonEmerald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Scored", color = TextSecondary, fontSize = 10.sp)
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33F59E0B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (assessment.status == "in_progress") "In Progress" else "Pending",
                            color = WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
