package com.fishingcopilot.ui.home

import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.components.hijriDateText
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.astro.MoonPhaseName
import com.fishingcopilot.astro.SolunarType
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.data.hijri.HijriEntry
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SunMoonCard(state: SunMoonUiState?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = BorderStroke(1.dp, OceanCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.astro_card_title).uppercase(locale),
                style = MaterialTheme.typography.labelMedium,
                color = NauticalCyan
            )
            if (state == null) return@Column
            val day = state.day
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TimeColumn(R.string.astro_first_light, day.civilDawn, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_sunrise, day.sunrise, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_sunset, day.sunset, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_last_light, day.civilDusk, locale, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))
            var selected by rememberSaveable { mutableIntStateOf(0) }
            val selectedDay = state.calendar.getOrElse(selected) { state.calendar.first() }
            SelectedMoonRow(selectedDay)

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.moon_calendar_title), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            MoonCalendarStrip(calendar = state.calendar, selected = selected, onSelect = { selected = it })
            Text(stringResource(R.string.moon_calendar_hint), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TimeColumn(R.string.astro_moonrise, day.moonrise, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_moonset, day.moonset, locale, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.astro_solunar_title), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
            Spacer(Modifier.height(8.dp))
            val upcoming = state.periods.filter { it.status != PeriodStatus.PAST }
            if (upcoming.isEmpty()) {
                Text(stringResource(R.string.astro_solunar_none_left), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.periods.forEach { SolunarRow(it, locale) }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.astro_solunar_note), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            if (state.calendar.any { it.hijri.official }) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.hijri_source), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun SelectedMoonRow(day: CalendarDay) {
    val phaseName = stringResource(day.moon.phase.label)
    val percent = (day.moon.illumination * 100).roundToInt()
    val animationsOff = animationsDisabled()
    val lit = remember { Animatable(0f) }
    LaunchedEffect(day.moon.date) {
        val target = day.moon.illumination.toFloat()
        // First show fills the moon from dark; later selections morph from the previous day's phase.
        if (animationsOff) lit.snapTo(target) else lit.animateTo(target, tween(durationMillis = if (lit.value == 0f) 800 else 350))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MoonIcon(
            illumination = lit.value.toDouble(),
            waxing = day.moon.waxing,
            description = stringResource(R.string.astro_moon_description, phaseName, percent),
            diameter = 52.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(phaseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighContrast)
            Text(stringResource(R.string.astro_illumination, percent), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(hijriText(day.hijri), style = MaterialTheme.typography.bodySmall, color = NauticalCyan)
        }
        when (day.tideStrength) {
            TideStrength.SPRING -> Chip(stringResource(R.string.astro_spring_tide), PrimeGreen)
            TideStrength.NEAP -> Chip(stringResource(R.string.astro_neap_tide), CautionYellow)
            TideStrength.NORMAL -> Unit
        }
    }
}

/** 30 days of moons; dragging selects the day nearest the centre, tapping selects and centres a day. */
@Composable
private fun MoonCalendarStrip(calendar: List<CalendarDay>, selected: Int, onSelect: (Int) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val cellWidth = 56.dp
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }?.index
        }.filterNotNull().distinctUntilChanged().collect { if (listState.isScrollInProgress) currentOnSelect(it) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sidePadding = (maxWidth - cellWidth) / 2
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = sidePadding),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(calendar) { index, day ->
                val isSelected = index == selected
                val date = day.moon.date
                val description = stringResource(
                    R.string.moon_calendar_day_description,
                    date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)),
                    stringResource(day.moon.phase.label),
                    hijriText(day.hijri)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(cellWidth)
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NauticalCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (isSelected) NauticalCyan else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            onSelect(index)
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                        .semantics(mergeDescendants = true) { contentDescription = description }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = if (index == 0) stringResource(R.string.moon_calendar_today)
                        else date.format(DateTimeFormatter.ofPattern("EEE", locale)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) NauticalCyan else TextMuted,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    MoonIcon(illumination = day.moon.illumination, waxing = day.moon.waxing, description = null, diameter = 22.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = day.hijri.hijri.day.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (day.tideStrength) {
                            TideStrength.SPRING -> PrimeGreen
                            TideStrength.NEAP -> CautionYellow
                            TideStrength.NORMAL -> TextHighContrast
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun hijriText(entry: HijriEntry): String {
    val date = hijriDateText(entry.hijri)
    return if (entry.official) date else stringResource(R.string.hijri_estimate, date)
}

/** True when the user turned off animations (Developer options or Accessibility "Remove animations"). */
@Composable
private fun animationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
private fun TimeColumn(@StringRes label: Int, time: ZonedDateTime?, locale: Locale, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = OceanMidnight, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
            Text(
                text = time?.let { clock(it, locale) } ?: stringResource(R.string.astro_no_event),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighContrast,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SolunarRow(rated: RatedPeriod, locale: Locale) {
    val period = rated.period
    val isMajor = period.type == SolunarType.MAJOR
    val active = rated.status == PeriodStatus.NOW
    val faded = rated.status == PeriodStatus.PAST
    val accent = if (isMajor) PrimeGreen else NauticalCyan
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) accent.copy(alpha = 0.15f) else OceanMidnight,
        border = if (active) BorderStroke(1.dp, accent) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(if (isMajor) R.string.astro_solunar_major else R.string.astro_solunar_minor),
                style = MaterialTheme.typography.labelMedium,
                color = if (faded) TextMuted.copy(alpha = 0.5f) else accent,
                modifier = Modifier.width(72.dp)
            )
            Text(
                text = stringResource(R.string.astro_solunar_range, clock(period.start, locale), clock(period.end, locale)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (faded) TextMuted.copy(alpha = 0.5f) else TextHighContrast,
                modifier = Modifier.weight(1f)
            )
            when (rated.status) {
                PeriodStatus.NOW -> Chip(stringResource(R.string.astro_solunar_now), accent)
                PeriodStatus.NEXT -> Chip(stringResource(R.string.astro_solunar_next), TextMuted)
                else -> Unit
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), border = BorderStroke(1.dp, color)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/**
 * Lit part of the moon as seen from the tropics: the terminator is an ellipse whose width follows the
 * illuminated fraction; the lit limb is on the right while waxing and on the left while waning.
 */
@Composable
private fun MoonIcon(illumination: Double, waxing: Boolean, description: String?, diameter: Dp) {
    val semanticsModifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    Canvas(modifier = Modifier.size(diameter).then(semanticsModifier)) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(OceanCardBorder, radius = r, center = c)
        val lit = Path().apply {
            // Half disc on the lit side...
            addArc(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r), if (waxing) -90f else 90f, 180f)
        }
        val terminatorHalfWidth = (r * (1 - 2 * illumination)).toFloat().let { kotlin.math.abs(it) }
        clipPath(lit) { drawCircle(TextHighContrast, radius = r, center = c) }
        val ellipseTopLeft = Offset(c.x - terminatorHalfWidth, c.y - r)
        val ellipseSize = Size(terminatorHalfWidth * 2, r * 2)
        // ...then the terminator ellipse either adds light (gibbous) or removes it (crescent).
        if (illumination >= 0.5) drawOval(TextHighContrast, ellipseTopLeft, ellipseSize)
        else drawOval(OceanCardBorder, ellipseTopLeft, ellipseSize)
    }
}

private fun clock(time: ZonedDateTime, locale: Locale): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
