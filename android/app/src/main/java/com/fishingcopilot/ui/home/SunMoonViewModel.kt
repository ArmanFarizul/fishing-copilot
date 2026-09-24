package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.astro.Astro
import com.fishingcopilot.astro.AstroDay
import com.fishingcopilot.astro.MoonDay
import com.fishingcopilot.astro.SolunarPeriod
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.data.hijri.HijriEntry
import com.fishingcopilot.data.hijri.HijriRepository
import com.fishingcopilot.data.local.FishingDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class PeriodStatus { PAST, NOW, NEXT, LATER }

data class RatedPeriod(val period: SolunarPeriod, val status: PeriodStatus)

data class CalendarDay(val moon: MoonDay, val hijri: HijriEntry, val tideStrength: TideStrength)

data class SunMoonUiState(
    val day: AstroDay,
    val tideStrength: TideStrength,
    val periods: List<RatedPeriod>,
    /** Today first, then the next 29 days. */
    val calendar: List<CalendarDay>
)

/**
 * Offline sun, moon and solunar times for the home spot, recomputed each minute from the device clock,
 * plus a 30-day moon calendar with JAKIM Hijri dates (MABIMS estimate where JAKIM has not published yet).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SunMoonViewModel(
    spotId: Long,
    spots: FishingDao,
    private val hijri: HijriRepository,
    ticks: Flow<Long>? = null,
    private val zone: ZoneId = ZoneId.systemDefault(),
    // The moon-transit scan and a month of moon positions are CPU work, so keep them off the main thread.
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val now = (ticks ?: minuteTicks()).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    private val today = now.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.distinctUntilChanged()
    private val spot = spots.spotFlow(spotId).filterNotNull()

    private val moonDays: Flow<List<MoonDay>> = combine(spot, today) { spot, date ->
        (0L until CALENDAR_DAYS).map { Astro.moon(spot.latitude, spot.longitude, date.plusDays(it), zone) }
    }.flowOn(computeDispatcher)

    private val hijriDays: Flow<List<HijriEntry>> =
        today.flatMapLatest { hijri.calendar(it, CALENDAR_DAYS.toInt()) }.flowOn(computeDispatcher)

    private val astroNow = combine(spot, now) { spot, millis ->
        val instant = Instant.ofEpochMilli(millis)
        val day = Astro.day(spot.latitude, spot.longitude, instant.atZone(zone).toLocalDate(), zone)
        day to rate(day.solunar, instant)
    }.flowOn(computeDispatcher)

    val uiState: StateFlow<SunMoonUiState?> = combine(astroNow, moonDays, hijriDays) { (day, periods), moons, hijriDays ->
        val calendar = moons.zip(hijriDays) { moon, h -> CalendarDay(moon, h, TideStrength.ofHijriDay(h.hijri.day)) }
        SunMoonUiState(day, calendar.first().tideStrength, periods, calendar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            try {
                hijri.refreshIfNeeded(Instant.ofEpochMilli(clock()).atZone(zone).toLocalDate())
            } catch (e: IOException) {
                // Offline: the calendar falls back to the MABIMS estimate, marked as such.
            }
        }
    }

    private fun rate(periods: List<SolunarPeriod>, now: Instant): List<RatedPeriod> {
        val nextIndex = periods.indexOfFirst { it.start.toInstant().isAfter(now) }
        return periods.mapIndexed { i, p ->
            val status = when {
                !p.start.toInstant().isAfter(now) && p.end.toInstant().isAfter(now) -> PeriodStatus.NOW
                !p.end.toInstant().isAfter(now) -> PeriodStatus.PAST
                i == nextIndex -> PeriodStatus.NEXT
                else -> PeriodStatus.LATER
            }
            RatedPeriod(p, status)
        }
    }

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    companion object {
        const val CALENDAR_DAYS = 30L

        fun factory(spotId: Long, spots: FishingDao, hijri: HijriRepository) = viewModelFactory {
            initializer { SunMoonViewModel(spotId, spots, hijri) }
        }
    }
}
