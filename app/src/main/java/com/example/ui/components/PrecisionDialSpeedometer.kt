package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision Dial Speedometer (Safe-Zone Gauge)
 * Commercial-grade semicircular needle gauge inspired by automotive & aerospace telemetry.
 * Smoothly animates needle deflection based on current aggregate attendance vs statutory 75% cutoff.
 */
@Composable
fun PrecisionDialSpeedometer(
    percentage: Double,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    val clampedPercentage = percentage.coerceIn(0.0, 100.0).toFloat()

    // Gauge sweeps from 180° (left) to 360° (right), total sweep 180°
    // 0% -> 180°, 75% -> 180 + 0.75 * 180 = 315°, 100% -> 360°
    val targetAngle = 180f + (clampedPercentage / 100f) * 180f

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "speedometer_needle_angle"
    )

    val animatedPercentage by animateFloatAsState(
        targetValue = clampedPercentage,
        animationSpec = tween(durationMillis = 800),
        label = "speedometer_pct_text"
    )

    // Status classification based on 75% cutoff
    val (statusLabel, statusColor, statusBg) = when {
        percentage >= 80.0 -> Triple("Optimal Buffer", VelvetSageEmerald, VelvetSageEmerald.copy(alpha = 0.15f))
        percentage >= 75.0 -> Triple("Nominal Margin", IceBlue, IceBlue.copy(alpha = 0.15f))
        percentage >= 65.0 -> Triple("At Risk (<75%)", MutedChampagneAmber, MutedChampagneAmber.copy(alpha = 0.15f))
        else -> Triple("Critical Action Required", SoftCoralRose, SoftCoralRose.copy(alpha = 0.15f))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.width.coerceAtMost(size.height * 2f) - strokeWidth * 2) / 2f
                val centerX = size.width / 2f
                val centerY = size.height - 12.dp.toPx()

                val arcRect = androidx.compose.ui.geometry.Rect(
                    left = centerX - radius,
                    top = centerY - radius,
                    right = centerX + radius,
                    bottom = centerY + radius
                )

                // 1. Subtle background track
                drawArc(
                    color = Color.White.copy(alpha = 0.05f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Multi-segment gradient arc
                // Segment A: Danger zone (0% - 65% -> 180° to 297°)
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(SoftCoralRose.copy(alpha = 0.6f), MutedChampagneAmber.copy(alpha = 0.6f))
                    ),
                    startAngle = 180f,
                    sweepAngle = 117f, // 65% of 180
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                // Segment B: Warning / Margin zone (65% - 75% -> 297° to 315°)
                drawArc(
                    color = MutedChampagneAmber,
                    startAngle = 297f,
                    sweepAngle = 18f, // 10% of 180
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                // Segment C: Safe buffer zone (75% - 100% -> 315° to 360°)
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(IceBlue, VelvetSageEmerald)
                    ),
                    startAngle = 315f,
                    sweepAngle = 45f, // 25% of 180
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Tick Marks
                val tickCount = 10
                for (i in 0..tickCount) {
                    val tickPct = i / tickCount.toFloat()
                    val tickAngleRad = Math.toRadians((180.0 + tickPct * 180.0)).toFloat()
                    val isMajor = (i == 0 || i == 5 || i == 10 || i == 7 || i == 8)
                    val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                    val tickColor = if (i >= 8) VelvetSageEmerald.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)

                    val innerR = radius - strokeWidth / 2f - 4.dp.toPx()
                    val outerR = innerR - tickLength

                    val startX = centerX + innerR * cos(tickAngleRad)
                    val startY = centerY + innerR * sin(tickAngleRad)
                    val endX = centerX + outerR * cos(tickAngleRad)
                    val endY = centerY + outerR * sin(tickAngleRad)

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 4. Statutory 75% Cutoff Marker Line
                val cutoffAngleRad = Math.toRadians(315.0).toFloat()
                val cutoffInnerR = radius - strokeWidth - 6.dp.toPx()
                val cutoffOuterR = radius + strokeWidth / 2f + 4.dp.toPx()
                drawLine(
                    color = VelvetSageEmerald,
                    start = Offset(centerX + cutoffInnerR * cos(cutoffAngleRad), centerY + cutoffInnerR * sin(cutoffAngleRad)),
                    end = Offset(centerX + cutoffOuterR * cos(cutoffAngleRad), centerY + cutoffOuterR * sin(cutoffAngleRad)),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 5. Needle Rendering
                val needleRad = Math.toRadians(animatedAngle.toDouble()).toFloat()
                val needleLength = radius - 8.dp.toPx()
                val needleEndX = centerX + needleLength * cos(needleRad)
                val needleEndY = centerY + needleLength * sin(needleRad)

                // Needle line with glow
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Secondary needle shadow / accent tip
                val tipLength = 12.dp.toPx()
                val tipStartX = needleEndX - tipLength * cos(needleRad)
                val tipStartY = needleEndY - tipLength * sin(needleRad)
                drawLine(
                    color = statusColor,
                    start = Offset(tipStartX, tipStartY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center Pivot Hub
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = 9.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = statusColor,
                    radius = 4.5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }

            // Digital readout positioned in upper dial cavity
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-24).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f", animatedPercentage),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }

                Text(
                    text = "TARGET 75.0%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchitecturalTitanium,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Telemetry Status Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusBg)
                .border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel.uppercase(),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
