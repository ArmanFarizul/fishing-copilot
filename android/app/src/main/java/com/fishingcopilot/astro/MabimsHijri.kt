package com.fishingcopilot.astro

import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.SunTimes
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToLong

/**
 * Estimates Malaysian Hijri dates with the MABIMS imkanur rukyah criteria Malaysia uses: a month starts
 * the day after the first sunset following conjunction at which the moon is at least 3 degrees high and
 * at least 6.4 degrees from the sun; otherwise the month runs to 30 days.
 *
 * Only a fallback for days JAKIM's takwim does not cover yet; checked against JAKIM's 2026 month starts
 * in MabimsHijriTest.
 */
object MabimsHijri {
    private val zone: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")
    // Reference sighting point on the west coast, where the young moon is easiest to see.
    private const val LATITUDE = 3.14
    private const val LONGITUDE = 101.69
    private const val MIN_ALTITUDE = 3.0
    private const val MIN_ELONGATION = 6.4
    private const val SYNODIC_DAYS = 29.530588853

    // Anchor: JAKIM's 1 Muharram 1448 = 2026-06-17, i.e. the month after the conjunction of 2026-06-15.
    private val anchorConjunction: ZonedDateTime = conjunctionAfter(ZonedDateTime.of(2026, 6, 13, 0, 0, 0, 0, zone))
    private const val ANCHOR_YEAR = 1448

    fun date(date: LocalDate): HijriDate {
        // Walk conjunctions from about two months back and keep the last month start on or before the date.
        var conjunction = conjunctionAfter(date.minusDays(62).atStartOfDay(zone))
        var current: Pair<ZonedDateTime, LocalDate>? = null
        while (true) {
            val start = monthStart(conjunction)
            if (start.isAfter(date)) break
            current = conjunction to start
            conjunction = conjunctionAfter(conjunction.plusDays(1))
        }
        val (monthConjunction, start) = requireNotNull(current)
        val lunations = (Duration.between(anchorConjunction, monthConjunction).toMinutes() / (SYNODIC_DAYS * 1440)).roundToLong()
        return HijriDate(
            year = ANCHOR_YEAR + Math.floorDiv(lunations, 12L).toInt(),
            month = Math.floorMod(lunations, 12L).toInt() + 1,
            day = (date.toEpochDay() - start.toEpochDay()).toInt() + 1
        )
    }

    private fun conjunctionAfter(time: ZonedDateTime): ZonedDateTime =
        MoonPhase.compute().phase(MoonPhase.Phase.NEW_MOON).on(time).execute().time.withZoneSameInstant(zone)

    private fun monthStart(conjunction: ZonedDateTime): LocalDate {
        var day = conjunction.toLocalDate()
        var sunset = sunset(day)
        if (!sunset.isAfter(conjunction)) {
            day = day.plusDays(1)
            sunset = sunset(day)
        }
        val altitude = MoonPosition.compute().on(sunset).at(LATITUDE, LONGITUDE).execute().altitude
        val elongation = MoonIllumination.compute().on(sunset).at(LATITUDE, LONGITUDE).execute().elongation
        val visible = altitude >= MIN_ALTITUDE && elongation >= MIN_ELONGATION
        return day.plusDays(if (visible) 1 else 2)
    }

    private fun sunset(day: LocalDate): ZonedDateTime =
        requireNotNull(SunTimes.compute().on(day.atStartOfDay(zone)).at(LATITUDE, LONGITUDE).oneDay().execute().set)
}
