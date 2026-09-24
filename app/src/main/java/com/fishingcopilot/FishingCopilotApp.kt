package com.fishingcopilot

import android.app.Application
import com.fishingcopilot.data.profile.DataStoreProfileRepository
import com.fishingcopilot.data.profile.ProfileRepository

class FishingCopilotApp : Application() {
    val profileRepository: ProfileRepository by lazy { DataStoreProfileRepository(this) }
}
