package com.fishingcopilot.alerts

import com.fishingcopilot.bite.ScorePoint
import com.fishingcopilot.bite.ScoreWindow
import com.fishingcopilot.bite.primeWindows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoldenAlertPlannerTest {
    private val min = 60_000L
    private val t0 = 1_790_300_000_000L

    @Test
    fun `finds every run at or above the threshold`() {
        val scores = listOf(5.0, 7.6, 8.0, 6.0, 6.5, 7.5, 9.0, 7.7, 4.0)
        val points = scores.mapIndexed { i, s -> ScorePoint(t0 + i * 15 * min, s) }
        val windows = primeWindows(points, 7.5)
        assertEquals(2, windows.size)
        assertEquals(ScoreWindow(t0 + 15 * min, t0 + 30 * min, 8.0, t0 + 30 * min), windows[0])
        assertEquals(ScoreWindow(t0 + 75 * min, t0 + 105 * min, 9.0, t0 + 90 * min), windows[1])
    }

    private val morning = ScoreWindow(start = t0 + 120 * min, end = t0 + 210 * min, peak = 8.9, peakAt = t0 + 150 * min)
    private val evening = ScoreWindow(start = t0 + 720 * min, end = t0 + 800 * min, peak = 8.1, peakAt = t0 + 760 * min)

    @Test
    fun `next alert is 45 minutes before the first window not yet announced`() {
        val plan = GoldenAlertPlanner.next(listOf(morning, evening), now = t0, lastNotifiedStart = null)!!
        assertEquals(morning.start - 45 * min, plan.alertAt)
        assertEquals(morning, plan.window)

        val afterMorning = GoldenAlertPlanner.next(listOf(morning, evening), now = t0, lastNotifiedStart = morning.start)!!
        assertEquals(evening, afterMorning.window)
    }

    @Test
    fun `a window starting too soon to warn is skipped rather than announced late`() {
        val plan = GoldenAlertPlanner.next(listOf(morning, evening), now = morning.start - 30 * min, lastNotifiedStart = null)!!
        assertEquals(evening, plan.window)
        assertNull(GoldenAlertPlanner.next(emptyList(), t0, null))
    }

    @Test
    fun `when the alarm fires, only an unannounced window starting 15 to 75 minutes ahead is due`() {
        assertEquals(morning, GoldenAlertPlanner.due(listOf(morning), now = morning.start - 45 * min, lastNotifiedStart = null))
        assertEquals(morning, GoldenAlertPlanner.due(listOf(morning), now = morning.start - 70 * min, lastNotifiedStart = null))
        assertNull(GoldenAlertPlanner.due(listOf(morning), now = morning.start - 10 * min, lastNotifiedStart = null))
        assertNull(GoldenAlertPlanner.due(listOf(morning), now = morning.start - 45 * min, lastNotifiedStart = morning.start))
        // The forecast moved the window by more than the tolerance: nothing to announce this time.
        assertNull(GoldenAlertPlanner.due(listOf(evening), now = morning.start - 45 * min, lastNotifiedStart = null))
    }

    @Test
    fun `channel follows the sound and vibration switches`() {
        assertEquals(AlertChannel.FULL, AlertChannel.of(sound = true, vibrate = true))
        assertEquals(AlertChannel.SOUND, AlertChannel.of(sound = true, vibrate = false))
        assertEquals(AlertChannel.VIBRATE, AlertChannel.of(sound = false, vibrate = true))
        assertEquals(AlertChannel.SILENT, AlertChannel.of(sound = false, vibrate = false))
    }
}
