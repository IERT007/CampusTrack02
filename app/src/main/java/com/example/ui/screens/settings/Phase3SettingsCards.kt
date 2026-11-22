package com.example.ui.screens.settings

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Multi-Semester Profiles Card:
 * Switch active semester, create new semester profiles, and toggle Read-Only Archive mode.
 */
@Composable
fun SemesterProfilesCard(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors

    val semesters by viewModel.semesters.collectAsState()
    var showNewSemesterDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("semester_profiles_card"),
        borderColors = listOf(colors.primaryAccent.copy(alpha = 0.4f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MULTI-SEMESTER PROFILES",
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Switch term profiles or archive completed semesters",
                    color = colors.textMuted,
                    fontSize = 10.5.sp
                )
            }

            Button(
                onClick = {
                    CaliperSoundManager.playSnap(view)
                    CaliperHapticManager.tick(context)
                    showNewSemesterDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.16f)),
                border = BorderStroke(0.5.dp, colors.primaryAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("new_semester_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Semester",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Semester", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (semesters.isEmpty()) {
            Text(
                text = "Initializing default semester profile...",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                semesters.forEach { sem ->
                    val isSelected = sem.isActive
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) colors.primaryAccent.copy(alpha = 0.12f)
                                else colors.surface
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) colors.primaryAccent else colors.border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isSelected) {
                                    viewModel.switchActiveSemester(sem.id)
                                    Toast.makeText(context, "Switched to ${sem.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("semester_profile_item_${sem.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                sem.isArchived -> colors.warnAmber
                                                isSelected -> colors.safeGreen
                                                else -> colors.offSlate
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = sem.name,
                                        color = if (isSelected) colors.primaryAccent else colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sem.startDate}  →  ${sem.endDate}",
                                        color = colors.textSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (sem.isArchived) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.warnAmber.copy(alpha = 0.16f))
                                            .border(0.5.dp, colors.warnAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("READ-ONLY", color = colors.warnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.safeGreen.copy(alpha = 0.16f))
                                            .border(0.5.dp, colors.safeGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIVE", color = colors.safeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (sem.isArchived) "Archived (Protected from edits)" else "Archive Semester (Read-Only)",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                            Switch(
                                checked = sem.isArchived,
                                onCheckedChange = { archived ->
                                    viewModel.setSemesterArchived(sem.id, archived)
                                    val status = if (archived) "archived (Read-Only)" else "unlocked for editing"
                                    Toast.makeText(context, "${sem.name} $status", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onPrimaryAccent,
                                    checkedTrackColor = colors.warnAmber,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceVariant
                                ),
                                modifier = Modifier.testTag("semester_archive_switch_${sem.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewSemesterDialog) {
        NewSemesterDialog(
            onDismiss = { showNewSemesterDialog = false },
            onCreate = { name, start, end ->
                viewModel.createSemester(name = name, startDate = start, endDate = end, activate = true)
                showNewSemesterDialog = false
                Toast.makeText(context, "Created & activated $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun NewSemesterDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, startDate: String, endDate: String) -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    var name by remember { mutableStateOf("Semester 4 • Tool Engg") }
    var startDate by remember { mutableStateOf(LocalDate.now().format(formatter)) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusMonths(4).format(formatter)) }

    fun openPicker(initialIso: String, onPicked: (String) -> Unit) {
        val parsed = runCatching { LocalDate.parse(initialIso, formatter) }.getOrElse { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                onPicked(LocalDate.of(y, m + 1, d).format(formatter))
            },
            parsed.year,
            parsed.monthValue - 1,
            parsed.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Create New Semester Profile",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Semester Name", color = colors.textSecondary) },
                    placeholder = { Text("e.g., Semester 4 • Tool Engg") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_semester_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { openPicker(startDate) { startDate = it } },
                        border = BorderStroke(0.5.dp, colors.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Start Date", color = colors.textSecondary, fontSize = 9.5.sp)
                            Text(startDate, color = colors.primaryAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { openPicker(endDate) { endDate = it } },
                        border = BorderStroke(0.5.dp, colors.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("End Date", color = colors.textSecondary, fontSize = 9.5.sp)
                            Text(endDate, color = colors.primaryAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        Toast.makeText(context, "Enter a semester name", Toast.LENGTH_SHORT).show()
                    } else if (endDate < startDate) {
                        Toast.makeText(context, "End date must be after start date", Toast.LENGTH_SHORT).show()
                    } else {
                        onCreate(trimmed, startDate, endDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_new_semester_btn")
            ) {
                Text("Create & Switch", color = colors.onPrimaryAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * Offline-First Supabase Cloud Backup & Sync Card:
 * Email/Password authentication, Last Synced status, Sync Now, Restore from Cloud, and Sign Out.
 */
@Composable
fun CloudBackupSyncCard(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors

    val session by viewModel.supabaseSession.collectAsState()
    val lastSyncedAt by viewModel.cloudLastSyncedAt.collectAsState()
    val isBusy by viewModel.cloudSyncBusy.collectAsState()
    val statusMsg by viewModel.cloudSyncStatusMessage.collectAsState()
    val isConfigured = viewModel.isSupabaseConfigured

    var showAuthDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supabase_cloud_sync_card"),
        borderColors = listOf(colors.safeGreen.copy(alpha = 0.4f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !isConfigured -> colors.warnAmber
                                session != null -> colors.safeGreen
                                else -> colors.offSlate
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            !isConfigured -> "Supabase Keys Not Configured"
                            session != null -> session?.email.orEmpty()
                            else -> "Not Signed In (Offline-Only Mode)"
                        },
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            !isConfigured -> "Set SUPABASE_URL & SUPABASE_ANON_KEY in AI Studio Secrets"
                            lastSyncedAt.isNotBlank() -> "Last synced: $lastSyncedAt"
                            session != null -> "Auto-sync queued on every change (WorkManager)"
                            else -> "Sign in for automatic offline-first cloud vault sync"
                        },
                        color = colors.textSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }

            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.primaryAccent
                )
            }
        }

        if (!statusMsg.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMsg ?: "",
                color = colors.primaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (session == null) {
            Button(
                onClick = {
                    CaliperSoundManager.playSnap(view)
                    CaliperHapticManager.tick(context)
                    showAuthDialog = true
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.16f)),
                border = BorderStroke(0.5.dp, colors.primaryAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supabase_sign_in_open_btn")
            ) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign In / Create Supabase Account", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        CaliperSoundManager.playSnap(view)
                        viewModel.syncNowCloud { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) CaliperSoundManager.playSuccess()
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.16f)),
                    border = BorderStroke(0.5.dp, colors.primaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("supabase_sync_now_btn")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Now", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        CaliperSoundManager.playSnap(view)
                        viewModel.restoreFromCloud { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) CaliperSoundManager.playSuccess()
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.safeGreen.copy(alpha = 0.16f)),
                    border = BorderStroke(0.5.dp, colors.safeGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("supabase_restore_cloud_btn")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = colors.safeGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", color = colors.safeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    viewModel.signOutCloud {
                        Toast.makeText(context, "Signed out from Supabase Cloud", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isBusy,
                border = BorderStroke(0.5.dp, colors.dangerRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supabase_sign_out_btn")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = colors.dangerRed, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out", color = colors.dangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAuthDialog) {
        SupabaseAuthDialog(
            isBusy = isBusy,
            onDismiss = { showAuthDialog = false },
            onSignIn = { email, password ->
                viewModel.signInCloud(email, password) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (ok) {
                        CaliperSoundManager.playSuccess()
                        showAuthDialog = false
                    }
                }
            },
            onSignUp = { email, password ->
                viewModel.signUpCloud(email, password) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (ok) {
                        CaliperSoundManager.playSuccess()
                        showAuthDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun SupabaseAuthDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit
) {
    val colors = CaliperTheme.colors
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = colors.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (isSignUpMode) "Create Cloud Sync Account" else "Sign In to Supabase Cloud",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !isSignUpMode,
                        onClick = { isSignUpMode = false },
                        label = { Text("Sign In", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isSignUpMode,
                        onClick = { isSignUpMode = true },
                        label = { Text("Sign Up", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = colors.textSecondary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSignUpMode) onSignUp(email, password) else onSignIn(email, password)
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("supabase_auth_submit_btn")
            ) {
                Text(
                    text = if (isBusy) "Please wait..." else if (isSignUpMode) "Sign Up" else "Sign In",
                    color = colors.onPrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * In-App GitHub Releases OTA Updater Card:
 * Shows current semantic version and manual Check for Updates button.
 */
@Composable
fun AppUpdatesOtaCard(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = CaliperTheme.colors

    val isChecking by viewModel.isCheckingUpdate.collectAsState()
    val statusMsg by viewModel.updateStatusMessage.collectAsState()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ota_updates_card"),
        borderColors = listOf(colors.primaryAccent.copy(alpha = 0.4f), Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Caliper v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GitHub Releases • IERT007/CampusTrack02",
                    color = colors.textSecondary,
                    fontSize = 10.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.14f))
                    .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("OTA READY", color = colors.primaryAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!statusMsg.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMsg ?: "",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                CaliperSoundManager.playSnap(view)
                CaliperHapticManager.tick(context)
                viewModel.checkForUpdates(force = true) { feedback ->
                    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isChecking,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent.copy(alpha = 0.16f)),
            border = BorderStroke(0.5.dp, colors.primaryAccent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("check_for_updates_btn")
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking GitHub Releases...", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Check for Updates", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
