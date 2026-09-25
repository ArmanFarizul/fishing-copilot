package com.fishingcopilot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.theme.TextMuted

/** Small ✕ for clearing a time picked on a chart, with a full-size touch target and a spoken label. */
@Composable
fun ClearSelectionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.chart_clear_selection)
    IconButton(onClick = onClick, modifier = modifier.semantics { contentDescription = label }) {
        Canvas(Modifier.size(14.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(TextMuted, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(TextMuted, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}
