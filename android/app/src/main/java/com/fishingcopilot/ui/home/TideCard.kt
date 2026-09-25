package com.fishingcopilot.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.tide.TideEvent
import com.fishingcopilot.tide.TideSummary
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        TrendArrow(rising = summary.rising, color = trendColor)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (summary.rising) R.string.tide_rising else R.string.tide_falling),
            style = MaterialTheme.typography.headlineMedium,
            color = TextHighContrast
        )
    }
    Text(
        text = stringResource(R.string.tide_height_relative, String.format(locale, "%+.1f m", summary.heightNow)),
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted
    )

    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        summary.nextHigh?.let { NextEvent(R.string.tide_next_high, it, now, Modifier.weight(1f)) }
        summary.nextLow?.let { NextEvent(R.string.tide_next_low, it, now, Modifier.weight(1f)) }
    }

    Spacer(Modifier.height(14.dp))
    TideChart(summary = summary, now = now)

    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.tide_offset_label), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.tide_offset_value, offsetMinutes), style = MaterialTheme.typography.labelMedium, color = NauticalCyan)
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

@Composable
private fun NextEvent(labelRes: Int, event: TideEvent, now: Long, modifier: Modifier) {
    val locale = currentLocale()
    val time = Instant.ofEpochMilli(event.epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    val minutes = ((event.epochMillis - now) / 60_000).toInt().coerceAtLeast(0)
    val countdown = if (minutes >= 60) {
        stringResource(R.string.tide_countdown_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.tide_countdown_minutes, minutes)
    }
    Surface(shape = RoundedCornerShape(14.dp), color = OceanMidnight, border = InsetBorder, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Label and time share one string; let it wrap so the time is never clipped on narrow cards.
            Text(stringResource(labelRes, time), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
            Text(countdown, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

/** 24-hour curve from two hours ago, with a dashed "now" line and dots on the next high and low. */
@Composable
private fun TideChart(summary: TideSummary, now: Long) {
    val description = stringResource(R.string.tide_chart_description)
    val nowLabel = stringResource(R.string.tide_now)
    val curve = summary.curve
    val start = curve.first().epochMillis
    val span = (curve.last().epochMillis - start).toFloat()
    val maxAbs = curve.maxOf { kotlin.math.abs(it.height) }.coerceAtLeast(0.1)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .semantics { contentDescription = description }
        ) {
            val pad = 8.dp.toPx()
            fun x(t: Long) = (t - start) / span * size.width
            fun y(h: Double) = (pad + (1 - (h / maxAbs + 1) / 2) * (size.height - 2 * pad)).toFloat()

            val line = Path().apply {
                curve.forEachIndexed { i, s -> if (i == 0) moveTo(x(s.epochMillis), y(s.height)) else lineTo(x(s.epochMillis), y(s.height)) }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(NauticalCyan.copy(alpha = 0.35f), NauticalCyan.copy(alpha = 0f))))
            drawPath(line, NauticalCyan, style = Stroke(width = 2.5.dp.toPx()))
            drawLine(OceanCardBorder, Offset(0f, y(0.0)), Offset(size.width, y(0.0)), strokeWidth = 1.dp.toPx())

            val nowX = x(now)
            drawLine(
                TextHighContrast.copy(alpha = 0.7f), Offset(nowX, 0f), Offset(nowX, size.height),
                strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            drawCircle(TextHighContrast, radius = 4.dp.toPx(), center = Offset(nowX, y(summary.heightNow)))
            listOfNotNull(summary.nextHigh, summary.nextLow)
                .filter { it.epochMillis <= curve.last().epochMillis }
                .forEach { drawCircle(PrimeGreen, radius = 4.dp.toPx(), center = Offset(x(it.epochMillis), y(it.height))) }
        }
        Text(nowLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun TrendArrow(rising: Boolean, color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            if (rising) {
                moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height)
            }
            close()
        }
        drawPath(path, color)
    }
}

// Read through LocalConfiguration so formatting follows a per-app language change.
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
