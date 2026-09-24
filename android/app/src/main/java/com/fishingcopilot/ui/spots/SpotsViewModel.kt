package com.fishingcopilot.ui.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.CatchDao
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.profile.ProfileRepository
import com.fishingcopilot.ui.onboarding.SpotSelection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpotItem(val spot: SpotEntity, val isHome: Boolean, val catchCount: Int)

class SpotsViewModel(
    private val spots: FishingDao,
    catches: CatchDao,
    private val profiles: ProfileRepository
) : ViewModel() {
    /** Home spot first, then newest. */
    val uiState: StateFlow<List<SpotItem>> = combine(
        spots.getAllSpotsFlow(),
        catches.countsBySpotFlow(),
        profiles.profile
    ) { all, counts, profile ->
        val countBySpot = counts.associate { it.spotId to it.count }
        all.map { SpotItem(it, it.id == profile?.homeSpotId, countBySpot[it.id] ?: 0) }
            .sortedWith(compareByDescending<SpotItem> { it.isHome }.thenByDescending { it.spot.createdAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(selection: SpotSelection, name: String) {
        viewModelScope.launch {
            spots.insertSpot(SpotEntity(name = name.trim(), latitude = selection.latitude, longitude = selection.longitude))
        }
    }

    fun rename(spot: SpotEntity, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { spots.updateSpot(spot.copy(name = trimmed)) }
    }

    fun setHome(spot: SpotEntity) {
        viewModelScope.launch {
            val profile = profiles.profile.filterNotNull().first()
            profiles.save(profile.copy(homeSpotId = spot.id))
        }
    }

    /** Refuses to delete the home spot, which every home card depends on. Returns whether it deleted. */
    fun delete(item: SpotItem): Boolean {
        if (item.isHome) return false
        viewModelScope.launch { spots.deleteSpot(item.spot) }
        return true
    }

    companion object {
        fun factory(spots: FishingDao, catches: CatchDao, profiles: ProfileRepository) = viewModelFactory {
            initializer { SpotsViewModel(spots, catches, profiles) }
        }
    }
}
