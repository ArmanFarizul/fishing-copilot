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
                "THUNDERSTORMS WARNING", // coastal, names Terengganu, so it leads as the spot's own
                "THUNDERSTORMS WARNING", // land, issued last so first of the rest
                "LOW HORIZONTAL VISIBILITY WARNING",
                "WARNING ON STRONG WINDS AND ROUGH SEAS (FIRST CATEGORY)",
                "THUNDERSTORMS WARNING" // shipping areas
            ),
            all.map { it.headingEn }
        )
    }

    @Test
    fun `Terengganu gets only the coastal thunderstorm warning that names it`() {
        val s = summary(CoastalArea.KUALA_TERENGGANU)
        val warning = s.forSpot.single()
        assertEquals("AMARAN RIBUT PETIR", warning.headingBm)
        assertTrue(warning.textEn.startsWith("Thunderstorms, heavy rain and strong winds are expected over the waters of Perak"))
        assertTrue(warning.textBm.startsWith("Ribut petir, hujan lebat"))
        assertNull(warning.instructionEn)
        assertEquals(4, s.elsewhere.size)
    }

    @Test
    fun `land and coastal warnings both reach a Selangor spot`() {
        val s = summary(CoastalArea.PELABUHAN_KLANG)
        assertEquals(2, s.forSpot.size)
        assertTrue(s.forSpot[0].textEn.contains("Klang and Kuala Langat"))
    }

    @Test
    fun `Sarawak also gets the low visibility warning`() {
        assertEquals(3, summary(CoastalArea.MIRI).forSpot.size)
    }

    @Test
    fun `west Johor is named but east Johor is not`() {
        assertEquals(1, summary(CoastalArea.KUKUP).forSpot.size)
        assertEquals(0, summary(CoastalArea.DESARU).forSpot.size)
    }

    @Test
    fun `western Sabah and Labuan match, eastern Sabah does not`() {
        assertEquals(1, summary(CoastalArea.KOTA_KINABALU).forSpot.size)
        assertEquals(1, summary(CoastalArea.LABUAN).forSpot.size)
        assertEquals(0, summary(CoastalArea.SANDAKAN).forSpot.size)
    }

    @Test
    fun `Straits of Melaka shipping area does not count as the state of Melaka`() {
        val s = summary(CoastalArea.MELAKA)
        assertEquals(listOf("THUNDERSTORMS WARNING"), s.forSpot.map { it.headingEn })
        assertTrue(s.forSpot.single().textEn.contains("West Johor"))
    }

    @Test
    fun `expired warnings drop out`() {
        val afterStorm = summary(CoastalArea.PELABUHAN_KLANG, now = at("2026-09-25T10:00"))
        // The land warning ended at 09:00; the bulletin stays while one of its windows (27 to 28 September) is open.
        assertEquals(1, afterStorm.forSpot.size)
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
