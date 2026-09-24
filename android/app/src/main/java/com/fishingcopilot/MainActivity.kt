package com.fishingcopilot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fishingcopilot.data.profile.ProfileRepository
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.ui.home.HomeScreen
import com.fishingcopilot.ui.onboarding.OnboardingScreen
import com.fishingcopilot.ui.onboarding.OnboardingViewModel
import com.fishingcopilot.ui.theme.FishingCopilotTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons must stay light even when the phone is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val repository = (application as FishingCopilotApp).profileRepository
        setContent {
            FishingCopilotTheme {
                AppRoot(repository)
            }
        }
    }
}

private sealed interface ProfileState {
    data object Loading : ProfileState
    data object Missing : ProfileState
    data class Ready(val profile: UserProfile) : ProfileState
}

@Composable
private fun AppRoot(repository: ProfileRepository) {
    val profileState by remember(repository) {
        repository.profile.map { if (it == null) ProfileState.Missing else ProfileState.Ready(it) }
    }.collectAsStateWithLifecycle<ProfileState>(ProfileState.Loading)

    when (val state = profileState) {
        ProfileState.Loading -> Unit
        ProfileState.Missing -> OnboardingScreen(viewModel(factory = OnboardingViewModel.factory(repository)))
        is ProfileState.Ready -> HomeScreen(state.profile)
    }
}
