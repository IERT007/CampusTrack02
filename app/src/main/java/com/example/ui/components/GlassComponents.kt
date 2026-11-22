package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.tactilePressEffect

fun triggerHapticFeedback(context: Context, isHeavy: Boolean = false) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (isHeavy) {
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createOneShot(18, 120)
            }
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(if (isHeavy) 40L else 20L)
        }
    } catch (_: Exception) {
        // Safe fallback if permission or hardware unavailable
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = Color.Unspecified,
    borderColors: List<Color>? = null,
    borderWidth: Dp = 0.5.dp,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val resolvedBorder = borderColors ?: listOf(colors.border, Color.Transparent)

    val interactiveModifier = if (onClick != null) {
        Modifier.tactilePressEffect(context) {
            onClick()
        }
    } else Modifier

    val fillBrush = if (backgroundColor != Color.Unspecified) {
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                colors.surface.copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colors.surfaceVariant.copy(alpha = 0.92f),
                colors.surface.copy(alpha = 0.94f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(colors = resolvedBorder),
                shape = shape
            )
            .then(interactiveModifier)
            .padding(innerPadding)
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = CaliperTheme.colors
    val resolvedBorder = borderColors ?: listOf(colors.border, Color.Transparent)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.surfaceVariant.copy(alpha = 0.92f),
                        colors.surface.copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(resolvedBorder),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun MetricRadialRing(
    percentage: Double,
    size: Dp = 130.dp,
    strokeWidth: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label = "radial_pct"
    )

    val ringColor = when {
        percentage >= 75.0 -> colors.safeGreen
        percentage >= 65.0 -> colors.warnAmber
        else -> colors.dangerRed
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            drawCircle(
                color = colors.border.copy(alpha = 0.45f),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            val sweep = (animatedPercentage.coerceIn(0f, 100f) / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        ringColor.copy(alpha = 0.6f),
                        ringColor,
                        ringColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f%%", percentage),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = if (percentage >= 75.0) "SAFE ZONE" else if (percentage >= 65.0) "WARNING" else "CRITICAL",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = ringColor,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StrictnessBadge(strictness: String) {
    val colors = CaliperTheme.colors
    val (label, fg) = when (strictness.lowercase()) {
        "strict" -> "Strict" to colors.dangerRed
        "moderate" -> "Moderate" to colors.warnAmber
        else -> "Chill" to colors.safeGreen
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fg.copy(alpha = 0.16f))
            .border(0.5.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TypeBadge(type: String) {
    val colors = CaliperTheme.colors
    val (label, fg) = when (type.lowercase()) {
        "lab" -> "LAB" to colors.primaryAccent
        "workshop" -> "WORKSHOP" to colors.warnAmber
        else -> "THEORY" to colors.primaryAccent
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fg.copy(alpha = 0.16f))
            .border(0.5.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val resolvedTint = if (tint == Color.Unspecified) colors.textPrimary else tint
    val resolvedContainer = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(resolvedContainer)
            .border(0.5.dp, colors.border, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = resolvedTint),
                onClick = {
                    triggerHapticFeedback(context, false)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(20.dp)
        )
    }
}
