package com.fishingcopilot

import android.app.Application
import com.fishingcopilot.alerts.AlertSettingsRepository
import com.fishingcopilot.alerts.GoldenAlerts
import com.fishingcopilot.bite.BiteForecaster
import com.fishingcopilot.catchlog.AppPhotoStore
import com.fishingcopilot.catchlog.PhotoStore
import com.fishingcopilot.data.hijri.HijriRepository
import com.fishingcopilot.data.local.FishingDatabase
import com.fishingcopilot.data.marine.MarineRepository
import com.fishingcopilot.data.remote.JakimTakwimClient
import com.fishingcopilot.data.remote.MetWarningClient
import com.fishingcopilot.data.remote.OpenMeteoMarineClient
import com.fishingcopilot.data.remote.OpenMeteoWeatherClient
import com.fishingcopilot.data.remote.SatelliteClient
import com.fishingcopilot.data.satellite.SatelliteRepository
import com.fishingcopilot.data.tide.TideRepository
import com.fishingcopilot.data.warnings.WarningRepository
import org.maplibre.android.MapLibre
import java.io.File
import com.fishingcopilot.data.profile.DataStoreProfileRepository
import com.fishingcopilot.data.profile.ProfileRepository

class FishingCopilotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        goldenAlerts.createChannels()
    }

    val profileRepository: ProfileRepository by lazy { DataStoreProfileRepository(this) }
    val database: FishingDatabase by lazy { FishingDatabase.get(this) }
    val tideRepository: TideRepository by lazy { TideRepository(database.tideDao(), OpenMeteoMarineClient()) }
    val marineRepository: MarineRepository by lazy { MarineRepository(database.marineDao(), OpenMeteoWeatherClient()) }
    val hijriRepository: HijriRepository by lazy { HijriRepository(database.hijriDao(), JakimTakwimClient()) }
    val satelliteRepository: SatelliteRepository by lazy {
        SatelliteRepository(File(filesDir, "satellite/daily_marine_fronts.json"), SatelliteClient())
    }
    val warningRepository: WarningRepository by lazy {
        WarningRepository(File(filesDir, "warnings/metmalaysia.json"), MetWarningClient())
    }
    val photoStore: PhotoStore by lazy { AppPhotoStore(this) }
    val alertSettings: AlertSettingsRepository by lazy { AlertSettingsRepository(this) }
    val biteForecaster: BiteForecaster by lazy {
        BiteForecaster(database.fishingDao(), tideRepository, marineRepository, hijriRepository)
    }
    val goldenAlerts: GoldenAlerts by lazy { GoldenAlerts(this) }
}
