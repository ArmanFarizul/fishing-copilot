package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.marine.MarineRepository
import com.fishingcopilot.marine.MarineSummary
import com.fishingcopilot.marine.summarizeMarine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface MarineCardState {
    data object Loading : MarineCardState
    data object Offline : MarineCardState
    data class Ready(val summary: MarineSummary, val fetchedAtMillis: Long, val stale: Boolean) : MarineCardState
}

class MarineViewModel(
    private val spotId: Long,
    private val spots: FishingDao,
    private val marine: MarineRepository,
    ticks: Flow<Long>? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val failed = MutableStateFlow(false)

    val uiState: StateFlow<MarineCardState> = combine(
        marine.forecast(spotId),
        failed,
        ticks ?: minuteTicks()
    ) { cached, failed, now ->
        // A cache keeps the card useful offline; it only gets a stale note.
        val summary = cached?.let { summarizeMarine(it.hours, now) }
        when {
            summary != null -> MarineCardState.Ready(summary, cached.fetchedAtMillis, now - cached.fetchedAtMillis > STALE_AFTER_MILLIS)
            failed -> MarineCardState.Offline
            else -> MarineCardState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarineCardState.Loading)

    init {
        viewModelScope.launch {
            var first = true
            // A moved spot (e.g. "use nearest area" on the tide card) needs a fresh forecast for its new position.
            spots.spotFlow(spotId).filterNotNull().distinctUntilChangedBy { it.latitude to it.longitude }.collect { spot ->
                download(spot, force = !first)
                first = false
            }
        }
    }

    fun retry() {
        viewModelScope.launch { download(spots.spotFlow(spotId).filterNotNull().first(), force = false) }
    }

    private suspend fun download(spot: SpotEntity, force: Boolean) {
        failed.value = try {
            if (force) marine.refresh(spot) else marine.refreshIfStale(spot)
            false
        } catch (e: IOException) {
            true
        }
    }

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    companion object {
        const val STALE_AFTER_MILLIS = 6L * 60 * 60 * 1000

        fun factory(spotId: Long, spots: FishingDao, marine: MarineRepository) = viewModelFactory {
            initializer { MarineViewModel(spotId, spots, marine) }
        }
    }
}
