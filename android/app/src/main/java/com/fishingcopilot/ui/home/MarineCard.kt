package com.fishingcopilot.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.marine.Beaufort
import com.fishingcopilot.marine.Compass
import com.fishingcopilot.marine.CurrentLevel
import com.fishingcopilot.marine.MarineSummary
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.marine.SeaState
import com.fishingcopilot.marine.WaveBody
import com.fishingcopilot.marine.WaveStatus
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
import kotlin.math.roundToInt

@Composable
fun MarineCard(state: MarineCardState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = BorderStroke(1.dp, OceanCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.marine_card_title).uppercase(locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = NauticalCyan
                )
                if (state is MarineCardState.Ready) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.marine_updated, clockTime(state.fetchedAtMillis, locale)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            when (state) {
                MarineCardState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = NauticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.marine_loading), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                MarineCardState.Offline -> {
                    Text(stringResource(R.string.marine_error_offline), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.marine_retry), color = NauticalCyan) }
                }
                is MarineCardState.Ready -> ReadyContent(state, locale)
            }
        }
    }
}

@Composable
private fun ReadyContent(state: MarineCardState.Ready, locale: Locale) {
    val summary = state.summary
    val hour = summary.current

    summary.waveStatus?.let { status ->
        val (color, label, advice) = when (status) {
            WaveStatus.CALM -> Triple(PrimeGreen, R.string.marine_status_calm, R.string.marine_advice_calm)
            WaveStatus.CAUTION -> Triple(CautionYellow, R.string.marine_status_caution, R.string.marine_advice_caution)
            WaveStatus.DANGER -> Triple(AlertRed, R.string.marine_status_danger, R.string.marine_advice_danger)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), border = BorderStroke(1.dp, color)) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(advice), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.height(12.dp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        hour.waveHeight?.let { height -> WaveRow(height, hour.wavePeriod, hour.waveDirection, summary.waveStatus, locale) }
        hour.windKn?.let { knots -> WindRow(knots, hour.gustKn ?: knots, hour.windDirection) }
        hour.pressureHpa?.let { hPa -> PressureRow(hPa, summary.pressureTrend) }
        hour.currentKn?.let { knots -> CurrentRow(knots, hour.seaTempC, locale) }
    }
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.guide_sources), style = MaterialTheme.typography.labelSmall, color = TextMuted)

    val maxWave = summary.maxWaveNext24h
    val maxWind = summary.maxWindNext24h
    if (maxWave != null && maxWind != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.marine_next_24h, oneDecimal(maxWave, locale), maxWind.roundToInt()),
            style = MaterialTheme.typography.bodySmall,
            color = TextHighContrast
        )
    }
    if (state.stale) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.marine_stale, clockTime(state.fetchedAtMillis, locale)),
            style = MaterialTheme.typography.bodySmall,
            color = CautionYellow
        )
    }
}

@Composable
private fun WaveRow(height: Double, period: Double?, direction: Double?, status: WaveStatus?, locale: Locale) {
    val state = SeaState.of(height)
    val color = status?.color ?: NauticalCyan
    val numbers = listOfNotNull(
        stringResource(R.string.marine_wave_value, oneDecimal(height, locale), (period ?: 0.0).roundToInt()),
        direction?.let { stringResource(R.string.marine_from, compassLabel(it)) }
    ).joinToString(" · ")
    // The everyday scale stops at "very rough"; anything bigger still shows as the last step.
    val shown = SeaState.entries.take(7)
    GuideRow(
        title = stringResource(R.string.marine_waves),
        levelName = stringResource(state.label),
        comparison = stringResource(WaveBody.of(height).label),
        numbers = numbers,
        levelIndex = state.code.coerceAtMost(shown.lastIndex),
        levelCount = shown.size,
        levelColor = color,
        explainer = stringResource(R.string.guide_wave_explainer) + " " + stringResource(R.string.guide_body_note),
        scale = shown.mapIndexed { i, s ->
            ScaleStep(stringResource(s.label), if (i == 0) "0 m" else "${meters(shown[i - 1].maxHeight, locale)}–${meters(s.maxHeight, locale)} m")
        }
    ) { WaveAnimation(height, period ?: 6.0, color) }
}

@Composable
private fun WindRow(knots: Double, gusts: Double, direction: Double?) {
    // The Beaufort table is in whole knots, and so is the number shown, so classify the rounded value.
    val beaufort = Beaufort.of(knots.roundToInt().toDouble())
    val color = when {
        beaufort.force <= 3 -> PrimeGreen
        beaufort.force <= 5 -> CautionYellow
        else -> AlertRed
    }
    val shown = Beaufort.entries.take(9)
    val numbers = listOfNotNull(
        stringResource(R.string.marine_wind_value, knots.roundToInt(), gusts.roundToInt()),
        direction?.let { stringResource(R.string.marine_from, compassLabel(it)) }
    ).joinToString(" · ")
    GuideRow(
        title = stringResource(R.string.marine_wind),
        levelName = stringResource(beaufort.label),
        comparison = stringResource(beaufort.sign),
        numbers = numbers,
        levelIndex = beaufort.force.coerceAtMost(shown.lastIndex),
        levelCount = shown.size,
        levelColor = color,
        explainer = stringResource(R.string.guide_wind_explainer),
        scale = shown.mapIndexed { i, b ->
            val low = if (i == 0) null else (shown[i - 1].belowKnots + 0.5).toInt()
            val high = if (b.belowKnots == Double.MAX_VALUE) null else (b.belowKnots - 0.5).toInt()
            ScaleStep(stringResource(b.label), when {
                low == null -> "< 1 kn"
                high == null -> "$low+ kn"
                else -> "$low–$high kn"
            })
        }
    ) { WindAnimation(knots, beaufort.force, color, gusts) }
}

@Composable
private fun PressureRow(hPa: Double, trend: PressureTrend?) {
    val color = when (trend) {
        PressureTrend.FALLING_FAST -> AlertRed
        PressureTrend.FALLING -> CautionYellow
        else -> PrimeGreen
    }
    val trendIndex = when (trend) {
        PressureTrend.RISING -> 0
        null, PressureTrend.STEADY -> 1
        PressureTrend.FALLING -> 2
        PressureTrend.FALLING_FAST -> 3
    }
    val steps = listOf(PressureTrend.RISING, PressureTrend.STEADY, PressureTrend.FALLING, PressureTrend.FALLING_FAST)
    GuideRow(
        title = stringResource(R.string.marine_pressure),
        levelName = stringResource((trend ?: PressureTrend.STEADY).label),
        comparison = stringResource((trend ?: PressureTrend.STEADY).meaning),
        numbers = stringResource(R.string.marine_pressure_value, hPa.roundToInt()),
        levelIndex = trendIndex,
        levelCount = steps.size,
        levelColor = color,
        explainer = stringResource(R.string.guide_pressure_explainer),
        scale = steps.map { ScaleStep(stringResource(it.label) + " — " + stringResource(it.meaning), null) }
    ) {
        PressureGauge(
            hPa = hPa,
            trendDirection = when (trend) {
                PressureTrend.RISING -> 1
                PressureTrend.FALLING, PressureTrend.FALLING_FAST -> -1
                else -> 0
            },
            alarm = trend == PressureTrend.FALLING_FAST,
            color = color
        )
    }
}

@Composable
private fun CurrentRow(knots: Double, seaTemp: Double?, locale: Locale) {
    val level = CurrentLevel.of(knots)
    val (name, feel, sinker) = when (level) {
        CurrentLevel.SLOW -> Triple(R.string.current_slow, R.string.current_feel_slow, R.string.current_sinker_slow)
        CurrentLevel.MODERATE -> Triple(R.string.current_moderate, R.string.current_feel_moderate, R.string.current_sinker_moderate)
        CurrentLevel.FAST -> Triple(R.string.current_fast, R.string.current_feel_fast, R.string.current_sinker_fast)
    }
    val numbers = listOfNotNull(
        stringResource(R.string.marine_current_value, oneDecimal(knots, locale)),
        seaTemp?.let { stringResource(R.string.marine_sea_temp, oneDecimal(it, locale)) }
    ).joinToString(" · ")
    GuideRow(
        title = stringResource(R.string.marine_current),
        levelName = stringResource(name),
        comparison = stringResource(feel),
        numbers = numbers,
        levelIndex = level.ordinal,
        levelCount = CurrentLevel.entries.size,
        levelColor = NauticalCyan,
        explainer = stringResource(R.string.guide_current_explainer),
        scale = listOf(
            ScaleStep(stringResource(R.string.current_slow), "< 0.5 kn"),
            ScaleStep(stringResource(R.string.current_moderate), "0.5–1.2 kn"),
            ScaleStep(stringResource(R.string.current_fast), "> 1.2 kn")
        ),
        extra = stringResource(sinker)
    ) { CurrentAnimation(knots, NauticalCyan) }
}

private val WaveStatus.color: Color
    get() = when (this) {
        WaveStatus.CALM -> PrimeGreen
        WaveStatus.CAUTION -> CautionYellow
        WaveStatus.DANGER -> AlertRed
    }

/** Scale bounds such as 0.1, 1.25 and 4 without trailing zeros, in the user's decimal separator. */
private fun meters(value: Double, locale: Locale): String =
    java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols.getInstance(locale)).format(value)

@get:StringRes
private val SeaState.label: Int
    get() = listOf(
        R.string.sea_state_0, R.string.sea_state_1, R.string.sea_state_2, R.string.sea_state_3, R.string.sea_state_4,
        R.string.sea_state_5, R.string.sea_state_6, R.string.sea_state_7, R.string.sea_state_8, R.string.sea_state_9
    )[code]

@get:StringRes
private val WaveBody.label: Int
    get() = when (this) {
        WaveBody.FLAT -> R.string.wave_body_flat
        WaveBody.ANKLE -> R.string.wave_body_ankle
        WaveBody.KNEE -> R.string.wave_body_knee
        WaveBody.WAIST -> R.string.wave_body_waist
        WaveBody.CHEST -> R.string.wave_body_chest
        WaveBody.HEAD -> R.string.wave_body_head
        WaveBody.OVERHEAD -> R.string.wave_body_overhead
    }

@get:StringRes
private val Beaufort.label: Int
    get() = listOf(
        R.string.beaufort_0, R.string.beaufort_1, R.string.beaufort_2, R.string.beaufort_3, R.string.beaufort_4,
        R.string.beaufort_5, R.string.beaufort_6, R.string.beaufort_7, R.string.beaufort_8, R.string.beaufort_9,
        R.string.beaufort_10, R.string.beaufort_11, R.string.beaufort_12
    )[force]

@get:StringRes
private val Beaufort.sign: Int
    get() = listOf(
        R.string.beaufort_sign_0, R.string.beaufort_sign_1, R.string.beaufort_sign_2, R.string.beaufort_sign_3,
        R.string.beaufort_sign_4, R.string.beaufort_sign_5, R.string.beaufort_sign_6, R.string.beaufort_sign_7,
        R.string.beaufort_sign_8, R.string.beaufort_sign_9, R.string.beaufort_sign_10, R.string.beaufort_sign_11,
        R.string.beaufort_sign_12
    )[force]

@get:StringRes
private val PressureTrend.meaning: Int
    get() = when (this) {
        PressureTrend.STEADY -> R.string.pressure_meaning_steady
        PressureTrend.RISING -> R.string.pressure_meaning_rising
        PressureTrend.FALLING -> R.string.pressure_meaning_falling
        PressureTrend.FALLING_FAST -> R.string.pressure_meaning_falling_fast
    }

@Composable
internal fun compassLabel(degrees: Double): String = stringResource(
    when (Compass.of(degrees)) {
        Compass.N -> R.string.direction_n
        Compass.NE -> R.string.direction_ne
        Compass.E -> R.string.direction_e
        Compass.SE -> R.string.direction_se
        Compass.S -> R.string.direction_s
        Compass.SW -> R.string.direction_sw
        Compass.W -> R.string.direction_w
        Compass.NW -> R.string.direction_nw
    }
)

@get:StringRes
private val PressureTrend.label: Int
    get() = when (this) {
        PressureTrend.STEADY -> R.string.marine_pressure_steady
        PressureTrend.RISING -> R.string.marine_pressure_rising
        PressureTrend.FALLING -> R.string.marine_pressure_falling
        PressureTrend.FALLING_FAST -> R.string.marine_pressure_falling_fast
    }

private fun oneDecimal(value: Double, locale: Locale) = String.format(locale, "%.1f", value)

private fun clockTime(epochMillis: Long, locale: Locale): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
