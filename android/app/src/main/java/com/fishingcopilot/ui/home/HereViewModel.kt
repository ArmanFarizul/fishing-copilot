package com.fishingcopilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.data.local.FishingDao
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.prayer.PrayerMonth
import com.fishingcopilot.data.prayer.PrayerRepository
import com.fishingcopilot.data.spots.haversineKm
import com.fishingcopilot.data.weather.CachedForecast
import com.fishingcopilot.data.weather.WeatherRepository
import com.fishingcopilot.maps.LatLon
import com.fishingcopilot.prayer.MALAYSIA
import com.fishingcopilot.prayer.NextPrayer
import com.fishingcopilot.prayer.nextPrayer
import com.fishingcopilot.weather.StormAlert
import com.fishingcopilot.weather.stormAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime

sealed interface HereLocation {
    data object NoPermission : HereLocation
    data object Locating : HereLocation
    /** Location is off, or no fix arrived. */
    data object Unavailable : HereLocation
    data class Found(val point: LatLon) : HereLocation
}

data class HereUiState(
    val location: HereLocation = HereLocation.Locating,
    val forecast: CachedForecast? = null,
    val weatherFailed: Boolean = false,
    val storm: StormAlert? = null,
    val prayers: PrayerMonth? = null,
    val nextPrayer: NextPrayer? = null,
    /** Town or area name for the phone's position, when the phone can look one up. */
    val placeName: String? = null,
    /** A saved spot within [NEAREST_SPOT_KM], nearest first. */
    val nearbySpot: SpotEntity? = null
)

const val NEAREST_SPOT_KM = 2.0

/**
 * "Where you are": the phone's position, the weather and prayer times there, and whether the angler is
 * standing at one of their saved spots. [locate] wraps the platform location call so tests can fake it.
 */
class HereViewModel(
    spots: FishingDao,
    private val weather: WeatherRepository,
    private val prayers: PrayerRepository,
    private val hasPermission: () -> Boolean,
    private val locate: suspend () -> LatLon?,
    private val placeName: suspend (LatLon) -> String? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
    ticks: Flow<Long>? = null
) : ViewModel() {
    private val location = MutableStateFlow<HereLocation>(HereLocation.Locating)
    private val weatherFailed = MutableStateFlow(false)
    private val place = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HereUiState> = combine(
        combine(location, weatherFailed, place, ::Triple),
        weather.data,
        prayers.data,
        spots.getAllSpotsFlow(),
        ticks ?: minuteTicks()
    ) { (location, failed, placeName), forecast, month, allSpots, now ->
        val point = (location as? HereLocation.Found)?.point
        HereUiState(
            location = location,
            placeName = placeName,
            forecast = forecast,
            weatherFailed = failed,
            storm = forecast?.let { stormAlert(it.forecast.hours, now) },
            prayers = month,
            nextPrayer = month?.let { nextPrayer(it.days, LocalDateTime.ofInstant(Instant.ofEpochMilli(now), MALAYSIA)) },
            nearbySpot = point?.let { p ->
                allSpots.map { it to haversineKm(p.latitude, p.longitude, it.latitude, it.longitude) }
                    .filter { it.second <= NEAREST_SPOT_KM }
                    .minByOrNull { it.second }?.first
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HereUiState())

    init {
        refresh()
    }

    /** Finds the phone again and refreshes what is stale; cheap to call on every screen visit. */
    fun refresh() {
        viewModelScope.launch {
            if (!hasPermission()) {
                location.value = HereLocation.NoPermission
                return@launch
            }
            if (location.value !is HereLocation.Found) location.value = HereLocation.Locating
            val point = locate()
            if (point == null) {
                // Keep a fix we already have rather than blanking the card.
                if (location.value !is HereLocation.Found) location.value = HereLocation.Unavailable
                return@launch
            }
            location.value = HereLocation.Found(point)
            launch { place.value = placeName(point) ?: place.value }
            launch {
                weatherFailed.value = try {
                    weather.refreshIfStale(point.latitude, point.longitude)
                    false
                } catch (e: IOException) {
                    true
                }
            }
            launch {
                // Prayer times fall back to the saved month; a failed refresh needs no message.
                try {
                    prayers.refresh(point.latitude, point.longitude)
                } catch (e: IOException) {
                    Unit
                }
            }
        }
    }

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    companion object {
        fun factory(
            spots: FishingDao,
            weather: WeatherRepository,
            prayers: PrayerRepository,
            hasPermission: () -> Boolean,
            locate: suspend () -> LatLon?,
            placeName: suspend (LatLon) -> String?
        ) = viewModelFactory {
            initializer { HereViewModel(spots, weather, prayers, hasPermission, locate, placeName) }
        }
    }
}
