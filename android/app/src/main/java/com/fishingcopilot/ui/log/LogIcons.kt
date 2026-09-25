package com.fishingcopilot.ui.log

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Small line icons shared by the catch log and the catch sheet; the app ships no icon library.

@Composable
internal fun PinIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.5f, h * 0.9f)
                cubicTo(w * 0.2f, h * 0.55f, w * 0.22f, h * 0.18f, w * 0.5f, h * 0.18f)
                cubicTo(w * 0.78f, h * 0.18f, w * 0.8f, h * 0.55f, w * 0.5f, h * 0.9f)
                close()
            },
            color = color,
            style = stroke
        )
        drawCircle(color, radius = w * 0.11f, center = Offset(w * 0.5f, h * 0.42f), style = stroke)
    }
}

@Composable
internal fun TideFlowIcon(rising: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Water wave
        drawPath(
            Path().apply {
                moveTo(w * 0.12f, h * 0.68f)
                cubicTo(w * 0.25f, h * 0.52f, w * 0.4f, h * 0.82f, w * 0.6f, h * 0.68f)
                cubicTo(w * 0.75f, h * 0.56f, w * 0.85f, h * 0.72f, w * 0.9f, h * 0.68f)
            },
            color = color,
            style = stroke
        )
        // Arrow indicating flow
        if (rising) {
            drawPath(
                Path().apply {
                    moveTo(w * 0.35f, h * 0.48f)
                    lineTo(w * 0.68f, h * 0.2f)
                    moveTo(w * 0.48f, h * 0.18f)
                    lineTo(w * 0.7f, h * 0.18f)
                    lineTo(w * 0.7f, h * 0.4f)
                },
                color = color,
                style = stroke
            )
        } else {
            drawPath(
                Path().apply {
                    moveTo(w * 0.35f, h * 0.22f)
                    lineTo(w * 0.68f, h * 0.5f)
                    moveTo(w * 0.48f, h * 0.52f)
                    lineTo(w * 0.7f, h * 0.52f)
                    lineTo(w * 0.7f, h * 0.3f)
                },
                color = color,
                style = stroke
            )
        }
    }
}

@Composable
internal fun MoonPhaseMiniIcon(phaseName: String?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 1.3.dp.toPx())
        drawCircle(color.copy(alpha = 0.3f), radius = r - 1.dp.toPx(), center = center, style = stroke)
        when (phaseName) {
            "FULL_MOON" -> drawCircle(color, radius = r - 1.dp.toPx(), center = center)
            "NEW_MOON" -> drawCircle(color.copy(alpha = 0.5f), radius = r - 1.dp.toPx(), center = center, style = stroke)
            else -> {
                drawArc(
                    color = color,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(center.x - r + 1.dp.toPx(), center.y - r + 1.dp.toPx()),
                    size = Size((r - 1.dp.toPx()) * 2, (r - 1.dp.toPx()) * 2)
                )
            }
        }
    }
}

@Composable
internal fun BaitIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Fishing hook shape
        drawPath(
            Path().apply {
                moveTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.5f, h * 0.62f)
                cubicTo(w * 0.5f, h * 0.86f, w * 0.2f, h * 0.86f, w * 0.2f, h * 0.65f)
                cubicTo(w * 0.2f, h * 0.52f, w * 0.26f, h * 0.42f, w * 0.34f, h * 0.45f)
                lineTo(w * 0.3f, h * 0.56f)
            },
            color = color,
            style = stroke
        )
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.16f), style = stroke)
    }
}

@Composable
internal fun LightningIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        drawPath(
            Path().apply {
                moveTo(w * 0.55f, h * 0.1f)
                lineTo(w * 0.25f, h * 0.52f)
                lineTo(w * 0.52f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.9f)
                lineTo(w * 0.75f, h * 0.45f)
                lineTo(w * 0.48f, h * 0.45f)
                close()
            },
            color = color
        )
    }
}

@Composable
internal fun MemoIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.18f, h * 0.15f),
            size = Size(w * 0.64f, h * 0.72f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = stroke
        )
        drawLine(color, Offset(w * 0.32f, h * 0.38f), Offset(w * 0.68f, h * 0.38f), 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.55f), Offset(w * 0.68f, h * 0.55f), 1.2.dp.toPx(), StrokeCap.Round)
    }
}
