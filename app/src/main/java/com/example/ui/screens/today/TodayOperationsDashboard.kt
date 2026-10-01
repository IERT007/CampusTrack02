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
import com.example.ui.dialogs.HolidayManagerDialog
import com.example.ui.dialogs.RetroactiveDatePickerDialog
import com.example.ui.dialogs.WeeklyTimetableEditorDialog
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
    val allSlots by viewModel.slots.collectAsState()
    val slotItems by viewModel.currentDaySlots.collectAsState()
    val extraClasses by viewModel.currentDayExtraClasses.collectAsState()
    val dayStatus by viewModel.selectedDayStatus.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val streak by viewModel.collegeStreak.collectAsState()
    val currentHoliday by viewModel.currentHoliday.collectAsState()
    val holidayRanges by viewModel.holidayRanges.collectAsState()

    var showExtraClassDialog by remember { mutableStateOf(false) }
    var showWeeklyTimetableDialog by remember { mutableStateOf(false) }
    var showHolidayManagerDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showMarkedTray by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<com.example.data.local.entity.TimetableSlotEntity?>(null) }

    val parsedDate = remember(selectedDateStr) {
        try {
            LocalDate.parse(selectedDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
    val today = remember { LocalDate.now() }
    val isToday = remember(selectedDateStr) {
        selectedDateStr == today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val isPastDate = remember(parsedDate, today) {
        parsedDate.isBefore(today)
    }

    val dayName = remember(parsedDate) {
        parsedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    val formattedDate = remember(parsedDate) {
        parsedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    }

    // Dynamic Card Consumption Partitioning:
    // Unmarked slots stay in primary active view; once marked, they transition to Marked Tray
    val upcomingSlots = remember(slotItems) {
        slotItems.filter { it.currentLog == null }
    }
    val markedSlots = remember(slotItems) {
        slotItems.filter { it.currentLog != null }
    }

    val attendedCount = slotItems.count { it.currentLog?.status == "attended" }
    val hasAutoVault by viewModel.hasAutoVault.collectAsState()

    var showNotificationPermissionBanner by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            } else false
        )
    }

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showNotificationPermissionBanner = !isGranted
        if (isGranted) {
            viewModel.scheduleAllLectureAlerts()
            viewModel.scheduleAllDeadlineAlerts()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Auto-Vault Restore Banner (Shown when DB is clean/empty but AutoVault snapshot exists)
        if (hasAutoVault && subjects.isEmpty() && allSlots.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x3300E5FF),
                    borderColors = listOf(NeonCyan, GlassBorderBottom)
                ) {
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
                                    .background(NeonCyan.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AUTO-BACKUP VAULT FOUND",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Restore previous session timetable and records",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                triggerHapticFeedback(context, true)
                                viewModel.restoreFromAutoVault { success ->
                                    if (success) {
                                        viewModel.scheduleAllLectureAlerts()
                                        viewModel.scheduleAllDeadlineAlerts()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Restore", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Notification Permission Prompt Banner (Android 13+)
        if (showNotificationPermissionBanner) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x228B5CF6),
                    borderColors = listOf(ElectricViolet.copy(alpha = 0.6f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Enable 07:30 AM Briefing & 10m Alerts", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Get pre-lecture heads-up and deadline alerts", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Enable", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // 0. Top Management & Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        showWeeklyTimetableDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("manage_weekly_timetable_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Timetable",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        showHolidayManagerDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Holidays",
                        color = WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        showDatePickerDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderTop),
                    modifier = Modifier.testTag("open_calendar_picker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Date", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Date Switcher Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showDatePickerDialog = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isToday) "Today • $formattedDate" else formattedDate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) NeonCyan else (if (isPastDate) WarningAmber else TextPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Date", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "IERT Prayagraj • Mechanical Engg (Tool)",
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

                // Horizontal Past / Current Days Strip for quick 1-tap retroactive jumping
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x12FFFFFF))
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (offset in -4..2) {
                        val stripDate = today.plusDays(offset.toLong())
                        val stripDateStr = stripDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val isSelected = (stripDateStr == selectedDateStr)
                        val isDayToday = (stripDate.isEqual(today))
                        val isDayPast = (stripDate.isBefore(today))
                        val dayLabel = stripDate.format(DateTimeFormatter.ofPattern("EEE"))
                        val dayNum = stripDate.dayOfMonth

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else (if (isDayToday) NeonCyan.copy(alpha = 0.5f) else Color.Transparent),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    triggerHapticFeedback(context, false)
                                    viewModel.setSelectedDate(stripDateStr)
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NeonCyan else (if (isDayPast) TextSecondary else TextMuted)
                                )
                                Text(
                                    text = "$dayNum",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isDayToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else (if (isDayToday) NeonCyan else TextPrimary)
                                )
                            }
                        }
                    }
                }

                // Retroactive Logging Banner if past date is active
                if (isPastDate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarningAmber.copy(alpha = 0.15f))
                            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retroactive Mode: Attendance updates recorded for past date ($formattedDate)",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Institutional Holiday / College Off Banner if date falls in declared range
        if (currentHoliday != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WarningAmber.copy(alpha = 0.2f),
                    borderColors = listOf(WarningAmber.copy(alpha = 0.8f), GlassBorderBottom)
                ) {
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
                                    .background(WarningAmber.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "OFFICIAL INSTITUTIONAL CLOSURE",
                                    color = WarningAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = currentHoliday?.title ?: "College Holiday",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active: ${currentHoliday?.startDate} to ${currentHoliday?.endDate} • No attendance penalty",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                triggerHapticFeedback(context, true)
                                viewModel.applyHolidayToCurrentDay(currentHoliday?.title ?: "Holiday")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Sync Off", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Live Day Hero Card with Streak Widget
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_hero_card"),
                borderColors = listOf(Color(0x6600E5FF), GlassBorderBottom)
            ) {
                // Streak Widget on Top of Hero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    // Attendance Streak Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x28FFB300))
                            .border(1.dp, WarningAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streak-Day College Streak",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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

                // 1-Tap Quick Bulk Actions
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UPCOMING PERIODS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2200E5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${upcomingSlots.size} remaining", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

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
                    Text("+ Extra Class", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            text = "No Scheduled Classes for $dayName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (dayName.equals("Sunday", ignoreCase = true)) {
                                "Enjoy your Sunday or revise for Sessional exams! Use '+ Extra Class' if an ad-hoc lecture was conducted."
                            } else {
                                "No recurring slots set for $dayName yet. Tap below to manage the weekly schedule."
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                triggerHapticFeedback(context, false)
                                showWeeklyTimetableDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Configure $dayName Slots", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (upcomingSlots.isEmpty()) {
            // All scheduled slots marked! (Dynamic Card Consumption celebration state)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(NeonEmerald.copy(alpha = 0.6f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All Periods Logged For Today!",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${markedSlots.size} of ${slotItems.size} periods recorded. Expand the tray below to review or undo.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Upcoming Slots (Dynamic Card Consumption: only unmarked slots stay here)
        items(upcomingSlots, key = { it.slot.id }) { item ->
            SlotCard(
                item = item,
                onEditClick = { editingSlot = item.slot },
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
                    text = "AD-HOC & EXTRA CLASSES",
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

        // 5. Dynamic Card Consumption: Toggleable "Marked Periods (N/Total)" Tray
        if (markedSlots.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            triggerHapticFeedback(context, false)
                            showMarkedTray = !showMarkedTray
                        },
                    borderColors = listOf(Color(0x33FFFFFF), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showMarkedTray) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marked Periods (${markedSlots.size} / ${slotItems.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (showMarkedTray) "Tap to Collapse" else "Tap to Inspect / Undo",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (showMarkedTray) {
                items(markedSlots, key = { "marked_${it.slot.id}" }) { item ->
                    MarkedSlotCard(
                        item = item,
                        onEditClick = { editingSlot = item.slot },
                        onUndo = {
                            triggerHapticFeedback(context, false)
                            viewModel.undoSlotAttendance(item.slot.id)
                        },
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

    if (showWeeklyTimetableDialog) {
        WeeklyTimetableEditorDialog(
            slots = allSlots,
            subjects = subjects,
            onDismiss = { showWeeklyTimetableDialog = false },
            onSaveSlot = { slot -> viewModel.saveSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.deleteSlot(slotId) },
            onAddNewSubject = { newSub -> viewModel.saveSubject(newSub) }
        )
    }

    if (showHolidayManagerDialog) {
        HolidayManagerDialog(
            holidayRanges = holidayRanges,
            onDismiss = { showHolidayManagerDialog = false },
            onSaveHoliday = { range -> viewModel.saveHolidayRange(range) },
            onDeleteHoliday = { id -> viewModel.deleteHolidayRange(id) }
        )
    }

    if (showDatePickerDialog) {
        RetroactiveDatePickerDialog(
            initialDate = selectedDateStr,
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { dateStr -> viewModel.setSelectedDate(dateStr) }
        )
    }

    if (editingSlot != null) {
        com.example.ui.dialogs.EditSlotDialog(
            slot = editingSlot!!,
            subjects = subjects,
            onDismissRequest = { editingSlot = null },
            onSaveSlot = { updatedSlot ->
                viewModel.updateTimetableSlot(updatedSlot)
                editingSlot = null
            },
            onDeleteSlot = { slotId ->
                viewModel.deleteTimetableSlot(slotId)
                editingSlot = null
            }
        )
    }
}

@Composable
fun SlotCard(
    item: SlotDisplayItem,
    onEditClick: (() -> Unit)? = null,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit
) {
    val context = LocalContext.current
    val currentLog = item.currentLog
    val currentStatus = currentLog?.status
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
                if (onEditClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Slot",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
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

@Composable
fun MarkedSlotCard(
    item: SlotDisplayItem,
    onEditClick: (() -> Unit)? = null,
    onUndo: () -> Unit,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit
) {
    val status = item.currentLog?.status ?: "attended"
    val isProxy = item.currentLog?.isProxy ?: false

    val (badgeText, badgeColor) = when (status) {
        "attended" -> (if (isProxy) "PROXY PRESENT" else "PRESENT") to NeonEmerald
        "bunked" -> "BUNKED" to StrictRed
        "cancelled_by_faculty" -> "FACULTY CANCELLED" to WarningAmber
        "college_off" -> "COLLEGE OFF" to TextSecondary
        else -> "MARKED" to NeonCyan
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(badgeColor.copy(alpha = 0.4f), GlassBorderBottom)
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
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(0.5.dp, badgeColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.slot.startTime} - ${item.slot.endTime}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subject?.name ?: "Subject",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.subject?.code} • ${item.slot.roomNo}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEditClick != null) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Slot", tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                OutlinedButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = WarningAmber, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
