package com.fishingcopilot.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.R
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.ui.components.AvatarBadge
import java.time.LocalTime

@Composable
fun HomeScreen(profile: UserProfile, homeViewModel: HomeViewModel?, marineViewModel: MarineViewModel?) {
    val period = remember { DayPeriod.fromHour(LocalTime.now().hour) }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarBadge(avatar = profile.avatar, size = 56.dp)
                Column {
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
            }
            if (homeViewModel != null) {
                val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                Spacer(Modifier.height(24.dp))
                TideCard(
                    state = state,
                    onRetry = homeViewModel::retry,
                    onUseNearestArea = homeViewModel::useNearestArea,
                    onOffsetChange = homeViewModel::onOffsetChange,
                    onOffsetCommit = homeViewModel::onOffsetCommit
                )
            }
            if (marineViewModel != null) {
                val marineState by marineViewModel.uiState.collectAsStateWithLifecycle()
                Spacer(Modifier.height(16.dp))
                MarineCard(state = marineState, onRetry = marineViewModel::retry)
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
