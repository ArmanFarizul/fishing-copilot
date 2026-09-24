package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.data.tide.NoMarineDataException
import com.fishingcopilot.data.tide.TideRepository
import com.fishingcopilot.tide.TideSummary
import com.fishingcopilot.tide.summarize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface TideCardState {
    data object Loading : TideCardState
    data object Offline : TideCardState
    data class NoMarineData(val nearest: CoastalArea, val distanceKm: Double) : TideCardState
    data class Ready(val summary: TideSummary) : TideCardState
}

data class HomeUiState(
    val spotName: String? = null,
    val offsetMinutes: Int = 0,
    val tide: TideCardState = TideCardState.Loading,
    /** The instant the summary was computed for, so the card's countdowns and chart agree with it. */
    val now: Long = 0
)

private enum class Sync { WORKING, DONE, FAILED, NO_DATA }

class HomeViewModel(
    private val spotId: Long,
    private val spots: FishingDao,
    private val tides: TideRepository,
    ticks: Flow<Long>? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val sync = MutableStateFlow(Sync.WORKING)

    /** Slider value while the user is dragging; saved to the spot on release. */
    private val offsetDraft = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        spots.spotFlow(spotId),
        tides.model(spotId),
        sync,
        offsetDraft,
        ticks ?: minuteTicks()
    ) { spot, model, sync, draft, now ->
        val offset = draft ?: spot?.tideOffsetMinutes ?: 0
        // A cached model keeps working offline, so it wins over a failed refresh.
        val tide = when {
            model != null -> TideCardState.Ready(model.summarize(now, offset))
            sync == Sync.NO_DATA && spot != null -> CoastalArea.nearest(spot.latitude, spot.longitude)
                .let { (area, km) -> TideCardState.NoMarineData(area, km) }
            sync == Sync.FAILED -> TideCardState.Offline
            else -> TideCardState.Loading
        }
        HomeUiState(spot?.name, offset, tide, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { download(currentSpot(), force = false) }
    }

    fun retry() {
        viewModelScope.launch { download(currentSpot(), force = false) }
    }

    /** Moves an inland spot to the nearest preset coastal area, keeping its name, then fetches tides there. */
    fun useNearestArea() {
        viewModelScope.launch {
            val spot = currentSpot()
            val (area, _) = CoastalArea.nearest(spot.latitude, spot.longitude)
            val moved = spot.copy(latitude = area.latitude, longitude = area.longitude)
            spots.updateSpot(moved)
            download(moved, force = true)
        }
    }

    fun onOffsetChange(minutes: Int) {
        offsetDraft.value = minutes
    }

    fun onOffsetCommit() {
        val minutes = offsetDraft.value ?: return
        viewModelScope.launch {
            spots.updateSpot(currentSpot().copy(tideOffsetMinutes = minutes))
            offsetDraft.value = null
        }
    }

    private suspend fun currentSpot(): SpotEntity = spots.spotFlow(spotId).filterNotNull().first()

    private suspend fun download(spot: SpotEntity, force: Boolean) {
        sync.value = Sync.WORKING
        sync.value = try {
            if (force) tides.refresh(spot) else tides.refreshIfStale(spot)
            Sync.DONE
        } catch (e: NoMarineDataException) {
            Sync.NO_DATA
        } catch (e: IOException) {
            Sync.FAILED
        }
    }

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    companion object {
        fun factory(spotId: Long, spots: FishingDao, tides: TideRepository) = viewModelFactory {
            initializer { HomeViewModel(spotId, spots, tides) }
        }
    }
}
