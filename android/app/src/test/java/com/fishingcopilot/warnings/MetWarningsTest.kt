package com.fishingcopilot.warnings

import com.fishingcopilot.data.remote.MetWarningClient
import com.fishingcopilot.data.spots.CoastalArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MetWarningsTest {
    // The live feed on 2026-09-25: one marine bulletin listed three times, a land thunderstorm warning and "No Advisory".
    private val feed = MetWarningClient.parse(javaClass.getResource("/metmalaysia_warnings_2026-09-25.json")!!.readText())
    private fun at(time: String) = LocalDateTime.parse(time).atZone(ZoneId.of("Asia/Kuala_Lumpur")).toInstant()
    private val morning = at("2026-09-25T07:00")

    private fun summary(area: CoastalArea, now: java.time.Instant = morning) =
        summarizeWarnings(feed, area.latitude, area.longitude, now)

    @Test
    fun `the marine bulletin splits into its numbered warnings`() {
        val all = summary(CoastalArea.KUALA_TERENGGANU).let { it.forSpot + it.elsewhere }
        assertEquals(
            listOf(
                // The spot's own first: the coastal warning naming Terengganu, then the Samui shipping warning.
                "THUNDERSTORMS WARNING",
                "THUNDERSTORMS WARNING",
                // Then the rest, newest bulletin first: the land warning, then the other marine items.
                "THUNDERSTORMS WARNING",
                "LOW HORIZONTAL VISIBILITY WARNING",
                "WARNING ON STRONG WINDS AND ROUGH SEAS (FIRST CATEGORY)"
            ),
            all.map { it.headingEn }
        )
    }

    @Test
    fun `Terengganu gets the coastal warning naming it and the Samui shipping warning`() {
        val s = summary(CoastalArea.KUALA_TERENGGANU)
        assertEquals(2, s.forSpot.size)
        val coastal = s.forSpot.first { it.textEn.contains("waters of Perak") }
        assertEquals("AMARAN RIBUT PETIR", coastal.headingBm)
        assertTrue(coastal.textBm.startsWith("Ribut petir, hujan lebat"))
        assertNull(coastal.instructionEn)
        assertTrue(s.forSpot.any { it.textEn.contains("Samui") })
        assertEquals(3, s.elsewhere.size)
    }

    @Test
    fun `land, coastal and Straits shipping warnings all reach a Selangor spot`() {
        val s = summary(CoastalArea.PELABUHAN_KLANG)
        assertEquals(3, s.forSpot.size)
        assertTrue(s.forSpot[0].textEn.contains("Klang and Kuala Langat"))
    }

    @Test
    fun `Miri gets low visibility, coastal, land and the Reef South shipping warning`() {
        assertEquals(4, summary(CoastalArea.MIRI).forSpot.size)
    }

    @Test
    fun `west Johor gets the coastal and Straits warnings, east Johor only Tioman`() {
        assertEquals(2, summary(CoastalArea.KUKUP).forSpot.size)
        val desaru = summary(CoastalArea.DESARU).forSpot.single()
        assertTrue(desaru.textEn.contains("Tioman"))
    }

    @Test
    fun `western Sabah and Labuan match, eastern Sabah does not`() {
        assertEquals(1, summary(CoastalArea.KOTA_KINABALU).forSpot.size)
        assertEquals(1, summary(CoastalArea.LABUAN).forSpot.size)
        assertEquals(0, summary(CoastalArea.SANDAKAN).forSpot.size)
    }

    @Test
    fun `shipping areas match by containment, and only where their waters are near`() {
        assertEquals(setOf("tioman"), shippingAreasFor(CoastalArea.MERSING.latitude, CoastalArea.MERSING.longitude))
        assertEquals(setOf("sulu", "sulawesi"), shippingAreasFor(CoastalArea.SEMPORNA.latitude, CoastalArea.SEMPORNA.longitude))
        assertEquals(setOf("sulu"), shippingAreasFor(CoastalArea.SANDAKAN.latitude, CoastalArea.SANDAKAN.longitude))
        assertEquals(setOf("bunguran"), shippingAreasFor(CoastalArea.SANTUBONG.latitude, CoastalArea.SANTUBONG.longitude))
    }

    @Test
    fun `Melaka gets the coastal warning and the Southern Straits warning`() {
        val s = summary(CoastalArea.MELAKA)
        assertEquals(2, s.forSpot.size)
        assertTrue(s.forSpot.any { it.textEn.contains("West Johor") })
        assertTrue(s.forSpot.any { it.textEn.contains("Southern Straits Of Melaka") })
    }

    @Test
    fun `expired warnings drop out`() {
        val afterStorm = summary(CoastalArea.PELABUHAN_KLANG, now = at("2026-09-25T10:00"))
        // The land warning ended at 09:00; the bulletin's coastal and Straits items stay while one of its windows is open.
        assertEquals(2, afterStorm.forSpot.size)
        val later = summary(CoastalArea.PELABUHAN_KLANG, now = at("2026-09-28T01:00"))
        assertEquals(0, later.forSpot.size + later.elsewhere.size)
    }

    @Test
    fun `a warning without readable areas is shown for every spot`() {
        val cyclone = feed.last().copy(
            titleEn = "Tropical Cyclone Advisory",
            headingEn = "TROPICAL CYCLONE ADVISORY",
            textEn = "Tropical storm observed at 12.0N 115.0E moving west.",
            textBm = "Ribut tropika dicerap pada 12.0U 115.0T bergerak ke barat.",
            validTo = at("2026-09-26T00:00")
        )
        val s = summarizeWarnings(listOf(cyclone), CoastalArea.SEMPORNA.latitude, CoastalArea.SEMPORNA.longitude, morning)
        assertEquals("TROPICAL CYCLONE ADVISORY", s.forSpot.single().headingEn)
    }
}
