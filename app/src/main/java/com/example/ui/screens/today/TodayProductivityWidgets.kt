package com.example.ui.screens.today

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.local.entity.QuickNoteEntity
import com.example.ui.GlobalAttendanceSummary
import com.example.ui.components.AttendanceTrajectoryWaveChart
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

/**
 * Top Hero Split Bento Row matching the reference design:
 * Left: Warm Architectural Botanical Art + Semester Progress Ring Card
 * Right: "Focus • Deep Work" Timer Card with "2h 45m" & Warm Taupe "Start" Pill Button
 */
@Composable
fun FocusAndProgressBentoRow(
    summary: GlobalAttendanceSummary,
    nextLectureLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var presetIndex by rememberSaveable { mutableIntStateOf(0) }
    val presets = remember {
        listOf(
            "Deep Work" to 165 * 60, // 2h 45m (exact match to reference image)
            "Pomodoro" to 25 * 60,   // 25m
            "Lab Sprint" to 50 * 60  // 50m
        )
    }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(presets[0].second) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        if (remainingSeconds == 0) {
            isTimerRunning = false
        }
    }

    val formattedTime = remember(remainingSeconds, isTimerRunning) {
        val hours = remainingSeconds / 3600
        val mins = (remainingSeconds % 3600) / 60
        val secs = remainingSeconds % 60
        if (!isTimerRunning && secs == 0 && hours > 0) {
            "${hours}h ${mins}m"
        } else if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LEFT CARD: Architectural Botanical & Live Attendance Progress Ring
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("today_hero_card"),
            shape = RoundedCornerShape(26.dp),
            containerColor = colors.cardFillSecondary
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Warm Sunlit Architectural Arch + Botanical Leaf Canvas Illustration
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Soft sunlit arch backdrop
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.scheduledLecture.copy(alpha = 0.18f),
                                colors.safeZone.copy(alpha = 0.08f)
                            )
                        ),
                        topLeft = Offset(w * 0.12f, h * 0.10f),
                        size = Size(w * 0.76f, h * 0.85f),
                        cornerRadius = CornerRadius(w * 0.38f, w * 0.38f)
                    )
                    // Minimalist ceramic vase at bottom-left
                    drawRoundRect(
                        color = colors.scheduledLecture.copy(alpha = 0.32f),
                        topLeft = Offset(w * 0.16f, h * 0.64f),
                        size = Size(w * 0.22f, h * 0.24f),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                    // Botanical stem & leaves
                    val stemPath = Path().apply {
                        moveTo(w * 0.27f, h * 0.64f)
                        quadraticTo(w * 0.32f, h * 0.40f, w * 0.42f, h * 0.22f)
                    }
                    drawPath(
                        path = stemPath,
                        color = colors.safeZone.copy(alpha = 0.55f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawOval(
                        color = colors.safeZone.copy(alpha = 0.40f),
                        topLeft = Offset(w * 0.33f, h * 0.28f),
                        size = Size(26f, 14f)
                    )
                    drawOval(
                        color = colors.safeZone.copy(alpha = 0.45f),
                        topLeft = Offset(w * 0.20f, h * 0.38f),
                        size = Size(24f, 13f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Semester Goal",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaFamily,
                                    color = colors.textSecondary
                                )
                            )
                            Text(
                                text = String.format(Locale.US, "%.0f%%", summary.overallPercentage),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = SpaceGroteskFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = colors.textPrimary
                                )
                            )
                        }

                        // Mini Circular Ring matching the "76%" ring in reference image
                        val sweep = ((summary.overallPercentage.toFloat().coerceIn(0f, 100f)) / 100f) * 360f
                        Canvas(modifier = Modifier.size(44.dp)) {
                            val stroke = 4.5.dp.toPx()
                            drawArc(
                                color = colors.divider,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (summary.overallPercentage >= 75.0) colors.safeZone else colors.bunkDanger,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardFill)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${summary.totalAttended}/${summary.totalConducted} Lectures",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = nextLectureLabel,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }
        }

        // RIGHT CARD: "Focus • Deep Work • 2h 45m • [Start]" (Exact match to reference image)
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("deep_work_focus_card"),
            shape = RoundedCornerShape(26.dp),
            onClick = {
                CaliperHapticManager.tick(context)
                if (!isTimerRunning) {
                    presetIndex = (presetIndex + 1) % presets.size
                    remainingSeconds = presets[presetIndex].second
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Focus",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaFamily,
                            color = colors.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = presets[presetIndex].first,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = colors.textPrimary
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .fluidTouchFeedback(
                            enabled = true,
                            onClick = {
                                CaliperHapticManager.confirmTap(context)
                                isTimerRunning = !isTimerRunning
                            }
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isTimerRunning) colors.safeZone else colors.scheduledLecture,
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("focus_timer_start_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTimerRunning) "Pause" else "Start",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * Second Bento Row matching the floating "Tasks (8 Completed)" and "Water Reminder (6/8 Cups)" cards
 */
@Composable
fun TasksAndHydrationBentoRow(
    tasks: List<AcademicTaskEntity>,
    onOpenTasksModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val prefs = remember { context.getSharedPreferences("caliper_wellness_prefs", Context.MODE_PRIVATE) }
    val todayKey = remember { "water_cups_${LocalDate.now()}" }
    var waterCups by remember { mutableIntStateOf(prefs.getInt(todayKey, 6)) }

    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.size.coerceAtLeast(1)
    val taskProgress by animateFloatAsState(
        targetValue = if (tasks.isEmpty()) 0.65f else (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0.08f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "task_progress"
    )
    val waterProgress by animateFloatAsState(
        targetValue = (waterCups.toFloat() / 8f).coerceIn(0.08f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "water_progress"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tasks Card ("Tasks • X Completed")
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .testTag("bento_tasks_card"),
            shape = RoundedCornerShape(24.dp),
            onClick = {
                CaliperHapticManager.tick(context)
                onOpenTasksModal()
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaFamily,
                            color = colors.textSecondary
                        )
                    )
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Manage Tasks",
                        tint = colors.safeZone,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (tasks.isEmpty()) "0" else "$completedTasks/${tasks.size}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = if (tasks.isEmpty()) "Tap to add tasks" else "Completed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(taskProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(colors.safeZone)
                    )
                }
            }
        }

        // Water / Study Hydration Card ("Water Reminder • 6/8 Cups")
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .testTag("bento_hydration_card"),
            shape = RoundedCornerShape(24.dp),
            onClick = {
                CaliperHapticManager.tick(context)
                waterCups = if (waterCups >= 8) 1 else waterCups + 1
                prefs.edit().putInt(todayKey, waterCups).apply()
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Water Reminder",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaFamily,
                            color = colors.textSecondary
                        )
                    )
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = "Log Water Cup",
                        tint = colors.safeZone,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$waterCups/8",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cups",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        ),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Text(
                    text = "Tap to log +1 cup",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(waterProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(colors.safeZone)
                    )
                }
            }
        }
    }
}

/**
 * "Statistics" Wave Card matching the bottom of the phone screen in the reference image
 */
@Composable
fun StatisticsWaveBentoCard(
    logs: List<AttendanceLogEntity>,
    overallPercentage: Double,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val deltaFromTarget = overallPercentage - 75.0
    val deltaSign = if (deltaFromTarget >= 0) "+" else ""

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.35f)) {
                    AttendanceTrajectoryWaveChart(
                        logs = logs,
                        currentPercentage = overallPercentage,
                        height = 96.dp
                    )
                }

                // Frosted sub-card on the right ("+X% vs 75% goal")
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardFillSecondary, RoundedCornerShape(20.dp))
                        .border(1.dp, colors.cardBorderTop, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%s%.1f%%", deltaSign, deltaFromTarget),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (deltaFromTarget >= 0) colors.safeZone else colors.bunkDanger
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "vs 75% target",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.safeZone)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.scheduledLecture.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.divider)
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Quick Actions" Bar matching the bottom floating card in the reference image
 */
@Composable
fun QuickActionsGlassBar(
    onOpenNotes: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenExtraSlot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionTile(
                    icon = Icons.Outlined.EditNote,
                    label = "Notes",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        onOpenNotes()
                    }
                )
                QuickActionTile(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Tasks",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        onOpenTasks()
                    }
                )
                QuickActionTile(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Schedule",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        onOpenTimetable()
                    }
                )
                QuickActionTile(
                    icon = Icons.Outlined.MoreHoriz,
                    label = "Extra",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        onOpenExtraSlot()
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = CaliperTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fluidTouchFeedback(enabled = true, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.cardFillSecondary, RoundedCornerShape(18.dp))
                .border(1.dp, colors.cardBorderTop, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = PlusJakartaFamily,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        )
    }
}

/**
 * Botanical Quote Card ("Small steps every day lead to big changes.")
 * matching the left floating quote card in the reference image
 */
@Composable
fun BotanicalInspirationQuoteCard(modifier: Modifier = Modifier) {
    val colors = CaliperTheme.colors
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.cardFillSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "“",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = colors.scheduledLecture
                    )
                )
                Text(
                    text = "Small steps every day lead to big changes in your semester.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PlusJakartaFamily,
                        fontStyle = FontStyle.Italic,
                        color = colors.textPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Canvas(modifier = Modifier.size(48.dp)) {
                val w = size.width
                val h = size.height
                val stem = Path().apply {
                    moveTo(w * 0.2f, h * 0.9f)
                    quadraticTo(w * 0.5f, h * 0.5f, w * 0.85f, h * 0.15f)
                }
                drawPath(
                    path = stem,
                    color = colors.safeZone.copy(alpha = 0.65f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawOval(
                    color = colors.safeZone.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.48f, h * 0.22f),
                    size = Size(22f, 12f)
                )
                drawOval(
                    color = colors.safeZone.copy(alpha = 0.35f),
                    topLeft = Offset(w * 0.24f, h * 0.48f),
                    size = Size(20f, 11f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicTasksModal(
    tasks: List<AcademicTaskEntity>,
    onAddTask: (String, String) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CaliperTheme.colors
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDue by remember { mutableStateOf("Today") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Academic Tasks & Goals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Add assignment or study goal...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                onAddTask(newTaskTitle.trim(), newTaskDue)
                                newTaskTitle = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = colors.safeZone
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (tasks.isEmpty()) {
                    Text(
                        text = "No study tasks yet. Add your first goal above!",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                } else {
                    tasks.take(6).forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fluidTouchFeedback(
                                        onClick = { onToggleTask(task.id, !task.isCompleted) }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (task.isCompleted) colors.safeZone else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            1.5.dp,
                                            if (task.isCompleted) colors.safeZone else colors.textMuted,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (task.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = PlusJakartaFamily,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (task.isCompleted) colors.textMuted else colors.textPrimary
                                    )
                                )
                            }
                            IconButton(onClick = { onDeleteTask(task.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Task",
                                    tint = colors.bunkDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = colors.scheduledLecture)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStudyNotesModal(
    notes: List<QuickNoteEntity>,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CaliperTheme.colors
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Study Notes & Formulas",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    placeholder = { Text("Note title (e.g., Room / Lab Timing)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    placeholder = { Text("Key details or quick reminder...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            if (noteTitle.isNotBlank()) {
                                onAddNote(noteTitle.trim(), noteBody.trim().ifBlank { "Saved note" })
                                noteTitle = ""
                                noteBody = ""
                            }
                        }
                    ) {
                        Text("+ Save Note", color = colors.safeZone, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                notes.take(4).forEach { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.cardFillSecondary)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = colors.bunkDanger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = colors.scheduledLecture)
                    }
                }
            }
        }
    }
}
