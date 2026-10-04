package com.example.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.components.claymorphicSurface
import com.example.ui.theme.CaliperTheme

data class TimetableSlot(
    val slotId: Long,
    val subjectName: String,
    val subjectCode: String = "",
    val startTime: String,
    val endTime: String,
    val roomNo: String,
    val facultyName: String = ""
)

data class ProcessedSlotRecord(
    val slotId: Long,
    val subjectName: String,
    val status: String,
    val time: String,
    val roomNo: String
)

enum class AttendanceStatus {
    PRESENT, BUNKED, CANCELLED, COLLEGE_OFF
}

@Composable
fun TodayScheduleFeed(
    unprocessedSlots: List<TimetableSlot>,
    completedSlots: List<ProcessedSlotRecord>,
    onMarkAttendance: (Long, AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var trayExpanded by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(unprocessedSlots, key = { it.slotId }) { slot ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(250))
            ) {
                ActiveSlotCard(
                    slot = slot,
                    onStatusSelected = { status -> onMarkAttendance(slot.slotId, status) }
                )
            }
        }

        item {
            if (completedSlots.isNotEmpty()) {
                CompletedTrayHeader(
                    count = completedSlots.size,
                    total = unprocessedSlots.size + completedSlots.size,
                    isExpanded = trayExpanded,
                    onToggle = { trayExpanded = !trayExpanded }
                )
            }
        }

        if (trayExpanded) {
            items(completedSlots, key = { it.slotId }) { record ->
                HistoricalSlotRow(record = record)
            }
        }
    }
}

@Composable
fun ActiveSlotCard(
    slot: TimetableSlot,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .claymorphicSurface(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${slot.startTime} - ${slot.endTime}",
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(slot.roomNo, color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = slot.subjectName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (slot.subjectCode.isNotEmpty()) {
                Text(
                    text = slot.subjectCode,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-state action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        CaliperSoundManager.playSuccess()
                        CaliperHapticManager.successClick(context)
                        onStatusSelected(AttendanceStatus.PRESENT)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.safeZone.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.safeZone),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Present", color = colors.safeZone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        CaliperSoundManager.playThud()
                        CaliperHapticManager.bunkDoubleTap(context)
                        onStatusSelected(AttendanceStatus.BUNKED)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.bunkDanger.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.bunkDanger),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bunk", color = colors.bunkDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        CaliperSoundManager.playPip()
                        CaliperHapticManager.tick(context)
                        onStatusSelected(AttendanceStatus.CANCELLED)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.deadlineWarning.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.deadlineWarning),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = colors.deadlineWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        CaliperSoundManager.playSnap()
                        CaliperHapticManager.tick(context)
                        onStatusSelected(AttendanceStatus.COLLEGE_OFF)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Off", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CompletedTrayHeader(
    count: Int,
    total: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable {
                CaliperSoundManager.playSnap()
                CaliperHapticManager.tick(context)
                onToggle()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Completed Today ($count / $total)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun HistoricalSlotRow(record: ProcessedSlotRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(record.subjectName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${record.time} • ${record.roomNo}", color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(record.status.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
