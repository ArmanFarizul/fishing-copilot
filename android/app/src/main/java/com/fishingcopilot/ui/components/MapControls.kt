package com.fishingcopilot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fishingcopilot.FishingCopilotApp
import com.fishingcopilot.R
import com.fishingcopilot.data.remote.PlaceResult
import com.fishingcopilot.maps.LatLon
import com.fishingcopilot.maps.MapController
import com.fishingcopilot.maps.parseCoordinate
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.SelectedContainer
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.io.IOException

/** Zoom buttons big enough for wet fingers, plus an optional "back to my spot". */
@Composable
fun MapZoomControls(controller: MapController, modifier: Modifier = Modifier, onMySpot: (() -> Unit)? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapButton(stringResource(R.string.map_zoom_in), onClick = controller::zoomIn) { plus() }
        MapButton(stringResource(R.string.map_zoom_out), onClick = controller::zoomOut) { minus() }
        if (onMySpot != null) MapButton(stringResource(R.string.map_my_spot), onClick = onMySpot) { pin() }
        MapButton(
            stringResource(if (controller.nautical) R.string.map_chart_hide else R.string.map_chart_show),
            onClick = controller::toggleNautical,
            active = controller.nautical
        ) { anchor() }
    }
}

/** Credit and "not for navigation" note, shown over the map while the nautical chart is on. */
@Composable
fun NauticalAttribution(controller: MapController, modifier: Modifier = Modifier) {
    if (!controller.nautical) return
    Surface(shape = RoundedCornerShape(8.dp), color = OceanMidnight.copy(alpha = 0.85f), modifier = modifier) {
        Text(
            stringResource(R.string.map_chart_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = TextHighContrast,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Small labelled button over the map that opens [GoToCoordinateDialog]. */
@Composable
fun GoToCoordinateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = OceanMidnight.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, NauticalCyan.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.map_go_to),
            style = MaterialTheme.typography.labelLarge,
            color = NauticalCyan,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Takes a coordinate in any common form, or a place name that is looked up online when the angler presses
 * search (the service allows no search-as-you-type). Returns the chosen point.
 */
@Composable
fun GoToCoordinateDialog(onGo: (LatLon) -> Unit, onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as FishingCopilotApp
    val language = LocalConfiguration.current.locales[0].language
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }

    fun submit() {
        parseCoordinate(text)?.let { onGo(it); return }
        // Not a coordinate: treat it as a place name.
        if (text.trim().length < 3) {
            state = SearchState.Invalid
            return
        }
        state = SearchState.Searching
        scope.launch {
            state = try {
                val found = app.placeSearch.search(text, language)
                if (found.isEmpty()) SearchState.NoResults else SearchState.Results(found)
            } catch (e: IOException) {
                SearchState.Failed
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_go_to_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; state = SearchState.Idle },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.map_search_placeholder)) },
                    isError = state == SearchState.Invalid,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() })
                )
                when (val s = state) {
                    SearchState.Idle -> Hint(stringResource(R.string.map_go_to_hint))
                    SearchState.Invalid -> Hint(stringResource(R.string.map_go_to_invalid), error = true)
                    SearchState.Searching -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = NauticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Hint(stringResource(R.string.map_search_searching))
                    }
                    SearchState.NoResults -> Hint(stringResource(R.string.map_search_none))
                    SearchState.Failed -> Hint(stringResource(R.string.map_search_failed), error = true)
                    is SearchState.Results -> LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(s.places) { place ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable(role = Role.Button) { onGo(LatLon(place.latitude, place.longitude)) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(place.name, style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
                                if (place.detail.isNotBlank()) {
                                    Text(place.detail, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.map_search_attribution), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = text.isNotBlank() && state != SearchState.Searching) {
                Text(stringResource(R.string.map_go_to_confirm), color = NauticalCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_go_to_cancel)) } },
        containerColor = OceanSurface
    )
}

private sealed interface SearchState {
    data object Idle : SearchState
    data object Invalid : SearchState
    data object Searching : SearchState
    data object NoResults : SearchState
    data object Failed : SearchState
    data class Results(val places: List<PlaceResult>) : SearchState
}

@Composable
private fun Hint(text: String, error: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = if (error) MaterialTheme.colorScheme.error else TextMuted)
}

@Composable
private fun MapButton(description: String, onClick: () -> Unit, active: Boolean = false, icon: DrawScope.() -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (active) SelectedContainer else OceanMidnight.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, NauticalCyan.copy(alpha = 0.5f)),
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description; role = Role.Button }
    ) {
        Canvas(modifier = Modifier.size(48.dp)) { icon() }
    }
}

private fun DrawScope.plus() {
    val arm = size.minDimension * 0.22f
    drawLine(NauticalCyan, center - Offset(arm, 0f), center + Offset(arm, 0f), 3.dp.toPx(), StrokeCap.Round)
    drawLine(NauticalCyan, center - Offset(0f, arm), center + Offset(0f, arm), 3.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.minus() {
    val arm = size.minDimension * 0.22f
    drawLine(NauticalCyan, center - Offset(arm, 0f), center + Offset(arm, 0f), 3.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.anchor() {
    val w = size.width
    val h = size.height
    val stroke = 2.5.dp.toPx()
    drawCircle(NauticalCyan, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.26f), style = Stroke(stroke))
    drawLine(NauticalCyan, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.74f), stroke, StrokeCap.Round)
    drawLine(NauticalCyan, Offset(w * 0.38f, h * 0.4f), Offset(w * 0.62f, h * 0.4f), stroke, StrokeCap.Round)
    drawArc(NauticalCyan, 20f, 140f, useCenter = false, topLeft = Offset(w * 0.3f, h * 0.5f),
        size = Size(w * 0.4f, h * 0.26f), style = Stroke(stroke, cap = StrokeCap.Round))
}

private fun DrawScope.pin() {
    val r = size.minDimension * 0.16f
    val head = center - Offset(0f, r * 0.6f)
    val path = Path().apply {
        moveTo(head.x - r, head.y)
        quadraticTo(head.x - r, head.y + r * 1.4f, center.x, head.y + r * 2.6f)
        quadraticTo(head.x + r, head.y + r * 1.4f, head.x + r, head.y)
    }
    drawCircle(NauticalCyan, radius = r, center = head, style = Stroke(2.5.dp.toPx()))
    drawPath(path, NauticalCyan, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    drawCircle(NauticalCyan, radius = r * 0.35f, center = head)
}
