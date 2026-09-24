package com.fishingcopilot.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.ProfileRepository
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.data.spots.CoastalArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SpotSelection {
    val latitude: Double
    val longitude: Double

    data class Area(val area: CoastalArea) : SpotSelection {
        override val latitude get() = area.latitude
        override val longitude get() = area.longitude
    }

    data class Point(override val latitude: Double, override val longitude: Double) : SpotSelection
}

data class OnboardingUiState(
    val step: Int = 0,
    val nickname: String = "",
    val avatar: Avatar = Avatar.JETTY,
    val fishingStyle: FishingStyle? = null,
    val targetSpecies: Set<Species> = emptySet(),
    val spotSelection: SpotSelection? = null,
    val spotName: String = "",
    /** True once the user types a name, so picking another spot no longer overwrites it. */
    val spotNameEdited: Boolean = false,
    val isSaving: Boolean = false
) {
    val isLastStep: Boolean get() = step == LAST_STEP

    val canContinue: Boolean
        get() = when (step) {
            0 -> nickname.isNotBlank()
            1 -> fishingStyle != null
            2 -> targetSpecies.isNotEmpty()
            else -> spotSelection != null && spotName.isNotBlank()
        }

    companion object {
        const val STEP_COUNT = 4
        const val LAST_STEP = STEP_COUNT - 1
        const val NICKNAME_MAX_LENGTH = 20
        const val SPOT_NAME_MAX_LENGTH = 30
    }
}

class OnboardingViewModel(
    private val repository: ProfileRepository,
    private val insertSpot: suspend (SpotEntity) -> Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value.take(OnboardingUiState.NICKNAME_MAX_LENGTH)) }
    }

    fun onAvatarSelect(avatar: Avatar) {
        _uiState.update { it.copy(avatar = avatar) }
    }

    fun onFishingStyleSelect(style: FishingStyle) {
        _uiState.update { it.copy(fishingStyle = style) }
    }

    fun onSpeciesToggle(species: Species) {
        _uiState.update {
            val selected = if (species in it.targetSpecies) it.targetSpecies - species else it.targetSpecies + species
            it.copy(targetSpecies = selected)
        }
    }

    /** [defaultName] is the localised label for the selection; it fills the name field unless the user has typed one. */
    fun onSpotSelect(selection: SpotSelection, defaultName: String) {
        _uiState.update {
            it.copy(
                spotSelection = selection,
                spotName = if (it.spotNameEdited) it.spotName else defaultName.take(OnboardingUiState.SPOT_NAME_MAX_LENGTH)
            )
        }
    }

    fun onSpotNameChange(value: String) {
        _uiState.update { it.copy(spotName = value.take(OnboardingUiState.SPOT_NAME_MAX_LENGTH), spotNameEdited = true) }
    }

    fun back() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    /**
     * Advances a step, or on the last step saves the home spot and then the profile pointing at it.
     * The app leaves onboarding once the saved profile is emitted.
     */
    fun next() {
        val state = _uiState.value
        if (!state.canContinue || state.isSaving) return
        if (!state.isLastStep) {
            _uiState.update { it.copy(step = it.step + 1) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val spot = requireNotNull(state.spotSelection)
            val spotId = insertSpot(
                SpotEntity(
                    name = state.spotName.trim(),
                    latitude = spot.latitude,
                    longitude = spot.longitude,
                    isFavorite = true
                )
            )
            repository.save(
                UserProfile(
                    nickname = state.nickname.trim(),
                    avatar = state.avatar,
                    fishingStyle = requireNotNull(state.fishingStyle),
                    targetSpecies = state.targetSpecies,
                    homeSpotId = spotId
                )
            )
        }
    }

    companion object {
        fun factory(repository: ProfileRepository, insertSpot: suspend (SpotEntity) -> Long) = viewModelFactory {
            initializer { OnboardingViewModel(repository, insertSpot) }
        }
    }
}
