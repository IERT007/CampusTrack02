package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.notification.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

/**
 * Dedicated Full-Page Slide-In Settings Screen
 * Features 5 modular card sections:
 * 1. Audio & Vernier Haptics Hub
 * 2. Notifications & System Alarms Hub
 * 3. Display & Time Formatting Hub
 * 4. Data Auto-Vault & SAF Backup Hub
 * 5. Curriculum & Timetable Presets Hub
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val is24HourFormat by viewModel.is24HourFormat.collectAsState()
    val isMonochromeMode by viewModel.isMonochromeMode.collectAsState()
    val hasAutoVault by viewModel.hasAutoVault.collectAsState()

    var showResetConfirmation by remember { mutableStateOf(false) }
    var showWipeConfirmation by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back
    BackHandler {
        CaliperSoundManager.playSnap()
        CaliperHapticManager.tick(context)
        onNavigateBack()
    }

    // SAF Launchers for Export and Import
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
        containerColor = AmoledBackground,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmoledBackground)
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
                        CaliperSoundManager.playSnap()
                        CaliperHapticManager.tick(context)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IceBlue
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Caliper Settings & Controls",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tactical Academic OS • System Preferences",
                        color = ArchitecturalTitanium,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION 1: Audio & Vernier Haptics Hub
                SettingsSectionHeader("AUDIO & VERNIER HAPTIC ENGINE", Icons.Default.VolumeUp, IceBlue)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(IceBlue.copy(alpha = 0.35f), GlassBorderBottom)
                ) {
                    SettingsToggleRow(
                        title = "Procedural Audio Synthesizer",
                        subtitle = "Hardware-synthesized tones for ticks, chimes, and alerts",
                        checked = isAudioEnabled,
                        onCheckedChange = { viewModel.setAudioEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsToggleRow(
                        title = "Vernier Micro-Haptic Pulses",
                        subtitle = "Precision physical clicks on slot toggles and swipes",
                        checked = isHapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Audition Procedural Tones:",
                        color = ArchitecturalTitanium,
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
                                CaliperSoundManager.playSnap()
                                CaliperHapticManager.tick(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, IceBlue.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tick", fontSize = 10.sp, color = IceBlue)
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playSuccess()
                                CaliperHapticManager.successClick(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, VelvetSageEmerald.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Chime", fontSize = 10.sp, color = VelvetSageEmerald)
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playThud()
                                CaliperHapticManager.bunkDoubleTap(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SoftCoralRose.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Thud", fontSize = 10.sp, color = SoftCoralRose)
                        }

                        OutlinedButton(
                            onClick = {
                                CaliperSoundManager.playPip()
                                CaliperHapticManager.tick(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MutedChampagneAmber.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pip", fontSize = 10.sp, color = MutedChampagneAmber)
                        }
                    }
                }

                // SECTION 2: System Notifications & Background Alarms Hub
                SettingsSectionHeader("NOTIFICATIONS & ALARM ENGINE", Icons.Default.NotificationsActive, VelvetSageEmerald)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(VelvetSageEmerald.copy(alpha = 0.35f), GlassBorderBottom)
                ) {
                    NotificationSettingItem(
                        icon = Icons.Default.WbSunny,
                        title = "07:30 AM Morning Briefing",
                        description = "Summary of today's IERT periods & scheduled lab sessions",
                        active = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    NotificationSettingItem(
                        icon = Icons.Default.Timer,
                        title = "10-Minute Pre-Lecture Heads-Up",
                        description = "Notification ping with classroom & room number before slot starts",
                        active = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    NotificationSettingItem(
                        icon = Icons.Default.AssignmentLate,
                        title = "24h & 2h Sheet & Lab Deadlines",
                        description = "Urgent alerts for Machine Drawing (ED-301) and Workshop jobs",
                        active = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            CaliperSoundManager.playPip()
                            CaliperHapticManager.tick(context)
                            NotificationHelper.sendNotification(
                                context,
                                id = 9999,
                                title = "Caliper OS • Pre-Lecture Heads-Up",
                                message = "Thermal Engg (ME-302) starts in 10 mins • Room 204"
                            )
                            Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetSageEmerald.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, VelvetSageEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = VelvetSageEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Notification Ping", color = VelvetSageEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // SECTION 3: Display & Dual-Theme Engine Hub
                SettingsSectionHeader("DISPLAY & DUAL-THEME ENGINE", Icons.Default.Palette, MutedChampagneAmber)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(MutedChampagneAmber.copy(alpha = 0.35f), GlassBorderBottom)
                ) {
                    Text(
                        text = "THEME SYSTEM & COLOR PSYCHOLOGY",
                        color = ArchitecturalTitanium,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsToggleRow(
                        title = "Monochrome Luxury Dark Mode",
                        subtitle = if (isMonochromeMode) "Pure Obsidian (#000000), crisp white glass & zero color noise" else "Disabled • Using Color Psychology semantic palette",
                        checked = isMonochromeMode,
                        onCheckedChange = {
                            CaliperSoundManager.playSnap()
                            CaliperHapticManager.tick(context)
                            viewModel.setMonochromeMode(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode visual summary card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        if (isMonochromeMode) {
                            Text(
                                text = "Active: Monochrome Luxury Glass • High-contrast crisp white typography, silver secondary accents, and translucent frosted glass cards. Perfect for late-night review without glare.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        } else {
                            Text(
                                text = "Active: Color Psychology Normal • Ergonomic Dark Slate surface (#0B0E14) with semantic signals: Green (Safe 75%), Blue (Lectures), Orange (Deadlines), Red (Bunk/Debar), and Gold (Streak).",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        title = "24-Hour Military Time Format",
                        subtitle = "Display timetable as 13:00 - 14:00 instead of 01:00 PM",
                        checked = is24HourFormat,
                        onCheckedChange = { viewModel.set24HourFormat(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Statutory Attendance Threshold", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("IERT Prayagraj Academic Ordinance Rule", color = ArchitecturalTitanium, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VelvetSageEmerald.copy(alpha = 0.15f))
                                .border(0.5.dp, VelvetSageEmerald, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("75.0%", color = VelvetSageEmerald, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // SECTION 4: Data Auto-Vault & Cloud-Free Storage Hub
                SettingsSectionHeader("DATA AUTO-VAULT & BACKUP", Icons.Default.CloudSync, SlateTeal)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(SlateTeal.copy(alpha = 0.35f), GlassBorderBottom)
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
                                        .background(if (hasAutoVault) VelvetSageEmerald else ArchitecturalTitanium)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Silent Background Auto-Vault", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "Serializes to context.filesDir/caliper_vault_snapshot.json on every commit",
                                color = ArchitecturalTitanium,
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
                            colors = ButtonDefaults.buttonColors(containerColor = SlateTeal.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SlateTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Vault Now", color = SlateTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Storage Access Framework (SAF) File Operations:", color = ArchitecturalTitanium, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap()
                                CaliperHapticManager.tick(context)
                                exportLauncher.launch("caliper_academic_vault_${System.currentTimeMillis()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IceBlue.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, IceBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = IceBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export JSON", color = IceBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                CaliperSoundManager.playSnap()
                                CaliperHapticManager.tick(context)
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetSageEmerald.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, VelvetSageEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = VelvetSageEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore JSON", color = VelvetSageEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION 5: Curriculum & Semester Timetable Engine
                SettingsSectionHeader("CURRICULUM & ENGINE PRESETS", Icons.Default.School, SoftCoralRose)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColors = listOf(SoftCoralRose.copy(alpha = 0.35f), GlassBorderBottom)
                ) {
                    Column {
                        Text(
                            text = "IERT Prayagraj 3rd Semester",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mechanical & Tool Engineering (7 Core Subjects)",
                            color = ArchitecturalTitanium,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• ME-301 Manufacturing Process (Theory)\n• ME-302 Thermal Engineering (Theory)\n• ME-303W Workshop Practice (Workshop)\n• ED-301 M/C Drawing 1 (Drafting/Lab)\n• CS-301 Computer Lecture (Theory)\n• ME-304 Material Science (Theory)\n• ME-305L Computer / Thermal Lab (Lab)",
                        color = TextSecondary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = MutedChampagneAmber.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MutedChampagneAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MutedChampagneAmber, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reload IERT", color = MutedChampagneAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showWipeConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCoralRose.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SoftCoralRose),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = SoftCoralRose, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe Records", color = SoftCoralRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // App Signature Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CALIPER ACADEMIC OS • V2.0", color = ArchitecturalTitanium, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("IERT Mechanical & Tool Engineering • Offline-First", color = TextMuted, fontSize = 9.sp)
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = Color(0xFF10141D),
            title = { Text("Reload IERT Curriculum?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will re-initialize the 7 official IERT 3rd semester subjects and recurring weekly slots with a clean slate.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.preloadDefaultData()
                        showResetConfirmation = false
                        CaliperSoundManager.playSuccess()
                        CaliperHapticManager.successClick(context)
                        Toast.makeText(context, "IERT Curriculum Reloaded!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedChampagneAmber)
                ) {
                    Text("Reload", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel", color = ArchitecturalTitanium)
                }
            }
        )
    }

    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmation = false },
            containerColor = Color(0xFF10141D),
            title = { Text("Wipe Attendance Logs?", color = SoftCoralRose, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all marked attendance logs and reset back to 0%? Timetable slots will remain intact.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAttendanceLogs()
                        showWipeConfirmation = false
                        CaliperSoundManager.playThud()
                        CaliperHapticManager.bunkDoubleTap(context)
                        Toast.makeText(context, "Attendance records cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCoralRose)
                ) {
                    Text("Wipe", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmation = false }) {
                    Text("Cancel", color = ArchitecturalTitanium)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = ArchitecturalTitanium, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IceBlue,
                uncheckedThumbColor = ArchitecturalTitanium,
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
            )
        )
    }
}

@Composable
private fun NotificationSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    active: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(VelvetSageEmerald.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = VelvetSageEmerald, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = ArchitecturalTitanium, fontSize = 10.sp)
        }
        Text("Active", color = VelvetSageEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
