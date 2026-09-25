package com.fishingcopilot.ui.home

import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import com.fishingcopilot.data.remote.SatelliteClient
import com.fishingcopilot.data.satellite.SatelliteRepository
import com.fishingcopilot.data.satellite.SatelliteRepositoryTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SatelliteViewModelTest {
    @get:Rule val folder = TemporaryFolder()

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

    // Inside the test document's first cell (1.0-1.25 N, 103.0-103.25 E).
    private val spot = SpotEntity(id = 7, name = "Tanjung", latitude = 1.1, longitude = 103.1)
    private var online = true
    private val client = SatelliteClient {
        if (!online) throw IOException("offline")
        SatelliteRepositoryTest.document(sst = 29.5)
    }

    private fun TestScope.viewModel(today: LocalDate, spots: FakeSpots = FakeSpots(spot)): SatelliteViewModel {
        val repo = SatelliteRepository(File(folder.root, "fronts.json"), client, { 1_790_220_000_000L }, Dispatchers.Unconfined)
        val vm = SatelliteViewModel(spot.id, spots, repo) { today }
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shows the spot's readings once downloaded`() = runTest {
        val state = viewModel(today = LocalDate.of(2026, 9, 25)).uiState.value as SatelliteCardState.Ready
        assertEquals(29.5, state.summary.seaTempC!!, 0.0)
        assertEquals(LocalDate.of(2026, 9, 22), state.summary.dataDate)
        assertFalse(state.stale)
    }

    @Test
    fun `old data is flagged stale`() = runTest {
        val state = viewModel(today = LocalDate.of(2026, 9, 27)).uiState.value as SatelliteCardState.Ready
        assertTrue(state.stale)
    }

    @Test
    fun `offline with no cache offers a retry that recovers`() = runTest {
        online = false
        val vm = viewModel(today = LocalDate.of(2026, 9, 25))
        assertEquals(SatelliteCardState.Offline, vm.uiState.value)
        online = true
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is SatelliteCardState.Ready)
    }

    @Test
    fun `a spot far from any reading has no data`() = runTest {
        val spots = FakeSpots(spot.copy(latitude = 6.0, longitude = 116.0))
        assertEquals(SatelliteCardState.NoData, viewModel(LocalDate.of(2026, 9, 25), spots).uiState.value)
    }
}
