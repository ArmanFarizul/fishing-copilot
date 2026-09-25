package com.fishingcopilot.ui.onboarding

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.maps.MAP_STYLE_URL
import com.fishingcopilot.maps.MapController
import com.fishingcopilot.data.spots.haversineKm
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.TextMuted
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val AREAS_SOURCE = "coastal-areas"
private const val AREAS_LAYER = "coastal-areas-pins"
private const val SELECTED_SOURCE = "selected-area"
private const val SELECTED_LAYER = "selected-area-ring"
private const val AREA_ID = "area"
private const val FOCUS_ZOOM = 9.0

// Peninsular Malaysia to eastern Sabah.
private val MALAYSIA = LatLngBounds.from(7.6, 119.6, 0.8, 99.3)

/**
 * Map for picking the home spot. Area pins are local GeoJSON, so they still show when tiles
 * cannot load offline. Dragging the map reports the centre (under the overlay crosshair) as a point.
 */
@Composable
fun SpotMap(
    selection: SpotSelection?,
    onAreaTap: (CoastalArea) -> Unit,
    onPointPicked: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    controller: MapController? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tapTolerancePx = with(LocalDensity.current) { 24.dp.toPx() }
    val currentOnAreaTap by rememberUpdatedState(onAreaTap)
    val currentOnPointPicked by rememberUpdatedState(onPointPicked)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { libreMap ->
                libreMap.uiSettings.apply {
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isCompassEnabled = false
                    isLogoEnabled = false
                    setAttributionTintColor(TextMuted.toArgb())
                }
                libreMap.moveCamera(CameraUpdateFactory.newLatLngBounds(MALAYSIA, 24))
                libreMap.setStyle(MAP_STYLE_URL) { style ->
                    style.addSource(GeoJsonSource(AREAS_SOURCE, areaFeatures()))
                    style.addSource(GeoJsonSource(SELECTED_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                    style.addLayer(
                        CircleLayer(AREAS_LAYER, AREAS_SOURCE).withProperties(
                            circleRadius(6f),
                            circleColor(NauticalCyan.toArgb()),
                            circleStrokeWidth(2f),
                            circleStrokeColor(OceanMidnight.toArgb())
                        )
                    )
                    style.addLayer(
                        CircleLayer(SELECTED_LAYER, SELECTED_SOURCE).withProperties(
                            circleRadius(12f),
                            circleColor(android.graphics.Color.TRANSPARENT),
                            circleStrokeWidth(3f),
                            circleStrokeColor(android.graphics.Color.WHITE)
                        )
                    )
                    map = libreMap
                    controller?.map = libreMap
                }

                libreMap.addOnMapClickListener { latLng ->
                    val screen = libreMap.projection.toScreenLocation(latLng)
                    val box = RectF(
                        screen.x - tapTolerancePx, screen.y - tapTolerancePx,
                        screen.x + tapTolerancePx, screen.y + tapTolerancePx
                    )
                    val hit = libreMap.queryRenderedFeatures(box, AREAS_LAYER).firstOrNull()
                    val area = hit?.getStringProperty(AREA_ID)?.let { id -> CoastalArea.entries.firstOrNull { it.name == id } }
                    if (area != null) currentOnAreaTap(area)
                    area != null
                }

                // Only a finger drag picks a point; camera moves we start ourselves must not.
                var draggedByUser = false
                libreMap.addOnCameraMoveStartedListener { reason ->
                    draggedByUser = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
                }
                libreMap.addOnCameraIdleListener {
                    if (draggedByUser) {
                        draggedByUser = false
                        val target = libreMap.cameraPosition.target ?: return@addOnCameraIdleListener
                        currentOnPointPicked(target.latitude, target.longitude)
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Keep the highlight ring and camera in step with the selection, whichever control changed it.
    LaunchedEffect(map, selection) {
        val libreMap = map ?: return@LaunchedEffect
        val style = libreMap.style ?: return@LaunchedEffect
        val selectedArea = (selection as? SpotSelection.Area)?.area
        style.getSourceAs<GeoJsonSource>(SELECTED_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(listOfNotNull(selectedArea?.let(::areaFeature)))
        )
        if (selection != null) {
            val target = libreMap.cameraPosition.target
            val offKm = target?.let { haversineKm(it.latitude, it.longitude, selection.latitude, selection.longitude) }
            if (offKm == null || offKm > 0.05) {
                libreMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(selection.latitude, selection.longitude),
                        maxOf(libreMap.cameraPosition.zoom, FOCUS_ZOOM)
                    )
                )
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun areaFeature(area: CoastalArea): Feature =
    Feature.fromGeometry(Point.fromLngLat(area.longitude, area.latitude)).apply { addStringProperty(AREA_ID, area.name) }

private fun areaFeatures(): FeatureCollection = FeatureCollection.fromFeatures(CoastalArea.entries.map(::areaFeature))
