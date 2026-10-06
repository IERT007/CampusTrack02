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
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

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
    backgroundColor: Color = GlassFill,
    borderColors: List<Color> = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f)),
    borderWidth: Dp = 0.5.dp,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    val interactiveModifier = if (onClick != null) {
        Modifier.tactilePressEffect(context) {
            onClick()
        }
    } else Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = Color(0x33000000),
                ambientColor = Color.White.copy(alpha = 0.02f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.045f),
                        Color.White.copy(alpha = 0.02f),
                        colors.containerSurface.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(colors = borderColors),
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
    borderColors: List<Color> = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f)),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = CaliperTheme.colors
    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = shape, spotColor = Color(0x66000000))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        colors.containerSurface.copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(borderColors),
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
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label = "radial_pct"
    )

    val color = when {
        percentage >= 75.0 -> NeonEmerald
        percentage >= 65.0 -> WarningAmber
        else -> DangerRed
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            // Background track
            drawCircle(
                color = Color(0x22FFFFFF),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Active arc
            val sweep = (animatedPercentage.coerceIn(0f, 100f) / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        color.copy(alpha = 0.6f),
                        color,
                        color
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
                color = TextPrimary
            )
            Text(
                text = if (percentage >= 75.0) "SAFE ZONE" else if (percentage >= 65.0) "WARNING" else "CRITICAL",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StrictnessBadge(strictness: String) {
    val (label, bg, fg) = when (strictness.lowercase()) {
        "strict" -> Triple("Strict", Color(0x33EF4444), StrictRed)
        "moderate" -> Triple("Moderate", Color(0x33F59E0B), ModerateAmber)
        else -> Triple("Chill", Color(0x3310B981), ChillEmerald)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
    val (label, bg, fg) = when (type.lowercase()) {
        "lab" -> Triple("LAB", Color(0x33A855F7), Color(0xFFC084FC))
        "workshop" -> Triple("WORKSHOP", Color(0x33F43F5E), Color(0xFFFB7185))
        else -> Triple("THEORY", Color(0x3300E5FF), NeonCyan)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
    tint: Color = TextPrimary,
    containerColor: Color = Color(0x26FFFFFF)
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, GlassBorderTop, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint),
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
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
