package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Academic Milestones & Gamified Consistency Badges
 * Horizontal chips with subtle pastel cards honoring engineering rigor:
 * "🔥 N-Day College Streak", "🛡️ Workshop Veteran", "📐 Drawing Precision", and "⚡ 85%+ Elite Shield".
 */
@Composable
fun AcademicMilestoneBadges(
    streakDays: Int,
    overallPercentage: Double,
    isWorkshopVeteran: Boolean = true,
    isDrawingPrecision: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ENGINEERING CONSISTENCY SHIELDS",
                color = ArchitecturalTitanium,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Academic Milestones",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. College Streak Shield
            MilestoneBadgeCard(
                icon = "🔥",
                title = "$streakDays-Day Streak",
                subtitle = if (streakDays > 0) "Active Discipline" else "Starts Next Class",
                accentColor = MutedChampagneAmber,
                isUnlocked = streakDays > 0
            )

            // 2. Workshop Veteran (ME-303W)
            MilestoneBadgeCard(
                icon = "🛡️",
                title = "Workshop Veteran",
                subtitle = "ME-303W Lathe & Fitting",
                accentColor = SlateTeal,
                isUnlocked = isWorkshopVeteran
            )

            // 3. Drawing Precision (ED-301)
            MilestoneBadgeCard(
                icon = "📐",
                title = "Drawing Precision",
                subtitle = "ED-301 Sheet Drafter",
                accentColor = IceBlue,
                isUnlocked = isDrawingPrecision
            )

            // 4. Elite Shield (85%+)
            MilestoneBadgeCard(
                icon = "⚡",
                title = "85%+ Elite Shield",
                subtitle = if (overallPercentage >= 85.0) "Admit Card Safe" else "Target 85.0%",
                accentColor = VelvetSageEmerald,
                isUnlocked = overallPercentage >= 85.0
            )
        }
    }
}

@Composable
private fun MilestoneBadgeCard(
    icon: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    isUnlocked: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isUnlocked) accentColor.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f)
            )
            .border(
                0.5.dp,
                if (isUnlocked) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = if (isUnlocked) Color.White else ArchitecturalTitanium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = if (isUnlocked) accentColor else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
