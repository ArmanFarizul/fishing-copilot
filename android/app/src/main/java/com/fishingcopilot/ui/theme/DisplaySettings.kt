package com.fishingcopilot.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.fishingcopilot.alerts.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Remembers the sunlight toggle across launches. */
class DisplaySettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val sunlight: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SUNLIGHT] ?: false }

    suspend fun setSunlight(enabled: Boolean) = store.edit { it[SUNLIGHT] = enabled }

    private companion object {
        val SUNLIGHT = booleanPreferencesKey("sunlight_mode")
    }
}
