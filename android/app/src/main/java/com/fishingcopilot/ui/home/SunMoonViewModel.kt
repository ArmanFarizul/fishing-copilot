package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.astro.Astro
import com.fishingcopilot.astro.AstroDay
import com.fishingcopilot.astro.SolunarPeriod
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.data.local.FishingDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

enum class PeriodStatus { PAST, NOW, NEXT, LATER }

data class RatedPeriod(val period: SolunarPeriod, val status: PeriodStatus)

data class SunMoonUiState(val day: AstroDay, val tideStrength: TideStrength, val periods: List<RatedPeriod>)

/** Offline sun, moon and solunar times for the home spot, recomputed each minute from the device clock. */
class SunMoonViewModel(
    spotId: Long,
    spots: FishingDao,
    ticks: Flow<Long>? = null,
    private val zone: ZoneId = ZoneId.systemDefault(),
    // The moon-transit scan is a few hundred position calculations, so keep it off the main thread.
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    val uiState: StateFlow<SunMoonUiState?> = combine(
        spots.spotFlow(spotId).filterNotNull(),
        ticks ?: minuteTicks()
    ) { spot, now ->
        val instant = Instant.ofEpochMilli(now)
        val day = Astro.day(spot.latitude, spot.longitude, instant.atZone(zone).toLocalDate(), zone)
        SunMoonUiState(day, TideStrength.of(day.moonAgeDays), rate(day.solunar, instant))
    }.flowOn(computeDispatcher).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        fun factory(spotId: Long, spots: FishingDao) = viewModelFactory {
            initializer { SunMoonViewModel(spotId, spots) }
        }
    }
}
