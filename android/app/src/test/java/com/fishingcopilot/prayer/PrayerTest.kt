package com.fishingcopilot.prayer

import com.fishingcopilot.data.remote.JakimTakwimClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrayerTest {
    private val body = javaClass.getResource("/esolat_month_TRG01_2026-09.json")!!.readText()

    @Test
    fun `reads a JAKIM month`() = runTest {
        val days = JakimTakwimClient { body }.prayerMonth("TRG01")
        assertEquals(30, days.size)
        val first = days.first()
        assertEquals(LocalDate.of(2026, 9, 1), first.date)
        assertEquals(LocalTime.of(5, 52), first.times[Prayer.FAJR])
        assertEquals(LocalTime.of(19, 15), first.times[Prayer.MAGHRIB])
    }

    @Test
    fun `next prayer skips syuruk and rolls over to tomorrow`() = runTest {
        val days = JakimTakwimClient { body }.prayerMonth("TRG01")
        val morning = nextPrayer(days, LocalDateTime.of(2026, 9, 25, 6, 0))!!
        assertEquals(Prayer.DHUHR, morning.prayer)
        val lateNight = nextPrayer(days, LocalDateTime.of(2026, 9, 25, 22, 0))!!
        assertEquals(Prayer.FAJR, lateNight.prayer)
        assertEquals(LocalDate.of(2026, 9, 26), lateNight.at.toLocalDate())
    }
}
