package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.notification.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.CaliperThemePreset
import com.example.ui.theme.tokensForPreset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Caliper v2.0 Settings & Controls Screen
 * - Live 5-Preset Centralized Theme Engine Picker (DataStore-persisted)
 * - Soft Ear-Soothing Acoustic Feedback & Vernier Haptics Hub
 * - Academic Semester Timeline Hub
 * - Notifications & System Alarms Hub
 * - Data Auto-Vault & SAF Backup Hub
 * - Curriculum & Timetable Presets Hub
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    val scrollState = rememberScrollState()

    val currentThemePresetKey by viewModel.themePreset.collectAsState()
    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val is24HourFormat by viewModel.is24HourFormat.collectAsState()
    val hasAutoVault by viewModel.hasAutoVault.collectAsState()
    val semesterStartDate by viewModel.semesterStartDate.collectAsState()
    val semesterEndDate by viewModel.semesterEndDate.collectAsState()

    var showResetConfirmation by remember { mutableStateOf(false) }
    var showWipeConfirmation by remember { mutableStateOf(false) }

    BackHandler {
        CaliperSoundManager.playSnap(view)
        CaliperHapticManager.tick(context)
        onNavigateBack()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToFile(context, uri) { success ->
                if (success) {
                    CaliperSoundManager.playSuccess()
                    CaliperHapticManager.successClick(context)
                    Toast.makeText(context, "Backup exported successfully to JSON!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromFile(context, uri) { success ->
                if (success) {
                    CaliperSoundManager.playSuccess()
                    CaliperHapticManager.successClick(context)
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    CaliperSoundManager.playThud()
                    Toast.makeText(context, "Invalid JSON backup file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            // Top Toolbar Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        CaliperSoundManager.playSnap(view)
                        CaliperHapticManager.tick(context)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .border(0.5.dp, colors.border, CircleShape)
                        .testTag("settings_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Caliper Settings & Controls",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tactical Academic OS • System Preferences",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION 1: Live Theme Preset Engine (5 Presets)
                SettingsSectionHeader("THEME ENGINE & COLOR PRESETS", Icons.Default.Palette, colors.primaryAccent)

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_preset_picker_card"),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.4f), Color.Transparent)
                ) {
                    Text(
                        text = "LIVE THEME PRESET (INSTANT RE-THEME)",
                        color = colors.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CaliperThemePreset.entries.forEach { preset ->
                            val previewTokens = remember(preset) { tokensForPreset(preset) }
                            val isSelected = (currentThemePresetKey == preset.key)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) colors.primaryAccent.copy(alpha = 0.14f)
                                        else colors.surface
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) colors.primaryAccent else colors.border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        CaliperSoundManager.playSnap(view)
                                        viewModel.setThemePreset(preset.key)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("theme_preset_${preset.key}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Live Palette Preview Capsule
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(previewTokens.background)
                                            .border(1.dp, previewTokens.border, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(previewTokens.primaryAccent)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(previewTokens.safeGreen)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = preset.displayName,
                                            color = if (isSelected) colors.primaryAccent else colors.textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            color = colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = colors.onPrimaryAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        title = "24-Hour Military Time Format",
                        subtitle = "Display timetable as 13:00 - 14:00 instead of 01:00 PM",
                        checked = is24HourFormat,
                        onCheckedChange = { viewModel.set24HourFormat(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Statutory Attendance Threshold", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("IERT Prayagraj Academic Ordinance Rule", color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.safeGreen.copy(alpha = 0.15f))
                                .border(0.5.dp, colors.safeGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("75.0%", color = colors.safeGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // SECTION 2: Soft Acoustic Engine & Vernier Haptics Hub
                SettingsSectionHeader("SOFT ACOUSTIC & HAPTIC ENGINE", Icons.Default.VolumeUp, colors.primaryAccent)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.35f), Color.Transparent)
                ) {
                    SettingsToggleRow(
                        title = "Soft Micro-Click Audio",
                        subtitle = "Damped wooden/glass impulses & native UI clicks (zero harsh beeps)",
                        checked = isAudioEnabled,
                        onCheckedChange = { viewModel.setAudioEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsToggleRow(
                        title = "Vernier Micro-Haptic Pulses",
                        subtitle = "Precision physical clicks on slot toggles and swipes",
                        checked = isHapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Audition Soft Feedback:",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, colors.primaryAccent.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Click", fontSize = 10.sp, color = colors.primaryAccent)
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playSuccess()
                                CaliperHapticManager.successClick(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, colors.safeGreen.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Present", fontSize = 10.sp, color = colors.safeGreen)
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playThud()
                                CaliperHapticManager.bunkDoubleTap(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, colors.dangerRed.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bunk", fontSize = 10.sp, color = colors.dangerRed)
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playPip()
                                CaliperHapticManager.tick(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, colors.warnAmber.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pip", fontSize = 10.sp, color = colors.warnAmber)
                        }
                    }
                }

                // SECTION 3: Academic Semester Timeline
                SettingsSectionHeader("ACADEMIC SEMESTER TIMELINE", Icons.Default.DateRange, colors.primaryAccent)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.35f), Color.Transparent)
                ) {
                    Text(
                        text = "TERM COMMENCEMENT & CONCLUSION",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SemesterDatePickerRow(
                        title = "Semester Start Date",
                        subtitle = "Curriculum begins on this date",
                        isoDate = semesterStartDate,
                        onDateSelected = { newStart ->
                            viewModel.setSemesterDates(newStart, semesterEndDate)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    SemesterDatePickerRow(
                        title = "Semester End Date",
                        subtitle = "Term ends; triggers preparation leave state",
                        isoDate = semesterEndDate,
                        onDateSelected = { newEnd ->
                            viewModel.setSemesterDates(semesterStartDate, newEnd)
                        }
                    )
                }

                // SECTION 4: System Notifications & Background Alarms Hub
                SettingsSectionHeader("NOTIFICATIONS & ALARM ENGINE", Icons.Default.NotificationsActive, colors.safeGreen)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.safeGreen.copy(alpha = 0.35f), Color.Transparent)
                ) {
                    NotificationSettingItem(
                        icon = Icons.Default.WbSunny,
                        title = "07:30 AM Morning Briefing",
                        description = "Formatted numbered summary of today's classes & room numbers"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    NotificationSettingItem(
                        icon = Icons.Default.Timer,
                        title = "10-Minute Pre-Lecture Heads-Up",
                        description = "Exact alarm heads-up with subject name and classroom before slot starts"
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            CaliperSoundManager.playPip()
                            CaliperHapticManager.tick(context)
                            NotificationHelper.showPreLectureAlert(
                                context,
                                subjectName = "Thermal Engg (ME-302)",
                                roomNo = "204",
                                time = "in 10 mins"
                            )
                            Toast.makeText(context, "Pre-lecture notification ping dispatched!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.safeGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, colors.safeGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Pre-Lecture Ping", color = colors.safeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // SECTION 5: Data Auto-Vault & Cloud-Free Storage Hub
                SettingsSectionHeader("DATA AUTO-VAULT & BACKUP", Icons.Default.CloudSync, colors.primaryAccent)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.primaryAccent.copy(alpha = 0.35f), Color.Transparent)
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
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (hasAutoVault) colors.safeGreen else colors.offSlate)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Silent Background Auto-Vault", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "Serializes to context.filesDir/caliper_vault_snapshot.json on every commit",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.triggerAutoBackup()
                                CaliperSoundManager.playSuccess()
                                CaliperHapticManager.successClick(context)
                                Toast.makeText(context, "Auto-Vault snapshot captured!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.18f)),
                            border = BorderStroke(0.5.dp, colors.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Vault Now", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Storage Access Framework (SAF) File Operations:", color = colors.textSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                exportLauncher.launch("caliper_academic_vault_${System.currentTimeMillis()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, colors.primaryAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export JSON", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap(view)
                                CaliperHapticManager.tick(context)
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.safeGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, colors.safeGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore JSON", color = colors.safeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION 6: Curriculum & Semester Timetable Engine
                SettingsSectionHeader("CURRICULUM & ENGINE PRESETS", Icons.Default.School, colors.dangerRed)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(colors.dangerRed.copy(alpha = 0.35f), Color.Transparent)
                ) {
                    Column {
                        Text(
                            text = "IERT Prayagraj 3rd Semester",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mechanical & Tool Engineering (7 Core Subjects)",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• ME-301 Manufacturing Process (Theory)\n• ME-302 Thermal Engineering (Theory)\n• ME-303W Workshop Practice (Workshop)\n• ED-301 M/C Drawing 1 (Drafting/Lab)\n• CS-301 Computer Lecture (Theory)\n• ME-304 Material Science (Theory)\n• ME-305L Computer / Thermal Lab (Lab)",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showResetConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.warnAmber.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, colors.warnAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = colors.warnAmber, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reload IERT", color = colors.warnAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showWipeConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dangerRed.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, colors.dangerRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = colors.dangerRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe Records", color = colors.dangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CALIPER ACADEMIC OS • V2.0", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("IERT Mechanical & Tool Engineering • Offline-First", color = colors.textMuted, fontSize = 9.sp)
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Reload IERT Curriculum?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will re-initialize the 7 official IERT 3rd semester subjects and recurring weekly slots with a clean slate.", color = colors.textSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.preloadDefaultData()
                        showResetConfirmation = false
                        CaliperSoundManager.playSuccess()
                        CaliperHapticManager.successClick(context)
                        Toast.makeText(context, "IERT Curriculum Reloaded!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.warnAmber)
                ) {
                    Text("Reload", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmation = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Wipe Attendance Logs?", color = colors.dangerRed, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all marked attendance logs and reset back to 0%? Timetable slots will remain intact.", color = colors.textSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAttendanceLogs()
                        showWipeConfirmation = false
                        CaliperSoundManager.playThud()
                        CaliperHapticManager.bunkDoubleTap(context)
                        Toast.makeText(context, "Attendance records cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.dangerRed)
                ) {
                    Text("Wipe", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmation = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun SettingsToggleRow(
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
            Text(title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.textSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimaryAccent,
                checkedTrackColor = colors.primaryAccent,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surface
            )
        )
    }
}

@Composable
private fun NotificationSettingItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.safeGreen.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = colors.textSecondary, fontSize = 10.sp)
        }
        Text("Active", color = colors.safeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SemesterDatePickerRow(
    title: String,
    subtitle: String,
    isoDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors
    val parsedDate = remember(isoDate) {
        try {
            LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
    val formattedDisplay = remember(parsedDate) {
        parsedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                CaliperSoundManager.playSnap(view)
                CaliperHapticManager.tick(context)
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selected = LocalDate.of(year, month + 1, dayOfMonth)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onDateSelected(selected)
                        CaliperSoundManager.playSuccess()
                        CaliperHapticManager.successClick(context)
                        Toast.makeText(context, "$title updated: $selected", Toast.LENGTH_SHORT).show()
                    },
                    parsedDate.year,
                    parsedDate.monthValue - 1,
                    parsedDate.dayOfMonth
                ).show()
            }
            .background(colors.surface)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.textSecondary, fontSize = 11.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primaryAccent.copy(alpha = 0.15f))
                .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(formattedDisplay, color = colors.primaryAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
