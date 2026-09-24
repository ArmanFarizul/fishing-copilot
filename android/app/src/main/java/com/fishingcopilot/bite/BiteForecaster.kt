package com.fishingcopilot.bite

import com.fishingcopilot.astro.Astro
import com.fishingcopilot.data.hijri.HijriRepository
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.marine.MarineRepository
import com.fishingcopilot.data.tide.TideRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

/**
 * One-shot Bite Score forecast from stored data, for work that runs without the UI (alerts).
 * Uses the same inputs as the home card, so both agree.
 */
class BiteForecaster(
    private val spots: FishingDao,
    private val tides: TideRepository,
    private val marine: MarineRepository,
    private val hijri: HijriRepository,
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    suspend fun forecast(spotId: Long, now: Long): Pair<String, BiteForecast>? {
        val spot = spots.spotFlow(spotId).first() ?: return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val hijriByDate = hijri.calendar(today, 2).first().associate { it.date to it.hijri }
        val sunByDate = (0L..1L).associate { offset ->
            val day = today.plusDays(offset)
            val (dawn, rise, set, dusk) = Astro.sun(spot.latitude, spot.longitude, day, zone)
            day to SunWindow(dawn, rise, set, dusk)
        }
        val inputs = BiteInputs(
            tideModel = tides.model(spotId).first(),
            tideOffsetMinutes = spot.tideOffsetMinutes,
            marineHours = marine.forecast(spotId).first()?.hours.orEmpty(),
            hijriByDate = hijriByDate,
            sunByDate = sunByDate,
            zone = zone
        )
        return spot.name to BiteTimeline.forecast(now, inputs)
    }
}
