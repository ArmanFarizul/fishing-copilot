package com.fishingcopilot.data.tide

import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.local.TideConstantEntity
import com.fishingcopilot.data.local.TideDao
import com.fishingcopilot.data.local.TideModelEntity
import com.fishingcopilot.data.local.TideModelWithConstants
import com.fishingcopilot.data.remote.OpenMeteoMarineClient
import com.fishingcopilot.tide.Constituent
import com.fishingcopilot.tide.HarmonicAnalysis
import com.fishingcopilot.tide.HarmonicConstant
import com.fishingcopilot.tide.TideModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneOffset

/** Open-Meteo returned no sea level for the spot, usually because the point is on land. */
class NoMarineDataException : Exception("No sea level data for this location")

class TideRepository(
    private val dao: TideDao,
    private val client: OpenMeteoMarineClient,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun model(spotId: Long): Flow<TideModel?> = dao.modelWithConstants(spotId).map { it?.toModel() }

    /** Fits a spot's model from the last full year of Open-Meteo sea level. Needs internet. */
    suspend fun refresh(spot: SpotEntity): TideModel {
        val today = Instant.ofEpochMilli(clock()).atZone(ZoneOffset.UTC).toLocalDate()
        val end = today.minusDays(1)
        val samples = client.seaLevel(spot.latitude, spot.longitude, start = end.minusDays(365), end = end)
        // A month of hours is the least that still separates the main constituents.
        if (samples.size < MIN_SAMPLES) throw NoMarineDataException()
        val model = HarmonicAnalysis.fit(samples, epochMillis = samples.first().epochMillis)
        dao.replaceModel(
            TideModelEntity(spot.id, model.epochMillis, model.meanLevel, fittedAtMillis = clock()),
            model.constants.map { TideConstantEntity(spot.id, it.constituent.name, it.amplitude, it.phaseDegrees) }
        )
        return model
    }

    /** Refits when the spot has no model or its model is older than [REFRESH_AFTER_MILLIS]. Returns whether it refitted. */
    suspend fun refreshIfStale(spot: SpotEntity): Boolean {
        val existing = dao.findModel(spot.id)
        if (existing != null && clock() - existing.fittedAtMillis <= REFRESH_AFTER_MILLIS) return false
        refresh(spot)
        return true
    }

    private fun TideModelWithConstants.toModel() = TideModel(
        epochMillis = model.epochMillis,
        meanLevel = model.meanLevel,
        // Rows for a constituent that no longer exists in code are skipped rather than crashing.
        constants = constants.mapNotNull { row ->
            Constituent.entries.firstOrNull { it.name == row.constituent }
                ?.let { HarmonicConstant(it, row.amplitude, row.phaseDegrees) }
        }
    )

    companion object {
        // Nodal modulation shifts amplitudes by up to a few percent per year, so refit twice a year.
        const val REFRESH_AFTER_MILLIS = 180L * 24 * 60 * 60 * 1000
        private const val MIN_SAMPLES = 30 * 24
    }
}
