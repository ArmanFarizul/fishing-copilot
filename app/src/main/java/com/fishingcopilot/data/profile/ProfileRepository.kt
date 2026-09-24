package com.fishingcopilot.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface ProfileRepository {
    /** Emits null until onboarding has been completed. */
    val profile: Flow<UserProfile?>

    suspend fun save(profile: UserProfile)
}

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

class DataStoreProfileRepository(context: Context) : ProfileRepository {
    private val dataStore = context.applicationContext.profileDataStore

    override val profile: Flow<UserProfile?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toProfile() }

    override suspend fun save(profile: UserProfile) {
        dataStore.edit {
            it[NICKNAME] = profile.nickname
            it[AVATAR] = profile.avatar.name
            it[FISHING_STYLE] = profile.fishingStyle.name
            it[TARGET_SPECIES] = profile.targetSpecies.mapTo(mutableSetOf()) { species -> species.name }
        }
    }

    private fun Preferences.toProfile(): UserProfile? {
        val nickname = this[NICKNAME] ?: return null
        return UserProfile(
            nickname = nickname,
            avatar = enumOrNull<Avatar>(this[AVATAR]) ?: Avatar.JETTY,
            fishingStyle = enumOrNull<FishingStyle>(this[FISHING_STYLE]) ?: FishingStyle.SHORE,
            targetSpecies = this[TARGET_SPECIES].orEmpty().mapNotNullTo(mutableSetOf()) { enumOrNull<Species>(it) }
        )
    }

    // Stored names can outlive a renamed enum constant, so unknown values fall back instead of crashing.
    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        enumValues<T>().firstOrNull { it.name == name }

    private companion object {
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR = stringPreferencesKey("avatar")
        val FISHING_STYLE = stringPreferencesKey("fishing_style")
        val TARGET_SPECIES = stringSetPreferencesKey("target_species")
    }
}
