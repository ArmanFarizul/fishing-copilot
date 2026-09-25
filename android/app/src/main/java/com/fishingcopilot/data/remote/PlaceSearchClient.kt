package com.fishingcopilot.data.remote

import com.fishingcopilot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PlaceResult(val name: String, val detail: String, val latitude: Double, val longitude: Double)

/**
 * Place search through OpenStreetMap's Nominatim (operations.osmfoundation.org/policies/nominatim).
 * The policy asks for at most one request a second, an identifying User-Agent, cached results and no
 * search-as-you-type, so this only runs when the angler presses search, spaces requests and keeps
 * answers for the app's lifetime.
 */
class PlaceSearchClient(
    private val get: suspend (url: String) -> String = ::nominatimGet,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val cache = HashMap<String, List<PlaceResult>>()
    private val lock = Mutex()
    private var lastRequest = 0L

    suspend fun search(query: String, language: String): List<PlaceResult> = lock.withLock {
        val key = "$language|${query.trim().lowercase()}"
        cache[key]?.let { return@withLock it }
        val wait = lastRequest + MIN_GAP_MS - clock()
        if (wait > 0) delay(wait)
        lastRequest = clock()
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6" +
            "&q=${URLEncoder.encode(query.trim(), "UTF-8")}" +
            "&accept-language=${URLEncoder.encode(language, "UTF-8")}" +
            // Prefer Malaysian waters and neighbours without excluding them.
            "&viewbox=99.0,7.6,119.6,0.5&bounded=0"
        val results = parse(get(url))
        cache[key] = results
        results
    }

    companion object {
        private const val MIN_GAP_MS = 1_100L
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): List<PlaceResult> = try {
            json.decodeFromString<List<NominatimPlace>>(body).mapNotNull { p ->
                val lat = p.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lon = p.lon.toDoubleOrNull() ?: return@mapNotNull null
                val name = p.name?.takeIf { it.isNotBlank() } ?: p.displayName.substringBefore(",")
                PlaceResult(name, p.displayName.removePrefix(name).removePrefix(",").trim(), lat, lon)
            }
        } catch (e: IllegalArgumentException) {
            throw IOException("Unreadable search reply", e)
        }
    }
}

@Serializable
private data class NominatimPlace(
    val lat: String,
    val lon: String,
    val name: String? = null,
    @SerialName("display_name") val displayName: String = ""
)

private suspend fun nominatimGet(url: String): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        // Stock library user agents are refused by the policy.
        connection.setRequestProperty("User-Agent", "FishingCopilot/${BuildConfig.VERSION_NAME} (personal Android fishing app)")
        if (connection.responseCode >= 400) throw IOException("HTTP ${connection.responseCode} from Nominatim")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
