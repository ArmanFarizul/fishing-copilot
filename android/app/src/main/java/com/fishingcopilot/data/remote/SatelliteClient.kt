package com.fishingcopilot.data.remote

import com.fishingcopilot.satellite.SatelliteData
import com.fishingcopilot.satellite.SatelliteGrid
import com.fishingcopilot.satellite.SatelliteLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.DateTimeException
import java.time.LocalDate

/** Daily satellite summary published by our own pipeline (pipeline/README.md). */
class SatelliteClient(
    private val get: suspend (url: String) -> String = ::httpGet
) {
    /** Returns the raw body only after it parses, so a broken download never replaces a good cache. */
    suspend fun download(): String = get(URL_LATEST).also { body ->
        try {
            parse(body)
        } catch (e: IllegalArgumentException) {
            // Covers SerializationException; to callers either is just a bad download.
            throw IOException("Unreadable satellite file", e)
        } catch (e: DateTimeException) {
            throw IOException("Unreadable satellite file", e)
        }
    }

    companion object {
        const val URL_LATEST = "https://github.com/ArmanFarizul/fishing-copilot/releases/download/latest-data/daily_marine_fronts.json"
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): SatelliteData {
            val doc = json.decodeFromString<FrontsDocument>(body)
            if (doc.version != 1) throw IOException("Unsupported satellite file version ${doc.version}")
            val g = doc.grid
            val grid = SatelliteGrid(g.lat0, g.lon0, g.step, g.nlat, g.nlon)
            fun layer(name: String) = doc.layers[name]?.let { l ->
                if (l.values.size != grid.nlat * grid.nlon) throw IOException("Layer $name has ${l.values.size} cells")
                SatelliteLayer(LocalDate.parse(l.date), l.values)
            }
            return SatelliteData(grid, layer("sst_c"), layer("sst_front_c_per_10km"), layer("chl_mg_m3"), layer("zsd_m"))
        }
    }
}

@Serializable
private data class FrontsDocument(val version: Int, val grid: GridJson, val layers: Map<String, LayerJson>)

@Serializable
private data class GridJson(val lat0: Double, val lon0: Double, val step: Double, val nlat: Int, val nlon: Int)

@Serializable
private data class LayerJson(val date: String, val values: List<Double?>)

/** GitHub redirects release downloads to its asset host; HttpURLConnection follows https to https. */
private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        if (connection.responseCode >= 400) throw IOException("HTTP ${connection.responseCode} from $url")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
