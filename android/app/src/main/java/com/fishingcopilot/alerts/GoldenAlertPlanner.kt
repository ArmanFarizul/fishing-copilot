package com.fishingcopilot.alerts

import com.fishingcopilot.bite.ScoreWindow

data class PlannedAlert(val alertAt: Long, val window: ScoreWindow)

/** Which notification channel matches the user's sound and vibration switches (channel sound is fixed once created). */
enum class AlertChannel(val id: String, val chimeId: String) {
    FULL("golden_time_full", "tide_chime_full"),
    SOUND("golden_time_sound", "tide_chime_sound"),
    VIBRATE("golden_time_vibrate", "tide_chime_vibrate"),
    /** Posted on the vibrate channel with the notification marked silent. */
    SILENT("golden_time_vibrate", "tide_chime_vibrate");

    companion object {
        fun of(sound: Boolean, vibrate: Boolean): AlertChannel = when {
            sound && vibrate -> FULL
            sound -> SOUND
            vibrate -> VIBRATE
            else -> SILENT
        }
    }
}

/** The spec's alert: 45 minutes before prime time starts, once per window. */
object GoldenAlertPlanner {
    const val LEAD_MINUTES = 45L
    private const val MINUTE_MS = 60_000L

    // An inexact alarm can fire up to ~10 minutes off, and the forecast shifts a little between runs,
    // so a window counts as "this alert's" if it starts 15-75 minutes after the alarm fires.
    private const val DUE_FROM_MINUTES = 15L
    private const val DUE_TO_MINUTES = 75L

    fun next(windows: List<ScoreWindow>, now: Long, lastNotifiedStart: Long?): PlannedAlert? =
        windows
            .filter { it.start != lastNotifiedStart }
            .map { PlannedAlert(it.start - LEAD_MINUTES * MINUTE_MS, it) }
            .firstOrNull { it.alertAt > now }

    fun due(windows: List<ScoreWindow>, now: Long, lastNotifiedStart: Long?): ScoreWindow? =
        windows.firstOrNull {
            it.start != lastNotifiedStart &&
                it.start - now in DUE_FROM_MINUTES * MINUTE_MS..DUE_TO_MINUTES * MINUTE_MS
        }
}
