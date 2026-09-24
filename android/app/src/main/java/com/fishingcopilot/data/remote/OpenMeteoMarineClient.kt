package com.fishingcopilot.data.remote

import com.fishingcopilot.tide.SeaLevelSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Open-Meteo Marine API. No key needed; free for non-commercial use with attribution.
 * Endpoint and variables are documented in docs/sumber-data.md.
 */
class OpenMeteoMarineClient(
    private val get: suspend (url: String) -> String = ::httpGet
) {
    suspend fun seaLevel(latitude: Double, longitude: Double, start: LocalDate, end: LocalDate): List<SeaLevelSample> {
        val url = "$BASE_URL?latitude=$latitude&longitude=$longitude" +
            "&hourly=sea_level_height_msl&timeformat=unixtime&timezone=GMT" +
            "&start_date=$start&end_date=$end"
        return parseSeaLevel(get(url))
    }

    companion object {
        private const val BASE_URL = "https://marine-api.open-meteo.com/v1/marine"
        private val json = Json { ignoreUnknownKeys = true }

        fun parseSeaLevel(body: String): List<SeaLevelSample> {
            val response = json.decodeFromString<MarineResponse>(body)
            if (response.error) throw IOException("Open-Meteo: ${response.reason}")
            val hourly = requireNotNull(response.hourly) { "Open-Meteo response has no hourly block" }
            return hourly.time.zip(hourly.seaLevel).mapNotNull { (seconds, height) ->
                height?.let { SeaLevelSample(seconds * 1000, it) }
            }
        }

        private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                // Open-Meteo returns its JSON error body with HTTP 400, so read whichever stream exists.
                val stream = if (connection.responseCode < 400) connection.inputStream else connection.errorStream
                stream?.bufferedReader()?.use { it.readText() }
                    ?: throw IOException("HTTP ${connection.responseCode} from Open-Meteo")
            } finally {
                connection.disconnect()
            }
        }
    }
}

@Serializable
private data class MarineResponse(
    val error: Boolean = false,
    val reason: String? = null,
    val hourly: Hourly? = null
)

@Serializable
private data class Hourly(
    val time: List<Long>,
    @SerialName("sea_level_height_msl") val seaLevel: List<Double?>
)
