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
import com.fishingcopilot.marine.Compass
import com.fishingcopilot.marine.MarineSummary
import com.fishingcopilot.marine.PressureTrend
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

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Tile(
            label = R.string.marine_waves,
            value = hour.waveHeight?.let {
                stringResource(R.string.marine_wave_value, oneDecimal(it, locale), (hour.wavePeriod ?: 0.0).roundToInt())
            },
            detail = hour.waveDirection?.let { stringResource(R.string.marine_from, compassLabel(it)) },
            modifier = Modifier.weight(1f)
        )
        Tile(
            label = R.string.marine_wind,
            value = hour.windKn?.let {
                stringResource(R.string.marine_wind_value, it.roundToInt(), (hour.gustKn ?: it).roundToInt())
            },
            detail = hour.windDirection?.let { stringResource(R.string.marine_from, compassLabel(it)) },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Tile(
            label = R.string.marine_pressure,
            value = hour.pressureHpa?.let { stringResource(R.string.marine_pressure_value, it.roundToInt()) },
            detail = summary.pressureTrend?.let { stringResource(it.label) },
            detailColor = if (summary.pressureTrend == PressureTrend.FALLING_FAST) AlertRed else TextMuted,
            modifier = Modifier.weight(1f)
        )
        Tile(
            label = R.string.marine_current,
            value = hour.currentKn?.let { stringResource(R.string.marine_current_value, oneDecimal(it, locale)) },
            detail = hour.seaTempC?.let { stringResource(R.string.marine_sea_temp, oneDecimal(it, locale)) },
            modifier = Modifier.weight(1f)
        )
    }

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
private fun Tile(
    @StringRes label: Int,
    value: String?,
    detail: String?,
    modifier: Modifier = Modifier,
    detailColor: Color = TextMuted
) {
    Surface(shape = RoundedCornerShape(14.dp), color = OceanMidnight, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(value ?: "–", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighContrast)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = detailColor)
        }
    }
}

@Composable
private fun compassLabel(degrees: Double): String = stringResource(
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
