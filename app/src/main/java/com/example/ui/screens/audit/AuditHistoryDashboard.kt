package com.example.ui.screens.audit

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
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.AddMedicalLeaveDialog
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun AuditHistoryDashboard(
    viewModel: MainViewModel,
    onNavigateToDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()
    val medicalLeaves by viewModel.medicalLeaves.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()

    var showMedicalDialog by remember { mutableStateOf(false) }
    var selectedAuditDate by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val currentMonth = remember { YearMonth.now() }
    val daysInMonth = remember(currentMonth) { currentMonth.lengthOfMonth() }
    val firstDayOffset = remember(currentMonth) { currentMonth.atDay(1).dayOfWeek.value - 1 } // 0..6 (Mon..Sun)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Visual Monthly Heatmap Calendar Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("heatmap_calendar_card"),
                borderColors = listOf(Color(0x6600E5FF), GlassBorderBottom)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ATTENDANCE HEATMAP", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeatmapLegendItem("Present", NeonEmerald)
                        HeatmapLegendItem("Partial", WarningAmber)
                        HeatmapLegendItem("Bunk", StrictRed)
                        HeatmapLegendItem("Off", IceSky)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Day headers (Mon..Sun)
                val daysHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    daysHeaders.forEach { h ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(h, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Grid
                val totalCells = firstDayOffset + daysInMonth
                val rowCount = (totalCells + 6) / 7

                for (r in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNum)
                                val dateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val dayLogs = logs.filter { it.date == dateStr }
                                val dayStatus = dayStatuses.find { it.date == dateStr }

                                val attCount = dayLogs.count { it.status == "attended" }
                                val bunkCount = dayLogs.count { it.status == "bunked" }
                                val canCount = dayLogs.count { it.status == "cancelled_by_faculty" }

                                val nodeColor = when {
                                    dayStatus?.leaveCategory == "mass_bunk" -> StrictRed
                                    dayStatus?.leaveCategory == "strike" || dayStatus?.leaveCategory == "college_holiday" -> IceSky
                                    dayLogs.isEmpty() -> Color(0x1AFFFFFF)
                                    attCount > 0 && bunkCount == 0 -> NeonEmerald
                                    attCount > 0 && bunkCount > 0 -> WarningAmber
                                    bunkCount > 0 && attCount == 0 -> StrictRed
                                    canCount > 0 -> IceSky
                                    else -> Color(0x33FFFFFF)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(nodeColor.copy(alpha = if (dayLogs.isEmpty() && dayStatus == null) 0.15f else 0.45f))
                                        .border(
                                            1.dp,
                                            if (selectedAuditDate == dateStr) NeonCyan else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            triggerHapticFeedback(context, false)
                                            selectedAuditDate = dateStr
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (nodeColor != Color(0x1AFFFFFF)) nodeColor else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                            }
                        }
                    }
                }

                // If a date is clicked in heatmap, show quick audit preview
                selectedAuditDate?.let { dateStr ->
                    val dayLogs = logs.filter { it.date == dateStr }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x2200E5FF))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Audit Date: $dateStr", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (dayLogs.isNotEmpty()) "${dayLogs.size} logs recorded on this date" else "No classes held or logged",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { onNavigateToDate(dateStr) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Open in Day Log", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Leave & Audit Analytics Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SEMESTER AUDIT BREAKDOWN",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditMetricBox(
                        label = "Physical Present",
                        value = "${summary.totalAttended - summary.totalProxy}",
                        color = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    AuditMetricBox(
                        label = "Proxy Entries",
                        value = "${summary.totalProxy}",
                        color = IceSky,
                        modifier = Modifier.weight(1f)
                    )
                    AuditMetricBox(
                        label = "Faculty Cancelled",
                        value = "${summary.totalFacultyCancelled}",
                        color = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Medical Leave & Application Vault
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEDICAL LEAVE & APPLICATION VAULT",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showMedicalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_medical_slip_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Slip", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (medicalLeaves.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = IceSky, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No Medical Certificates Archived", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Save medical applications submitted to HOD Mechanical for attendance condonation.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        items(medicalLeaves, key = { it.id }) { leave ->
            MedicalLeaveCard(leave = leave)
        }

        // 4. Data Backup, Reset & Privacy Settings
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DATA MANAGEMENT & IERT DEFAULTS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            triggerHapticFeedback(context, true)
                            viewModel.preloadDefaultData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("reset_iert_schedule_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load IERT Default", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StrictRed.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StrictRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StrictRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All Logs", color = StrictRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Offline-First Architecture. All database records remain securely isolated inside local Room DB.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    if (showMedicalDialog) {
        AddMedicalLeaveDialog(
            onDismiss = { showMedicalDialog = false },
            onSave = { leave ->
                viewModel.saveMedicalLeave(leave)
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Clear All App Records?", color = TextPrimary) },
            text = { Text("This will erase all attendance logs, assessments, and subjects. You can reload the IERT sample curriculum anytime.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StrictRed)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AmoledCardSurface
        )
    }
}

@Composable
fun HeatmapLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
fun AuditMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun MedicalLeaveCard(leave: MedicalLeaveEntity) {
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
                            .background(Color(0x2210B981))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(leave.status.uppercase(), color = NeonEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(leave.refNo, color = TextSecondary, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(leave.reason, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Doctor: ${leave.doctorName}", color = TextSecondary, fontSize = 11.sp)
                Text("Duration: ${leave.startDate} to ${leave.endDate}", color = IceSky, fontSize = 11.sp)

                if (leave.notes != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(leave.notes, color = TextMuted, fontSize = 10.sp)
                }
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
