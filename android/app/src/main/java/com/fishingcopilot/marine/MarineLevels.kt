package com.fishingcopilot.marine

/** WMO sea state (code table 3700, from the Douglas scale); [maxHeight] is the band's upper wave height in metres. */
enum class SeaState(val code: Int, val maxHeight: Double) {
    CALM_GLASSY(0, 0.0), CALM_RIPPLED(1, 0.1), SMOOTH(2, 0.5), SLIGHT(3, 1.25), MODERATE(4, 2.5),
    ROUGH(5, 4.0), VERY_ROUGH(6, 6.0), HIGH(7, 9.0), VERY_HIGH(8, 14.0), PHENOMENAL(9, Double.MAX_VALUE);

    companion object {
        fun of(waveHeight: Double): SeaState = entries.first { waveHeight <= it.maxHeight }
    }
}

/**
 * Beaufort force from wind speed in knots (Met Office table). The table lists whole knots,
 * so each band runs to half a knot below the next one's first value.
 */
enum class Beaufort(val force: Int, val belowKnots: Double) {
    CALM(0, 1.0), LIGHT_AIR(1, 3.5), LIGHT_BREEZE(2, 6.5), GENTLE_BREEZE(3, 10.5), MODERATE_BREEZE(4, 16.5),
    FRESH_BREEZE(5, 21.5), STRONG_BREEZE(6, 27.5), NEAR_GALE(7, 33.5), GALE(8, 40.5), STRONG_GALE(9, 47.5),
    STORM(10, 55.5), VIOLENT_STORM(11, 63.5), HURRICANE(12, Double.MAX_VALUE);

    companion object {
        fun of(knots: Double): Beaufort = entries.first { knots < it.belowKnots }
    }
}

/** Rough comparison of wave height to an adult standing at the water's edge. */
enum class WaveBody(val below: Double) {
    FLAT(0.1), ANKLE(0.3), KNEE(0.6), WAIST(1.0), CHEST(1.4), HEAD(1.9), OVERHEAD(Double.MAX_VALUE);

    companion object {
        fun of(waveHeight: Double): WaveBody = entries.first { waveHeight < it.below }
    }
}

/** Product spec tackle advisor bands: slow < 0.5 kn, moderate 0.5-1.2 kn, fast above. */
enum class CurrentLevel {
    SLOW, MODERATE, FAST;

    companion object {
        fun of(knots: Double): CurrentLevel = when {
            knots < 0.5 -> SLOW
            knots <= 1.2 -> MODERATE
            else -> FAST
        }
    }
}
