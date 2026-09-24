package com.fishingcopilot.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {
    @Test
    fun `hours map to Malaysian day periods at their boundaries`() {
        val expected = mapOf(
            0 to DayPeriod.NIGHT,
            4 to DayPeriod.NIGHT,
            5 to DayPeriod.MORNING,
            11 to DayPeriod.MORNING,
            12 to DayPeriod.MIDDAY,
            13 to DayPeriod.MIDDAY,
            14 to DayPeriod.AFTERNOON,
            18 to DayPeriod.AFTERNOON,
            19 to DayPeriod.NIGHT
        )
        expected.forEach { (hour, period) ->
            assertEquals("hour $hour", period, DayPeriod.fromHour(hour))
        }
    }
}
