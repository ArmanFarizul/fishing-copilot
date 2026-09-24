package com.fishingcopilot.ui.home

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.SolunarType
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.data.hijri.HijriRepository
import com.fishingcopilot.data.local.HijriDao
import com.fishingcopilot.data.local.HijriDayEntity
import com.fishingcopilot.data.remote.JakimTakwimClient
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

    private class FakeHijriDao : HijriDao {
        val rows = MutableStateFlow<Map<String, HijriDayEntity>>(emptyMap())
        override fun range(from: String, to: String): Flow<List<HijriDayEntity>> =
            rows.map { all -> all.values.filter { it.date in from..to }.sortedBy { it.date } }
        override suspend fun countInYear(yearPrefix: String): Int = rows.value.keys.count { it.startsWith(yearPrefix) }
        override suspend fun upsertAll(days: List<HijriDayEntity>) { rows.value += days.associateBy { it.date } }
    }

    // JAKIM 2026-10-10 = 28 Rabiulakhir 1448 and 2026-10-12 = 1 Jamadilawal 1448 (e-solat takwim).
    private val takwim = JakimTakwimClient {
        """{"status":"OK!","prayerTime":[{"hijri":"1448-04-28","date":"10-Okt-2026"},{"hijri":"1448-04-29","date":"11-Okt-2026"},{"hijri":"1448-05-01","date":"12-Okt-2026"}]}"""
    }

    private val myt = ZoneId.of("Asia/Kuala_Lumpur")
    private val penang = SpotEntity(id = 9, name = "Pulau Pinang", latitude = 5.42, longitude = 100.36)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun stateAt(time: ZonedDateTime): SunMoonUiState? {
        val now = time.toInstant().toEpochMilli()
        val vm = SunMoonViewModel(penang.id, FakeSpots(penang), HijriRepository(FakeHijriDao(), takwim), ticks = flowOf(now), zone = myt, computeDispatcher = Dispatchers.Main) { now }
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

    @Test
    fun `calendar covers 30 days with official Hijri dates where JAKIM has them`() {
        val state = stateAt(ZonedDateTime.of(2026, 10, 10, 9, 0, 0, 0, myt))!!
        assertEquals(30, state.calendar.size)
        assertEquals(HijriDate(1448, 4, 28), state.calendar[0].hijri.hijri)
        assertTrue(state.calendar[0].hijri.official)
        assertEquals(HijriDate(1448, 5, 1), state.calendar[2].hijri.hijri)
        assertTrue("later days are estimates", state.calendar.drop(3).none { it.hijri.official })
        assertEquals(HijriDate(1448, 5, 2), state.calendar[3].hijri.hijri)
        // Hijri day 28 is outside the spec's spring and neap bands; day 1 is spring.
        assertEquals(TideStrength.NORMAL, state.tideStrength)
        assertEquals(TideStrength.SPRING, state.calendar[2].tideStrength)
    }
}
