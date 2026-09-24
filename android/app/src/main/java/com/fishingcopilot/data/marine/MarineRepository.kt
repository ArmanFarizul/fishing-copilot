package com.fishingcopilot.data.marine

import com.fishingcopilot.data.local.MarineDao
import com.fishingcopilot.data.local.MarineForecastEntity
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.remote.OpenMeteoWeatherClient
import com.fishingcopilot.marine.MarineHour
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class CachedMarineForecast(val fetchedAtMillis: Long, val hours: List<MarineHour>)

class MarineRepository(
    private val dao: MarineDao,
    private val client: OpenMeteoWeatherClient,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun forecast(spotId: Long): Flow<CachedMarineForecast?> = dao.forecastFlow(spotId).map { entity ->
        entity?.let { CachedMarineForecast(it.fetchedAtMillis, Json.decodeFromString(it.hoursJson)) }
    }

    suspend fun refresh(spot: SpotEntity) {
        val hours = client.marineHours(spot.latitude, spot.longitude)
        dao.upsert(MarineForecastEntity(spot.id, clock(), Json.encodeToString(hours)))
    }

    /** Downloads when there is no cache or it is older than [REFRESH_AFTER_MILLIS]. Returns whether it downloaded. */
    suspend fun refreshIfStale(spot: SpotEntity): Boolean {
        val fetchedAt = dao.fetchedAt(spot.id)
        if (fetchedAt != null && clock() - fetchedAt < REFRESH_AFTER_MILLIS) return false
        refresh(spot)
        return true
    }

    companion object {
        // Open-Meteo's models update several times a day; hourly is plenty for a fishing card.
        const val REFRESH_AFTER_MILLIS = 60L * 60 * 1000
    }
}
