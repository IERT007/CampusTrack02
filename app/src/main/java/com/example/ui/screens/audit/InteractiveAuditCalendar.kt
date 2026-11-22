package com.example.ui.screens.audit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.local.entity.DailyDayStatusEntity
import com.example.data.local.entity.HolidayRangeEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CaliperTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

typealias CampusViewModel = MainViewModel

enum class DayAttendanceState {
    FULL_PRESENT,
    PARTIAL_CANCELLED,
    ALL_CANCELLED,
    COLLEGE_OFF_HOLIDAY,
    BUNKED,
    UNRECORDED
}

@Composable
fun InteractiveAuditCalendar(
    viewModel: CampusViewModel,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToTodayView: ((String) -> Unit)? = null,
    onMonthChanged: ((YearMonth) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val view = LocalView.current
    val initialPage = remember { 1200 }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2400 })
    val coroutineScope = rememberCoroutineScope()

    val currentMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    LaunchedEffect(currentMonth) {
        onMonthChanged?.invoke(currentMonth)
    }

    var showMonthPicker by remember { mutableStateOf(false) }
    var inspectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    CaliperSoundManager.playSnap(view)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = colors.textPrimary)
            }

            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        CaliperSoundManager.playSnap(view)
                        showMonthPicker = true
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            IconButton(
                onClick = {
                    CaliperSoundManager.playSnap(view)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = colors.textPrimary)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val month = remember(page) {
                YearMonth.now().plusMonths((page - initialPage).toLong())
            }
            MonthGrid(
                month = month,
                viewModel = viewModel,
                onDateSelected = { date ->
                    inspectedDate = date
                    onDateSelected(date)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuditLegendChip("● Present", colors.safeGreen)
            AuditLegendChip("● Partial", colors.warnAmber)
            AuditLegendChip("● All Cancel", colors.dangerRed.copy(alpha = 0.78f))
            AuditLegendChip("● Bunked", colors.dangerRed)
            AuditLegendChip("○ Off/Hol", colors.offSlate)
        }
    }

    if (showMonthPicker) {
        val currentYear = remember { LocalDate.now().year }
        val semesterMonths = (1..12).map { YearMonth.of(currentYear, it) }
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Select Month", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    semesterMonths.chunked(3).forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowMonths.forEach { ym ->
                                val isChosen = (ym == currentMonth)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) colors.primaryAccent.copy(alpha = 0.2f) else colors.surface)
                                        .border(
                                            0.5.dp,
                                            if (isChosen) colors.primaryAccent else colors.border,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            val targetPage = initialPage + ((ym.year - YearMonth.now().year) * 12 + (ym.monthValue - YearMonth.now().monthValue))
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(targetPage)
                                            }
                                            showMonthPicker = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ym.format(DateTimeFormatter.ofPattern("MMM")),
                                        color = if (isChosen) colors.primaryAccent else colors.textSecondary,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("Close", color = colors.primaryAccent)
                }
            }
        )
    }

    inspectedDate?.let { date ->
        DaySummaryModal(
            date = date,
            viewModel = viewModel,
            onDismiss = { inspectedDate = null },
            onNavigateToToday = { dateStr ->
                inspectedDate = null
                onNavigateToTodayView?.invoke(dateStr)
            }
        )
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    viewModel: CampusViewModel,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val firstDayOffset = remember(month) { month.atDay(1).dayOfWeek.value - 1 }
    val logs by viewModel.logs.collectAsState()
    val holidays by viewModel.holidayRanges.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()
    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayHeaders.forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

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
                        val cellDate = month.atDay(dayNum)
                        val isToday = cellDate.isEqual(today)
                        val state = evaluateDayAttendanceState(cellDate, logs, holidays, dayStatuses)

                        val dotColor = when (state) {
                            DayAttendanceState.FULL_PRESENT -> colors.safeGreen
                            DayAttendanceState.PARTIAL_CANCELLED -> colors.warnAmber
                            DayAttendanceState.ALL_CANCELLED -> colors.dangerRed.copy(alpha = 0.78f)
                            DayAttendanceState.COLLEGE_OFF_HOLIDAY -> colors.offSlate
                            DayAttendanceState.BUNKED -> colors.dangerRed
                            DayAttendanceState.UNRECORDED -> Color.Transparent
                        }

                        val cellBg = when (state) {
                            DayAttendanceState.FULL_PRESENT -> colors.safeGreen.copy(alpha = 0.16f)
                            DayAttendanceState.PARTIAL_CANCELLED -> colors.warnAmber.copy(alpha = 0.16f)
                            DayAttendanceState.ALL_CANCELLED -> colors.dangerRed.copy(alpha = 0.14f)
                            DayAttendanceState.COLLEGE_OFF_HOLIDAY -> colors.offSlate.copy(alpha = 0.20f)
                            DayAttendanceState.BUNKED -> colors.dangerRed.copy(alpha = 0.18f)
                            DayAttendanceState.UNRECORDED -> if (isToday) colors.primaryAccent.copy(alpha = 0.14f) else colors.textPrimary.copy(alpha = 0.03f)
                        }

                        val borderColor = when {
                            isToday -> colors.primaryAccent
                            state == DayAttendanceState.COLLEGE_OFF_HOLIDAY -> colors.offSlate.copy(alpha = 0.35f)
                            dotColor != Color.Transparent -> dotColor.copy(alpha = 0.35f)
                            else -> colors.border.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .border(
                                    if (isToday) 1.2.dp else 0.5.dp,
                                    borderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    CaliperSoundManager.playSnap(view)
                                    CaliperHapticManager.tick(context)
                                    onDateSelected(cellDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state == DayAttendanceState.ALL_CANCELLED || state == DayAttendanceState.COLLEGE_OFF_HOLIDAY) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = dotColor.copy(alpha = 0.45f),
                                        start = Offset(size.width * 0.18f, size.height * 0.82f),
                                        end = Offset(size.width * 0.82f, size.height * 0.18f),
                                        strokeWidth = 1.3f
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    color = if (isToday) colors.primaryAccent else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday || dotColor != Color.Transparent) FontWeight.Bold else FontWeight.Normal
                                )
                                if (dotColor != Color.Transparent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).padding(2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

fun evaluateDayAttendanceState(
    date: LocalDate,
    logs: List<AttendanceLogEntity>,
    holidays: List<HolidayRangeEntity>,
    dayStatuses: List<DailyDayStatusEntity>
): DayAttendanceState {
    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isSunday = (date.dayOfWeek.value == 7)
    val isHoliday = holidays.any { dateStr >= it.startDate && dateStr <= it.endDate }
    val dayStatus = dayStatuses.find { it.date == dateStr }

    if (isSunday || isHoliday || dayStatus?.leaveCategory == "college_holiday") {
        return DayAttendanceState.COLLEGE_OFF_HOLIDAY
    }

    val dayLogs = logs.filter { it.date == dateStr }
    if (dayLogs.isEmpty()) {
        return DayAttendanceState.UNRECORDED
    }

    val attended = dayLogs.count { it.status == "attended" }
    val bunked = dayLogs.count { it.status == "bunked" }
    val cancelled = dayLogs.count { it.status == "cancelled_by_faculty" }
    val off = dayLogs.count { it.status == "college_off" }
    val total = dayLogs.size

    return when {
        bunked > 0 -> DayAttendanceState.BUNKED
        cancelled == total && total > 0 -> DayAttendanceState.ALL_CANCELLED
        cancelled in 1..2 && attended > 0 -> DayAttendanceState.PARTIAL_CANCELLED
        attended > 0 && bunked == 0 && cancelled == 0 -> DayAttendanceState.FULL_PRESENT
        off == total && total > 0 -> DayAttendanceState.COLLEGE_OFF_HOLIDAY
        attended > 0 -> DayAttendanceState.FULL_PRESENT
        cancelled > 0 -> DayAttendanceState.PARTIAL_CANCELLED
        else -> DayAttendanceState.UNRECORDED
    }
}

@Composable
fun AuditLegendChip(label: String, color: Color) {
    Text(
        text = label,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
fun DaySummaryModal(
    date: LocalDate,
    viewModel: CampusViewModel,
    onDismiss: () -> Unit,
    onNavigateToToday: (String) -> Unit
) {
    val colors = CaliperTheme.colors
    val dateStr = remember(date) { date.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val formattedDate = remember(date) { date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")) }
    val logs by viewModel.logs.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val holidays by viewModel.holidayRanges.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()

    val dayLogs = remember(logs, dateStr) { logs.filter { it.date == dateStr } }
    val isSunday = (date.dayOfWeek.value == 7)
    val isHoliday = holidays.any { dateStr >= it.startDate && dateStr <= it.endDate }
    val holidayObj = holidays.find { dateStr >= it.startDate && dateStr <= it.endDate }

    val dayState = evaluateDayAttendanceState(date, logs, holidays, dayStatuses)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Day Summary • ${date.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.textPrimary
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = colors.primaryAccent,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val (stateTitle, stateColor) = when (dayState) {
                    DayAttendanceState.FULL_PRESENT -> "100% Attended (Full Present)" to colors.safeGreen
                    DayAttendanceState.PARTIAL_CANCELLED -> "Partial Faculty Cancellation" to colors.warnAmber
                    DayAttendanceState.ALL_CANCELLED -> "All Scheduled Classes Cancelled" to colors.dangerRed.copy(alpha = 0.8f)
                    DayAttendanceState.COLLEGE_OFF_HOLIDAY -> (if (isSunday) "Sunday Break" else "Institutional Holiday (${holidayObj?.title ?: "Closed"})") to colors.offSlate
                    DayAttendanceState.BUNKED -> "Missed Lectures (Bunked)" to colors.dangerRed
                    DayAttendanceState.UNRECORDED -> "No Period Attendance Recorded" to colors.offSlate
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .border(0.5.dp, stateColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stateTitle,
                        color = stateColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (dayLogs.isEmpty()) {
                    Text(
                        text = if (isSunday) "Sunday / Institutional Break. No recurring periods."
                        else if (isHoliday) "Declared gazetted holiday: ${holidayObj?.title ?: "Off"}."
                        else "No period logs recorded for this day in Caliper DB.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "RECORDED LECTURES (${dayLogs.size}):",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLogs.forEach { log ->
                            val sub = subjects.find { it.id == log.subjectId }
                            val slot = allSlots.find { it.id == log.slotId }
                            val (statusText, statusColor) = when (log.status) {
                                "attended" -> (if (log.isProxy) "PROXY PRESENT" else "PRESENT") to colors.safeGreen
                                "bunked" -> "BUNKED" to colors.dangerRed
                                "cancelled_by_faculty" -> "CANCELLED BY FACULTY" to colors.warnAmber
                                "college_off" -> "COLLEGE OFF" to colors.offSlate
                                else -> log.status.uppercase() to colors.primaryAccent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub?.name ?: "Subject #${log.subjectId}",
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val timeSpan = if (slot != null) "${slot.startTime} - ${slot.endTime} • " else ""
                                    Text(
                                        text = "$timeSpan${sub?.code ?: ""} ${if (log.isExtraClass) "• Extra Class" else ""}",
                                        color = colors.textSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.2f))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onNavigateToToday(dateStr) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.primaryAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Open in Today →", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textSecondary)
            }
        }
    )
}
