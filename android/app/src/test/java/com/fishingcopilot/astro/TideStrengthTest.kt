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

    @Test
    fun `Hijri day bands match the spec exactly`() {
        val spring = setOf(1, 2, 3, 14, 15, 16)
        val neap = setOf(7, 8, 9, 21, 22, 23)
        (1..30).forEach { day ->
            val expected = when (day) {
                in spring -> TideStrength.SPRING
                in neap -> TideStrength.NEAP
                else -> TideStrength.NORMAL
            }
            assertEquals("Hijri day $day", expected, TideStrength.ofHijriDay(day))
        }
    }
}
