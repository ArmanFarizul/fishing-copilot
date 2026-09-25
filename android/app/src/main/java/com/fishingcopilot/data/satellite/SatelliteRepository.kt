package com.fishingcopilot.data.satellite

import com.fishingcopilot.data.remote.SatelliteClient
import com.fishingcopilot.satellite.SatelliteData
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

data class CachedSatellite(val fetchedAtMillis: Long, val data: SatelliteData)

/**
 * One file covers every spot, so it is cached as the downloaded JSON rather than per spot in Room.
 * The file's modified time is the download time.
 */
class SatelliteRepository(
    private val cacheFile: File,
    private val client: SatelliteClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val cached = MutableStateFlow<CachedSatellite?>(null)
    private val lock = Mutex()

    val data: Flow<CachedSatellite?> = cached.onStart { if (cached.value == null) loadCache() }

    /** Downloads when there is no cache or it is older than [REFRESH_AFTER_MILLIS]. Returns whether it downloaded. */
    suspend fun refreshIfStale(): Boolean = lock.withLock {
        val fetchedAt = withContext(io) { cacheFile.takeIf { it.exists() }?.lastModified() }
        if (fetchedAt != null && clock() - fetchedAt < REFRESH_AFTER_MILLIS) {
            if (cached.value == null) loadCache()
            return false
        }
        val body = client.download()
        val now = clock()
        withContext(io) {
            cacheFile.parentFile?.mkdirs()
            val partial = File(cacheFile.path + ".part")
            partial.writeText(body)
            if (!partial.renameTo(cacheFile)) throw IOException("Could not save $cacheFile")
            cacheFile.setLastModified(now)
        }
        cached.value = CachedSatellite(now, SatelliteClient.parse(body))
        true
    }

    private suspend fun loadCache() {
        cached.value = withContext(io) {
            if (!cacheFile.exists()) return@withContext null
            try {
                CachedSatellite(cacheFile.lastModified(), SatelliteClient.parse(cacheFile.readText()))
            } catch (e: Exception) {
                // A cache from an older app build with another layout is just ignored until the next download.
                null
            }
        }
    }

    companion object {
        // The pipeline publishes once a day at 02:00 UTC; checking every 6 hours picks it up the same day.
        const val REFRESH_AFTER_MILLIS = 6L * 60 * 60 * 1000
    }
}
