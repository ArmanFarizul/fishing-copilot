package com.fishingcopilot.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.R
import com.fishingcopilot.bite.BiteForecast
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.ui.components.AvatarBadge
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.TextMuted
import java.time.LocalTime

@Composable
fun HomeScreen(
    profile: UserProfile,
    homeViewModel: HomeViewModel?,
    marineViewModel: MarineViewModel?,
    satelliteViewModel: SatelliteViewModel?,
    warningViewModel: WarningViewModel?,
    sunMoonViewModel: SunMoonViewModel?,
    biteViewModel: BiteViewModel?,
    onStrike: (CatchConditions) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWarnings: () -> Unit
) {
    val period = remember { DayPeriod.fromHour(LocalTime.now().hour) }
    val tideState = homeViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val marineState = marineViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val satelliteState = satelliteViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val warningState = warningViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val sunMoonState = sunMoonViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val bite = biteViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val haptics = LocalHapticFeedback.current
    val strikeDescription = stringResource(R.string.strike_button_description)

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Spec "Quick Strike Snap": a long-press haptic confirms the tap even with wet hands.
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStrike(strikeConditions(profile.homeSpotId, tideState, sunMoonState, bite))
                },
                containerColor = NauticalCyan,
                contentColor = OceanMidnight,
                modifier = Modifier.semantics { contentDescription = strikeDescription }
            ) {
                Text(stringResource(R.string.strike_button), fontWeight = FontWeight.Black)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 96.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarBadge(avatar = profile.avatar, size = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(period.greeting),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profile.nickname,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                val settingsLabel = stringResource(R.string.settings_open)
                IconButton(onClick = onOpenSettings, modifier = Modifier.semantics { contentDescription = settingsLabel }) {
                    GearIcon()
                }
            }
            if (warningState != null) {
                Spacer(Modifier.height(16.dp))
                WarningChip(state = warningState, onOpen = onOpenWarnings)
            }
            if (biteViewModel != null) {
                Spacer(Modifier.height(24.dp))
                BiteScoreCard(forecast = bite)
            }
            if (homeViewModel != null && tideState != null) {
                Spacer(Modifier.height(16.dp))
                TideCard(
                    state = tideState,
                    onRetry = homeViewModel::retry,
                    onUseNearestArea = homeViewModel::useNearestArea,
                    onOffsetChange = homeViewModel::onOffsetChange,
                    onOffsetCommit = homeViewModel::onOffsetCommit
                )
            }
            if (marineViewModel != null && marineState != null) {
                Spacer(Modifier.height(16.dp))
                MarineCard(state = marineState, onRetry = marineViewModel::retry)
            }
            if (satelliteViewModel != null && satelliteState != null) {
                Spacer(Modifier.height(16.dp))
                SatelliteCard(state = satelliteState, onRetry = satelliteViewModel::retry)
            }
            if (sunMoonViewModel != null) {
                Spacer(Modifier.height(16.dp))
                SunMoonCard(state = sunMoonState)
            }
            if (homeViewModel != null || marineViewModel != null) {
                val uriHandler = LocalUriHandler.current
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.attribution_open_meteo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(role = Role.Button) { uriHandler.openUri("https://open-meteo.com/") }
                        .padding(8.dp)
                )
            }
        }
    }
}

/** What the cards show right now, frozen at the moment of the strike. */
private fun strikeConditions(
    spotId: Long?,
    tide: HomeUiState?,
    sunMoon: SunMoonUiState?,
    bite: BiteForecast?
): CatchConditions {
    val summary = (tide?.tide as? TideCardState.Ready)?.summary
    val today = sunMoon?.calendar?.firstOrNull()
    return CatchConditions(
        timestamp = System.currentTimeMillis(),
        spotId = spotId,
        spotName = tide?.spotName,
        waterLevel = summary?.heightNow,
        rising = summary?.rising,
        moonPhase = today?.moon?.phase,
        hijri = today?.hijri?.hijri,
        biteScore = bite?.now?.score?.score
    )
}

/** Settings gear drawn in code; the app ships no icon library. */
@Composable
private fun GearIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.dp.toPx())
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(TextMuted, radius = size.minDimension * 0.28f, center = center, style = stroke)
        drawCircle(TextMuted, radius = size.minDimension * 0.1f, center = center, style = stroke)
        repeat(8) { i ->
            val angle = Math.toRadians(i * 45.0)
            val inner = size.minDimension * 0.3f
            val outer = size.minDimension * 0.45f
            drawLine(
                TextMuted,
                Offset(center.x + inner * kotlin.math.cos(angle).toFloat(), center.y + inner * kotlin.math.sin(angle).toFloat()),
                Offset(center.x + outer * kotlin.math.cos(angle).toFloat(), center.y + outer * kotlin.math.sin(angle).toFloat()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

