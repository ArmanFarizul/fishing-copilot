package com.fishingcopilot.bite

import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.tide.TideEvent
import java.time.ZonedDateTime

/*
 * Bite Score from the product spec (docs/spesifikasi.md, section 5):
 *   raw = 0.35 tide + 0.25 light + 0.25 solunar + 0.15 barometer,  score = 1 + 9 * raw.
 * Where the spec leaves a case undefined, the value chosen here is listed in docs/bite-score.md.
 */

private const val MINUTE_MS = 60_000L

/** Water movement relative to the nearest high or low water. */
enum class TideFactor(val value: Double) {
    /** Within 30 minutes of a turn: slack water. */
    SLACK(0.2),
    /** 45-150 minutes after a turn: the water starts to run. */
    MOVING(1.0),
    /** Everything else, including mid-cycle when the current is strongest. */
    MID_CYCLE(0.6);

    companion object {
        fun at(time: Long, events: List<TideEvent>): TideFactor {
            val nearest = events.minOfOrNull { kotlin.math.abs(it.epochMillis - time) } ?: return MID_CYCLE
            if (nearest <= 30 * MINUTE_MS) return SLACK
            val sinceLast = events.filter { it.epochMillis <= time }.minOfOrNull { time - it.epochMillis }
            return if (sinceLast != null && sinceLast in 45 * MINUTE_MS..150 * MINUTE_MS) MOVING else MID_CYCLE
        }
    }
}

/** Civil twilight and sun times for the day being scored. */
data class SunWindow(
    val civilDawn: ZonedDateTime?,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val civilDusk: ZonedDateTime?
)

enum class LightFactor(val value: Double) {
    /** Dawn (first light to an hour after sunrise) and dusk (an hour before sunset to last light). */
    GOLDEN(1.0),
    NIGHT(0.6),
    /** 11:00-15:00. */
    MIDDAY(0.3),
    /** Other daylight hours; not defined by the spec, treated like night. */
    DAYLIGHT(0.6);

    companion object {
        fun at(time: ZonedDateTime, sun: SunWindow): LightFactor {
            val dawnStart = sun.civilDawn ?: sun.sunrise?.minusMinutes(20)
            val dawnEnd = sun.sunrise?.plusHours(1)
            val duskStart = sun.sunset?.minusHours(1)
            val duskEnd = sun.civilDusk ?: sun.sunset?.plusMinutes(20)
            fun inside(start: ZonedDateTime?, end: ZonedDateTime?) =
                start != null && end != null && !time.isBefore(start) && time.isBefore(end)
            return when {
                inside(dawnStart, dawnEnd) || inside(duskStart, duskEnd) -> GOLDEN
                sun.sunrise == null || sun.sunset == null -> NIGHT
                time.isBefore(sun.sunrise) || !time.isBefore(sun.sunset) -> NIGHT
                time.hour in 11..14 -> MIDDAY
                else -> DAYLIGHT
            }
        }
    }
}

/** Spec: new/full moon spring days 1.0, quarter-moon neap days 0.3, other crescent/gibbous days 0.7. */
object SolunarFactor {
    fun of(strength: TideStrength): Double = when (strength) {
        TideStrength.SPRING -> 1.0
        TideStrength.NEAP -> 0.3
        TideStrength.NORMAL -> 0.7
    }
}

object BaroFactor {
    fun of(hPa: Double, trend: PressureTrend): Double = when (trend) {
        PressureTrend.STEADY -> if (hPa in 1010.0..1015.0) 1.0 else 0.8
        PressureTrend.RISING -> 0.7
        PressureTrend.FALLING -> 0.5
        PressureTrend.FALLING_FAST -> 0.2
    }
}

data class BiteScore(
    val score: Double,
    val tide: Double?,
    val light: Double?,
    val solunar: Double?,
    val baro: Double?
) {
    companion object {
        const val TIDE_WEIGHT = 0.35
        const val LIGHT_WEIGHT = 0.25
        const val SOLUNAR_WEIGHT = 0.25
        const val BARO_WEIGHT = 0.15

        /** A factor with no data (e.g. no pressure while offline) is dropped and the remaining weights rescaled. */
        fun combine(tide: Double?, light: Double?, solunar: Double?, baro: Double?): BiteScore {
            val parts = listOfNotNull(
                tide?.let { it to TIDE_WEIGHT },
                light?.let { it to LIGHT_WEIGHT },
                solunar?.let { it to SOLUNAR_WEIGHT },
                baro?.let { it to BARO_WEIGHT }
            )
            val weight = parts.sumOf { it.second }
            val raw = if (weight == 0.0) 0.0 else parts.sumOf { (value, w) -> value * w } / weight
            return BiteScore(1 + 9 * raw, tide, light, solunar, baro)
        }
    }
}

data class ScorePoint(val epochMillis: Long, val score: Double)

data class ScoreWindow(val start: Long, val end: Long, val peak: Double, val peakAt: Long)

/** The run of points at or above [threshold] that contains the highest score, or null if none reach it. */
fun bestWindow(points: List<ScorePoint>, threshold: Double): ScoreWindow? {
    val peakIndex = points.indices.maxByOrNull { points[it].score } ?: return null
    if (points[peakIndex].score < threshold) return null
    var first = peakIndex
    while (first > 0 && points[first - 1].score >= threshold) first--
    var last = peakIndex
    while (last < points.lastIndex && points[last + 1].score >= threshold) last++
    return ScoreWindow(points[first].epochMillis, points[last].epochMillis, points[peakIndex].score, points[peakIndex].epochMillis)
}

/** Every separate run of points at or above [threshold], in time order. */
fun primeWindows(points: List<ScorePoint>, threshold: Double): List<ScoreWindow> {
    val windows = mutableListOf<ScoreWindow>()
    var runStart = -1
    for (i in 0..points.size) {
        val inside = i < points.size && points[i].score >= threshold
        if (inside && runStart < 0) runStart = i
        if (!inside && runStart >= 0) {
            val run = points.subList(runStart, i)
            val peak = run.maxBy { it.score }
            windows += ScoreWindow(run.first().epochMillis, run.last().epochMillis, peak.score, peak.epochMillis)
            runStart = -1
        }
    }
    return windows
}
