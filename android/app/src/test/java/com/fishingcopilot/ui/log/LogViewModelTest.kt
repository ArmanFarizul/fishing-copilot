package com.fishingcopilot.ui.log

import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.catchlog.PhotoStore
import com.fishingcopilot.data.local.CatchDao
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotCatchCount
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {
    private class FakeCatchDao : CatchDao {
        val rows = MutableStateFlow<List<CatchLogEntity>>(emptyList())
        private var nextId = 1L
        override suspend fun insert(log: CatchLogEntity): Long {
            val id = nextId++
            rows.value = listOf(log.copy(id = id)) + rows.value
            return id
        }
        override suspend fun update(log: CatchLogEntity) { rows.value = rows.value.map { if (it.id == log.id) log else it } }
        override suspend fun delete(log: CatchLogEntity) { rows.value = rows.value.filterNot { it.id == log.id } }
        override fun allFlow(): Flow<List<CatchLogEntity>> = rows.map { list -> list.sortedByDescending { it.timestamp } }
        override fun betweenFlow(start: Long, end: Long): Flow<List<CatchLogEntity>> =
            rows.map { list -> list.filter { it.timestamp in start until end }.sortedByDescending { it.timestamp } }
        override fun yearsFlow(): Flow<List<Int>> = rows.map { list ->
            list.map { java.time.Instant.ofEpochMilli(it.timestamp).atZone(java.time.ZoneId.systemDefault()).year }.distinct().sortedDescending()
        }
        override fun recentBaitsFlow(limit: Int): Flow<List<String>> =
            rows.map { logs -> logs.mapNotNull { it.baitUsed }.distinct().take(limit) }
        override fun countsBySpotFlow(): Flow<List<SpotCatchCount>> = error("unused")
    }

    private class FakeSpots : FishingDao {
        override fun getAllSpotsFlow(): Flow<List<SpotEntity>> =
            MutableStateFlow(listOf(SpotEntity(id = 3, name = "Kukup", latitude = 1.3, longitude = 103.4)))
        override suspend fun insertSpot(spot: SpotEntity): Long = error("unused")
        override fun spotFlow(id: Long): Flow<SpotEntity?> = error("unused")
        override suspend fun updateSpot(spot: SpotEntity) = error("unused")
        override suspend fun deleteSpot(spot: SpotEntity) = error("unused")
        override suspend fun insertTideCache(cacheList: List<TideCacheEntity>) = error("unused")
        override suspend fun getTideForecast(stationCode: String, startTime: Long, endTime: Long): List<TideCacheEntity> =
            error("unused")
    }

    private class FakePhotos : PhotoStore {
        val stored = mutableListOf<String>()
        override suspend fun import(source: String): String = "file:///app/$source".also { stored += it }
        override suspend fun delete(uri: String) { stored -= uri }
    }

    private val conditions = CatchConditions(1_000L, 3, "Kukup", 0.5, true, null, null, 7.9)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `saving copies the photo, stores the catch and names its spot`() = runTest {
        val dao = FakeCatchDao()
        val photos = FakePhotos()
        val vm = LogViewModel(dao, FakeSpots(), photos)
        backgroundScope.launch { vm.uiState.collect {} }
        // The fixture catches are dated 1970, outside the default "this year".
        vm.show(com.fishingcopilot.catchlog.LogPeriod.All)
        var saved = false

        vm.save(conditions, "SIAKAP", "udang hidup", 2.1, null, "picked.jpg", null) { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        val state = vm.uiState.value
        assertEquals("Kukup", state.catches.single().spotName)
        assertEquals("file:///app/picked.jpg", state.catches.single().entity.photoUri)
        assertEquals(listOf("udang hidup"), state.recentBaits)
        assertEquals(1, state.summary!!.count)
    }

    @Test
    fun `deleting a catch removes its photo copy`() = runTest {
        val dao = FakeCatchDao()
        val photos = FakePhotos()
        val vm = LogViewModel(dao, FakeSpots(), photos)
        backgroundScope.launch { vm.uiState.collect {} }
        // The fixture catches are dated 1970, outside the default "this year".
        vm.show(com.fishingcopilot.catchlog.LogPeriod.All)
        vm.save(conditions, "PARI", null, null, null, "p.jpg", null)
        advanceUntilIdle()

        vm.delete(vm.uiState.value.catches.single().entity)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.catches.isEmpty())
        assertTrue(photos.stored.isEmpty())
    }

    @Test
    fun `editing changes the details but keeps the strike conditions`() = runTest {
        val dao = FakeCatchDao()
        val vm = LogViewModel(dao, FakeSpots(), FakePhotos())
        backgroundScope.launch { vm.uiState.collect {} }
        // The fixture catches are dated 1970, outside the default "this year".
        vm.show(com.fishingcopilot.catchlog.LogPeriod.All)
        vm.save(conditions, "SIAKAP", "udang", 2.1, null, null, null)
        advanceUntilIdle()
        val original = vm.uiState.value.catches.single().entity

        var saved = false
        vm.update(original, "Belukang", " sotong ", 3.4, 55.0, null, "Dekat tiang jeti") { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        val edited = vm.uiState.value.catches.single().entity
        assertEquals(original.id, edited.id)
        assertEquals("Belukang", edited.species)
        assertEquals("sotong", edited.baitUsed)
        assertEquals(3.4, edited.weightKg!!, 0.0)
        assertEquals("Dekat tiang jeti", edited.notes)
        assertEquals(original.timestamp, edited.timestamp)
        assertEquals(original.tideState, edited.tideState)
        assertEquals(original.biteScore, edited.biteScore)
    }

    @Test
    fun `editing keeps, replaces or removes the photo copy`() = runTest {
        val dao = FakeCatchDao()
        val photos = FakePhotos()
        val vm = LogViewModel(dao, FakeSpots(), photos)
        backgroundScope.launch { vm.uiState.collect {} }
        // The fixture catches are dated 1970, outside the default "this year".
        vm.show(com.fishingcopilot.catchlog.LogPeriod.All)
        vm.save(conditions, "PARI", null, null, null, "first.jpg", null)
        advanceUntilIdle()
        fun current() = vm.uiState.value.catches.single().entity

        // Unchanged photo: nothing copied or deleted.
        vm.update(current(), "PARI", "udang", null, null, current().photoUri, null)
        advanceUntilIdle()
        assertEquals(listOf("file:///app/first.jpg"), photos.stored)

        // New pick: copied in, old copy removed.
        vm.update(current(), "PARI", null, null, null, "second.jpg", null)
        advanceUntilIdle()
        assertEquals("file:///app/second.jpg", current().photoUri)
        assertEquals(listOf("file:///app/second.jpg"), photos.stored)

        // Removed: the copy goes too.
        vm.update(current(), "PARI", null, null, null, null, null)
        advanceUntilIdle()
        assertNull(current().photoUri)
        assertTrue(photos.stored.isEmpty())
    }

    @Test
    fun `the log opens on this year and can show other periods`() = runTest {
        val dao = FakeCatchDao()
        val zone = java.time.ZoneId.of("Asia/Kuala_Lumpur")
        fun at(date: String) = java.time.LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000
        val vm = LogViewModel(dao, FakeSpots(), FakePhotos(), { zone }, { java.time.LocalDate.of(2026, 9, 25) })
        backgroundScope.launch { vm.uiState.collect {} }
        listOf("2026-09-20", "2026-08-01", "2025-06-15", "2024-02-02").forEach { date ->
            vm.save(conditions.copy(timestamp = at(date)), "SIAKAP", null, null, null, null, null)
        }
        advanceUntilIdle()

        var state = vm.uiState.value
        assertEquals(com.fishingcopilot.catchlog.LogPeriod.ThisYear, state.period)
        assertEquals(2, state.catches.size)
        assertEquals(2, state.summary!!.count)
        assertEquals(2, state.months.size)
        assertEquals(listOf(2026, 2025, 2024), state.years)

        vm.show(com.fishingcopilot.catchlog.LogPeriod.LastYear)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.catches.size)

        vm.show(com.fishingcopilot.catchlog.LogPeriod.Year(2023))
        advanceUntilIdle()
        state = vm.uiState.value
        assertTrue(state.catches.isEmpty())
        assertNull(state.summary)
        assertTrue(state.hasAnyCatch)

        vm.show(com.fishingcopilot.catchlog.LogPeriod.All)
        advanceUntilIdle()
        assertEquals(4, vm.uiState.value.catches.size)

        vm.show(com.fishingcopilot.catchlog.LogPeriod.Year(2025))
        advanceUntilIdle()
        assertEquals(com.fishingcopilot.catchlog.LogPeriod.LastYear, vm.uiState.value.period)
    }

    @Test
    fun `a species filter narrows the list, summary and baits, and resets when absent`() = runTest {
        val dao = FakeCatchDao()
        val vm = LogViewModel(dao, FakeSpots(), FakePhotos())
        backgroundScope.launch { vm.uiState.collect {} }
        vm.show(com.fishingcopilot.catchlog.LogPeriod.All)
        vm.save(conditions, "SIAKAP", "udang", 2.0, null, null, null)
        vm.save(conditions, "SIAKAP", "sotong", 1.0, null, null, null)
        vm.save(conditions, "PARI", "sotong", 5.0, null, null, null)
        advanceUntilIdle()

        assertEquals(listOf("SIAKAP" to 2, "PARI" to 1), vm.uiState.value.speciesOptions)
        vm.showSpecies("PARI")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("PARI", state.species)
        assertEquals(1, state.catches.size)
        assertEquals(1, state.summary!!.count)
        assertEquals(listOf("sotong"), state.baits.map { it.bait })

        vm.showSpecies("KERAPU")
        advanceUntilIdle()
        assertNull(vm.uiState.value.species)
        assertEquals(3, vm.uiState.value.catches.size)
    }
}
