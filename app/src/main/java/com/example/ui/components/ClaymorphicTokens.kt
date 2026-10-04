package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class Shadow(
    val radius: Dp,
    val spread: Dp = 0.dp,
    val color: Color,
    val offset: DpOffset = DpOffset(0.dp, 0.dp)
)

fun Modifier.dropShadow(
    shape: Shape,
    shadow: Shadow
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            color = shadow.color.toArgb()
            val dx = shadow.offset.x.toPx()
            val dy = shadow.offset.y.toPx()
            val radiusPx = shadow.radius.toPx()
            if (radiusPx > 0f) {
                setShadowLayer(radiusPx, dx, dy, shadow.color.toArgb())
            }
        }
        val outline = shape.createOutline(size, layoutDirection, this)
        val p = Paint().apply { asFrameworkPaint().set(paint) }
        when (outline) {
            is Outline.Rectangle -> canvas.drawRect(outline.rect, p)
            is Outline.Rounded -> canvas.drawRoundRect(
                outline.roundRect.left,
                outline.roundRect.top,
                outline.roundRect.right,
                outline.roundRect.bottom,
                outline.roundRect.bottomLeftCornerRadius.x,
                outline.roundRect.bottomLeftCornerRadius.y,
                p
            )
            is Outline.Generic -> canvas.drawPath(outline.path, p)
        }
    }
}

fun Modifier.innerShadow(
    shape: Shape,
    shadow: Shadow
): Modifier = this.drawWithContent {
    drawContent()
    drawIntoCanvas { canvas ->
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }
        canvas.save()
        canvas.clipPath(path)
        val strokePaint = Paint().apply {
            style = PaintingStyle.Stroke
            strokeWidth = (shadow.radius + shadow.spread).toPx() * 2
            this.color = shadow.color
        }
        canvas.drawPath(path, strokePaint)
        canvas.restore()
    }
}

fun Modifier.claymorphicSurface(
    elevation: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    isPressed: Boolean = false
): Modifier = this.then(
    if (isPressed) {
        Modifier
            .background(color = Color(0xFF111622).copy(alpha = 0.6f), shape = shape)
            .innerShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 4.dp,
                    spread = 1.dp,
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = DpOffset(0.dp, 2.dp)
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                ),
                shape = shape
            )
    } else {
        Modifier
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = elevation * 2,
                    spread = 0.dp,
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = DpOffset(0.dp, elevation / 2)
                )
            )
            .background(color = Color(0xFF111622).copy(alpha = 0.75f), shape = shape)
            .innerShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 2.dp,
                    spread = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    offset = DpOffset(0.dp, 1.dp)
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
                ),
                shape = shape
            )
    }
)
