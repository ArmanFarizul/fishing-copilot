package com.fishingcopilot.ui.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.satellite.SatelliteRepository
import com.fishingcopilot.satellite.MapLayerCells
import com.fishingcopilot.satellite.SatelliteMapLayer
import com.fishingcopilot.satellite.mapCells
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface SatelliteMapState {
    val layer: SatelliteMapLayer

    data class Loading(override val layer: SatelliteMapLayer) : SatelliteMapState
    data class Offline(override val layer: SatelliteMapLayer) : SatelliteMapState
    /** [cells] is null when today's file has no such layer. */
    data class Ready(override val layer: SatelliteMapLayer, val cells: MapLayerCells?) : SatelliteMapState
}

class SatelliteMapViewModel(private val satellite: SatelliteRepository) : ViewModel() {
    private val layer = MutableStateFlow(SatelliteMapLayer.PLANKTON)
    private val failed = MutableStateFlow(false)

    val uiState: StateFlow<SatelliteMapState> = combine(satellite.data, layer, failed) { cached, layer, failed ->
        when {
            cached != null -> SatelliteMapState.Ready(layer, mapCells(cached.data, layer))
            failed -> SatelliteMapState.Offline(layer)
            else -> SatelliteMapState.Loading(layer)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SatelliteMapState.Loading(SatelliteMapLayer.PLANKTON))

    init {
        retry()
    }

    fun show(next: SatelliteMapLayer) {
        layer.value = next
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
        fun factory(satellite: SatelliteRepository) = viewModelFactory {
            initializer { SatelliteMapViewModel(satellite) }
        }
    }
}
