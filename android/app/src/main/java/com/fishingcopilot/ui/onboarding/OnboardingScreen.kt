package com.fishingcopilot.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.R
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.ui.components.AvatarBadge
import com.fishingcopilot.ui.components.GlowCheckIndicator
import com.fishingcopilot.ui.components.accent
import com.fishingcopilot.ui.components.description
import com.fishingcopilot.ui.components.habitat
import com.fishingcopilot.ui.components.icon
import com.fishingcopilot.ui.components.imageRes
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.components.subtitle
import com.fishingcopilot.ui.components.tag
import com.fishingcopilot.ui.spots.SpotPicker
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step > 0) { viewModel.back() }

    Scaffold(
        containerColor = OceanMidnight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step, state.step + 1, OnboardingUiState.STEP_COUNT).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = NauticalCyan,
                    letterSpacing = 1.2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(OnboardingUiState.STEP_COUNT) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = if (index == state.step) 24.dp else 8.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == state.step) NauticalCyan
                                    else if (index < state.step) PrimeGreen
                                    else OceanCardBorder
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (state.step + 1f) / OnboardingUiState.STEP_COUNT },
                color = NauticalCyan,
                trackColor = OceanCardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
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
                        // The map step owns its own gestures, so it must not sit inside a scrolling column.
                        .then(if (step == OnboardingUiState.LAST_STEP) Modifier else Modifier.verticalScroll(rememberScrollState()))
                        .padding(top = 16.dp, bottom = 12.dp)
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
                        2 -> SpeciesStep(
                            selected = state.targetSpecies,
                            onToggle = viewModel::onSpeciesToggle
                        )
                        else -> SpotStep(
                            selection = state.spotSelection,
                            spotName = state.spotName,
                            onSelect = viewModel::onSpotSelect,
                            onNameChange = viewModel::onSpotNameChange
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (state.step > 0) {
                    TextButton(onClick = viewModel::back) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            color = TextMuted
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = viewModel::next,
                    enabled = state.canContinue && !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NauticalCyan,
                        contentColor = OceanMidnight,
                        disabledContainerColor = OceanCardBorder,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (state.isLastStep) R.string.onboarding_finish else R.string.onboarding_next
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = TextHighContrast
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = TextMuted
    )
    Spacer(Modifier.height(20.dp))
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
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NauticalCyan,
            unfocusedBorderColor = OceanCardBorder,
            focusedLabelColor = NauticalCyan,
            unfocusedLabelColor = TextMuted,
            focusedTextColor = TextHighContrast,
            unfocusedTextColor = TextHighContrast,
            focusedContainerColor = OceanSurface,
            unfocusedContainerColor = OceanSurface
        ),
        shape = RoundedCornerShape(14.dp),
        supportingText = {
            Text(
                text = "${nickname.length}/${OnboardingUiState.NICKNAME_MAX_LENGTH}",
                color = TextMuted
            )
        },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))

    // Active Captain / Avatar Card Preview
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OceanSurface,
        border = CardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            AvatarBadge(
                avatar = avatar,
                size = 64.dp,
                selected = true
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = nickname.ifBlank { stringResource(R.string.onboarding_preview_name_placeholder) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(avatar.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = avatar.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.onboarding_avatar_label),
        style = MaterialTheme.typography.titleMedium,
        color = TextHighContrast
    )
    Spacer(Modifier.height(14.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 4,
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        Avatar.entries.forEach { option ->
            val isSelected = option == avatar
            val label = stringResource(option.label)
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "avatarScale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(14.dp))
                    .selectable(
                        selected = isSelected,
                        onClick = { onAvatarSelect(option) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp)
            ) {
                AvatarBadge(
                    avatar = option,
                    size = 64.dp,
                    selected = isSelected
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) option.accent else TextMuted
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
            val animatedBorderColor by animateColorAsState(
                targetValue = if (isSelected) NauticalCyan else OceanCardBorder,
                label = "styleBorderColor"
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = OceanSurface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = animatedBorderColor
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
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(OceanMidnight)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NauticalCyan.copy(alpha = 0.5f) else OceanCardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(style.icon),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(style.label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(style.description),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NauticalCyan.copy(alpha = 0.15f) else OceanMidnight)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NauticalCyan.copy(alpha = 0.35f) else OceanCardBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(style.tag),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) NauticalCyan else TextMuted
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    GlowCheckIndicator(
                        selected = isSelected,
                        accentColor = NauticalCyan
                    )
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.onboarding_species_selected, selected.size, selected.size),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected.isNotEmpty()) PrimeGreen else TextMuted,
            fontWeight = FontWeight.Bold
        )
    }

    val speciesList = Species.entries
    speciesList.chunked(2).forEach { rowPair ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            rowPair.forEach { species ->
                val isSelected = species in selected
                SpeciesCard(
                    species = species,
                    selected = isSelected,
                    onToggle = { onToggle(species) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowPair.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpeciesCard(
    species: Species,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) PrimeGreen else OceanCardBorder,
        label = "speciesBorderColor"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OceanSurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = animatedBorderColor
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .toggleable(
                value = selected,
                onValueChange = { onToggle() },
                role = Role.Checkbox
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(
                        if (selected) Brush.verticalGradient(
                            listOf(PrimeGreen.copy(alpha = 0.15f), OceanSurface)
                        ) else Brush.verticalGradient(
                            listOf(OceanMidnight, OceanSurface)
                        )
                    )
            ) {
                // Source art is square with the fish in a horizontal band, so cropping to the wide header only trims empty background.
                Image(
                    painter = painterResource(species.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    GlowCheckIndicator(
                        selected = selected,
                        accentColor = PrimeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(species.label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(species.subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) PrimeGreen else TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(species.habitat),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.SpotStep(
    selection: SpotSelection?,
    spotName: String,
    onSelect: (SpotSelection, String) -> Unit,
    onNameChange: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_spot_title),
        style = MaterialTheme.typography.headlineMedium,
        color = TextHighContrast
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.onboarding_spot_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted
    )
    Spacer(Modifier.height(12.dp))

    SpotPicker(selection, spotName, onSelect, onNameChange)
}

