package com.example.ui.screens.today

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.audio.CaliperHardwareEngine
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TodayScreen — Ultra-Compact Dynamic Daily Operations Console
 * - Zero top void (tight alignment beneath TopBar)
 * - Old static AI card deleted (3D Floating Robo Copilot is sole AI entity)
 * - Streamlined single-row "Live Day Operations" container with 34.dp bulk buttons
 * - Compact card system (8.dp to 10.dp padding, 8.dp vertical arrangement)
 */
@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedDateStr by viewModel.selectedDate.collectAsState()
    val slotItems by viewModel.currentDaySlots.collectAsState()
    val currentHoliday by viewModel.currentHoliday.collectAsState()
    val holidayRanges by viewModel.holidayRanges.collectAsState()
    val dayStatus by viewModel.selectedDayStatus.collectAsState()
    val hasAutoVault by viewModel.hasAutoVault.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showExtraClassDialog by remember { mutableStateOf(false) }
    var showWeeklyTimetableDialog by remember { mutableStateOf(false) }
    var showHolidayManagerDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var showMarkedTray by remember { mutableStateOf(false) }

    var showNotificationPermissionBanner by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showNotificationPermissionBanner = !isGranted
        if (isGranted) {
            viewModel.scheduleAllLectureAlerts()
            viewModel.scheduleAllDeadlineAlerts()
        }
    }

    val today = remember { LocalDate.now() }
    val todayStr = remember { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val isToday = (selectedDateStr == todayStr)

    val parsedSelectedDate = remember(selectedDateStr) {
        try {
            LocalDate.parse(selectedDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            today
        }
    }
    val isPastDate = parsedSelectedDate.isBefore(today)

    val dayName = remember(parsedSelectedDate) {
        parsedSelectedDate.format(DateTimeFormatter.ofPattern("EEEE"))
    }
    val formattedDate = remember(parsedSelectedDate) {
        parsedSelectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    }

    val upcomingSlots = remember(slotItems) { slotItems.filter { it.currentLog == null } }
    val markedSlots = remember(slotItems) { slotItems.filter { it.currentLog != null } }
    val attendedCount = remember(markedSlots) { markedSlots.count { it.currentLog?.status == "attended" } }

    val dateStripList = remember(today) {
        (-14..14).map { offset ->
            val date = today.plusDays(offset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayLabel = date.format(DateTimeFormatter.ofPattern("EEE"))
            val dayNum = date.dayOfMonth
            Triple(date, dateStr, Pair(dayLabel, dayNum))
        }
    }
    val dateStripLazyState = rememberLazyListState()

    LaunchedEffect(selectedDateStr) {
        val selectedIndex = dateStripList.indexOfFirst { it.second == selectedDateStr }
        if (selectedIndex >= 0) {
            val targetIndex = (selectedIndex - 2).coerceAtLeast(0)
            dateStripLazyState.animateScrollToItem(targetIndex)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Auto-Vault Restore Banner
        if (hasAutoVault && subjects.isEmpty() && allSlots.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("AUTO-BACKUP VAULT FOUND", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Restore previous semester timetable", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                CaliperHardwareEngine.pulseHaptic(context, true)
                                viewModel.restoreFromAutoVault { success ->
                                    if (success) {
                                        viewModel.scheduleAllLectureAlerts()
                                        viewModel.scheduleAllDeadlineAlerts()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Restore", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Notification Permission Prompt Banner
        if (showNotificationPermissionBanner) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
                    backgroundColor = Color(0x228B5CF6),
                    borderColors = listOf(ElectricViolet.copy(alpha = 0.6f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Enable 07:30 AM Briefing Alerts", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                Text("Pre-lecture heads-up & attendance alerts", color = TextSecondary, fontSize = 9.5.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Enable", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Date Switcher Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1A1F293D))
                        .border(1.dp, GlassBorderTop, RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.stepDay(-1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)
                                showDatePickerDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isToday) "Today • $formattedDate" else formattedDate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) NeonCyan else (if (isPastDate) WarningAmber else TextPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Date", tint = NeonCyan, modifier = Modifier.size(15.dp))
                        }
                        Text(
                            text = "IERT Prayagraj • Mechanical (Tool)",
                            fontSize = 9.5.sp,
                            color = TextMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isToday) {
                            TextButton(
                                onClick = { viewModel.resetToToday() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Today", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.stepDay(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Horizontal Interactive Weekly Date Strip with dynamic cursor & auto-scroll
                LazyRow(
                    state = dateStripLazyState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x12FFFFFF))
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(dateStripList, key = { it.second }) { item ->
                        val (stripDate, stripDateStr, dayPair) = item
                        val (dayLabel, dayNum) = dayPair
                        val isSelected = (stripDateStr == selectedDateStr)
                        val isDayToday = (stripDate.isEqual(today))
                        val isDayPast = (stripDate.isBefore(today))

                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 0.32f else if (isDayToday) 0.12f else 0.0f,
                            label = "stripAlpha"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isSelected) NeonCyan else if (isDayToday) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                            label = "stripBorder"
                        )

                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(NeonCyan.copy(alpha = animatedAlpha))
                                .border(1.dp, animatedBorderColor, RoundedCornerShape(7.dp))
                                .clickable {
                                    CaliperHardwareEngine.pulseHaptic(context)
                                    viewModel.setSelectedDate(stripDateStr)
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NeonCyan else (if (isDayPast) TextSecondary else TextMuted)
                                )
                                Text(
                                    text = "$dayNum",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected || isDayToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else (if (isDayToday) NeonCyan else TextPrimary)
                                )
                            }
                        }
                    }
                }

                // Retroactive Logging Banner if past date
                if (isPastDate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningAmber.copy(alpha = 0.15f))
                            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retroactive Mode: Updates logged for $formattedDate",
                                color = WarningAmber,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Ultra-Compact "Live Day Operations" Card (Streamlined Single-Row Header + 34.dp Bulk Buttons)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_hero_card"),
                innerPadding = 10.dp,
                borderColors = listOf(Color(0x4400E5FF), GlassBorderBottom)
            ) {
                // Streamlined Single Header Row: Day Title (Left) + Marked Progress Badge (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isToday) NeonEmerald else WarningAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$dayName • ${slotItems.size} Classes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Compact Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1F38BDF8))
                            .border(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$attendedCount/${slotItems.size} Marked",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Slim Down 1-Tap Bulk Action Buttons (Height = 34.dp, 11.sp text, 14.dp icons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            CaliperSoundManager.playSuccess()
                            CaliperHapticManager.successClick(context)
                            viewModel.markWholeDayPresent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("bulk_present_btn")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Present", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            CaliperSoundManager.playThud()
                            CaliperHapticManager.bunkDoubleTap(context)
                            viewModel.markMassBunkOrOff("mass_bunk")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StrictRed.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StrictRed.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("bulk_bunk_btn")
                    ) {
                        Icon(Icons.Default.GroupOff, contentDescription = null, tint = StrictRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mass Bunk", color = StrictRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            CaliperSoundManager.playSnap()
                            CaliperHapticManager.tick(context)
                            viewModel.markMassBunkOrOff("strike")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x18FFFFFF)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderTop),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("bulk_off_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("College Off", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (dayStatus != null && dayStatus?.leaveCategory != "none") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22F59E0B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Status: ${dayStatus?.notes ?: dayStatus?.leaveCategory}",
                            color = WarningAmber,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Institutional Holiday / Break Banner if active
        if (currentHoliday != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
                    backgroundColor = WarningAmber.copy(alpha = 0.18f),
                    borderColors = listOf(WarningAmber.copy(alpha = 0.6f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("OFFICIAL INSTITUTIONAL CLOSURE", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(currentHoliday?.title ?: "Holiday", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                CaliperHardwareEngine.pulseHaptic(context, true)
                                viewModel.applyHolidayToCurrentDay(currentHoliday?.title ?: "Holiday")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Sync Off", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
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
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x2200E5FF))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("${upcomingSlots.size} remaining", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showExtraClassDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("add_extra_class_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Extra Class", color = NeonCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Empty state if Sunday / no classes
        if (slotItems.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 16.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Weekend,
                            contentDescription = null,
                            tint = IceSky,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Scheduled Classes for $dayName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (dayName.equals("Sunday", ignoreCase = true)) {
                                "Sunday / Institutional Break. Revise workshop journals or drawing sheets."
                            } else {
                                "No recurring slots set for $dayName. Tap below to configure schedule."
                            },
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                CaliperHardwareEngine.pulseHaptic(context)
                                showWeeklyTimetableDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Configure $dayName Slots", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (upcomingSlots.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
                    borderColors = listOf(NeonEmerald.copy(alpha = 0.6f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "All Periods Logged For Today!",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${markedSlots.size} of ${slotItems.size} recorded. Expand tray below to review or undo.",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Upcoming Slots (Compact height ~35% reduction)
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

        // 5. Completed / Marked Slots Expandable Tray
        if (markedSlots.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x12FFFFFF))
                        .clickable { showMarkedTray = !showMarkedTray }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marked Periods (${markedSlots.size} / ${slotItems.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (showMarkedTray) "Tap to Collapse" else "Tap to Inspect / Undo",
                            fontSize = 10.5.sp,
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
                            CaliperHardwareEngine.pulseHaptic(context)
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

    // Dialogs
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
        EditSlotDialog(
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
