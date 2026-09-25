package com.fishingcopilot.catchlog

import com.fishingcopilot.data.local.CatchLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaitTrackerTest {
    private fun log(species: String, bait: String?, kg: Double? = null) = CatchLogEntity(
        spotId = null, species = species, weightKg = kg, lengthCm = null, baitUsed = bait,
        waterLevel = null, tideState = null, moonPhase = null, biteScore = null, photoUri = null, timestamp = 0
    )

    @Test
    fun `baits rank by catches, merge spellings and keep the newest spelling`() {
        val stats = baitStats(
            listOf(
                log("SIAKAP", "Udang hidup", 2.0),
                log("SIAKAP", "udang hidup ", 4.0),
                log("SIAKAP", "sotong", 1.0),
                log("SIAKAP", null, 9.0),
                log("SIAKAP", "  ")
            )
        )
        assertEquals(listOf("Udang hidup", "sotong"), stats.map { it.bait })
        val top = stats.first()
        assertEquals(2, top.catches)
        // Shares count only catches with a bait noted: 2 of 3.
        assertEquals(66, top.sharePercent)
        assertEquals(3.0, top.averageKg!!, 1e-9)
        assertEquals(4.0, top.biggestKg!!, 1e-9)
    }

    @Test
    fun `a tie on catches goes to the bait with bigger fish`() {
        val stats = baitStats(listOf(log("PARI", "sotong", 1.0), log("PARI", "tamban", 3.0)))
        assertEquals("tamban", stats.first().bait)
    }

    @Test
    fun `no weights leave averages empty, no baits give no stats`() {
        assertNull(baitStats(listOf(log("PARI", "sotong"))).single().averageKg)
        assertTrue(baitStats(listOf(log("PARI", null))).isEmpty())
    }

    @Test
    fun `species are listed most caught first`() {
        val counts = speciesCounts(listOf(log("PARI", null), log("SIAKAP", null), log("SIAKAP", null)))
        assertEquals(listOf("SIAKAP" to 2, "PARI" to 1), counts)
    }
}
