package com.fishingcopilot.ui.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.tide.SeaLevelSample
import com.fishingcopilot.tide.TideEvent
import com.fishingcopilot.tide.TideEventType
import com.fishingcopilot.tide.TideSummary
import com.fishingcopilot.ui.components.ClearSelectionButton
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.InsetBorder
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
import kotlin.math.abs
import kotlin.math.roundToInt

private const val OFFSET_LIMIT_MINUTES = 60
private const val OFFSET_STEP_MINUTES = 5

@Composable
fun TideCard(
    state: HomeUiState,
    onRetry: () -> Unit,
    onUseNearestArea: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    onOffsetCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = CardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.tide_card_title).uppercase(currentLocale()),
                    style = MaterialTheme.typography.labelMedium,
                    color = NauticalCyan
                )
                Spacer(Modifier.width(8.dp))
                state.spotName?.let {
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
                }
            }
            Spacer(Modifier.height(12.dp))

            when (val tide = state.tide) {
                TideCardState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = NauticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.tide_loading), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                TideCardState.Offline -> {
                    Text(stringResource(R.string.tide_error_offline), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.tide_retry), color = NauticalCyan) }
                }
                is TideCardState.NoMarineData -> {
                    Text(stringResource(R.string.tide_no_marine_data), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onUseNearestArea,
                        colors = ButtonDefaults.buttonColors(containerColor = NauticalCyan, contentColor = OceanMidnight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.tide_use_nearest_area, stringResource(tide.nearest.label), tide.distanceKm.roundToInt()),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is TideCardState.Ready -> ReadyContent(
                    summary = tide.summary,
                    now = state.now,
                    offsetMinutes = state.offsetMinutes,
                    onOffsetChange = onOffsetChange,
                    onOffsetCommit = onOffsetCommit
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    summary: TideSummary,
    now: Long,
    offsetMinutes: Int,
    onOffsetChange: (Int) -> Unit,
    onOffsetCommit: () -> Unit
) {
    val locale = currentLocale()
    val trendColor = if (summary.rising) PrimeGreen else NauticalCyan

    // 1. Water Status Headline with Flow Direction Arrow
    Row(verticalAlignment = Alignment.CenterVertically) {
        FlowDirectionBadge(rising = summary.rising, color = trendColor)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(if (summary.rising) R.string.tide_rising else R.string.tide_falling),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighContrast
            )
            Text(
                text = stringResource(R.string.tide_height_relative, signedMeters(summary.heightNow, locale)),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    // 2. Tactical Angler Advice Badge
    val tacticalAdviceRes = when {
        // Slack is about time to the turn, not height: at mean level the current runs hardest.
        summary.slack -> R.string.tide_advice_slack
        summary.rising -> R.string.tide_advice_rising
        else -> R.string.tide_advice_falling
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = trendColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, trendColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = stringResource(tacticalAdviceRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = trendColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }

    Spacer(Modifier.height(14.dp))

    // 3. Next High & Next Low Interactive Event Tiles
    val nextHigh = summary.nextHigh
    val nextLow = summary.nextLow
    val nextUpIsHigh = when {
        nextHigh != null && nextLow != null -> nextHigh.epochMillis < nextLow.epochMillis
        nextHigh != null -> true
        else -> false
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        nextHigh?.let {
            NextEvent(
                event = it,
                now = now,
                isNextUp = nextUpIsHigh,
                modifier = Modifier.weight(1f)
            )
        }
        nextLow?.let {
            NextEvent(
                event = it,
                now = now,
                isNextUp = !nextUpIsHigh,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    // 4. Interactive 24-Hour Tide Curve with Scrubber & Time Axis
    TideChart(summary = summary, now = now, locale = locale)

    Spacer(Modifier.height(16.dp))

    // 5. Ergonomic Adjust Timing Controls (Presets + Slider)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.tide_offset_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextHighContrast
        )
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (offsetMinutes != 0) NauticalCyan.copy(alpha = 0.18f) else OceanMidnight,
            border = BorderStroke(1.dp, if (offsetMinutes != 0) NauticalCyan else OceanCardBorder)
        ) {
            Text(
                text = stringResource(R.string.tide_offset_value, offsetMinutes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (offsetMinutes != 0) NauticalCyan else TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    // Quick Offset Presets
    val presets = listOf(-30, 0, 30, 60)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        presets.forEach { preset ->
            val isSelected = offsetMinutes == preset
            val presetLabel = if (preset == 0) stringResource(R.string.tide_preset_zero)
            else stringResource(R.string.tide_offset_value, preset)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) NauticalCyan.copy(alpha = 0.20f) else OceanMidnight,
                border = BorderStroke(1.dp, if (isSelected) NauticalCyan else OceanCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) {
                        onOffsetChange(preset)
                        onOffsetCommit()
                    }
            ) {
                Text(
                    text = presetLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) NauticalCyan else TextMuted,
                    modifier = Modifier.wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Slider(
        value = offsetMinutes.toFloat(),
        onValueChange = { onOffsetChange((it / OFFSET_STEP_MINUTES).roundToInt() * OFFSET_STEP_MINUTES) },
        onValueChangeFinished = onOffsetCommit,
        valueRange = -OFFSET_LIMIT_MINUTES.toFloat()..OFFSET_LIMIT_MINUTES.toFloat(),
        steps = 2 * OFFSET_LIMIT_MINUTES / OFFSET_STEP_MINUTES - 1,
        colors = SliderDefaults.colors(
            thumbColor = NauticalCyan,
            activeTrackColor = NauticalCyan,
            inactiveTrackColor = OceanCardBorder,
            activeTickColor = OceanMidnight,
            inactiveTickColor = OceanMidnight
        )
    )
    Text(stringResource(R.string.tide_offset_help), style = MaterialTheme.typography.bodySmall, color = TextMuted)

    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.tide_source), style = MaterialTheme.typography.labelSmall, color = TextMuted.copy(alpha = 0.8f))
}

/** Animated / styled flow direction chevron badge. */
@Composable
private fun FlowDirectionBadge(rising: Boolean, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.60f)),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    if (rising) {
                        moveTo(w * 0.5f, h * 0.15f)
                        lineTo(w * 0.9f, h * 0.85f)
                        lineTo(w * 0.5f, h * 0.65f)
                        lineTo(w * 0.1f, h * 0.85f)
                    } else {
                        moveTo(w * 0.5f, h * 0.85f)
                        lineTo(w * 0.9f, h * 0.15f)
                        lineTo(w * 0.5f, h * 0.35f)
                        lineTo(w * 0.1f, h * 0.15f)
                    }
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}

/** Next High or Low tide card with water glyph, height, countdown, and active highlight. */
@Composable
private fun NextEvent(
    event: TideEvent,
    now: Long,
    isNextUp: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val time = Instant.ofEpochMilli(event.epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    val minutes = ((event.epochMillis - now) / 60_000).toInt().coerceAtLeast(0)
    val countdown = if (minutes >= 60) {
        stringResource(R.string.tide_countdown_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.tide_countdown_minutes, minutes)
    }

    val isHigh = event.type == TideEventType.HIGH
    val accentColor = if (isHigh) PrimeGreen else NauticalCyan
    val heightFormatted = signedMeters(event.height, locale)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = OceanMidnight,
        border = if (isNextUp) BorderStroke(1.2.dp, accentColor) else InsetBorder,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        if (isHigh) {
                            // High tide water crest icon
                            val p = Path().apply {
                                moveTo(0f, size.height * 0.6f)
                                cubicTo(size.width * 0.25f, 0f, size.width * 0.75f, size.height, size.width, size.height * 0.4f)
                            }
                            drawPath(p, accentColor, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                        } else {
                            // Low tide anchor / basin icon
                            val p = Path().apply {
                                moveTo(size.width * 0.15f, size.height * 0.4f)
                                lineTo(size.width * 0.5f, size.height * 0.85f)
                                lineTo(size.width * 0.85f, size.height * 0.4f)
                            }
                            drawPath(p, accentColor, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (isHigh) R.string.tide_event_high else R.string.tide_event_low),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }
                if (isNextUp) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = stringResource(R.string.tide_nearest_event),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = heightFormatted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
            Text(
                text = countdown,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

/**
 * 24-hour interactive tidal wave curve.
 * Features mean sea level 0.0m reference, smooth wave area with tidal run highlights,
 * calibrated 24h time axis, and interactive touch scrubber with floating HUD.
 */
@Composable
private fun TideChart(summary: TideSummary, now: Long, locale: Locale) {
    val description = stringResource(R.string.tide_chart_description)
    val nowLabel = stringResource(R.string.tide_now)
    val curve = summary.curve
    if (curve.isEmpty()) return

    val start = curve.first().epochMillis
    val span = (curve.last().epochMillis - start).toFloat()
    val maxAbs = curve.maxOf { abs(it.height) }.coerceAtLeast(0.2)

    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // The hint sits where the picked-time chip appears, so the card does not jump when a time is picked.
        if (scrubbedIndex == null) {
            Text(
                text = stringResource(R.string.tide_scrub_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        if (scrubbedIndex != null && scrubbedIndex in curve.indices) {
            val sample = curve[scrubbedIndex!!]
            val sTime = clockTime(sample.epochMillis, locale)
            val sHeight = signedMeters(sample.height, locale)
            val isRising = if (scrubbedIndex!! < curve.lastIndex) {
                curve[scrubbedIndex!! + 1].height >= sample.height
            } else {
                summary.rising
            }
            val sState = stringResource(if (isRising) R.string.tide_rising else R.string.tide_falling)
            val sColor = if (isRising) PrimeGreen else NauticalCyan

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = sColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, sColor)
                ) {
                    Text(
                        text = stringResource(R.string.tide_scrub_selected, sTime, sHeight, sState),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = sColor,
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
                    .height(115.dp)
                    .pointerInput(curve) {
                        detectTapGestures(
                            onTap = { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                scrubbedIndex = (fraction * curve.lastIndex).roundToInt()
                            }
                        )
                    }
                    .pointerInput(curve) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                scrubbedIndex = (fraction * curve.lastIndex).roundToInt()
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                scrubbedIndex = (fraction * curve.lastIndex).roundToInt()
                            },
                            onDragEnd = { /* keep active */ },
                            onDragCancel = { /* keep active */ }
                        )
                    }
                    .semantics { contentDescription = description }
            ) {
                val padY = 14.dp.toPx()
                val chartHeight = size.height - 2 * padY
                fun x(t: Long) = ((t - start) / span) * size.width
                fun y(h: Double) = (padY + (1 - (h / maxAbs + 1) / 2) * chartHeight).toFloat()

                // 1. Mean Sea Level Reference Line (0.0 m)
                val zeroY = y(0.0)
                drawLine(
                    color = OceanCardBorder.copy(alpha = 0.85f),
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 1.dp.toPx()
                )

                // 2. Smooth Tide Curve & Area Fill
                val linePath = Path()
                curve.forEachIndexed { i, s ->
                    val px = x(s.epochMillis)
                    val py = y(s.height)
                    if (i == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
                }

                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NauticalCyan.copy(alpha = 0.35f),
                            NauticalCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = padY,
                        endY = size.height
                    )
                )

                // Stroke wave line
                drawPath(
                    path = linePath,
                    color = NauticalCyan,
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Next High and Next Low event nodes on the curve
                listOfNotNull(summary.nextHigh, summary.nextLow)
                    .filter { it.epochMillis in start..curve.last().epochMillis }
                    .forEach { event ->
                        val ex = x(event.epochMillis)
                        val ey = y(event.height)
                        val isHigh = event.type == TideEventType.HIGH
                        val dotColor = if (isHigh) PrimeGreen else NauticalCyan

                        drawCircle(
                            color = dotColor.copy(alpha = 0.35f),
                            radius = 6.dp.toPx(),
                            center = Offset(ex, ey)
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(ex, ey)
                        )
                    }

                // 4. Current Time ("Now") Marker
                val nowX = x(now).coerceIn(0f, size.width)
                val nowY = y(summary.heightNow)
                drawLine(
                    color = TextHighContrast.copy(alpha = 0.75f),
                    start = Offset(nowX, 0f),
                    end = Offset(nowX, size.height),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )
                drawCircle(
                    color = TextHighContrast.copy(alpha = 0.40f),
                    radius = 6.dp.toPx(),
                    center = Offset(nowX, nowY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(nowX, nowY)
                )

                // 5. Interactive Scrubber Marker (when active)
                if (scrubbedIndex != null && scrubbedIndex in curve.indices) {
                    val sSample = curve[scrubbedIndex!!]
                    val sx = x(sSample.epochMillis)
                    val sy = y(sSample.height)

                    drawLine(
                        color = NauticalCyan,
                        start = Offset(sx, 0f),
                        end = Offset(sx, size.height),
                        strokeWidth = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawCircle(
                        color = NauticalCyan.copy(alpha = 0.50f),
                        radius = 7.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 6. X-Axis Time Labels Across 24 Hours
        val labelCount = 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until labelCount) {
                val sampleIdx = (i.toFloat() / (labelCount - 1) * curve.lastIndex).roundToInt()
                val sampleTime = curve[sampleIdx].epochMillis
                val isNearNow = abs(sampleTime - now) < 30 * 60_000L
                val labelText = if (isNearNow) nowLabel else clockTime(sampleTime, locale)

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNearNow) NauticalCyan else TextMuted
                )
            }
        }
    }
}

// Read through LocalConfiguration so formatting follows a per-app language change.
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** "+0.4 m"; heights that round to zero read "+0.0 m" rather than "-0.0 m". */
private fun signedMeters(height: Double, locale: Locale): String =
    String.format(locale, "%+.1f m", if (abs(height) < 0.05) 0.0 else height)

private fun clockTime(epochMillis: Long, locale: Locale): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
