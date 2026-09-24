package com.fishingcopilot.data.remote

import com.fishingcopilot.marine.MarineHour
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Hourly sea state (Marine API) merged with wind and pressure (Forecast API), both in knots.
 * One day of history is included so the pressure trend can be computed from the first hour.
 */
class OpenMeteoWeatherClient(
    private val get: suspend (url: String) -> String = ::openMeteoGet
) {
    suspend fun marineHours(latitude: Double, longitude: Double): List<MarineHour> = coroutineScope {
        val common = "latitude=$latitude&longitude=$longitude&timeformat=unixtime&timezone=GMT" +
            "&wind_speed_unit=kn&past_days=1&forecast_days=3"
        val marine = async {
            parse<MarineHourly>(
                get(
                    "https://marine-api.open-meteo.com/v1/marine?$common&hourly=wave_height,wave_period," +
                        "wave_direction,ocean_current_velocity,ocean_current_direction,sea_surface_temperature"
                )
            )
        }
        val weather = async {
            parse<WeatherHourly>(
                get(
                    "https://api.open-meteo.com/v1/forecast?$common" +
                        "&hourly=wind_speed_10m,wind_gusts_10m,wind_direction_10m,pressure_msl"
                )
            )
        }
        merge(marine.await(), weather.await())
    }

    private fun merge(marine: MarineHourly, weather: WeatherHourly): List<MarineHour> {
        val sea = marine.time.indices.associateBy { marine.time[it] }
        val air = weather.time.indices.associateBy { weather.time[it] }
        return (marine.time + weather.time).toSortedSet().map { seconds ->
            val s = sea[seconds]
            val a = air[seconds]
            MarineHour(
                epochMillis = seconds * 1000,
                waveHeight = s?.let { marine.waveHeight[it] },
                wavePeriod = s?.let { marine.wavePeriod[it] },
                waveDirection = s?.let { marine.waveDirection[it] },
                windKn = a?.let { weather.windSpeed[it] },
                gustKn = a?.let { weather.windGusts[it] },
                windDirection = a?.let { weather.windDirection[it] },
                pressureHpa = a?.let { weather.pressure[it] },
                currentKn = s?.let { marine.currentVelocity[it] },
                currentDirection = s?.let { marine.currentDirection[it] },
                seaTempC = s?.let { marine.seaTemperature[it] }
            )
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        inline fun <reified T> parse(body: String): T {
            val envelope = json.decodeFromString<Envelope<T>>(body)
            if (envelope.error) throw IOException("Open-Meteo: ${envelope.reason}")
            return requireNotNull(envelope.hourly) { "Open-Meteo response has no hourly block" }
        }
    }
}

@Serializable
private data class Envelope<T>(val error: Boolean = false, val reason: String? = null, val hourly: T? = null)

@Serializable
private data class MarineHourly(
    val time: List<Long>,
    @SerialName("wave_height") val waveHeight: List<Double?>,
    @SerialName("wave_period") val wavePeriod: List<Double?>,
    @SerialName("wave_direction") val waveDirection: List<Double?>,
    @SerialName("ocean_current_velocity") val currentVelocity: List<Double?>,
    @SerialName("ocean_current_direction") val currentDirection: List<Double?>,
    @SerialName("sea_surface_temperature") val seaTemperature: List<Double?>
)

@Serializable
private data class WeatherHourly(
    val time: List<Long>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>,
    @SerialName("wind_gusts_10m") val windGusts: List<Double?>,
    @SerialName("wind_direction_10m") val windDirection: List<Double?>,
    @SerialName("pressure_msl") val pressure: List<Double?>
)
