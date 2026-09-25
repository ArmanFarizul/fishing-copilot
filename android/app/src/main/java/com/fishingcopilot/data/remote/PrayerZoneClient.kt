package com.fishingcopilot.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * JAKIM zone for a point on land, from the community waktusolat.app API (JAKIM has no such lookup).
 * The times themselves still come from JAKIM. Returns null at sea, where no zone covers the point.
 */
class PrayerZoneClient(private val get: suspend (url: String) -> String? = ::zoneGet) {
    suspend fun zoneAt(latitude: Double, longitude: Double): PrayerZone? {
        val body = get(String.format(Locale.ROOT, "https://api.waktusolat.app/zones/%.4f/%.4f", latitude, longitude))
            ?: return null
        return try {
            json.decodeFromString<ZoneResponse>(body).let { r ->
                r.zone?.takeIf { it.isNotBlank() }?.let { PrayerZone(it, r.district?.takeIf { d -> d.isNotBlank() }) }
            }
        } catch (e: IllegalArgumentException) {
            throw IOException("Unreadable zone response", e)
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** A JAKIM zone code such as "TRG01" and the district it is named after, when known. */
data class PrayerZone(val code: String, val district: String?)

@Serializable
private data class ZoneResponse(val zone: String? = null, val district: String? = null)

/** Body of a 200 reply; null when the service says the point has no zone (it answers 404 or 500 at sea). */
private suspend fun zoneGet(url: String): String? = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        when (connection.responseCode) {
            in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
            404, 500 -> null
            else -> throw IOException("HTTP ${connection.responseCode} from waktusolat.app")
        }
    } finally {
        connection.disconnect()
    }
}
