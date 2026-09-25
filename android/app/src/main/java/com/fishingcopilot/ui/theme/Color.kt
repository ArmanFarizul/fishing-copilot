package com.fishingcopilot.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's colours by role. The names come from the original night palette and are kept so every screen
 * reads the same; in sunlight mode each role maps to its high-contrast daylight colour.
 */
private data class Palette(
    val background: Color,
    val surface: Color,
    /** Tracks, dividers and empty bar segments. */
    val muted: Color,
    val cardBorder: Color,
    val cardBorderWidth: Dp,
    val accent: Color,
    val good: Color,
    val caution: Color,
    val alert: Color,
    val text: Color,
    val textMuted: Color,
    val selected: Color
)

private val Night = Palette(
    background = Color(0xFF030C1B),
    surface = Color(0xFF0A192F),
    muted = Color(0xFF112240),
    cardBorder = Color(0xFF112240),
    cardBorderWidth = 1.dp,
    accent = Color(0xFF00E5FF),
    good = Color(0xFF00E676),
    caution = Color(0xFFFFD600),
    alert = Color(0xFFFF1744),
    text = Color(0xFFFFFFFF),
    textMuted = Color(0xFF8892B0),
    selected = Color(0xFF003A45)
)

// Spec: pure white background, thick black 2 dp card borders, black text. Accents are darkened so each
// stays at 6:1 contrast or better on white.
private val Sunlight = Palette(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    muted = Color(0xFFD0D5DB),
    cardBorder = Color(0xFF000000),
    cardBorderWidth = 2.dp,
    accent = Color(0xFF00606E),
    good = Color(0xFF006B2E),
    caution = Color(0xFF8A5300),
    alert = Color(0xFFB3001B),
    text = Color(0xFF000000),
    textMuted = Color(0xFF3A3A3A),
    selected = Color(0xFFCDEFF3)
)

/**
 * Which palette is showing. Snapshot state, so composition and Canvas drawing that read a colour update
 * when it flips, including draw code that is not @Composable.
 */
object SunlightMode {
    var enabled by mutableStateOf(false)
}

private val palette: Palette get() = if (SunlightMode.enabled) Sunlight else Night

val OceanMidnight: Color get() = palette.background
val OceanSurface: Color get() = palette.surface
val OceanCardBorder: Color get() = palette.muted
val NauticalCyan: Color get() = palette.accent
val PrimeGreen: Color get() = palette.good
val CautionYellow: Color get() = palette.caution
val AlertRed: Color get() = palette.alert
val TextHighContrast: Color get() = palette.text
val TextMuted: Color get() = palette.textMuted
val SelectedContainer: Color get() = palette.selected

/** Inner rows inside a card: none at night, where their darker fill separates them; a grey line on white. */
val InsetBorder: BorderStroke? get() = if (SunlightMode.enabled) BorderStroke(1.dp, Sunlight.muted) else null

/** Behind the marine animations, which are drawn in light colours for the night palette. */
val AnimationBackdrop: Color get() = if (SunlightMode.enabled) Night.surface else Color.Transparent

/** Outline for cards and panels: a thin line at night, a thick black edge in sunlight. */
val CardBorder: BorderStroke get() = BorderStroke(palette.cardBorderWidth, palette.cardBorder)
