package com.fishingcopilot.data.spots

import org.junit.Assert.assertEquals
import org.junit.Test

class CoastalAreaTest {
    @Test
    fun `one degree of latitude is about 111 km`() {
        assertEquals(111.19, haversineKm(2.0, 101.0, 3.0, 101.0), 0.01)
    }

    @Test
    fun `an inland point in Kuala Lumpur suggests Port Klang`() {
        val (area, km) = CoastalArea.nearest(3.139, 101.687)
        assertEquals(CoastalArea.PELABUHAN_KLANG, area)
        assertEquals(46.0, km, 3.0)
    }

    @Test
    fun `a point on an area's own coordinates is zero km from it`() {
        CoastalArea.entries.forEach { area ->
            val (nearest, km) = CoastalArea.nearest(area.latitude, area.longitude)
            assertEquals(area, nearest)
            assertEquals(0.0, km, 1e-9)
        }
    }
}
