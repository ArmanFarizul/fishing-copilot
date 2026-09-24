package com.fishingcopilot

import android.app.Application
import com.fishingcopilot.data.local.FishingDatabase
import com.fishingcopilot.data.remote.OpenMeteoMarineClient
import com.fishingcopilot.data.tide.TideRepository
import org.maplibre.android.MapLibre
import com.fishingcopilot.data.profile.DataStoreProfileRepository
import com.fishingcopilot.data.profile.ProfileRepository

class FishingCopilotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }

    val profileRepository: ProfileRepository by lazy { DataStoreProfileRepository(this) }
    val database: FishingDatabase by lazy { FishingDatabase.get(this) }
    val tideRepository: TideRepository by lazy { TideRepository(database.tideDao(), OpenMeteoMarineClient()) }
}
