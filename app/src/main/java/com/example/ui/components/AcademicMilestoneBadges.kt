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
import com.example.ui.theme.PlusJakartaFamily

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
            title = "Consistency Streak",
            subtitle = "${streakDays}d Active",
            code = "STREAK",
            accentColor = colors.streakGold,
            unlocked = streakDays >= 3
        )
        MilestoneBadgeCard(
            title = "Safe Zone Shield",
            subtitle = ">= 75% All",
            code = "SHIELD",
            accentColor = colors.safeZone,
            unlocked = allSubjectsSafe
        )
        MilestoneBadgeCard(
            title = "Dean's Honors",
            subtitle = ">= 85% Agg",
            code = "HONORS",
            accentColor = colors.scheduledLecture,
            unlocked = overallPercentage >= 85.0
        )
        MilestoneBadgeCard(
            title = "Zero Drift",
            subtitle = "Clean Week",
            code = "FOCUS",
            accentColor = colors.safeZone,
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
    val shape = RoundedCornerShape(20.dp)
    val effectiveColor = if (unlocked) accentColor else colors.textMuted

    Column(
        modifier = Modifier
            .width(142.dp)
            .clip(shape)
            .background(colors.cardFill, shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (unlocked) accentColor.copy(alpha = 0.45f) else colors.cardBorderTop,
                        colors.cardBorderBottom
                    )
                ),
                shape = shape
            )
            .padding(14.dp)
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
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (unlocked) colors.textPrimary else colors.textMuted
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = effectiveColor
            )
        )
    }
}
