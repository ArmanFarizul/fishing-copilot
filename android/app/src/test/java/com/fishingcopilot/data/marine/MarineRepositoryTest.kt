package com.fishingcopilot.data.marine

import com.fishingcopilot.data.local.MarineDao
import com.fishingcopilot.data.local.MarineForecastEntity
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.remote.OpenMeteoWeatherClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarineRepositoryTest {
    private class FakeMarineDao : MarineDao {
        val rows = MutableStateFlow<Map<Long, MarineForecastEntity>>(emptyMap())
        override fun forecastFlow(spotId: Long): Flow<MarineForecastEntity?> = rows.map { it[spotId] }
        override suspend fun fetchedAt(spotId: Long): Long? = rows.value[spotId]?.fetchedAtMillis
        override suspend fun upsert(forecast: MarineForecastEntity) { rows.value += forecast.spotId to forecast }
    }

    private val spot = SpotEntity(id = 3, name = "Kukup", latitude = 1.325, longitude = 103.44)
    private val now = 1_790_220_000_000L
    private var requests = 0
    private val client = OpenMeteoWeatherClient { url ->
        requests++
        if ("marine-api" in url) {
            """{"hourly":{"time":[1790208000],"wave_height":[0.9],"wave_period":[6],"wave_direction":[30],
               "ocean_current_velocity":[0.7],"ocean_current_direction":[200],"sea_surface_temperature":[29.8]}}"""
        } else {
            """{"hourly":{"time":[1790208000],"wind_speed_10m":[11],"wind_gusts_10m":[17],"wind_direction_10m":[45],"pressure_msl":[1009]}}"""
        }
    }

    @Test
    fun `downloads once, serves the cache, and refetches after an hour`() = runTest {
        val dao = FakeMarineDao()
        assertTrue(MarineRepository(dao, client) { now }.refreshIfStale(spot))
        assertEquals(2, requests)

        assertFalse(MarineRepository(dao, client) { now + 59 * 60_000 }.refreshIfStale(spot))
        assertEquals(2, requests)

        assertTrue(MarineRepository(dao, client) { now + 61 * 60_000 }.refreshIfStale(spot))
        assertEquals(4, requests)

        val cached = MarineRepository(dao, client) { now }.forecast(spot.id).first()!!
        assertEquals(now + 61 * 60_000, cached.fetchedAtMillis)
        assertEquals(0.9, cached.hours.single().waveHeight!!, 0.0)
        assertEquals(11.0, cached.hours.single().windKn!!, 0.0)
    }
}
