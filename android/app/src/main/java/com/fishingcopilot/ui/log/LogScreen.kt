package com.fishingcopilot.ui.log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.astro.MoonPhaseName
import com.fishingcopilot.catchlog.LogSummary
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.ui.components.imageRes
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun LogScreen(state: LogUiState, onEdit: (LoggedCatch) -> Unit, onDelete: (CatchLogEntity) -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    var pendingDelete by remember { mutableStateOf<CatchLogEntity?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium, color = TextHighContrast)
        }
        val summary = state.summary
        if (summary == null) {
            item { EmptyLog() }
        } else {
            item { SummaryCard(summary) }
            items(state.catches, key = { it.entity.id }) { logged ->
                CatchCard(logged, locale, onEdit = { onEdit(logged) }, onDelete = { pendingDelete = logged.entity })
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
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.log_delete_cancel)) }
            },
            containerColor = OceanSurface
        )
    }
}

@Composable
private fun EmptyLog() {
    Surface(shape = RoundedCornerShape(20.dp), color = OceanSurface, border = BorderStroke(1.dp, OceanCardBorder)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(stringResource(R.string.log_empty_title), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.log_empty_body), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

@Composable
private fun SummaryCard(summary: LogSummary) {
    Surface(shape = RoundedCornerShape(20.dp), color = OceanSurface, border = BorderStroke(1.dp, OceanCardBorder)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.log_summary_title).uppercase(), style = MaterialTheme.typography.labelMedium, color = NauticalCyan)
            Text(
                pluralStringResource(R.plurals.log_summary_count, summary.count, summary.count),
                style = MaterialTheme.typography.headlineMedium,
                color = TextHighContrast
            )
            summary.topSpecies?.let {
                Text(
                    stringResource(R.string.log_summary_best_species, speciesName(it), summary.topSpeciesCount),
                    style = MaterialTheme.typography.bodyMedium, color = TextHighContrast
                )
            }
            summary.topBait?.let {
                Text(
                    stringResource(R.string.log_summary_top_bait, it, summary.topBaitPercent),
                    style = MaterialTheme.typography.bodyMedium, color = TextHighContrast
                )
            }
            summary.bestTideRising?.let { rising ->
                Text(
                    stringResource(
                        R.string.log_summary_best_tide,
                        stringResource(if (rising) R.string.log_tide_rising_phrase else R.string.log_tide_falling_phrase)
                    ),
                    style = MaterialTheme.typography.bodyMedium, color = TextHighContrast
                )
            }
        }
    }
}

@Composable
private fun CatchCard(logged: LoggedCatch, locale: Locale, onEdit: () -> Unit, onDelete: () -> Unit) {
    val log = logged.entity
    val name = speciesName(log.species)
    Surface(shape = RoundedCornerShape(18.dp), color = OceanSurface, border = BorderStroke(1.dp, OceanCardBorder)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            CatchThumbnail(log, name)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHighContrast)
                Text(
                    listOfNotNull(
                        Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)),
                        logged.spotName ?: if (log.spotId == null) stringResource(R.string.log_unknown_spot) else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                val sizes = listOfNotNull(
                    log.weightKg?.let { stringResource(R.string.log_weight_value, String.format(locale, "%.1f", it)) },
                    log.lengthCm?.let { stringResource(R.string.log_length_value, String.format(locale, "%.0f", it)) },
                    log.baitUsed?.let { stringResource(R.string.log_bait_value, it) }
                )
                if (sizes.isNotEmpty()) {
                    Text(sizes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
                }
                val conditions = listOfNotNull(
                    log.tideState?.let { stringResource(if (it == "RISING") R.string.tide_rising else R.string.tide_falling) },
                    log.moonPhase?.let { phase -> MoonPhaseName.entries.firstOrNull { it.name == phase }?.let { stringResource(it.label) } },
                    log.biteScore?.let { stringResource(R.string.log_score_value, String.format(locale, "%.1f", it)) }
                )
                if (conditions.isNotEmpty()) {
                    Text(conditions.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = NauticalCyan)
                }
                log.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted) }
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.log_edit), color = NauticalCyan) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.log_delete), color = TextMuted) }
            }
        }
    }
}

@Composable
private fun CatchThumbnail(log: CatchLogEntity, name: String) {
    val modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
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
        else -> Box(modifier = modifier.background(OceanMidnight))
    }
}

/** Picked species are stored as the enum name and shown in the user's language; typed ones as written. */
@Composable
private fun speciesName(stored: String): String =
    Species.entries.firstOrNull { it.name == stored }?.let { stringResource(it.label) } ?: stored
