package com.fishingcopilot.ui.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.components.biteBandColor
import com.fishingcopilot.ui.components.signedMeters
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.ui.components.hijriDateText
import com.fishingcopilot.ui.components.imageRes
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.InsetBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.SelectedContainer
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Custom species typed by the user are stored as text; picked ones as the Species enum name. */
private const val OTHER = "__other__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCatchSheet(
    conditions: CatchConditions,
    targetSpecies: Set<Species>,
    recentBaits: List<String>,
    onSave: (species: String, bait: String?, weightKg: Double?, lengthCm: Double?, photo: String?, notes: String?) -> Unit,
    onDismiss: () -> Unit,
    /** A saved catch to edit; its details fill the form. Null for a new catch. */
    existing: CatchLogEntity? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val picked = existing?.species?.takeIf { stored -> Species.entries.any { it.name == stored } }
    var species by rememberSaveable { mutableStateOf(existing?.let { picked ?: OTHER }) }
    var otherSpecies by rememberSaveable { mutableStateOf(if (existing != null && picked == null) existing.species else "") }
    var bait by rememberSaveable { mutableStateOf(existing?.baitUsed.orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(existing?.weightKg.toField()) }
    var length by rememberSaveable { mutableStateOf(existing?.lengthCm.toField()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var photo by rememberSaveable { mutableStateOf(existing?.photoUri) }
    var showSpeciesError by rememberSaveable { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = uri.toString()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OceanMidnight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(if (existing == null) R.string.catch_sheet_title else R.string.catch_edit_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast
                )
            }

            Spacer(Modifier.height(14.dp))

            // Signature Strike Telemetry Hub
            ConditionsPanel(conditions = conditions, locale = locale, saved = existing != null)

            Spacer(Modifier.height(20.dp))

            // Section: Species Picker with 3D Artwork
            SectionLabel(R.string.catch_species_label)
            val ordered = targetSpecies.toList().sortedBy { it.ordinal } + Species.entries.filterNot { it in targetSpecies }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ordered.forEach { option ->
                    SpeciesChip(
                        selected = species == option.name,
                        label = stringResource(option.label),
                        imageRes = option.imageRes,
                        onClick = {
                            species = option.name
                            showSpeciesError = false
                        }
                    )
                }
                OtherSpeciesChip(
                    selected = species == OTHER,
                    label = stringResource(R.string.catch_species_other),
                    onClick = {
                        species = OTHER
                        showSpeciesError = false
                    }
                )
            }

            if (species == OTHER) {
                Spacer(Modifier.height(8.dp))
                FormField(
                    value = otherSpecies,
                    onChange = { otherSpecies = it },
                    labelRes = R.string.catch_species_other_hint,
                    capitalize = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showSpeciesError) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.catch_species_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(18.dp))

            // Section: Bait
            SectionLabel(R.string.catch_bait_label)
            if (recentBaits.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.catch_bait_recent),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentBaits.forEach { recent ->
                        val isRecentSelected = bait == recent
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRecentSelected) SelectedContainer else OceanSurface,
                            border = if (isRecentSelected) BorderStroke(1.dp, NauticalCyan) else InsetBorder,
                            modifier = Modifier.clickable { bait = recent }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BaitIcon(color = if (isRecentSelected) NauticalCyan else TextMuted, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = recent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isRecentSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isRecentSelected) NauticalCyan else TextHighContrast
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            FormField(
                value = bait,
                onChange = { bait = it },
                labelRes = R.string.catch_bait_hint,
                leadingIcon = { BaitIcon(color = NauticalCyan, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Section: Weight & Length
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FormField(
                    value = weight,
                    onChange = { weight = it },
                    labelRes = R.string.catch_weight_label,
                    numeric = true,
                    leadingIcon = { ScaleIcon(color = NauticalCyan, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FormField(
                    value = length,
                    onChange = { length = it },
                    labelRes = R.string.catch_length_label,
                    numeric = true,
                    leadingIcon = { RulerIcon(color = NauticalCyan, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Section: Notes
            FormField(
                value = notes,
                onChange = { notes = it },
                labelRes = R.string.catch_notes_label,
                capitalize = true,
                singleLine = false,
                leadingIcon = { MemoIcon(color = NauticalCyan, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            // Section: Catch Photo Attachment
            val currentPhoto = photo
            if (currentPhoto != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OceanSurface,
                    border = InsetBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitmap = rememberImageBitmap(currentPhoto, maxSizePx = 320)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OceanMidnight),
                                contentAlignment = Alignment.Center
                            ) {
                                CameraIcon(color = NauticalCyan, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CameraIcon(color = NauticalCyan, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.catch_photo_change), color = NauticalCyan, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = { photo = null }) {
                                Text(stringResource(R.string.catch_photo_remove), color = AlertRed)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OceanSurface,
                    border = InsetBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SelectedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraIcon(color = NauticalCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.catch_photo_add),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NauticalCyan
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.catch_photo_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Footer Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.catch_cancel), color = TextMuted)
                }

                Button(
                    onClick = {
                        val chosen = if (species == OTHER) otherSpecies.trim() else species
                        if (chosen.isNullOrEmpty()) {
                            showSpeciesError = true
                        } else {
                            onSave(chosen, bait, weight.toDecimal(), length.toDecimal(), photo, notes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NauticalCyan,
                        contentColor = OceanMidnight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckIcon(color = OceanMidnight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(if (existing == null) R.string.catch_save else R.string.catch_edit_save),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Strike Telemetry Card: Shows automatic live environmental conditions captured at the strike instant.
 */
@Composable
private fun ConditionsPanel(conditions: CatchConditions, locale: Locale, saved: Boolean) {
    val none = stringResource(R.string.catch_no_data)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = OceanSurface,
        border = CardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header with strike badge & lock indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LightningIcon(color = NauticalCyan, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.catch_autofill_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NauticalCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OceanMidnight,
                    border = InsetBorder
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LockIcon(color = TextMuted, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.catch_telemetry_locked).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Time & Spot Strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OceanMidnight,
                border = InsetBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                        ClockIcon(color = NauticalCyan, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        val formattedTime = Instant.ofEpochMilli(conditions.timestamp).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextHighContrast
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        PinIcon(color = NauticalCyan, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = conditions.spotName ?: none,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Telemetry Grid: Tide & Moon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tide & Water Level Tile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OceanMidnight,
                    border = InsetBorder,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TideFlowIcon(
                                rising = conditions.rising == true,
                                color = NauticalCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.catch_tide_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = conditions.rising?.let { stringResource(if (it) R.string.tide_rising else R.string.tide_falling) } ?: none,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast
                        )
                        conditions.waterLevel?.let { wl ->
                            Text(
                                text = stringResource(R.string.catch_water_value, signedMeters(wl, locale)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NauticalCyan
                            )
                        }
                    }
                }

                // Moon & Hijri Tile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OceanMidnight,
                    border = InsetBorder,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MoonPhaseMiniIcon(
                                phaseName = conditions.moonPhase?.name,
                                color = NauticalCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.catch_moon_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = conditions.moonPhase?.let { stringResource(it.label) } ?: none,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        conditions.hijri?.let { hj ->
                            Text(
                                text = hijriDateText(hj),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bite Score Banner & Explanatory Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OceanMidnight,
                border = InsetBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(if (saved) R.string.catch_autofill_note_saved else R.string.catch_autofill_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    conditions.biteScore?.let { scoreVal ->
                        // Same band colours as the Bite Score card, so 6.1 reads "good" here too.
                        val badgeText = biteBandColor(scoreVal)
                        val badgeBg = badgeText.copy(alpha = 0.2f)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, badgeText.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LightningIcon(color = badgeText, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.catch_score_label) + " " + String.format(locale, "%.1f", scoreVal),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tactile, interactive chip for a species with its rich 3D artwork.
 */
@Composable
private fun SpeciesChip(
    selected: Boolean,
    label: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) SelectedContainer else OceanSurface,
        border = if (selected) BorderStroke(1.5.dp, NauticalCyan) else InsetBorder,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) NauticalCyan else TextHighContrast
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                CheckIcon(color = NauticalCyan, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/**
 * Tactile chip for "Other" species option.
 */
@Composable
private fun OtherSpeciesChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) SelectedContainer else OceanSurface,
        border = if (selected) BorderStroke(1.5.dp, NauticalCyan) else InsetBorder,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) NauticalCyan else TextHighContrast
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                CheckIcon(color = NauticalCyan, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextHighContrast
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    labelRes: Int,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    capitalize: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
            capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
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
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(top = 4.dp)
    )
}

/** 2.0 shows as "2" and 2.45 as "2.45", ready to edit. */
private fun Double?.toField(): String = this?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()

/** Accepts both "2.4" and "2,4". */
private fun String.toDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

// ==========================================
// TACTICAL VECTOR ICONS DRAWN IN COMPOSE
// ==========================================

@Composable
private fun LockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Shackle
        drawPath(
            Path().apply {
                moveTo(w * 0.3f, h * 0.5f)
                lineTo(w * 0.3f, h * 0.32f)
                cubicTo(w * 0.3f, h * 0.12f, w * 0.7f, h * 0.12f, w * 0.7f, h * 0.32f)
                lineTo(w * 0.7f, h * 0.5f)
            },
            color = color,
            style = stroke
        )
        // Body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.2f, h * 0.48f),
            size = Size(w * 0.6f, h * 0.44f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = stroke
        )
    }
}

@Composable
private fun ClockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 1.4.dp.toPx())
        drawCircle(color, radius = r - 1.dp.toPx(), center = center, style = stroke)
        drawLine(color, center, Offset(center.x, center.y - r * 0.5f), 1.4.dp.toPx(), StrokeCap.Round)
        drawLine(color, center, Offset(center.x + r * 0.38f, center.y), 1.4.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun ScaleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Beam
        drawLine(color, Offset(w * 0.2f, h * 0.35f), Offset(w * 0.8f, h * 0.35f), 1.4.dp.toPx(), StrokeCap.Round)
        // Center stand
        drawLine(color, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.85f), 1.5.dp.toPx(), StrokeCap.Round)
        // Base
        drawLine(color, Offset(w * 0.32f, h * 0.85f), Offset(w * 0.68f, h * 0.85f), 1.6.dp.toPx(), StrokeCap.Round)
        // Left pan
        drawLine(color, Offset(w * 0.25f, h * 0.35f), Offset(w * 0.15f, h * 0.55f), 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w * 0.25f, h * 0.35f), Offset(w * 0.35f, h * 0.55f), 1.2.dp.toPx(), StrokeCap.Round)
        drawArc(color, 0f, 180f, false, Offset(w * 0.12f, h * 0.52f), Size(w * 0.26f, h * 0.14f), style = stroke)
        // Right pan
        drawLine(color, Offset(w * 0.75f, h * 0.35f), Offset(w * 0.65f, h * 0.55f), 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(w * 0.75f, h * 0.35f), Offset(w * 0.85f, h * 0.55f), 1.2.dp.toPx(), StrokeCap.Round)
        drawArc(color, 0f, 180f, false, Offset(w * 0.62f, h * 0.52f), Size(w * 0.26f, h * 0.14f), style = stroke)
    }
}

@Composable
private fun RulerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.3f),
            size = Size(w * 0.7f, h * 0.4f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = stroke
        )
        // Ticks
        listOf(0.3f, 0.45f, 0.6f, 0.72f).forEachIndexed { i, x ->
            val tickH = if (i % 2 == 0) 0.55f else 0.48f
            drawLine(color, Offset(w * x, h * 0.3f), Offset(w * x, h * tickH), 1.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun CameraIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.3f),
            size = Size(w * 0.76f, h * 0.55f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = stroke
        )
        drawPath(
            Path().apply {
                moveTo(w * 0.32f, h * 0.3f)
                lineTo(w * 0.38f, h * 0.18f)
                lineTo(w * 0.62f, h * 0.18f)
                lineTo(w * 0.68f, h * 0.3f)
            },
            color = color,
            style = stroke
        )
        drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.58f), style = stroke)
    }
}

@Composable
private fun CheckIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(
            Path().apply {
                moveTo(w * 0.2f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.74f)
                lineTo(w * 0.82f, h * 0.28f)
            },
            color = color,
            style = stroke
        )
    }
}
