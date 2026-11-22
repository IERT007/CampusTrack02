package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.MainViewModel
import com.example.ui.theme.CaliperTheme
import kotlin.math.roundToInt

/**
 * In-App Material 3 GitHub Releases OTA Update Dialog.
 * Displays remote version, release notes, live APK download progress bar,
 * and invokes FileProvider system installation via MainViewModel.
 */
@Composable
fun OtaUpdateDialog(
    viewModel: MainViewModel
) {
    val release by viewModel.availableUpdate.collectAsState()
    val isDownloading by viewModel.isDownloadingUpdate.collectAsState()
    val progress by viewModel.updateDownloadProgress.collectAsState()
    val statusMessage by viewModel.updateStatusMessage.collectAsState()

    val activeRelease = release ?: return
    val colors = CaliperTheme.colors
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) viewModel.dismissUpdateDialog()
        },
        containerColor = colors.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.16f))
                    .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Update available",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Caliper v${activeRelease.versionName} Available",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Installed: v${BuildConfig.VERSION_NAME}  →  Latest: ${activeRelease.tagName}",
                    color = colors.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WHAT'S NEW",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = activeRelease.changelog,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                if (isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading APK...",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(progress * 100f).roundToInt()}%",
                                color = colors.primaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = colors.primaryAccent,
                            trackColor = colors.surface
                        )
                    }
                } else if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage ?: "",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    CaliperSoundManager.playSuccess()
                    CaliperHapticManager.successClick(context)
                    viewModel.downloadAndInstallUpdate { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("ota_update_now_btn")
            ) {
                Text(
                    text = if (isDownloading) "Downloading..." else "Update Now",
                    color = colors.onPrimaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.dismissUpdateDialog() },
                enabled = !isDownloading,
                modifier = Modifier.testTag("ota_update_later_btn")
            ) {
                Text("Later", color = colors.textSecondary)
            }
        }
    )
}
