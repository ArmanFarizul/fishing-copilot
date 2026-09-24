package com.fishingcopilot.bite

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.marine.MarineHour
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.tide.Constituent
import com.fishingcopilot.tide.HarmonicConstant
import com.fishingcopilot.tide.TideModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class BiteTimelineTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")
    private val today = LocalDate.of(2026, 9, 25)
    private val now = ZonedDateTime.of(today, java.time.LocalTime.of(5, 7), zone).toInstant().toEpochMilli()
    private val hourMs = 3_600_000L

    private val model = TideModel(
        epochMillis = now,
        meanLevel = 0.0,
        constants = listOf(HarmonicConstant(Constituent.M2, 1.0, 0.0)) // high water right now
    )
    private val sun = SunWindow(
        civilDawn = ZonedDateTime.of(today, java.time.LocalTime.of(6, 34), zone),
        sunrise = ZonedDateTime.of(today, java.time.LocalTime.of(6, 55), zone),
        sunset = ZonedDateTime.of(today, java.time.LocalTime.of(19, 1), zone),
        civilDusk = ZonedDateTime.of(today, java.time.LocalTime.of(19, 22), zone)
    )
    private val hours = (-6..30).map { i ->
        val t = now - Math.floorMod(now, hourMs) + i * hourMs
        MarineHour(epochMillis = t, pressureHpa = 1012.0)
    }

    private fun inputs(model: TideModel? = this.model, hours: List<MarineHour> = this.hours) = BiteInputs(
        tideModel = model,
        tideOffsetMinutes = 0,
        marineHours = hours,
        hijriByDate = mapOf(today to HijriDate(1448, 4, 14), today.plusDays(1) to HijriDate(1448, 4, 15)),
        sunByDate = mapOf(today to sun, today.plusDays(1) to sun.copy(
            civilDawn = sun.civilDawn!!.plusDays(1), sunrise = sun.sunrise!!.plusDays(1),
            sunset = sun.sunset!!.plusDays(1), civilDusk = sun.civilDusk!!.plusDays(1)
        )),
        zone = zone
    )

    @Test
    fun `current moment combines the four factors`() {
        val forecast = BiteTimeline.forecast(now, inputs())
        val moment = forecast.now
        assertEquals(TideFactor.SLACK, moment.tide)          // high water right now
        assertEquals(LightFactor.NIGHT, moment.light)        // 05:07, before first light
        assertEquals(TideStrength.SPRING, moment.tideStrength) // Hijri day 14
        assertEquals(PressureTrend.STEADY, moment.pressureTrend)
        val expected = BiteScore.combine(0.2, 0.6, 1.0, 1.0).score
        assertEquals(expected, moment.score.score, 1e-9)
    }

    @Test
    fun `timeline covers 24 hours in 15 minute steps and finds a prime window`() {
        val forecast = BiteTimeline.forecast(now, inputs())
        assertEquals(1 + 96, forecast.points.size)
        assertTrue(forecast.points.drop(1).zipWithNext().all { (a, b) -> b.epochMillis - a.epochMillis == 15 * 60_000L })
        val best = forecast.best
        assertNotNull(best)
        assertTrue(best!!.peak >= BiteTimeline.PRIME_THRESHOLD)
        assertTrue(best.start in now..now + 24 * hourMs)
    }

    @Test
    fun `missing tide and pressure are left out rather than scored as zero`() {
        val moment = BiteTimeline.forecast(now, inputs(model = null, hours = emptyList())).now
        assertNull(moment.tide)
        assertNull(moment.score.baro)
        // Light 0.6 and moon 1.0 only, each weighted 0.25: raw 0.8 -> 8.2
        assertEquals(1 + 9 * 0.8, moment.score.score, 1e-9)
    }
}
