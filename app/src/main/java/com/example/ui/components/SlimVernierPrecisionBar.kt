package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaliperTheme

/**
 * Slim Vernier Precision Bar:
 * - Sleek horizontal bar (height = 8.dp, shape = CircleShape)
 * - Subtle track with distinct statutory 75% indicator tick mark
 * - Smooth animated fill up to current overall percentage
 */
@Composable
fun SlimVernierPrecisionBar(
    percentage: Float,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val colors = CaliperTheme.colors
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "vernierProgress"
    )

    val fillColor = when {
        percentage >= 80f -> colors.safeGreen
        percentage >= 75f -> colors.primaryAccent
        percentage >= 65f -> colors.warnAmber
        else -> colors.dangerRed
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f%%", percentage),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = fillColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (percentage >= 75f) "SAFE ZONE" else "DEBAR RISK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (percentage >= 75f) colors.safeGreen else colors.dangerRed,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = "STATUTORY 75% CUTOFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Horizontal Track with 75% Statutory Marker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary.copy(alpha = 0.08f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                fillColor.copy(alpha = 0.72f),
                                fillColor
                            )
                        )
                    )
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val tickX = size.width * 0.75f
                drawLine(
                    color = colors.textPrimary.copy(alpha = 0.9f),
                    start = Offset(tickX, 0f),
                    end = Offset(tickX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
