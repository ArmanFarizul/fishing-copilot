package com.fishingcopilot.data.prayer

import com.fishingcopilot.data.remote.JakimTakwimClient
import com.fishingcopilot.data.remote.PrayerZone
import com.fishingcopilot.data.remote.PrayerZoneClient
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.data.spots.haversineKm
import com.fishingcopilot.prayer.MALAYSIA
import com.fishingcopilot.prayer.Prayer
import com.fishingcopilot.prayer.PrayerDay
import com.fishingcopilot.prayer.jakimZone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// The farthest Malaysian waters (Layang-Layang, Sulu Sea edge) are within about 350 km of a listed coastal town.
private const val MALAYSIA_REACH_KM = 400.0

/**
 * [district] is null when the zone is the nearest coastal town's, used because the angler is at sea.
 * [manual] is true when the angler picked the zone by hand.
 */
data class PrayerMonth(val zone: String, val district: String?, val month: YearMonth, val days: List<PrayerDay>, val manual: Boolean = false)

/**
 * JAKIM prayer times for the zone the angler is in, one month per download, kept for offline use.
 * At sea no zone covers the point, so the nearest coastal town's zone is used instead.
 */
class PrayerRepository(
    private val cacheFile: File,
    private val zones: PrayerZoneClient,
    private val jakim: JakimTakwimClient,
    private val today: () -> LocalDate = { LocalDate.now(MALAYSIA) },
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val cached = MutableStateFlow<PrayerMonth?>(null)
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var lastLookup: Triple<Double, Double, PrayerZone>? = null

    val data: Flow<PrayerMonth?> = cached.onStart { if (cached.value == null) loadCache() }

    /** A [manualZone] wins over the location, which may then be null. */
    suspend fun refresh(latitude: Double?, longitude: Double?, manualZone: String? = null) = lock.withLock {
        if (cached.value == null) loadCache()
        val zone = when {
            manualZone != null -> PrayerZone(manualZone, jakimZone(manualZone)?.districts)
            latitude == null || longitude == null -> return@withLock
            // JAKIM zones only cover Malaysia; far from its coast any fallback zone would give wrong times.
            CoastalArea.nearest(latitude, longitude).second > MALAYSIA_REACH_KM -> {
                cached.value = null
                return@withLock
            }
            else -> zoneFor(latitude, longitude)
        }
        val manual = manualZone != null
        val month = YearMonth.from(today())
        val copy = cached.value
        if (copy != null && copy.zone == zone.code && copy.district == zone.district && copy.month == month && copy.manual == manual) {
            return@withLock
        }
        val days = if (copy != null && copy.zone == zone.code && copy.month == month) copy.days else jakim.prayerMonth(zone.code)
        val fresh = PrayerMonth(zone.code, zone.district, month, days, manual)
        withContext(io) {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json.encodeToString(StoredMonth.serializer(), StoredMonth.of(fresh)))
        }
        cached.value = fresh
    }

    /** Looks the zone up again only after moving about 10 km, since zones are whole districts. */
    private suspend fun zoneFor(latitude: Double, longitude: Double): PrayerZone {
        lastLookup?.let { (lat, lon, zone) ->
            if (haversineKm(lat, lon, latitude, longitude) < 10.0) return zone
        }
        val zone = zones.zoneAt(latitude, longitude) ?: PrayerZone(CoastalArea.nearest(latitude, longitude).first.prayerZone, null)
        lastLookup = Triple(latitude, longitude, zone)
        return zone
    }

    private suspend fun loadCache() {
        cached.value = withContext(io) {
            if (!cacheFile.exists()) return@withContext null
            try {
                json.decodeFromString(StoredMonth.serializer(), cacheFile.readText()).toMonth()
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Serializable
private data class StoredMonth(
    val zone: String,
    val district: String? = null,
    val month: String,
    val days: List<StoredDay>,
    val manual: Boolean = false
) {
    fun toMonth() = PrayerMonth(zone, district, YearMonth.parse(month), manual = manual, days = days.map { d ->
        PrayerDay(LocalDate.parse(d.date), d.times.mapKeys { Prayer.valueOf(it.key) }.mapValues { LocalTime.parse(it.value) })
    })

    companion object {
        fun of(m: PrayerMonth) = StoredMonth(m.zone, m.district, m.month.toString(), manual = m.manual, days = m.days.map { d ->
            StoredDay(d.date.toString(), d.times.mapKeys { it.key.name }.mapValues { it.value.toString() })
        })
    }
}

@Serializable
private data class StoredDay(val date: String, val times: Map<String, String>)
