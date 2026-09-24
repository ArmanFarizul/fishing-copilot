package com.fishingcopilot.data.tide

import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideConstantEntity
import com.fishingcopilot.data.local.TideDao
import com.fishingcopilot.data.local.TideModelEntity
import com.fishingcopilot.data.local.TideModelWithConstants
import com.fishingcopilot.data.remote.OpenMeteoMarineClient
import com.fishingcopilot.tide.Constituent
import com.fishingcopilot.tide.HarmonicConstant
import com.fishingcopilot.tide.TideModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TideRepositoryTest {
    private class FakeTideDao : TideDao {
        val models = MutableStateFlow<Map<Long, TideModelWithConstants>>(emptyMap())

        override fun modelWithConstants(spotId: Long): Flow<TideModelWithConstants?> = models.map { it[spotId] }
        override suspend fun findModel(spotId: Long): TideModelEntity? = models.value[spotId]?.model
        override suspend fun deleteModel(spotId: Long) { models.value -= spotId }
        override suspend fun insertModel(model: TideModelEntity) {
            models.value += model.spotId to TideModelWithConstants(model, emptyList())
        }
        override suspend fun insertConstants(constants: List<TideConstantEntity>) {
            constants.groupBy { it.spotId }.forEach { (spotId, rows) ->
                val existing = models.value.getValue(spotId)
                models.value += spotId to existing.copy(constants = existing.constants + rows)
            }
        }
    }

    private val spot = SpotEntity(id = 7, name = "Kukup", latitude = 1.325, longitude = 103.44)
    private val now = Instant.parse("2026-09-25T03:00:00Z").toEpochMilli()
    private val dayMs = 86_400_000L

    private val truth = TideModel(
        epochMillis = Instant.parse("2025-09-24T00:00:00Z").toEpochMilli(),
        meanLevel = 0.7,
        constants = listOf(
            HarmonicConstant(Constituent.M2, 0.87, 20.0),
            HarmonicConstant(Constituent.K1, 0.29, 115.0)
        )
    )

    private var requestedUrls = mutableListOf<String>()

    /** Serves a year of hourly heights from [truth], so the fitted model can be checked against it. */
    private val client = OpenMeteoMarineClient { url ->
        requestedUrls += url
        val hours = (0 until 365 * 24).map { truth.epochMillis / 1000 + it * 3600L }
        val heights = hours.map { truth.heightAt(it * 1000) }
        """{"hourly":{"time":$hours,"sea_level_height_msl":$heights}}"""
    }

    private fun repository(dao: TideDao, clock: Long = now) = TideRepository(dao, client) { clock }

    @Test
    fun `refresh fits a year ending yesterday and stores the model`() = runTest {
        val dao = FakeTideDao()
        repository(dao).refresh(spot)

        assertTrue("start_date=2025-09-24" in requestedUrls.single())
        assertTrue("end_date=2026-09-24" in requestedUrls.single())

        val stored = repository(dao).model(spot.id).first()!!
        val probe = Instant.parse("2026-10-01T06:00:00Z").toEpochMilli()
        assertEquals(truth.heightAt(probe), stored.heightAt(probe), 1e-4)
        assertEquals(now, dao.findModel(spot.id)!!.fittedAtMillis)
    }

    @Test
    fun `model is null for a spot that was never fitted`() = runTest {
        assertNull(repository(FakeTideDao()).model(spot.id).first())
    }

    @Test
    fun `refreshIfStale fetches only when missing or older than the refresh window`() = runTest {
        val dao = FakeTideDao()
        assertTrue(repository(dao).refreshIfStale(spot))
        assertEquals(1, requestedUrls.size)

        assertFalse(repository(dao, clock = now + 179 * dayMs).refreshIfStale(spot))
        assertEquals(1, requestedUrls.size)

        assertTrue(repository(dao, clock = now + 181 * dayMs).refreshIfStale(spot))
        assertEquals(2, requestedUrls.size)
    }

    @Test(expected = NoMarineDataException::class)
    fun `a point without sea data reports NoMarineDataException`() = runTest {
        val landClient = OpenMeteoMarineClient { """{"hourly":{"time":[1789862400,1789866000],"sea_level_height_msl":[null,null]}}""" }
        TideRepository(FakeTideDao(), landClient) { now }.refresh(spot)
    }

    @Test
    fun `refresh replaces rather than appends constants`() = runTest {
        val dao = FakeTideDao()
        repository(dao).refresh(spot)
        repository(dao).refresh(spot)
        val constants = dao.models.value.getValue(spot.id).constants
        assertEquals(Constituent.entries.size, constants.size)
    }
}
