package com.fishingcopilot.tide

import kotlin.math.cos

data class SeaLevelSample(val epochMillis: Long, val height: Double)

data class HarmonicConstant(val constituent: Constituent, val amplitude: Double, val phaseDegrees: Double)

enum class TideEventType { HIGH, LOW }

data class TideEvent(val epochMillis: Long, val type: TideEventType, val height: Double)

/**
 * Sea level as mean + sum of A cos(speed * t - phase), with t in hours since [epochMillis].
 * Phases are local to this epoch (not Greenwich), which is enough because a model is only
 * ever used to predict at the spot it was fitted for. Nodal modulation is ignored, so refit
 * from fresh data at least yearly.
 */
data class TideModel(
    val epochMillis: Long,
    val meanLevel: Double,
    val constants: List<HarmonicConstant>
) {
    fun heightAt(epochMillis: Long): Double {
        val hours = (epochMillis - this.epochMillis) / MILLIS_PER_HOUR
        return meanLevel + constants.sumOf {
            it.amplitude * cos(Math.toRadians(it.constituent.speedDegreesPerHour * hours - it.phaseDegrees))
        }
    }

    /** High and low waters in [from, to), located to the nearest minute. */
    fun events(from: Long, to: Long): List<TideEvent> {
        val events = mutableListOf<TideEvent>()
        var previous = heightAt(from - MILLIS_PER_MINUTE)
        var current = heightAt(from)
        var t = from
        while (t < to) {
            val next = heightAt(t + MILLIS_PER_MINUTE)
            if (current > previous && current >= next) events += TideEvent(t, TideEventType.HIGH, current)
            if (current < previous && current <= next) events += TideEvent(t, TideEventType.LOW, current)
            previous = current
            current = next
            t += MILLIS_PER_MINUTE
        }
        return events
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val MILLIS_PER_HOUR = 3_600_000.0
    }
}
