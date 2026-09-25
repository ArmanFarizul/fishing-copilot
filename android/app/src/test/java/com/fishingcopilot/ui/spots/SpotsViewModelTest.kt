package com.fishingcopilot.ui.spots

import com.fishingcopilot.data.local.CatchDao
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotCatchCount
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideCacheEntity
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.ProfileRepository
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.ui.onboarding.SpotSelection
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotsViewModelTest {
    private class FakeSpots : FishingDao {
        val rows = MutableStateFlow(
            listOf(
                SpotEntity(id = 1, name = "Kukup", latitude = 1.325, longitude = 103.44, createdAt = 1),
                SpotEntity(id = 2, name = "Jeti Muar", latitude = 2.03, longitude = 102.52, createdAt = 2)
            )
        )
        override fun getAllSpotsFlow(): Flow<List<SpotEntity>> = rows
        override suspend fun insertSpot(spot: SpotEntity): Long {
            val id = rows.value.maxOf { it.id } + 1
            rows.value = rows.value + spot.copy(id = id)
            return id
        }
        override suspend fun updateSpot(spot: SpotEntity) { rows.value = rows.value.map { if (it.id == spot.id) spot else it } }
        override suspend fun deleteSpot(spot: SpotEntity) { rows.value = rows.value.filterNot { it.id == spot.id } }
        override fun spotFlow(id: Long): Flow<SpotEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun insertTideCache(cacheList: List<TideCacheEntity>) = error("unused")
        override suspend fun getTideForecast(stationCode: String, startTime: Long, endTime: Long): List<TideCacheEntity> =
            error("unused")
    }

    private class FakeCatches : CatchDao {
        override suspend fun insert(log: CatchLogEntity): Long = error("unused")
        override suspend fun update(log: CatchLogEntity) = error("unused")
        override fun betweenFlow(start: Long, end: Long): Flow<List<CatchLogEntity>> = error("unused")
        override fun yearsFlow(): Flow<List<Int>> = error("unused")
        override suspend fun delete(log: CatchLogEntity) = error("unused")
        override fun allFlow(): Flow<List<CatchLogEntity>> = error("unused")
        override fun recentBaitsFlow(limit: Int): Flow<List<String>> = error("unused")
        override fun countsBySpotFlow(): Flow<List<SpotCatchCount>> = MutableStateFlow(listOf(SpotCatchCount(2, 4)))
    }

    private class FakeProfiles : ProfileRepository {
        override val profile = MutableStateFlow<UserProfile?>(
            UserProfile("Arman", Avatar.JETTY, FishingStyle.SHORE, emptySet(), homeSpotId = 1)
        )
        override suspend fun save(profile: UserProfile) { this.profile.value = profile }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `lists the home spot first with catch counts`() = runTest {
        val vm = SpotsViewModel(FakeSpots(), FakeCatches(), FakeProfiles())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val items = vm.uiState.value
        assertEquals(listOf(1L, 2L), items.map { it.spot.id })
        assertTrue(items[0].isHome)
        assertEquals(4, items[1].catchCount)
    }

    @Test
    fun `the home spot cannot be deleted but another can`() = runTest {
        val spots = FakeSpots()
        val vm = SpotsViewModel(spots, FakeCatches(), FakeProfiles())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(vm.delete(vm.uiState.value[0]))
        assertTrue(vm.delete(vm.uiState.value[1]))
        advanceUntilIdle()
        assertEquals(listOf(1L), spots.rows.value.map { it.id })
    }

    @Test
    fun `set home, rename and add`() = runTest {
        val spots = FakeSpots()
        val profiles = FakeProfiles()
        val vm = SpotsViewModel(spots, FakeCatches(), profiles)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.setHome(vm.uiState.value[1].spot)
        vm.rename(spots.rows.value[0], "  Jeti Kukup ")
        vm.rename(spots.rows.value[0], "   ")
        vm.add(SpotSelection.Area(CoastalArea.MERSING), " Mersing ")
        advanceUntilIdle()

        assertEquals(2L, profiles.profile.value!!.homeSpotId)
        assertEquals("Jeti Kukup", spots.rows.value[0].name)
        assertEquals("Mersing", spots.rows.value.last().name)
        assertEquals(CoastalArea.MERSING.latitude, spots.rows.value.last().latitude, 0.0)
        assertEquals(2L, vm.uiState.value[0].spot.id) // new home now listed first
    }

    @Test
    fun `a new spot can become the main spot as it is added`() = runTest {
        val spots = FakeSpots()
        val profiles = FakeProfiles()
        val vm = SpotsViewModel(spots, FakeCatches(), profiles)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.add(SpotSelection.Point(1.3538, 104.2273), "Sungai Rengit", makeMain = true)
        advanceUntilIdle()

        assertEquals(3L, profiles.profile.value!!.homeSpotId)
        assertEquals("Sungai Rengit", vm.uiState.value[0].spot.name)
    }
}
