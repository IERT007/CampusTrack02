package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AttendanceLogEntity
import com.example.ui.theme.CaliperTheme

@Composable
fun AttendanceTrajectoryWaveChart(
    logs: List<AttendanceLogEntity>,
    currentPercentage: Double,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val colors = CaliperTheme.colors
    val lineColor = when {
        currentPercentage >= 80.0 -> colors.safeZone
        currentPercentage >= 75.0 -> colors.deadlineWarning
        else -> colors.bunkDanger
    }

    val sortedLogs = logs.filter { it.status == "PRESENT" || it.status == "ABSENT" }.sortedBy { it.date }
    val points = if (sortedLogs.isEmpty()) {
        listOf(currentPercentage.toFloat().coerceIn(0f, 100f), currentPercentage.toFloat().coerceIn(0f, 100f))
    } else {
        var attended = 0
        var total = 0
        sortedLogs.map { log ->
            total++
            if (log.status == "PRESENT") attended++
            ((attended.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
        }.takeLast(24).let { list ->
            if (list.size == 1) listOf(list.first(), list.first()) else list
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val padVertical = 12.dp.toPx()
        val usableH = (h - padVertical * 2f).coerceAtLeast(1f)

        // 75% Reference Line
        val y75 = padVertical + usableH * (1f - 0.75f)
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(0f, y75),
            end = Offset(w, y75),
            strokeWidth = 1.dp.toPx()
        )

        val stepX = w / (points.size - 1).coerceAtLeast(1)
        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { idx, pct ->
            val x = idx * stepX
            val y = padVertical + usableH * (1f - (pct / 100f))
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (idx - 1) * stepX
                val prevY = padVertical + usableH * (1f - (points[idx - 1] / 100f))
                val ctrlX = (prevX + x) / 2f
                linePath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                fillPath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.28f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
