package com.fishingcopilot.ui.home

import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import com.fishingcopilot.data.local.TideConstantEntity
import com.fishingcopilot.data.local.TideDao
import com.fishingcopilot.data.local.TideModelEntity
import com.fishingcopilot.data.local.TideModelWithConstants
import com.fishingcopilot.data.remote.OpenMeteoMarineClient
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.data.tide.TideRepository
import com.fishingcopilot.tide.Constituent
import com.fishingcopilot.tide.HarmonicConstant
import com.fishingcopilot.tide.TideModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class FakeFishingDao(spot: SpotEntity) : FishingDao {
        val spots = MutableStateFlow(mapOf(spot.id to spot))
        override fun spotFlow(id: Long): Flow<SpotEntity?> = spots.map { it[id] }
        override suspend fun updateSpot(spot: SpotEntity) { spots.value += spot.id to spot }
        override suspend fun insertSpot(spot: SpotEntity): Long = error("unused")
        override fun getAllSpotsFlow(): Flow<List<SpotEntity>> = error("unused")
        override suspend fun insertCatchLog(log: CatchLogEntity): Long = error("unused")
        override fun getAllCatchLogsFlow(): Flow<List<CatchLogEntity>> = error("unused")
        override suspend fun getLogsBySpotAndSpecies(spotId: Long, species: String): List<CatchLogEntity> = error("unused")
        override suspend fun insertTideCache(cacheList: List<TideCacheEntity>) = error("unused")
        override suspend fun getTideForecast(stationCode: String, startTime: Long, endTime: Long): List<TideCacheEntity> =
            error("unused")
    }

    private class FakeTideDao : TideDao {
        val models = MutableStateFlow<Map<Long, TideModelWithConstants>>(emptyMap())
        override fun modelWithConstants(spotId: Long): Flow<TideModelWithConstants?> = models.map { it[spotId] }
        override suspend fun findModel(spotId: Long): TideModelEntity? = models.value[spotId]?.model
        override suspend fun deleteModel(spotId: Long) { models.value -= spotId }
        override suspend fun insertModel(model: TideModelEntity) {
            models.value += model.spotId to TideModelWithConstants(model, emptyList())
        }
        override suspend fun insertConstants(constants: List<TideConstantEntity>) {
            constants.groupBy { it.spotId }.forEach { (id, rows) ->
                models.value += id to models.value.getValue(id).let { it.copy(constants = it.constants + rows) }
            }
        }
    }

    private val now = Instant.parse("2026-09-25T03:00:00Z").toEpochMilli()
    private val truth = TideModel(
        epochMillis = Instant.parse("2025-09-24T00:00:00Z").toEpochMilli(),
        meanLevel = 0.7,
        constants = listOf(HarmonicConstant(Constituent.M2, 0.87, 20.0), HarmonicConstant(Constituent.K1, 0.29, 115.0))
    )
    private val seaJson: String = run {
        val hours = (0 until 365 * 24).map { truth.epochMillis / 1000 + it * 3600L }
        """{"hourly":{"time":$hours,"sea_level_height_msl":${hours.map { truth.heightAt(it * 1000) }}}}"""
    }
    private val landJson = """{"hourly":{"time":[1789862400],"sea_level_height_msl":[null]}}"""

    private val inland = SpotEntity(id = 1, name = "Lubuk saya", latitude = 3.139, longitude = 101.687)
    private val coastal = SpotEntity(id = 1, name = "Kukup", latitude = 1.325, longitude = 103.44)

    /** Serves sea level everywhere except the inland test point; flip [online] to simulate no internet. */
    private var online = true
    private val client = OpenMeteoMarineClient { url ->
        if (!online) throw IOException("offline")
        if ("latitude=3.139" in url) landJson else seaJson
    }

    private fun TestScope.viewModel(spot: SpotEntity): Pair<HomeViewModel, FakeFishingDao> {
        val dao = FakeFishingDao(spot)
        val vm = HomeViewModel(spot.id, dao, TideRepository(FakeTideDao(), client) { now }, ticks = flowOf(now)) { now }
        backgroundScope.launch { vm.uiState.collect {} }
        return vm to dao
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `downloads a model for a coastal spot and shows the summary`() = runTest {
        val (vm, _) = viewModel(coastal)
        advanceUntilIdle()
        val tide = vm.uiState.value.tide
        assertTrue("expected Ready, got $tide", tide is TideCardState.Ready)
        assertEquals("Kukup", vm.uiState.value.spotName)
    }

    @Test
    fun `an inland point offers the nearest coastal area, which then loads`() = runTest {
        val (vm, dao) = viewModel(inland)
        advanceUntilIdle()
        val tide = vm.uiState.value.tide as TideCardState.NoMarineData
        assertEquals(CoastalArea.PELABUHAN_KLANG, tide.nearest)

        vm.useNearestArea()
        advanceUntilIdle()
        val moved = dao.spots.value.getValue(1)
        assertEquals(CoastalArea.PELABUHAN_KLANG.latitude, moved.latitude, 0.0)
        assertEquals("Lubuk saya", moved.name)
        assertTrue(vm.uiState.value.tide is TideCardState.Ready)
    }

    @Test
    fun `offline first download shows an error that retry clears`() = runTest {
        online = false
        val (vm, _) = viewModel(coastal)
        advanceUntilIdle()
        assertEquals(TideCardState.Offline, vm.uiState.value.tide)

        online = true
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.tide is TideCardState.Ready)
    }

    @Test
    fun `offset follows the slider and is saved when released`() = runTest {
        val (vm, dao) = viewModel(coastal)
        advanceUntilIdle()

        vm.onOffsetChange(35)
        assertEquals(35, vm.uiState.value.offsetMinutes)
        assertEquals(0, dao.spots.value.getValue(1).tideOffsetMinutes)

        vm.onOffsetCommit()
        advanceUntilIdle()
        assertEquals(35, dao.spots.value.getValue(1).tideOffsetMinutes)
        assertEquals(35, vm.uiState.value.offsetMinutes)
    }
}
