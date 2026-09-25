package com.fishingcopilot.maps

import com.fishingcopilot.data.local.SpotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineMapPlanTest {
    @Test
    fun `overview first, then a close-up per spot`() {
        val plan = offlinePlan(
            listOf(
                SpotEntity(id = 4, name = "Kuala Terengganu", latitude = 5.35, longitude = 103.17),
                SpotEntity(id = 9, name = "Kukup", latitude = 1.325, longitude = 103.44)
            )
        )
        assertEquals(listOf(OVERVIEW_KEY, "spot-4", "spot-9"), plan.map { it.key })
        assertEquals(0.0, plan[0].minZoom, 0.0)
        assertEquals(10.0, plan[0].maxZoom, 0.0)

        val kt = plan[1]
        assertEquals(11.0, kt.minZoom, 0.0)
        assertEquals(14.0, kt.maxZoom, 0.0)
        assertEquals(5.0, kt.bounds.south, 1e-9)
        assertEquals(5.7, kt.bounds.north, 1e-9)
        assertEquals(102.82, kt.bounds.west, 1e-9)
        assertEquals(103.52, kt.bounds.east, 1e-9)
    }

    @Test
    fun `no spots still keeps the overview`() {
        assertEquals(listOf(OVERVIEW_KEY), offlinePlan(emptyList()).map { it.key })
    }
}
