package com.example.ui.screens.audit

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AcademicMilestoneBadges
import com.example.ui.components.AttendanceTrajectoryWaveChart
import com.example.ui.components.GlassCard
import com.example.ui.dialogs.AddMedicalLeaveDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback
import java.util.Locale

@Composable
fun AuditScreen(
    viewModel: MainViewModel,
    onJumpToDate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val summary by viewModel.globalSummary.collectAsState()
    val cia by viewModel.ciaBreakdown.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val streak by viewModel.collegeStreak.collectAsState()
    val medicalLeaves by viewModel.medicalLeaves.collectAsState()

    var showAddMedicalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("audit_screen_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "audit_header") {
            Column {
                Text(
                    text = "Analytics & CIA Report",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = "Continuous Internal Assessment & Calendar History",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = PlusJakartaFamily,
                        color = colors.textSecondary
                    )
                )
            }
        }

        // Trajectory Wave Chart
        item(key = "trajectory_wave_card") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance Trajectory",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", summary.overallPercentage),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.safeZone
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AttendanceTrajectoryWaveChart(
                        logs = logs,
                        currentPercentage = summary.overallPercentage
                    )
                }
            }
        }

        // Metric Boxes
        item(key = "audit_metrics_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuditMetricBox(
                    label = "CIA Score",
                    value = String.format(Locale.US, "%.1f/50", cia.totalInternalScore),
                    accentColor = colors.scheduledLecture,
                    modifier = Modifier.weight(1f)
                )
                AuditMetricBox(
                    label = "Faculty Off",
                    value = summary.totalFacultyCancelled.toString(),
                    accentColor = colors.deadlineWarning,
                    modifier = Modifier.weight(1f)
                )
                AuditMetricBox(
                    label = "Proxies",
                    value = summary.totalProxy.toString(),
                    accentColor = colors.safeZone,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Milestone Badges
        item(key = "milestone_badges") {
            AcademicMilestoneBadges(
                streakDays = streak,
                overallPercentage = summary.overallPercentage,
                allSubjectsSafe = summary.criticalSubjectCount == 0,
                zeroBunksThisWeek = true
            )
        }

        // Interactive Audit Calendar
        item(key = "interactive_calendar") {
            InteractiveAuditCalendar(
                viewModel = viewModel,
                onDateSelected = { dateStr ->
                    viewModel.setSelectedDate(dateStr)
                    onJumpToDate(dateStr)
                }
            )
        }

        // Medical Leaves Section
        item(key = "medical_leave_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medical Leave Logs",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.scheduledLecture)
                        .fluidTouchFeedback(onClick = { showAddMedicalDialog = true })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Medical Leave",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Log",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        items(medicalLeaves, key = { it.id }) { leave ->
            MedicalLeaveCard(leave = leave)
        }
    }

    if (showAddMedicalDialog) {
        AddMedicalLeaveDialog(
            onDismiss = { showAddMedicalDialog = false },
            onSave = { leave ->
                viewModel.saveMedicalLeave(leave)
                showAddMedicalDialog = false
            }
        )
    }
}

@Composable
fun AuditMetricBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.cardFill, shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.cardBorderTop, colors.cardBorderBottom)
                ),
                shape = shape
            )
            .let { if (onClick != null) it.fluidTouchFeedback(onClick = onClick) else it }
            .padding(14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = PlusJakartaFamily,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        )
    }
}

@Composable
fun CalendarLegendDot(
    label: String,
    color: Color
) {
    val colors = CaliperTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaFamily,
                color = colors.textSecondary
            )
        )
    }
}

@Composable
fun MedicalLeaveCard(leave: MedicalLeaveEntity) {
    val colors = CaliperTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = leave.reason,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${leave.startDate} → ${leave.endDate} • ${leave.classesMissedCount} lectures",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    color = colors.textSecondary
                )
            )
        }
    }
}
