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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHardwareEngine
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.CustomActivityEntity
import com.example.data.local.entity.TestRecordEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TodayScreen — CALIPER OS Daily Operations Console
 * Features:
 * 1. Semester Timeline Boundary (Pre-Semester & Post-Semester detection).
 * 2. In-Slot Class Test System (Mark test, persist marksObtained/maxMarks, view test scores).
 * 3. 1-Day Custom Activity Creation (e.g. Seminars, Training, Sports) with direct delete icon.
 * 4. Ultra-compact, dense card system without extra bloat or static AI cards.
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
    val testRecords by viewModel.testRecords.collectAsState()
    val customActivities by viewModel.currentDayActivities.collectAsState()

    val semesterStartDate by viewModel.semesterStartDate.collectAsState()
    val semesterEndDate by viewModel.semesterEndDate.collectAsState()

    var showExtraClassDialog by remember { mutableStateOf(false) }
    var showWeeklyTimetableDialog by remember { mutableStateOf(false) }
    var showHolidayManagerDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSemesterDatesDialog by remember { mutableStateOf(false) }
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

    // Semester Timeline Boundary check
    val isPreSemester = selectedDateStr < semesterStartDate
    val isPostSemester = selectedDateStr > semesterEndDate

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
                    backgroundColor = WarningAmber.copy(alpha = 0.15f),
                    borderColors = listOf(WarningAmber.copy(alpha = 0.6f), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PRE-CLASS ALERTS DISABLED", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Enable reminders 5m prior to lab / lecture", color = TextPrimary, fontSize = 11.5.sp)
                            }
                        }

                        Button(
                            onClick = {
                                CaliperHardwareEngine.pulseHaptic(context)
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Allow", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Date Navigation & Interactive Horizontal Week Strip
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header Date Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            CaliperHardwareEngine.pulseHaptic(context)
                            showDatePickerDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            tint = NeonCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Jump Chevrons (< Today >)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)
                                viewModel.setSelectedDate(parsedSelectedDate.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (!isToday) {
                            TextButton(
                                onClick = {
                                    CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                    CaliperHardwareEngine.pulseHaptic(context)
                                    viewModel.setSelectedDate(todayStr)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Today", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = {
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)
                                viewModel.setSelectedDate(parsedSelectedDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
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
                                .fluidTouchFeedback {
                                    CaliperHardwareEngine.pulseHaptic(context)
                                    viewModel.setSelectedDate(stripDateStr)
                                }
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

        // SEMESTER TIMELINE BOUNDARY BANNERS
        if (isPreSemester) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 12.dp,
                    backgroundColor = Color(0x2238BDF8),
                    borderColors = listOf(Color(0xFF38BDF8), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pre-Semester / Session Not Started",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Curriculum begins on $semesterStartDate. Regular slots will activate then.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSemesterDatesDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Edit Dates", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (isPostSemester) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 12.dp,
                    backgroundColor = Color(0x22A855F7),
                    borderColors = listOf(Color(0xFFA855F7), GlassBorderBottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Semester Concluded / Preparation Leave",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Official semester ended on $semesterEndDate. Best wishes for term exams!",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSemesterDatesDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Edit Dates", tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 2. Ultra-Compact "Live Day Operations" Card
        if (!isPreSemester && !isPostSemester) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_hero_card"),
                    innerPadding = 10.dp,
                    borderColors = listOf(Color(0x4400E5FF), GlassBorderBottom)
                ) {
                    // Streamlined Single Header Row
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

        // 3. Dynamic Slot Timeline Header with Upgraded Extra Class / Activity Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SCHEDULE & ACTIVITIES",
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
                        val totalRemaining = upcomingSlots.size + customActivities.size
                        Text("$totalRemaining items", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                    Text("+ Extra Event", color = NeonCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1-Day Custom Activities (Specific to this date)
        if (customActivities.isNotEmpty()) {
            items(customActivities, key = { "custom_act_${it.id}" }) { activity ->
                CustomActivityCard(
                    activity = activity,
                    onDelete = {
                        viewModel.deleteCustomActivity(activity.id)
                    }
                )
            }
        }

        // Slot Rendering (Only when within semester boundary)
        if (isPreSemester || isPostSemester) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isPreSemester) Icons.Default.HourglassEmpty else Icons.Default.Celebration,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPreSemester) "Session Not Yet Active" else "Term Completed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isPreSemester)
                                "Standard timetable begins on $semesterStartDate."
                            else
                                "Semester concluded on $semesterEndDate.",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (slotItems.isEmpty() && customActivities.isEmpty()) {
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
                            textAlign = TextAlign.Center
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
        } else if (upcomingSlots.isEmpty() && customActivities.isEmpty()) {
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

        // 4. Primary Upcoming Slots with In-Slot Test Marking
        if (!isPreSemester && !isPostSemester) {
            items(upcomingSlots, key = { it.slot.id }) { item ->
                val slotTest = testRecords.find { it.slotId == item.slot.id && it.date == selectedDateStr }
                SlotCard(
                    item = item,
                    testRecord = slotTest,
                    selectedDate = selectedDateStr,
                    onEditClick = { editingSlot = item.slot },
                    onStatusChange = { status, isProxy, notes ->
                        viewModel.setSlotAttendance(
                            slotId = item.slot.id,
                            subjectId = item.slot.subjectId,
                            status = status,
                            isProxy = isProxy,
                            notes = notes
                        )
                    },
                    onSaveTest = { name, obtained, max ->
                        viewModel.saveSlotTestRecord(
                            subjectId = item.slot.subjectId,
                            date = selectedDateStr,
                            slotId = item.slot.id,
                            testName = name,
                            marksObtained = obtained,
                            maxMarks = max
                        )
                    },
                    onDeleteTest = { testId ->
                        viewModel.deleteTestRecord(testId)
                    }
                )
            }
        }

        // 5. Completed / Marked Slots Expandable Tray
        if (!isPreSemester && !isPostSemester && markedSlots.isNotEmpty()) {
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
                    val slotTest = testRecords.find { it.slotId == item.slot.id && it.date == selectedDateStr }
                    MarkedSlotCard(
                        item = item,
                        testRecord = slotTest,
                        selectedDate = selectedDateStr,
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
                        },
                        onSaveTest = { name, obtained, max ->
                            viewModel.saveSlotTestRecord(
                                subjectId = item.slot.subjectId,
                                date = selectedDateStr,
                                slotId = item.slot.id,
                                testName = name,
                                marksObtained = obtained,
                                maxMarks = max
                            )
                        },
                        onDeleteTest = { testId ->
                            viewModel.deleteTestRecord(testId)
                        }
                    )
                }
            }
        }
    }

    // Upgraded Extra Class / Activity Creation Dialog (2 Paths)
    if (showExtraClassDialog) {
        AddExtraEventDialog(
            subjects = subjects,
            selectedDate = selectedDateStr,
            onDismiss = { showExtraClassDialog = false },
            onConfirmSubjectExtra = { subId, status, isProxy, notes ->
                viewModel.logExtraClass(subId, status, isProxy, notes ?: "")
                showExtraClassDialog = false
            },
            onConfirmCustomActivity = { title, startTime, endTime, loc, countAttendance ->
                viewModel.saveCustomActivity(selectedDateStr, title, startTime, endTime, loc, countAttendance)
                showExtraClassDialog = false
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

    if (showSemesterDatesDialog) {
        SemesterDatesDialog(
            currentStartDate = semesterStartDate,
            currentEndDate = semesterEndDate,
            onDismiss = { showSemesterDatesDialog = false },
            onSave = { sDate, eDate ->
                viewModel.setSemesterDates(sDate, eDate)
                showSemesterDatesDialog = false
            }
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

/**
 * 1-Day Single-Day Custom Activity Card
 * Claymorphic Matching Card Design:
 * - Background: Color(0xFF111622).copy(alpha = 0.75f)
 * - Border: 0.5.dp subtle vertical gradient (Color.White.copy(alpha = 0.15f) to Transparent)
 * - Corner radius: 16.dp
 * - Displays Event Title, Time Range, Location, Attendance tag (if enabled), and 1-tap delete trash icon
 */
@Composable
fun CustomActivityCard(
    activity: CustomActivityEntity,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val timeRangeText = if (activity.endTime.isNotBlank()) {
        "${activity.startTime} — ${activity.endTime}"
    } else {
        activity.startTime
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E121B).copy(alpha = 0.85f))
            .border(
                0.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
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
                            .background(Color(0xFF818CF8).copy(alpha = 0.18f))
                            .border(0.5.dp, Color(0xFF818CF8).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("1-DAY ACTIVITY", color = Color(0xFF818CF8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    if (activity.countsTowardsAttendance) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonEmerald.copy(alpha = 0.18f))
                                .border(0.5.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("COUNTS FOR ATTENDANCE", color = NeonEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeRangeText,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activity.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (activity.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = activity.location,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    CaliperSoundManager.playThud()
                    CaliperHapticManager.tick(context)
                    onDelete()
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StrictRed.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Discard Activity",
                    tint = StrictRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * SlotCard with In-Slot Class Test System
 */
@Composable
fun SlotCard(
    item: SlotDisplayItem,
    testRecord: TestRecordEntity?,
    selectedDate: String,
    onEditClick: (() -> Unit)? = null,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit,
    onSaveTest: (testName: String, marksObtained: Double, maxMarks: Double) -> Unit,
    onDeleteTest: (testId: Long) -> Unit
) {
    val context = LocalContext.current
    val currentLog = item.currentLog
    val currentStatus = currentLog?.status
    val isProxy = currentLog?.isProxy ?: false

    var isTestExpanded by remember { mutableStateOf(false) }
    var testNameInput by remember(testRecord) { mutableStateOf(testRecord?.testName ?: "Class Test") }
    var marksObtainedInput by remember(testRecord) { mutableStateOf(testRecord?.marksObtained?.toString() ?: "") }
    var maxMarksInput by remember(testRecord) { mutableStateOf(testRecord?.maxMarks?.toString() ?: "20") }

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
        borderWidth = if (item.isOngoing) 1.5.dp else 1.dp,
        innerPadding = 11.dp
    ) {
        // Top Row: Time, Room, and Live Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.slot.startTime} - ${item.slot.endTime}",
                    color = if (item.isOngoing) NeonCyan else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(item.slot.roomNo, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isOngoing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(0.5.dp, NeonCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("LIVE NOW", color = NeonCyan, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item.subject?.type?.let { TypeBadge(it) }
                if (onEditClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Slot",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subject Title & Faculty
        Text(
            text = item.subject?.name ?: "Unknown Subject",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.subject?.code} • ${item.subject?.facultyName}",
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp
            )
            item.subject?.strictness?.let { StrictnessBadge(it) }
        }

        // Test Score Display Badge (If test is already recorded)
        if (testRecord != null) {
            Spacer(modifier = Modifier.height(6.dp))
            val pct = if (testRecord.maxMarks > 0) (testRecord.marksObtained / testRecord.maxMarks) * 100 else 0.0
            val testColor = if (pct >= 75.0) NeonEmerald else if (pct >= 50.0) WarningAmber else StrictRed

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(testColor.copy(alpha = 0.15f))
                    .border(0.5.dp, testColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📝", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${testRecord.testName}: ${testRecord.marksObtained}/${testRecord.maxMarks} (${String.format("%.1f", pct)}%)",
                        color = testColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { onDeleteTest(testRecord.id) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Delete Test", tint = testColor, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4-State Quick Action Bar: Attended, Bunked, Cancelled, Off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val isAttended = (currentStatus == "attended")
            Button(
                onClick = {
                    CaliperSoundManager.playSuccess()
                    CaliperHapticManager.successClick(context)
                    onStatusChange("attended", isProxy, currentLog?.notes)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAttended) NeonEmerald else Color(0x1A00E676)
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isAttended) NeonEmerald else Color(0x3300E676)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Present", color = if (isAttended) Color.Black else NeonEmerald, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            val isBunked = (currentStatus == "bunked")
            Button(
                onClick = {
                    CaliperSoundManager.playThud()
                    CaliperHapticManager.bunkDoubleTap(context)
                    onStatusChange("bunked", false, currentLog?.notes)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBunked) StrictRed else Color(0x1AEF4444)
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBunked) StrictRed else Color(0x33EF4444)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Bunked", color = if (isBunked) Color.White else StrictRed, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            val isCancelled = (currentStatus == "cancelled_by_faculty")
            Button(
                onClick = {
                    CaliperSoundManager.playPip()
                    CaliperHapticManager.tick(context)
                    onStatusChange("cancelled_by_faculty", false, "Faculty absent")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCancelled) WarningAmber else Color(0x1AF59E0B)
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCancelled) WarningAmber else Color(0x33F59E0B)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelled", color = if (isCancelled) Color.Black else WarningAmber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }

            val isOff = (currentStatus == "college_off")
            Button(
                onClick = {
                    CaliperSoundManager.playSnap()
                    CaliperHapticManager.tick(context)
                    onStatusChange("college_off", false, "College closure")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOff) TextSecondary else Color(0x1AFFFFFF)
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isOff) TextSecondary else GlassBorderTop),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Off", color = if (isOff) Color.Black else TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Class Test Toggle Action Bar
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isTestExpanded) Color(0x2238BDF8) else Color(0x0CFFFFFF))
                .clickable { isTestExpanded = !isTestExpanded }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📝", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (testRecord != null) "Update Class Test Score" else "Mark as Class Test",
                    color = if (isTestExpanded) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (isTestExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isTestExpanded) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }

        // Expandable Class Test Score Entry Panel
        if (isTestExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = marksObtainedInput,
                        onValueChange = { marksObtainedInput = it },
                        label = { Text("Marks Scored", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxMarksInput,
                        onValueChange = { maxMarksInput = it },
                        label = { Text("Max Marks", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val obt = marksObtainedInput.toDoubleOrNull() ?: 0.0
                            val max = maxMarksInput.toDoubleOrNull() ?: 20.0
                            onSaveTest(testNameInput.ifBlank { "Class Test" }, obt, max)
                            isTestExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("Save Test Score", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Proxy Toggle
        if (currentStatus == "attended") {
            Spacer(modifier = Modifier.height(6.dp))
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isProxy) "Proxy Active (Tracked internally)" else "Mark as Proxy Presence",
                        fontSize = 10.5.sp,
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

/**
 * MarkedSlotCard with test record info and undo action
 */
@Composable
fun MarkedSlotCard(
    item: SlotDisplayItem,
    testRecord: TestRecordEntity?,
    selectedDate: String,
    onEditClick: (() -> Unit)? = null,
    onUndo: () -> Unit,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit,
    onSaveTest: (testName: String, marksObtained: Double, maxMarks: Double) -> Unit,
    onDeleteTest: (testId: Long) -> Unit
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

                if (testRecord != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📝 Test: ${testRecord.marksObtained}/${testRecord.maxMarks}",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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

/**
 * Upgraded Extra Event Dialog with 2 Creation Paths:
 * Path 1: Extra Subject Slot (existing curriculum subject)
 * Path 2: Single-Day Custom Activity (e.g. Seminar, Training, Sports)
 */
@Composable
fun AddExtraEventDialog(
    subjects: List<com.example.data.local.entity.SubjectEntity>,
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirmSubjectExtra: (subjectId: Long, status: String, isProxy: Boolean, notes: String?) -> Unit,
    onConfirmCustomActivity: (title: String, startTime: String, endTime: String, location: String, countAttendance: Boolean) -> Unit
) {
    var selectedPath by remember { mutableIntStateOf(0) } // 0 = Curriculum Subject, 1 = Single-Day Activity

    // Path 0 state
    var selectedSubjectId by remember { mutableLongStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var selectedStatus by remember { mutableStateOf("attended") }
    var extraNotes by remember { mutableStateOf("Extra Class") }

    // Path 1 state
    var activityTitle by remember { mutableStateOf("") }
    var activityStartTime by remember { mutableStateOf("02:00 PM") }
    var activityEndTime by remember { mutableStateOf("04:30 PM") }
    var activityLocation by remember { mutableStateOf("") }
    var countTowardsAttendance by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E121B),
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Add Event for $selectedDate",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Path Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedPath == 0) NeonCyan else Color.Transparent)
                            .clickable { selectedPath = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Extra Class",
                            color = if (selectedPath == 0) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedPath == 1) NeonCyan else Color.Transparent)
                            .clickable { selectedPath = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1-Day Activity",
                            color = if (selectedPath == 1) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedPath == 0) {
                    // Path 1: Curriculum Subject
                    Text("Select Subject:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    subjects.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubjectId = sub.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedSubjectId == sub.id),
                                onClick = { selectedSubjectId = sub.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${sub.code} — ${sub.name}", fontSize = 13.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attendance Outcome:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedStatus = "attended" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedStatus == "attended") NeonEmerald else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Present", color = if (selectedStatus == "attended") Color.Black else Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { selectedStatus = "bunked" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedStatus == "bunked") StrictRed else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bunked", color = Color.White, fontSize = 11.sp)
                        }
                    }
                } else {
                    // Path 2: Single-Day Custom Activity
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        label = { Text("Activity Title (e.g., Seminar, Sports, Training)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = activityStartTime,
                            onValueChange = { activityStartTime = it },
                            label = { Text("Start Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = activityEndTime,
                            onValueChange = { activityEndTime = it },
                            label = { Text("End Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = activityLocation,
                        onValueChange = { activityLocation = it },
                        label = { Text("Location (e.g., Main Auditorium, Lab 3)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Count towards overall attendance?",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Flags this event as valid academic attendance credit",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = countTowardsAttendance,
                            onCheckedChange = { countTowardsAttendance = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonEmerald,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPath == 0) {
                        onConfirmSubjectExtra(selectedSubjectId, selectedStatus, false, extraNotes)
                    } else {
                        if (activityTitle.isNotBlank()) {
                            onConfirmCustomActivity(
                                activityTitle,
                                activityStartTime,
                                activityEndTime,
                                activityLocation,
                                countTowardsAttendance
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

/**
 * Semester Dates Configuration Dialog
 */
@Composable
fun SemesterDatesDialog(
    currentStartDate: String,
    currentEndDate: String,
    onDismiss: () -> Unit,
    onSave: (startDate: String, endDate: String) -> Unit
) {
    var startDateInput by remember { mutableStateOf(currentStartDate) }
    var endDateInput by remember { mutableStateOf(currentEndDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Semester Timeline Boundary", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configure start and end dates of current semester. Slots will be suspended outside these bounds.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = startDateInput,
                    onValueChange = { startDateInput = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDateInput,
                    onValueChange = { endDateInput = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(startDateInput, endDateInput) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Save Timeline", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
