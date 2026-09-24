package com.fishingcopilot

import android.app.Application
import com.fishingcopilot.catchlog.AppPhotoStore
import com.fishingcopilot.catchlog.PhotoStore
import com.fishingcopilot.data.hijri.HijriRepository
import com.fishingcopilot.data.local.FishingDatabase
import com.fishingcopilot.data.marine.MarineRepository
import com.fishingcopilot.data.remote.JakimTakwimClient
import com.fishingcopilot.data.remote.OpenMeteoMarineClient
import com.fishingcopilot.data.remote.OpenMeteoWeatherClient
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
    val marineRepository: MarineRepository by lazy { MarineRepository(database.marineDao(), OpenMeteoWeatherClient()) }
    val hijriRepository: HijriRepository by lazy { HijriRepository(database.hijriDao(), JakimTakwimClient()) }
    val photoStore: PhotoStore by lazy { AppPhotoStore(this) }
}
