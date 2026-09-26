package com.example.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.components.*
import com.example.ui.dialogs.ExtraClassDialog
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayOperationsDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDateStr by viewModel.selectedDate.collectAsState()
    val slotItems by viewModel.currentDaySlots.collectAsState()
    val extraClasses by viewModel.currentDayExtraClasses.collectAsState()
    val dayStatus by viewModel.selectedDayStatus.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val globalSummary by viewModel.globalSummary.collectAsState()

    var showExtraClassDialog by remember { mutableStateOf(false) }
    var showDayEndConfirmPrompt by remember { mutableStateOf(true) }

    val parsedDate = remember(selectedDateStr) {
        try {
            LocalDate.parse(selectedDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
    val isToday = remember(selectedDateStr) {
        selectedDateStr == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    val dayName = remember(parsedDate) {
        parsedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    val formattedDate = remember(parsedDate) {
        parsedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    }

    val attendedCount = slotItems.count { it.currentLog?.status == "attended" }
    val conductedCount = slotItems.count {
        it.currentLog?.status == "attended" || it.currentLog?.status == "bunked"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Date Switcher Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1A1F293D))
                    .border(1.dp, GlassBorderTop, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.stepDay(-1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        tint = NeonCyan
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isToday) "Today • $formattedDate" else formattedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) NeonCyan else TextPrimary
                    )
                    Text(
                        text = "IERT Prayagraj • Mechanical Engg",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isToday) {
                        TextButton(onClick = { viewModel.resetToToday() }) {
                            Text("Today", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { viewModel.stepDay(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = NeonCyan
                        )
                    }
                }
            }
        }

        // 2. Live Day Hero Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_hero_card"),
                borderColors = listOf(Color(0x6600E5FF), GlassBorderBottom)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) NeonEmerald else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isToday) "LIVE DAY OPERATIONS" else "SCHEDULE AUDIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) NeonEmerald else WarningAmber,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$dayName's Timetable",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (slotItems.isNotEmpty()) "${slotItems.size} Academic Periods Scheduled" else "No Scheduled Slots (Sunday/Off)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x2200E5FF))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$attendedCount / ${slotItems.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text("Marked", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1-Tap Quick Action Bar
                Text("1-Tap Bulk Day Actions", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            triggerHapticFeedback(context, true)
                            viewModel.markWholeDayPresent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f).testTag("bulk_present_btn")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Present", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            triggerHapticFeedback(context, true)
                            viewModel.markMassBunkOrOff("mass_bunk")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StrictRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StrictRed.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).testTag("bulk_bunk_btn")
                    ) {
                        Icon(Icons.Default.GroupOff, contentDescription = null, tint = StrictRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mass Bunk", color = StrictRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            triggerHapticFeedback(context, true)
                            viewModel.markMassBunkOrOff("strike")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderTop),
                        modifier = Modifier.weight(1f).testTag("bulk_off_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("College Off", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (dayStatus != null && dayStatus?.leaveCategory != "none") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33F59E0B))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Status: ${dayStatus?.notes ?: dayStatus?.leaveCategory}",
                            color = WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Dynamic Slot Timeline Header with Extra Class Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIMETABLE PERIODS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showExtraClassDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_extra_class_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Extra Lecture", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Empty state if Sunday / no classes
        if (slotItems.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Weekend,
                            contentDescription = null,
                            tint = IceSky,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Scheduled Classes Today",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Enjoy your Sunday or revise for Sessional exams! Use '+ Extra Lecture' if an ad-hoc class was held.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. Slots Items
        items(slotItems, key = { it.slot.id }) { item ->
            SlotCard(
                item = item,
                onStatusChange = { status, isProxy, notes ->
                    viewModel.setSlotAttendance(
                        slotId = item.slot.id,
                        subjectId = item.slot.subjectId,
                        status = status,
                        isProxy = isProxy,
                        notes = notes
                    )
                }
            )
        }

        // Extra classes if logged
        if (extraClasses.isNotEmpty()) {
            item {
                Text(
                    text = "AD-HOC & EXTRA CLASSES LOGGED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber,
                    letterSpacing = 1.sp
                )
            }

            items(extraClasses) { (log, sub) ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(WarningAmber.copy(alpha = 0.5f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(WarningAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("EXTRA", color = WarningAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(sub?.name ?: "Extra Subject", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(log.notes ?: "Compensatory slot", color = TextSecondary, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (log.status == "attended") NeonEmerald.copy(alpha = 0.2f) else StrictRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (log.status == "attended") (if (log.isProxy) "PROXY ATTENDED" else "ATTENDED") else "MISSED",
                                color = if (log.status == "attended") NeonEmerald else StrictRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Day-End Summary Prompt
        if (showDayEndConfirmPrompt && slotItems.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(ElectricViolet.copy(alpha = 0.5f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Day-End Attendance Verification",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Marked $attendedCount of ${slotItems.size} periods. All figures safely synced offline.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = { showDayEndConfirmPrompt = false }) {
                            Icon(Icons.Default.Check, contentDescription = "Dismiss", tint = NeonEmerald)
                        }
                    }
                }
            }
        }
    }

    if (showExtraClassDialog) {
        ExtraClassDialog(
            subjects = subjects,
            onDismiss = { showExtraClassDialog = false },
            onConfirm = { subId, status, isProxy, notes ->
                viewModel.logExtraClass(subId, status, isProxy, notes)
            }
        )
    }
}

@Composable
fun SlotCard(
    item: SlotDisplayItem,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit
) {
    val context = LocalContext.current
    val currentLog = item.currentLog
    val currentStatus = currentLog?.status // "attended", "bunked", "cancelled_by_faculty", "college_off", or null
    val isProxy = currentLog?.isProxy ?: false

    val borderColor = when {
        item.isOngoing -> NeonCyan
        currentStatus == "attended" -> NeonEmerald.copy(alpha = 0.6f)
        currentStatus == "bunked" -> StrictRed.copy(alpha = 0.6f)
        currentStatus == "cancelled_by_faculty" -> WarningAmber.copy(alpha = 0.6f)
        currentStatus == "college_off" -> Color(0x6694A3B8)
        else -> GlassBorderTop
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(borderColor, GlassBorderBottom),
        borderWidth = if (item.isOngoing) 1.5.dp else 1.dp
    ) {
        // Slot Top Row: Time, Room, and Live Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.slot.startTime} - ${item.slot.endTime}",
                    color = if (item.isOngoing) NeonCyan else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.slot.roomNo, color = TextSecondary, fontSize = 10.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isOngoing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(0.5.dp, NeonCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE NOW", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                item.subject?.type?.let { TypeBadge(it) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subject Title & Faculty
        Text(
            text = item.subject?.name ?: "Unknown Subject",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.subject?.code} • ${item.subject?.facultyName}",
                color = TextSecondary,
                fontSize = 12.sp
            )
            item.subject?.strictness?.let { StrictnessBadge(it) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4-State Quick Action Bar: Attended, Bunked, Cancelled, Off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Attended
            val isAttended = (currentStatus == "attended")
            Button(
                onClick = {
                    triggerHapticFeedback(context, false)
                    onStatusChange("attended", isProxy, currentLog?.notes)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAttended) NeonEmerald else Color(0x1A00E676)
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAttended) NeonEmerald else Color(0x3300E676)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Present",
                    color = if (isAttended) Color.Black else NeonEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Bunked
            val isBunked = (currentStatus == "bunked")
            Button(
                onClick = {
                    triggerHapticFeedback(context, false)
                    onStatusChange("bunked", false, currentLog?.notes)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBunked) StrictRed else Color(0x1AEF4444)
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBunked) StrictRed else Color(0x33EF4444)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Bunked",
                    color = if (isBunked) Color.White else StrictRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Cancelled by Faculty
            val isCancelled = (currentStatus == "cancelled_by_faculty")
            Button(
                onClick = {
                    triggerHapticFeedback(context, false)
                    onStatusChange("cancelled_by_faculty", false, "Faculty absent")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCancelled) WarningAmber else Color(0x1AF59E0B)
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCancelled) WarningAmber else Color(0x33F59E0B)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Cancelled",
                    color = if (isCancelled) Color.Black else WarningAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. College Off
            val isOff = (currentStatus == "college_off")
            Button(
                onClick = {
                    triggerHapticFeedback(context, false)
                    onStatusChange("college_off", false, "College closure")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOff) TextSecondary else Color(0x1AFFFFFF)
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOff) TextSecondary else GlassBorderTop
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Off",
                    color = if (isOff) Color.Black else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Proxy Safe-Guard Toggle (Shown when marked present)
        if (currentStatus == "attended") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1200E5FF))
                    .clickable {
                        onStatusChange("attended", !isProxy, currentLog.notes)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isProxy) Icons.Default.Shield else Icons.Default.ShieldMoon,
                        contentDescription = "Proxy Safeguard",
                        tint = if (isProxy) NeonCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isProxy) "Proxy Active (Recorded on roll, tracked internally)" else "Mark as Proxy Presence",
                        fontSize = 11.sp,
                        color = if (isProxy) NeonCyan else TextSecondary
                    )
                }

                Checkbox(
                    checked = isProxy,
                    onCheckedChange = { checked ->
                        onStatusChange("attended", checked, currentLog.notes)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                )
            }
        }
    }
}
