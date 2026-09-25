package com.fishingcopilot.ui

import androidx.activity.compose.BackHandler
import com.fishingcopilot.ui.settings.SettingsScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fishingcopilot.FishingCopilotApp
import com.fishingcopilot.R
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.ui.home.BiteViewModel
import com.fishingcopilot.ui.home.HomeScreen
import com.fishingcopilot.ui.home.HomeViewModel
import com.fishingcopilot.ui.home.MarineViewModel
import com.fishingcopilot.ui.home.SatelliteViewModel
import com.fishingcopilot.ui.home.SunMoonViewModel
import com.fishingcopilot.ui.home.WarningViewModel
import com.fishingcopilot.ui.home.WarningsScreen
import com.fishingcopilot.ui.log.AddCatchSheet
import com.fishingcopilot.ui.log.LogScreen
import com.fishingcopilot.ui.log.LogViewModel
import com.fishingcopilot.ui.spots.SpotsScreen
import com.fishingcopilot.ui.spots.SpotsViewModel
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.launch

private enum class Tab(val label: Int) { HOME(R.string.nav_home), LOG(R.string.nav_log), SPOTS(R.string.nav_spots) }

/** The app after onboarding: bottom navigation between home, catch log and spots. */
@Composable
fun AppShell(app: FishingCopilotApp, profile: UserProfile) {
    var tab by rememberSaveable { mutableIntStateOf(Tab.HOME.ordinal) }
    val spotId = profile.homeSpotId
    val db = app.database

    // Keyed by spot, so choosing another home spot builds fresh cards for it.
    val home = spotId?.let { viewModel<HomeViewModel>(key = "home-$it", factory = HomeViewModel.factory(it, db.fishingDao(), app.tideRepository)) }
    val marine = spotId?.let { viewModel<MarineViewModel>(key = "marine-$it", factory = MarineViewModel.factory(it, db.fishingDao(), app.marineRepository)) }
    val satellite = spotId?.let {
        viewModel<SatelliteViewModel>(key = "satellite-$it", factory = SatelliteViewModel.factory(it, db.fishingDao(), app.satelliteRepository))
    }
    val warning = spotId?.let {
        viewModel<WarningViewModel>(key = "warning-$it", factory = WarningViewModel.factory(it, db.fishingDao(), app.warningRepository))
    }
    val sunMoon = spotId?.let { viewModel<SunMoonViewModel>(key = "sunmoon-$it", factory = SunMoonViewModel.factory(it, db.fishingDao(), app.hijriRepository)) }
    val bite = spotId?.let {
        viewModel<BiteViewModel>(
            key = "bite-$it",
            factory = BiteViewModel.factory(it, db.fishingDao(), app.tideRepository, app.marineRepository, app.hijriRepository)
        )
    }
    val log = viewModel<LogViewModel>(key = "log", factory = LogViewModel.factory(db.catchDao(), db.fishingDao(), app.photoStore))
    val spots = viewModel<SpotsViewModel>(key = "spots", factory = SpotsViewModel.factory(db.fishingDao(), db.catchDao(), app.profileRepository))
    val logState by log.uiState.collectAsStateWithLifecycle()

    var strike by remember { mutableStateOf<CatchConditions?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showWarnings by rememberSaveable { mutableStateOf(false) }

    // Re-plan prime-time alerts on launch and whenever the home spot changes.
    LaunchedEffect(spotId) { app.goldenAlerts.reschedule() }

    if (showWarnings && warning != null) {
        val warningState by warning.uiState.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize().background(OceanMidnight).systemBarsPadding()) {
            WarningsScreen(warningState, onRetry = warning::retry, onBack = { showWarnings = false })
        }
        return
    }
    if (showSettings) {
        Box(modifier = Modifier.fillMaxSize().background(OceanMidnight).systemBarsPadding()) {
            SettingsScreen(app, onBack = { showSettings = false })
        }
        return
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.catch_saved)

    BackHandler(enabled = tab != Tab.HOME.ordinal) { tab = Tab.HOME.ordinal }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = OceanSurface) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item.ordinal,
                        onClick = { tab = item.ordinal },
                        icon = { NavIcon(item, if (tab == item.ordinal) NauticalCyan else TextMuted) },
                        label = { Text(stringResource(item.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = NauticalCyan,
                            unselectedTextColor = TextMuted,
                            indicatorColor = NauticalCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        containerColor = OceanMidnight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (Tab.entries[tab]) {
                Tab.HOME -> HomeScreen(
                    profile, home, marine, satellite, warning, sunMoon, bite,
                    onStrike = { strike = it },
                    onOpenSettings = { showSettings = true },
                    onOpenWarnings = { showWarnings = true }
                )
                Tab.LOG -> LogScreen(logState, onDelete = log::delete)
                Tab.SPOTS -> SpotsScreen(spots, homeSpotId = spotId, snackbar = snackbar)
            }
        }
    }

    strike?.let { conditions ->
        AddCatchSheet(
            conditions = conditions,
            targetSpecies = profile.targetSpecies,
            recentBaits = logState.recentBaits,
            onSave = { species, bait, weight, length, photo, notes ->
                log.save(conditions, species, bait, weight, length, photo, notes) {
                    strike = null
                    scope.launch { snackbar.showSnackbar(savedMessage) }
                }
            },
            onDismiss = { strike = null }
        )
    }
}

/** Simple line icons drawn in code; the app ships no icon library. */
@Composable
private fun NavIcon(tab: Tab, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (tab) {
            Tab.HOME -> drawPath(
                Path().apply {
                    moveTo(w * 0.15f, h * 0.5f); lineTo(w * 0.5f, h * 0.18f); lineTo(w * 0.85f, h * 0.5f)
                    moveTo(w * 0.25f, h * 0.42f); lineTo(w * 0.25f, h * 0.85f); lineTo(w * 0.75f, h * 0.85f); lineTo(w * 0.75f, h * 0.42f)
                },
                color, style = stroke
            )
            Tab.LOG -> {
                drawRoundRect(color, topLeft = Offset(w * 0.2f, h * 0.12f), size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.76f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = stroke)
                listOf(0.35f, 0.5f, 0.65f).forEach { y -> drawLine(color, Offset(w * 0.32f, h * y), Offset(w * 0.68f, h * y), 2.dp.toPx(), StrokeCap.Round) }
            }
            Tab.SPOTS -> {
                drawPath(
                    Path().apply {
                        moveTo(w * 0.5f, h * 0.9f)
                        cubicTo(w * 0.15f, h * 0.55f, w * 0.2f, h * 0.12f, w * 0.5f, h * 0.12f)
                        cubicTo(w * 0.8f, h * 0.12f, w * 0.85f, h * 0.55f, w * 0.5f, h * 0.9f)
                    },
                    color, style = stroke
                )
                drawCircle(color, radius = w * 0.1f, center = Offset(w * 0.5f, h * 0.4f), style = stroke)
            }
        }
    }
}
