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
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.AddAssessmentDialog
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun AcademicsDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assessments by viewModel.assessments.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val ciaBreakdown by viewModel.ciaBreakdown.collectAsState()

    var activeTab by remember { mutableStateOf("sessionals") } // "sessionals", "practical", "deadlines"
    var showAddDialog by remember { mutableStateOf(false) }

    val sessionals = remember(assessments) {
        assessments.filter { it.type.startsWith("sessional") }
    }
    val classTests = remember(assessments) {
        assessments.filter { it.type == "ct" || it.type == "assignment" }
    }
    val practicals = remember(assessments) {
        assessments.filter { it.type == "drawing_sheet" || it.type == "workshop_job" || it.type.startsWith("viva") }
    }
    val deadlines = remember(assessments) {
        assessments.filter { it.status != "submitted" && it.status != "appeared" }
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
                    "sessionals" to "Sessionals & CTs",
                    "practical" to "Workshop & Sheets",
                    "deadlines" to "Deadlines (${deadlines.size})"
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
                            onClick = { showAddDialog = true },
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
                    Text(
                        text = "WORKSHOP JOBS & MACHINE DRAWING SHEETS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                items(practicals, key = { it.id }) { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    AssessmentCard(
                        assessment = item,
                        subject = subject,
                        onDelete = { viewModel.deleteAssessment(item.id) }
                    )
                }
            }

            "deadlines" -> {
                item {
                    Text(
                        text = "DEADLINE COUNTDOWN VAULT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        letterSpacing = 1.sp
                    )
                }

                if (deadlines.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Celebration, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("All Tasks Submitted!", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("No pending assignment files, drawing sheets, or workshop jobs.", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(deadlines, key = { it.id }) { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    DeadlineCountdownCard(
                        assessment = item,
                        subject = subject,
                        onMarkSubmitted = {
                            viewModel.saveAssessment(item.copy(status = "submitted"))
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssessmentDialog(
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onSave = { assessment ->
                viewModel.saveAssessment(assessment)
            }
        )
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
    subject: com.example.data.local.entity.SubjectEntity?,
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

@Composable
fun DeadlineCountdownCard(
    assessment: AssessmentEntity,
    subject: com.example.data.local.entity.SubjectEntity?,
    onMarkSubmitted: () -> Unit
) {
    val daysRemaining = remember(assessment.dueDate) {
        try {
            val due = LocalDate.parse(assessment.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, due)
        } catch (_: Exception) {
            3L
        }
    }

    val urgencyColor = when {
        daysRemaining <= 2 -> DangerRed
        daysRemaining <= 5 -> WarningAmber
        else -> NeonCyan
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(urgencyColor.copy(alpha = 0.6f), GlassBorderBottom)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${subject?.name ?: "Subject"} (${subject?.code ?: ""})",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = assessment.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Due: ${assessment.dueDate}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(urgencyColor.copy(alpha = 0.2f))
                        .border(1.dp, urgencyColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (daysRemaining >= 0) "$daysRemaining d" else "Overdue",
                            color = urgencyColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("Left", color = TextSecondary, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onMarkSubmitted,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Submit", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
