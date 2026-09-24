package com.fishingcopilot.ui.home

import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.MarineDao
import com.fishingcopilot.data.local.MarineForecastEntity
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import com.fishingcopilot.data.marine.MarineRepository
import com.fishingcopilot.data.remote.OpenMeteoWeatherClient
import com.fishingcopilot.marine.WaveStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MarineViewModelTest {
    private class FakeSpots(spot: SpotEntity) : FishingDao {
        val spots = MutableStateFlow(mapOf(spot.id to spot))
        override fun spotFlow(id: Long): Flow<SpotEntity?> = spots.map { it[id] }
        override suspend fun updateSpot(spot: SpotEntity) { spots.value += spot.id to spot }
        override suspend fun insertSpot(spot: SpotEntity): Long = error("unused")
        override fun getAllSpotsFlow(): Flow<List<SpotEntity>> = error("unused")
        override suspend fun deleteSpot(spot: SpotEntity) = error("unused")
        override suspend fun insertTideCache(cacheList: List<TideCacheEntity>) = error("unused")
        override suspend fun getTideForecast(stationCode: String, startTime: Long, endTime: Long): List<TideCacheEntity> =
            error("unused")
    }

    private class FakeMarineDao : MarineDao {
        val rows = MutableStateFlow<Map<Long, MarineForecastEntity>>(emptyMap())
        override fun forecastFlow(spotId: Long): Flow<MarineForecastEntity?> = rows.map { it[spotId] }
        override suspend fun fetchedAt(spotId: Long): Long? = rows.value[spotId]?.fetchedAtMillis
        override suspend fun upsert(forecast: MarineForecastEntity) { rows.value += forecast.spotId to forecast }
    }

    private val hour0 = 1_790_208_000L // seconds
    private val now = (hour0 + 2 * 3600) * 1000 + 600_000
    private var online = true
    private val requestedLatitudes = mutableListOf<String>()
    private val client = OpenMeteoWeatherClient { url ->
        if (!online) throw IOException("offline")
        requestedLatitudes += url.substringAfter("latitude=").substringBefore("&")
        val times = (0..30).map { hour0 + it * 3600 }
        if ("marine-api" in url) {
            """{"hourly":{"time":$times,"wave_height":${times.map { 1.1 }},"wave_period":${times.map { 6 }},
               "wave_direction":${times.map { 40 }},"ocean_current_velocity":${times.map { 0.7 }},
               "ocean_current_direction":${times.map { 200 }},"sea_surface_temperature":${times.map { 29.8 }}}}"""
        } else {
            """{"hourly":{"time":$times,"wind_speed_10m":${times.map { 11 }},"wind_gusts_10m":${times.map { 17 }},
               "wind_direction_10m":${times.map { 45 }},"pressure_msl":${times.map { 1009 }}}}"""
        }
    }
    private val spot = SpotEntity(id = 5, name = "Kukup", latitude = 1.325, longitude = 103.44)

    private fun TestScope.viewModel(clock: Long = now, dao: FakeMarineDao = FakeMarineDao()): Triple<MarineViewModel, FakeSpots, FakeMarineDao> {
        val spots = FakeSpots(spot)
        val vm = MarineViewModel(spot.id, spots, MarineRepository(dao, client) { clock }, ticks = flowOf(clock)) { clock }
        backgroundScope.launch { vm.uiState.collect {} }
        return Triple(vm, spots, dao)
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `downloads and summarises the current hour`() = runTest {
        val (vm, _, _) = viewModel()
        advanceUntilIdle()
        val ready = vm.uiState.value as MarineCardState.Ready
        assertEquals(WaveStatus.CAUTION, ready.summary.waveStatus)
        assertFalse(ready.stale)
    }

    @Test
    fun `offline with no cache shows an error that retry clears`() = runTest {
        online = false
        val (vm, _, _) = viewModel()
        advanceUntilIdle()
        assertEquals(MarineCardState.Offline, vm.uiState.value)

        online = true
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is MarineCardState.Ready)
    }

    @Test
    fun `an old cache is still shown offline but marked stale`() = runTest {
        val (first, _, dao) = viewModel()
        advanceUntilIdle()
        assertTrue(first.uiState.value is MarineCardState.Ready)

        online = false
        val (later, _, _) = viewModel(clock = now + 7 * 3_600_000, dao = dao)
        advanceUntilIdle()
        val ready = later.uiState.value as MarineCardState.Ready
        assertTrue(ready.stale)
    }

    @Test
    fun `moving the spot refetches for the new position`() = runTest {
        val (_, spots, _) = viewModel()
        advanceUntilIdle()
        spots.updateSpot(spot.copy(latitude = 2.98, longitude = 101.30))
        advanceUntilIdle()
        assertEquals(listOf("1.325", "1.325", "2.98", "2.98"), requestedLatitudes)
    }
}
