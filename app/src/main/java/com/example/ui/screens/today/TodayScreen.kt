package com.example.ui.screens.today

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalView
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
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestRecordEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.components.GlassCard
import com.example.ui.components.StrictnessBadge
import com.example.ui.components.TypeBadge
import com.example.ui.dialogs.EditSlotDialog
import com.example.ui.dialogs.HolidayManagerDialog
import com.example.ui.dialogs.RetroactiveDatePickerDialog
import com.example.ui.dialogs.WeeklyTimetableEditorDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.fluidTouchFeedback
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TodayScreen — CALIPER v2.0 Daily Operations Console
 * Centralized CaliperTheme.colors token usage throughout with zero hardcoded hex colors.
 */
@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors

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
    val orphanExtraLogs by viewModel.currentDayOrphanExtraLogs.collectAsState()

    val semesterStartDate by viewModel.semesterStartDate.collectAsState()
    val semesterEndDate by viewModel.semesterEndDate.collectAsState()

    var showExtraClassDialog by remember { mutableStateOf(false) }
    var showWeeklyTimetableDialog by remember { mutableStateOf(false) }
    var showHolidayManagerDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSemesterDatesDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var showMarkedTray by remember { mutableStateOf(false) }
    var reschedulingSlot by remember { mutableStateOf<SlotDisplayItem?>(null) }
    var enteringMarksForTest by remember { mutableStateOf<Pair<TestRecordEntity, String>?>(null) }
    var extraSlotPendingDelete by remember { mutableStateOf<SlotDisplayItem?>(null) }

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
    val totalMarkedCount = markedSlots.size + orphanExtraLogs.size
    val totalPeriodsCount = slotItems.size + orphanExtraLogs.size
    val attendedCount = remember(markedSlots, orphanExtraLogs) {
        markedSlots.count { it.currentLog?.status == "attended" } +
            orphanExtraLogs.count { it.first.status == "attended" }
    }

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
        contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Auto-Vault Restore Banner
        if (hasAutoVault && subjects.isEmpty() && allSlots.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
                    backgroundColor = colors.primaryAccent.copy(alpha = 0.16f),
                    borderColors = listOf(colors.primaryAccent, Color.Transparent)
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
                                    .background(colors.primaryAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("AUTO-BACKUP VAULT FOUND", color = colors.primaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Restore previous semester timetable", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = colors.onPrimaryAccent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    backgroundColor = colors.warnAmber.copy(alpha = 0.15f),
                    borderColors = listOf(colors.warnAmber.copy(alpha = 0.6f), Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PRE-CLASS ALERTS DISABLED", color = colors.warnAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Enable reminders 10m prior to lab / lecture", color = colors.textPrimary, fontSize = 11.5.sp)
                            }
                        }

                        Button(
                            onClick = {
                                CaliperHardwareEngine.pulseHaptic(context)
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.warnAmber),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            showDatePickerDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                viewModel.setSelectedDate(parsedSelectedDate.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (!isToday) {
                            TextButton(
                                onClick = {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    viewModel.setSelectedDate(todayStr)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Today", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                viewModel.setSelectedDate(parsedSelectedDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                LazyRow(
                    state = dateStripLazyState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.7f))
                        .border(0.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(dateStripList, key = { it.second }) { item ->
                        val (stripDate, stripDateStr, dayPair) = item
                        val (dayLabel, dayNum) = dayPair
                        val isSelected = (stripDateStr == selectedDateStr)
                        val isDayToday = stripDate.isEqual(today)
                        val isDayPast = stripDate.isBefore(today)

                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 0.28f else if (isDayToday) 0.12f else 0.0f,
                            label = "stripAlpha"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isSelected) colors.primaryAccent else if (isDayToday) colors.primaryAccent.copy(alpha = 0.5f) else Color.Transparent,
                            label = "stripBorder"
                        )

                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(colors.primaryAccent.copy(alpha = animatedAlpha))
                                .border(1.dp, animatedBorderColor, RoundedCornerShape(7.dp))
                                .fluidTouchFeedback {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    viewModel.setSelectedDate(stripDateStr)
                                }
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
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
                                    color = if (isSelected) colors.primaryAccent else (if (isDayPast) colors.textSecondary else colors.textMuted)
                                )
                                Text(
                                    text = "$dayNum",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected || isDayToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected || isDayToday) colors.primaryAccent else colors.textPrimary
                                )
                            }
                        }
                    }
                }

                if (isPastDate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.warnAmber.copy(alpha = 0.15f))
                            .border(0.5.dp, colors.warnAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retroactive Mode: Updates logged for $formattedDate",
                                color = colors.warnAmber,
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
                    backgroundColor = colors.primaryAccent.copy(alpha = 0.14f),
                    borderColors = listOf(colors.primaryAccent, Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pre-Semester / Session Not Started",
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Curriculum begins on $semesterStartDate. Regular slots will activate then.",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSemesterDatesDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Edit Dates", tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
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
                    backgroundColor = colors.primaryAccent.copy(alpha = 0.14f),
                    borderColors = listOf(colors.primaryAccent, Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Semester Concluded / Preparation Leave",
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Official semester ended on $semesterEndDate. Best wishes for term exams!",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSemesterDatesDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Edit Dates", tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
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
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.35f), Color.Transparent)
                ) {
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
                                    .background(if (isToday) colors.safeGreen else colors.warnAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$dayName • $totalPeriodsCount Classes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primaryAccent.copy(alpha = 0.14f))
                                .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$attendedCount/$totalPeriodsCount Marked",
                                color = colors.primaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                            colors = ButtonDefaults.buttonColors(containerColor = colors.safeGreen.copy(alpha = 0.16f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.safeGreen.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("bulk_present_btn")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Present", color = colors.safeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                CaliperSoundManager.playThud()
                                CaliperHapticManager.bunkDoubleTap(context)
                                viewModel.markMassBunkOrOff("mass_bunk")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dangerRed.copy(alpha = 0.16f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.dangerRed.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("bulk_bunk_btn")
                        ) {
                            Icon(Icons.Default.GroupOff, contentDescription = null, tint = colors.dangerRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mass Bunk", color = colors.dangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                viewModel.markMassBunkOrOff("strike")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, colors.border),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("bulk_off_btn")
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("College Off", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (dayStatus != null && dayStatus?.leaveCategory != "none") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.warnAmber.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Status: ${dayStatus?.notes ?: dayStatus?.leaveCategory}",
                                color = colors.warnAmber,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Institutional Holiday / Break Banner
        if (currentHoliday != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
                    backgroundColor = colors.warnAmber.copy(alpha = 0.16f),
                    borderColors = listOf(colors.warnAmber.copy(alpha = 0.6f), Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("OFFICIAL INSTITUTIONAL CLOSURE", color = colors.warnAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(currentHoliday?.title ?: "Holiday", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                CaliperHardwareEngine.pulseHaptic(context, true)
                                viewModel.applyHolidayToCurrentDay(currentHoliday?.title ?: "Holiday")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.warnAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Sync Off", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Dynamic Slot Timeline Header with Fixed "+ Extra Event" Button (Zero Duplicate Plus)
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
                        color = colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.16f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        val totalRemaining = upcomingSlots.size + customActivities.size
                        Text("$totalRemaining items", color = colors.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        CaliperSoundManager.playSnap(view)
                        CaliperHapticManager.tick(context)
                        showExtraClassDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.16f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("add_extra_class_btn")
                ) {
                    Text("+ Extra Event", color = colors.primaryAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1-Day Custom Activities
        if (customActivities.isNotEmpty()) {
            items(customActivities, key = { "custom_act_${it.id}" }) { activity ->
                CustomActivityCard(
                    activity = activity,
                    onDelete = { viewModel.deleteCustomActivity(activity.id) }
                )
            }
        }

        // Slot Rendering
        if (isPreSemester || isPostSemester) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 16.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isPreSemester) Icons.Default.HourglassEmpty else Icons.Default.Celebration,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPreSemester) "Session Not Yet Active" else "Term Completed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isPreSemester) "Standard timetable begins on $semesterStartDate."
                            else "Semester concluded on $semesterEndDate.",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (slotItems.isEmpty() && customActivities.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 16.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Weekend, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Scheduled Classes for $dayName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (dayName.equals("Sunday", ignoreCase = true)) {
                                "Sunday / Institutional Break. Revise workshop journals or drawing sheets."
                            } else {
                                "No recurring slots set for $dayName. Tap below to configure schedule."
                            },
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                showWeeklyTimetableDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.18f)),
                            border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure $dayName Slots", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (upcomingSlots.isEmpty() && customActivities.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = 10.dp,
                    borderColors = listOf(colors.safeGreen.copy(alpha = 0.55f), Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.safeGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "All Periods Logged For Today!",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${markedSlots.size} of ${slotItems.size} recorded. Expand tray below to review or undo.",
                                color = colors.textSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Upcoming Slots (regular slots strictly suppressed outside semester; date-specific extras still allowed)
        items(upcomingSlots, key = { it.slot.id }) { item ->
            val slotTest = testRecords.find { it.slotId == item.slot.id && it.date == selectedDateStr }
            val isExtra = item.slot.roomNo.contains("Extra", ignoreCase = true) || item.isDateSpecificExtra
            SlotCard(
                item = item,
                testRecord = slotTest,
                selectedDate = selectedDateStr,
                onEditClick = if (!item.isDateSpecificExtra) { { editingSlot = item.slot } } else null,
                onDeleteSlot = {
                    CaliperHapticManager.tick(context)
                    if (item.isDateSpecificExtra) {
                        if (item.currentLog != null) {
                            CaliperSoundManager.playSnap(view)
                            extraSlotPendingDelete = item
                        } else {
                            CaliperSoundManager.playThud()
                            viewModel.deleteDateSpecificExtraSlot(
                                extraSlotId = item.extraSlotId ?: -item.slot.id,
                                deleteAttendanceLog = true
                            )
                        }
                    } else {
                        CaliperSoundManager.playThud()
                        viewModel.deleteSlot(item.slot.id)
                    }
                },
                onRescheduleClick = { reschedulingSlot = item },
                onRevertReschedule = if (item.isRescheduled) {
                    { viewModel.revertSlotReschedule(item.slot.id, selectedDateStr) }
                } else null,
                onEnterMarksClick = { record ->
                    enteringMarksForTest = record to (item.subject?.name ?: "Class Test")
                },
                onStatusChange = { status, isProxy, notes ->
                    viewModel.setSlotAttendance(
                        slotId = item.slot.id,
                        subjectId = item.slot.subjectId,
                        status = status,
                        isProxy = isProxy,
                        isExtraClass = isExtra,
                        notes = notes ?: if (isExtra) "Extra Class" else null
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
                onDeleteTest = { testId -> viewModel.deleteTestRecord(testId) }
            )
        }

        // 5. Completed / Marked Slots + Orphan Extra Logs Expandable Tray (Home-Audit Synchronized)
        if (markedSlots.isNotEmpty() || orphanExtraLogs.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.75f))
                        .border(0.5.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable {
                            CaliperSoundManager.playSnap(view)
                            showMarkedTray = !showMarkedTray
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("marked_periods_tray_toggle")
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
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marked Periods ($totalMarkedCount / $totalPeriodsCount)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Text(
                            text = if (showMarkedTray) "Tap to Collapse" else "Tap to Inspect / Undo",
                            fontSize = 10.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (showMarkedTray) {
                items(markedSlots, key = { "marked_${it.slot.id}" }) { item ->
                    val slotTest = testRecords.find { it.slotId == item.slot.id && it.date == selectedDateStr }
                    val isExtra = item.slot.roomNo.contains("Extra", ignoreCase = true) || item.isDateSpecificExtra
                    MarkedSlotCard(
                        item = item,
                        testRecord = slotTest,
                        selectedDate = selectedDateStr,
                        onEditClick = if (!item.isDateSpecificExtra) { { editingSlot = item.slot } } else null,
                        onDeleteSlot = {
                            CaliperHapticManager.tick(context)
                            if (item.isDateSpecificExtra) {
                                if (item.currentLog != null) {
                                    CaliperSoundManager.playSnap(view)
                                    extraSlotPendingDelete = item
                                } else {
                                    CaliperSoundManager.playThud()
                                    viewModel.deleteDateSpecificExtraSlot(
                                        extraSlotId = item.extraSlotId ?: -item.slot.id,
                                        deleteAttendanceLog = true
                                    )
                                }
                            } else {
                                CaliperSoundManager.playThud()
                                viewModel.deleteSlot(item.slot.id)
                            }
                        },
                        onRescheduleClick = { reschedulingSlot = item },
                        onRevertReschedule = if (item.isRescheduled) {
                            { viewModel.revertSlotReschedule(item.slot.id, selectedDateStr) }
                        } else null,
                        onEnterMarksClick = { record ->
                            enteringMarksForTest = record to (item.subject?.name ?: "Class Test")
                        },
                        onUndo = {
                            CaliperHapticManager.tick(context)
                            viewModel.undoSlotAttendance(item.slot.id)
                        },
                        onStatusChange = { status, isProxy, notes ->
                            viewModel.setSlotAttendance(
                                slotId = item.slot.id,
                                subjectId = item.slot.subjectId,
                                status = status,
                                isProxy = isProxy,
                                isExtraClass = isExtra,
                                notes = notes ?: if (isExtra) "Extra Class" else null
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
                        onDeleteTest = { testId -> viewModel.deleteTestRecord(testId) }
                    )
                }

                // Orphan Extra Class Logs (Historical Fallback keeping Home & Audit 100% synced)
                items(orphanExtraLogs, key = { "orphan_extra_${it.first.id}" }) { (log, subject) ->
                    OrphanExtraLogCard(
                        log = log,
                        subject = subject,
                        onUndoOrDelete = {
                            CaliperSoundManager.playThud()
                            CaliperHapticManager.tick(context)
                            viewModel.deleteAttendanceLog(log)
                        }
                    )
                }
            }
        }
    }

    if (showExtraClassDialog) {
        AddExtraEventDialog(
            subjects = subjects,
            selectedDate = selectedDateStr,
            onDismiss = { showExtraClassDialog = false },
            onConfirmExtraClass = { subId, startTime, endTime, roomNo, tag ->
                viewModel.saveDateSpecificExtraSlot(
                    date = selectedDateStr,
                    subjectId = subId,
                    startTime = startTime,
                    endTime = endTime,
                    roomNo = roomNo.ifBlank { "LT-Extra" },
                    tag = tag.ifBlank { "Extra Class" }
                )
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

    reschedulingSlot?.let { item ->
        SingleDayRescheduleDialog(
            item = item,
            date = selectedDateStr,
            onDismiss = { reschedulingSlot = null },
            onConfirmReschedule = { newStartTime, newEndTime, newRoomNo ->
                viewModel.rescheduleSlotForDay(
                    slotId = item.slot.id,
                    date = selectedDateStr,
                    newStartTime = newStartTime,
                    newEndTime = newEndTime,
                    newRoomNo = newRoomNo
                )
                reschedulingSlot = null
            }
        )
    }

    enteringMarksForTest?.let { (testRecord, subName) ->
        EnterMarksDialog(
            testRecord = testRecord,
            subjectName = subName,
            onDismiss = { enteringMarksForTest = null },
            onSaveMarks = { marks ->
                viewModel.updateTestRecordMarks(testRecord.id, marks)
                enteringMarksForTest = null
            }
        )
    }

    // 2-Option Confirmation Dialog when deleting an ExtraSlotEntity that already has a recorded AttendanceLogEntity
    extraSlotPendingDelete?.let { pendingItem ->
        val extraId = pendingItem.extraSlotId ?: -pendingItem.slot.id
        val subName = pendingItem.subject?.name ?: "Extra Class"
        val loggedStatusLabel = when (pendingItem.currentLog?.status) {
            "attended" -> "PRESENT"
            "bunked" -> "BUNKED"
            "cancelled_by_faculty" -> "CANCELLED"
            "college_off" -> "COLLEGE OFF"
            else -> "RECORDED"
        }

        AlertDialog(
            onDismissRequest = { extraSlotPendingDelete = null },
            containerColor = colors.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Remove Extra Class Slot",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "$subName (${pendingItem.slot.startTime} - ${pendingItem.slot.endTime}) already has an attendance log marked as $loggedStatusLabel.",
                        color = colors.textSecondary,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "Choose how you want to handle the recorded attendance:",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Option 1: Delete Slot & Attendance
                    Button(
                        onClick = {
                            viewModel.deleteDateSpecificExtraSlot(extraSlotId = extraId, deleteAttendanceLog = true)
                            extraSlotPendingDelete = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_extra_slot_and_attendance_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dangerRed.copy(alpha = 0.18f),
                            contentColor = colors.dangerRed
                        ),
                        border = BorderStroke(1.dp, colors.dangerRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete Slot & Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Option 2: Remove Slot Only (Keep Attendance)
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteDateSpecificExtraSlot(extraSlotId = extraId, deleteAttendanceLog = false)
                            extraSlotPendingDelete = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_extra_slot_keep_attendance_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryAccent),
                        border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Remove Slot Only (Keep Attendance)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { extraSlotPendingDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun OrphanExtraLogCard(
    log: com.example.data.local.entity.AttendanceLogEntity,
    subject: SubjectEntity?,
    onUndoOrDelete: () -> Unit
) {
    val colors = CaliperTheme.colors
    val (statusLabel, statusColor) = when (log.status) {
        "attended" -> (if (log.isProxy) "PROXY PRESENT" else "PRESENT") to colors.safeGreen
        "bunked" -> "BUNKED" to colors.dangerRed
        "cancelled_by_faculty" -> "CANCELLED" to colors.warnAmber
        "college_off" -> "COLLEGE OFF" to colors.offSlate
        else -> log.status.uppercase() to colors.primaryAccent
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("orphan_extra_log_${log.id}"),
        innerPadding = 10.dp,
        borderColors = listOf(statusColor.copy(alpha = 0.5f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.16f))
                            .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "EXTRA CLASS",
                            color = colors.primaryAccent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = subject?.name ?: "Subject #${log.subjectId}",
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(subject?.code?.ifBlank { "Extra" } ?: "Extra")
                        if (!log.notes.isNullOrBlank()) {
                            append(" • ${log.notes}")
                        } else {
                            append(" • Recorded Extra Session")
                        }
                    },
                    color = colors.textSecondary,
                    fontSize = 10.5.sp
                )
            }

            OutlinedButton(
                onClick = onUndoOrDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.dangerRed),
                border = BorderStroke(0.75.dp, colors.dangerRed.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("undo_orphan_extra_${log.id}")
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Undo Extra Log", modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomActivityCard(
    activity: CustomActivityEntity,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val timeRangeText = if (activity.endTime.isNotBlank()) {
        "${activity.startTime} — ${activity.endTime}"
    } else {
        activity.startTime
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.90f))
            .border(
                0.5.dp,
                Brush.verticalGradient(listOf(colors.border, Color.Transparent)),
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
                            .background(colors.primaryAccent.copy(alpha = 0.16f))
                            .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("1-DAY ACTIVITY", color = colors.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    if (activity.countsTowardsAttendance) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.safeGreen.copy(alpha = 0.16f))
                                .border(0.5.dp, colors.safeGreen.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("COUNTS FOR ATTENDANCE", color = colors.safeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeRangeText,
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activity.title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (activity.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = activity.location,
                            color = colors.textSecondary,
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
                    .background(colors.dangerRed.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Discard Activity",
                    tint = colors.dangerRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SlotCard(
    item: SlotDisplayItem,
    testRecord: TestRecordEntity?,
    selectedDate: String,
    onEditClick: (() -> Unit)? = null,
    onDeleteSlot: (() -> Unit)? = null,
    onRescheduleClick: (() -> Unit)? = null,
    onRevertReschedule: (() -> Unit)? = null,
    onEnterMarksClick: ((TestRecordEntity) -> Unit)? = null,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit,
    onSaveTest: (testName: String, marksObtained: Double?, maxMarks: Double) -> Unit,
    onDeleteTest: (testId: Long) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    val currentLog = item.currentLog
    val currentStatus = currentLog?.status
    val isProxy = currentLog?.isProxy ?: false
    val isExtraClass = item.slot.roomNo.contains("Extra", ignoreCase = true) || item.isDateSpecificExtra

    var isTestExpanded by remember { mutableStateOf(false) }
    var testNameInput by remember(testRecord) { mutableStateOf(testRecord?.testName ?: "Class Test") }
    var marksObtainedInput by remember(testRecord) { mutableStateOf(testRecord?.marksObtained?.toString() ?: "") }
    var maxMarksInput by remember(testRecord) { mutableStateOf(testRecord?.maxMarks?.toString() ?: "20") }

    val borderColor = when {
        item.isOngoing -> colors.primaryAccent
        item.isRescheduled -> colors.warnAmber.copy(alpha = 0.7f)
        isExtraClass -> colors.primaryAccent.copy(alpha = 0.5f)
        currentStatus == "attended" -> colors.safeGreen.copy(alpha = 0.6f)
        currentStatus == "bunked" -> colors.dangerRed.copy(alpha = 0.6f)
        currentStatus == "cancelled_by_faculty" -> colors.warnAmber.copy(alpha = 0.6f)
        currentStatus == "college_off" -> colors.offSlate.copy(alpha = 0.6f)
        else -> colors.border
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(borderColor, Color.Transparent),
        borderWidth = if (item.isOngoing || item.isRescheduled) 1.5.dp else 0.75.dp,
        innerPadding = 11.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.slot.startTime} - ${item.slot.endTime}",
                    color = if (item.isOngoing) colors.primaryAccent else if (item.isRescheduled) colors.warnAmber else colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surface)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(item.slot.roomNo, color = colors.textSecondary, fontSize = 11.sp)
                }

                if (item.isRescheduled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.warnAmber.copy(alpha = 0.18f))
                            .border(0.5.dp, colors.warnAmber, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "SHIFTED (${item.originalTime})",
                            color = colors.warnAmber,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (onRevertReschedule != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Revert",
                            color = colors.primaryAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onRevertReschedule() }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExtraClass) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.18f))
                            .border(0.5.dp, colors.primaryAccent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("EXTRA", color = colors.primaryAccent, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (item.isOngoing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.18f))
                            .border(0.5.dp, colors.primaryAccent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("LIVE", color = colors.primaryAccent, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item.subject?.type?.let { TypeBadge(it) }

                if (onRescheduleClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onRescheduleClick, modifier = Modifier.size(22.dp)) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Shift Time",
                            tint = if (item.isRescheduled) colors.warnAmber else colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (onEditClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEditClick, modifier = Modifier.size(22.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Slot",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (onDeleteSlot != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteSlot,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("delete_slot_${item.slot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Slot",
                            tint = colors.dangerRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.subject?.name ?: "Unknown Subject",
            color = colors.textPrimary,
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
                color = colors.textSecondary,
                fontSize = 11.5.sp
            )
            item.subject?.strictness?.let { StrictnessBadge(it) }
        }

        if (testRecord != null) {
            Spacer(modifier = Modifier.height(6.dp))
            if (testRecord.marksObtained == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.warnAmber.copy(alpha = 0.14f))
                        .border(0.5.dp, colors.warnAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${testRecord.testName} (Max: ${testRecord.maxMarks})",
                                color = colors.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pending Evaluation",
                                color = colors.warnAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEnterMarksClick != null) {
                            Button(
                                onClick = { onEnterMarksClick(testRecord) },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.warnAmber),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Enter Marks", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = { onDeleteTest(testRecord.id) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete Test", tint = colors.warnAmber, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            } else {
                val pct = if (testRecord.maxMarks > 0) ((testRecord.marksObtained ?: 0.0) / testRecord.maxMarks) * 100 else 0.0
                val testColor = if (pct >= 75.0) colors.safeGreen else if (pct >= 50.0) colors.warnAmber else colors.dangerRed

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(testColor.copy(alpha = 0.14f))
                        .border(0.5.dp, testColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = testColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${testRecord.testName}: ${testRecord.marksObtained}/${testRecord.maxMarks} (${String.format("%.1f", pct)}%)",
                            color = testColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEnterMarksClick != null) {
                            Text(
                                text = "Edit",
                                color = testColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onEnterMarksClick(testRecord) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = { onDeleteTest(testRecord.id) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete Test", tint = testColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4-State Quick Action Bar
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
                    containerColor = if (isAttended) colors.safeGreen else colors.safeGreen.copy(alpha = 0.14f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isAttended) colors.safeGreen else colors.safeGreen.copy(alpha = 0.35f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Present", color = if (isAttended) Color.Black else colors.safeGreen, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            val isBunked = (currentStatus == "bunked")
            Button(
                onClick = {
                    CaliperSoundManager.playThud()
                    CaliperHapticManager.bunkDoubleTap(context)
                    onStatusChange("bunked", false, currentLog?.notes)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBunked) colors.dangerRed else colors.dangerRed.copy(alpha = 0.14f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isBunked) colors.dangerRed else colors.dangerRed.copy(alpha = 0.35f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Bunked", color = if (isBunked) Color.White else colors.dangerRed, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            val isCancelled = (currentStatus == "cancelled_by_faculty")
            Button(
                onClick = {
                    CaliperSoundManager.playPip()
                    CaliperHapticManager.tick(context)
                    onStatusChange("cancelled_by_faculty", false, "Faculty absent")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCancelled) colors.warnAmber else colors.warnAmber.copy(alpha = 0.14f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isCancelled) colors.warnAmber else colors.warnAmber.copy(alpha = 0.35f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelled", color = if (isCancelled) Color.Black else colors.warnAmber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }

            val isOff = (currentStatus == "college_off")
            Button(
                onClick = {
                    CaliperSoundManager.playSnap(view)
                    CaliperHapticManager.tick(context)
                    onStatusChange("college_off", false, "College closure")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOff) colors.offSlate else colors.surface
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isOff) colors.offSlate else colors.border),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Off", color = if (isOff) Color.White else colors.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Class Test Toggle Action Bar
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isTestExpanded) colors.primaryAccent.copy(alpha = 0.14f) else colors.surface)
                .clickable {
                    CaliperSoundManager.playSnap(view)
                    isTestExpanded = !isTestExpanded
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = if (isTestExpanded) colors.primaryAccent else colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (testRecord != null) "Update Class Test Score" else "Mark as Class Test",
                    color = if (isTestExpanded) colors.primaryAccent else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (isTestExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isTestExpanded) colors.primaryAccent else colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        if (isTestExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = marksObtainedInput,
                        onValueChange = { marksObtainedInput = it },
                        label = { Text("Marks (blank = Pending)", fontSize = 10.sp) },
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
                            val obt = marksObtainedInput.toDoubleOrNull()
                            val max = maxMarksInput.toDoubleOrNull() ?: 20.0
                            onSaveTest(testNameInput.ifBlank { "Class Test" }, obt, max)
                            isTestExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (marksObtainedInput.isBlank()) "Log (Pending Result)" else "Save Test Score",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (currentStatus == "attended") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.10f))
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
                        tint = if (isProxy) colors.primaryAccent else colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isProxy) "Proxy Active (Tracked internally)" else "Mark as Proxy Presence",
                        fontSize = 10.5.sp,
                        color = if (isProxy) colors.primaryAccent else colors.textSecondary
                    )
                }

                Checkbox(
                    checked = isProxy,
                    onCheckedChange = { checked -> onStatusChange("attended", checked, currentLog.notes) },
                    colors = CheckboxDefaults.colors(checkedColor = colors.primaryAccent)
                )
            }
        }
    }
}

@Composable
fun MarkedSlotCard(
    item: SlotDisplayItem,
    testRecord: TestRecordEntity?,
    selectedDate: String,
    onEditClick: (() -> Unit)? = null,
    onDeleteSlot: (() -> Unit)? = null,
    onRescheduleClick: (() -> Unit)? = null,
    onRevertReschedule: (() -> Unit)? = null,
    onEnterMarksClick: ((TestRecordEntity) -> Unit)? = null,
    onUndo: () -> Unit,
    onStatusChange: (status: String, isProxy: Boolean, notes: String?) -> Unit,
    onSaveTest: (testName: String, marksObtained: Double?, maxMarks: Double) -> Unit,
    onDeleteTest: (testId: Long) -> Unit
) {
    val colors = CaliperTheme.colors
    val status = item.currentLog?.status ?: "attended"
    val isProxy = item.currentLog?.isProxy ?: false
    val isExtraClass = item.slot.roomNo.contains("Extra", ignoreCase = true) || item.isDateSpecificExtra

    val (badgeText, badgeColor) = when (status) {
        "attended" -> (if (isProxy) "PROXY PRESENT" else "PRESENT") to colors.safeGreen
        "bunked" -> "BUNKED" to colors.dangerRed
        "cancelled_by_faculty" -> "FACULTY CANCELLED" to colors.warnAmber
        "college_off" -> "COLLEGE OFF" to colors.offSlate
        else -> "MARKED" to colors.primaryAccent
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(badgeColor.copy(alpha = 0.45f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExtraClass) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primaryAccent.copy(alpha = 0.18f))
                                .border(0.5.dp, colors.primaryAccent, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("EXTRA", color = colors.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .border(0.5.dp, badgeColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.slot.startTime} - ${item.slot.endTime}",
                        color = if (item.isRescheduled) colors.warnAmber else colors.textSecondary,
                        fontSize = 11.sp
                    )

                    if (item.isRescheduled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.warnAmber.copy(alpha = 0.18f))
                                .border(0.5.dp, colors.warnAmber, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("SHIFTED (${item.originalTime})", color = colors.warnAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subject?.name ?: "Subject",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.subject?.code} • ${item.slot.roomNo}",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )

                if (testRecord != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (testRecord.marksObtained == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Test: Pending Evaluation", color = colors.warnAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (onEnterMarksClick != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enter Marks",
                                    color = colors.primaryAccent,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onEnterMarksClick(testRecord) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Test: ${testRecord.marksObtained}/${testRecord.maxMarks}",
                            color = colors.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRescheduleClick != null) {
                    IconButton(onClick = onRescheduleClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Shift Time",
                            tint = if (item.isRescheduled) colors.warnAmber else colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Slot", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (onDeleteSlot != null) {
                    IconButton(
                        onClick = onDeleteSlot,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_marked_slot_${item.slot.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Slot",
                            tint = colors.dangerRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                OutlinedButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.warnAmber),
                    border = BorderStroke(1.dp, colors.warnAmber.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = colors.warnAmber, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SingleDayRescheduleDialog(
    item: SlotDisplayItem,
    date: String,
    onDismiss: () -> Unit,
    onConfirmReschedule: (newStartTime: String, newEndTime: String, newRoomNo: String?) -> Unit
) {
    val colors = CaliperTheme.colors
    var startTime by remember { mutableStateOf(item.slot.startTime) }
    var endTime by remember { mutableStateOf(item.slot.endTime) }
    var roomNo by remember { mutableStateOf(item.slot.roomNo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        title = {
            Column {
                Text("Reschedule / Shift Time", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Today Only ($date) • ${item.subject?.name ?: "Slot"}", color = colors.textSecondary, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This time shift will apply to $date only. Your master recurring weekly timetable will not be changed.",
                    color = colors.textSecondary,
                    fontSize = 11.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("10:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = roomNo,
                    onValueChange = { roomNo = it },
                    label = { Text("Room / Location (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmReschedule(startTime.trim(), endTime.trim(), roomNo.trim().ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent
                )
            ) {
                Text("Apply (Today Only)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun EnterMarksDialog(
    testRecord: TestRecordEntity,
    subjectName: String,
    onDismiss: () -> Unit,
    onSaveMarks: (Double?) -> Unit
) {
    val colors = CaliperTheme.colors
    var marksInput by remember { mutableStateOf(testRecord.marksObtained?.toString() ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        title = {
            Column {
                Text("Enter Test Marks", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${testRecord.testName} • $subjectName", color = colors.textSecondary, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Maximum Marks: ${testRecord.maxMarks}",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = marksInput,
                    onValueChange = {
                        marksInput = it
                        errorMsg = null
                    },
                    label = { Text("Marks Scored (leave blank for Pending)") },
                    placeholder = { Text("e.g. 18.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = colors.dangerRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (marksInput.isBlank()) {
                        onSaveMarks(null)
                    } else {
                        val parsed = marksInput.toDoubleOrNull()
                        if (parsed == null || parsed < 0.0 || parsed > testRecord.maxMarks) {
                            errorMsg = "Please enter a valid score between 0 and ${testRecord.maxMarks}"
                            return@Button
                        }
                        onSaveMarks(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent
                )
            ) {
                Text("Save Marks", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun AddExtraEventDialog(
    subjects: List<SubjectEntity>,
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirmExtraClass: (subjectId: Long, startTime: String, endTime: String, roomNo: String, tag: String) -> Unit,
    onConfirmCustomActivity: (title: String, startTime: String, endTime: String, location: String, countAttendance: Boolean) -> Unit
) {
    val colors = CaliperTheme.colors
    var selectedPath by remember { mutableIntStateOf(0) }

    var selectedSubjectId by remember { mutableLongStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var slotStartTime by remember { mutableStateOf("09:00") }
    var slotEndTime by remember { mutableStateOf("10:00") }
    var slotRoomNo by remember { mutableStateOf("Room 301") }
    var selectedTag by remember { mutableStateOf("Extra Class") }

    val quickTimePresets = listOf(
        "08:00" to "09:00",
        "09:00" to "10:00",
        "10:00" to "11:00",
        "11:30" to "12:30",
        "13:30" to "14:30",
        "14:30" to "15:30",
        "15:30" to "16:30"
    )

    val tagPresets = listOf("Extra Class", "Theory", "Lab", "Tutorial", "Doubt Session")

    var activityTitle by remember { mutableStateOf("") }
    var activityStartTime by remember { mutableStateOf("02:00 PM") }
    var activityEndTime by remember { mutableStateOf("04:30 PM") }
    var activityLocation by remember { mutableStateOf("") }
    var countTowardsAttendance by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Schedule Event for $selectedDate",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedPath == 0) colors.primaryAccent else Color.Transparent)
                            .clickable { selectedPath = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Extra Class",
                            color = if (selectedPath == 0) colors.onPrimaryAccent else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedPath == 1) colors.primaryAccent else Color.Transparent)
                            .clickable { selectedPath = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1-Day Activity",
                            color = if (selectedPath == 1) colors.onPrimaryAccent else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedPath == 0) {
                    Text("Select Subject:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (subjects.isEmpty()) {
                        Text("No subjects registered yet. Add subjects first.", fontSize = 12.sp, color = colors.dangerRed)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            subjects.forEach { sub ->
                                val isSelected = (selectedSubjectId == sub.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            0.5.dp,
                                            if (isSelected) colors.primaryAccent else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedSubjectId = sub.id }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedSubjectId = sub.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.primaryAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "${sub.code} — ${sub.name}",
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.primaryAccent else colors.textPrimary
                                        )
                                        Text(
                                            text = sub.facultyName,
                                            fontSize = 10.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Lecture Time:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTimePresets.forEach { (start, end) ->
                            val isPresetSelected = (slotStartTime == start && slotEndTime == end)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isPresetSelected) colors.primaryAccent.copy(alpha = 0.22f) else colors.surface)
                                    .border(0.5.dp, if (isPresetSelected) colors.primaryAccent else colors.border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        slotStartTime = start
                                        slotEndTime = end
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$start - $end",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPresetSelected) colors.primaryAccent else colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = slotStartTime,
                            onValueChange = { slotStartTime = it },
                            label = { Text("Start Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = slotEndTime,
                            onValueChange = { slotEndTime = it },
                            label = { Text("End Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = slotRoomNo,
                        onValueChange = { slotRoomNo = it },
                        label = { Text("Room / Hall (e.g., Room 301, LT-4)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Type Tag:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagPresets.forEach { tag ->
                            val isTagSelected = (selectedTag == tag)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTagSelected) colors.primaryAccent.copy(alpha = 0.22f) else colors.surface)
                                    .border(0.5.dp, if (isTagSelected) colors.primaryAccent else colors.border, RoundedCornerShape(6.dp))
                                    .clickable { selectedTag = tag }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTagSelected) colors.primaryAccent else colors.textPrimary
                                )
                            }
                        }
                    }
                } else {
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
                            .background(colors.surface)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Count towards overall attendance?",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Flags this event as valid academic attendance credit",
                                color = colors.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = countTowardsAttendance,
                            onCheckedChange = { countTowardsAttendance = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.safeGreen,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceVariant
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
                        onConfirmExtraClass(
                            selectedSubjectId,
                            slotStartTime,
                            slotEndTime,
                            slotRoomNo,
                            selectedTag
                        )
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent
                ),
                modifier = Modifier.testTag("add_to_schedule_btn")
            ) {
                Text("Add to Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun SemesterDatesDialog(
    currentStartDate: String,
    currentEndDate: String,
    onDismiss: () -> Unit,
    onSave: (startDate: String, endDate: String) -> Unit
) {
    val colors = CaliperTheme.colors
    var startDateInput by remember { mutableStateOf(currentStartDate) }
    var endDateInput by remember { mutableStateOf(currentEndDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        title = { Text("Semester Timeline Boundary", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configure start and end dates of current semester. Slots will be suspended outside these bounds.",
                    fontSize = 12.sp,
                    color = colors.textSecondary
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent
                )
            ) {
                Text("Save Timeline", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
