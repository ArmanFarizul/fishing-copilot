package com.fishingcopilot.data.remote

import com.fishingcopilot.weather.Forecast
import com.fishingcopilot.weather.Sky
import com.fishingcopilot.weather.WeatherDay
import com.fishingcopilot.weather.WeatherHour
import com.fishingcopilot.weather.WeatherNow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset

/** Everyday weather where the angler is: now, the next hours and seven days (Open-Meteo Forecast API). */
class ForecastClient(private val get: suspend (url: String) -> String = ::openMeteoGet) {
    /** Returns the raw body only after it parses, so a bad reply never replaces a good cache. */
    suspend fun download(latitude: Double, longitude: Double): String {
        val body = get(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,apparent_temperature,weather_code,uv_index,wind_speed_10m" +
                "&hourly=temperature_2m,precipitation_probability,weather_code" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&timeformat=unixtime&forecast_days=7&wind_speed_unit=kn"
        )
        parse(body)
        return body
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): Forecast {
            val r = try {
                json.decodeFromString<ForecastResponse>(body)
            } catch (e: IllegalArgumentException) {
                throw IOException("Unreadable forecast", e)
            }
            if (r.error) throw IOException("Open-Meteo: ${r.reason}")
            val current = r.current ?: throw IOException("Forecast has no current block")
            val hourly = r.hourly ?: throw IOException("Forecast has no hourly block")
            val daily = r.daily ?: throw IOException("Forecast has no daily block")
            // Daily times are local midnights at the forecast point; the offset turns them back into dates.
            val offset = ZoneOffset.ofTotalSeconds(r.utcOffsetSeconds)
            return Forecast(
                latitude = r.latitude,
                longitude = r.longitude,
                now = WeatherNow(
                    tempC = current.temperature ?: throw IOException("No current temperature"),
                    feelsC = current.apparent,
                    sky = Sky.of(current.weatherCode ?: 3),
                    uvIndex = current.uvIndex,
                    windKn = current.wind
                ),
                hours = hourly.time.indices.map { i ->
                    WeatherHour(
                        epochMillis = hourly.time[i] * 1000,
                        tempC = hourly.temperature.getOrNull(i),
                        rainChance = hourly.rainChance.getOrNull(i),
                        sky = Sky.of(hourly.weatherCode.getOrNull(i) ?: 3)
                    )
                },
                days = daily.time.indices.map { i ->
                    WeatherDay(
                        date = Instant.ofEpochSecond(daily.time[i]).atOffset(offset).toLocalDate(),
                        sky = Sky.of(daily.weatherCode.getOrNull(i) ?: 3),
                        maxC = daily.max.getOrNull(i),
                        minC = daily.min.getOrNull(i),
                        rainChance = daily.rainChance.getOrNull(i)
                    )
                }
            )
        }
    }
}

@Serializable
private data class ForecastResponse(
    val error: Boolean = false,
    val reason: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val current: CurrentJson? = null,
    val hourly: HourlyJson? = null,
    val daily: DailyJson? = null
)

@Serializable
private data class CurrentJson(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparent: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("uv_index") val uvIndex: Double? = null,
    @SerialName("wind_speed_10m") val wind: Double? = null
)

@Serializable
private data class HourlyJson(
    val time: List<Long>,
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val rainChance: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList()
)

@Serializable
private data class DailyJson(
    val time: List<Long>,
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val rainChance: List<Int?> = emptyList()
)
