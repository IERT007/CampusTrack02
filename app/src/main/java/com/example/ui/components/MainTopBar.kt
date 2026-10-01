package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.tactilePressEffect

/**
 * MainTopBar
 * Cleanly aligned enterprise header bar:
 * Brand Title ("Caliper") on the left, and an integrated, translucent 20px Settings icon on the top right.
 * Dual-theme aware via CaliperTheme.colors.
 */
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Clean Brand Title & Academic OS Descriptor
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Caliper",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.safeZoneContainer)
                        .border(0.5.dp, colors.safeZone.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "IERT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.safeZone,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Text(
                text = "Tactical Academic OS • 3rd Sem Mechanical",
                fontSize = 11.sp,
                color = colors.textSecondary,
                letterSpacing = 0.2.sp
            )
        }

        // Right: Integrated Translucent 20px Settings Icon Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .tactilePressEffect(context) {
                    CaliperSoundManager.playSnap()
                    CaliperHapticManager.tick(context)
                    onOpenSettings()
                }
                .testTag("top_bar_settings_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = colors.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
