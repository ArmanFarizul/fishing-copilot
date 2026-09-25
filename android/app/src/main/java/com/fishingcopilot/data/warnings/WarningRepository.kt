package com.fishingcopilot.data.warnings

import com.fishingcopilot.data.remote.MetWarningClient
import com.fishingcopilot.warnings.MetWarning
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

data class CachedWarnings(val fetchedAtMillis: Long, val warnings: List<MetWarning>)

/** The whole national feed in one small file, like the satellite cache; its modified time is the download time. */
class WarningRepository(
    private val cacheFile: File,
    private val client: MetWarningClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val cached = MutableStateFlow<CachedWarnings?>(null)
    private val lock = Mutex()

    val data: Flow<CachedWarnings?> = cached.onStart { if (cached.value == null) loadCache() }

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
        cached.value = CachedWarnings(now, MetWarningClient.parse(body))
        true
    }

    private suspend fun loadCache() {
        cached.value = withContext(io) {
            if (!cacheFile.exists()) return@withContext null
            try {
                CachedWarnings(cacheFile.lastModified(), MetWarningClient.parse(cacheFile.readText()))
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        // Thunderstorm warnings last about three hours, so check often while the app is open.
        const val REFRESH_AFTER_MILLIS = 30L * 60 * 1000
    }
}
