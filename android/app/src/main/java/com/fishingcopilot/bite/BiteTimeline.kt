package com.fishingcopilot.bite

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.TideStrength
import com.fishingcopilot.marine.MarineHour
import com.fishingcopilot.marine.PressureTrend
import com.fishingcopilot.tide.TideModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Everything the score needs; any of it may be missing while data is still downloading. */
data class BiteInputs(
    val tideModel: TideModel?,
    val tideOffsetMinutes: Int,
    val marineHours: List<MarineHour>,
    val hijriByDate: Map<LocalDate, HijriDate>,
    val sunByDate: Map<LocalDate, SunWindow>,
    val zone: ZoneId
)

data class BiteMoment(
    val epochMillis: Long,
    val score: BiteScore,
    val tide: TideFactor?,
    val light: LightFactor?,
    val tideStrength: TideStrength?,
    val pressureHpa: Double?,
    val pressureTrend: PressureTrend?
)

data class BiteForecast(val now: BiteMoment, val points: List<ScorePoint>, val best: ScoreWindow?)

object BiteTimeline {
    /** Scores at or above this are the spec's "golden time" (Waktu Emas). */
    const val PRIME_THRESHOLD = 7.5

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val STEP_MS = 15 * MINUTE_MS

    /** The score now, plus every 15 minutes for the next 24 hours and the best prime window in that span. */
    fun forecast(now: Long, inputs: BiteInputs): BiteForecast {
        val shifted = inputs.tideModel?.let { it.copy(epochMillis = it.epochMillis + inputs.tideOffsetMinutes * MINUTE_MS) }
        val events = shifted?.events(now - 3 * HOUR_MS, now + 27 * HOUR_MS)
        val firstStep = now - now % STEP_MS + STEP_MS
        val points = (0 until 24 * 4).map { i ->
            val t = firstStep + i * STEP_MS
            ScorePoint(t, moment(t, inputs, events).score.score)
        }
        val current = moment(now, inputs, events)
        return BiteForecast(current, listOf(ScorePoint(now, current.score.score)) + points, bestWindow(points, PRIME_THRESHOLD))
    }

    private fun moment(t: Long, inputs: BiteInputs, events: List<com.fishingcopilot.tide.TideEvent>?): BiteMoment {
        val time = Instant.ofEpochMilli(t).atZone(inputs.zone)
        val tide = events?.let { TideFactor.at(t, it) }
        val light = inputs.sunByDate[time.toLocalDate()]?.let { LightFactor.at(time, it) }
        val strength = inputs.hijriByDate[time.toLocalDate()]?.let { TideStrength.ofHijriDay(it.day) }

        val hourStart = t - Math.floorMod(t, HOUR_MS)
        val pressure = inputs.marineHours.firstOrNull { it.epochMillis == hourStart }?.pressureHpa
        val before = inputs.marineHours.firstOrNull { it.epochMillis == hourStart - 3 * HOUR_MS }?.pressureHpa
        val trend = if (pressure != null && before != null) PressureTrend.of(pressure - before) else null

        val score = BiteScore.combine(
            tide = tide?.value,
            light = light?.value,
            solunar = strength?.let(SolunarFactor::of),
            baro = if (pressure != null && trend != null) BaroFactor.of(pressure, trend) else null
        )
        return BiteMoment(t, score, tide, light, strength, pressure, trend)
    }
}
