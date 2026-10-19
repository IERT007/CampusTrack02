package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.dialogs.HolidayManagerDialog
import com.example.ui.dialogs.WeeklyTimetableEditorDialog
import com.example.ui.screens.today.SemesterDatesDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val colors = CaliperTheme.colors

    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val is24HourFormat by viewModel.is24HourFormat.collectAsState()
    val semesterStart by viewModel.semesterStartDate.collectAsState()
    val semesterEnd by viewModel.semesterEndDate.collectAsState()
    val slots by viewModel.slots.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val holidays by viewModel.holidayRanges.collectAsState()

    var showTimetableEditor by remember { mutableStateOf(false) }
    var showHolidayManager by remember { mutableStateOf(false) }
    var showSemesterDatesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "settings_top_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CALIPER OS // SETTINGS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = "HARDWARE FEEDBACK & ACADEMIC ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            color = colors.textSecondary
                        )
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close Settings",
                    onClick = {
                        CaliperHapticManager.tick(context)
                        onClose()
                    },
                    modifier = Modifier.testTag("close_settings_button")
                )
            }
        }

        // Section 1: Tactile & Audio Engine
        item(key = "section_tactile") {
            SettingsSectionHeader(
                title = "LIVING FLUID & HAPTIC ENGINE",
                icon = Icons.Default.Tune,
                accentColor = colors.scheduledLecture
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsToggleRow(
                        title = "Tactile Haptic Feedback",
                        subtitle = "Crisp mechanical clicks on touch press & actions",
                        checked = isHapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )
                    SettingsToggleRow(
                        title = "Acoustic Confirmation",
                        subtitle = "Subtle audio feedback on attendance logging",
                        checked = isAudioEnabled,
                        onCheckedChange = { viewModel.setAudioEnabled(it) }
                    )
                    SettingsToggleRow(
                        title = "24-Hour Telemetry Clock",
                        subtitle = "Display timetable slots in military 24h format",
                        checked = is24HourFormat,
                        onCheckedChange = { viewModel.set24HourFormat(it) }
                    )
                }
            }
        }

        // Section 2: Academic Timetable & Calendar
        item(key = "section_academic") {
            SettingsSectionHeader(
                title = "ACADEMIC SCHEDULE & CALENDAR",
                icon = Icons.Default.CalendarMonth,
                accentColor = colors.safeZone
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SemesterDatePickerRow(
                        label = "SEMESTER WINDOW",
                        startDate = semesterStart,
                        endDate = semesterEnd,
                        onClick = { showSemesterDatesDialog = true }
                    )

                    ActionRowButton(
                        title = "Weekly Timetable Matrix",
                        subtitle = "${slots.size} active slots across Mon-Sat",
                        icon = Icons.Default.Event,
                        accent = colors.scheduledLecture,
                        onClick = { showTimetableEditor = true }
                    )

                    ActionRowButton(
                        title = "Gazetted Holidays Manager",
                        subtitle = "${holidays.size} holiday windows configured",
                        icon = Icons.Default.CalendarMonth,
                        accent = colors.deadlineWarning,
                        onClick = { showHolidayManager = true }
                    )
                }
            }
        }

        // Section 3: Notifications
        item(key = "section_notifications") {
            SettingsSectionHeader(
                title = "ALARM & LECTURE ALERTS",
                icon = Icons.Default.Notifications,
                accentColor = colors.deadlineWarning
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NotificationSettingItem(
                        icon = Icons.Default.Notifications,
                        title = "10-Min Pre-Lecture Alerts",
                        subtitle = "Scheduled for all ${slots.size} weekly slots",
                        enabled = true
                    )
                }
            }
        }

        // Section 4: Data & Vault
        item(key = "section_vault") {
            SettingsSectionHeader(
                title = "DATABASE & VAULT MANAGEMENT",
                icon = Icons.Default.Storage,
                accentColor = colors.bunkDanger
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionRowButton(
                        title = "Reload IERT CSE Sem-2 Defaults",
                        subtitle = "Restore official subjects, faculty, and timetable",
                        icon = Icons.Default.Refresh,
                        accent = colors.safeZone,
                        onClick = {
                            viewModel.preloadDefaultIertData()
                            Toast.makeText(context, "IERT CSE defaults restored", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ActionRowButton(
                        title = "Clear Attendance Logs",
                        subtitle = "Reset attendance logs while keeping subjects",
                        icon = Icons.Default.DeleteForever,
                        accent = colors.bunkDanger,
                        onClick = {
                            viewModel.clearAttendanceLogs()
                            Toast.makeText(context, "Attendance logs cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showTimetableEditor) {
        WeeklyTimetableEditorDialog(
            slots = slots,
            subjects = subjects,
            onDismiss = { showTimetableEditor = false },
            onAddSlot = { viewModel.saveSlot(it) },
            onUpdateSlot = { viewModel.updateTimetableSlot(it) },
            onDeleteSlot = { viewModel.deleteSlot(it) }
        )
    }

    if (showHolidayManager) {
        HolidayManagerDialog(
            holidays = holidays,
            onDismiss = { showHolidayManager = false },
            onAddHoliday = { viewModel.saveHolidayRange(it) },
            onDeleteHoliday = { viewModel.deleteHolidayRange(it) }
        )
    }

    if (showSemesterDatesDialog) {
        SemesterDatesDialog(
            startDate = semesterStart,
            endDate = semesterEnd,
            onDismiss = { showSemesterDatesDialog = false },
            onSave = { start, end ->
                viewModel.setSemesterDates(start, end)
                showSemesterDatesDialog = false
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = accentColor
            )
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SemesterDatePickerRow(
    label: String,
    startDate: String,
    endDate: String,
    onClick: () -> Unit
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fluidTouchFeedback(onClick = onClick)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    color = colors.textSecondary
                )
            )
            Text(
                text = "$startDate → $endDate",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        }
    }
}

@Composable
fun NotificationSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.safeZone else colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }
    }
}

@Composable
private fun ActionRowButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fluidTouchFeedback(onClick = onClick)
            .clip(shape)
            .background(accent.copy(alpha = 0.1f), shape)
            .border(1.dp, accent.copy(alpha = 0.3f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }
    }
}
