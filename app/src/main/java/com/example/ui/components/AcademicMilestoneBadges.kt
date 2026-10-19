package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun AcademicMilestoneBadges(
    streakDays: Int,
    overallPercentage: Double,
    allSubjectsSafe: Boolean,
    zeroBunksThisWeek: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MilestoneBadgeCard(
            title = "IRON STREAK",
            subtitle = "${streakDays}D ACTIVE",
            code = "MS-01",
            accentColor = colors.streakGold,
            unlocked = streakDays >= 3
        )
        MilestoneBadgeCard(
            title = "SAFE ZONE SHIELD",
            subtitle = ">= 75% ALL",
            code = "MS-02",
            accentColor = colors.safeZone,
            unlocked = allSubjectsSafe
        )
        MilestoneBadgeCard(
            title = "DEAN'S TIER",
            subtitle = ">= 85% AGG",
            code = "MS-03",
            accentColor = colors.scheduledLecture,
            unlocked = overallPercentage >= 85.0
        )
        MilestoneBadgeCard(
            title = "ZERO DRIFT",
            subtitle = "CLEAN WEEK",
            code = "MS-04",
            accentColor = Color(0xFF06B6D4),
            unlocked = zeroBunksThisWeek
        )
    }
}

@Composable
fun MilestoneBadgeCard(
    title: String,
    subtitle: String,
    code: String,
    accentColor: Color,
    unlocked: Boolean
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val effectiveColor = if (unlocked) accentColor else colors.textMuted

    Column(
        modifier = Modifier
            .width(138.dp)
            .clip(shape)
            .background(Color(0xFF0E121B).copy(alpha = 0.85f), shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (unlocked) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = effectiveColor
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (unlocked) colors.textPrimary else colors.textMuted
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = effectiveColor
            )
        )
    }
}
