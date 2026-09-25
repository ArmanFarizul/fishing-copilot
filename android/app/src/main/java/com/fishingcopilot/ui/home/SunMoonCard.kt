package com.fishingcopilot.ui.home

import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import java.time.Duration
import java.time.LocalDate
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
            val nowTime = remember(day) {
                day.civilDawn?.zone?.let { ZonedDateTime.now(it) } ?: ZonedDateTime.now()
            }
            Spacer(Modifier.height(10.dp))

            // 1. Visual Sun & Daylight Arc (Waktu Siang & Puncak Emas)
            SunArc(
                civilDawn = day.civilDawn,
                sunrise = day.sunrise,
                sunset = day.sunset,
                civilDusk = day.civilDusk,
                now = nowTime
            )
            Spacer(Modifier.height(8.dp))

            // Four daylight milestones
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TimeColumn(R.string.astro_first_light, day.civilDawn, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_sunrise, day.sunrise, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_sunset, day.sunset, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_last_light, day.civilDusk, locale, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            var selected by rememberSaveable { mutableIntStateOf(0) }
            val selectedDay = state.calendar.getOrElse(selected) { state.calendar.first() }

            // 2. Selected Moon Chapter: 3D Moon, Phase, Illumination, Hijri date, Fishing Advice & Moon times
            SelectedMoonRow(
                day = selectedDay,
                moonrise = if (selected == 0) day.moonrise else null,
                moonset = if (selected == 0) day.moonset else null,
                locale = locale
            )

            Spacer(Modifier.height(16.dp))
            // 3. 30-Day Moon Calendar Strip with Air Besar/Mati indicator dots & legend
            Text(stringResource(R.string.moon_calendar_title), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            MoonCalendarStrip(calendar = state.calendar, selected = selected, onSelect = { selected = it })

            Spacer(Modifier.height(16.dp))
            // 4. Solunar Feeding Times with Active Pulse and Live Countdown
            Text(stringResource(R.string.astro_solunar_title), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
            Spacer(Modifier.height(8.dp))
            val upcoming = state.periods.filter { it.status != PeriodStatus.PAST }
            if (upcoming.isEmpty()) {
                Text(stringResource(R.string.astro_solunar_none_left), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.periods.forEach { SolunarRow(it, nowTime, locale) }
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

/** Visual parabolic daylight arc showing daylight progression and highlighting morning/evening golden hours. */
@Composable
private fun SunArc(
    civilDawn: ZonedDateTime?,
    sunrise: ZonedDateTime?,
    sunset: ZonedDateTime?,
    civilDusk: ZonedDateTime?,
    now: ZonedDateTime
) {
    if (civilDawn == null || civilDusk == null || sunrise == null || sunset == null) return

    val dawnEpoch = civilDawn.toEpochSecond()
    val duskEpoch = civilDusk.toEpochSecond()
    val nowEpoch = now.toEpochSecond()
    val totalSeconds = (duskEpoch - dawnEpoch).toFloat().coerceAtLeast(1f)
    val isDaylight = nowEpoch in dawnEpoch..duskEpoch
    val sunProgress = ((nowEpoch - dawnEpoch) / totalSeconds).coerceIn(0f, 1f)

    val sunriseFraction = ((sunrise.toEpochSecond() - dawnEpoch) / totalSeconds).coerceIn(0.01f, 0.40f)
    val sunsetFraction = ((sunset.toEpochSecond() - dawnEpoch) / totalSeconds).coerceIn(0.60f, 0.99f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 6.dp)
    ) {
        val w = size.width
        val h = size.height
        val baseY = h - 4.dp.toPx()
        val arcH = h - 12.dp.toPx()

        fun getSunPoint(t: Float): Offset {
            val x = t * w
            val y = baseY - (kotlin.math.sin(t * Math.PI.toFloat()) * arcH)
            return Offset(x, y)
        }

        // 1. Horizon baseline
        drawLine(
            color = OceanCardBorder,
            start = Offset(0f, baseY),
            end = Offset(w, baseY),
            strokeWidth = 1.dp.toPx()
        )

        // 2. Daytime sky arc
        val arcPath = Path().apply {
            moveTo(0f, baseY)
            val steps = 30
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val pt = getSunPoint(t)
                lineTo(pt.x, pt.y)
            }
        }
        drawPath(
            path = arcPath,
            color = NauticalCyan.copy(alpha = 0.25f),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
        )

        // 3. Highlight Golden Hour segments (dawn to sunrise, and sunset to dusk)
        val morningPath = Path().apply {
            val p0 = getSunPoint(0f)
            moveTo(p0.x, p0.y)
            for (i in 1..10) {
                val t = i / 10f * sunriseFraction
                val pt = getSunPoint(t)
                lineTo(pt.x, pt.y)
            }
        }
        drawPath(
            path = morningPath,
            color = CautionYellow.copy(alpha = 0.85f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        val eveningPath = Path().apply {
            val p0 = getSunPoint(sunsetFraction)
            moveTo(p0.x, p0.y)
            for (i in 1..10) {
                val t = sunsetFraction + (i / 10f * (1f - sunsetFraction))
                val pt = getSunPoint(t)
                lineTo(pt.x, pt.y)
            }
        }
        drawPath(
            path = eveningPath,
            color = CautionYellow.copy(alpha = 0.85f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Sunrise & Sunset node dots
        drawCircle(CautionYellow, radius = 2.5.dp.toPx(), center = getSunPoint(sunriseFraction))
        drawCircle(CautionYellow, radius = 2.5.dp.toPx(), center = getSunPoint(sunsetFraction))

        // 4. Current sun location if currently daylight
        if (isDaylight) {
            val sunPos = getSunPoint(sunProgress)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CautionYellow.copy(alpha = 0.45f), Color.Transparent),
                    center = sunPos,
                    radius = 12.dp.toPx()
                ),
                radius = 12.dp.toPx(),
                center = sunPos
            )
            drawCircle(Color(0xFFFFEA00), radius = 4.5.dp.toPx(), center = sunPos)
            drawCircle(Color.White, radius = 2.2.dp.toPx(), center = sunPos)
        }
    }
}

@Composable
private fun SelectedMoonRow(
    day: CalendarDay,
    moonrise: ZonedDateTime?,
    moonset: ZonedDateTime?,
    locale: Locale
) {
    val phaseName = stringResource(day.moon.phase.label)
    val percent = (day.moon.illumination * 100).roundToInt()
    val animationsOff = animationsDisabled()
    val lit = remember { Animatable(0f) }
    val spinY = remember { Animatable(0f) }
    var previousDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(day.moon.date) {
        val target = day.moon.illumination.toFloat()
        val prev = previousDate
        previousDate = day.moon.date
        if (prev != null && prev != day.moon.date && !animationsOff) {
            val isForward = day.moon.date.isAfter(prev)
            spinY.snapTo(if (isForward) -35f else 35f)
            spinY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 320f))
        }
        if (animationsOff) lit.snapTo(target)
        else lit.animateTo(target, tween(durationMillis = if (lit.value == 0f) 800 else 400))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Moon3D(
                illumination = lit.value.toDouble(),
                waxing = day.moon.waxing,
                description = stringResource(R.string.astro_moon_description, phaseName, percent),
                diameter = 68.dp,
                interactiveTilt = !animationsOff,
                rotationYOffset = spinY.value
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = phaseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighContrast,
                        modifier = Modifier.weight(1f)
                    )
                    when (day.tideStrength) {
                        TideStrength.SPRING -> Chip(stringResource(R.string.astro_spring_tide), PrimeGreen)
                        TideStrength.NEAP -> Chip(stringResource(R.string.astro_neap_tide), CautionYellow)
                        TideStrength.NORMAL -> Chip(stringResource(R.string.astro_normal_tide), TextMuted)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.astro_illumination, percent), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(hijriText(day.hijri), style = MaterialTheme.typography.bodySmall, color = NauticalCyan)
            }
        }

        // Practical Angler Tide Advice (Petua Arus & Fasa Air)
        Spacer(Modifier.height(8.dp))
        val tideAdvice = when (day.tideStrength) {
            TideStrength.SPRING -> stringResource(R.string.astro_spring_advice)
            TideStrength.NEAP -> stringResource(R.string.astro_neap_advice)
            TideStrength.NORMAL -> stringResource(R.string.astro_normal_advice)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when (day.tideStrength) {
                TideStrength.SPRING -> PrimeGreen.copy(alpha = 0.10f)
                TideStrength.NEAP -> CautionYellow.copy(alpha = 0.10f)
                TideStrength.NORMAL -> OceanMidnight
            },
            border = BorderStroke(1.dp, when (day.tideStrength) {
                TideStrength.SPRING -> PrimeGreen.copy(alpha = 0.35f)
                TideStrength.NEAP -> CautionYellow.copy(alpha = 0.35f)
                TideStrength.NORMAL -> OceanCardBorder
            }),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tideAdvice,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (day.tideStrength) {
                        TideStrength.SPRING -> PrimeGreen
                        TideStrength.NEAP -> CautionYellow
                        TideStrength.NORMAL -> TextMuted
                    }
                )
            }
        }

        // Paired Moonrise & Moonset inside the Moon Chapter for today
        if (moonrise != null || moonset != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TimeColumn(R.string.astro_moonrise, moonrise, locale, Modifier.weight(1f))
                TimeColumn(R.string.astro_moonset, moonset, locale, Modifier.weight(1f))
            }
        }
    }
}

/** 30 days of moons with Air Besar / Air Mati indicators and a clear legend below. */
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
                    Moon3D(
                        illumination = day.moon.illumination,
                        waxing = day.moon.waxing,
                        description = null,
                        diameter = 24.dp,
                        isThumbnail = true
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = day.hijri.hijri.day.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (day.tideStrength) {
                                TideStrength.SPRING -> PrimeGreen
                                TideStrength.NEAP -> CautionYellow
                                TideStrength.NORMAL -> TextHighContrast
                            }
                        )
                        if (day.tideStrength != TideStrength.NORMAL) {
                            Spacer(Modifier.width(3.dp))
                            Canvas(modifier = Modifier.size(5.dp)) {
                                drawCircle(if (day.tideStrength == TideStrength.SPRING) PrimeGreen else CautionYellow)
                            }
                        }
                    }
                }
            }
        }
    }

    // Legend Row under the calendar strip
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.moon_calendar_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Canvas(modifier = Modifier.size(6.dp)) { drawCircle(PrimeGreen) }
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.astro_legend_spring), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.width(8.dp))
        Canvas(modifier = Modifier.size(6.dp)) { drawCircle(CautionYellow) }
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.astro_legend_neap), style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
private fun SolunarRow(rated: RatedPeriod, nowTime: ZonedDateTime, locale: Locale) {
    val period = rated.period
    val isMajor = period.type == SolunarType.MAJOR
    val active = rated.status == PeriodStatus.NOW
    val faded = rated.status == PeriodStatus.PAST
    val accent = if (isMajor) PrimeGreen else NauticalCyan
    val animationsOff = animationsDisabled()

    val pulseAlpha = if (active && !animationsOff) {
        val transition = rememberInfiniteTransition(label = "solunarPulse")
        val alpha by transition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.28f,
            animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "solunarAlpha"
        )
        alpha
    } else if (active) 0.18f else 0f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) accent.copy(alpha = pulseAlpha) else OceanMidnight,
        border = if (active) BorderStroke(1.dp, accent) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(if (isMajor) R.string.astro_solunar_major else R.string.astro_solunar_minor),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (faded) TextMuted.copy(alpha = 0.5f) else accent,
                modifier = Modifier.width(68.dp)
            )
            Text(
                text = stringResource(R.string.astro_solunar_range, clock(period.start, locale), clock(period.end, locale)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (faded) TextMuted.copy(alpha = 0.5f) else TextHighContrast,
                modifier = Modifier.weight(1f)
            )
            when (rated.status) {
                PeriodStatus.NOW -> Chip(stringResource(R.string.astro_solunar_active_now), accent)
                PeriodStatus.NEXT -> {
                    val minutesUntil = Duration.between(nowTime, period.start).toMinutes().coerceAtLeast(0)
                    val badge = if (minutesUntil < 60) {
                        stringResource(R.string.astro_solunar_starts_in_minutes, minutesUntil)
                    } else {
                        stringResource(R.string.astro_solunar_starts_in_hours_minutes, minutesUntil / 60, minutesUntil % 60)
                    }
                    Chip(badge, NauticalCyan)
                }
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
 * 3D spherical moon with Lambertian lighting falloff, celestial glow halo, earthshine volume base,
 * lunar maria and crater features, and interactive 3D perspective tilt.
 */
@Composable
private fun Moon3D(
    illumination: Double,
    waxing: Boolean,
    description: String?,
    diameter: Dp,
    modifier: Modifier = Modifier,
    isThumbnail: Boolean = false,
    interactiveTilt: Boolean = false,
    rotationYOffset: Float = 0f
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val rotX by animateFloatAsState(
        targetValue = if (interactiveTilt) dragY.coerceIn(-25f, 25f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "moonTiltX"
    )
    val rotY by animateFloatAsState(
        targetValue = if (interactiveTilt) (rotationYOffset + dragX).coerceIn(-45f, 45f) else rotationYOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "moonTiltY"
    )

    val semanticsModifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    val gestureModifier = if (interactiveTilt) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = { dragX = 0f; dragY = 0f },
                onDragCancel = { dragX = 0f; dragY = 0f }
            ) { change, dragAmount ->
                change.consume()
                dragX += dragAmount.x * 0.4f
                dragY -= dragAmount.y * 0.4f
            }
        }
    } else Modifier

    Canvas(
        modifier = modifier
            .size(diameter)
            .graphicsLayer {
                rotationX = rotX
                rotationY = rotY
                cameraDistance = 14f * density
            }
            .then(gestureModifier)
            .then(semanticsModifier)
    ) {
        val totalRadius = size.minDimension / 2f
        val r = if (isThumbnail) totalRadius * 0.90f else totalRadius * 0.80f
        val c = center

        // 1. Celestial glow aura behind the sphere (intensifies with illumination)
        if (illumination > 0.05) {
            val glowRadius = if (isThumbnail) totalRadius else r * 1.30f
            val glowAlpha = (illumination * 0.35f).toFloat().coerceIn(0.06f, 0.40f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NauticalCyan.copy(alpha = glowAlpha),
                        NauticalCyan.copy(alpha = glowAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = c
            )
        }

        // 2. Dark side 3D sphere volume (Earthshine base)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF16253B),
                    Color(0xFF0F1A2A),
                    Color(0xFF08101C)
                ),
                center = Offset(c.x, c.y - r * 0.2f),
                radius = r
            ),
            radius = r,
            center = c
        )
        // Subtle outer rim definition
        drawCircle(
            color = OceanCardBorder,
            radius = r,
            center = c,
            style = Stroke(width = 1.dp.toPx())
        )

        // Craters on the dark side (faint earthshine details for the main moon)
        if (!isThumbnail) {
            drawLunarSurface(c, r, isLit = false)
        }

        // 3. Lit hemisphere: computed with 3D spherical terminator
        if (illumination > 0.005) {
            val litPath = Path().apply {
                val rect = androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)
                val w = (r * (1f - 2f * illumination.toFloat())).let { kotlin.math.abs(it) }
                val ellipseRect = androidx.compose.ui.geometry.Rect(c.x - w, c.y - r, c.x + w, c.y + r)

                if (illumination >= 0.999f) {
                    addOval(rect)
                } else if (waxing) {
                    // Right limb is lit
                    arcTo(rect, -90f, 180f, forceMoveTo = true)
                    if (illumination >= 0.5f) {
                        // Gibbous: terminator curves into dark side (left)
                        arcTo(ellipseRect, 90f, 180f, forceMoveTo = false)
                    } else {
                        // Crescent: terminator curves into lit side (right)
                        arcTo(ellipseRect, 90f, -180f, forceMoveTo = false)
                    }
                    close()
                } else {
                    // Waning: Left limb is lit
                    arcTo(rect, -90f, -180f, forceMoveTo = true)
                    if (illumination >= 0.5f) {
                        // Gibbous: terminator curves into dark side (right)
                        arcTo(ellipseRect, 90f, -180f, forceMoveTo = false)
                    } else {
                        // Crescent: terminator curves into lit side (left)
                        arcTo(ellipseRect, 90f, 180f, forceMoveTo = false)
                    }
                    close()
                }
            }

            clipPath(litPath) {
                // 3D diffuse lighting centered towards the sun direction
                val sunOffset = if (waxing) {
                    Offset(c.x + r * 0.45f, c.y - r * 0.12f)
                } else {
                    Offset(c.x - r * 0.45f, c.y - r * 0.12f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF), // Specular sunlit peak
                            Color(0xFFF1F5F9), // Pure bright regolith
                            Color(0xFFCBD5E1), // Mid tone
                            Color(0xFF94A3B8), // Lunar slope
                            Color(0xFF475569)  // Shading towards terminator
                        ),
                        center = sunOffset,
                        radius = r * 1.35f
                    ),
                    radius = r,
                    center = c
                )

                if (!isThumbnail) {
                    drawLunarSurface(c, r, isLit = true)
                }

                // Spherical limb darkening: enhances 3D depth perception
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x38000000)
                        ),
                        center = c,
                        radius = r
                    ),
                    radius = r,
                    center = c
                )
            }
        }
    }
}

/** Authentic lunar maria and crater landmarks drawn with spherical foreshortening. */
private fun DrawScope.drawLunarSurface(c: Offset, r: Float, isLit: Boolean) {
    val mareColor = if (isLit) Color(0xFF64748B).copy(alpha = 0.38f) else Color(0xFF060D17).copy(alpha = 0.6f)
    val craterRim = if (isLit) Color(0xFFFFFFFF).copy(alpha = 0.65f) else Color(0xFF1E293B).copy(alpha = 0.4f)
    val craterFloor = if (isLit) Color(0xFF475569).copy(alpha = 0.45f) else Color(0xFF030712).copy(alpha = 0.7f)
    val rayColor = if (isLit) Color(0xFFFFFFFF).copy(alpha = 0.28f) else Color.Transparent

    // Maria (Dark volcanic plains)
    // Mare Tranquillitatis
    drawOval(
        color = mareColor,
        topLeft = Offset(c.x + 0.15f * r, c.y - 0.18f * r),
        size = Size(0.32f * r, 0.22f * r)
    )
    // Mare Serenitatis
    drawOval(
        color = mareColor,
        topLeft = Offset(c.x + 0.05f * r, c.y - 0.42f * r),
        size = Size(0.24f * r, 0.22f * r)
    )
    // Mare Imbrium
    drawOval(
        color = mareColor,
        topLeft = Offset(c.x - 0.42f * r, c.y - 0.45f * r),
        size = Size(0.36f * r, 0.32f * r)
    )
    // Oceanus Procellarum
    drawOval(
        color = mareColor,
        topLeft = Offset(c.x - 0.65f * r, c.y - 0.18f * r),
        size = Size(0.38f * r, 0.52f * r)
    )
    // Mare Crisium (isolated oval near eastern limb)
    drawOval(
        color = mareColor,
        topLeft = Offset(c.x + 0.52f * r, c.y - 0.28f * r),
        size = Size(0.18f * r, 0.15f * r)
    )
    // Mare Nubium
    drawOval(
        color = mareColor,
        topLeft = Offset(c.x - 0.28f * r, c.y + 0.18f * r),
        size = Size(0.26f * r, 0.22f * r)
    )

    // Tycho crater rays (prominent in south)
    if (isLit) {
        val tycho = Offset(c.x - 0.06f * r, c.y + 0.62f * r)
        val rayAngles = listOf(-60f, -40f, -15f, 10f, 35f, 70f, 110f, 150f)
        rayAngles.forEach { deg ->
            val rad = Math.toRadians(deg.toDouble())
            val len = 0.55f * r
            drawLine(
                color = rayColor,
                start = tycho,
                end = Offset((tycho.x + len * kotlin.math.cos(rad)).toFloat(), (tycho.y - len * kotlin.math.sin(rad)).toFloat()),
                strokeWidth = 1.2.dp.toPx()
            )
        }
    }

    // Key craters with sun-facing bright rim
    fun drawCrater(centerNorm: Offset, radiusNorm: Float) {
        val pos = Offset(c.x + centerNorm.x * r, c.y + centerNorm.y * r)
        val cr = radiusNorm * r
        drawCircle(color = craterFloor, radius = cr, center = pos)
        if (isLit) {
            drawCircle(
                color = craterRim,
                radius = cr,
                center = Offset(pos.x + 0.3f * cr, pos.y - 0.3f * cr),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }

    // Tycho
    drawCrater(Offset(-0.06f, 0.62f), 0.055f)
    // Copernicus
    drawCrater(Offset(-0.25f, -0.10f), 0.050f)
    // Kepler
    drawCrater(Offset(-0.45f, -0.06f), 0.038f)
    // Aristarchus
    drawCrater(Offset(-0.48f, -0.32f), 0.032f)
}

private fun clock(time: ZonedDateTime, locale: Locale): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
