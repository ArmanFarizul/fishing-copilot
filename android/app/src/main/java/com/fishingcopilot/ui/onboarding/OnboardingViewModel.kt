package com.fishingcopilot.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.ProfileRepository
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.data.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val nickname: String = "",
    val avatar: Avatar = Avatar.JETTY,
    val fishingStyle: FishingStyle? = null,
    val targetSpecies: Set<Species> = emptySet(),
    val isSaving: Boolean = false
) {
    val isLastStep: Boolean get() = step == LAST_STEP

    val canContinue: Boolean
        get() = when (step) {
            0 -> nickname.isNotBlank()
            1 -> fishingStyle != null
            else -> targetSpecies.isNotEmpty()
        }

    companion object {
        const val STEP_COUNT = 3
        const val LAST_STEP = STEP_COUNT - 1
        const val NICKNAME_MAX_LENGTH = 20
    }
}

class OnboardingViewModel(private val repository: ProfileRepository) : ViewModel() {
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

    fun back() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    /** Advances a step, or saves the profile on the last step. The app leaves onboarding once the saved profile is emitted. */
    fun next() {
        val state = _uiState.value
        if (!state.canContinue || state.isSaving) return
        if (!state.isLastStep) {
            _uiState.update { it.copy(step = it.step + 1) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.save(
                UserProfile(
                    nickname = state.nickname.trim(),
                    avatar = state.avatar,
                    fishingStyle = requireNotNull(state.fishingStyle),
                    targetSpecies = state.targetSpecies
                )
            )
        }
    }

    companion object {
        fun factory(repository: ProfileRepository) = viewModelFactory {
            initializer { OnboardingViewModel(repository) }
        }
    }
}
