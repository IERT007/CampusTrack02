package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TelemetrySafeZoneGauge(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val centerOffset = Offset(size.width / 2f, size.height - 16.dp.toPx())
        val radius = size.width * 0.40f
        val strokeWidth = 12.dp.toPx()

        // Background Track Cavity
        drawArc(
            color = Color.White.copy(alpha = 0.05f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Dynamic Telemetry Gradient Arc
        drawArc(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E))
            ),
            startAngle = 180f,
            sweepAngle = animatedProgress * 180f,
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 75% Statutory Limit Marker
        val markerAngleRad = Math.toRadians((180f + 0.75f * 180f).toDouble())
        val innerMarker = Offset(
            (centerOffset.x + (radius - strokeWidth) * Math.cos(markerAngleRad)).toFloat(),
            (centerOffset.y + (radius - strokeWidth) * Math.sin(markerAngleRad)).toFloat()
        )
        val outerMarker = Offset(
            (centerOffset.x + (radius + strokeWidth) * Math.cos(markerAngleRad)).toFloat(),
            (centerOffset.y + (radius + strokeWidth) * Math.sin(markerAngleRad)).toFloat()
        )
        drawLine(
            color = Color.White,
            start = innerMarker,
            end = outerMarker,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Rotary Needle Indicator
        val needleAngleRad = Math.toRadians((180f + animatedProgress * 180f).toDouble())
        val needleLength = radius * 0.85f
        val needleTip = Offset(
            (centerOffset.x + needleLength * Math.cos(needleAngleRad)).toFloat(),
            (centerOffset.y + needleLength * Math.sin(needleAngleRad)).toFloat()
        )

        drawLine(
            color = Color.White,
            start = centerOffset,
            end = needleTip,
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = centerOffset)
        drawCircle(color = Color(0xFF080A0F), radius = 2.5.dp.toPx(), center = centerOffset)
    }
}
