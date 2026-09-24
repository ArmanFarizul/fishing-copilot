package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.astro.Astro
import com.fishingcopilot.bite.BiteForecast
import com.fishingcopilot.bite.BiteInputs
import com.fishingcopilot.bite.BiteTimeline
import com.fishingcopilot.bite.SunWindow
import com.fishingcopilot.data.hijri.HijriRepository
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.marine.MarineRepository
import com.fishingcopilot.data.tide.TideRepository
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
import java.time.Instant
import java.time.ZoneId

/**
 * Bite Score for the home spot. Reads only what the other cards already store (tide model, marine
 * forecast, JAKIM Hijri days) plus offline sun times, so it makes no network requests of its own.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BiteViewModel(
    spotId: Long,
    spots: FishingDao,
    tides: TideRepository,
    marine: MarineRepository,
    hijri: HijriRepository,
    ticks: Flow<Long>? = null,
    private val zone: ZoneId = ZoneId.systemDefault(),
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val now = (ticks ?: minuteTicks()).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    private val today = now.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.distinctUntilChanged()
    private val spot = spots.spotFlow(spotId).filterNotNull()

    private val hijriDays = today.flatMapLatest { date -> hijri.calendar(date, 2) }
        .map { entries -> entries.associate { it.date to it.hijri } }

    private val sunDays = combine(spot, today) { spot, date ->
        (0L..1L).associate { offset ->
            val day = date.plusDays(offset)
            val (dawn, rise, set, dusk) = Astro.sun(spot.latitude, spot.longitude, day, zone)
            day to SunWindow(dawn, rise, set, dusk)
        }
    }

    val uiState: StateFlow<BiteForecast?> = combine(
        combine(spot, tides.model(spotId)) { spot, model -> spot to model },
        marine.forecast(spotId),
        hijriDays,
        sunDays,
        now
    ) { (spot, model), cached, hijriByDate, sunByDate, millis ->
        BiteTimeline.forecast(
            millis,
            BiteInputs(model, spot.tideOffsetMinutes, cached?.hours.orEmpty(), hijriByDate, sunByDate, zone)
        )
    }.flowOn(computeDispatcher).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    companion object {
        fun factory(spotId: Long, spots: FishingDao, tides: TideRepository, marine: MarineRepository, hijri: HijriRepository) =
            viewModelFactory { initializer { BiteViewModel(spotId, spots, tides, marine, hijri) } }
    }
}
