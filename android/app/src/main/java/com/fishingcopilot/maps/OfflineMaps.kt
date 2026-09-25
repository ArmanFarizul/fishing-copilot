package com.fishingcopilot.maps

import android.content.Context
import com.fishingcopilot.data.local.FishingDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface OfflineMapsState {
    data object Checking : OfflineMapsState

    /**
     * What is on the phone. [coversAllSpots] is false when a spot was added or moved since the last
     * download; [failed] is true when the last download stopped with an error.
     */
    data class Saved(
        val bytes: Long,
        val complete: Boolean,
        val coversAllSpots: Boolean,
        val downloadedAt: Long?,
        val failed: Boolean = false
    ) : OfflineMapsState {
        val isEmpty get() = bytes == 0L && downloadedAt == null
    }

    data class Downloading(val fraction: Float, val bytes: Long) : OfflineMapsState
}

class OfflineMapException(message: String) : Exception(message)

/**
 * Keeps map tiles for Malaysian waters and every saved spot in MapLibre's offline database. The maps
 * read that database by themselves when there is no signal, as long as they use [MAP_STYLE_URL].
 * Downloads run while the app is open; a stopped one resumes where it left off.
 */
class OfflineMaps(context: Context, private val spots: FishingDao, private val clock: () -> Long = System::currentTimeMillis) {
    private val manager = OfflineManager.getInstance(context)
    private val pixelRatio = context.resources.displayMetrics.density
    // MapLibre's offline callbacks arrive on the main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow<OfflineMapsState>(OfflineMapsState.Checking)
    private var job: Job? = null

    val state: StateFlow<OfflineMapsState> = _state.asStateFlow()

    fun refresh() {
        if (job?.isActive == true) return
        scope.launch { _state.value = saved() }
    }

    fun download() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                downloadAll()
                _state.value = saved()
            } catch (e: CancellationException) {
                _state.value = saved()
                throw e
            } catch (e: OfflineMapException) {
                _state.value = saved(failed = true)
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    fun delete() {
        job?.cancel()
        scope.launch {
            regions().forEach { it.deleteAwait() }
            _state.value = saved()
        }
    }

    private suspend fun downloadAll() {
        val plan = offlinePlan(spots.getAllSpotsFlow().first())
        // Regions for deleted or moved spots go; matching ones are kept and resumed.
        val (kept, stale) = regions().map { it to meta(it) }.partition { (_, meta) -> meta != null && plan.any { it.matches(meta) } }
        stale.forEach { (region, _) -> region.deleteAwait() }
        val targets = plan.map { planned ->
            kept.firstOrNull { (_, meta) -> planned.matches(meta!!) }?.first ?: create(planned)
        }

        var doneBytes = 0L
        targets.forEachIndexed { index, region ->
            val bytes = region.downloadAwait { status ->
                val fraction = if (status.requiredResourceCount > 0) {
                    status.completedResourceCount.toFloat() / status.requiredResourceCount
                } else 0f
                _state.value = OfflineMapsState.Downloading((index + fraction) / targets.size, doneBytes + status.completedResourceSize)
            }
            doneBytes += bytes
        }
    }

    private suspend fun saved(failed: Boolean = false): OfflineMapsState.Saved {
        val plan = offlinePlan(spots.getAllSpotsFlow().first())
        val regions = regions()
        val statuses = regions.map { it.statusAwait() }
        val metas = regions.mapNotNull(::meta)
        return OfflineMapsState.Saved(
            bytes = statuses.sumOf { it.completedResourceSize },
            complete = statuses.isNotEmpty() && statuses.all { it.isComplete },
            coversAllSpots = plan.all { planned -> metas.any(planned::matches) },
            downloadedAt = metas.minOfOrNull { it.createdAt },
            failed = failed
        )
    }

    private suspend fun create(planned: PlannedRegion): OfflineRegion = suspendCancellableCoroutine { cont ->
        val b = planned.bounds
        val definition = OfflineTilePyramidRegionDefinition(
            MAP_STYLE_URL,
            LatLngBounds.from(b.north, b.east, b.south, b.west),
            planned.minZoom,
            planned.maxZoom,
            pixelRatio
        )
        val meta = RegionMeta(planned.key, b.south, b.west, b.north, b.east, clock())
        manager.createOfflineRegion(definition, json.encodeToString(meta).toByteArray(), object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) = cont.resume(offlineRegion)
            override fun onError(error: String) = cont.resumeWithException(OfflineMapException(error))
        })
    }

    private suspend fun regions(): List<OfflineRegion> = suspendCancellableCoroutine { cont ->
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) = cont.resume(offlineRegions?.toList().orEmpty())
            override fun onError(error: String) = cont.resumeWithException(OfflineMapException(error))
        })
    }

    private fun meta(region: OfflineRegion): RegionMeta? =
        runCatching { json.decodeFromString<RegionMeta>(region.metadata.decodeToString()) }.getOrNull()
}

@Serializable
private data class RegionMeta(
    val key: String,
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val createdAt: Long
)

private fun PlannedRegion.matches(meta: RegionMeta): Boolean =
    key == meta.key && bounds == Bounds(meta.south, meta.west, meta.north, meta.east)

/** Downloads until complete and returns the bytes stored. MapLibre retries dropped connections itself. */
private suspend fun OfflineRegion.downloadAwait(onProgress: (OfflineRegionStatus) -> Unit): Long =
    suspendCancellableCoroutine { cont ->
        setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                onProgress(status)
                if (status.isComplete && cont.isActive) {
                    setDownloadState(OfflineRegion.STATE_INACTIVE)
                    cont.resume(status.completedResourceSize)
                }
            }

            override fun onError(error: OfflineRegionError) = Unit

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                setDownloadState(OfflineRegion.STATE_INACTIVE)
                if (cont.isActive) cont.resumeWithException(OfflineMapException("Tile limit $limit reached"))
            }
        })
        cont.invokeOnCancellation { setDownloadState(OfflineRegion.STATE_INACTIVE) }
        setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

private suspend fun OfflineRegion.statusAwait(): OfflineRegionStatus = suspendCancellableCoroutine { cont ->
    getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
        override fun onStatus(status: OfflineRegionStatus?) {
            if (status != null) cont.resume(status) else cont.resumeWithException(OfflineMapException("No status"))
        }
        override fun onError(error: String?) = cont.resumeWithException(OfflineMapException(error ?: "Status failed"))
    })
}

private suspend fun OfflineRegion.deleteAwait(): Unit = suspendCancellableCoroutine { cont ->
    delete(object : OfflineRegion.OfflineRegionDeleteCallback {
        override fun onDelete() = cont.resume(Unit)
        override fun onError(error: String) = cont.resumeWithException(OfflineMapException(error))
    })
}
