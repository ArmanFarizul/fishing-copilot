package com.fishingcopilot.astro

import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunTimes
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class MoonPhaseName {
    NEW_MOON, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS, FULL_MOON, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT
}

enum class SolunarType { MAJOR, MINOR }

data class SolunarPeriod(val type: SolunarType, val start: ZonedDateTime, val center: ZonedDateTime, val end: ZonedDateTime)

/** The moon at local noon, for the moon calendar strip. */
data class MoonDay(val date: LocalDate, val illumination: Double, val phase: MoonPhaseName, val moonAgeDays: Double) {
    val waxing: Boolean get() = moonAgeDays < 29.530588853 / 2
}

/** Sun, moon and solunar periods for one local calendar day. Any event may be null on a day it does not occur. */
data class AstroDay(
    val civilDawn: ZonedDateTime?,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val civilDusk: ZonedDateTime?,
    val moonrise: ZonedDateTime?,
    val moonset: ZonedDateTime?,
    val moonUpperTransit: ZonedDateTime?,
    val moonLowerTransit: ZonedDateTime?,
    /** Illuminated fraction of the disc, 0..1, at local noon. */
    val illumination: Double,
    val phase: MoonPhaseName,
    /** Days since the last new moon, at local noon; spring tides fall near 0 and ~14.8. */
    val moonAgeDays: Double,
    val solunar: List<SolunarPeriod>
)

/**
 * Offline sun and moon calculations (commons-suncalc, a Java port of SunCalc), checked against
 * US Naval Observatory data in AstroTest. commons-suncalc has no moon transit, so transits are
 * found here from the moon's altitude.
 *
 * Solunar periods follow the usual convention: major = moon overhead or underfoot +/- 1 h,
 * minor = moonrise or moonset +/- 30 min.
 */
object Astro {
    private val MAJOR_HALF = Duration.ofHours(1)
    private val MINOR_HALF = Duration.ofMinutes(30)

    fun day(latitude: Double, longitude: Double, date: LocalDate, zone: ZoneId): AstroDay {
        val start = date.atStartOfDay(zone)
        val noon = start.plusHours(12)

        val sun = SunTimes.compute().on(start).timezone(zone).at(latitude, longitude).oneDay().execute()
        val civil = SunTimes.compute().on(start).timezone(zone).at(latitude, longitude).oneDay()
            .twilight(SunTimes.Twilight.CIVIL).execute()
        val moon = MoonTimes.compute().on(start).timezone(zone).at(latitude, longitude).oneDay().execute()
        val (upper, lower) = transits(latitude, longitude, start)
        val illumination = MoonIllumination.compute().on(noon).at(latitude, longitude).execute()

        val periods = listOfNotNull(
            upper?.let { period(SolunarType.MAJOR, it, MAJOR_HALF) },
            lower?.let { period(SolunarType.MAJOR, it, MAJOR_HALF) },
            moon.rise?.let { period(SolunarType.MINOR, it, MINOR_HALF) },
            moon.set?.let { period(SolunarType.MINOR, it, MINOR_HALF) }
        ).sortedBy { it.center }

        return AstroDay(
            civilDawn = civil.rise,
            sunrise = sun.rise,
            sunset = sun.set,
            civilDusk = civil.set,
            moonrise = moon.rise,
            moonset = moon.set,
            moonUpperTransit = upper,
            moonLowerTransit = lower,
            illumination = illumination.fraction,
            phase = phaseName(start, noon, latitude, longitude),
            moonAgeDays = moonAgeDays(noon),
            solunar = periods
        )
    }

    /** Just the sun times for a day: first light, sunrise, sunset and last light (civil twilight). */
    fun sun(latitude: Double, longitude: Double, date: LocalDate, zone: ZoneId): List<ZonedDateTime?> {
        val start = date.atStartOfDay(zone)
        val sun = SunTimes.compute().on(start).timezone(zone).at(latitude, longitude).oneDay().execute()
        val civil = SunTimes.compute().on(start).timezone(zone).at(latitude, longitude).oneDay()
            .twilight(SunTimes.Twilight.CIVIL).execute()
        return listOf(civil.rise, sun.rise, sun.set, civil.set)
    }

    /** Just the moon for a day, without the transit scan, so a month of days stays cheap. */
    fun moon(latitude: Double, longitude: Double, date: LocalDate, zone: ZoneId): MoonDay {
        val start = date.atStartOfDay(zone)
        val noon = start.plusHours(12)
        return MoonDay(
            date = date,
            illumination = MoonIllumination.compute().on(noon).at(latitude, longitude).execute().fraction,
            phase = phaseName(start, noon, latitude, longitude),
            moonAgeDays = moonAgeDays(noon)
        )
    }

    private fun period(type: SolunarType, center: ZonedDateTime, half: Duration) =
        SolunarPeriod(type, center.minus(half), center, center.plus(half))

    /** Upper (highest altitude) and lower (lowest) transit within the day, to the minute. */
    private fun transits(latitude: Double, longitude: Double, start: ZonedDateTime): Pair<ZonedDateTime?, ZonedDateTime?> {
        fun altitude(t: ZonedDateTime) = MoonPosition.compute().on(t).at(latitude, longitude).execute().altitude
        val end = start.plusDays(1)
        var upper: ZonedDateTime? = null
        var lower: ZonedDateTime? = null
        // Coarse 10-minute scan with one step of margin either side, then refine each turning point by minute.
        val coarse = (-1..145).map { start.plusMinutes(10L * it) }.map { it to altitude(it) }
        for (i in 1 until coarse.size - 1) {
            val (_, a) = coarse[i - 1]
            val (t, b) = coarse[i]
            val (_, c) = coarse[i + 1]
            val isMax = b > a && b >= c
            val isMin = b < a && b <= c
            if (!isMax && !isMin) continue
            val refined = (-10..10).map { t.plusMinutes(it.toLong()) }
                .let { window -> if (isMax) window.maxBy(::altitude) else window.minBy(::altitude) }
            if (refined.isBefore(start) || !refined.isBefore(end)) continue
            if (isMax) upper = refined else lower = refined
        }
        return upper to lower
    }

    /**
     * USNO convention: a principal phase (new, first quarter, full, last quarter) names only the local day
     * it happens on; other days get the intermediate name. commons-suncalc's closestPhase would call the
     * days either side of full moon "full" too.
     */
    private fun phaseName(start: ZonedDateTime, noon: ZonedDateTime, latitude: Double, longitude: Double): MoonPhaseName {
        val end = start.plusDays(1)
        val principal = listOf(
            MoonPhase.Phase.NEW_MOON to MoonPhaseName.NEW_MOON,
            MoonPhase.Phase.FIRST_QUARTER to MoonPhaseName.FIRST_QUARTER,
            MoonPhase.Phase.FULL_MOON to MoonPhaseName.FULL_MOON,
            MoonPhase.Phase.LAST_QUARTER to MoonPhaseName.LAST_QUARTER
        ).firstOrNull { (phase, _) ->
            MoonPhase.compute().phase(phase).on(start).execute().time.isBefore(end)
        }
        if (principal != null) return principal.second

        fun fraction(t: ZonedDateTime) = MoonIllumination.compute().on(t).at(latitude, longitude).execute().fraction
        val now = fraction(noon)
        val waxing = fraction(noon.plusHours(1)) > now
        return when {
            waxing && now < 0.5 -> MoonPhaseName.WAXING_CRESCENT
            waxing -> MoonPhaseName.WAXING_GIBBOUS
            now >= 0.5 -> MoonPhaseName.WANING_GIBBOUS
            else -> MoonPhaseName.WANING_CRESCENT
        }
    }

    private fun moonAgeDays(at: ZonedDateTime): Double {
        var newMoon = MoonPhase.compute().phase(MoonPhase.Phase.NEW_MOON).on(at.minusDays(31)).execute().time
        while (true) {
            val next = MoonPhase.compute().phase(MoonPhase.Phase.NEW_MOON).on(newMoon.plusDays(1)).execute().time
            if (next.isAfter(at)) break
            newMoon = next
        }
        return Duration.between(newMoon, at).toMinutes() / 1440.0
    }
}

enum class TideStrength {
    SPRING, NEAP, NORMAL;

    companion object {
        /**
         * Product spec: Hijri days 1-3 and 14-16 are spring tides (air hidup), 7-9 and 21-23 neap (air mati).
         * Mapped onto moon age as Hijri day ~ age + 1; age 29+ is the next new moon, so spring again.
         */
        /** The spec's own definition, used whenever the official Hijri date is known. */
        fun ofHijriDay(day: Int): TideStrength = when (day) {
            in 1..3, in 14..16 -> SPRING
            in 7..9, in 21..23 -> NEAP
            else -> NORMAL
        }

        fun of(moonAgeDays: Double): TideStrength = when {
            moonAgeDays < 3 || moonAgeDays in 13.0..<16.0 || moonAgeDays >= 29 -> SPRING
            moonAgeDays in 6.0..<9.0 || moonAgeDays in 20.0..<23.0 -> NEAP
            else -> NORMAL
        }
    }
}

/** A date in the Islamic (Hijri) calendar; month 1 = Muharram. */
data class HijriDate(val year: Int, val month: Int, val day: Int)
