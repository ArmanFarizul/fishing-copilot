package com.fishingcopilot.maps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/** Lets buttons drawn over a map move its camera; the map view attaches itself once it is ready. */
class MapController {
    internal var map: MapLibreMap? = null

    fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut())
    }

    /** Centres on a point, zooming in to at least [minZoom] so the spot is readable. */
    fun moveTo(latitude: Double, longitude: Double, minZoom: Double = 11.0) {
        val libreMap = map ?: return
        libreMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), maxOf(libreMap.cameraPosition.zoom, minZoom))
        )
    }
}

@Composable
fun rememberMapController(): MapController = remember { MapController() }
