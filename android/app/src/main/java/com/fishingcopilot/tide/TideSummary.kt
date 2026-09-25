package com.fishingcopilot.tide

/** What the home tide card shows. Heights are metres relative to the model's mean level. */
data class TideSummary(
    val heightNow: Double,
    val rising: Boolean,
    val nextHigh: TideEvent?,
    val nextLow: TideEvent?,
    val curve: List<SeaLevelSample>,
    /** Within 30 minutes of high or low water, the same slack rule the Bite Score uses. */
    val slack: Boolean
)

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val CURVE_STEP_MS = 10 * MINUTE_MS
private const val LOOK_AHEAD_MS = 30 * HOUR_MS
private const val SLACK_MS = 30 * MINUTE_MS

/**
 * [offsetMinutes] delays (positive) or advances (negative) the whole prediction, for spots such as
 * estuaries that turn later than the open sea the model was fitted to.
 */
fun TideModel.summarize(now: Long, offsetMinutes: Int): TideSummary {
    val shifted = copy(epochMillis = epochMillis + offsetMinutes * MINUTE_MS)
    fun relative(t: Long) = shifted.heightAt(t) - meanLevel

    val upcoming = shifted.events(now + MINUTE_MS, now + LOOK_AHEAD_MS)
        .map { it.copy(height = it.height - meanLevel) }
    val curveStart = now - 2 * HOUR_MS
    val curve = (0..(24 * HOUR_MS / CURVE_STEP_MS).toInt()).map { i ->
        val t = curveStart + i * CURVE_STEP_MS
        SeaLevelSample(t, relative(t))
    }
    return TideSummary(
        heightNow = relative(now),
        rising = relative(now + MINUTE_MS) > relative(now),
        nextHigh = upcoming.firstOrNull { it.type == TideEventType.HIGH },
        nextLow = upcoming.firstOrNull { it.type == TideEventType.LOW },
        curve = curve,
        slack = shifted.events(now - SLACK_MS, now + SLACK_MS).isNotEmpty()
    )
}
