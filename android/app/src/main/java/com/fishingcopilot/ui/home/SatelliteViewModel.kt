package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.satellite.SatelliteRepository
import com.fishingcopilot.satellite.SatelliteSummary
import com.fishingcopilot.satellite.summarizeSatellite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

sealed interface SatelliteCardState {
    data object Loading : SatelliteCardState
    data object Offline : SatelliteCardState
    /** The spot is outside the pipeline's area, or every cell near it is land or cloud. */
    data object NoData : SatelliteCardState
    data class Ready(val summary: SatelliteSummary, val stale: Boolean) : SatelliteCardState
}

class SatelliteViewModel(
    spotId: Long,
    spots: FishingDao,
    private val satellite: SatelliteRepository,
    private val today: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) }
) : ViewModel() {
    private val failed = MutableStateFlow(false)

    val uiState: StateFlow<SatelliteCardState> = combine(satellite.data, spots.spotFlow(spotId), failed) { cached, spot, failed ->
        when {
            cached != null && spot != null -> summarizeSatellite(cached.data, spot.latitude, spot.longitude)
                ?.let { SatelliteCardState.Ready(it, it.dataDate.isBefore(today().minusDays(STALE_AFTER_DAYS))) }
                ?: SatelliteCardState.NoData
            failed -> SatelliteCardState.Offline
            else -> SatelliteCardState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SatelliteCardState.Loading)

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            failed.value = try {
                satellite.refreshIfStale()
                false
            } catch (e: IOException) {
                true
            }
        }
    }

    companion object {
        // Products normally lag 1 to 2 days; beyond 4 the pipeline has probably stopped.
        const val STALE_AFTER_DAYS = 4L

        fun factory(spotId: Long, spots: FishingDao, satellite: SatelliteRepository) = viewModelFactory {
            initializer { SatelliteViewModel(spotId, spots, satellite) }
        }
    }
}
