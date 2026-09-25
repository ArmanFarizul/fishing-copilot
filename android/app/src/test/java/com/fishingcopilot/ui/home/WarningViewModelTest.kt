package com.fishingcopilot.ui.home

import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import com.fishingcopilot.data.remote.MetWarningClient
import com.fishingcopilot.data.warnings.WarningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class WarningViewModelTest {
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

    private val feed = javaClass.getResource("/metmalaysia_warnings_2026-09-25.json")!!.readText()
    private val now = LocalDateTime.parse("2026-09-25T07:00").atZone(ZoneId.of("Asia/Kuala_Lumpur")).toInstant().toEpochMilli()
    private var online = true
    private var requests = 0
    private val client = MetWarningClient {
        requests++
        if (!online) throw IOException("offline")
        feed
    }
    private val spot = SpotEntity(id = 2, name = "Klang", latitude = 2.98, longitude = 101.30)

    private fun TestScope.viewModel(ticks: Flow<Long> = flowOf(now)): WarningViewModel {
        val repo = WarningRepository(File(folder.root, "w.json"), client, { now }, Dispatchers.Unconfined)
        val vm = WarningViewModel(spot.id, FakeSpots(spot), repo, ticks = ticks) { now }
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shows the warnings for the spot's state`() = runTest {
        val state = viewModel().uiState.value as WarningState.Ready
        assertEquals(3, state.summary.forSpot.size)
        assertFalse(state.stale)
    }

    @Test
    fun `offline with nothing saved says so and can retry`() = runTest {
        online = false
        val vm = viewModel()
        assertEquals(WarningState.Unknown(failed = true), vm.uiState.value)
        online = true
        vm.retry()
        runCurrent()
        assertTrue(vm.uiState.value is WarningState.Ready)
    }

    @Test
    fun `re-checks on every tick but only downloads when the copy is old`() = runTest {
        // The clock is fixed, so after the first download the saved copy stays fresh.
        viewModel(ticks = flowOf(now, now, now))
        assertEquals(1, requests)
    }
}
