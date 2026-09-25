package com.fishingcopilot.ui.components

import androidx.compose.ui.graphics.Color
import com.fishingcopilot.bite.BiteTimeline
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.PrimeGreen
import java.util.Locale
import kotlin.math.abs

/** "+0.4 m" from mean sea level; heights that round to zero read "+0.0 m" rather than "-0.0 m". */
fun signedMeters(height: Double, locale: Locale): String =
    String.format(locale, "%+.1f m", if (abs(height) < 0.05) 0.0 else height)

/** Colour of a Bite Score band, the same everywhere a score is shown: prime, good, fair, poor. */
fun biteBandColor(score: Double): Color = when {
    score >= BiteTimeline.PRIME_THRESHOLD -> PrimeGreen
    score >= 6 -> NauticalCyan
    score >= 4 -> CautionYellow
    else -> AlertRed
}
