package com.fishingcopilot.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fishingcopilot.R
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.ui.onboarding.placeName
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Geocoder answers are slow and rate-limited, so each point is looked up once per app run.
private val placeCache = ConcurrentHashMap<String, String>()

/**
 * Town name for a saved spot, shown under the angler's own name for it. Starts as "Near <coastal area>",
 * which works offline, and switches to the town once the phone's geocoder answers.
 */
@Composable
fun rememberSpotPlace(latitude: Double, longitude: Double): String {
    val context = LocalContext.current
    val key = String.format(Locale.ROOT, "%.4f,%.4f", latitude, longitude)
    val fallback = stringResource(R.string.here_near_area, stringResource(CoastalArea.nearest(latitude, longitude).first.label))
    val place by produceState(initialValue = placeCache[key] ?: fallback, key) {
        placeCache[key]?.let { value = it; return@produceState }
        placeName(context, latitude, longitude)?.let {
            placeCache[key] = it
            value = it
        }
    }
    return place
}
