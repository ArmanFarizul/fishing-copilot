package com.fishingcopilot.astro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Expected values from the US Naval Observatory API (aa.usno.navy.mil/api/rstt/oneday and
 * /api/moon/phases), fetched 2026-09-25, local time UTC+8.
 */
class AstroTest {
    private val myt = ZoneId.of("Asia/Kuala_Lumpur")

    private fun assertAt(label: String, expected: String, actual: ZonedDateTime?, toleranceMinutes: Long) {
        assertNotNull("$label missing", actual)
        val diff = Duration.between(LocalTime.parse(expected), actual!!.toLocalTime()).toMinutes()
        assertTrue("$label expected $expected got ${actual.toLocalTime()} (diff $diff min)", kotlin.math.abs(diff) <= toleranceMinutes)
    }

    @Test
    fun `Kukup 2026-09-25 sun and moon match USNO`() {
        val day = Astro.day(1.325, 103.44, LocalDate.of(2026, 9, 25), myt)
        assertAt("civil dawn", "06:34", day.civilDawn, 2)
        assertAt("sunrise", "06:55", day.sunrise, 2)
        assertAt("sunset", "19:01", day.sunset, 2)
        assertAt("civil dusk", "19:22", day.civilDusk, 2)
        assertAt("moonset", "05:34", day.moonset, 3)
        assertAt("moonrise", "17:58", day.moonrise, 3)
        assertEquals(MoonPhaseName.WAXING_GIBBOUS, day.phase)
        assertTrue("illumination ${day.illumination}", day.illumination in 0.93..0.99)
    }

    @Test
    fun `Penang and Kota Kinabalu moon transits match USNO`() {
        val penang = Astro.day(5.42, 100.36, LocalDate.of(2026, 10, 10), myt)
        assertAt("Penang moonrise", "06:31", penang.moonrise, 3)
        assertAt("Penang transit", "12:39", penang.moonUpperTransit, 5)
        assertAt("Penang moonset", "18:46", penang.moonset, 3)
        assertAt("Penang sunrise", "07:05", penang.sunrise, 2)

        val kk = Astro.day(5.99, 116.03, LocalDate.of(2026, 12, 15), myt)
        assertAt("KK moonrise", "10:50", kk.moonrise, 3)
        assertAt("KK transit", "16:55", kk.moonUpperTransit, 5)
        assertAt("KK moonset", "23:01", kk.moonset, 3)
        assertEquals(MoonPhaseName.WAXING_CRESCENT, kk.phase)
    }

    @Test
    fun `principal phases name only the local day they happen on`() {
        // USNO: new moon 2026-10-10 23:50 MYT, full moon 2026-09-27 00:49 MYT.
        assertEquals(MoonPhaseName.NEW_MOON, Astro.day(5.42, 100.36, LocalDate.of(2026, 10, 10), myt).phase)
        assertEquals(MoonPhaseName.WAXING_CRESCENT, Astro.day(5.42, 100.36, LocalDate.of(2026, 10, 11), myt).phase)
        assertEquals(MoonPhaseName.FULL_MOON, Astro.day(1.325, 103.44, LocalDate.of(2026, 9, 27), myt).phase)
        assertEquals(MoonPhaseName.WAXING_GIBBOUS, Astro.day(1.325, 103.44, LocalDate.of(2026, 9, 26), myt).phase)
    }

    @Test
    fun `moon age counts from the last new moon`() {
        // USNO new moon 2026-10-10 15:50 UT = 23:50 MYT.
        val dayAfter = Astro.day(1.325, 103.44, LocalDate.of(2026, 10, 11), myt)
        assertAt("Kukup transit", "13:12", dayAfter.moonUpperTransit, 5)
        val expectedAge = Duration.between(
            ZonedDateTime.of(2026, 10, 10, 23, 50, 0, 0, myt),
            ZonedDateTime.of(2026, 10, 11, 12, 0, 0, 0, myt)
        ).toMinutes() / 1440.0
        assertEquals(expectedAge, dayAfter.moonAgeDays, 10 / 1440.0)
    }

    @Test
    fun `solunar majors sit on transits and minors on rise and set`() {
        val day = Astro.day(5.42, 100.36, LocalDate.of(2026, 10, 10), myt)
        val majors = day.solunar.filter { it.type == SolunarType.MAJOR }
        val minors = day.solunar.filter { it.type == SolunarType.MINOR }

        val upper = majors.single { it.center == day.moonUpperTransit }
        assertEquals(Duration.ofHours(2), Duration.between(upper.start, upper.end))
        assertEquals(Duration.ofHours(1), Duration.between(upper.start, upper.center))
        assertTrue(minors.any { it.center == day.moonrise && Duration.between(it.start, it.end) == Duration.ofHours(1) })
        assertTrue(minors.any { it.center == day.moonset })
        assertTrue(day.solunar.zipWithNext().all { (a, b) -> !a.center.isAfter(b.center) })
    }

    @Test
    fun `a day without moonrise has no rise-based minor period`() {
        // Moonrise drifts ~50 min later each day, so some local days have none; the model must allow null.
        val days = (0L until 30L).map { Astro.day(1.325, 103.44, LocalDate.of(2026, 10, 1).plusDays(it), myt) }
        val withoutRise = days.firstOrNull { it.moonrise == null }
        assertNotNull("expected at least one day in a month without a moonrise", withoutRise)
        assertNull(withoutRise!!.moonrise)
        assertEquals(
            listOfNotNull(withoutRise.moonrise, withoutRise.moonset).size,
            withoutRise.solunar.count { it.type == SolunarType.MINOR }
        )
    }
}
