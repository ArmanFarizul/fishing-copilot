package com.fishingcopilot.ui.spots

import android.graphics.PointF
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fishingcopilot.R
import com.fishingcopilot.satellite.ClarityLevel
import com.fishingcopilot.satellite.FrontStrength
import com.fishingcopilot.satellite.MapCell
import com.fishingcopilot.satellite.PlanktonLevel
import com.fishingcopilot.satellite.SatelliteMapLayer
import com.fishingcopilot.satellite.WaterTempLevel
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillAntialias
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val CELLS_SOURCE = "satellite-cells"
private const val CELLS_LAYER = "satellite-cells-fill"
private const val SPOTS_SOURCE = "my-spots"
private const val SPOTS_LAYER = "my-spots-pins"
private const val CELL_INDEX = "cell"
private const val COLOR = "color"
private const val HOME = "home"

// The pipeline's area: Straits of Melaka to eastern Sabah.
private val COVERAGE = LatLngBounds.from(7.5, 119.5, 0.5, 99.0)

private val Warm = Color(0xFFFF9100)
private val Murky = Color(0xFFB08D57)
private val Teal = Color(0xFF26A69A)
private val ClearBlue = Color(0xFF4FC3F7)

/** Satellite layer over the spots map: pick a layer, tap a square to read it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SatelliteMapPanel(
    state: SatelliteMapState,
    spots: List<SpotItem>,
    onLayer: (SatelliteMapLayer) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val cells = (state as? SatelliteMapState.Ready)?.cells
    var tapped by remember(state.layer, cells) { mutableStateOf<MapCell?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SatelliteMapLayer.entries.forEach { layer ->
                FilterChip(
                    selected = state.layer == layer,
                    onClick = { onLayer(layer) },
                    label = { Text(stringResource(layer.label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NauticalCyan.copy(alpha = 0.15f),
                        selectedLabelColor = NauticalCyan
                    )
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp))) {
            SatelliteMapView(cells?.cells.orEmpty(), spots, onCellTap = { tapped = it }, modifier = Modifier.fillMaxSize())
            when (state) {
                is SatelliteMapState.Loading -> Status {
                    CircularProgressIndicator(color = NauticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.satellite_loading), style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
                }
                is SatelliteMapState.Offline -> Status {
                    Text(
                        stringResource(R.string.satellite_error_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHighContrast,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.satellite_retry), color = NauticalCyan) }
                }
                is SatelliteMapState.Ready -> Unit
            }
        }

        Text(
            tapped?.let { cellText(state.layer, it, locale) } ?: stringResource(R.string.satellite_map_hint),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (tapped != null) FontWeight.Bold else FontWeight.Normal,
            color = tapped?.let { levelColor(it.level) } ?: TextMuted
        )
        Legend(state.layer)
        if (cells != null) {
            val uriHandler = LocalUriHandler.current
            Text(
                stringResource(R.string.satellite_data_date, cells.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                stringResource(R.string.attribution_copernicus),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri("https://marine.copernicus.eu/") }
            )
        }
    }
}

@Composable
private fun Status(content: @Composable RowScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = OceanSurface.copy(alpha = 0.92f), modifier = Modifier.padding(12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(layer: SatelliteMapLayer) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        layer.levels.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(levelColor(level)))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(levelName(level)), style = MaterialTheme.typography.labelMedium, color = TextHighContrast)
            }
        }
    }
}

@Composable
private fun cellText(layer: SatelliteMapLayer, cell: MapCell, locale: Locale): String {
    val name = stringResource(levelName(cell.level))
    val value = when (layer) {
        SatelliteMapLayer.PLANKTON -> stringResource(
            R.string.satellite_plankton_value,
            String.format(locale, if (cell.value < 1) "%.2f" else "%.1f", cell.value)
        )
        SatelliteMapLayer.SEA_TEMP -> stringResource(R.string.satellite_temp_value, String.format(locale, "%.1f", cell.value))
        SatelliteMapLayer.CLARITY -> stringResource(R.string.satellite_clarity_value, cell.value.roundToInt())
        // The front's strength is already in its meaning line.
        SatelliteMapLayer.FRONTS -> stringResource(
            if (cell.level == FrontStrength.STRONG) R.string.satellite_front_strong_meaning else R.string.satellite_front_weak_meaning
        )
    }
    return "$name · $value"
}

/** MapLibre map with the chosen layer's squares and the angler's own spots on top. */
@Composable
private fun SatelliteMapView(cells: List<MapCell>, spots: List<SpotItem>, onCellTap: (MapCell?) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCells by rememberUpdatedState(cells)
    val currentOnCellTap by rememberUpdatedState(onCellTap)
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
                libreMap.moveCamera(CameraUpdateFactory.newLatLngBounds(COVERAGE, 16))
                libreMap.setStyle(STYLE_URL) { style ->
                    style.addSource(GeoJsonSource(CELLS_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                    style.addSource(GeoJsonSource(SPOTS_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                    style.addLayer(
                        FillLayer(CELLS_LAYER, CELLS_SOURCE).withProperties(
                            fillColor(Expression.toColor(Expression.get(COLOR))),
                            fillOpacity(0.55f),
                            // Antialiased edges leave hairline seams between neighbouring squares.
                            fillAntialias(false)
                        )
                    )
                    style.addLayer(
                        CircleLayer(SPOTS_LAYER, SPOTS_SOURCE).withProperties(
                            circleRadius(Expression.switchCase(Expression.get(HOME), Expression.literal(8f), Expression.literal(5f))),
                            circleColor(Color.White.toArgb()),
                            circleStrokeWidth(2f),
                            circleStrokeColor(OceanMidnight.toArgb())
                        )
                    )
                    map = libreMap
                }
                libreMap.addOnMapClickListener { latLng ->
                    val point: PointF = libreMap.projection.toScreenLocation(latLng)
                    val index = libreMap.queryRenderedFeatures(point, CELLS_LAYER).firstOrNull()
                        ?.getNumberProperty(CELL_INDEX)?.toInt()
                    currentOnCellTap(index?.let { currentCells.getOrNull(it) })
                    true
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

    LaunchedEffect(map, cells) {
        val style = map?.style ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(CELLS_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(cells.mapIndexed { i, cell -> cellFeature(i, cell) })
        )
    }
    LaunchedEffect(map, spots) {
        val style = map?.style ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(SPOTS_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(spots.map { item ->
                Feature.fromGeometry(Point.fromLngLat(item.spot.longitude, item.spot.latitude)).apply {
                    addBooleanProperty(HOME, item.isHome)
                }
            })
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun cellFeature(index: Int, cell: MapCell): Feature {
    val ring = listOf(
        Point.fromLngLat(cell.west, cell.south),
        Point.fromLngLat(cell.east, cell.south),
        Point.fromLngLat(cell.east, cell.north),
        Point.fromLngLat(cell.west, cell.north),
        Point.fromLngLat(cell.west, cell.south)
    )
    return Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
        addNumberProperty(CELL_INDEX, index)
        addStringProperty(COLOR, "#%06X".format(levelColor(cell.level).toArgb() and 0xFFFFFF))
    }
}

private val SatelliteMapLayer.levels: List<Enum<*>>
    get() = when (this) {
        SatelliteMapLayer.PLANKTON -> PlanktonLevel.entries
        SatelliteMapLayer.SEA_TEMP -> WaterTempLevel.entries
        SatelliteMapLayer.FRONTS -> FrontStrength.entries
        SatelliteMapLayer.CLARITY -> ClarityLevel.entries
    }

@get:StringRes
private val SatelliteMapLayer.label: Int
    get() = when (this) {
        SatelliteMapLayer.PLANKTON -> R.string.satellite_plankton
        SatelliteMapLayer.SEA_TEMP -> R.string.satellite_temp
        SatelliteMapLayer.FRONTS -> R.string.satellite_front
        SatelliteMapLayer.CLARITY -> R.string.satellite_clarity
    }

private fun levelColor(level: Enum<*>): Color = when (level) {
    PlanktonLevel.LOW -> TextMuted
    PlanktonLevel.IDEAL -> PrimeGreen
    PlanktonLevel.HIGH -> CautionYellow
    WaterTempLevel.COOL -> NauticalCyan
    WaterTempLevel.IDEAL -> PrimeGreen
    WaterTempLevel.WARM -> Warm
    ClarityLevel.MURKY -> Murky
    ClarityLevel.MODERATE -> Teal
    ClarityLevel.CLEAR -> ClearBlue
    FrontStrength.WEAK -> NauticalCyan
    FrontStrength.STRONG -> PrimeGreen
    else -> TextMuted
}

@StringRes
private fun levelName(level: Enum<*>): Int = when (level) {
    PlanktonLevel.LOW -> R.string.satellite_plankton_low
    PlanktonLevel.IDEAL -> R.string.satellite_plankton_ideal
    PlanktonLevel.HIGH -> R.string.satellite_plankton_high
    WaterTempLevel.COOL -> R.string.satellite_temp_cool
    WaterTempLevel.IDEAL -> R.string.satellite_temp_ideal
    WaterTempLevel.WARM -> R.string.satellite_temp_warm
    ClarityLevel.MURKY -> R.string.satellite_clarity_murky
    ClarityLevel.MODERATE -> R.string.satellite_clarity_moderate
    ClarityLevel.CLEAR -> R.string.satellite_clarity_clear
    FrontStrength.WEAK -> R.string.satellite_front_weak
    else -> R.string.satellite_front_strong
}
