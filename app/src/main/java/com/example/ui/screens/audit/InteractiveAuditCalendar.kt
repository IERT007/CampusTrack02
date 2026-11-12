package com.example.ui.screens.audit

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    modifier: Modifier = Modifier
) {
    val initialPage = remember { 1200 }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2400 })
    val coroutineScope = rememberCoroutineScope()
    
    val currentMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
    }
    var showMonthPicker by remember { mutableStateOf(false) }
    var inspectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = CaliperTheme.colors.textPrimary)
            }

            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CaliperTheme.colors.textPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showMonthPicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = CaliperTheme.colors.textPrimary)
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

        // Refactored Clean Bottom Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuditLegendChip("● Present (All)", Color(0xFF22C55E))
            AuditLegendChip("● Partial Cancel", Color(0xFFF59E0B))
            AuditLegendChip("● All Cancelled", Color(0xFFF87171))
            AuditLegendChip("● Bunked", Color(0xFFEF4444))
            AuditLegendChip("○ Off/Holiday", CaliperTheme.colors.textSecondary)
        }
    }

    // Month Selector Dialog
    if (showMonthPicker) {
        val currentYear = remember { LocalDate.now().year }
        val semesterMonths = listOf(
            YearMonth.of(currentYear, 7),
            YearMonth.of(currentYear, 8),
            YearMonth.of(currentYear, 9),
            YearMonth.of(currentYear, 10),
            YearMonth.of(currentYear, 11),
            YearMonth.of(currentYear, 12)
        )
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            containerColor = CaliperTheme.colors.cardFill,
            title = { Text("Select Semester Month", color = CaliperTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    semesterMonths.forEach { ym ->
                        val isChosen = (ym == currentMonth)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) CaliperTheme.colors.primaryAccent else CaliperTheme.colors.containerSurface)
                                .clickable {
                                    val targetPage = initialPage + ((ym.year - YearMonth.now().year) * 12 + (ym.monthValue - YearMonth.now().monthValue))
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(targetPage)
                                    }
                                    showMonthPicker = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                color = if (isChosen) CaliperTheme.colors.onPrimaryAccent else CaliperTheme.colors.textPrimary,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("Close", color = CaliperTheme.colors.textPrimary)
                }
            }
        )
    }

    // Interactive Date Inspector Dialog: "Day Summary • [Date]"
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
    val colors = CaliperTheme.colors
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val firstDayOffset = remember(month) { month.atDay(1).dayOfWeek.value - 1 }
    val logs by viewModel.logs.collectAsState()
    val holidays by viewModel.holidayRanges.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()
    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // Weekday header
        val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayHeaders.forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, color = CaliperTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                        val cellBg = when (state) {
                            DayAttendanceState.FULL_PRESENT -> Color(0x2822C55E)
                            DayAttendanceState.PARTIAL_CANCELLED -> Color(0x28F59E0B)
                            DayAttendanceState.ALL_CANCELLED -> Color(0x26F87171)
                            DayAttendanceState.COLLEGE_OFF_HOLIDAY -> Color(0x33475569)
                            DayAttendanceState.BUNKED -> Color(0x28EF4444)
                            DayAttendanceState.UNRECORDED -> if (isToday) colors.primaryAccent else colors.containerSurface
                        }

                        val dotColor = when (state) {
                            DayAttendanceState.FULL_PRESENT -> Color(0xFF22C55E)
                            DayAttendanceState.PARTIAL_CANCELLED -> Color(0xFFF59E0B)
                            DayAttendanceState.ALL_CANCELLED -> Color(0xFFF87171)
                            DayAttendanceState.COLLEGE_OFF_HOLIDAY -> CaliperTheme.colors.textSecondary
                            DayAttendanceState.BUNKED -> Color(0xFFEF4444)
                            DayAttendanceState.UNRECORDED -> Color.Transparent
                        }

                        val borderColor = when {
                            isToday -> colors.primaryAccent
                            state == DayAttendanceState.COLLEGE_OFF_HOLIDAY -> Color(0x4464748B)
                            dotColor != Color.Transparent -> dotColor.copy(alpha = 0.35f)
                            else -> CaliperTheme.colors.containerSurface
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
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    onDateSelected(cellDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Slash indicator for ALL_CANCELLED or COLLEGE_OFF_HOLIDAY
                            if (state == DayAttendanceState.ALL_CANCELLED || state == DayAttendanceState.COLLEGE_OFF_HOLIDAY) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = dotColor.copy(alpha = 0.45f),
                                        start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.82f),
                                        end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f),
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
                                    color = if (isToday) colors.onPrimaryAccent else colors.textPrimary,
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

/**
 * Interactive Date Inspector Bottom Sheet / Modal Dialog
 * "Day Summary • [Date]"
 */
@Composable
fun DaySummaryModal(
    date: LocalDate,
    viewModel: CampusViewModel,
    onDismiss: () -> Unit,
    onNavigateToToday: (String) -> Unit
) {
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
    val dayStatus = dayStatuses.find { it.date == dateStr }

    val dayState = evaluateDayAttendanceState(date, logs, holidays, dayStatuses)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CaliperTheme.colors.cardFill,
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
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Day Summary • ${date.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CaliperTheme.colors.textPrimary
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CaliperTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Summary Status Banner
                val (stateTitle, stateColor) = when (dayState) {
                    DayAttendanceState.FULL_PRESENT -> "100% Attended (Full Present)" to Color(0xFF22C55E)
                    DayAttendanceState.PARTIAL_CANCELLED -> "Partial Faculty Cancellation" to Color(0xFFF59E0B)
                    DayAttendanceState.ALL_CANCELLED -> "All Scheduled Classes Cancelled" to Color(0xFFF87171)
                    DayAttendanceState.COLLEGE_OFF_HOLIDAY -> (if (isSunday) "Sunday Break" else "Institutional Holiday (${holidayObj?.title ?: "Closed"})") to CaliperTheme.colors.textSecondary
                    DayAttendanceState.BUNKED -> "Missed Lectures (Bunked)" to Color(0xFFEF4444)
                    DayAttendanceState.UNRECORDED -> "No Period Attendance Recorded" to CaliperTheme.colors.textSecondary
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
                        color = CaliperTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "RECORDED LECTURES (${dayLogs.size}):",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CaliperTheme.colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLogs.forEach { log ->
                            val sub = subjects.find { it.id == log.subjectId }
                            val slot = allSlots.find { it.id == log.slotId }
                            val (statusText, statusColor) = when (log.status) {
                                "attended" -> (if (log.isProxy) "PROXY PRESENT" else "PRESENT") to Color(0xFF22C55E)
                                "bunked" -> "BUNKED" to Color(0xFFEF4444)
                                "cancelled_by_faculty" -> "CANCELLED BY FACULTY" to Color(0xFFF59E0B)
                                "college_off" -> "COLLEGE OFF" to CaliperTheme.colors.textSecondary
                                else -> log.status.uppercase() to Color(0xFF38BDF8)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CaliperTheme.colors.containerSurface)
                                    .border(0.5.dp, CaliperTheme.colors.divider, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub?.name ?: "Subject #${log.subjectId}",
                                        color = CaliperTheme.colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val timeSpan = if (slot != null) "${slot.startTime} - ${slot.endTime} • " else ""
                                    Text(
                                        text = "$timeSpan${sub?.code ?: ""} ${if (log.isExtraClass) "• Extra Class" else ""}",
                                        color = CaliperTheme.colors.textSecondary,
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8).copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF38BDF8)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Open in Today View →", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CaliperTheme.colors.textPrimary)
            }
        }
    )
}
