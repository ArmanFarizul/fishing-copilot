package com.fishingcopilot.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.R
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.ui.components.AvatarBadge
import com.fishingcopilot.ui.components.accent
import com.fishingcopilot.ui.components.description
import com.fishingcopilot.ui.components.label

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step > 0) { viewModel.back() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            LinearProgressIndicator(
                progress = { (state.step + 1f) / OnboardingUiState.STEP_COUNT },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_step, state.step + 1, OnboardingUiState.STEP_COUNT),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * direction / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * direction / 4 } + fadeOut())
                },
                label = "onboardingStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp)
                ) {
                    when (step) {
                        0 -> IdentityStep(
                            nickname = state.nickname,
                            avatar = state.avatar,
                            onNicknameChange = viewModel::onNicknameChange,
                            onAvatarSelect = viewModel::onAvatarSelect
                        )
                        1 -> FishingStyleStep(
                            selected = state.fishingStyle,
                            onSelect = viewModel::onFishingStyleSelect
                        )
                        else -> SpeciesStep(
                            selected = state.targetSpecies,
                            onToggle = viewModel::onSpeciesToggle
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                if (state.step > 0) {
                    TextButton(onClick = viewModel::back) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = viewModel::next,
                    enabled = state.canContinue && !state.isSaving
                ) {
                    Text(
                        stringResource(
                            if (state.isLastStep) R.string.onboarding_finish else R.string.onboarding_next
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(text = title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun IdentityStep(
    nickname: String,
    avatar: Avatar,
    onNicknameChange: (String) -> Unit,
    onAvatarSelect: (Avatar) -> Unit
) {
    StepHeader(
        title = stringResource(R.string.onboarding_identity_title),
        subtitle = stringResource(R.string.onboarding_identity_subtitle)
    )
    OutlinedTextField(
        value = nickname,
        onValueChange = onNicknameChange,
        label = { Text(stringResource(R.string.onboarding_nickname_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        supportingText = { Text("${nickname.length}/${OnboardingUiState.NICKNAME_MAX_LENGTH}") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_avatar_label),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = 3,
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        Avatar.entries.forEach { option ->
            val label = stringResource(option.label)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = option == avatar,
                        onClick = { onAvatarSelect(option) },
                        role = Role.RadioButton
                    )
            ) {
                AvatarBadge(avatar = option, size = 72.dp, selected = option == avatar)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (option == avatar) option.accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FishingStyleStep(selected: FishingStyle?, onSelect: (FishingStyle) -> Unit) {
    StepHeader(
        title = stringResource(R.string.onboarding_style_title),
        subtitle = stringResource(R.string.onboarding_style_subtitle)
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.selectableGroup()
    ) {
        FishingStyle.entries.forEach { style ->
            val isSelected = style == selected
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(style) },
                        role = Role.RadioButton
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(stringResource(style.label), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(style.description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeciesStep(selected: Set<Species>, onToggle: (Species) -> Unit) {
    StepHeader(
        title = stringResource(R.string.onboarding_species_title),
        subtitle = stringResource(R.string.onboarding_species_subtitle)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Species.entries.forEach { species ->
            FilterChip(
                selected = species in selected,
                onClick = { onToggle(species) },
                label = { Text(stringResource(species.label)) }
            )
        }
    }
}
