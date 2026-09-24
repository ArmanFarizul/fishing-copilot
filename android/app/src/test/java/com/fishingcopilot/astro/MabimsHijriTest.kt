package com.fishingcopilot.astro

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** First days of Hijri months in JAKIM's 2026 takwim (e-solat, fetched 2026-09-25). */
class MabimsHijriTest {
    private val jakimMonthStarts = mapOf(
        "2026-01-20" to HijriDate(1447, 8, 1),
        "2026-02-19" to HijriDate(1447, 9, 1),
        "2026-03-21" to HijriDate(1447, 10, 1),
        "2026-04-19" to HijriDate(1447, 11, 1),
        "2026-05-18" to HijriDate(1447, 12, 1),
        "2026-06-17" to HijriDate(1448, 1, 1),
        "2026-07-16" to HijriDate(1448, 2, 1),
        "2026-08-14" to HijriDate(1448, 3, 1),
        "2026-09-13" to HijriDate(1448, 4, 1),
        "2026-10-12" to HijriDate(1448, 5, 1),
        "2026-11-11" to HijriDate(1448, 6, 1),
        "2026-12-11" to HijriDate(1448, 7, 1)
    )

    @Test
    fun `matches every JAKIM month start in 2026 and the day before it`() {
        val mismatches = jakimMonthStarts.flatMap { (iso, expected) ->
            val date = LocalDate.parse(iso)
            val start = MabimsHijri.date(date)
            val before = MabimsHijri.date(date.minusDays(1))
            listOfNotNull(
                "$iso: expected $expected got $start".takeIf { start != expected },
                "$iso-1: expected end of previous month got $before".takeIf { before.day !in 29..30 || before.month == expected.month }
            )
        }
        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun `counts days within a month`() {
        assertEquals(HijriDate(1448, 4, 13), MabimsHijri.date(LocalDate.of(2026, 9, 25)))
    }
}
