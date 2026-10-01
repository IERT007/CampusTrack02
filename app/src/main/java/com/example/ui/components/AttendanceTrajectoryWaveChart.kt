package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceLogEntity
import com.example.ui.theme.*

/**
 * Attendance Trajectory Wave Chart
 * Implements a smooth quadratic Bezier wave area chart illustrating cumulative
 * attendance percentage progression across the semester, with a subtle 75% cutoff threshold line.
 */
@Composable
fun AttendanceTrajectoryWaveChart(
    logs: List<AttendanceLogEntity>,
    overallPercentage: Double,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp
) {
    // Generate chronological cumulative progression points
    val trajectoryPoints = remember(logs, overallPercentage) {
        val conductedLogs = logs.filter { it.status == "attended" || it.status == "bunked" }
            .sortedBy { it.date }

        if (conductedLogs.isEmpty()) {
            // Default baseline trajectory showing 75% statutory target
            listOf(75.0, 78.0, 80.0, 77.0, overallPercentage.coerceAtLeast(0.0))
        } else {
            var cumAttended = 0
            var cumConducted = 0
            val points = mutableListOf<Double>()
            // Group by distinct dates to calculate cumulative % after each day
            val dateGroups = conductedLogs.groupBy { it.date }
            dateGroups.toSortedMap().forEach { (_, dayLogs) ->
                cumAttended += dayLogs.count { it.status == "attended" }
                cumConducted += dayLogs.size
                val pct = if (cumConducted > 0) (cumAttended * 100.0) / cumConducted else 0.0
                points.add(pct)
            }
            if (points.size < 2) {
                listOf(points.firstOrNull() ?: overallPercentage, overallPercentage)
            } else {
                points
            }
        }
    }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "trajectory_wave_anim"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColors = listOf(IceBlue.copy(alpha = 0.35f), GlassBorderBottom)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(IceBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ATTENDANCE TRAJECTORY WAVE",
                        color = IceBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Cumulative Progression Trend",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Target pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VelvetSageEmerald.copy(alpha = 0.12f))
                    .border(0.5.dp, VelvetSageEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "75% Cutoff Line",
                    color = VelvetSageEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Wave Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val width = size.width
            val chartHeight = size.height
            val bottomPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val usableHeight = chartHeight - topPadding - bottomPadding

            // Fixed Y-scale: 50% to 100% (or min 0 if low)
            val minY = (trajectoryPoints.minOrNull() ?: 50.0).coerceAtMost(50.0).toFloat()
            val maxY = 100f
            val yRange = (maxY - minY).coerceAtLeast(10f)

            fun getY(value: Float): Float {
                val normalized = ((value - minY) / yRange).coerceIn(0f, 1f)
                return (chartHeight - bottomPadding) - (normalized * usableHeight)
            }

            // Draw horizontal 75% statutory threshold line
            val thresholdY = getY(75f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            drawLine(
                color = VelvetSageEmerald.copy(alpha = 0.65f),
                start = Offset(0f, thresholdY),
                end = Offset(width, thresholdY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )

            // Draw points & Bezier wave path
            if (trajectoryPoints.size >= 2) {
                val stepX = width / (trajectoryPoints.size - 1).toFloat()
                val wavePath = Path()
                val fillPath = Path()

                val startY = getY(trajectoryPoints[0].toFloat())
                wavePath.moveTo(0f, startY)
                fillPath.moveTo(0f, chartHeight)
                fillPath.lineTo(0f, startY)

                for (i in 0 until trajectoryPoints.size - 1) {
                    val x1 = i * stepX
                    val y1 = getY(trajectoryPoints[i].toFloat())
                    val x2 = (i + 1) * stepX
                    val y2 = getY(trajectoryPoints[i + 1].toFloat())

                    // Mid control points for smooth quadratic Bezier wave
                    val cx = (x1 + x2) / 2f
                    wavePath.cubicTo(cx, y1, cx, y2, x2, y2)
                    fillPath.cubicTo(cx, y1, cx, y2, x2, y2)
                }

                fillPath.lineTo(width, chartHeight)
                fillPath.close()

                // Draw gradient area beneath the wave
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            IceBlue.copy(alpha = 0.25f * animProgress),
                            Color(0x0238BDF8)
                        ),
                        startY = topPadding,
                        endY = chartHeight
                    )
                )

                // Draw glowing wave stroke line
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            IceBlue.copy(alpha = 0.8f),
                            SlateTeal,
                            VelvetSageEmerald
                        )
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight latest / current terminal node
                val lastIdx = trajectoryPoints.size - 1
                val lastX = lastIdx * stepX
                val lastY = getY(trajectoryPoints[lastIdx].toFloat())

                drawCircle(
                    color = VelvetSageEmerald,
                    radius = 4.5.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Semester Origin", color = ArchitecturalTitanium, fontSize = 10.sp)
            Text("75% Statutory Cutoff", color = VelvetSageEmerald, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("Present Day", color = ArchitecturalTitanium, fontSize = 10.sp)
        }
    }
}
