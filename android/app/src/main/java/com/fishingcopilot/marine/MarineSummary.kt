package com.fishingcopilot.marine

import kotlinx.serialization.Serializable

/** One hour of merged Open-Meteo marine + weather forecast. Speeds in knots, heights in metres. */
@Serializable
data class MarineHour(
    val epochMillis: Long,
    val waveHeight: Double? = null,
    val wavePeriod: Double? = null,
    val waveDirection: Double? = null,
    val windKn: Double? = null,
    val gustKn: Double? = null,
    val windDirection: Double? = null,
    val pressureHpa: Double? = null,
    val currentKn: Double? = null,
    val currentDirection: Double? = null,
    val seaTempC: Double? = null
)

/** Product spec status bands: calm below 0.8 m, caution up to 1.5 m, danger above. */
enum class WaveStatus {
    CALM, CAUTION, DANGER;

    companion object {
        fun of(waveHeight: Double): WaveStatus = when {
            waveHeight < 0.8 -> CALM
            waveHeight <= 1.5 -> CAUTION
            else -> DANGER
        }
    }
}

/** Change over three hours, per the spec's barometer factor; a drop of more than 3 hPa signals a storm. */
enum class PressureTrend {
    STEADY, RISING, FALLING, FALLING_FAST;

    companion object {
        fun of(changeOverThreeHours: Double): PressureTrend = when {
            changeOverThreeHours < -3.0 -> FALLING_FAST
            changeOverThreeHours <= -1.0 -> FALLING
            changeOverThreeHours >= 1.0 -> RISING
            else -> STEADY
        }
    }
}

enum class Compass {
    N, NE, E, SE, S, SW, W, NW;

    companion object {
        fun of(degrees: Double): Compass = entries[(((degrees % 360 + 360) % 360 + 22.5) / 45).toInt() % 8]
    }
}

data class MarineSummary(
    val current: MarineHour,
    val waveStatus: WaveStatus?,
    val pressureTrend: PressureTrend?,
    val maxWaveNext24h: Double?,
    val maxWindNext24h: Double?
)

private const val HOUR_MS = 3_600_000L

/** Null when the forecast has no hour at or before [now], e.g. a cache from a different day's run. */
fun summarizeMarine(hours: List<MarineHour>, now: Long): MarineSummary? {
    val current = hours.lastOrNull { it.epochMillis <= now } ?: return null
    val threeHoursBefore = hours.firstOrNull { it.epochMillis == current.epochMillis - 3 * HOUR_MS }
    val change = current.pressureHpa?.let { p -> threeHoursBefore?.pressureHpa?.let { p - it } }
    val next24h = hours.filter { it.epochMillis > now && it.epochMillis <= now + 24 * HOUR_MS }
    return MarineSummary(
        current = current,
        waveStatus = current.waveHeight?.let(WaveStatus::of),
        pressureTrend = change?.let(PressureTrend::of),
        maxWaveNext24h = next24h.mapNotNull { it.waveHeight }.maxOrNull(),
        maxWindNext24h = next24h.mapNotNull { it.windKn }.maxOrNull()
    )
}
