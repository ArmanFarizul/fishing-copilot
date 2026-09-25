package com.fishingcopilot.alerts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.fishingcopilot.tide.TideEventType
import java.io.IOException

data class AlertSettings(
    val goldenAlerts: Boolean = false,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    /** The alert currently scheduled, for the settings screen. */
    val nextAlertAt: Long? = null,
    val nextWindowStart: Long? = null,
    val nextWindowEnd: Long? = null,
    /** Start of the last window announced, so a window is never announced twice. */
    val lastNotifiedStart: Long? = null,
    val tideChime: Boolean = false,
    /** The next tide turn chimed for, for the settings screen. */
    val nextChimeAt: Long? = null,
    val nextChimeHigh: Boolean? = null,
    /** Time of the last turn chimed for, so a turn is never chimed twice. */
    val lastChimedTurn: Long? = null
) {
    val anyAlert get() = goldenAlerts || tideChime
}

// One store per file: DataStore allows only one instance for "settings", so display settings share it.
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AlertSettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AlertSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            AlertSettings(
                goldenAlerts = it[GOLDEN] ?: false,
                sound = it[SOUND] ?: true,
                vibration = it[VIBRATION] ?: true,
                nextAlertAt = it[NEXT_ALERT],
                nextWindowStart = it[NEXT_START],
                nextWindowEnd = it[NEXT_END],
                lastNotifiedStart = it[LAST_NOTIFIED],
                tideChime = it[TIDE_CHIME] ?: false,
                nextChimeAt = it[NEXT_CHIME],
                nextChimeHigh = it[NEXT_CHIME_HIGH],
                lastChimedTurn = it[LAST_CHIMED]
            )
        }

    suspend fun current(): AlertSettings = settings.first()

    suspend fun setGoldenAlerts(enabled: Boolean) = store.edit { it[GOLDEN] = enabled }
    suspend fun setSound(enabled: Boolean) = store.edit { it[SOUND] = enabled }
    suspend fun setVibration(enabled: Boolean) = store.edit { it[VIBRATION] = enabled }

    suspend fun setScheduled(plan: PlannedAlert?) = store.edit {
        if (plan == null) {
            it.remove(NEXT_ALERT); it.remove(NEXT_START); it.remove(NEXT_END)
        } else {
            it[NEXT_ALERT] = plan.alertAt; it[NEXT_START] = plan.window.start; it[NEXT_END] = plan.window.end
        }
    }

    suspend fun setLastNotified(start: Long) = store.edit { it[LAST_NOTIFIED] = start }

    suspend fun setTideChime(enabled: Boolean) = store.edit { it[TIDE_CHIME] = enabled }

    suspend fun setChimeScheduled(plan: PlannedChime?) = store.edit {
        if (plan == null) {
            it.remove(NEXT_CHIME); it.remove(NEXT_CHIME_HIGH)
        } else {
            it[NEXT_CHIME] = plan.event.epochMillis; it[NEXT_CHIME_HIGH] = plan.event.type == TideEventType.HIGH
        }
    }

    suspend fun setLastChimed(turn: Long) = store.edit { it[LAST_CHIMED] = turn }

    private companion object {
        val GOLDEN = booleanPreferencesKey("golden_alerts")
        val SOUND = booleanPreferencesKey("golden_sound")
        val VIBRATION = booleanPreferencesKey("golden_vibration")
        val NEXT_ALERT = longPreferencesKey("next_alert_at")
        val NEXT_START = longPreferencesKey("next_window_start")
        val NEXT_END = longPreferencesKey("next_window_end")
        val LAST_NOTIFIED = longPreferencesKey("last_notified_start")
        val TIDE_CHIME = booleanPreferencesKey("tide_chime")
        val NEXT_CHIME = longPreferencesKey("next_chime_turn")
        val NEXT_CHIME_HIGH = booleanPreferencesKey("next_chime_high")
        val LAST_CHIMED = longPreferencesKey("last_chimed_turn")
    }
}
