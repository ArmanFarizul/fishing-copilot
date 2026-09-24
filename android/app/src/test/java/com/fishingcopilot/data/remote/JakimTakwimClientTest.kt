package com.fishingcopilot.data.remote

import com.fishingcopilot.astro.HijriDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class JakimTakwimClientTest {
    @Test
    fun `parses Malay month abbreviations and Hijri dates`() = runTest {
        var requested = ""
        val client = JakimTakwimClient { url ->
            requested = url
            """{"prayerTime":[
                {"hijri":"1447-09-01","date":"19-Feb-2026","day":"Thursday"},
                {"hijri":"1447-10-01","date":"21-Mac-2026","day":"Saturday"},
                {"hijri":"1447-12-01","date":"18-Mei-2026","day":"Monday"},
                {"hijri":"1448-03-01","date":"14-Ogos-2026","day":"Friday"},
                {"hijri":"1448-05-01","date":"12-Okt-2026","day":"Monday"},
                {"hijri":"1448-07-21","date":"31-Dis-2026","day":"Thursday"}
            ],"status":"OK!"}"""
        }

        val days = client.currentYear()

        assertEquals(HijriDate(1447, 9, 1), days[LocalDate.of(2026, 2, 19)])
        assertEquals(HijriDate(1447, 10, 1), days[LocalDate.of(2026, 3, 21)])
        assertEquals(HijriDate(1447, 12, 1), days[LocalDate.of(2026, 5, 18)])
        assertEquals(HijriDate(1448, 3, 1), days[LocalDate.of(2026, 8, 14)])
        assertEquals(HijriDate(1448, 5, 1), days[LocalDate.of(2026, 10, 12)])
        assertEquals(HijriDate(1448, 7, 21), days[LocalDate.of(2026, 12, 31)])
        assertTrue("period=year" in requested)
    }

    @Test(expected = IOException::class)
    fun `a non OK status is an error`() = runTest {
        JakimTakwimClient { """{"prayerTime":[],"status":"NO_RECORD!"}""" }.currentYear()
    }
}
