package com.fishingcopilot.satellite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SatelliteSummaryTest {
    // 4 x 4 cells of 0.25 degrees from 1.0 N, 103.0 E.
    private val grid = SatelliteGrid(lat0 = 1.0, lon0 = 103.0, step = 0.25, nlat = 4, nlon = 4)
    private val day = LocalDate.of(2026, 9, 23)

    private fun layer(vararg cells: Pair<Pair<Int, Int>, Double>, date: LocalDate = day): SatelliteLayer {
        val values = MutableList<Double?>(16) { null }
        cells.forEach { (rc, v) -> values[rc.first * 4 + rc.second] = v }
        return SatelliteLayer(date, values)
    }

    private fun data(
        sst: SatelliteLayer? = null,
        front: SatelliteLayer? = null,
        chl: SatelliteLayer? = null,
        zsd: SatelliteLayer? = null
    ) = SatelliteData(grid, sst, front, chl, zsd)

    @Test
    fun `reads the spot's own cell`() {
        // Cell row 1, col 1 covers 1.25-1.5 N, 103.25-103.5 E.
        val summary = summarizeSatellite(data(sst = layer((1 to 1) to 29.4, (1 to 2) to 31.0)), 1.3, 103.3)!!
        assertEquals(29.4, summary.seaTempC!!, 0.0)
    }

    @Test
    fun `falls back to the nearest sea cell when the spot's cell is land`() {
        val summary = summarizeSatellite(data(chl = layer((1 to 2) to 0.8, (3 to 3) to 5.0)), 1.3, 103.45)!!
        assertEquals(0.8, summary.chlorophyll!!, 0.0)
    }

    @Test
    fun `ignores cells beyond the nearest-cell radius`() {
        assertNull(summarizeSatellite(data(sst = layer((3 to 3) to 29.0)), 1.1, 103.1))
    }

    @Test
    fun `spot outside the grid has no data`() {
        assertNull(summarizeSatellite(data(sst = layer((0 to 0) to 29.0)), 5.0, 110.0))
    }

    @Test
    fun `finds the nearest front at or above the weak band, with its direction`() {
        val summary = summarizeSatellite(
            data(sst = layer((1 to 1) to 29.0), front = layer((1 to 1) to 0.3, (2 to 1) to 0.6, (1 to 3) to 1.2)),
            1.375, 103.375
        )!!
        val front = summary.front!!
        assertEquals(FrontStrength.WEAK, front.strength)
        assertEquals(27.8, front.distanceKm, 0.5)
        assertEquals(0.0, front.bearingDeg, 1.0)
    }

    @Test
    fun `a front in the spot's own cell is at the spot`() {
        val summary = summarizeSatellite(data(sst = layer((1 to 1) to 29.0), front = layer((1 to 1) to 1.1)), 1.3, 103.3)!!
        assertEquals(FrontStrength.STRONG, summary.front!!.strength)
        assertEquals(0.0, summary.front!!.distanceKm, 0.0)
    }

    @Test
    fun `no front when every nearby gradient is gentle`() {
        val summary = summarizeSatellite(data(sst = layer((1 to 1) to 29.0), front = layer((1 to 1) to 0.49)), 1.3, 103.3)!!
        assertNull(summary.front)
    }

    @Test
    fun `data date is the oldest layer actually shown`() {
        val summary = summarizeSatellite(
            data(sst = layer((1 to 1) to 29.0), chl = layer((1 to 1) to 0.5, date = day.minusDays(1)), zsd = layer(date = day.minusDays(5))),
            1.3, 103.3
        )!!
        assertEquals(day.minusDays(1), summary.dataDate)
    }

    @Test
    fun `levels follow the spec bands`() {
        assertEquals(PlanktonLevel.LOW, PlanktonLevel.of(0.19))
        assertEquals(PlanktonLevel.IDEAL, PlanktonLevel.of(0.2))
        assertEquals(PlanktonLevel.IDEAL, PlanktonLevel.of(1.5))
        assertEquals(PlanktonLevel.HIGH, PlanktonLevel.of(1.51))
        assertEquals(WaterTempLevel.COOL, WaterTempLevel.of(27.9))
        assertEquals(WaterTempLevel.IDEAL, WaterTempLevel.of(30.0))
        assertEquals(WaterTempLevel.WARM, WaterTempLevel.of(30.1))
        assertEquals(ClarityLevel.MURKY, ClarityLevel.of(4.9))
        assertEquals(ClarityLevel.MODERATE, ClarityLevel.of(5.0))
        assertEquals(ClarityLevel.CLEAR, ClarityLevel.of(15.0))
        assertNull(FrontStrength.of(0.49))
        assertEquals(FrontStrength.STRONG, FrontStrength.of(1.0))
    }
}
