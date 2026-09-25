package com.fishingcopilot.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import com.fishingcopilot.warnings.ShownWarning
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Bell beside the settings gear that opens [WarningsScreen]. A red count shows warnings naming the
 * spot's area; the spoken label says the same in words.
 */
@Composable
fun WarningBell(state: WarningState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val count = (state as? WarningState.Ready)?.summary?.forSpot?.size ?: 0
    val label = when {
        count > 0 -> pluralStringResource(R.plurals.warning_for_spot_title, count, count)
        state is WarningState.Unknown && state.failed -> stringResource(R.string.warning_error_offline)
        else -> stringResource(R.string.warning_none_for_spot)
    }
    IconButton(onClick = onOpen, modifier = modifier.semantics { contentDescription = label }) {
        Box {
            Canvas(modifier = Modifier.size(24.dp)) {
                val tint = if (count > 0) AlertRed else TextMuted
                val w = size.width
                val h = size.height
                val bell = Path().apply {
                    moveTo(w * 0.2f, h * 0.72f)
                    lineTo(w * 0.8f, h * 0.72f)
                    lineTo(w * 0.72f, h * 0.6f)
                    lineTo(w * 0.72f, h * 0.42f)
                    cubicTo(w * 0.72f, h * 0.18f, w * 0.28f, h * 0.18f, w * 0.28f, h * 0.42f)
                    lineTo(w * 0.28f, h * 0.6f)
                    close()
                }
                drawPath(bell, tint, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.2f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawArc(tint, 0f, 180f, useCenter = false, topLeft = Offset(w * 0.42f, h * 0.74f), size = Size(w * 0.16f, h * 0.12f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = AlertRed,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp).size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (count > 9) "9+" else count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** Every current MetMalaysia warning, in MetMalaysia's own words; the spot's area first. */
@Composable
fun WarningsScreen(state: WarningState, onRetry: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val locale = LocalConfiguration.current.locales[0]
    val malay = locale.language == "ms"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back), color = NauticalCyan) }
        Text(stringResource(R.string.warnings_title), style = MaterialTheme.typography.headlineMedium, color = TextHighContrast)
        when (state) {
            is WarningState.Unknown -> if (state.failed) {
                Text(stringResource(R.string.warning_error_offline), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.warning_retry), color = NauticalCyan) }
            } else {
                CircularProgressIndicator(color = NauticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            is WarningState.Ready -> {
                val summary = state.summary
                if (summary.forSpot.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.warning_for_spot_title, summary.forSpot.size, summary.forSpot.size).uppercase(locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = AlertRed
                    )
                    summary.forSpot.forEach { WarningItem(it, malay, locale, AlertRed, startExpanded = true) }
                } else {
                    Text(stringResource(R.string.warning_none_for_spot), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                if (summary.elsewhere.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        pluralStringResource(R.plurals.warning_elsewhere, summary.elsewhere.size, summary.elsewhere.size).uppercase(locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = CautionYellow
                    )
                    summary.elsewhere.forEach { WarningItem(it, malay, locale, CautionYellow, startExpanded = false) }
                }
                Footer(state, locale)
            }
        }
    }
}

@Composable
private fun WarningItem(warning: ShownWarning, malay: Boolean, locale: Locale, accent: Color, startExpanded: Boolean) {
    var expanded by rememberSaveable(warning.textEn) { mutableStateOf(startExpanded) }
    // Only offer "read all" when the collapsed text was actually cut short.
    var truncated by remember(warning.textEn) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = OceanSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .animateContentSize()
                .padding(12.dp)
        ) {
            Text(
                if (malay) warning.headingBm else warning.headingEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (malay) warning.textBm else warning.textEn,
                style = MaterialTheme.typography.bodySmall,
                color = TextHighContrast,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) truncated = it.hasVisualOverflow }
            )
            val instruction = if (malay) warning.instructionBm else warning.instructionEn
            if (expanded && instruction != null) {
                Spacer(Modifier.height(4.dp))
                Text(instruction, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextHighContrast)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.warning_issued, dateTime(warning.issued, locale)),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            if (!expanded && truncated) {
                Text(stringResource(R.string.warning_read_more), style = MaterialTheme.typography.labelSmall, color = NauticalCyan)
            }
        }
    }
}

@Composable
private fun Footer(state: WarningState.Ready, locale: Locale) {
    Text(
        stringResource(R.string.warning_source, dateTime(Instant.ofEpochMilli(state.fetchedAtMillis), locale)),
        style = MaterialTheme.typography.labelSmall,
        color = if (state.stale) CautionYellow else TextMuted
    )
}

private fun dateTime(instant: Instant, locale: Locale): String =
    instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
