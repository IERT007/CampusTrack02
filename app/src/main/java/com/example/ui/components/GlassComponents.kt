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
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.Unspecified,
    borderColors: List<Color> = emptyList(),
    borderWidth: Dp = 1.dp,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val resolvedBg = if (backgroundColor == Color.Unspecified || backgroundColor == GlassFill) {
        colors.cardFill
    } else {
        backgroundColor
    }
    val resolvedBorders = if (borderColors.isEmpty()) {
        listOf(colors.cardBorderTop, colors.cardBorderBottom)
    } else {
        borderColors
    }

    val interactiveModifier = if (onClick != null) {
        Modifier.tactilePressEffect(context) {
            onClick()
        }
    } else Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation = if (colors.isMonochrome) 0.dp else 6.dp,
                shape = shape,
                ambientColor = CalmShadowDark.copy(alpha = 0.55f),
                spotColor = CalmShadowDark.copy(alpha = 0.65f)
            )
            .clip(shape)
            .background(resolvedBg)
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(colors = resolvedBorders),
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
    shape: Shape = RoundedCornerShape(22.dp),
    borderColors: List<Color> = emptyList(),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = CaliperTheme.colors
    val resolvedBorders = if (borderColors.isEmpty()) {
        listOf(colors.cardBorderTop, colors.cardBorderBottom)
    } else {
        borderColors
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (colors.isMonochrome) 0.dp else 8.dp,
                shape = shape,
                ambientColor = CalmShadowDark.copy(alpha = 0.60f),
                spotColor = CalmShadowDark.copy(alpha = 0.70f)
            )
            .clip(shape)
            .background(colors.cardFill)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(resolvedBorders),
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
        percentage >= 75.0 -> colors.primaryAccent
        percentage >= 65.0 -> colors.deadlineWarning
        else -> colors.bunkDanger
    }

    val trackColor = if (colors.isMonochrome) Color(0x22FFFFFF) else CalmMist

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            val sweep = (animatedPercentage.coerceIn(0f, 100f) / 100f) * 360f
            drawArc(
                color = ringColor,
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
    val (label, bg, fg) = when (strictness.lowercase()) {
        "strict" -> Triple("Strict", PsychologyRed.copy(alpha = 0.14f), PsychologyRed)
        "moderate" -> Triple("Moderate", PsychologyAmber.copy(alpha = 0.16f), PsychologyAmber)
        else -> Triple("Chill", CalmSagePrimary.copy(alpha = 0.16f), CalmSagePrimary)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.8.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
        "lab" -> Triple("LAB", ElectricViolet.copy(alpha = 0.14f), ElectricViolet)
        "workshop" -> Triple("WORKSHOP", PsychologyOrange.copy(alpha = 0.14f), PsychologyOrange)
        else -> Triple("THEORY", PsychologyBlue.copy(alpha = 0.14f), PsychologyBlue)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.8.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
    val resolvedBg = if (containerColor == Color.Unspecified) colors.cardFill else containerColor

    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(
                elevation = if (colors.isMonochrome) 0.dp else 4.dp,
                shape = CircleShape,
                ambientColor = CalmShadowDark.copy(alpha = 0.5f),
                spotColor = CalmShadowDark.copy(alpha = 0.6f)
            )
            .clip(CircleShape)
            .background(resolvedBg)
            .border(1.dp, colors.cardBorderTop, CircleShape)
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
