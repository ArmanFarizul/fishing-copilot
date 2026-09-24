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
        override suspend fun delete(log: CatchLogEntity) { rows.value = rows.value.filterNot { it.id == log.id } }
        override fun allFlow(): Flow<List<CatchLogEntity>> = rows
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
        vm.save(conditions, "PARI", null, null, null, "p.jpg", null)
        advanceUntilIdle()

        vm.delete(vm.uiState.value.catches.single().entity)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.catches.isEmpty())
        assertTrue(photos.stored.isEmpty())
    }
}
