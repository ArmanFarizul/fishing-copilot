package com.fishingcopilot.ui.home

import com.fishingcopilot.astro.SolunarType
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SunMoonViewModelTest {
    private class FakeSpots(spot: SpotEntity) : FishingDao {
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

    private val myt = ZoneId.of("Asia/Kuala_Lumpur")
    private val penang = SpotEntity(id = 9, name = "Pulau Pinang", latitude = 5.42, longitude = 100.36)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun stateAt(time: ZonedDateTime): SunMoonUiState? {
        val now = time.toInstant().toEpochMilli()
        val vm = SunMoonViewModel(penang.id, FakeSpots(penang), ticks = flowOf(now), zone = myt, computeDispatcher = Dispatchers.Main)
        var state: SunMoonUiState? = null
        runTest {
            backgroundScope.launch { vm.uiState.collect { state = it } }
            advanceUntilIdle()
        }
        return state
    }

    @Test
    fun `marks the running period as now and the following one as next`() {
        // Penang 2026-10-10: moon transit ~12:39 (USNO), so 12:00 sits inside that major period.
        val state = stateAt(ZonedDateTime.of(2026, 10, 10, 12, 0, 0, 0, myt))!!
        val now = state.periods.single { it.status == PeriodStatus.NOW }
        assertEquals(SolunarType.MAJOR, now.period.type)
        val next = state.periods.single { it.status == PeriodStatus.NEXT }
        assertTrue(next.period.start.isAfter(now.period.start))
        assertTrue(state.periods.filter { it.period.end.isBefore(now.period.start) }.all { it.status == PeriodStatus.PAST })
    }

    @Test
    fun `only the first period that has not started is next`() {
        val time = ZonedDateTime.of(2026, 10, 10, 5, 0, 0, 0, myt)
        val state = stateAt(time)!!
        val upcoming = state.periods.filter { it.period.start.isAfter(time) }
        assertEquals(PeriodStatus.NEXT, upcoming.first().status)
        assertTrue(upcoming.drop(1).all { it.status == PeriodStatus.LATER })
        assertEquals(1, state.periods.count { it.status == PeriodStatus.NEXT })
    }
}
