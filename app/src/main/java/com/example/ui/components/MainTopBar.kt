package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.audio.CaliperHapticManager
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback
import java.util.Locale

@Composable
fun MainTopBar(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val summary by viewModel.globalSummary.collectAsState()
    val streak by viewModel.collegeStreak.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val subjectStats by viewModel.subjectStats.collectAsState()
    val slots by viewModel.slots.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedSubjectForDialog by remember { mutableStateOf<SubjectEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
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
                            .background(
                                if (summary.overallPercentage >= 75.0) colors.safeZone else colors.bunkDanger
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALIPER OS // IERT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", summary.overallPercentage),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colors.textPrimary
                        )
                    )
                    if (streak > 0) {
                        Spacer(modifier = Modifier.width(10.dp))
                        val pillShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(colors.streakGold.copy(alpha = 0.14f), pillShape)
                                .border(1.dp, colors.streakGold.copy(alpha = 0.35f), pillShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${streak}D STREAK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = colors.streakGold
                                )
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassIconButton(
                    icon = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search Subjects",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.testTag("top_bar_search_button")
                )
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        onOpenSettings()
                    },
                    modifier = Modifier.testTag("top_bar_settings_button")
                )
            }
        }

        AnimatedVisibility(visible = isSearchExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search subject, code, faculty, room...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )

                if (searchQuery.isNotBlank()) {
                    val matches = subjects.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.code.contains(searchQuery, ignoreCase = true) ||
                            it.facultyName.contains(searchQuery, ignoreCase = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    matches.take(4).forEach { subj ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                CaliperHapticManager.tick(context)
                                selectedSubjectForDialog = subj
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${subj.code} • ${subj.name}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = SpaceGroteskFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = "${subj.facultyName} • ${subj.roomNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSubjectForDialog?.let { subject ->
        val stats = subjectStats.firstOrNull { it.subject.id == subject.id }
        val subjectSlots = slots.filter { it.subjectId == subject.id }
        SubjectTelemetryDialog(
            subject = subject,
            stats = stats,
            slots = subjectSlots,
            onDismiss = { selectedSubjectForDialog = null }
        )
    }
}

@Composable
fun MainTopBar(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CALIPER OS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )
        GlassIconButton(
            icon = Icons.Default.Tune,
            contentDescription = "Settings",
            onClick = {
                CaliperHapticManager.tick(context)
                onOpenSettings()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTelemetryDialog(
    subject: SubjectEntity,
    stats: SubjectAttendanceStats?,
    slots: List<TimetableSlotEntity>,
    onDismiss: () -> Unit
) {
    val colors = CaliperTheme.colors
    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = subject.code,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = colors.scheduledLecture
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${subject.facultyName} • ${subject.roomNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (stats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TallyStatBox(
                            label = "ATTENDED",
                            value = "${stats.attended}/${stats.totalConducted}",
                            accentColor = colors.safeZone,
                            modifier = Modifier.weight(1f)
                        )
                        TallyStatBox(
                            label = "CURRENT",
                            value = String.format(Locale.US, "%.1f%%", stats.percentage),
                            accentColor = if (stats.percentage >= 75.0) colors.safeZone else colors.bunkDanger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.scheduledLecture.copy(alpha = 0.18f))
                        .fluidTouchFeedback(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE TELEMETRY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TallyStatBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f), shape)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)),
                shape
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = colors.textSecondary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        )
    }
}
