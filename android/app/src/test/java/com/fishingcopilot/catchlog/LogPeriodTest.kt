package com.fishingcopilot.catchlog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class LogPeriodTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")
    private val today = LocalDate.of(2026, 9, 25)
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `years run from local midnight on 1 January`() {
        val thisYear = LogPeriod.ThisYear.bounds(today, zone)!!
        assertEquals(at("2026-01-01T00:00"), thisYear.first)
        assertEquals(at("2027-01-01T00:00") - 1, thisYear.last)
        assertEquals(at("2025-01-01T00:00"), LogPeriod.LastYear.bounds(today, zone)!!.first)
        assertEquals(at("2021-01-01T00:00"), LogPeriod.Year(2021).bounds(today, zone)!!.first)
        assertNull(LogPeriod.All.bounds(today, zone))
    }

    @Test
    fun `a range includes both days, in either order`() {
        val range = LogPeriod.Range(LocalDate.of(2025, 3, 5), LocalDate.of(2025, 1, 1)).bounds(today, zone)!!
        assertEquals(at("2025-01-01T00:00"), range.first)
        assertEquals(at("2025-03-06T00:00") - 1, range.last)
        // A late-night catch on the last day is inside.
        assert(at("2025-03-05T23:30") in range)
    }

    @Test
    fun `months group consecutive catches, newest first`() {
        val times = listOf("2026-09-20T06:00", "2026-09-02T18:00", "2026-08-31T23:59", "2025-12-01T07:00").map(::at)
        val groups = groupByMonth(times, zone) { it }
        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2025, 12)), groups.map { it.month })
        assertEquals(listOf(2, 1, 1), groups.map { it.items.size })
        assertEquals(emptyList<MonthGroup<Long>>(), groupByMonth(emptyList<Long>(), zone) { it })
    }
}
