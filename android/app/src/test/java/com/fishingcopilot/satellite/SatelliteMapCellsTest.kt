package com.fishingcopilot.satellite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SatelliteMapCellsTest {
    // 2 x 2 cells of 0.25 degrees from 1.0 N, 103.0 E.
    private val grid = SatelliteGrid(lat0 = 1.0, lon0 = 103.0, step = 0.25, nlat = 2, nlon = 2)
    private val day = LocalDate.of(2026, 9, 23)

    @Test
    fun `squares follow the grid and skip land`() {
        val data = SatelliteData(grid, null, null, SatelliteLayer(day, listOf(0.1, null, 0.8, 2.0)), null)
        val layer = mapCells(data, SatelliteMapLayer.PLANKTON)!!
        assertEquals(day, layer.date)
        assertEquals(3, layer.cells.size)
        // Index 2 is row 1, column 0.
        val second = layer.cells[1]
        assertEquals(1.25, second.south, 1e-9)
        assertEquals(1.5, second.north, 1e-9)
        assertEquals(103.0, second.west, 1e-9)
        assertEquals(103.25, second.east, 1e-9)
        assertEquals(listOf(PlanktonLevel.LOW, PlanktonLevel.IDEAL, PlanktonLevel.HIGH), layer.cells.map { it.level })
    }

    @Test
    fun `fronts draw only squares at a front level`() {
        val data = SatelliteData(grid, null, SatelliteLayer(day, listOf(0.2, 0.6, 1.1, null)), null, null)
        assertEquals(listOf(FrontStrength.WEAK, FrontStrength.STRONG), mapCells(data, SatelliteMapLayer.FRONTS)!!.cells.map { it.level })
    }

    @Test
    fun `a layer missing from the file has nothing to draw`() {
        assertNull(mapCells(SatelliteData(grid, null, null, null, null), SatelliteMapLayer.CLARITY))
    }
}
