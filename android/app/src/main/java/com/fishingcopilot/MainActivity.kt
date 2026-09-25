package com.fishingcopilot

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.ui.AppShell
import com.fishingcopilot.ui.onboarding.OnboardingScreen
import com.fishingcopilot.ui.onboarding.OnboardingViewModel
import com.fishingcopilot.ui.theme.FishingCopilotTheme
import com.fishingcopilot.ui.theme.SunlightMode
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDisplay(sunlight = false)
        val app = application as FishingCopilotApp
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.displaySettings.sunlight.collect { sunlight ->
                    SunlightMode.enabled = sunlight
                    applyDisplay(sunlight)
                }
            }
        }
        setContent {
            FishingCopilotTheme {
                AppRoot(app)
            }
        }
    }
}

/**
 * System bar icons follow the app's own palette, not the phone's theme. Sunlight mode also asks for full
 * screen brightness while the app is in front; Android restores the user's level when it leaves.
 */
private fun ComponentActivity.applyDisplay(sunlight: Boolean) {
    val bars = if (sunlight) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) else SystemBarStyle.dark(Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    window.attributes = window.attributes.apply {
        screenBrightness = if (sunlight) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
}

private sealed interface ProfileState {
    data object Loading : ProfileState
    data object Missing : ProfileState
    data class Ready(val profile: UserProfile) : ProfileState
}

@Composable
private fun AppRoot(app: FishingCopilotApp) {
    val repository = app.profileRepository
    val profileState by remember(repository) {
        repository.profile.map { if (it == null) ProfileState.Missing else ProfileState.Ready(it) }
    }.collectAsStateWithLifecycle<ProfileState>(ProfileState.Loading)

    when (val state = profileState) {
        ProfileState.Loading -> Unit
        ProfileState.Missing -> OnboardingScreen(
            viewModel(factory = OnboardingViewModel.factory(repository) { app.database.fishingDao().insertSpot(it) })
        )
        is ProfileState.Ready -> AppShell(app, state.profile)
    }
}
