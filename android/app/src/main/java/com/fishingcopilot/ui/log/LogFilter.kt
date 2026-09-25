package com.fishingcopilot.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.catchlog.LogPeriod
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.SelectedContainer
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** "This year · Last year · All · Pick", the last showing a chosen year or range. */
@Composable
fun PeriodChips(period: LogPeriod, years: List<Int>, today: LocalDate, onPick: (LogPeriod) -> Unit, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    var picking by rememberSaveable { mutableStateOf(false) }
    var choosingDates by rememberSaveable { mutableStateOf(false) }
    val custom = period is LogPeriod.Year || period is LogPeriod.Range

    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodChip(stringResource(R.string.log_filter_this_year), period == LogPeriod.ThisYear) { onPick(LogPeriod.ThisYear) }
        PeriodChip(stringResource(R.string.log_filter_last_year), period == LogPeriod.LastYear) { onPick(LogPeriod.LastYear) }
        PeriodChip(stringResource(R.string.log_filter_all), period == LogPeriod.All) { onPick(LogPeriod.All) }
        PeriodChip(
            if (custom) periodLabel(period, today, locale) else stringResource(R.string.log_filter_pick),
            custom
        ) { picking = true }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.log_filter_pick_title)) },
            text = {
                Column {
                    years.forEach { year ->
                        DialogRow(year.toString(), selected = period == LogPeriod.Year(year)) {
                            onPick(LogPeriod.Year(year)); picking = false
                        }
                    }
                    DialogRow(stringResource(R.string.log_filter_custom), selected = period is LogPeriod.Range) {
                        picking = false; choosingDates = true
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.log_filter_cancel)) } },
            containerColor = OceanSurface
        )
    }
    if (choosingDates) {
        RangeDialog(
            onPick = { onPick(it); choosingDates = false },
            onDismiss = { choosingDates = false }
        )
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SelectedContainer,
            selectedLabelColor = NauticalCyan,
            labelColor = TextHighContrast
        )
    )
}

@Composable
private fun DialogRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) NauticalCyan else TextHighContrast,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeDialog(onPick: (LogPeriod) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDateRangePickerState()
    // The picker reports days as UTC midnight, whatever the phone's zone.
    fun day(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = state.selectedStartDateMillis
            TextButton(
                onClick = {
                    val end = state.selectedEndDateMillis ?: start
                    if (start != null && end != null) onPick(LogPeriod.Range(day(start), day(end)))
                },
                enabled = start != null
            ) { Text(stringResource(R.string.log_filter_apply), color = NauticalCyan) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.log_filter_cancel)) } }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    stringResource(R.string.log_filter_range_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/** The period in words for chips and titles: "2026", or "1 Jan 2026 – 5 Mar 2026". "All" has its own titles. */
fun periodLabel(period: LogPeriod, today: LocalDate, locale: Locale): String {
    val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return when (period) {
        LogPeriod.ThisYear -> today.year.toString()
        LogPeriod.LastYear -> (today.year - 1).toString()
        LogPeriod.All -> ""
        is LogPeriod.Year -> period.year.toString()
        is LogPeriod.Range -> {
            val (from, to) = minOf(period.from, period.to) to maxOf(period.from, period.to)
            if (from == to) from.format(dates) else "${from.format(dates)} – ${to.format(dates)}"
        }
    }
}

/** Sticky month header: "September 2026 · 12 catches". */
@Composable
fun MonthHeader(month: YearMonth, count: Int, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OceanMidnight)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$name ${month.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHighContrast
        )
        Text(
            text = "  ·  " + pluralStringResource(R.plurals.log_month_count, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
    }
}

/**
 * Handle on the right edge for long logs: dragging it jumps through the list and shows the month
 * under the finger. [anchors] maps each catch card's list index to its month.
 */
@Composable
fun BoxScope.FastScroller(listState: LazyListState, anchors: List<Pair<Int, YearMonth>>) {
    if (anchors.isEmpty()) return
    val locale = currentLocale()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var trackHeight by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf<Int?>(null) }
    val description = stringResource(R.string.log_fast_scroll_description)

    // Where the handle sits: the first visible catch among all catches. derivedStateOf keeps
    // recomposition to when that catch changes, not every scrolled pixel.
    val visibleAnchor by remember(anchors) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            anchors.indexOfFirst { it.first >= firstVisible }.let { if (it < 0) anchors.lastIndex else it }
        }
    }
    val current = dragging ?: visibleAnchor
    val fraction = if (anchors.size > 1) current.toFloat() / anchors.lastIndex else 0f
    val handleHeight = with(density) { 48.dp.toPx() }
    val y = ((trackHeight - handleHeight) * fraction).coerceAtLeast(0f)

    fun jump(offsetY: Float) {
        val f = ((offsetY - handleHeight / 2) / (trackHeight - handleHeight)).coerceIn(0f, 1f)
        val target = (f * anchors.lastIndex).roundToInt()
        dragging = target
        scope.launch { listState.scrollToItem(anchors[target].first) }
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(28.dp)
            .onSizeChanged { trackHeight = it.height.toFloat() }
            .semantics { contentDescription = description }
            .pointerInput(anchors) {
                detectVerticalDragGestures(
                    onDragStart = { jump(it.y) },
                    onDragEnd = { dragging = null },
                    onDragCancel = { dragging = null }
                ) { change, _ ->
                    change.consume()
                    jump(change.position.y)
                }
            }
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (dragging != null) NauticalCyan else TextMuted.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, y.roundToInt()) }
                .padding(end = 4.dp)
                .size(width = 6.dp, height = 48.dp)
        ) {}
    }
    dragging?.let { index ->
        val month = anchors[index].second
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NauticalCyan,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(-with(density) { 40.dp.roundToPx() }, y.roundToInt()) }
        ) {
            Text(
                text = month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale) + " " + month.year,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OceanMidnight,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
