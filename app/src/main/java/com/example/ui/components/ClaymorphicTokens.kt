package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Immutable
data class Shadow(
    val radius: Dp,
    val spread: Dp = 0.dp,
    val color: Color,
    val offset: DpOffset = DpOffset.Zero
)

fun Modifier.dropShadow(
    shape: Shape,
    shadow: Shadow
): Modifier = this

fun Modifier.innerShadow(
    shape: Shape,
    shadow: Shadow
): Modifier = this

fun Modifier.claymorphicSurface(
    elevation: Dp = 8.dp,
    shape: Shape,
    isAccent: Boolean = false
): Modifier {
    val fill = if (isAccent) {
        Color(0xFF141923).copy(alpha = 0.92f)
    } else {
        Color(0xFF0E121B).copy(alpha = 0.85f)
    }
    return this
        .clip(shape)
        .background(fill, shape)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.Transparent
                )
            ),
            shape = shape
        )
}
