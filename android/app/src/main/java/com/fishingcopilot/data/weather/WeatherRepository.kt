package com.fishingcopilot.data.weather

import com.fishingcopilot.data.remote.ForecastClient
import com.fishingcopilot.data.spots.haversineKm
import com.fishingcopilot.weather.Forecast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class CachedForecast(val fetchedAtMillis: Long, val forecast: Forecast)

/** Last forecast for where the angler was, kept as a file so it still shows without signal. */
class WeatherRepository(
    private val cacheFile: File,
    private val client: ForecastClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val cached = MutableStateFlow<CachedForecast?>(null)
    private val lock = Mutex()

    val data: Flow<CachedForecast?> = cached.onStart { if (cached.value == null) loadCache() }

    /** Downloads when there is no copy, it is over 30 minutes old, or the angler moved more than 5 km. */
    suspend fun refreshIfStale(latitude: Double, longitude: Double): Boolean = lock.withLock {
        if (cached.value == null) loadCache()
        val copy = cached.value
        if (copy != null && clock() - copy.fetchedAtMillis < REFRESH_AFTER_MILLIS &&
            haversineKm(latitude, longitude, copy.forecast.latitude, copy.forecast.longitude) < MOVED_KM
        ) return false
        val body = client.download(latitude, longitude)
        val now = clock()
        withContext(io) {
            cacheFile.parentFile?.mkdirs()
            val partial = File(cacheFile.path + ".part")
            partial.writeText(body)
            if (!partial.renameTo(cacheFile)) throw IOException("Could not save $cacheFile")
            cacheFile.setLastModified(now)
        }
        cached.value = CachedForecast(now, ForecastClient.parse(body))
        true
    }

    private suspend fun loadCache() {
        cached.value = withContext(io) {
            if (!cacheFile.exists()) return@withContext null
            try {
                CachedForecast(cacheFile.lastModified(), ForecastClient.parse(cacheFile.readText()))
            } catch (e: IOException) {
                null
            }
        }
    }

    companion object {
        const val REFRESH_AFTER_MILLIS = 30L * 60 * 1000
        // Open-Meteo's grid is about 11 km; closer than 5 km gives the same forecast.
        const val MOVED_KM = 5.0
    }
}
