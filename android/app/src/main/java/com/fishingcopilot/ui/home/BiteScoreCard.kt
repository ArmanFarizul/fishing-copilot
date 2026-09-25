package com.fishingcopilot.ui.home

import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.components.biteBandColor
import com.fishingcopilot.ui.components.ClearSelectionButton
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.bite.BiteForecast
import com.fishingcopilot.bite.BiteScore
import com.fishingcopilot.bite.BiteTimeline
import com.fishingcopilot.bite.LightFactor
import com.fishingcopilot.bite.TideFactor
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class FactorIcon { TIDE, LIGHT, MOON, PRESSURE }

@Composable
fun BiteScoreCard(forecast: BiteForecast?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = CardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.bite_card_title).uppercase(locale),
                style = MaterialTheme.typography.labelMedium,
                color = NauticalCyan
            )
            if (forecast == null) return@Column
            val moment = forecast.now
            val score = moment.score.score
            val prime = score >= BiteTimeline.PRIME_THRESHOLD
            val band = stringResource(bandLabel(score))
            val scoreText = String.format(locale, "%.1f", score)

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadialBiteGauge(
                    score = score,
                    scoreText = scoreText,
                    isPrimeTime = prime,
                    description = stringResource(R.string.bite_score_description, scoreText, band)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = band,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = biteBandColor(score)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = bestWindowText(forecast, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    // Tactical Angler Advice Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = biteBandColor(score).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, biteBandColor(score).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = stringResource(tacticalAdvice(score)),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = biteBandColor(score),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FactorRow(
                    label = R.string.bite_factor_tide,
                    weight = BiteScore.TIDE_WEIGHT,
                    value = moment.score.tide,
                    iconType = FactorIcon.TIDE,
                    reason = moment.tide?.let {
                        when (it) {
                            TideFactor.SLACK -> R.string.bite_tide_slack
                            TideFactor.MOVING -> R.string.bite_tide_moving
                            TideFactor.MID_CYCLE -> R.string.bite_tide_mid
                        }
                    }
                )
                FactorRow(
                    label = R.string.bite_factor_light,
                    weight = BiteScore.LIGHT_WEIGHT,
                    value = moment.score.light,
                    iconType = FactorIcon.LIGHT,
                    reason = moment.light?.let {
                        when (it) {
                            LightFactor.GOLDEN -> R.string.bite_light_golden
                            LightFactor.NIGHT -> R.string.bite_light_night
                            LightFactor.MIDDAY -> R.string.bite_light_midday
                            LightFactor.DAYLIGHT -> R.string.bite_light_daylight
                        }
                    }
                )
                FactorRow(
                    label = R.string.bite_factor_moon,
                    weight = BiteScore.SOLUNAR_WEIGHT,
                    value = moment.score.solunar,
                    iconType = FactorIcon.MOON,
                    reason = moment.tideStrength?.let {
                        when (it) {
                            TideStrength.SPRING -> R.string.bite_moon_spring
                            TideStrength.NEAP -> R.string.bite_moon_neap
                            TideStrength.NORMAL -> R.string.bite_moon_normal
                        }
                    }
                )
                FactorRow(
                    label = R.string.bite_factor_pressure,
                    weight = BiteScore.BARO_WEIGHT,
                    value = moment.score.baro,
                    iconType = FactorIcon.PRESSURE,
                    reason = moment.pressureTrend?.let { trend ->
                        when (trend) {
                            PressureTrend.STEADY ->
                                if ((moment.pressureHpa ?: 0.0) in 1010.0..1015.0) R.string.bite_pressure_ideal else R.string.bite_pressure_steady
                            PressureTrend.RISING -> R.string.bite_pressure_rising
                            PressureTrend.FALLING -> R.string.bite_pressure_falling
                            PressureTrend.FALLING_FAST -> R.string.bite_pressure_falling_fast
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.bite_timeline_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextHighContrast
            )
            // Own line: beside the title the Malay hint wrapped into a squeezed column.
            Text(
                text = stringResource(R.string.bite_timeline_scrub_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            ScoreTimeline(
                forecast = forecast,
                description = stringResource(R.string.bite_timeline_description, bestWindowText(forecast, locale)),
                locale = locale
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.bite_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

/**
 * Modern radial activity gauge.
 * Features a circular 240° progress arc, calibrated gauge track,
 * luminous progress head dot, pulsing ambient glow during prime times, and bold typography.
 */
@Composable
private fun RadialBiteGauge(
    score: Double,
    scoreText: String,
    isPrimeTime: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val pulseAlpha = if (isPrimeTime && !animationsOff) {
        val transition = rememberInfiniteTransition(label = "pulseTransition")
        val alpha by transition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseAlpha"
        )
        alpha
    } else if (isPrimeTime) 0.20f else 0f

    val color = biteBandColor(score)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(88.dp)
            .semantics { contentDescription = description }
    ) {
        Canvas(modifier = Modifier.size(88.dp)) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2, h / 2)
            val strokeW = 6.dp.toPx()
            val radius = (w - strokeW) / 2 - 2.dp.toPx()

            // 1. Prime background radial glow
            if (isPrimeTime) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimeGreen.copy(alpha = pulseAlpha), Color.Transparent),
                        center = center,
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = center
                )
            }

            // 2. Dial track arc (240 degrees sweep: 150° to 390°)
            val startAngle = 150f
            val totalSweep = 240f
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = OceanCardBorder.copy(alpha = 0.70f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // 3. Active score progress arc
            val fraction = ((score - 1.0) / 9.0).coerceIn(0.04, 1.0).toFloat()
            val activeSweep = totalSweep * fraction

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // 4. Glowing head dot at leading edge of arc
            val headAngleDeg = startAngle + activeSweep
            val headRad = headAngleDeg * (PI.toFloat() / 180f)
            val headX = center.x + radius * cos(headRad)
            val headY = center.y + radius * sin(headRad)

            drawCircle(
                color = color.copy(alpha = 0.40f),
                radius = 5.5.dp.toPx(),
                center = Offset(headX, headY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.2.dp.toPx(),
                center = Offset(headX, headY)
            )
        }

        // Center Score Typography
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = scoreText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (isPrimeTime) PrimeGreen else TextHighContrast
            )
        }
    }
}

/** Draws custom Canvas vector glyphs for the 4 bite factors. */
@Composable
private fun FactorGlyph(type: FactorIcon, color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        when (type) {
            FactorIcon.TIDE -> {
                val p1 = Path().apply {
                    moveTo(0f, h * 0.40f)
                    cubicTo(w * 0.25f, h * 0.15f, w * 0.75f, h * 0.65f, w, h * 0.40f)
                }
                val p2 = Path().apply {
                    moveTo(0f, h * 0.72f)
                    cubicTo(w * 0.25f, h * 0.47f, w * 0.75f, h * 0.97f, w, h * 0.72f)
                }
                drawPath(p1, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                drawPath(p2, color.copy(alpha = 0.65f), style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
            }
            FactorIcon.LIGHT -> {
                val horizonY = h * 0.68f
                drawLine(
                    color = color.copy(alpha = 0.50f),
                    start = Offset(0f, horizonY),
                    end = Offset(w, horizonY),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.24f, horizonY - w * 0.26f),
                    size = Size(w * 0.52f, w * 0.52f)
                )
                val rayLen = 2.5.dp.toPx()
                val sunCenter = Offset(w * 0.5f, horizonY)
                listOf(-45f, -90f, -135f).forEach { deg ->
                    val rad = deg * (PI.toFloat() / 180f)
                    val rInner = w * 0.35f
                    val rOuter = rInner + rayLen
                    drawLine(
                        color = color,
                        start = Offset(sunCenter.x + rInner * cos(rad), sunCenter.y + rInner * sin(rad)),
                        end = Offset(sunCenter.x + rOuter * cos(rad), sunCenter.y + rOuter * sin(rad)),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            FactorIcon.MOON -> {
                val center = Offset(w * 0.5f, h * 0.5f)
                val r = w * 0.40f
                val crescentPath = Path().apply {
                    arcTo(
                        rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = true
                    )
                    cubicTo(
                        center.x + r * 0.25f, center.y - r * 0.6f,
                        center.x + r * 0.25f, center.y + r * 0.6f,
                        center.x, center.y + r
                    )
                    close()
                }
                drawPath(crescentPath, color)
            }
            FactorIcon.PRESSURE -> {
                val center = Offset(w * 0.5f, h * 0.55f)
                val r = w * 0.40f
                drawArc(
                    color = color.copy(alpha = 0.55f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)
                )
                val needleRad = -65f * (PI.toFloat() / 180f)
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x + r * 0.85f * cos(needleRad), center.y + r * 0.85f * sin(needleRad)),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color, radius = 2.dp.toPx(), center = center)
            }
        }
    }
}

@Composable
private fun FactorRow(
    @StringRes label: Int,
    weight: Double,
    value: Double?,
    iconType: FactorIcon,
    @StringRes reason: Int?
) {
    // No data is shown grey, like its "no data yet" line, rather than as a middling value.
    val factorCol = value?.let(::factorColor) ?: TextMuted

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(110.dp)
        ) {
            FactorGlyph(iconType, factorCol)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHighContrast,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.bite_factor_weight, (weight * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                // Background Track
                drawRoundRect(
                    color = OceanMidnight,
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawRoundRect(
                    color = OceanCardBorder.copy(alpha = 0.6f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(0.8.dp.toPx())
                )

                // Filled Value Bar with Gradient
                if (value != null) {
                    val activeW = (size.width * value.toFloat()).coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(factorCol.copy(alpha = 0.70f), factorCol)
                        ),
                        size = Size(activeW, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    // Luminous Head Dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.90f),
                        radius = 2.dp.toPx(),
                        center = Offset(activeW - 2.dp.toPx(), size.height / 2)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(if (value == null || reason == null) R.string.bite_factor_missing else reason),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

/**
 * 24-hour interactive Bezier spline area chart with touch scrubber,
 * golden prime window radiant backdrop, calibrated 24h time axis, and interactive tooltip HUD.
 */
@Composable
private fun ScoreTimeline(
    forecast: BiteForecast,
    description: String,
    locale: Locale
) {
    val points = forecast.points
    val best = forecast.best
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Scrubber Tooltip Header (when user touches the graph)
        if (scrubbedIndex != null && scrubbedIndex in points.indices) {
            val pt = points[scrubbedIndex!!]
            val ptTime = clockTime(pt.epochMillis, locale)
            val ptScoreText = String.format(locale, "%.1f", pt.score)
            val ptBand = stringResource(bandLabel(pt.score))
            val ptColor = biteBandColor(pt.score)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ptColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ptColor)
                ) {
                    Text(
                        text = stringResource(R.string.bite_timeline_scrub_selected, ptTime, ptScoreText, ptBand),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ptColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                ClearSelectionButton(onClick = { scrubbedIndex = null })
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onTap = { offset ->
                                val idx = ((offset.x / size.width) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                scrubbedIndex = idx
                            }
                        )
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val idx = ((offset.x / size.width) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                scrubbedIndex = idx
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val idx = ((change.position.x / size.width) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                scrubbedIndex = idx
                            },
                            onDragEnd = { /* keep active */ },
                            onDragCancel = { /* keep active */ }
                        )
                    }
                    .semantics { contentDescription = description }
            ) {
                if (points.isEmpty()) return@Canvas
                val w = size.width
                val h = size.height
                val topPadding = 12.dp.toPx()
                val bottomPadding = 4.dp.toPx()
                val chartHeight = h - topPadding - bottomPadding

                fun getX(index: Int): Float = (index.toFloat() / (points.size - 1)) * w
                fun getY(scoreVal: Double): Float {
                    val norm = ((scoreVal - 1.0) / 9.0).coerceIn(0.0, 1.0).toFloat()
                    return h - bottomPadding - (norm * chartHeight)
                }

                // 1. Highlight Prime Time Window Background Zone
                if (best != null) {
                    val firstIdx = points.indexOfFirst { it.epochMillis >= best.start }.coerceAtLeast(0)
                    val lastIdx = points.indexOfLast { it.epochMillis <= best.end }.coerceAtLeast(firstIdx)
                    val x1 = getX(firstIdx)
                    val x2 = getX(lastIdx).coerceAtLeast(x1 + 8.dp.toPx())

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PrimeGreen.copy(alpha = 0.22f),
                                PrimeGreen.copy(alpha = 0.05f)
                            ),
                            startY = 0f,
                            endY = h
                        ),
                        topLeft = Offset(x1, 0f),
                        size = Size(x2 - x1, h),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    drawRoundRect(
                        color = PrimeGreen.copy(alpha = 0.40f),
                        topLeft = Offset(x1, 0f),
                        size = Size(x2 - x1, h),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(1.dp.toPx())
                    )
                }

                // 2. Smooth Cubic Bezier Area Path
                val areaPath = Path()
                val strokePath = Path()

                val p0 = Offset(getX(0), getY(points[0].score))
                areaPath.moveTo(0f, h)
                areaPath.lineTo(p0.x, p0.y)
                strokePath.moveTo(p0.x, p0.y)

                for (i in 0 until points.size - 1) {
                    val xPrev = getX(i)
                    val yPrev = getY(points[i].score)
                    val xNext = getX(i + 1)
                    val yNext = getY(points[i + 1].score)

                    val midX = (xPrev + xNext) / 2f
                    areaPath.cubicTo(midX, yPrev, midX, yNext, xNext, yNext)
                    strokePath.cubicTo(midX, yPrev, midX, yNext, xNext, yNext)
                }

                areaPath.lineTo(w, h)
                areaPath.close()

                // Area Gradient Fill
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimeGreen.copy(alpha = 0.35f),
                            NauticalCyan.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                // Spline Line Stroke
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = points.map { factorColor((it.score - 1.0) / 9.0) }
                    ),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Current Time ("Now") Marker
                val nowX = getX(0)
                drawLine(
                    color = NauticalCyan.copy(alpha = 0.70f),
                    start = Offset(nowX, 0f),
                    end = Offset(nowX, h),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                // 4. Interactive Scrubber Cursor (when active)
                if (scrubbedIndex != null && scrubbedIndex in points.indices) {
                    val idx = scrubbedIndex!!
                    val scrubX = getX(idx)
                    val scrubY = getY(points[idx].score)
                    val scrubColor = biteBandColor(points[idx].score)

                    // Vertical Scrubber Line
                    drawLine(
                        color = scrubColor.copy(alpha = 0.85f),
                        start = Offset(scrubX, 0f),
                        end = Offset(scrubX, h),
                        strokeWidth = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )

                    // Luminous Cursor Halo Dot
                    drawCircle(
                        color = scrubColor.copy(alpha = 0.40f),
                        radius = 6.dp.toPx(),
                        center = Offset(scrubX, scrubY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(scrubX, scrubY)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 5. 24-Hour Time Axis Labels
        if (points.size >= 96) {
            val labelIndices = listOf(0, 24, 48, 72, 96.coerceAtMost(points.lastIndex))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labelIndices.forEachIndexed { i, idx ->
                    val text = if (i == 0) {
                        stringResource(R.string.bite_timeline_now)
                    } else {
                        clockTime(points[idx].epochMillis, locale)
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == 0) NauticalCyan else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun bestWindowText(forecast: BiteForecast, locale: Locale): String {
    val best = forecast.best ?: return stringResource(R.string.bite_no_prime_window)
    return stringResource(
        R.string.bite_best_window,
        clockTime(best.start, locale),
        clockTime(best.end, locale),
        String.format(locale, "%.1f", best.peak)
    )
}

@StringRes
private fun tacticalAdvice(score: Double): Int = when {
    score >= BiteTimeline.PRIME_THRESHOLD -> R.string.bite_advice_prime
    score >= 6.0 -> R.string.bite_advice_good
    score >= 4.0 -> R.string.bite_advice_fair
    else -> R.string.bite_advice_poor
}

@StringRes
private fun bandLabel(score: Double): Int = when {
    score >= BiteTimeline.PRIME_THRESHOLD -> R.string.bite_band_prime
    score >= 6 -> R.string.bite_band_good
    score >= 4 -> R.string.bite_band_fair
    else -> R.string.bite_band_poor
}

/** Colour for a 0..1 factor value, from red through yellow to green. */
private fun factorColor(value: Double): Color = when {
    value >= 0.75 -> PrimeGreen
    value >= 0.5 -> NauticalCyan
    value >= 0.3 -> CautionYellow
    else -> AlertRed
}

private fun clockTime(epochMillis: Long, locale: Locale): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
