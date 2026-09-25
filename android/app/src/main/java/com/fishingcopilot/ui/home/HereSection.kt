package com.fishingcopilot.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.data.spots.bearingDeg
import com.fishingcopilot.data.spots.haversineKm
import com.fishingcopilot.maps.formatDegreesMinutes
import com.fishingcopilot.prayer.JAKIM_ZONES
import com.fishingcopilot.prayer.MALAYSIA
import com.fishingcopilot.prayer.Prayer
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.InsetBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import com.fishingcopilot.weather.Forecast
import com.fishingcopilot.weather.Sky
import com.fishingcopilot.weather.StormAlert
import com.fishingcopilot.weather.StormKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val HOURS_SHOWN = 12
private const val STALE_AFTER_MS = 3 * 60 * 60 * 1000L

/** Everything about where the phone is: weather, storm warning, prayer times and the way to the spot. */
@Composable
fun HereSection(
    state: HereUiState,
    spot: SpotEntity?,
    onRefresh: () -> Unit,
    onPickZone: (String?) -> Unit,
    now: Long,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onRefresh() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(R.string.here_section)
        when (val location = state.location) {
            HereLocation.NoPermission -> PromptCard(R.string.here_permission_body, R.string.here_permission_button) {
                permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
            HereLocation.Locating -> Note(stringResource(R.string.here_locating))
            HereLocation.Unavailable -> PromptCard(R.string.here_unavailable, R.string.here_retry, onRefresh)
            is HereLocation.Found -> {
                LocationLine(state.placeName, location.point.latitude, location.point.longitude)
                state.storm?.let { StormBanner(it, locale) }
                val cached = state.forecast
                when {
                    cached != null -> WeatherCard(cached.forecast, now, locale, stale = now - cached.fetchedAtMillis > STALE_AFTER_MS)
                    state.weatherFailed -> PromptCard(R.string.here_weather_failed, R.string.here_retry, onRefresh)
                    else -> Note(stringResource(R.string.here_weather_loading))
                }
                spot?.let { DistanceLine(location.point.latitude, location.point.longitude, it) }
            }
        }
        // Outside the location states: a zone picked by hand still has times without a fix.
        state.prayers?.let { PrayerCard(state, it.zone, it.district, it.manual, locale, onPickZone) }
    }
}

@Composable
fun SectionLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = NauticalCyan
    )
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
}

@Composable
private fun PromptCard(@StringRes body: Int, @StringRes button: Int, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = OceanSurface, border = CardBorder) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = TextHighContrast)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = NauticalCyan, contentColor = OceanMidnight),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(button), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StormBanner(alert: StormAlert, locale: Locale) {
    val text = stringResource(
        if (alert.kind == StormKind.THUNDERSTORM) R.string.here_storm_thunder else R.string.here_storm_rain,
        clockTime(alert.atMillis, locale)
    )
    Surface(shape = RoundedCornerShape(16.dp), color = AlertRed.copy(alpha = 0.10f), border = BorderStroke(CardBorder.width, AlertRed)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SkyIcon(if (alert.kind == StormKind.THUNDERSTORM) Sky.THUNDERSTORM else Sky.HEAVY_RAIN, 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AlertRed)
        }
    }
}

@Composable
private fun WeatherCard(forecast: Forecast, now: Long, locale: Locale, stale: Boolean) {
    val current = forecast.now
    val hourNow = forecast.hours.lastOrNull { it.epochMillis <= now }
    Surface(shape = RoundedCornerShape(20.dp), color = OceanSurface, border = CardBorder) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.here_weather_title).uppercase(), style = MaterialTheme.typography.labelMedium, color = NauticalCyan)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkyIcon(current.sky, 56.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = degrees(current.tempC),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextHighContrast
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(current.sky.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighContrast)
                    current.feelsC?.let {
                        Text(stringResource(R.string.here_feels_like, degrees(it)), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hourNow?.rainChance?.let { Stat(stringResource(R.string.here_rain_label), "$it%") }
                current.windKn?.let { Stat(stringResource(R.string.here_wind_label), stringResource(R.string.marine_current_value, it.roundToInt().toString())) }
                current.uvIndex?.let { uv ->
                    Stat(stringResource(R.string.here_uv_label), "${uv.roundToInt()} · ${stringResource(uvLabel(uv))}", if (uv >= 8) AlertRed else if (uv >= 6) CautionYellow else TextHighContrast)
                }
            }

            Spacer(Modifier.height(12.dp))
            // Folded by default so the card stays short; the current conditions above cover most checks.
            var hourlyOpen by rememberSaveable { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { hourlyOpen = !hourlyOpen }
                    .semantics {
                        if (hourlyOpen) collapse { hourlyOpen = false; true } else expand { hourlyOpen = true; true }
                    }
            ) {
                Text(stringResource(R.string.here_hourly_title), style = MaterialTheme.typography.titleMedium, color = TextHighContrast, modifier = Modifier.weight(1f))
                Text(if (hourlyOpen) "▴" else "▾", style = MaterialTheme.typography.titleLarge, color = NauticalCyan)
            }
            AnimatedVisibility(visible = hourlyOpen) {
                Column {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    forecast.hours.filter { it.epochMillis + 3_600_000 > now }.take(HOURS_SHOWN).forEachIndexed { i, hour ->
                        Surface(shape = RoundedCornerShape(12.dp), color = OceanMidnight, border = InsetBorder) {
                            Column(
                                modifier = Modifier.widthIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (i == 0) stringResource(R.string.here_now) else hourLabel(hour.epochMillis, locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (i == 0) NauticalCyan else TextMuted
                                )
                                Spacer(Modifier.height(4.dp))
                                SkyIcon(hour.sky, 26.dp)
                                Spacer(Modifier.height(4.dp))
                                hour.tempC?.let { Text(degrees(it), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextHighContrast) }
                                hour.rainChance?.let { Text("$it%", style = MaterialTheme.typography.labelSmall, color = if (it >= 60) NauticalCyan else TextMuted) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.here_daily_title), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
                Spacer(Modifier.height(4.dp))
                val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
                forecast.days.forEach { day ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dayLabel(day.date, today, locale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHighContrast,
                            modifier = Modifier.width(92.dp)
                        )
                        SkyIcon(day.sky, 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            day.rainChance?.let { "$it%" }.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if ((day.rainChance ?: 0) >= 60) NauticalCyan else TextMuted,
                            modifier = Modifier.width(44.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            listOfNotNull(day.maxC?.let(::degrees), day.minC?.let(::degrees)).joinToString(" / "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHighContrast,
                            textAlign = TextAlign.End
                        )
                    }
                }
                }
            }
            if (stale) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.here_weather_stale), style = MaterialTheme.typography.bodySmall, color = CautionYellow)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color = TextHighContrast) {
    Surface(shape = RoundedCornerShape(12.dp), color = OceanMidnight, border = InsetBorder) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun PrayerCard(
    state: HereUiState,
    zone: String,
    district: String?,
    manual: Boolean,
    locale: Locale,
    onPickZone: (String?) -> Unit
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now(MALAYSIA)
    val day = state.prayers?.days?.firstOrNull { it.date == today } ?: return
    val next = state.nextPrayer
    Surface(shape = RoundedCornerShape(20.dp), color = OceanSurface, border = CardBorder) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(R.string.here_prayer_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = NauticalCyan
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.here_zone_change)) { picking = true }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (district != null) stringResource(R.string.here_prayer_zone, zone, district)
                        else stringResource(R.string.here_prayer_zone_sea, zone),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHighContrast
                    )
                    Text(
                        stringResource(if (manual) R.string.here_zone_manual else R.string.here_zone_auto),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (manual) CautionYellow else TextMuted
                    )
                }
                Text(stringResource(R.string.here_zone_change), style = MaterialTheme.typography.labelLarge, color = NauticalCyan)
            }
            next?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.here_prayer_next, stringResource(it.prayer.label), it.at.toLocalTime().format(timeFormat(locale))),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Prayer.entries.forEach { prayer ->
                    val time = day.times[prayer] ?: return@forEach
                    val isNext = next?.prayer == prayer && next.at.toLocalDate() == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(prayer.label), style = MaterialTheme.typography.labelSmall, color = if (isNext) NauticalCyan else TextMuted)
                        Text(
                            time.format(timeFormat(locale)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNext) NauticalCyan else TextHighContrast
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.here_prayer_source), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
    if (picking) {
        ZonePicker(current = zone.takeIf { manual }, onPick = { onPickZone(it); picking = false }, onDismiss = { picking = false })
    }
}

/** "Follow my location" first, then every JAKIM zone grouped by state. */
@Composable
private fun ZonePicker(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.here_zone_pick_title)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    ZoneRow(stringResource(R.string.here_zone_auto_option), null, selected = current == null) { onPick(null) }
                }
                JAKIM_ZONES.groupBy { it.state }.forEach { (state, zones) ->
                    item(key = state) {
                        Text(
                            state.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = NauticalCyan,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                    }
                    items(zones, key = { it.code }) { z ->
                        ZoneRow(z.code, z.districts, selected = current == z.code) { onPick(z.code) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_go_to_cancel)) } },
        containerColor = OceanSurface
    )
}

@Composable
private fun ZoneRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NauticalCyan else TextHighContrast
        )
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted) }
    }
}

/** "📍 Kuala Terengganu" with the coordinates under it, so it is always clear which place the weather is for. */
@Composable
private fun LocationLine(placeName: String?, latitude: Double, longitude: Double) {
    val name = placeName ?: stringResource(R.string.here_near_area, stringResource(CoastalArea.nearest(latitude, longitude).first.label))
    Row(verticalAlignment = Alignment.CenterVertically) {
        LocationPin()
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighContrast)
            Text(formatDegreesMinutes(latitude, longitude), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun LocationPin() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val r = size.minDimension * 0.28f
        val head = Offset(center.x, size.height * 0.36f)
        val path = Path().apply {
            moveTo(head.x - r, head.y)
            quadraticTo(head.x - r, head.y + r * 1.5f, center.x, size.height * 0.95f)
            quadraticTo(head.x + r, head.y + r * 1.5f, head.x + r, head.y)
        }
        drawPath(path, NauticalCyan)
        drawCircle(NauticalCyan, radius = r, center = head)
        drawCircle(OceanSurface, radius = r * 0.4f, center = head)
    }
}

@Composable
private fun DistanceLine(latitude: Double, longitude: Double, spot: SpotEntity) {
    val km = haversineKm(latitude, longitude, spot.latitude, spot.longitude)
    val text = if (km < NEAREST_SPOT_KM) stringResource(R.string.here_at_spot, spot.name)
    else stringResource(
        R.string.here_spot_distance,
        spot.name,
        if (km < 10) String.format(Locale.ROOT, "%.1f", km) else km.roundToInt().toString(),
        compassLabel(bearingDeg(latitude, longitude, spot.latitude, spot.longitude))
    )
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextHighContrast)
}

/** Simple weather glyphs drawn in code; the app ships no icon library. */
@Composable
fun SkyIcon(sky: Sky, size: Dp) {
    val label = stringResource(sky.label)
    Canvas(modifier = Modifier.size(size).clearAndSetSemantics { contentDescription = label }) {
        when (sky) {
            Sky.CLEAR -> sun(center, this.size.minDimension * 0.22f)
            Sky.PARTLY_CLOUDY -> {
                sun(Offset(this.size.width * 0.38f, this.size.height * 0.38f), this.size.minDimension * 0.16f)
                cloud(0.55f)
            }
            Sky.CLOUDY -> cloud(0.45f)
            Sky.FOG -> {
                cloud(0.35f)
                fogLines()
            }
            Sky.DRIZZLE -> {
                cloud(0.35f)
                rain(2, heavy = false)
            }
            Sky.RAIN -> {
                cloud(0.35f)
                rain(3, heavy = false)
            }
            Sky.HEAVY_RAIN -> {
                cloud(0.35f)
                rain(4, heavy = true)
            }
            Sky.THUNDERSTORM -> {
                cloud(0.35f)
                bolt()
            }
        }
    }
}

private fun DrawScope.sun(c: Offset, r: Float) {
    drawCircle(CautionYellow, radius = r, center = c)
    repeat(8) { i ->
        val a = Math.toRadians(i * 45.0)
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        drawLine(CautionYellow, Offset(c.x + dx * r * 1.4f, c.y + dy * r * 1.4f), Offset(c.x + dx * r * 1.9f, c.y + dy * r * 1.9f), strokeWidth = r * 0.25f, cap = StrokeCap.Round)
    }
}

/** A cloud whose base sits at [baseFraction] of the height from the bottom. */
private fun DrawScope.cloud(baseFraction: Float) {
    val w = size.width
    val h = size.height
    val base = h * (1 - baseFraction * 0.6f) - h * 0.05f
    val color = TextMuted
    drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.35f, base - h * 0.08f))
    drawCircle(color, radius = w * 0.21f, center = Offset(w * 0.55f, base - h * 0.15f))
    drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.74f, base - h * 0.06f))
    drawRoundRect(color, topLeft = Offset(w * 0.2f, base - h * 0.1f), size = Size(w * 0.68f, h * 0.14f),
        cornerRadius = CornerRadius(h * 0.07f))
}

private fun DrawScope.rain(drops: Int, heavy: Boolean) {
    val w = size.width
    val h = size.height
    repeat(drops) { i ->
        val x = w * (0.3f + i * (0.45f / (drops - 1).coerceAtLeast(1)))
        drawLine(NauticalCyan, Offset(x, h * 0.72f), Offset(x - w * 0.06f, h * 0.9f), strokeWidth = w * if (heavy) 0.07f else 0.05f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.bolt() {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.55f, h * 0.6f)
        lineTo(w * 0.42f, h * 0.8f)
        lineTo(w * 0.52f, h * 0.8f)
        lineTo(w * 0.45f, h * 0.98f)
        lineTo(w * 0.65f, h * 0.74f)
        lineTo(w * 0.55f, h * 0.74f)
        lineTo(w * 0.62f, h * 0.6f)
        close()
    }
    drawPath(path, CautionYellow)
}

private fun DrawScope.fogLines() {
    val w = size.width
    val h = size.height
    listOf(0.76f, 0.88f).forEach { y ->
        drawLine(TextMuted, Offset(w * 0.2f, h * y), Offset(w * 0.8f, h * y), strokeWidth = h * 0.05f, cap = StrokeCap.Round)
    }
}

@get:StringRes
val Sky.label: Int
    get() = when (this) {
        Sky.CLEAR -> R.string.sky_clear
        Sky.PARTLY_CLOUDY -> R.string.sky_partly_cloudy
        Sky.CLOUDY -> R.string.sky_cloudy
        Sky.FOG -> R.string.sky_fog
        Sky.DRIZZLE -> R.string.sky_drizzle
        Sky.RAIN -> R.string.sky_rain
        Sky.HEAVY_RAIN -> R.string.sky_heavy_rain
        Sky.THUNDERSTORM -> R.string.sky_thunderstorm
    }

@get:StringRes
private val Prayer.label: Int
    get() = when (this) {
        Prayer.FAJR -> R.string.prayer_fajr
        Prayer.SYURUK -> R.string.prayer_syuruk
        Prayer.DHUHR -> R.string.prayer_dhuhr
        Prayer.ASR -> R.string.prayer_asr
        Prayer.MAGHRIB -> R.string.prayer_maghrib
        Prayer.ISHA -> R.string.prayer_isha
    }

/** WHO UV index bands. */
@StringRes
private fun uvLabel(uv: Double): Int = when {
    uv < 3 -> R.string.uv_low
    uv < 6 -> R.string.uv_moderate
    uv < 8 -> R.string.uv_high
    uv < 11 -> R.string.uv_very_high
    else -> R.string.uv_extreme
}

private fun degrees(celsius: Double) = "${celsius.roundToInt()}°"

private fun timeFormat(locale: Locale) = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

private fun clockTime(millis: Long, locale: Locale) =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat(locale))

private fun hourLabel(millis: Long, locale: Locale): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h a", locale))

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale): String = when (date) {
    today -> stringResource(R.string.here_today)
    today.plusDays(1) -> stringResource(R.string.here_tomorrow)
    else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) + " " + date.dayOfMonth
}
