package com.fishingcopilot.catchlog

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.MoonPhaseName
import com.fishingcopilot.data.local.CatchLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatchLogTest {
    private val conditions = CatchConditions(
        timestamp = 1_790_300_000_000L,
        spotId = 3,
        spotName = "Kukup",
        waterLevel = 0.62,
        rising = true,
        moonPhase = MoonPhaseName.WAXING_GIBBOUS,
        hijri = HijriDate(1448, 4, 13),
        biteScore = 8.4
    )

    @Test
    fun `entry keeps the auto-recorded conditions and trims what the user typed`() {
        val entity = conditions.toEntity(
            species = "  Siakap ", bait = " udang hidup  ", weightKg = 2.4, lengthCm = null, photoUri = null, notes = "  "
        )
        assertEquals(
            CatchLogEntity(
                spotId = 3, species = "Siakap", weightKg = 2.4, lengthCm = null, baitUsed = "udang hidup",
                waterLevel = 0.62, tideState = "RISING", moonPhase = "WAXING_GIBBOUS", biteScore = 8.4,
                photoUri = null, timestamp = 1_790_300_000_000L, hijriDate = "1448-04-13", notes = null
            ),
            entity
        )
    }

    @Test
    fun `missing conditions stay null`() {
        val entity = CatchConditions(1L, null, null, null, null, null, null, null)
            .toEntity("Pari", null, null, null, null, null)
        assertNull(entity.tideState)
        assertNull(entity.hijriDate)
        assertNull(entity.biteScore)
    }

    private fun log(species: String, bait: String?, tide: String?) = CatchLogEntity(
        spotId = 1, species = species, weightKg = null, lengthCm = null, baitUsed = bait, waterLevel = null,
        tideState = tide, moonPhase = null, biteScore = null, photoUri = null
    )

    @Test
    fun `summary finds the top bait, tide and species`() {
        val logs = listOf(
            log("SIAKAP", "Udang hidup", "RISING"),
            log("SIAKAP", "udang hidup ", "RISING"),
            log("JENAHAK", "Sotong", "FALLING"),
            log("SIAKAP", null, "RISING"),
            log("Belanak", "udang hidup", null)
        )
        val summary = LogSummary.of(logs)!!
        assertEquals(5, summary.count)
        assertEquals("Udang hidup", summary.topBait)
        assertEquals(75, summary.topBaitPercent) // 3 of the 4 catches with a bait
        assertEquals(true, summary.bestTideRising)
        assertEquals("SIAKAP", summary.topSpecies)
        assertEquals(3, summary.topSpeciesCount)
    }

    @Test
    fun `no summary for an empty log and no tide verdict on a tie`() {
        assertNull(LogSummary.of(emptyList()))
        val tie = LogSummary.of(listOf(log("PARI", null, "RISING"), log("PARI", null, "FALLING")))!!
        assertNull(tie.bestTideRising)
        assertNull(tie.topBait)
    }

    @Test
    fun `conditions come back from a saved catch`() {
        val log = CatchConditions(1_790_000_000_000L, 3, "Kukup", 0.4, false, MoonPhaseName.FULL_MOON, HijriDate(1448, 4, 13), 8.2)
            .toEntity("SIAKAP", null, null, null, null, null)
        val back = CatchConditions.of(log, "Kukup")
        assertEquals(false, back.rising)
        assertEquals(MoonPhaseName.FULL_MOON, back.moonPhase)
        assertEquals(HijriDate(1448, 4, 13), back.hijri)
        assertEquals(8.2, back.biteScore!!, 0.0)
    }
}
