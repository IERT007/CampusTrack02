package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.DailyDayStatusEntity
import com.example.data.local.entity.ExtraSlotEntity
import com.example.data.local.entity.HolidayRangeEntity
import com.example.data.local.entity.RescheduleOverrideEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.theme.CaliperTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.max

/**
 * Dynamic Future-Slot Resolver fixing Search Bar Temporal Drift.
 *
 * 1. Checks today's remaining slots (regular + RescheduleOverrideEntity + ExtraSlotEntity)
 *    strictly where `startTime > nowDateTime.toLocalTime()`.
 * 2. If none remain today, scans forward day-by-day up to 14 days, skipping Sundays,
 *    declared holidays (HolidayRangeEntity & DailyDayStatusEntity), and dates outside
 *    semester boundaries (unless a date-specific ExtraSlotEntity exists), returning the
 *    exact next upcoming lecture e.g. "Next: Wednesday, 09:30 AM (LT-3)".
 */
fun resolveNextUpcomingSlot(
    subjectId: Long,
    nowDateTime: LocalDateTime,
    regularSlots: List<TimetableSlotEntity>,
    extraSlots: List<ExtraSlotEntity>,
    overrides: List<RescheduleOverrideEntity>,
    holidays: List<HolidayRangeEntity>,
    dayStatuses: List<DailyDayStatusEntity>,
    semesterStartDate: String,
    semesterEndDate: String
): String {
    val subjectRegularSlots = regularSlots.filter { it.subjectId == subjectId }
    val subjectExtraSlots = extraSlots.filter { it.subjectId == subjectId }
    if (subjectRegularSlots.isEmpty() && subjectExtraSlots.isEmpty()) {
        return "No timetable slot scheduled"
    }

    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val displayTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    val today = nowDateTime.toLocalDate()
    val nowTime = nowDateTime.toLocalTime()

    data class CandidateSlot(
        val startTime: LocalTime,
        val roomNo: String,
        val isExtra: Boolean
    )

    fun candidatesForDate(date: LocalDate): List<CandidateSlot> {
        val dateStr = date.format(isoFormatter)
        val dow = date.dayOfWeek.value // 1..7
        val isSunday = (dow == 7)
        val isHoliday = holidays.any { dateStr >= it.startDate && dateStr <= it.endDate }
        val dayStatus = dayStatuses.find { it.date == dateStr }
        val isOffDay = dayStatus?.leaveCategory in listOf("college_holiday", "strike")
        val isOutsideSemester = (dateStr < semesterStartDate || dateStr > semesterEndDate)

        val list = mutableListOf<CandidateSlot>()

        // Regular weekly slots (only on active working days inside semester)
        if (!isSunday && !isHoliday && !isOffDay && !isOutsideSemester) {
            subjectRegularSlots.filter { it.dayOfWeek == dow }.forEach { slot ->
                val override = overrides.find { it.slotId == slot.id && it.date == dateStr }
                val effectiveStart = override?.newStartTime ?: slot.startTime
                val effectiveRoom = override?.newRoomNo ?: slot.roomNo
                try {
                    list.add(CandidateSlot(LocalTime.parse(effectiveStart), effectiveRoom, isExtra = false))
                } catch (_: Exception) {}
            }
        }

        // Date-specific extra slots (always considered for that exact date)
        subjectExtraSlots.filter { it.date == dateStr }.forEach { extra ->
            try {
                list.add(CandidateSlot(LocalTime.parse(extra.startTime), extra.roomNo, isExtra = true))
            } catch (_: Exception) {}
        }

        return list.sortedBy { it.startTime }
    }

    // Step 1: Remaining slots TODAY strictly after nowTime
    val todayRemaining = candidatesForDate(today).firstOrNull { it.startTime.isAfter(nowTime) }
    if (todayRemaining != null) {
        val timeText = todayRemaining.startTime.format(displayTimeFormatter)
        val extraTag = if (todayRemaining.isExtra) " • Extra" else ""
        return "Next: Today, $timeText (${todayRemaining.roomNo}$extraTag)"
    }

    // Step 2: Scan forward day-by-day up to 14 days
    for (offset in 1..14) {
        val targetDate = today.plusDays(offset.toLong())
        val nextSlot = candidatesForDate(targetDate).firstOrNull()
        if (nextSlot != null) {
            val dayLabel = when (offset) {
                1 -> "Tomorrow"
                in 2..6 -> targetDate.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.US)
                else -> "${targetDate.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.US)} (${targetDate.format(DateTimeFormatter.ofPattern("dd MMM"))})"
            }
            val timeText = nextSlot.startTime.format(displayTimeFormatter)
            val extraTag = if (nextSlot.isExtra) " • Extra" else ""
            return "Next: $dayLabel, $timeText (${nextSlot.roomNo}$extraTag)"
        }
    }

    return "No upcoming lecture in next 14 days"
}

@Composable
fun MainTopBar(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors

    val subjects by viewModel.subjects.collectAsState()
    val statsList by viewModel.subjectStats.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val extraSlots by viewModel.extraSlots.collectAsState()
    val rescheduleOverrides by viewModel.rescheduleOverrides.collectAsState()
    val holidayRanges by viewModel.holidayRanges.collectAsState()
    val dayStatuses by viewModel.dayStatuses.collectAsState()
    val semesterStartDate by viewModel.semesterStartDate.collectAsState()
    val semesterEndDate by viewModel.semesterEndDate.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedSubjectForTelemetry by remember { mutableStateOf<SubjectEntity?>(null) }

    val matchingSubjects = remember(searchQuery, subjects, allSlots) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim()
            subjects.filter { sub ->
                sub.name.contains(q, ignoreCase = true) ||
                        sub.code.contains(q, ignoreCase = true) ||
                        sub.facultyName.contains(q, ignoreCase = true) ||
                        allSlots.any { slot -> slot.subjectId == sub.id && slot.roomNo.contains(q, ignoreCase = true) }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.widthIn(min = 90.dp, max = 110.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Caliper",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = "IERT Prayagraj",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.85f))
                        .border(0.5.dp, colors.border, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.primaryAccent),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search subject, room...",
                                        style = TextStyle(
                                            color = colors.textSecondary.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            CaliperSoundManager.playSnap(view)
                            CaliperHapticManager.tick(context)
                            onNavigateToSettings()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "System Settings",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = searchQuery.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.96f))
                        .border(0.5.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    if (matchingSubjects.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No academic record matching \"$searchQuery\"",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "MATCHING SUBJECTS & LABS (${matchingSubjects.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            matchingSubjects.take(5).forEach { sub ->
                                val stats = statsList.find { it.subject.id == sub.id }
                                val subSlots = allSlots.filter { it.subjectId == sub.id }
                                val roomLabel = subSlots.firstOrNull()?.roomNo ?: "LT-TBD"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            CaliperSoundManager.playSnap(view)
                                            CaliperHapticManager.tick(context)
                                            selectedSubjectForTelemetry = sub
                                            viewModel.setSearchQuery("")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if ((stats?.percentage ?: 100.0) >= 75.0) colors.safeGreen
                                                    else colors.dangerRed
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${sub.code} — ${sub.name}",
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${sub.facultyName} • Room: $roomLabel",
                                                color = colors.textSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    stats?.let { s ->
                                        val pctColor = if (s.percentage >= 75.0) colors.safeGreen else colors.dangerRed
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(pctColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${String.format("%.1f", s.percentage)}%",
                                                color = pctColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSubjectForTelemetry?.let { subject ->
        val stats = statsList.find { it.subject.id == subject.id }
        val slots = allSlots.filter { it.subjectId == subject.id }
        SubjectTelemetryDialog(
            subject = subject,
            stats = stats,
            slots = slots,
            extraSlots = extraSlots,
            overrides = rescheduleOverrides,
            holidays = holidayRanges,
            dayStatuses = dayStatuses,
            semesterStartDate = semesterStartDate,
            semesterEndDate = semesterEndDate,
            onDismiss = { selectedSubjectForTelemetry = null }
        )
    }
}

@Composable
fun SubjectTelemetryDialog(
    subject: SubjectEntity,
    stats: SubjectAttendanceStats?,
    slots: List<TimetableSlotEntity>,
    extraSlots: List<ExtraSlotEntity> = emptyList(),
    overrides: List<RescheduleOverrideEntity> = emptyList(),
    holidays: List<HolidayRangeEntity> = emptyList(),
    dayStatuses: List<DailyDayStatusEntity> = emptyList(),
    semesterStartDate: String = "2026-07-15",
    semesterEndDate: String = "2026-12-15",
    onDismiss: () -> Unit
) {
    val colors = CaliperTheme.colors

    // Dynamically resolved using live LocalDateTime.now() so past today slots never drift as "Next"
    val nextSlotInfo = resolveNextUpcomingSlot(
        subjectId = subject.id,
        nowDateTime = LocalDateTime.now(),
        regularSlots = slots,
        extraSlots = extraSlots,
        overrides = overrides,
        holidays = holidays,
        dayStatuses = dayStatuses,
        semesterStartDate = semesterStartDate,
        semesterEndDate = semesterEndDate
    )

    val attended = stats?.attended ?: 0
    val totalConducted = stats?.totalConducted ?: 0
    val bunked = max(0, totalConducted - attended)
    val cancelled = stats?.facultyCancelled ?: 0
    val percentage = stats?.percentage ?: 100.0
    val bunksAvailable = stats?.bunksAvailable ?: 0
    val classesNeeded = stats?.classesNeeded ?: 0
    val isSafe = percentage >= 75.0
    val statusColor = if (isSafe) colors.safeGreen else colors.dangerRed

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                borderColors = listOf(statusColor.copy(alpha = 0.6f), Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${subject.code} Telemetry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAccent
                            )
                            Text(
                                text = subject.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${subject.facultyName} • ${subject.type.uppercase()}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(0.5.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.primaryAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "NEXT SCHEDULED LECTURE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Text(
                                text = nextSlotInfo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ACADEMIC TALLY & LECTURE LOGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TallyStatBox("Attended", "$attended", colors.safeGreen, Modifier.weight(1f))
                        TallyStatBox("Bunked", "$bunked", colors.dangerRed, Modifier.weight(1f))
                        TallyStatBox("Cancelled", "$cancelled", colors.warnAmber, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .border(0.5.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSafe) "SAFE-ZONE MAINTAINED" else "DEBAR CRITICAL RISK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSafe) {
                                    "Bunk Buffer Remaining: $bunksAvailable lectures"
                                } else {
                                    "Must Attend: $classesNeeded consecutive classes to reach 75%"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Formula: B = ⌊(Attended - 0.75×Total) / 0.75⌋",
                                fontSize = 9.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Text(
                            text = "${String.format("%.1f", percentage)}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Acknowledge Telemetry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TallyStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(0.5.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = label, color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MainTopBar(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.widthIn(min = 90.dp, max = 110.dp)) {
            Text(
                text = "Caliper",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            )
            Text(
                text = "IERT Prayagraj",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
            )
        }

        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "System Settings",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MainTopBar(
    onOpenSettings: () -> Unit
) {
    MainTopBar(onNavigateToSettings = onOpenSettings)
}
