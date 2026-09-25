package com.fishingcopilot.weather

import java.time.LocalDate

/** What the sky is doing, from the WMO weather code Open-Meteo reports. */
enum class Sky {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, HEAVY_RAIN, THUNDERSTORM;

    companion object {
        // WMO 4677 codes as used by Open-Meteo (https://open-meteo.com/en/docs). Snow codes never occur here.
        fun of(code: Int): Sky = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            45, 48 -> FOG
            in 51..57 -> DRIZZLE
            61, 63, 66, 80, 81 -> RAIN
            65, 67, 82 -> HEAVY_RAIN
            95, 96, 99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}

data class WeatherNow(val tempC: Double, val feelsC: Double?, val sky: Sky, val uvIndex: Double?, val windKn: Double?)
data class WeatherHour(val epochMillis: Long, val tempC: Double?, val rainChance: Int?, val sky: Sky)
data class WeatherDay(val date: LocalDate, val sky: Sky, val maxC: Double?, val minC: Double?, val rainChance: Int?)

data class Forecast(
    val latitude: Double,
    val longitude: Double,
    val now: WeatherNow,
    val hours: List<WeatherHour>,
    val days: List<WeatherDay>
)

enum class StormKind { THUNDERSTORM, HEAVY_RAIN }

data class StormAlert(val kind: StormKind, val atMillis: Long)

const val STORM_LOOKAHEAD_MS = 3 * 60 * 60 * 1000L

/**
 * The first thunderstorm, or failing that heavy rain, forecast within the next three hours.
 * Heavy rain also counts when rain is likely (70% or more) in a rain hour, since the model often
 * reports tropical downpours as moderate rain.
 */
fun stormAlert(hours: List<WeatherHour>, now: Long): StormAlert? {
    // The hour already under way still counts: a storm at 14:00 matters at 14:20.
    val window = hours.filter { it.epochMillis + 60 * 60 * 1000L > now && it.epochMillis <= now + STORM_LOOKAHEAD_MS }
    window.firstOrNull { it.sky == Sky.THUNDERSTORM }?.let { return StormAlert(StormKind.THUNDERSTORM, maxOf(it.epochMillis, now)) }
    window.firstOrNull { it.sky == Sky.HEAVY_RAIN || (it.sky == Sky.RAIN && (it.rainChance ?: 0) >= 70) }
        ?.let { return StormAlert(StormKind.HEAVY_RAIN, maxOf(it.epochMillis, now)) }
    return null
}
