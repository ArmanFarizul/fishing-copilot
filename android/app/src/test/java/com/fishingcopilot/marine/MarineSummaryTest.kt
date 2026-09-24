package com.fishingcopilot.marine

import org.junit.Assert.assertEquals
import org.junit.Test

class MarineSummaryTest {
    private val hourMs = 3_600_000L
    private val t0 = 1_790_208_000_000L // 2026-09-24T00:00Z

    private fun hour(i: Int, wave: Double = 0.5, wind: Double = 8.0, pressure: Double = 1010.0) = MarineHour(
        epochMillis = t0 + i * hourMs,
        waveHeight = wave, wavePeriod = 5.0, waveDirection = 45.0,
        windKn = wind, gustKn = wind + 5, windDirection = 90.0,
        pressureHpa = pressure, currentKn = 0.6, currentDirection = 180.0, seaTempC = 29.5
    )

    @Test
    fun `wave status follows the spec thresholds`() {
        assertEquals(WaveStatus.CALM, WaveStatus.of(0.79))
        assertEquals(WaveStatus.CAUTION, WaveStatus.of(0.8))
        assertEquals(WaveStatus.CAUTION, WaveStatus.of(1.5))
        assertEquals(WaveStatus.DANGER, WaveStatus.of(1.51))
    }

    @Test
    fun `pressure trend uses the change over three hours`() {
        assertEquals(PressureTrend.STEADY, PressureTrend.of(0.9))
        assertEquals(PressureTrend.STEADY, PressureTrend.of(-0.9))
        assertEquals(PressureTrend.RISING, PressureTrend.of(1.0))
        assertEquals(PressureTrend.FALLING, PressureTrend.of(-1.0))
        assertEquals(PressureTrend.FALLING, PressureTrend.of(-3.0))
        assertEquals(PressureTrend.FALLING_FAST, PressureTrend.of(-3.1))
    }

    @Test
    fun `compass maps degrees to eight points`() {
        assertEquals(Compass.N, Compass.of(0.0))
        assertEquals(Compass.N, Compass.of(22.4))
        assertEquals(Compass.NE, Compass.of(22.5))
        assertEquals(Compass.E, Compass.of(90.0))
        assertEquals(Compass.NW, Compass.of(337.4))
        assertEquals(Compass.N, Compass.of(359.0))
    }

    @Test
    fun `summary uses the latest past hour, the three hour pressure change and the next day's peaks`() {
        val hours = (0..48).map { i ->
            hour(i, wave = if (i == 30) 1.7 else 0.5, wind = if (i == 20) 22.0 else 8.0, pressure = 1012.0 - i * 0.5)
        }
        val now = t0 + 10 * hourMs + 20 * 60_000

        val summary = summarizeMarine(hours, now)!!

        assertEquals(t0 + 10 * hourMs, summary.current.epochMillis)
        assertEquals(WaveStatus.CALM, summary.waveStatus)
        assertEquals(PressureTrend.FALLING, summary.pressureTrend) // -1.5 hPa over 3 h
        assertEquals(1.7, summary.maxWaveNext24h!!, 1e-9)
        assertEquals(22.0, summary.maxWindNext24h!!, 1e-9)
    }

    @Test
    fun `no summary without any hour at or before now`() {
        assertEquals(null, summarizeMarine(listOf(hour(5)), t0))
    }
}
