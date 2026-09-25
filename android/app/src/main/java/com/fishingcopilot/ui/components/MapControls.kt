package com.fishingcopilot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.maps.LatLon
import com.fishingcopilot.maps.MapController
import com.fishingcopilot.maps.parseCoordinate
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextMuted

/** Zoom buttons big enough for wet fingers, plus an optional "back to my spot". */
@Composable
fun MapZoomControls(controller: MapController, modifier: Modifier = Modifier, onMySpot: (() -> Unit)? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapButton(stringResource(R.string.map_zoom_in), onClick = controller::zoomIn) { plus() }
        MapButton(stringResource(R.string.map_zoom_out), onClick = controller::zoomOut) { minus() }
        if (onMySpot != null) MapButton(stringResource(R.string.map_my_spot), onClick = onMySpot) { pin() }
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

/** Asks for a coordinate in any common form and returns it once it reads cleanly. */
@Composable
fun GoToCoordinateDialog(onGo: (LatLon) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    fun submit() {
        val parsed = parseCoordinate(text)
        if (parsed == null) invalid = true else onGo(parsed)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_go_to_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; invalid = false },
                    singleLine = true,
                    placeholder = { Text("5°21.0'N 103°10.2'E") },
                    isError = invalid,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() })
                )
                Text(
                    stringResource(if (invalid) R.string.map_go_to_invalid else R.string.map_go_to_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (invalid) MaterialTheme.colorScheme.error else TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.map_go_to_confirm), color = NauticalCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_go_to_cancel)) } },
        containerColor = OceanSurface
    )
}

@Composable
private fun MapButton(description: String, onClick: () -> Unit, icon: DrawScope.() -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = OceanMidnight.copy(alpha = 0.9f),
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
