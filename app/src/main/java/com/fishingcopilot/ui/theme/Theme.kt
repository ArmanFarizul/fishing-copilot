package com.fishingcopilot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OceanColorScheme = darkColorScheme(
    primary = NauticalCyan,
    onPrimary = OceanMidnight,
    secondary = PrimeGreen,
    tertiary = CautionYellow,
    error = AlertRed,
    background = OceanMidnight,
    onBackground = TextHighContrast,
    surface = OceanSurface,
    onSurface = TextHighContrast,
    onSurfaceVariant = TextMuted,
    outline = OceanCardBorder
)

// Dark-only on purpose: the app is used at dawn, dusk and night on the water. Sunlight mode comes later.
@Composable
fun FishingCopilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OceanColorScheme,
        typography = FishingTypography,
        content = content
    )
}
