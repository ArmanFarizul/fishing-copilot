package com.fishingcopilot.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.catchlog.LogSummary
import com.fishingcopilot.catchlog.PhotoStore
import com.fishingcopilot.catchlog.edited
import com.fishingcopilot.data.local.CatchDao
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.local.FishingDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoggedCatch(val entity: CatchLogEntity, val spotName: String?)

data class LogUiState(
    val catches: List<LoggedCatch> = emptyList(),
    val summary: LogSummary? = null,
    val recentBaits: List<String> = emptyList()
)

class LogViewModel(
    private val catches: CatchDao,
    spots: FishingDao,
    private val photos: PhotoStore
) : ViewModel() {
    val uiState: StateFlow<LogUiState> = combine(
        catches.allFlow(),
        spots.getAllSpotsFlow(),
        catches.recentBaitsFlow(RECENT_BAITS)
    ) { logs, spots, baits ->
        val names = spots.associate { it.id to it.name }
        LogUiState(
            catches = logs.map { LoggedCatch(it, it.spotId?.let(names::get)) },
            summary = LogSummary.of(logs),
            recentBaits = baits
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogUiState())

    fun save(
        conditions: CatchConditions,
        species: String,
        bait: String?,
        weightKg: Double?,
        lengthCm: Double?,
        pickedPhoto: String?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val photo = pickedPhoto?.let { photos.import(it) }
            catches.insert(conditions.toEntity(species, bait, weightKg, lengthCm, photo, notes))
            onSaved()
        }
    }

    /**
     * Saves changed details. [pickedPhoto] equal to the stored photo keeps it; a new pick is copied in
     * and the old copy removed; null removes the photo.
     */
    fun update(
        original: CatchLogEntity,
        species: String,
        bait: String?,
        weightKg: Double?,
        lengthCm: Double?,
        pickedPhoto: String?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val photo = if (pickedPhoto == original.photoUri) pickedPhoto else pickedPhoto?.let { photos.import(it) }
            catches.update(original.edited(species, bait, weightKg, lengthCm, photo, notes))
            // Only drop the old file once the row no longer points at it.
            original.photoUri?.takeIf { it != photo }?.let { photos.delete(it) }
            onSaved()
        }
    }

    fun delete(log: CatchLogEntity) {
        viewModelScope.launch {
            catches.delete(log)
            log.photoUri?.let { photos.delete(it) }
        }
    }

    companion object {
        const val RECENT_BAITS = 6

        fun factory(catches: CatchDao, spots: FishingDao, photos: PhotoStore) = viewModelFactory {
            initializer { LogViewModel(catches, spots, photos) }
        }
    }
}
