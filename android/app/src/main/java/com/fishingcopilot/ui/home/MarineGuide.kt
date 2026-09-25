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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
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

/**
 * Modern multi-layered oceanic swell and dynamic wave simulation.
 * Includes deep ocean background swell, translucent midground roll,
 * foreground trochoidal swell with breaking crest highlights, foam specks,
 * and a buoyant nautical angler's marker buoy pitching and bobbing realistically on the wave surface.
 */
@Composable
fun WaveAnimation(
    heightMeters: Double,
    periodSeconds: Double,
    color: Color
) {
    // Wave period clamped between 3s and 12s, scaled for realistic visual cadence
    val periodMillis = (periodSeconds.coerceIn(3.0, 12.0) * 380).toInt().coerceIn(1200, 4800)
    val phase = loopingPhase(periodMillis)

    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val w = size.width
        val h = size.height

        // Normalized wave energy: 0.1m is calm ripple, 1.5m is significant swell, 2.5m+ is heavy
        val strength = (heightMeters / 2.2).coerceIn(0.10, 1.0).toFloat()
        val amp = strength * (h * 0.24f)

        // 1. Distant Background Swell (soft deep navy rolling swell for perspective)
        val bgPath = Path().apply {
            val bgBaseY = h * 0.52f
            val bgAmp = amp * 0.45f
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val theta = (x / w) * (2f * PI.toFloat() * 1.1f) + ((phase * 0.65f) * 2f * PI.toFloat())
                val y = bgBaseY - bgAmp * sin(theta)
                lineTo(x, y)
                x += 2f
            }
            lineTo(w, h)
            close()
        }
        drawPath(
            path = bgPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F2B48).copy(alpha = 0.50f),
                    Color(0xFF081A2F).copy(alpha = 0.85f)
                ),
                startY = h * 0.40f,
                endY = h
            )
        )

        // 2. Midground Swell (translucent sea-green / cyan body)
        val midPath = Path().apply {
            val midBaseY = h * 0.60f
            val midAmp = amp * 0.68f
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val theta = (x / w) * (2f * PI.toFloat() * 1.35f) + ((phase * 0.88f + 0.35f) * 2f * PI.toFloat())
                val y = midBaseY - midAmp * (sin(theta) - 0.15f * cos(2f * theta))
                lineTo(x, y)
                x += 2f
            }
            lineTo(w, h)
            close()
        }
        drawPath(
            path = midPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.38f),
                    Color(0xFF051C33).copy(alpha = 0.75f)
                ),
                startY = h * 0.50f,
                endY = h
            )
        )

        // 3. Foreground Trochoidal Swell (main crisp wave)
        val fgBaseY = h * 0.68f
        val k = (2f * PI.toFloat() * 1.25f) / w
        fun waveY(x: Float): Float {
            val theta = k * x + (phase * 2f * PI.toFloat())
            return fgBaseY - amp * (sin(theta) - 0.22f * cos(2f * theta))
        }

        fun waveSlope(x: Float): Float {
            val theta = k * x + (phase * 2f * PI.toFloat())
            return -amp * k * (cos(theta) + 0.44f * sin(2f * theta))
        }

        val fgPath = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                lineTo(x, waveY(x))
                x += 2f
            }
            lineTo(w, h)
            close()
        }

        drawPath(
            path = fgPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.85f),
                    color.copy(alpha = 0.45f),
                    Color(0xFF031628).copy(alpha = 0.95f)
                ),
                startY = fgBaseY - amp,
                endY = h
            )
        )

        // Wave crest highlight stroke along top surface
        val crestStrokePath = Path().apply {
            moveTo(0f, waveY(0f))
            var x = 2f
            while (x <= w) {
                lineTo(x, waveY(x))
                x += 2f
            }
        }
        drawPath(
            path = crestStrokePath,
            color = Color.White.copy(alpha = 0.40f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Whitecap foam on crest peaks when wave is moderate to high
        var cx = 0f
        while (cx <= w) {
            val theta = k * cx + (phase * 2f * PI.toFloat())
            val sinVal = sin(theta)
            if (sinVal > 0.70f) {
                val foamAlpha = ((sinVal - 0.70f) / 0.30f).coerceIn(0f, 1f) * (0.6f + strength * 0.4f)
                val cy = waveY(cx)
                drawCircle(
                    color = Color.White.copy(alpha = foamAlpha),
                    radius = (1.2.dp.toPx() + (strength * 1.0.dp.toPx())),
                    center = Offset(cx, cy + 0.5.dp.toPx())
                )
            }
            cx += 4.dp.toPx()
        }

        // 4. Buoyant Nautical Angler's Marker Buoy riding on the wave
        val buoyX = w * 0.65f
        val buoyY = waveY(buoyX)
        val slope = waveSlope(buoyX)
        val pitchDeg = (atan(slope) * (180f / PI.toFloat())).coerceIn(-35f, 35f)

        drawBuoy(
            center = Offset(buoyX, buoyY),
            pitchDeg = pitchDeg,
            accentColor = color
        )
    }
}

/** Draws a buoyant marine marker buoy pitching realistically on the wave surface. */
private fun DrawScope.drawBuoy(
    center: Offset,
    pitchDeg: Float,
    accentColor: Color
) {
    val rad = pitchDeg * (PI.toFloat() / 180f)
    val cosP = cos(rad)
    val sinP = sin(rad)

    fun transform(dx: Float, dy: Float): Offset {
        return Offset(
            center.x + dx * cosP - dy * sinP,
            center.y + dx * sinP + dy * cosP
        )
    }

    // Buoy float hull
    val floatW = 6.dp.toPx()
    val floatH = 7.dp.toPx()
    val hullPath = Path().apply {
        val pTopL = transform(-floatW * 0.4f, -floatH * 0.4f)
        val pTopR = transform(floatW * 0.4f, -floatH * 0.4f)
        val pBotR = transform(floatW * 0.2f, floatH * 0.5f)
        val pBotL = transform(-floatW * 0.2f, floatH * 0.5f)
        moveTo(pTopL.x, pTopL.y)
        lineTo(pTopR.x, pTopR.y)
        lineTo(pBotR.x, pBotR.y)
        lineTo(pBotL.x, pBotL.y)
        close()
    }
    drawPath(hullPath, Color(0xFFFBBF24)) // Bright marine amber

    // White reflective band
    val bandPath = Path().apply {
        val pL1 = transform(-floatW * 0.38f, -floatH * 0.1f)
        val pR1 = transform(floatW * 0.38f, -floatH * 0.1f)
        val pR2 = transform(floatW * 0.32f, floatH * 0.15f)
        val pL2 = transform(-floatW * 0.32f, floatH * 0.15f)
        moveTo(pL1.x, pL1.y)
        lineTo(pR1.x, pR1.y)
        lineTo(pR2.x, pR2.y)
        lineTo(pL2.x, pL2.y)
        close()
    }
    drawPath(bandPath, Color.White.copy(alpha = 0.95f))

    // Mast with beacon LED
    val mastBase = transform(0f, -floatH * 0.4f)
    val mastTip = transform(0f, -floatH * 0.95f)
    drawLine(
        color = Color(0xFFE2E8F0),
        start = mastBase,
        end = mastTip,
        strokeWidth = 1.2.dp.toPx()
    )

    // Beacon light on top (glow aura)
    drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
        radius = 3.dp.toPx(),
        center = mastTip
    )
    drawCircle(
        color = Color.White,
        radius = 1.2.dp.toPx(),
        center = mastTip
    )
}

/**
 * Modern coastal wind turbine animation where blade rotation speed is physically
 * proportional to wind speed (knots), accompanied by aerodynamic wind streamlines,
 * gust surge highlights, and a distant offshore turbine for maritime depth.
 */
@Composable
fun WindAnimation(
    knots: Double,
    force: Int,
    color: Color,
    gusts: Double? = null
) {
    // Rotation period in milliseconds based on wind speed in knots.
    // Higher knots -> shorter period -> faster rotation.
    val periodMillis = when {
        knots < 0.5 -> 12_000
        else -> (48_000 / (knots * 2.8 + 8.0)).toInt().coerceIn(380, 5_500)
    }
    val phase = loopingPhase(periodMillis)
    val streakPeriod = (periodMillis * 0.75f).toInt().coerceIn(320, 3_800)
    val streakPhase = loopingPhase(streakPeriod)
    val hasGust = gusts != null && gusts >= knots + 4.0

    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val w = size.width
        val h = size.height
        val seaBaselineY = h - 2.dp.toPx()

        // 1. Subtle offshore sea baseline
        drawLine(
            color = OceanCardBorder.copy(alpha = 0.7f),
            start = Offset(0f, seaBaselineY),
            end = Offset(w, seaBaselineY),
            strokeWidth = 1.dp.toPx()
        )

        // 2. Aerodynamic wind streamlines flowing past the turbines
        val streakLen = (0.24f + force.coerceAtMost(8) * 0.05f) * w
        val streamRows = listOf(0.20f to 0f, 0.48f to 0.45f, 0.76f to 0.22f)
        streamRows.forEach { (rowFraction, offset) ->
            val y = rowFraction * h
            val head = ((streakPhase + offset) % 1f) * (w + streakLen)
            val startX = (head - streakLen).coerceAtLeast(0f)
            val endX = head.coerceAtMost(w)
            if (endX > startX) {
                drawLine(
                    color = color.copy(alpha = if (force == 0) 0.12f else 0.35f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Gust surge streak across the upper sky
        if (hasGust) {
            val gustStreakLen = 0.42f * w
            val gustHead = (((streakPhase * 1.35f) + 0.12f) % 1f) * (w + gustStreakLen)
            val gStart = (gustHead - gustStreakLen).coerceAtLeast(0f)
            val gEnd = gustHead.coerceAtMost(w)
            if (gEnd > gStart) {
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(gStart, h * 0.32f),
                    end = Offset(gEnd, h * 0.32f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Distant background offshore turbine (smaller, softer alpha for depth)
        drawModernTurbine(
            hub = Offset(w * 0.28f, h * 0.48f),
            baseY = seaBaselineY,
            rotorRadius = h * 0.28f,
            rotationAngle = (phase * 360f * 0.92f + 48f) % 360f,
            alpha = 0.38f,
            accentColor = color
        )

        // 4. Main modern foreground turbine
        drawModernTurbine(
            hub = Offset(w * 0.68f, h * 0.36f),
            baseY = seaBaselineY,
            rotorRadius = h * 0.46f,
            rotationAngle = (phase * 360f) % 360f,
            alpha = 0.95f,
            accentColor = color
        )
    }
}

/** Draws a modern 3-blade aerodynamic wind turbine with tapered tower, nacelle and hub spinner. */
private fun DrawScope.drawModernTurbine(
    hub: Offset,
    baseY: Float,
    rotorRadius: Float,
    rotationAngle: Float,
    alpha: Float,
    accentColor: Color
) {
    val scale = rotorRadius / (48.dp.toPx() * 0.46f)
    val towerTopHalfW = 1.4.dp.toPx() * scale
    val towerBaseHalfW = 3.6.dp.toPx() * scale

    // 1. Tapered tower with metallic gradient
    val towerPath = Path().apply {
        moveTo(hub.x - towerTopHalfW, hub.y + 1.dp.toPx())
        lineTo(hub.x + towerTopHalfW, hub.y + 1.dp.toPx())
        lineTo(hub.x + towerBaseHalfW, baseY)
        lineTo(hub.x - towerBaseHalfW, baseY)
        close()
    }
    drawPath(
        path = towerPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE2E8F0).copy(alpha = alpha * 0.90f),
                Color(0xFF94A3B8).copy(alpha = alpha * 0.70f),
                OceanCardBorder.copy(alpha = alpha * 0.85f)
            ),
            startY = hub.y,
            endY = baseY
        )
    )

    // 2. Aerodynamic Nacelle (housing pod at top of tower)
    val nacelleW = 8.dp.toPx() * scale
    val nacelleH = 3.6.dp.toPx() * scale
    drawRoundRect(
        color = Color(0xFFCBD5E1).copy(alpha = alpha),
        topLeft = Offset(hub.x - nacelleW * 0.55f, hub.y - nacelleH * 0.5f),
        size = Size(nacelleW, nacelleH),
        cornerRadius = CornerRadius(1.8.dp.toPx() * scale)
    )

    // 3. Three aerodynamic rotor blades (120 degrees apart)
    for (i in 0..2) {
        val angleDeg = (rotationAngle + i * 120f) % 360f
        val rad = Math.toRadians(angleDeg.toDouble())
        val ux = kotlin.math.cos(rad).toFloat()
        val uy = kotlin.math.sin(rad).toFloat()
        val vx = -uy
        val vy = ux

        val rootR = 2.dp.toPx() * scale
        val rootP = Offset(hub.x + ux * rootR, hub.y + uy * rootR)
        val midDist = rotorRadius * 0.35f
        val midCenter = Offset(hub.x + ux * midDist, hub.y + uy * midDist)
        val chordW = rotorRadius * 0.12f

        val leadingEdgeP = Offset(midCenter.x + vx * chordW * 0.65f, midCenter.y + vy * chordW * 0.65f)
        val trailingEdgeP = Offset(midCenter.x - vx * chordW * 0.35f, midCenter.y - vy * chordW * 0.35f)
        val tipP = Offset(hub.x + ux * rotorRadius, hub.y + uy * rotorRadius)

        val bladePath = Path().apply {
            moveTo(rootP.x, rootP.y)
            lineTo(leadingEdgeP.x, leadingEdgeP.y)
            lineTo(tipP.x, tipP.y)
            lineTo(trailingEdgeP.x, trailingEdgeP.y)
            close()
        }

        // Blade surface fill
        drawPath(
            path = bladePath,
            color = Color(0xFFF1F5F9).copy(alpha = alpha * 0.90f)
        )
        // Trailing edge shading
        drawLine(
            color = Color(0xFF94A3B8).copy(alpha = alpha * 0.50f),
            start = trailingEdgeP,
            end = tipP,
            strokeWidth = 1.dp.toPx() * scale
        )
        // Leading edge specular highlight
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.95f),
            start = leadingEdgeP,
            end = tipP,
            strokeWidth = 1.2.dp.toPx() * scale
        )
    }

    // 4. Rotor hub spinner (center nose cone)
    val spinnerR = 2.8.dp.toPx() * scale
    drawCircle(
        color = Color(0xFFE2E8F0).copy(alpha = alpha),
        radius = spinnerR,
        center = hub
    )
    drawCircle(
        color = accentColor.copy(alpha = alpha),
        radius = spinnerR * 0.55f,
        center = hub
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.85f),
        radius = spinnerR * 0.25f,
        center = hub
    )
}

/**
 * Modern high-precision marine barometer dial.
 * Features a circular cockpit gauge bezel, tri-zone barometric pressure arc
 * (Storm/Low Alert, Variable, Settled/High Fair), calibrated radial chronometer ticks,
 * a luminous reference mark at standard 1013.25 hPa, a tapered aneroid needle with
 * chrome pivot hub and counterweight, and a dynamic trend chevron capsule that pulses
 * in AlertRed when pressure is falling rapidly.
 */
@Composable
fun PressureGauge(hPa: Double, trendDirection: Int, alarm: Boolean, color: Color) {
    val pulse = if (alarm) loopingPhase(900) else 0f
    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp).clipToBounds()) {
        val w = size.width
        val h = size.height

        val center = Offset(w * 0.38f, h * 0.88f)
        val radius = h * 0.76f

        // 1. Recessed glass dial face; a half disc, since a full one would spill over the row below.
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF132F4C).copy(alpha = 0.50f),
                    Color(0xFF071728).copy(alpha = 0.85f)
                ),
                center = center,
                radius = radius * 1.05f
            ),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        // 2. Tri-zone barometric arc track (180° to 360°)
        // Range: 980 to 1040 hPa (total 60 hPa span)
        // 980 - 1000 hPa: Storm / Low (span 20 hPa = 60°)
        drawArc(
            color = AlertRed.copy(alpha = 0.40f),
            startAngle = 180f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        )
        // 1000 - 1016 hPa: Variable / Changeable (span 16 hPa = 48°)
        drawArc(
            color = CautionYellow.copy(alpha = 0.40f),
            startAngle = 240f,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        )
        // 1016 - 1040 hPa: High / Settled Fair (span 24 hPa = 72°)
        drawArc(
            color = PrimeGreen.copy(alpha = 0.40f),
            startAngle = 288f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Outer bezel ring stroke
        drawArc(
            color = OceanCardBorder.copy(alpha = 0.85f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Calibrated dial ticks
        // Major ticks at 980, 990, 1000, 1010, 1020, 1030, 1040 hPa (every 10 hPa = 30°)
        for (i in 0..6) {
            val tickAngleDeg = 180f + i * 30f
            val rad = tickAngleDeg * (PI.toFloat() / 180f)
            val cosT = cos(rad)
            val sinT = sin(rad)
            val rOuter = radius - 1.5.dp.toPx()
            val rInner = radius - 5.5.dp.toPx()
            drawLine(
                color = Color(0xFFCBD5E1).copy(alpha = 0.65f),
                start = Offset(center.x + rInner * cosT, center.y + rInner * sinT),
                end = Offset(center.x + rOuter * cosT, center.y + rOuter * sinT),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        // Minor ticks every 5 hPa (15°)
        for (i in 0..11) {
            if (i % 2 == 1) {
                val tickAngleDeg = 180f + i * 15f
                val rad = tickAngleDeg * (PI.toFloat() / 180f)
                val cosT = cos(rad)
                val sinT = sin(rad)
                val rOuter = radius - 1.5.dp.toPx()
                val rInner = radius - 3.5.dp.toPx()
                drawLine(
                    color = Color(0xFF94A3B8).copy(alpha = 0.40f),
                    start = Offset(center.x + rInner * cosT, center.y + rInner * sinT),
                    end = Offset(center.x + rOuter * cosT, center.y + rOuter * sinT),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        // Standard atmospheric pressure benchmark (1013.25 hPa) luminous cyan dot
        val stdFraction = ((1013.25 - 980.0) / 60.0).toFloat().coerceIn(0f, 1f)
        val stdRad = (180f + stdFraction * 180f) * (PI.toFloat() / 180f)
        val stdX = center.x + (radius - 3.5.dp.toPx()) * cos(stdRad)
        val stdY = center.y + (radius - 3.5.dp.toPx()) * sin(stdRad)
        drawCircle(
            color = NauticalCyan.copy(alpha = 0.85f),
            radius = 1.6.dp.toPx(),
            center = Offset(stdX, stdY)
        )

        // 4. Aneroid Barometer Needle with Luminous Tip and Counterweight
        val fraction = ((hPa - 980.0) / 60.0).toFloat().coerceIn(0f, 1f)
        val needleAngleDeg = 180f + fraction * 180f
        val needleRad = needleAngleDeg * (PI.toFloat() / 180f)
        val ux = cos(needleRad)
        val uy = sin(needleRad)
        val vx = -uy
        val vy = ux

        val needleLen = radius * 0.88f
        val tipPos = Offset(center.x + ux * needleLen, center.y + uy * needleLen)
        val shoulderDist = radius * 0.20f
        val shoulderHalfW = 1.8.dp.toPx()
        val pLeft = Offset(center.x + ux * shoulderDist + vx * shoulderHalfW, center.y + uy * shoulderDist + vy * shoulderHalfW)
        val pRight = Offset(center.x + ux * shoulderDist - vx * shoulderHalfW, center.y + uy * shoulderDist - vy * shoulderHalfW)

        // Counterweight tail
        val tailLen = radius * 0.22f
        val tailPos = Offset(center.x - ux * tailLen, center.y - uy * tailLen)

        // Needle body path
        val needlePath = Path().apply {
            moveTo(tailPos.x, tailPos.y)
            lineTo(pLeft.x, pLeft.y)
            lineTo(tipPos.x, tipPos.y)
            lineTo(pRight.x, pRight.y)
            close()
        }
        drawPath(
            path = needlePath,
            color = Color.Black.copy(alpha = 0.35f)
        )
        drawPath(
            path = needlePath,
            brush = Brush.linearGradient(
                colors = listOf(color, Color.White.copy(alpha = 0.90f), color),
                start = tailPos,
                end = tipPos
            )
        )
        drawCircle(
            color = Color.White,
            radius = 1.2.dp.toPx(),
            center = tipPos
        )

        // Center pivot hub
        drawCircle(
            color = Color(0xFF475569),
            radius = 4.2.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = 3.0.dp.toPx(),
            center = center
        )
        drawCircle(
            color = color,
            radius = 1.8.dp.toPx(),
            center = center
        )

        // 5. Dynamic Trend Capsule on the right side
        val capsuleX = w * 0.77f
        val capsuleY = h * 0.18f
        val capsuleW = 18.dp.toPx()
        val capsuleH = 28.dp.toPx()

        drawRoundRect(
            color = Color(0xFF0F263E).copy(alpha = 0.75f),
            topLeft = Offset(capsuleX, capsuleY),
            size = Size(capsuleW, capsuleH),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawRoundRect(
            color = if (alarm) AlertRed.copy(alpha = 0.75f) else OceanCardBorder.copy(alpha = 0.60f),
            topLeft = Offset(capsuleX, capsuleY),
            size = Size(capsuleW, capsuleH),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )

        if (alarm) {
            val pulseAlpha = 0.35f + 0.45f * sin(pulse * PI.toFloat())
            drawRoundRect(
                color = AlertRed.copy(alpha = pulseAlpha),
                topLeft = Offset(capsuleX - 2.dp.toPx(), capsuleY - 2.dp.toPx()),
                size = Size(capsuleW + 4.dp.toPx(), capsuleH + 4.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(1.5.dp.toPx())
            )
        }

        val arrowCenterX = capsuleX + capsuleW / 2
        val arrowCenterY = capsuleY + capsuleH / 2
        val arrowW = 4.5.dp.toPx()

        when {
            trendDirection > 0 -> {
                val arrowPath = Path().apply {
                    moveTo(arrowCenterX - arrowW, arrowCenterY + 4.dp.toPx())
                    lineTo(arrowCenterX, arrowCenterY - 4.dp.toPx())
                    lineTo(arrowCenterX + arrowW, arrowCenterY + 4.dp.toPx())
                }
                drawPath(
                    path = arrowPath,
                    color = PrimeGreen,
                    style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            trendDirection < 0 -> {
                val arrowPath = Path().apply {
                    moveTo(arrowCenterX - arrowW, arrowCenterY - 4.dp.toPx())
                    lineTo(arrowCenterX, arrowCenterY + 4.dp.toPx())
                    lineTo(arrowCenterX + arrowW, arrowCenterY - 4.dp.toPx())
                }
                drawPath(
                    path = arrowPath,
                    color = if (alarm) AlertRed else CautionYellow,
                    style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
                )
                if (alarm) {
                    val secondPath = Path().apply {
                        moveTo(arrowCenterX - arrowW, arrowCenterY - 8.dp.toPx())
                        lineTo(arrowCenterX, arrowCenterY)
                        lineTo(arrowCenterX + arrowW, arrowCenterY - 8.dp.toPx())
                    }
                    drawPath(
                        path = secondPath,
                        color = AlertRed,
                        style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            else -> {
                drawLine(
                    color = NauticalCyan.copy(alpha = 0.85f),
                    start = Offset(arrowCenterX - arrowW, arrowCenterY),
                    end = Offset(arrowCenterX + arrowW, arrowCenterY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = NauticalCyan,
                    radius = 1.5.dp.toPx(),
                    center = Offset(arrowCenterX, arrowCenterY)
                )
            }
        }
    }
}

/**
 * Modern hydrodynamic ocean flow field and angler drift sinker visualization.
 * Features laminar flow streamlines whose velocity directly reflects current speed (knots),
 * luminous suspended marine plankton particles, a sandy seabed gradient, and an angler's
 * submerged fishing line with sinker (ladong) whose drift deflection angle visually
 * demonstrates the current's dragging force.
 */
@Composable
fun CurrentAnimation(
    knots: Double,
    color: Color
) {
    val periodMillis = when {
        knots < 0.2 -> 9_000
        else -> (36_000 / (knots * 3.2 + 6.0)).toInt().coerceIn(400, 5_000)
    }
    val phase = loopingPhase(periodMillis)
    val microPhase = loopingPhase((periodMillis * 0.7f).toInt().coerceAtLeast(300))

    Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
        val w = size.width
        val h = size.height

        // 1. Water column gradient with deep seabed base
        val waterBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0C2B47).copy(alpha = 0.45f),
                Color(0xFF061E34).copy(alpha = 0.75f),
                Color(0xFF020E1A).copy(alpha = 0.95f)
            ),
            startY = 0f,
            endY = h
        )
        drawRect(brush = waterBrush)

        // Seabed ripple contour at bottom
        val seabedPath = Path().apply {
            val baseY = h - 3.dp.toPx()
            moveTo(0f, h)
            lineTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                val y = baseY + 1.2.dp.toPx() * sin((x / w * 4 * PI).toFloat())
                lineTo(x, y)
                x += 3.dp.toPx()
            }
            lineTo(w, h)
            close()
        }
        drawPath(
            path = seabedPath,
            brush = Brush.verticalGradient(
                colors = listOf(OceanCardBorder.copy(alpha = 0.50f), Color(0xFF010A14)),
                startY = h - 4.dp.toPx(),
                endY = h
            )
        )

        // 2. Hydrodynamic laminar flow streamlines
        val lanes = listOf(
            Triple(0.20f, 0.00f, 1.15f),
            Triple(0.38f, 0.42f, 1.00f),
            Triple(0.56f, 0.18f, 0.92f),
            Triple(0.74f, 0.65f, 0.80f)
        )

        val streamLen = (0.22f + (knots.toFloat().coerceIn(0.1f, 2.5f) / 2.5f) * 0.26f) * w

        lanes.forEach { (yFraction, offset, speedMult) ->
            val y = yFraction * h
            val lanePhase = ((phase * speedMult + offset) % 1f)
            val headX = lanePhase * (w + streamLen)
            val tailX = (headX - streamLen).coerceAtLeast(0f)
            val boundedHeadX = headX.coerceAtMost(w)

            if (boundedHeadX > tailX) {
                val streakBrush = Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = 0.04f),
                        color.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.85f)
                    ),
                    start = Offset(tailX, y),
                    end = Offset(boundedHeadX, y)
                )
                drawLine(
                    brush = streakBrush,
                    start = Offset(tailX, y),
                    end = Offset(boundedHeadX, y),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Glowing micro-plankton particles drifting along the current
        val particles = listOf(
            Pair(0.15f, 0.28f),
            Pair(0.48f, 0.52f),
            Pair(0.82f, 0.35f),
            Pair(0.28f, 0.68f),
            Pair(0.65f, 0.15f)
        )
        particles.forEachIndexed { idx, (normX, normY) ->
            val pPhase = ((microPhase + normX) % 1f)
            val px = pPhase * w
            val py = (normY * h) + 1.5.dp.toPx() * sin((pPhase * 2 * PI + idx).toFloat())
            val pAlpha = (0.35f + 0.45f * sin((pPhase * PI).toFloat())).coerceIn(0f, 1f)

            drawCircle(
                color = color.copy(alpha = pAlpha * 0.40f),
                radius = 2.5.dp.toPx(),
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White.copy(alpha = pAlpha * 0.90f),
                radius = 1.0.dp.toPx(),
                center = Offset(px, py)
            )
        }

        // 4. Tactical Angler Sinker Rig (Ladong Pancing Hanyut)
        val lineOriginX = w * 0.22f
        val lineOriginY = 1.dp.toPx()

        val driftDx = (knots.toFloat().coerceIn(0f, 3f) * 11.dp.toPx()).coerceIn(2.dp.toPx(), 26.dp.toPx())
        val sinkerY = h * 0.72f
        val sinkerX = lineOriginX + driftDx

        val linePath = Path().apply {
            moveTo(lineOriginX, lineOriginY)
            val ctrlX = lineOriginX + driftDx * 0.65f
            val ctrlY = sinkerY * 0.55f
            quadraticTo(ctrlX, ctrlY, sinkerX, sinkerY)
        }
        drawPath(
            path = linePath,
            color = Color.White.copy(alpha = 0.55f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )

        drawSinker(
            center = Offset(sinkerX, sinkerY)
        )

        val lureLen = 9.dp.toPx()
        val flutter = sin(phase * 4f * PI.toFloat()) * 1.5.dp.toPx()
        val lureTipX = sinkerX + lureLen
        val lureTipY = sinkerY + flutter

        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(sinkerX, sinkerY),
            end = Offset(lureTipX, lureTipY),
            strokeWidth = 0.8.dp.toPx()
        )
        drawCircle(
            color = NauticalCyan,
            radius = 1.2.dp.toPx(),
            center = Offset(lureTipX, lureTipY)
        )
    }
}

/** Draws a metallic pear sinker weight. */
private fun DrawScope.drawSinker(center: Offset) {
    val sinkerW = 3.2.dp.toPx()
    val sinkerH = 5.5.dp.toPx()
    val sinkerPath = Path().apply {
        moveTo(center.x, center.y - sinkerH * 0.5f)
        lineTo(center.x + sinkerW * 0.5f, center.y + sinkerH * 0.2f)
        lineTo(center.x, center.y + sinkerH * 0.5f)
        lineTo(center.x - sinkerW * 0.5f, center.y + sinkerH * 0.2f)
        close()
    }
    drawPath(
        path = sinkerPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B), Color(0xFF334155)),
            startY = center.y - sinkerH * 0.5f,
            endY = center.y + sinkerH * 0.5f
        )
    )
    drawLine(
        color = Color.White.copy(alpha = 0.85f),
        start = Offset(center.x - 0.5.dp.toPx(), center.y - sinkerH * 0.4f),
        end = Offset(center.x - 0.5.dp.toPx(), center.y + sinkerH * 0.2f),
        strokeWidth = 0.8.dp.toPx()
    )
}
