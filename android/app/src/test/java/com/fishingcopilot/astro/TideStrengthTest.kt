package com.fishingcopilot.astro

import org.junit.Assert.assertEquals
import org.junit.Test

class TideStrengthTest {
    @Test
    fun `moon age bands follow the spec's Hijri days`() {
        mapOf(
            0.0 to TideStrength.SPRING, 2.9 to TideStrength.SPRING, 3.0 to TideStrength.NORMAL,
            6.0 to TideStrength.NEAP, 8.9 to TideStrength.NEAP, 9.0 to TideStrength.NORMAL,
            13.0 to TideStrength.SPRING, 15.9 to TideStrength.SPRING, 16.0 to TideStrength.NORMAL,
            20.0 to TideStrength.NEAP, 22.9 to TideStrength.NEAP, 23.0 to TideStrength.NORMAL,
            28.9 to TideStrength.NORMAL, 29.2 to TideStrength.SPRING
        ).forEach { (age, expected) -> assertEquals("age $age", expected, TideStrength.of(age)) }
    }
}
