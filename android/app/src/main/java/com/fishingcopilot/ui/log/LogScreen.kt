package com.fishingcopilot.ui.log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.astro.MoonPhaseName
import com.fishingcopilot.catchlog.LogSummary
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.ui.components.imageRes
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.InsetBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.SelectedContainer
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun LogScreen(
    state: LogUiState,
    onEdit: (LoggedCatch) -> Unit,
    onDelete: (CatchLogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    var pendingDelete by remember { mutableStateOf<CatchLogEntity?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.log_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.log_summary_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = NauticalCyan
                )
            }
        }
        val summary = state.summary
        if (summary == null) {
            item { EmptyLog() }
        } else {
            item { SummaryCard(summary) }
            items(state.catches, key = { it.entity.id }) { logged ->
                CatchCard(
                    logged = logged,
                    locale = locale,
                    onEdit = { onEdit(logged) },
                    onDelete = { pendingDelete = logged.entity }
                )
            }
        }
    }

    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.log_delete_title)) },
            text = { Text(stringResource(R.string.log_delete_body)) },
            confirmButton = {
                TextButton(onClick = { onDelete(log); pendingDelete = null }) {
                    Text(stringResource(R.string.log_delete_confirm), color = AlertRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.log_delete_cancel))
                }
            },
            containerColor = OceanSurface
        )
    }
}

/**
 * Inspiring, beautifully illustrated empty logbook state with nautical ripples and float.
 */
@Composable
private fun EmptyLog() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = OceanSurface,
        border = CardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calm water ripple and fishing float graphic
            Canvas(modifier = Modifier.size(140.dp, 84.dp)) {
                val cx = size.width / 2f
                val cy = size.height * 0.62f
                val cyan = NauticalCyan

                // Expanding ripple rings
                drawOval(
                    color = cyan.copy(alpha = 0.15f),
                    topLeft = Offset(cx - 58.dp.toPx(), cy - 14.dp.toPx()),
                    size = Size(116.dp.toPx(), 28.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawOval(
                    color = cyan.copy(alpha = 0.35f),
                    topLeft = Offset(cx - 36.dp.toPx(), cy - 9.dp.toPx()),
                    size = Size(72.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Waterline calm wave
                val wavePath = Path().apply {
                    moveTo(cx - 64.dp.toPx(), cy)
                    cubicTo(cx - 30.dp.toPx(), cy - 3.dp.toPx(), cx - 15.dp.toPx(), cy + 3.dp.toPx(), cx, cy)
                    cubicTo(cx + 15.dp.toPx(), cy - 3.dp.toPx(), cx + 30.dp.toPx(), cy + 3.dp.toPx(), cx + 64.dp.toPx(), cy)
                }
                drawPath(wavePath, cyan.copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

                // Float / Bobber body
                val floatR = 10.dp.toPx()
                // Top half (bright orange/red)
                drawArc(
                    color = AlertRed,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - floatR, cy - floatR),
                    size = Size(floatR * 2, floatR * 2)
                )
                // Bottom half (white/bright)
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - floatR, cy - floatR),
                    size = Size(floatR * 2, floatR * 2)
                )
                // Outline keeps the white half of the float visible on the white sunlight background.
                drawCircle(
                    color = TextMuted,
                    radius = floatR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Antenna stem
                drawLine(
                    color = CautionYellow,
                    start = Offset(cx, cy - floatR),
                    end = Offset(cx, cy - floatR - 14.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Antenna top bead
                drawCircle(
                    color = AlertRed,
                    radius = 3.dp.toPx(),
                    center = Offset(cx, cy - floatR - 14.dp.toPx())
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.log_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighContrast,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.log_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))
            // Quick hint chip pointing to the Strike feature
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OceanMidnight,
                border = InsetBorder
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LightningIcon(color = NauticalCyan, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.log_empty_hint),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = NauticalCyan
                    )
                }
            }
        }
    }
}

/**
 * Tactical angler summary dashboard with live telemetry insights, top species, top bait, and best tide.
 */
@Composable
private fun SummaryCard(summary: LogSummary) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = OceanSurface,
        border = CardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Eyebrow + Hero Catch Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrophyIcon(color = NauticalCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.log_summary_title).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NauticalCyan
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = summary.count.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = TextHighContrast
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pluralStringResource(R.plurals.log_summary_count, summary.count, summary.count)
                                .substringAfter(summary.count.toString()).trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHighContrast,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Decorative trophy circle container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SelectedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    TrophyIcon(color = NauticalCyan, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tactical Analytics Tiles: Top Species & Top Bait
            val topSpecies = summary.topSpecies
            val topBait = summary.topBait

            if (topSpecies != null || topBait != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (topSpecies != null) {
                        val speciesObj = Species.entries.firstOrNull { it.name == topSpecies }
                        val dispName = speciesObj?.let { stringResource(it.label) } ?: topSpecies

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OceanMidnight,
                            border = InsetBorder,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.log_summary_species_label).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
                                    if (speciesObj != null) {
                                        Image(
                                            painter = painterResource(speciesObj.imageRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        FishSilhouetteIcon(color = NauticalCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = dispName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHighContrast,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = pluralStringResource(R.plurals.log_summary_species_count, summary.topSpeciesCount, summary.topSpeciesCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NauticalCyan
                                )
                            }
                        }
                    }

                    if (topBait != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OceanMidnight,
                            border = InsetBorder,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.log_summary_bait_label).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
                                    BaitIcon(color = PrimeGreen, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = topBait,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHighContrast,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.log_summary_bait_rate, summary.topBaitPercent),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimeGreen
                                )
                            }
                        }
                    }
                }
            }

            // Best Tide Insight Banner
            summary.bestTideRising?.let { rising ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OceanMidnight,
                    border = InsetBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SelectedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            TideFlowIcon(rising = rising, color = NauticalCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.log_summary_tide_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NauticalCyan
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = stringResource(
                                    if (rising) R.string.log_summary_best_tide_rising else R.string.log_summary_best_tide_falling
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextHighContrast
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual catch card with 3D thumbnail, physical specifications, live strike telemetry strip,
 * and top-right tactile edit/delete actions.
 */
@Composable
private fun CatchCard(
    logged: LoggedCatch,
    locale: Locale,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val log = logged.entity
    val name = speciesName(log.species)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OceanSurface,
        border = CardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Thumbnail + Specs + Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Photo / 3D species avatar with weight badge overlay
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OceanMidnight)
                ) {
                    CatchThumbnail(log = log, name = name, modifier = Modifier.fillMaxSize())

                    // Weight badge overlay on thumbnail if weight exists
                    log.weightKg?.let { kg ->
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp),
                            color = OceanMidnight.copy(alpha = 0.92f),
                            border = InsetBorder,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Text(
                                text = stringResource(R.string.log_weight_value, String.format(locale, "%.1f", kg)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NauticalCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Catch Details & Top Actions
                Column(modifier = Modifier.weight(1f)) {
                    // Species Name & Compact Top-Right Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast,
                            modifier = Modifier.weight(1f)
                        )

                        // Tactile Edit & Delete Icon Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val editLabel = stringResource(R.string.log_edit)
                            val deleteLabel = stringResource(R.string.log_delete)
                            // 48 dp: easy to hit with wet hands, and hard to hit the wrong one.
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SelectedContainer)
                                    .clickable(role = Role.Button, onClick = onEdit)
                                    .semantics { contentDescription = editLabel },
                                contentAlignment = Alignment.Center
                            ) {
                                EditIcon(color = NauticalCyan, modifier = Modifier.size(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AlertRed.copy(alpha = 0.12f))
                                    .clickable(role = Role.Button, onClick = onDelete)
                                    .semantics { contentDescription = deleteLabel },
                                contentAlignment = Alignment.Center
                            ) {
                                DeleteIcon(color = AlertRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Spot Name (with pin icon)
                    val spotText = logged.spotName ?: if (log.spotId == null) stringResource(R.string.log_unknown_spot) else null
                    if (spotText != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PinIcon(color = NauticalCyan, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = spotText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = TextHighContrast,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                    }

                    // Timestamp
                    val dateFormatted = Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Spacer(Modifier.height(6.dp))

                    // Physical metrics chips (Length, Bait)
                    val lengthVal = log.lengthCm
                    val baitVal = log.baitUsed
                    if (lengthVal != null || baitVal != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (lengthVal != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = OceanMidnight,
                                    border = InsetBorder
                                ) {
                                    Text(
                                        text = stringResource(R.string.log_length_value, String.format(locale, "%.0f", lengthVal)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = TextHighContrast,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (baitVal != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = OceanMidnight,
                                    border = InsetBorder
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BaitIcon(color = NauticalCyan, modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = baitVal,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = TextHighContrast
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Signature Strike Environmental Strip (Recorded at moment of strike)
            val tideState = log.tideState
            val moonPhaseStr = log.moonPhase
            val scoreVal = log.biteScore

            if (tideState != null || moonPhaseStr != null || scoreVal != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OceanMidnight,
                    border = InsetBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LightningIcon(color = NauticalCyan, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.log_strike_conditions_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NauticalCyan
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tide state badge
                            if (tideState != null) {
                                val isRising = tideState == "RISING"
                                val tideLabel = stringResource(if (isRising) R.string.tide_rising else R.string.tide_falling)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TideFlowIcon(rising = isRising, color = NauticalCyan, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = tideLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextHighContrast
                                    )
                                    log.waterLevel?.let { wl ->
                                        Text(
                                            text = " (${signedMeters(wl, locale)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            // Moon phase badge
                            if (moonPhaseStr != null) {
                                val phaseObj = MoonPhaseName.entries.firstOrNull { it.name == moonPhaseStr }
                                val phaseLabel = phaseObj?.let { stringResource(it.label) } ?: moonPhaseStr
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MoonPhaseMiniIcon(phaseName = moonPhaseStr, color = NauticalCyan, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = phaseLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextHighContrast,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            // Bite score badge
                            if (scoreVal != null) {
                                val (badgeBg, badgeText) = when {
                                    scoreVal >= 7.0 -> PrimeGreen.copy(alpha = 0.2f) to PrimeGreen
                                    scoreVal >= 4.0 -> CautionYellow.copy(alpha = 0.2f) to CautionYellow
                                    else -> NauticalCyan.copy(alpha = 0.2f) to NauticalCyan
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBg,
                                    border = BorderStroke(1.dp, badgeText.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.log_score_value, String.format(locale, "%.1f", scoreVal)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notes Bubble
            log.notes?.let { notesText ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OceanMidnight.copy(alpha = 0.6f),
                    border = InsetBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        MemoIcon(color = NauticalCyan, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = notesText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatchThumbnail(log: CatchLogEntity, name: String, modifier: Modifier = Modifier) {
    val photo = log.photoUri?.let { rememberImageBitmap(it, maxSizePx = 240) }
    val species = Species.entries.firstOrNull { it.name == log.species }
    when {
        photo != null -> Image(
            bitmap = photo,
            contentDescription = stringResource(R.string.log_photo_description, name),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        species != null -> Image(
            painter = painterResource(species.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        else -> Box(modifier = modifier.background(OceanMidnight), contentAlignment = Alignment.Center) {
            FishSilhouetteIcon(color = NauticalCyan, modifier = Modifier.size(36.dp))
        }
    }
}

/** "+0.4 m"; levels that round to zero read "+0.0 m" rather than "-0.0 m", as on the tide card. */
private fun signedMeters(height: Double, locale: Locale): String =
    String.format(locale, "%+.1f m", if (abs(height) < 0.05) 0.0 else height)

/** Picked species are stored as the enum name and shown in the user's language; typed ones as written. */
@Composable
private fun speciesName(stored: String): String =
    Species.entries.firstOrNull { it.name == stored }?.let { stringResource(it.label) } ?: stored

// ==========================================
// TACTICAL VECTOR ICONS DRAWN IN COMPOSE
// ==========================================

@Composable
private fun EditIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.72f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.28f)
                lineTo(w * 0.35f, h * 0.78f)
                lineTo(w * 0.18f, h * 0.82f)
                lineTo(w * 0.22f, h * 0.65f)
                close()
            },
            color = color,
            style = stroke
        )
        drawLine(color, Offset(w * 0.62f, h * 0.25f), Offset(w * 0.75f, h * 0.38f), 1.3.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun DeleteIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Can body
        drawPath(
            Path().apply {
                moveTo(w * 0.25f, h * 0.32f)
                lineTo(w * 0.3f, h * 0.85f)
                cubicTo(w * 0.3f, h * 0.88f, w * 0.35f, h * 0.88f, w * 0.5f, h * 0.88f)
                cubicTo(w * 0.65f, h * 0.88f, w * 0.7f, h * 0.88f, w * 0.7f, h * 0.85f)
                lineTo(w * 0.75f, h * 0.32f)
            },
            color = color,
            style = stroke
        )
        // Lid
        drawLine(color, Offset(w * 0.18f, h * 0.32f), Offset(w * 0.82f, h * 0.32f), 1.6.dp.toPx(), StrokeCap.Round)
        // Handle
        drawPath(
            Path().apply {
                moveTo(w * 0.38f, h * 0.32f)
                lineTo(w * 0.38f, h * 0.2f)
                lineTo(w * 0.62f, h * 0.2f)
                lineTo(w * 0.62f, h * 0.32f)
            },
            color = color,
            style = stroke
        )
        // Ribs
        drawLine(color, Offset(w * 0.44f, h * 0.44f), Offset(w * 0.45f, h * 0.74f), 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w * 0.56f, h * 0.44f), Offset(w * 0.55f, h * 0.74f), 1.2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun TideFlowIcon(rising: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Water wave
        drawPath(
            Path().apply {
                moveTo(w * 0.12f, h * 0.68f)
                cubicTo(w * 0.25f, h * 0.52f, w * 0.4f, h * 0.82f, w * 0.6f, h * 0.68f)
                cubicTo(w * 0.75f, h * 0.56f, w * 0.85f, h * 0.72f, w * 0.9f, h * 0.68f)
            },
            color = color,
            style = stroke
        )
        // Arrow indicating flow
        if (rising) {
            drawPath(
                Path().apply {
                    moveTo(w * 0.35f, h * 0.48f)
                    lineTo(w * 0.68f, h * 0.2f)
                    moveTo(w * 0.48f, h * 0.18f)
                    lineTo(w * 0.7f, h * 0.18f)
                    lineTo(w * 0.7f, h * 0.4f)
                },
                color = color,
                style = stroke
            )
        } else {
            drawPath(
                Path().apply {
                    moveTo(w * 0.35f, h * 0.22f)
                    lineTo(w * 0.68f, h * 0.5f)
                    moveTo(w * 0.48f, h * 0.52f)
                    lineTo(w * 0.7f, h * 0.52f)
                    lineTo(w * 0.7f, h * 0.3f)
                },
                color = color,
                style = stroke
            )
        }
    }
}

@Composable
private fun PinIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.5f, h * 0.9f)
                cubicTo(w * 0.2f, h * 0.55f, w * 0.22f, h * 0.18f, w * 0.5f, h * 0.18f)
                cubicTo(w * 0.78f, h * 0.18f, w * 0.8f, h * 0.55f, w * 0.5f, h * 0.9f)
                close()
            },
            color = color,
            style = stroke
        )
        drawCircle(color, radius = w * 0.11f, center = Offset(w * 0.5f, h * 0.42f), style = stroke)
    }
}

@Composable
private fun BaitIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Fishing hook shape
        drawPath(
            Path().apply {
                moveTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.5f, h * 0.62f)
                cubicTo(w * 0.5f, h * 0.86f, w * 0.2f, h * 0.86f, w * 0.2f, h * 0.65f)
                cubicTo(w * 0.2f, h * 0.52f, w * 0.26f, h * 0.42f, w * 0.34f, h * 0.45f)
                lineTo(w * 0.3f, h * 0.56f)
            },
            color = color,
            style = stroke
        )
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.16f), style = stroke)
    }
}

@Composable
private fun TrophyIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.26f, h * 0.2f)
                lineTo(w * 0.74f, h * 0.2f)
                lineTo(w * 0.7f, h * 0.52f)
                cubicTo(w * 0.7f, h * 0.68f, w * 0.58f, h * 0.72f, w * 0.5f, h * 0.72f)
                cubicTo(w * 0.42f, h * 0.72f, w * 0.3f, h * 0.68f, w * 0.3f, h * 0.52f)
                close()
            },
            color = color,
            style = stroke
        )
        drawLine(color, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.84f), 1.6.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.85f), Offset(w * 0.68f, h * 0.85f), 1.8.dp.toPx(), StrokeCap.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.26f, h * 0.26f)
                cubicTo(w * 0.12f, h * 0.26f, w * 0.12f, h * 0.48f, w * 0.28f, h * 0.48f)
                moveTo(w * 0.74f, h * 0.26f)
                cubicTo(w * 0.88f, h * 0.26f, w * 0.88f, h * 0.48f, w * 0.72f, h * 0.48f)
            },
            color = color,
            style = stroke
        )
    }
}

@Composable
private fun MoonPhaseMiniIcon(phaseName: String?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 1.3.dp.toPx())
        drawCircle(color.copy(alpha = 0.3f), radius = r - 1.dp.toPx(), center = center, style = stroke)
        when (phaseName) {
            "FULL_MOON" -> drawCircle(color, radius = r - 1.dp.toPx(), center = center)
            "NEW_MOON" -> drawCircle(color.copy(alpha = 0.5f), radius = r - 1.dp.toPx(), center = center, style = stroke)
            else -> {
                drawArc(
                    color = color,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(center.x - r + 1.dp.toPx(), center.y - r + 1.dp.toPx()),
                    size = Size((r - 1.dp.toPx()) * 2, (r - 1.dp.toPx()) * 2)
                )
            }
        }
    }
}

@Composable
private fun LightningIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        drawPath(
            Path().apply {
                moveTo(w * 0.55f, h * 0.1f)
                lineTo(w * 0.25f, h * 0.52f)
                lineTo(w * 0.52f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.9f)
                lineTo(w * 0.75f, h * 0.45f)
                lineTo(w * 0.48f, h * 0.45f)
                close()
            },
            color = color
        )
    }
}

@Composable
private fun MemoIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.18f, h * 0.15f),
            size = Size(w * 0.64f, h * 0.72f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = stroke
        )
        drawLine(color, Offset(w * 0.32f, h * 0.38f), Offset(w * 0.68f, h * 0.38f), 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.55f), Offset(w * 0.68f, h * 0.55f), 1.2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun FishSilhouetteIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.15f, h * 0.5f)
                cubicTo(w * 0.3f, h * 0.25f, w * 0.65f, h * 0.28f, w * 0.85f, h * 0.5f)
                cubicTo(w * 0.65f, h * 0.72f, w * 0.3f, h * 0.75f, w * 0.15f, h * 0.5f)
                close()
                // Tail fin
                moveTo(w * 0.85f, h * 0.5f)
                lineTo(w * 0.95f, h * 0.32f)
                lineTo(w * 0.9f, h * 0.5f)
                lineTo(w * 0.95f, h * 0.68f)
                close()
            },
            color = color,
            style = stroke
        )
        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.3f, h * 0.44f))
    }
}
