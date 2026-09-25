package com.fishingcopilot.ui.home

import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import com.fishingcopilot.data.prayer.PrayerRepository
import com.fishingcopilot.data.remote.ForecastClient
import com.fishingcopilot.data.remote.JakimTakwimClient
import com.fishingcopilot.data.remote.PrayerZoneClient
import com.fishingcopilot.data.weather.WeatherRepository
import com.fishingcopilot.maps.LatLon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HereViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private class FakeSpots(list: List<SpotEntity>) : FishingDao {
        val spots = MutableStateFlow(list)
        override fun getAllSpotsFlow(): Flow<List<SpotEntity>> = spots
        override fun spotFlow(id: Long): Flow<SpotEntity?> = error("unused")
        override suspend fun updateSpot(spot: SpotEntity) = error("unused")
        override suspend fun insertSpot(spot: SpotEntity): Long = error("unused")
        override suspend fun deleteSpot(spot: SpotEntity) = error("unused")
        override suspend fun insertTideCache(cacheList: List<TideCacheEntity>) = error("unused")
        override suspend fun getTideForecast(stationCode: String, startTime: Long, endTime: Long): List<TideCacheEntity> =
            error("unused")
    }

    private val forecastBody = javaClass.getResource("/openmeteo_forecast_2026-09-25.json")!!.readText()
    private val prayerBody = javaClass.getResource("/esolat_month_TRG01_2026-09.json")!!.readText()
    private val now = 1_790_308_800_000L
    private val kt = SpotEntity(id = 1, name = "Kuala Terengganu", latitude = 5.35, longitude = 103.17)
    private val redang = SpotEntity(id = 2, name = "Redang", latitude = 5.78, longitude = 103.02)

    private fun TestScope.viewModel(permission: Boolean, at: LatLon?, manual: MutableStateFlow<String?> = MutableStateFlow(null)): HereViewModel {
        val weather = WeatherRepository(File(folder.root, "w.json"), ForecastClient { forecastBody }, { now }, Dispatchers.Unconfined)
        val prayers = PrayerRepository(
            File(folder.root, "p.json"),
            PrayerZoneClient { """{"zone":"TRG01","district":"Kuala Terengganu"}""" },
            JakimTakwimClient { prayerBody },
            { LocalDate.of(2026, 9, 25) },
            Dispatchers.Unconfined
        )
        val vm = HereViewModel(
            FakeSpots(listOf(kt, redang)), weather, prayers,
            hasPermission = { permission }, locate = { at }, placeName = { "Kuala Terengganu" },
            manualZone = manual, saveZone = { manual.value = it },
            clock = { now }, ticks = flowOf(now)
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `without permission it asks instead of locating`() = runTest {
        assertEquals(HereLocation.NoPermission, viewModel(permission = false, at = null).uiState.value.location)
    }

    @Test
    fun `at a saved spot it finds weather, prayer zone and the spot`() = runTest {
        val state = viewModel(permission = true, at = LatLon(5.351, 103.171)).uiState.value
        assertTrue(state.location is HereLocation.Found)
        assertEquals("Kuala Terengganu", state.placeName)
        assertEquals(31.3, state.forecast!!.forecast.now.tempC, 0.0)
        assertEquals("TRG01", state.prayers!!.zone)
        assertEquals("Kuala Terengganu", state.prayers!!.district)
        assertEquals(kt.id, state.nearbySpot!!.id)
    }

    @Test
    fun `away from every spot none is picked`() = runTest {
        val state = viewModel(permission = true, at = LatLon(5.0, 103.3)).uiState.value
        assertNull(state.nearbySpot)
    }

    @Test
    fun `no fix says so`() = runTest {
        assertEquals(HereLocation.Unavailable, viewModel(permission = true, at = null).uiState.value.location)
    }

    @Test
    fun `a zone picked by hand wins, works without location, and can go back to auto`() = runTest {
        val manual = MutableStateFlow<String?>(null)
        val vm = viewModel(permission = false, at = null, manual = manual)
        assertNull(vm.uiState.value.prayers)

        vm.pickZone("JHR02")
        advanceUntilIdle()
        val picked = vm.uiState.value.prayers!!
        assertEquals("JHR02", picked.zone)
        assertTrue(picked.manual)
        assertEquals("Johor Bahru, Kota Tinggi, Mersing, Kulai", picked.district)
    }

    @Test
    fun `going back to auto uses the location's zone again`() = runTest {
        val manual = MutableStateFlow<String?>("JHR02")
        val vm = viewModel(permission = true, at = LatLon(5.35, 103.17), manual = manual)
        assertEquals("JHR02", vm.uiState.value.prayers!!.zone)

        vm.pickZone(null)
        advanceUntilIdle()
        val auto = vm.uiState.value.prayers!!
        assertEquals("TRG01", auto.zone)
        assertEquals(false, auto.manual)
    }
}
