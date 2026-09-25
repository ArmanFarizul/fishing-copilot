package com.fishingcopilot.ui.spots

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.maps.rememberMapController
import com.fishingcopilot.ui.components.GlowCheckIndicator
import com.fishingcopilot.ui.components.GoToCoordinateButton
import com.fishingcopilot.ui.components.GoToCoordinateDialog
import com.fishingcopilot.ui.components.MapZoomControls
import com.fishingcopilot.ui.components.NauticalAttribution
import com.fishingcopilot.ui.components.icon
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.components.subtitle
import com.fishingcopilot.ui.onboarding.SpotMap
import com.fishingcopilot.ui.onboarding.SpotSelection
import com.fishingcopilot.ui.onboarding.fetchCurrentLocation
import com.fishingcopilot.ui.onboarding.hasLocationPermission
import com.fishingcopilot.ui.onboarding.placeName
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private enum class LocationStatus { IDLE, LOCATING, DENIED, UNAVAILABLE }

/**
 * Map-based spot picker shared by onboarding step 4 and the Spots tab: tap one of the coastal area pins,
 * drag the map under the crosshair, or use the current location; a list sheet covers accessibility and
 * offline use. Emits the selection with a localised default name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.SpotPicker(
    selection: SpotSelection?,
    spotName: String,
    onSelect: (SpotSelection, String) -> Unit,
    onNameChange: (String) -> Unit
) {
    val context = LocalContext.current
    var status by rememberSaveable { mutableStateOf(LocationStatus.IDLE) }
    var showList by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val currentSelection by rememberUpdatedState(selection)
    val areaNames = CoastalArea.entries.associateWith { stringResource(it.label) }
    val nearTemplate = stringResource(R.string.here_near_area)

    // Names the point at once from the nearest coastal area (works offline), then with the town name
    // once the phone's geocoder answers. Both only fill the name while the angler has not typed one.
    fun selectPoint(latitude: Double, longitude: Double) {
        status = LocationStatus.IDLE
        val point = SpotSelection.Point(latitude, longitude)
        val nearest = CoastalArea.nearest(latitude, longitude).first
        onSelect(point, String.format(nearTemplate, areaNames.getValue(nearest)))
        scope.launch {
            val town = placeName(context, latitude, longitude) ?: return@launch
            if (currentSelection == point) onSelect(point, town)
        }
    }

    fun locate() {
        status = LocationStatus.LOCATING
        fetchCurrentLocation(context) { location ->
            if (location == null) status = LocationStatus.UNAVAILABLE
            else selectPoint(location.latitude, location.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) locate() else status = LocationStatus.DENIED
    }

    val mapController = rememberMapController()
    var goTo by rememberSaveable { mutableStateOf(false) }
    val mapDescription = stringResource(R.string.onboarding_spot_map_description)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(CardBorder, RoundedCornerShape(20.dp))
            .background(OceanSurface)
    ) {
        SpotMap(
            selection = selection,
            onAreaTap = { area ->
                status = LocationStatus.IDLE
                onSelect(SpotSelection.Area(area), areaNames.getValue(area))
            },
            onPointPicked = ::selectPoint,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = mapDescription },
            controller = mapController
        )
        MapCrosshair(Modifier.align(Alignment.Center))
        GoToCoordinateButton(onClick = { goTo = true }, modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
        // Left gap keeps clear of MapLibre's own attribution button; right gap of the zoom buttons.
        NauticalAttribution(mapController, modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, end = 72.dp, bottom = 8.dp))
        MapZoomControls(mapController, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
        SmallFloatingActionButton(
            onClick = {
                if (hasLocationPermission(context)) locate()
                else permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            },
            containerColor = OceanMidnight,
            contentColor = NauticalCyan,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .border(1.dp, NauticalCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            LocateIcon(contentDescription = stringResource(R.string.onboarding_spot_use_location))
        }
    }

    if (goTo) {
        // A typed coordinate becomes the spot, and the map follows the selection there.
        GoToCoordinateDialog(onGo = { selectPoint(it.latitude, it.longitude); goTo = false }, onDismiss = { goTo = false })
    }

    TextButton(
        onClick = { showList = true },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text = stringResource(R.string.onboarding_spot_pick_from_list), color = NauticalCyan)
    }

    SelectionCard(
        selection = selection,
        status = status,
        areaNames = areaNames,
        onPickNearest = { area -> onSelect(SpotSelection.Area(area), areaNames.getValue(area)) }
    )

    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = spotName,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.onboarding_spot_name_label)) },
        singleLine = true,
        enabled = selection != null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NauticalCyan,
            unfocusedBorderColor = OceanCardBorder,
            focusedLabelColor = NauticalCyan,
            unfocusedLabelColor = TextMuted,
            focusedTextColor = TextHighContrast,
            unfocusedTextColor = TextHighContrast,
            focusedContainerColor = OceanSurface,
            unfocusedContainerColor = OceanSurface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    if (showList) {
        ModalBottomSheet(
            onDismissRequest = { showList = false },
            containerColor = OceanMidnight
        ) {
            Text(
                text = stringResource(R.string.onboarding_spot_areas_label),
                style = MaterialTheme.typography.titleMedium,
                color = TextHighContrast,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                modifier = Modifier.selectableGroup()
            ) {
                items(CoastalArea.entries) { area ->
                    val name = areaNames.getValue(area)
                    val state = stringResource(area.state.label)
                    SpotOption(
                        title = name,
                        // Penang, Melaka and Labuan are both area and state; don't repeat the name.
                        subtitle = state.takeIf { it != name },
                        selected = selection == SpotSelection.Area(area),
                        onClick = {
                            status = LocationStatus.IDLE
                            onSelect(SpotSelection.Area(area), name)
                            showList = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionCard(
    selection: SpotSelection?,
    status: LocationStatus,
    areaNames: Map<CoastalArea, String>,
    onPickNearest: (CoastalArea) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OceanSurface,
        border = if (selection != null) BorderStroke(CardBorder.width, NauticalCyan) else CardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            when (selection) {
                null -> Text(
                    text = stringResource(R.string.onboarding_spot_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                is SpotSelection.Area -> {
                    val name = areaNames.getValue(selection.area)
                    val state = stringResource(selection.area.state.label)
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighContrast)
                    if (state != name) Text(state, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                is SpotSelection.Point -> {
                    val coordinates = String.format(Locale.ROOT, "%.4f, %.4f", selection.latitude, selection.longitude)
                    Text(
                        text = stringResource(R.string.onboarding_spot_custom_point, coordinates),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighContrast
                    )
                    val (nearest, km) = CoastalArea.nearest(selection.latitude, selection.longitude)
                    Text(
                        text = stringResource(R.string.onboarding_spot_nearest_area, areaNames.getValue(nearest), km.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = NauticalCyan,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(role = Role.Button) { onPickNearest(nearest) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
            when (status) {
                LocationStatus.LOCATING -> StatusNote(stringResource(R.string.onboarding_spot_locating))
                LocationStatus.DENIED -> StatusNote(stringResource(R.string.onboarding_spot_location_denied))
                LocationStatus.UNAVAILABLE -> StatusNote(stringResource(R.string.onboarding_spot_location_unavailable))
                LocationStatus.IDLE -> Unit
            }
        }
    }
}

/** Fixed crosshair over the map centre; dragging the map moves the spot under it. */
@Composable
private fun MapCrosshair(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(36.dp)) {
        val stroke = 2.dp.toPx()
        val gap = 5.dp.toPx()
        val c = center
        drawCircle(color = NauticalCyan, radius = 3.dp.toPx())
        drawCircle(color = NauticalCyan.copy(alpha = 0.9f), radius = 11.dp.toPx(), style = Stroke(stroke))
        listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach { d ->
            drawLine(
                color = NauticalCyan,
                start = c + d * (11.dp.toPx() + gap / 2),
                end = c + d * (size.minDimension / 2),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Target-style "my location" glyph, drawn so the app needs no icon library. */
@Composable
private fun LocateIcon(contentDescription: String) {
    val tint = NauticalCyan
    Canvas(
        modifier = Modifier
            .size(22.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val stroke = 2.dp.toPx()
        drawCircle(color = tint, radius = size.minDimension * 0.3f, style = Stroke(stroke))
        drawCircle(color = tint, radius = size.minDimension * 0.1f)
        listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach { d ->
            drawLine(tint, center + d * (size.minDimension * 0.3f), center + d * (size.minDimension * 0.5f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun StatusNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SpotOption(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) NauticalCyan else OceanCardBorder,
        label = "spotBorderColor"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = OceanSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast
                )
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            GlowCheckIndicator(selected = selected, accentColor = NauticalCyan)
        }
    }
}

