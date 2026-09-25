package com.fishingcopilot.tide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TideSummaryTest {
    private val epoch = 1_767_225_600_000L
    private val minuteMs = 60_000L
    private val halfPeriodMinutes = 180.0 / Constituent.M2.speedDegreesPerHour * 60

    private val pureM2 = TideModel(
        epochMillis = epoch,
        meanLevel = 1.0,
        constants = listOf(HarmonicConstant(Constituent.M2, amplitude = 0.8, phaseDegrees = 0.0))
    )

    private fun minutesFromEpoch(millis: Long) = (millis - epoch) / minuteMs.toDouble()

    @Test
    fun `at high water the tide is about to fall and heights are relative to mean`() {
        val summary = pureM2.summarize(now = epoch, offsetMinutes = 0)

        assertEquals(0.8, summary.heightNow, 1e-6)
        assertFalse(summary.rising)
        assertEquals(halfPeriodMinutes, minutesFromEpoch(summary.nextLow!!.epochMillis), 1.0)
        assertEquals(-0.8, summary.nextLow!!.height, 1e-3)
        assertEquals(2 * halfPeriodMinutes, minutesFromEpoch(summary.nextHigh!!.epochMillis), 1.0)
        assertEquals(0.8, summary.nextHigh!!.height, 1e-3)
    }

    @Test
    fun `a positive offset makes the spot turn later`() {
        val summary = pureM2.summarize(now = epoch, offsetMinutes = 30)

        assertTrue("30 min before the delayed high the water is still rising", summary.rising)
        assertEquals(30.0, minutesFromEpoch(summary.nextHigh!!.epochMillis), 1.0)
        assertEquals(30 + halfPeriodMinutes, minutesFromEpoch(summary.nextLow!!.epochMillis), 1.0)
    }

    @Test
    fun `curve spans the chart window in ten minute steps`() {
        val summary = pureM2.summarize(now = epoch, offsetMinutes = 0)

        assertEquals(epoch - 2 * 60 * minuteMs, summary.curve.first().epochMillis)
        assertEquals(epoch + 22 * 60 * minuteMs, summary.curve.last().epochMillis)
        assertEquals(24 * 6 + 1, summary.curve.size)
        summary.curve.forEach { assertTrue(it.height in -0.8001..0.8001) }
    }

    @Test
    fun `slack only near high or low water, not at mid tide`() {
        assertTrue(pureM2.summarize(now = epoch, offsetMinutes = 0).slack)
        assertTrue(pureM2.summarize(now = epoch + 25 * minuteMs, offsetMinutes = 0).slack)
        assertFalse(pureM2.summarize(now = epoch + 40 * minuteMs, offsetMinutes = 0).slack)
        // Mid tide is where the height crosses the mean and the current runs hardest.
        val midTide = epoch + (halfPeriodMinutes / 2 * minuteMs).toLong()
        val summary = pureM2.summarize(now = midTide, offsetMinutes = 0)
        assertEquals(0.0, summary.heightNow, 0.01)
        assertFalse(summary.slack)
    }
}
