package com.fishingcopilot.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.R
import com.fishingcopilot.bite.BiteForecast
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.ui.components.AvatarBadge
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextHighContrast
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
    onOpenSettings: () -> Unit,
    onOpenWarnings: () -> Unit,
    hereViewModel: HereViewModel,
    spots: List<SpotEntity>,
    viewedSpot: SpotEntity?,
    homeSpotId: Long?,
    onSelectSpot: (Long) -> Unit
) {
    val hereState by hereViewModel.uiState.collectAsStateWithLifecycle()
    // Look again whenever the angler comes back to the app; the repositories skip what is still fresh.
    LifecycleResumeEffect(hereViewModel) {
        hereViewModel.refresh()
        onPauseOrDispose { }
    }
    val period = remember { DayPeriod.fromHour(LocalTime.now().hour) }
    val tideState = homeViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val marineState = marineViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val satelliteState = satelliteViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val warningState = warningViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val sunMoonState = sunMoonViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val bite = biteViewModel?.uiState?.collectAsStateWithLifecycle()?.value

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            if (warningState != null) WarningBell(warningState, onOpenWarnings)
            val settingsLabel = stringResource(R.string.settings_open)
            IconButton(onClick = onOpenSettings, modifier = Modifier.semantics { contentDescription = settingsLabel }) {
                GearIcon()
            }
        }
        Spacer(Modifier.height(24.dp))
        HereSection(hereState, viewedSpot, onRefresh = hereViewModel::refresh, now = System.currentTimeMillis())

        Spacer(Modifier.height(28.dp))
        SectionLabel(R.string.spot_section)
        Spacer(Modifier.height(4.dp))
        SpotSwitcher(spots, viewedSpot, homeSpotId, nearbySpotId = hereState.nearbySpot?.id, onSelect = onSelectSpot)
        if (biteViewModel != null) {
            Spacer(Modifier.height(16.dp))
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

/** The spot every card below follows; tap to pick another saved spot. */
@Composable
private fun SpotSwitcher(spots: List<SpotEntity>, viewed: SpotEntity?, homeSpotId: Long?, nearbySpotId: Long?, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, enabled = spots.size > 1) { open = true }
        ) {
            Text(
                text = viewed?.name.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextHighContrast
            )
            if (spots.size > 1) {
                Spacer(Modifier.width(6.dp))
                Text("▾", style = MaterialTheme.typography.titleLarge, color = NauticalCyan)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = OceanSurface) {
            spots.forEach { spot ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(spot.name, fontWeight = if (spot.id == viewed?.id) FontWeight.Bold else FontWeight.Normal, color = TextHighContrast)
                            val tags = listOfNotNull(
                                stringResource(R.string.spot_home_tag).takeIf { spot.id == homeSpotId },
                                stringResource(R.string.spot_nearby_tag).takeIf { spot.id == nearbySpotId }
                            )
                            if (tags.isNotEmpty()) Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = NauticalCyan)
                        }
                    },
                    onClick = { onSelect(spot.id); open = false }
                )
            }
        }
    }
    val tags = listOfNotNull(
        stringResource(R.string.spot_home_tag).takeIf { viewed?.id == homeSpotId },
        stringResource(R.string.spot_nearby_tag).takeIf { viewed != null && viewed.id == nearbySpotId }
    )
    if (tags.isNotEmpty()) Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = TextMuted)
}

/** What the cards show right now, frozen at the moment of the strike. */
fun strikeConditions(
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

