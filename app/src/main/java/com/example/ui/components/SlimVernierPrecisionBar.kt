package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CaliperTheme

/**
 * Slim Horizontal Vernier Precision Progress Bar with 75% Target Tick.
 */
@Composable
fun SlimVernierPrecisionBar(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val clamped = percentage.coerceIn(0f, 100f)
    val animatedFill by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "vernier_fill"
    )

    val barColor = when {
        clamped >= 80f -> colors.safeZone
        clamped >= 75f -> colors.deadlineWarning
        else -> colors.bunkDanger
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val trackHeight = 6.dp.toPx()
        val trackTop = (size.height - trackHeight) / 2f
        val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

        // Track background
        drawRoundRect(
            color = colors.divider,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = radius
        )

        // Active fill
        val fillWidth = (size.width * animatedFill).coerceAtLeast(trackHeight)
        if (clamped > 0f) {
            drawRoundRect(
                color = barColor,
                topLeft = Offset(0f, trackTop),
                size = Size(fillWidth.coerceAtMost(size.width), trackHeight),
                cornerRadius = radius
            )
        }

        // 75% Mandatory Target Vernier Tick Marker
        val targetX = size.width * 0.75f
        drawLine(
            color = colors.textPrimary.copy(alpha = 0.55f),
            start = Offset(targetX, 0f),
            end = Offset(targetX, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}
