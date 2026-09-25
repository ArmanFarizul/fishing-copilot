package com.fishingcopilot.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.satellite.ClarityLevel
import com.fishingcopilot.satellite.FRONT_SEARCH_KM
import com.fishingcopilot.satellite.FrontStrength
import com.fishingcopilot.satellite.NearbyFront
import com.fishingcopilot.satellite.PlanktonLevel
import com.fishingcopilot.satellite.WaterTempLevel
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.InsetBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SatelliteCard(state: SatelliteCardState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = CardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.satellite_card_title).uppercase(locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = NauticalCyan
                )
                if (state is SatelliteCardState.Ready) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.satellite_data_date, shortDate(state.summary.dataDate, locale)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            when (state) {
                SatelliteCardState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = NauticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.satellite_loading), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                SatelliteCardState.Offline -> {
                    Text(stringResource(R.string.satellite_error_offline), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.satellite_retry), color = NauticalCyan) }
                }
                SatelliteCardState.NoData ->
                    Text(stringResource(R.string.satellite_no_data), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                is SatelliteCardState.Ready -> ReadyContent(state, locale)
            }
        }
    }
}

@Composable
private fun ReadyContent(state: SatelliteCardState.Ready, locale: Locale) {
    val summary = state.summary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        summary.seaTempC?.let { celsius ->
            val level = WaterTempLevel.of(celsius)
            val (name, meaning) = when (level) {
                WaterTempLevel.COOL -> R.string.satellite_temp_cool to R.string.satellite_temp_cool_meaning
                WaterTempLevel.IDEAL -> R.string.satellite_temp_ideal to R.string.satellite_temp_ideal_meaning
                WaterTempLevel.WARM -> R.string.satellite_temp_warm to R.string.satellite_temp_warm_meaning
            }
            SatelliteRow(
                title = stringResource(R.string.satellite_temp),
                value = stringResource(R.string.satellite_temp_value, String.format(locale, "%.1f", celsius)),
                levelName = stringResource(name),
                levelColor = if (level == WaterTempLevel.IDEAL) PrimeGreen else CautionYellow,
                meaning = stringResource(meaning)
            )
        }
        summary.chlorophyll?.let { mg ->
            val level = PlanktonLevel.of(mg)
            val (name, meaning) = when (level) {
                PlanktonLevel.LOW -> R.string.satellite_plankton_low to R.string.satellite_plankton_low_meaning
                PlanktonLevel.IDEAL -> R.string.satellite_plankton_ideal to R.string.satellite_plankton_ideal_meaning
                PlanktonLevel.HIGH -> R.string.satellite_plankton_high to R.string.satellite_plankton_high_meaning
            }
            SatelliteRow(
                title = stringResource(R.string.satellite_plankton),
                value = stringResource(R.string.satellite_plankton_value, String.format(locale, if (mg < 1) "%.2f" else "%.1f", mg)),
                levelName = stringResource(name),
                levelColor = when (level) {
                    PlanktonLevel.LOW -> TextMuted
                    PlanktonLevel.IDEAL -> PrimeGreen
                    PlanktonLevel.HIGH -> CautionYellow
                },
                meaning = stringResource(meaning)
            )
        }
        summary.clarityM?.let { meters ->
            val (name, meaning) = when (ClarityLevel.of(meters)) {
                ClarityLevel.MURKY -> R.string.satellite_clarity_murky to R.string.satellite_clarity_murky_meaning
                ClarityLevel.MODERATE -> R.string.satellite_clarity_moderate to R.string.satellite_clarity_moderate_meaning
                ClarityLevel.CLEAR -> R.string.satellite_clarity_clear to R.string.satellite_clarity_clear_meaning
            }
            SatelliteRow(
                title = stringResource(R.string.satellite_clarity),
                value = stringResource(R.string.satellite_clarity_value, meters.roundToInt()),
                levelName = stringResource(name),
                // Neither end is better; it only changes lure colour and line choice.
                levelColor = NauticalCyan,
                meaning = stringResource(meaning)
            )
        }
        FrontRow(summary.front)
    }

    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.satellite_note), style = MaterialTheme.typography.labelSmall, color = TextMuted)
    if (state.stale) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.satellite_stale, shortDate(summary.dataDate, locale)),
            style = MaterialTheme.typography.bodySmall,
            color = CautionYellow
        )
    }
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(R.string.attribution_copernicus),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(role = Role.Button) { uriHandler.openUri("https://marine.copernicus.eu/") }
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun FrontRow(front: NearbyFront?) {
    if (front == null) {
        SatelliteRow(
            title = stringResource(R.string.satellite_front),
            value = null,
            levelName = stringResource(R.string.satellite_front_none, FRONT_SEARCH_KM.roundToInt()),
            levelColor = TextMuted,
            meaning = stringResource(R.string.satellite_front_none_meaning)
        )
        return
    }
    val (name, meaning) = when (front.strength) {
        FrontStrength.WEAK -> R.string.satellite_front_weak to R.string.satellite_front_weak_meaning
        FrontStrength.STRONG -> R.string.satellite_front_strong to R.string.satellite_front_strong_meaning
    }
    // Distances are to a cell centre about 28 km wide, so round to 5 km.
    val km = ((front.distanceKm / 5).roundToInt() * 5)
    SatelliteRow(
        title = stringResource(R.string.satellite_front),
        value = if (km == 0) stringResource(R.string.satellite_front_here)
        else stringResource(R.string.satellite_front_away, km, compassLabel(front.bearingDeg)),
        levelName = stringResource(name),
        levelColor = if (front.strength == FrontStrength.STRONG) PrimeGreen else NauticalCyan,
        meaning = stringResource(meaning)
    )
}

@Composable
private fun SatelliteRow(title: String, value: String?, levelName: String, levelColor: Color, meaning: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = OceanMidnight, border = InsetBorder) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.weight(1f))
                value?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = TextHighContrast) }
            }
            Spacer(Modifier.height(4.dp))
            Text(levelName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = levelColor)
            Text(meaning, style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
        }
    }
}

private fun shortDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
