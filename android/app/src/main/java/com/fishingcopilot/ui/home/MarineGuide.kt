package com.fishingcopilot.ui.home

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.sin

/** One step of a scale shown in the expanded explainer, e.g. "Slight · 0.5–1.25 m". */
data class ScaleStep(val name: String, val range: String?)

/**
 * A marine metric in plain language: live animation, level name, everyday comparison, level bar and the
 * raw numbers. Tapping expands a short explainer and the full scale with a "you are here" marker.
 */
@Composable
fun GuideRow(
    title: String,
    levelName: String,
    comparison: String,
    numbers: String,
    levelIndex: Int,
    levelCount: Int,
    levelColor: Color,
    explainer: String,
    scale: List<ScaleStep>,
    extra: String? = null,
    animation: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = OceanMidnight,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Text(levelName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = levelColor)
                    Text(comparison, style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
                }
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.size(width = 96.dp, height = 48.dp)) { animation() }
            }
            Spacer(Modifier.height(8.dp))
            LevelBar(levelIndex, levelCount, levelColor, levelName)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(numbers, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.weight(1f))
                if (!expanded) Text(stringResource(R.string.guide_tap_hint), style = MaterialTheme.typography.labelSmall, color = NauticalCyan)
            }
            if (extra != null) {
                Spacer(Modifier.height(4.dp))
                Text(extra, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = NauticalCyan)
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(explainer, style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
                    Spacer(Modifier.height(8.dp))
                    scale.forEachIndexed { i, step -> ScaleLine(step, current = i == levelIndex, color = levelColor) }
                }
            }
        }
    }
}

@Composable
private fun ScaleLine(step: ScaleStep, current: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Canvas(Modifier.size(8.dp)) { drawCircle(if (current) color else OceanCardBorder) }
        Spacer(Modifier.width(8.dp))
        Text(
            text = step.name + (step.range?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) TextHighContrast else TextMuted,
            modifier = Modifier.weight(1f)
        )
        if (current) {
            Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f), border = BorderStroke(1.dp, color)) {
                Text(
                    stringResource(R.string.guide_you_are_here),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun LevelBar(levelIndex: Int, levelCount: Int, color: Color, levelName: String) {
    val description = stringResource(R.string.guide_level_description, levelName, levelIndex + 1, levelCount)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .semantics { contentDescription = description }
    ) {
        repeat(levelCount) { i ->
            Canvas(modifier = Modifier.weight(1f).height(6.dp)) {
                drawRoundRect(
                    color = if (i <= levelIndex) color else OceanCardBorder,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
            }
        }
    }
}

/** Live 0..1 phase that loops every [periodMillis]; frozen at 0 when the user turned animations off. */
@Composable
private fun loopingPhase(periodMillis: Int): Float {
    if (animationsOff()) return 0f
    val transition = rememberInfiniteTransition(label = "marineGuide")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    return phase
}

@Composable
private fun animationsOff(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Swell whose height follows the forecast wave height and whose speed follows the wave period. */
@Composable
fun WaveAnimation(heightMeters: Double, periodSeconds: Double, color: Color) {
    val phase = loopingPhase((periodSeconds.coerceIn(2.0, 12.0) * 450).toInt())
    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val strength = (heightMeters / 2.5).coerceIn(0.06, 1.0).toFloat()
        val amplitude = strength * (size.height / 2 - 4.dp.toPx())
        fun wave(offset: Float, alpha: Float, lift: Float) {
            val path = Path()
            val baseline = size.height / 2 + lift
            var x = 0f
            path.moveTo(0f, size.height)
            while (x <= size.width) {
                val y = baseline + amplitude * sin((x / size.width * 2 * PI * 1.5 + (phase + offset) * 2 * PI).toFloat())
                path.lineTo(x, y)
                x += 2f
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, color.copy(alpha = alpha))
        }
        wave(0.35f, 0.35f, -4.dp.toPx())
        wave(0f, 0.75f, 4.dp.toPx())
    }
}

/** Wind streaks that get longer and faster with the Beaufort force. */
@Composable
fun WindAnimation(knots: Double, force: Int, color: Color) {
    val periodMillis = (4200 - knots.coerceIn(0.0, 30.0) * 120).toInt()
    val phase = loopingPhase(periodMillis)
    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val streak = (0.18f + force.coerceAtMost(8) * 0.06f) * size.width
        listOf(0.25f to 0f, 0.5f to 0.4f, 0.75f to 0.7f).forEach { (row, offset) ->
            val head = ((phase + offset) % 1f) * (size.width + streak)
            val y = row * size.height
            drawLine(
                color = color.copy(alpha = if (force == 0) 0.25f else 0.85f),
                start = Offset((head - streak).coerceAtLeast(0f), y),
                end = Offset(head.coerceAtMost(size.width), y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/** Barometer needle on a 980-1040 hPa arc, with an arrow for the three-hour trend that pulses when falling fast. */
@Composable
fun PressureGauge(hPa: Double, trendDirection: Int, alarm: Boolean, color: Color) {
    val pulse = if (alarm) loopingPhase(1200) else 0f
    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val radius = size.height - 6.dp.toPx()
        val center = Offset(size.width / 2 - 8.dp.toPx(), size.height - 2.dp.toPx())
        drawArc(
            color = OceanCardBorder, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2),
            style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
        )
        val fraction = ((hPa - 980) / 60).coerceIn(0.0, 1.0)
        val angle = Math.toRadians(180 + fraction * 180)
        drawLine(
            color, center,
            Offset(center.x + (radius * 0.85f * kotlin.math.cos(angle)).toFloat(), center.y + (radius * 0.85f * kotlin.math.sin(angle)).toFloat()),
            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
        )
        drawCircle(color, radius = 3.dp.toPx(), center = center)
        if (trendDirection != 0) {
            val x = size.width - 10.dp.toPx()
            val top = 10.dp.toPx()
            val bottom = size.height - 8.dp.toPx()
            val alpha = if (alarm) 0.4f + 0.6f * sin(pulse * PI).toFloat() else 1f
            val arrow = Path().apply {
                if (trendDirection > 0) {
                    moveTo(x, top); lineTo(x + 7.dp.toPx(), top + 10.dp.toPx()); lineTo(x - 7.dp.toPx(), top + 10.dp.toPx())
                } else {
                    moveTo(x, bottom); lineTo(x + 7.dp.toPx(), bottom - 10.dp.toPx()); lineTo(x - 7.dp.toPx(), bottom - 10.dp.toPx())
                }
                close()
            }
            drawPath(arrow, color.copy(alpha = alpha))
        }
    }
}

/** Water particles drifting at a speed that follows the current. */
@Composable
fun CurrentAnimation(knots: Double, color: Color) {
    val periodMillis = (5200 - knots.coerceIn(0.0, 2.5) * 1800).toInt()
    val phase = loopingPhase(periodMillis)
    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val rows = listOf(0.3f, 0.55f, 0.8f)
        rows.forEachIndexed { r, row ->
            repeat(4) { i ->
                val x = (((phase + i * 0.25f + r * 0.13f) % 1f) * size.width)
                drawCircle(color.copy(alpha = 0.3f + 0.15f * i), radius = 2.5.dp.toPx(), center = Offset(x, row * size.height))
            }
        }
    }
}
