package com.fishingcopilot.alerts

import com.fishingcopilot.tide.TideEvent

data class PlannedChime(val alertAt: Long, val event: TideEvent)

/** The spec's "Tide Change Chime": a soft bell 15 minutes before high or low water, once per turn. */
object TideChimePlanner {
    const val LEAD_MINUTES = 15L
    private const val MINUTE_MS = 60_000L

    // An inexact alarm can be up to ~10 minutes late, so a turn 0-25 minutes away still belongs to this alarm.
    private const val DUE_FROM_MINUTES = 0L
    private const val DUE_TO_MINUTES = 25L

    fun next(events: List<TideEvent>, now: Long, lastNotified: Long?): PlannedChime? =
        events.sortedBy { it.epochMillis }
            .filter { it.epochMillis != lastNotified }
            .map { PlannedChime(it.epochMillis - LEAD_MINUTES * MINUTE_MS, it) }
            .firstOrNull { it.alertAt > now }

    fun due(events: List<TideEvent>, now: Long, lastNotified: Long?): TideEvent? =
        events.sortedBy { it.epochMillis }.firstOrNull {
            it.epochMillis != lastNotified &&
                it.epochMillis - now in DUE_FROM_MINUTES * MINUTE_MS..DUE_TO_MINUTES * MINUTE_MS
        }
}
