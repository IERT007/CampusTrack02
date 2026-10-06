package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.theme.CaliperTheme
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Centered Google-Style Integrated Glass Search Bar in Top Bar
 * 1. Left (~100.dp): Brand "Caliper", Subtitle "IERT Prayagraj"
 * 2. Center (Flexible Weight): 42.dp Frosted Glass Search Capsule with native BasicTextField
 * 3. Right (~40.dp): Embedded 20.dp Settings Icon (navigates smoothly to SettingsScreen)
 *
 * Reactive Autocomplete & Precision Telemetry Dropdown for matches.
 */
@Composable
fun MainTopBar(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onOpenAiAssistant: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    val subjects by viewModel.subjects.collectAsState()
    val statsList by viewModel.subjectStats.collectAsState()
    val allSlots by viewModel.slots.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedSubjectForTelemetry by remember { mutableStateOf<SubjectEntity?>(null) }

    // Real-time matched subjects and room slots
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
            // Unified Top Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Left Component (Width: ~100.dp)
                Column(
                    modifier = Modifier
                        .widthIn(min = 90.dp, max = 110.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Caliper",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "IERT Prayagraj",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Center Component (Flexible Weight / FillRemainingWidth) - Frosted Glass Search Capsule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
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
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.primaryAccent),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search subject, room...",
                                        style = TextStyle(
                                            color = Color(0xFF64748B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Clear Button or AI Sparkle Button
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else if (onOpenAiAssistant != null) {
                            IconButton(
                                onClick = {
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    onOpenAiAssistant()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "AI Copilot",
                                    tint = colors.primaryAccent.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. Right Component (Width: ~40.dp) - Embedded 20.dp Settings Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            CaliperSoundManager.playSnap()
                            CaliperHapticManager.tick(context)
                            onNavigateToSettings()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "System Settings",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Floating Frosted Glass Autocomplete Dropdown Modal
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
                        .background(Color(0xFF0F141C).copy(alpha = 0.95f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "MATCHING SUBJECTS & LABS (${matchingSubjects.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
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
                                            CaliperSoundManager.playSnap()
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
                                                    if ((stats?.percentage ?: 100.0) >= 75.0) colors.safeZone
                                                    else colors.bunkDanger
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${sub.code} — ${sub.name}",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${sub.facultyName} • Room: $roomLabel",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Attendance % chip
                                    stats?.let { s ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (s.percentage >= 75.0) colors.safeZone.copy(alpha = 0.15f)
                                                    else colors.bunkDanger.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${String.format("%.1f", s.percentage)}%",
                                                color = if (s.percentage >= 75.0) colors.safeZone else colors.bunkDanger,
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

    // Precision Telemetry Pop-up Card when a subject is tapped
    selectedSubjectForTelemetry?.let { subject ->
        val stats = statsList.find { it.subject.id == subject.id }
        val slots = allSlots.filter { it.subjectId == subject.id }
        SubjectTelemetryDialog(
            subject = subject,
            stats = stats,
            slots = slots,
            onDismiss = { selectedSubjectForTelemetry = null }
        )
    }
}

/**
 * Precision Telemetry Pop-Up Card
 * 1. Next Schedule: Day, Time (e.g., "Wednesday 11:00 AM - 12:30 PM"), and Room No.
 * 2. Academic Tally: Exact count of Attended, Bunked, and Cancelled lectures.
 * 3. Cutoff Status: Live Safe-Zone % and exact bunk buffer remaining B = floor((A - 0.75C)/0.75).
 */
@Composable
fun SubjectTelemetryDialog(
    subject: SubjectEntity,
    stats: SubjectAttendanceStats?,
    slots: List<TimetableSlotEntity>,
    onDismiss: () -> Unit
) {
    val colors = CaliperTheme.colors

    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val todayDayOfWeek = remember { LocalDate.now().dayOfWeek.value } // 1 (Mon) .. 7 (Sun)

    // Calculate next schedule
    val nextSlotInfo = remember(slots, todayDayOfWeek) {
        if (slots.isEmpty()) {
            "No timetable slot scheduled"
        } else {
            // Find slot on or after today
            val upcoming = slots.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
                .firstOrNull { it.dayOfWeek >= todayDayOfWeek }
                ?: slots.first()
            val dayName = dayNames.getOrElse(upcoming.dayOfWeek - 1) { "Day ${upcoming.dayOfWeek}" }
            "$dayName ${upcoming.startTime} - ${upcoming.endTime} (${upcoming.roomNo})"
        }
    }

    val attended = stats?.attended ?: 0
    val totalConducted = stats?.totalConducted ?: 0
    val bunked = max(0, totalConducted - attended)
    val cancelled = stats?.facultyCancelled ?: 0
    val percentage = stats?.percentage ?: 100.0
    val bunksAvailable = stats?.bunksAvailable ?: 0
    val classesNeeded = stats?.classesNeeded ?: 0
    val isSafe = percentage >= 75.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                borderColors = listOf(
                    if (isSafe) colors.safeZone.copy(alpha = 0.6f) else colors.bunkDanger.copy(alpha = 0.6f),
                    colors.cardBorderBottom
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header
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
                                color = Color.White
                            )
                            Text(
                                text = "${subject.facultyName} • ${subject.type.uppercase()}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Next Schedule Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.scheduledLecture.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = colors.scheduledLecture,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "NEXT SCHEDULED LECTURE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = nextSlotInfo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Academic Tally: Attended, Bunked, Cancelled
                    Text(
                        text = "ACADEMIC TALLY & LECTURE LOGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TallyStatBox(
                            label = "Attended",
                            value = "$attended",
                            color = colors.safeZone,
                            modifier = Modifier.weight(1f)
                        )
                        TallyStatBox(
                            label = "Bunked",
                            value = "$bunked",
                            color = colors.bunkDanger,
                            modifier = Modifier.weight(1f)
                        )
                        TallyStatBox(
                            label = "Cancelled",
                            value = "$cancelled",
                            color = colors.scheduledLecture,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Cutoff Status: Live Safe-Zone % and exact bunk buffer remaining B = floor((A - 0.75C)/0.75)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSafe) colors.safeZone.copy(alpha = 0.12f)
                                else colors.bunkDanger.copy(alpha = 0.12f)
                            )
                            .border(
                                0.5.dp,
                                if (isSafe) colors.safeZone.copy(alpha = 0.35f)
                                else colors.bunkDanger.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = if (isSafe) colors.safeZone else colors.bunkDanger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSafe) "SAFE-ZONE MAINTAINED" else "DEBAR CRITICAL RISK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSafe) colors.safeZone else colors.bunkDanger
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
                                color = Color.White
                            )
                            Text(
                                text = "Formula: B = ⌊(Attended - 0.75×Total) / 0.75⌋",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Text(
                            text = "${String.format("%.1f", percentage)}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSafe) colors.safeZone else colors.bunkDanger
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Backwards-compatible overload for existing calls
@Composable
fun MainTopBar(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // When called without ViewModel, provide standard aligned top bar
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
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
            Text(
                text = "IERT Prayagraj",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
            )
        }

        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "System Settings",
                tint = Color(0xFFE2E8F0),
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
