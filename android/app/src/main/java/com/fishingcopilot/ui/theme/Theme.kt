package com.fishingcopilot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private fun nightScheme() = darkColorScheme(
    primary = NauticalCyan,
    onPrimary = OceanMidnight,
    secondary = PrimeGreen,
    secondaryContainer = SelectedContainer,
    onSecondaryContainer = NauticalCyan,
    tertiary = CautionYellow,
    error = AlertRed,
    background = OceanMidnight,
    onBackground = TextHighContrast,
    surface = OceanSurface,
    onSurface = TextHighContrast,
    onSurfaceVariant = TextMuted,
    outline = OceanCardBorder
)

private fun sunlightScheme() = lightColorScheme(
    primary = NauticalCyan,
    onPrimary = OceanMidnight,
    secondary = PrimeGreen,
    secondaryContainer = SelectedContainer,
    onSecondaryContainer = NauticalCyan,
    tertiary = CautionYellow,
    error = AlertRed,
    background = OceanMidnight,
    onBackground = TextHighContrast,
    surface = OceanSurface,
    onSurface = TextHighContrast,
    onSurfaceVariant = TextMuted,
    outline = TextHighContrast
)

// Dark by default for dawn, dusk and night on the water; sunlight mode swaps in the daylight palette.
@Composable
fun FishingCopilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (SunlightMode.enabled) sunlightScheme() else nightScheme(),
        typography = FishingTypography,
        content = content
    )
}
