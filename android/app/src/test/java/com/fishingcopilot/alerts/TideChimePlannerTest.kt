package com.fishingcopilot.alerts

import com.fishingcopilot.tide.TideEvent
import com.fishingcopilot.tide.TideEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TideChimePlannerTest {
    private val min = 60_000L
    private val now = 1_790_308_800_000L
    private val low = TideEvent(now + 60 * min, TideEventType.LOW, -0.5)
    private val high = TideEvent(now + 7 * 60 * min, TideEventType.HIGH, 0.6)

    @Test
    fun `plans 15 minutes before the next turn`() {
        val plan = TideChimePlanner.next(listOf(high, low), now, lastNotified = null)!!
        assertEquals(low, plan.event)
        assertEquals(low.epochMillis - 15 * min, plan.alertAt)
    }

    @Test
    fun `skips a turn already chimed and one whose chime time has passed`() {
        assertEquals(high, TideChimePlanner.next(listOf(low, high), now, lastNotified = low.epochMillis)!!.event)
        assertEquals(high, TideChimePlanner.next(listOf(low, high), low.epochMillis - 10 * min, lastNotified = null)!!.event)
    }

    @Test
    fun `a late alarm still rings for a turn up to 25 minutes away, never twice`() {
        assertEquals(low, TideChimePlanner.due(listOf(low, high), low.epochMillis - 8 * min, lastNotified = null))
        assertNull(TideChimePlanner.due(listOf(low, high), low.epochMillis - 8 * min, lastNotified = low.epochMillis))
        assertNull(TideChimePlanner.due(listOf(low, high), low.epochMillis - 40 * min, lastNotified = null))
    }
}
