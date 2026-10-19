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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TelemetrySafeZoneGauge(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val clamped = percentage.coerceIn(0f, 100f)
    val animatedFill by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "telemetry_safe_zone_fill"
    )

    val fillColor = when {
        clamped >= 80f -> Color(0xFF22C55E)
        clamped >= 75f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val barHeight = 8.dp.toPx()
        val topY = (size.height - barHeight) / 2f
        val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(0f, topY),
            size = Size(size.width, barHeight),
            cornerRadius = cornerRadius
        )

        if (clamped > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(0f, topY),
                size = Size((size.width * animatedFill).coerceIn(barHeight, size.width), barHeight),
                cornerRadius = cornerRadius
            )
        }

        val thresholdX = size.width * 0.75f
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(thresholdX, 0f),
            end = Offset(thresholdX, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}
