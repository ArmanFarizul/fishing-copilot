package com.fishingcopilot.tide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TideModelTest {
    private val epoch = 1_767_225_600_000L
    private val minuteMs = 60_000L

    private val pureM2 = TideModel(
        epochMillis = epoch,
        meanLevel = 1.0,
        constants = listOf(HarmonicConstant(Constituent.M2, amplitude = 0.8, phaseDegrees = 0.0))
    )

    @Test
    fun `height follows amplitude cos of speed times time minus phase`() {
        assertEquals(1.8, pureM2.heightAt(epoch), 1e-9)
        val quarterPeriodMs = (90.0 / Constituent.M2.speedDegreesPerHour * 3_600_000).toLong()
        assertEquals(1.0, pureM2.heightAt(epoch + quarterPeriodMs), 1e-4)
    }

    @Test
    fun `events alternate high and low at the M2 half period`() {
        val halfPeriodMinutes = 180.0 / Constituent.M2.speedDegreesPerHour * 60
        val events = pureM2.events(epoch - 30 * minuteMs, epoch + 25 * 60 * minuteMs)

        // High at 0 h, then a turn every ~6.21 h: 0, 6.2, 12.4, 18.6 and 24.8 h fall inside the window.
        assertEquals(5, events.size)
        events.zipWithNext().forEach { (a, b) -> assertTrue(a.type != b.type) }
        events.forEachIndexed { i, event ->
            val expectedMinutes = i * halfPeriodMinutes
            val actualMinutes = (event.epochMillis - epoch) / minuteMs.toDouble()
            assertEquals("event $i time", expectedMinutes, actualMinutes, 1.0)
            assertEquals(if (i % 2 == 0) TideEventType.HIGH else TideEventType.LOW, event.type)
            assertEquals(if (i % 2 == 0) 1.8 else 0.2, event.height, 1e-3)
        }
    }
}
