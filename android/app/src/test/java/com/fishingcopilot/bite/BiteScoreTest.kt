package com.fishingcopilot.bite

import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.tide.TideEvent
import com.fishingcopilot.tide.TideEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class BiteScoreTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")
    private val base = ZonedDateTime.of(2026, 9, 25, 0, 0, 0, 0, zone)
    private fun at(h: Int, m: Int = 0) = base.plusHours(h.toLong()).plusMinutes(m.toLong())
    private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()

    // High water at 10:00, low at 16:12.
    private val events = listOf(
        TideEvent(ms(at(10)), TideEventType.HIGH, 1.0),
        TideEvent(ms(at(16, 12)), TideEventType.LOW, -1.0)
    )

    @Test
    fun `tide factor follows the spec around each turn`() {
        assertEquals(TideFactor.SLACK, TideFactor.at(ms(at(10, 20)), events))   // within 30 min after the high
        assertEquals(TideFactor.SLACK, TideFactor.at(ms(at(9, 35)), events))    // within 30 min before it
        assertEquals(TideFactor.MID_CYCLE, TideFactor.at(ms(at(10, 40)), events)) // 30-45 min: not yet moving well
        assertEquals(TideFactor.MOVING, TideFactor.at(ms(at(10, 45)), events))
        assertEquals(TideFactor.MOVING, TideFactor.at(ms(at(12, 30)), events))  // 150 min after
        assertEquals(TideFactor.MID_CYCLE, TideFactor.at(ms(at(13, 30)), events))
        assertEquals(TideFactor.MOVING, TideFactor.at(ms(at(17, 0)), events))   // 48 min after the low
        assertEquals(1.0, TideFactor.MOVING.value, 0.0)
        assertEquals(0.6, TideFactor.MID_CYCLE.value, 0.0)
        assertEquals(0.2, TideFactor.SLACK.value, 0.0)
    }

    @Test
    fun `light factor uses dawn and dusk windows, midday and night`() {
        val sun = SunWindow(
            civilDawn = at(6, 34), sunrise = at(6, 55), sunset = at(19, 1), civilDusk = at(19, 22)
        )
        assertEquals(LightFactor.GOLDEN, LightFactor.at(at(6, 40), sun))
        assertEquals(LightFactor.GOLDEN, LightFactor.at(at(7, 50), sun))    // up to an hour after sunrise
        assertEquals(LightFactor.DAYLIGHT, LightFactor.at(at(8, 0), sun))
        assertEquals(LightFactor.MIDDAY, LightFactor.at(at(11, 0), sun))
        assertEquals(LightFactor.MIDDAY, LightFactor.at(at(14, 59), sun))
        assertEquals(LightFactor.DAYLIGHT, LightFactor.at(at(15, 0), sun))
        assertEquals(LightFactor.GOLDEN, LightFactor.at(at(18, 1), sun))    // from an hour before sunset
        assertEquals(LightFactor.GOLDEN, LightFactor.at(at(19, 20), sun))
        assertEquals(LightFactor.NIGHT, LightFactor.at(at(19, 30), sun))
        assertEquals(LightFactor.NIGHT, LightFactor.at(at(2, 0), sun))
    }

    @Test
    fun `solunar factor from tide strength of the Hijri day`() {
        assertEquals(1.0, SolunarFactor.of(TideStrength.SPRING), 0.0)
        assertEquals(0.3, SolunarFactor.of(TideStrength.NEAP), 0.0)
        assertEquals(0.7, SolunarFactor.of(TideStrength.NORMAL), 0.0)
    }

    @Test
    fun `barometer factor`() {
        assertEquals(1.0, BaroFactor.of(1012.0, PressureTrend.STEADY), 0.0)
        assertEquals(0.8, BaroFactor.of(1008.0, PressureTrend.STEADY), 0.0)
        assertEquals(0.7, BaroFactor.of(1012.0, PressureTrend.RISING), 0.0)
        assertEquals(0.5, BaroFactor.of(1012.0, PressureTrend.FALLING), 0.0)
        assertEquals(0.2, BaroFactor.of(1012.0, PressureTrend.FALLING_FAST), 0.0)
    }

    @Test
    fun `score is 1 plus nine times the weighted factors`() {
        assertEquals(10.0, BiteScore.combine(1.0, 1.0, 1.0, 1.0).score, 1e-9)
        // Spec example weights: 0.35*0.2 + 0.25*0.3 + 0.25*0.3 + 0.15*0.2 = 0.25 -> 3.25
        assertEquals(3.25, BiteScore.combine(0.2, 0.3, 0.3, 0.2).score, 1e-9)
    }

    @Test
    fun `a missing factor is left out and the other weights rescaled`() {
        // Without the barometer: (0.35*1 + 0.25*0.6 + 0.25*1) / 0.85 = 0.882 -> 8.94
        val score = BiteScore.combine(tide = 1.0, light = 0.6, solunar = 1.0, baro = null)
        assertEquals(1 + 9 * (0.35 + 0.15 + 0.25) / 0.85, score.score, 1e-9)
        assertNull(score.baro)
    }

    @Test
    fun `best window is the longest run at or above the threshold around the peak`() {
        val start = ms(at(0))
        val step = Duration.ofMinutes(15).toMillis()
        val scores = listOf(5.0, 6.0, 7.6, 8.4, 8.9, 8.2, 7.5, 6.0, 7.9, 7.0).mapIndexed { i, s -> ScorePoint(start + i * step, s) }
        val best = bestWindow(scores, threshold = 7.5)!!
        assertEquals(start + 2 * step, best.start)
        assertEquals(start + 6 * step, best.end)
        assertEquals(8.9, best.peak, 0.0)
    }
}
