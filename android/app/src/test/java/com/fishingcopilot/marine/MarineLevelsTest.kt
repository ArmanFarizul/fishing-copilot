package com.fishingcopilot.marine

import org.junit.Assert.assertEquals
import org.junit.Test

class MarineLevelsTest {
    @Test
    fun `sea state follows WMO code 3700 bands`() {
        mapOf(
            0.0 to 0, 0.05 to 1, 0.1 to 1, 0.11 to 2, 0.5 to 2, 0.51 to 3, 1.25 to 3, 1.3 to 4,
            2.5 to 4, 2.6 to 5, 4.0 to 5, 4.1 to 6, 6.0 to 6, 6.1 to 7, 9.0 to 7, 9.1 to 8, 14.0 to 8, 14.1 to 9
        ).forEach { (height, code) -> assertEquals("height $height", code, SeaState.of(height).code) }
    }

    @Test
    fun `Beaufort follows the Met Office knot bands`() {
        mapOf(
            0.4 to 0, 1.0 to 1, 3.0 to 1, 4.0 to 2, 6.0 to 2, 7.0 to 3, 10.0 to 3, 11.0 to 4, 16.0 to 4,
            17.0 to 5, 21.0 to 5, 22.0 to 6, 27.0 to 6, 28.0 to 7, 33.0 to 7, 34.0 to 8, 40.0 to 8,
            41.0 to 9, 47.0 to 9, 48.0 to 10, 55.0 to 10, 56.0 to 11, 63.0 to 11, 64.0 to 12, 90.0 to 12
        ).forEach { (knots, force) -> assertEquals("$knots kn", force, Beaufort.of(knots).force) }
    }

    @Test
    fun `body comparison bands`() {
        mapOf(
            0.05 to WaveBody.FLAT, 0.2 to WaveBody.ANKLE, 0.5 to WaveBody.KNEE, 0.8 to WaveBody.WAIST,
            1.2 to WaveBody.CHEST, 1.6 to WaveBody.HEAD, 2.5 to WaveBody.OVERHEAD
        ).forEach { (height, body) -> assertEquals("height $height", body, WaveBody.of(height)) }
    }

    @Test
    fun `current bands match the spec's tackle advisor`() {
        assertEquals(CurrentLevel.SLOW, CurrentLevel.of(0.49))
        assertEquals(CurrentLevel.MODERATE, CurrentLevel.of(0.5))
        assertEquals(CurrentLevel.MODERATE, CurrentLevel.of(1.2))
        assertEquals(CurrentLevel.FAST, CurrentLevel.of(1.21))
    }
}
