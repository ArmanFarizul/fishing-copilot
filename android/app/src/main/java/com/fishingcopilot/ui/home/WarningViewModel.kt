package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.warnings.WarningRepository
import com.fishingcopilot.warnings.WarningSummary
import com.fishingcopilot.warnings.summarizeWarnings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

sealed interface WarningState {
    /** Nothing to show yet: first download still running, or it failed with no saved copy. */
    data class Unknown(val failed: Boolean) : WarningState
    data class Ready(val summary: WarningSummary, val fetchedAtMillis: Long, val stale: Boolean) : WarningState
}

class WarningViewModel(
    spotId: Long,
    spots: FishingDao,
    private val warnings: WarningRepository,
    ticks: Flow<Long>? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val failed = MutableStateFlow(false)
    // Each tick re-checks, only while the screen is showing; the repository skips the network until its copy is 30 minutes old.
    private val ticks: Flow<Long> = (ticks ?: minuteTicks()).onEach { refresh() }

    val uiState: StateFlow<WarningState> = combine(
        warnings.data, spots.spotFlow(spotId), failed, this.ticks
    ) { cached, spot, failed, now ->
        if (cached == null || spot == null) {
            WarningState.Unknown(failed)
        } else {
            WarningState.Ready(
                summarizeWarnings(cached.warnings, spot.latitude, spot.longitude, Instant.ofEpochMilli(now)),
                cached.fetchedAtMillis,
                now - cached.fetchedAtMillis > STALE_AFTER_MILLIS
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WarningState.Unknown(failed = false))

    fun retry() {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        failed.value = try {
            warnings.refreshIfStale()
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
        const val STALE_AFTER_MILLIS = 3L * 60 * 60 * 1000

        fun factory(spotId: Long, spots: FishingDao, warnings: WarningRepository) = viewModelFactory {
            initializer { WarningViewModel(spotId, spots, warnings) }
        }
    }
}
