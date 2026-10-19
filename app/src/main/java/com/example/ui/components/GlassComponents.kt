package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.fluidTouchFeedback
import java.util.Locale

fun triggerHapticFeedback(context: Context, isStrong: Boolean = false) {
    if (isStrong) {
        CaliperHapticManager.confirmTap(context)
    } else {
        CaliperHapticManager.tick(context)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    containerColor: Color = CaliperTheme.colors.cardFill,
    borderColors: List<Color> = listOf(
        CaliperTheme.colors.cardBorderTop,
        CaliperTheme.colors.cardBorderBottom
    ),
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CaliperTheme.colors
    val shadowSpotColor = if (colors.isLightGlass) {
        Color(0xFF4A4238).copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.45f)
    }

    val interactiveModifier = if (onClick != null) {
        modifier.fluidTouchFeedback(enabled = true, onClick = onClick)
    } else {
        modifier
    }

    Column(
        modifier = interactiveModifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowSpotColor,
                spotColor = shadowSpotColor
            )
            .clip(shape)
            .background(containerColor, shape)
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(borderColors),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    borderColors: List<Color> = listOf(
        CaliperTheme.colors.cardBorderTop,
        CaliperTheme.colors.cardBorderBottom
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = shape,
        borderColors = borderColors,
        content = content
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CaliperTheme.colors.textPrimary,
    containerColor: Color = CaliperTheme.colors.containerSurface
) {
    val colors = CaliperTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .fluidTouchFeedback(enabled = true, onClick = onClick)
            .clip(shape)
            .background(containerColor, shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.cardBorderTop, colors.cardBorderBottom)
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MetricRadialRing(
    percentage: Double,
    size: Dp = 60.dp,
    strokeWidth: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val ringColor = when {
        percentage >= 80.0 -> colors.safeZone
        percentage >= 75.0 -> colors.deadlineWarning
        else -> colors.bunkDanger
    }

    val animatedSweep by animateFloatAsState(
        targetValue = ((percentage.toFloat().coerceIn(0f, 100f)) / 100f) * 360f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "metric_radial_sweep"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = colors.divider,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = String.format(Locale.US, "%.0f%%", percentage),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = colors.textPrimary
        )
    }
}

@Composable
fun StrictnessBadge(strictness: String) {
    val colors = CaliperTheme.colors
    val badgeColor = when (strictness.uppercase(Locale.US)) {
        "STRICT" -> colors.bunkDanger
        "MODERATE" -> colors.deadlineWarning
        else -> colors.safeZone
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(badgeColor.copy(alpha = 0.14f), shape)
            .border(1.dp, badgeColor.copy(alpha = 0.35f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = strictness.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = badgeColor
        )
    }
}

@Composable
fun TypeBadge(type: String) {
    val colors = CaliperTheme.colors
    val badgeColor = when (type.uppercase(Locale.US)) {
        "LAB", "WORKSHOP" -> colors.safeZone
        else -> colors.scheduledLecture
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(badgeColor.copy(alpha = 0.14f), shape)
            .border(1.dp, badgeColor.copy(alpha = 0.35f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = badgeColor
        )
    }
}
