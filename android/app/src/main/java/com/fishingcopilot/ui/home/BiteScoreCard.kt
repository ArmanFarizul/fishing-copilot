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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.bite.BiteForecast
import com.fishingcopilot.bite.BiteScore
import com.fishingcopilot.bite.BiteTimeline
import com.fishingcopilot.bite.LightFactor
import com.fishingcopilot.bite.TideFactor
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.ui.theme.AlertRed
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

@Composable
fun BiteScoreCard(forecast: BiteForecast?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = BorderStroke(1.dp, OceanCardBorder),
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
                PulsingScoreBadge(
                    scoreText = scoreText,
                    isPrimeTime = prime,
                    description = stringResource(R.string.bite_score_description, scoreText, band)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(band, style = MaterialTheme.typography.headlineMedium, color = bandColor(score))
                    Text(bestWindowText(forecast, locale), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }

            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FactorRow(
                    label = R.string.bite_factor_tide, weight = BiteScore.TIDE_WEIGHT, value = moment.score.tide,
                    reason = moment.tide?.let {
                        when (it) {
                            TideFactor.SLACK -> R.string.bite_tide_slack
                            TideFactor.MOVING -> R.string.bite_tide_moving
                            TideFactor.MID_CYCLE -> R.string.bite_tide_mid
                        }
                    }
                )
                FactorRow(
                    label = R.string.bite_factor_light, weight = BiteScore.LIGHT_WEIGHT, value = moment.score.light,
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
                    label = R.string.bite_factor_moon, weight = BiteScore.SOLUNAR_WEIGHT, value = moment.score.solunar,
                    reason = moment.tideStrength?.let {
                        when (it) {
                            TideStrength.SPRING -> R.string.bite_moon_spring
                            TideStrength.NEAP -> R.string.bite_moon_neap
                            TideStrength.NORMAL -> R.string.bite_moon_normal
                        }
                    }
                )
                FactorRow(
                    label = R.string.bite_factor_pressure, weight = BiteScore.BARO_WEIGHT, value = moment.score.baro,
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

            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.bite_timeline_title), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
            Spacer(Modifier.height(8.dp))
            ScoreTimeline(forecast, description = stringResource(R.string.bite_timeline_description, bestWindowText(forecast, locale)))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.bite_disclaimer), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

/** The spec's score badge: pulses gently while the score is in the golden band. */
@Composable
private fun PulsingScoreBadge(scoreText: String, isPrimeTime: Boolean, description: String) {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val scale = if (isPrimeTime && !animationsOff) {
        val transition = rememberInfiniteTransition(label = "pulseTransition")
        val pulse by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseScale"
        )
        pulse
    } else {
        1f
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(90.dp)
            .scale(scale)
            .background(if (isPrimeTime) PrimeGreen.copy(alpha = 0.2f) else OceanCardBorder, CircleShape)
            .semantics { contentDescription = description }
    ) {
        Text(
            text = scoreText,
            style = MaterialTheme.typography.displayLarge,
            color = if (isPrimeTime) PrimeGreen else Color.White
        )
    }
}

@Composable
private fun FactorRow(@StringRes label: Int, weight: Double, value: Double?, @StringRes reason: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.width(96.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = TextHighContrast, maxLines = 1)
            Text(
                stringResource(R.string.bite_factor_weight, (weight * 100).toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                drawRoundRect(OceanCardBorder, cornerRadius = CornerRadius(3.dp.toPx()))
                if (value != null) {
                    drawRoundRect(
                        factorColor(value),
                        size = Size(size.width * value.toFloat(), size.height),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(if (value == null || reason == null) R.string.bite_factor_missing else reason),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

/** Bars for the next 24 hours; the prime window is highlighted and a dashed line marks now. */
@Composable
private fun ScoreTimeline(forecast: BiteForecast, description: String) {
    val points = forecast.points.drop(1)
    val best = forecast.best
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = description }
    ) {
        if (points.isEmpty()) return@Canvas
        val barWidth = size.width / points.size
        if (best != null) {
            val first = points.indexOfFirst { it.epochMillis >= best.start }
            val last = points.indexOfLast { it.epochMillis <= best.end }
            drawRoundRect(
                color = PrimeGreen.copy(alpha = 0.12f),
                topLeft = Offset(first * barWidth, 0f),
                size = Size((last - first + 1) * barWidth, size.height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
        points.forEachIndexed { i, point ->
            val height = ((point.score - 1) / 9).toFloat() * size.height
            drawRect(
                color = factorColor((point.score - 1) / 9),
                topLeft = Offset(i * barWidth + barWidth * 0.15f, size.height - height),
                size = Size(barWidth * 0.7f, height)
            )
        }
        drawLine(
            TextHighContrast.copy(alpha = 0.7f), Offset(0f, 0f), Offset(0f, size.height),
            strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
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
private fun bandLabel(score: Double): Int = when {
    score >= BiteTimeline.PRIME_THRESHOLD -> R.string.bite_band_prime
    score >= 6 -> R.string.bite_band_good
    score >= 4 -> R.string.bite_band_fair
    else -> R.string.bite_band_poor
}

private fun bandColor(score: Double): Color = when {
    score >= BiteTimeline.PRIME_THRESHOLD -> PrimeGreen
    score >= 6 -> NauticalCyan
    score >= 4 -> CautionYellow
    else -> AlertRed
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
