package com.example.ui.dialogs

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.notification.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun SettingsHubDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val is24HourFormat by viewModel.is24HourFormat.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    var showResetConfirmation by remember { mutableStateOf(false) }

    // SAF File Pickers for Export & Import
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToFile(context, uri) { success ->
                if (success) {
                    CaliperSoundManager.playSuccess()
                    CaliperHapticManager.successClick(context)
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD905070B))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .testTag("settings_hub_dialog"),
                backgroundColor = Color(0xF20C0E14),
                borderColors = listOf(IceBlue.copy(alpha = 0.5f), GlassBorderBottom)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(IceBlue.copy(alpha = 0.18f))
                                    .border(1.dp, IceBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = IceBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CALIPER SETTINGS",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tactile Engine & OS Preferences",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                CaliperSoundManager.playSnap()
                                onDismissRequest()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Settings Sections
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // SECTION 1: AUDIO SYNTHESIS & HAPTIC VERNIER ENGINE
                        Text(
                            text = "AUDIO & TACTILE ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0x0AFFFFFF),
                            borderColors = listOf(GlassBorderTop, GlassBorderBottom)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Procedural Audio Effects Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Procedural Audio Tones",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "In-memory PCM synthesis for snaps, chimes, and thuds",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isAudioEnabled,
                                        onCheckedChange = { viewModel.setAudioEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = SageMint,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = Color(0x22FFFFFF)
                                        )
                                    )
                                }

                                if (isAudioEnabled) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { CaliperSoundManager.playSnap() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A38BDF8)),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text("Snap", fontSize = 10.sp, color = IceBlue)
                                        }
                                        Button(
                                            onClick = { CaliperSoundManager.playSuccess() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A34D399)),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text("Success", fontSize = 10.sp, color = SageMint)
                                        }
                                        Button(
                                            onClick = { CaliperSoundManager.playThud() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFB7185)),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text("Thud", fontSize = 10.sp, color = SoftCoral)
                                        }
                                        Button(
                                            onClick = { CaliperSoundManager.playAlert() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFBBF24)),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text("Alert", fontSize = 10.sp, color = MutedAmber)
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0x1AFFFFFF))

                                // Tactile Vernier Haptic Feedback Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tactile Vernier Haptics",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Micro tick pulses and resistance feedback",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isHapticsEnabled,
                                        onCheckedChange = { viewModel.setHapticsEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = SageMint,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = Color(0x22FFFFFF)
                                        )
                                    )
                                }
                            }
                        }

                        // SECTION 2: DISPLAY & FORMAT PREFERENCES
                        Text(
                            text = "DISPLAY & TIME FORMAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0x0AFFFFFF),
                            borderColors = listOf(GlassBorderTop, GlassBorderBottom)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Time Format Toggle (12h vs 24h)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "24-Hour Time Format",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (is24HourFormat) "Displaying military time (e.g. 14:00)" else "Displaying 12-hour clock (e.g. 02:00 PM)",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = is24HourFormat,
                                        onCheckedChange = { viewModel.set24HourFormat(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = IceBlue,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = Color(0x22FFFFFF)
                                        )
                                    )
                                }

                                HorizontalDivider(color = Color(0x1AFFFFFF))

                                // Visual Theme Selection
                                Column {
                                    Text(
                                        text = "Interface Theme",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val themes = listOf("AMOLED Dark", "Soft Slate", "Minimal Light")
                                        themes.forEach { theme ->
                                            val isSelected = (selectedTheme == theme)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) IceBlue.copy(alpha = 0.2f) else Color(0x12FFFFFF))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) IceBlue else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { viewModel.setSelectedTheme(theme) }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = theme,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) IceBlue else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION 3: SYSTEM NOTIFICATIONS & BACKGROUND ALARMS
                        Text(
                            text = "NOTIFICATIONS & HEADS-UP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0x0AFFFFFF),
                            borderColors = listOf(GlassBorderTop, GlassBorderBottom)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = SageMint, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("07:30 AM Daily Schedule Briefing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Sends morning lecture and room numbers summary", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = IceBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("10-Minute Pre-Lecture Heads-Up", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Vibrates and informs class before start time", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = MutedAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("24h & 2h Submission Countdown Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Persistent alarms for Drawing Sheets & Lab files", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }

                                Button(
                                    onClick = {
                                        CaliperSoundManager.playAlert()
                                        CaliperHapticManager.tick(context)
                                        NotificationHelper.showMorningBriefing(context, "Test Alert: 5 classes scheduled today in LT-4 and Machine Shop.")
                                        Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x2238BDF8))
                                ) {
                                    Text("Dispatch Test Notification", color = IceBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // SECTION 4: DATA STORAGE, AUTO-VAULT & BACKUP
                        Text(
                            text = "DATA & AUTO-VAULT STORAGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceBlue,
                            letterSpacing = 1.sp
                        )

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0x0AFFFFFF),
                            borderColors = listOf(GlassBorderTop, GlassBorderBottom)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Silent Auto-Vault Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SageMint
                                )
                                Text(
                                    text = "Every attendance change automatically synchronizes to context.filesDir/caliper_vault_snapshot.json without user friction.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            CaliperSoundManager.playSnap()
                                            exportJsonLauncher.launch("Caliper_Academic_Backup_${System.currentTimeMillis()}.json")
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = IceBlue.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, IceBlue.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = IceBlue, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export JSON", color = IceBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            CaliperSoundManager.playSnap()
                                            importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SageMint.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SageMint.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = SageMint, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore JSON", color = SageMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.exportBackupJson { jsonString ->
                                            if (jsonString != null) {
                                                clipboardManager.setText(AnnotatedString(jsonString))
                                                CaliperSoundManager.playSuccess()
                                                Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x18FFFFFF))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Full JSON to Clipboard", color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }

                        // SECTION 5: CURRICULUM RESET (IERT DEFAULT 7 SUBJECTS)
                        Text(
                            text = "INSTITUTIONAL CURRICULUM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftCoral,
                            letterSpacing = 1.sp
                        )

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = SoftCoral.copy(alpha = 0.08f),
                            borderColors = listOf(SoftCoral.copy(alpha = 0.5f), GlassBorderBottom)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Reset & Load IERT Default Timetable",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftCoral
                                )
                                Text(
                                    text = "Injects the standard 7 subjects for IERT Mechanical (Tool Engg) 3rd Sem (ME-301, ME-302, ME-303W, ED-301, CS-301, ME-304, ME-305L) with clean 0-attendance.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                if (!showResetConfirmation) {
                                    Button(
                                        onClick = {
                                            CaliperSoundManager.playAlert()
                                            showResetConfirmation = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoral.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftCoral)
                                    ) {
                                        Text("Re-Initialize Curriculum", color = SoftCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.preloadDefaultIertData()
                                                showResetConfirmation = false
                                                CaliperSoundManager.playSuccess()
                                                CaliperHapticManager.successClick(context)
                                                Toast.makeText(context, "IERT Default curriculum loaded!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftCoral),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Confirm Reset", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { showResetConfirmation = false },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Cancel", color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // App Version & Signature Footer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CALIPER • ACADEMIC OPERATING SYSTEM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceBlue,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Version 2.0 • Offline-First • Room & Glance Engine",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
