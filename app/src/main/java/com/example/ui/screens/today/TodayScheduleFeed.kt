package com.example.ui.screens.today

import android.content.Context
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    COLLEGE_OFF
}

data class TimetableSlot(
    val slotId: Long,
    val subjectCode: String,
    val subjectName: String,
    val facultyName: String,
    val roomNo: String,
    val startTime: String,
    val endTime: String = ""
)

data class ProcessedSlotRecord(
    val slotId: Long,
    val subjectName: String,
    val roomNo: String,
    val time: String,
    val status: String
)

@Composable
fun TodayScheduleFeed(
    activeSlots: List<TimetableSlot>,
    completedSlots: List<ProcessedSlotRecord>,
    onMarkAttendance: (Long, AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTrayExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(activeSlots, key = { it.slotId }) { slot ->
            ActiveSlotCard(
                slot = slot,
                onMark = { status -> onMarkAttendance(slot.slotId, status) }
            )
        }

        if (completedSlots.isNotEmpty()) {
            item(key = "completed_tray_header") {
                val presentCount = completedSlots.count { it.status == "PRESENT" }
                CompletedTrayHeader(
                    presentCount = presentCount,
                    totalProcessed = completedSlots.size,
                    isExpanded = isTrayExpanded,
                    onToggle = { isTrayExpanded = !isTrayExpanded }
                )
            }

            item(key = "completed_tray_items") {
                AnimatedVisibility(
                    visible = isTrayExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        completedSlots.forEach { record ->
                            HistoricalSlotRow(record = record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSlotCard(
    slot: TimetableSlot,
    onMark: (AttendanceStatus) -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${slot.startTime}${if (slot.endTime.isNotBlank()) " - ${slot.endTime}" else ""}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = colors.scheduledLecture
                    )
                )
                Text(
                    text = slot.roomNo,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = colors.textSecondary
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = slot.subjectName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = "${slot.subjectCode} • ${slot.facultyName}",
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TactileActionPill(
                    label = "ATTENDED",
                    icon = Icons.Default.Check,
                    accentColor = colors.safeZone,
                    context = context,
                    onClick = { onMark(AttendanceStatus.PRESENT) },
                    modifier = Modifier.weight(1f)
                )
                TactileActionPill(
                    label = "BUNKED",
                    icon = Icons.Default.Close,
                    accentColor = colors.bunkDanger,
                    context = context,
                    onClick = { onMark(AttendanceStatus.ABSENT) },
                    modifier = Modifier.weight(1f)
                )
                TactileActionPill(
                    label = "OFF",
                    icon = Icons.Default.Remove,
                    accentColor = colors.deadlineWarning,
                    context = context,
                    onClick = { onMark(AttendanceStatus.COLLEGE_OFF) },
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    }
}

@Composable
private fun TactileActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    context: Context,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .fluidTouchFeedback(
                enabled = true,
                onClick = {
                    CaliperHapticManager.confirmTap(context)
                    onClick()
                }
            )
            .clip(shape)
            .background(accentColor.copy(alpha = 0.14f), shape)
            .border(1.dp, accentColor.copy(alpha = 0.38f), shape)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accentColor
            )
        )
    }
}

@Composable
fun CompletedTrayHeader(
    presentCount: Int,
    totalProcessed: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fluidTouchFeedback(enabled = true, onClick = onToggle)
            .clip(shape)
            .background(Color(0xFF0E121B).copy(alpha = 0.85f), shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
                ),
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                text = "COMPLETED / PROCESSED ($presentCount/$totalProcessed PRESENT)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = "Toggle Completed Tray",
            tint = colors.textSecondary
        )
    }
}

@Composable
fun HistoricalSlotRow(record: ProcessedSlotRecord) {
    val colors = CaliperTheme.colors
    val statusColor = when (record.status) {
        "PRESENT" -> colors.safeZone
        "ABSENT" -> colors.bunkDanger
        else -> colors.deadlineWarning
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0E121B).copy(alpha = 0.7f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.subjectName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = "${record.time} • ${record.roomNo}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    color = colors.textSecondary
                )
            )
        }
        Text(
            text = record.status,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        )
    }
}
