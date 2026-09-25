package com.fishingcopilot.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fishingcopilot.alerts.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Where the angler parked the Strike button: which side, and how far down the screen (0 top, 1 bottom). */
data class StrikePosition(val right: Boolean = true, val fraction: Float = 0.85f)

/** Remembers the sunlight toggle and the Strike button's place across launches. */
class DisplaySettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val sunlight: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SUNLIGHT] ?: false }

    suspend fun setSunlight(enabled: Boolean) = store.edit { it[SUNLIGHT] = enabled }

    val strikePosition: Flow<StrikePosition> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { StrikePosition(it[STRIKE_RIGHT] ?: true, it[STRIKE_FRACTION] ?: 0.85f) }

    suspend fun setStrikePosition(position: StrikePosition) = store.edit {
        it[STRIKE_RIGHT] = position.right
        it[STRIKE_FRACTION] = position.fraction.coerceIn(0f, 1f)
    }

    /** A JAKIM zone the angler picked by hand, or null to follow the phone's location. */
    val prayerZone: Flow<String?> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PRAYER_ZONE] }

    suspend fun setPrayerZone(code: String?) = store.edit { if (code == null) it.remove(PRAYER_ZONE) else it[PRAYER_ZONE] = code }

    private companion object {
        val PRAYER_ZONE = stringPreferencesKey("prayer_zone")
        val SUNLIGHT = booleanPreferencesKey("sunlight_mode")
        val STRIKE_RIGHT = booleanPreferencesKey("strike_right")
        val STRIKE_FRACTION = floatPreferencesKey("strike_fraction")
    }
}
