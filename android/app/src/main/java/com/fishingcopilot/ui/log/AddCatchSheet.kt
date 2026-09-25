package com.fishingcopilot.ui.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.ui.components.hijriDateText
import com.fishingcopilot.ui.components.label
import com.fishingcopilot.ui.theme.CardBorder
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
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
            Text(stringResource(if (existing == null) R.string.catch_sheet_title else R.string.catch_edit_title), style = MaterialTheme.typography.headlineMedium, color = TextHighContrast)
            Spacer(Modifier.height(12.dp))
            ConditionsPanel(conditions, locale, saved = existing != null)

            Spacer(Modifier.height(16.dp))
            SectionLabel(R.string.catch_species_label)
            val ordered = targetSpecies.toList().sortedBy { it.ordinal } + Species.entries.filterNot { it in targetSpecies }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ordered.forEach { option ->
                    FilterChip(
                        selected = species == option.name,
                        onClick = { species = option.name; showSpeciesError = false },
                        label = { Text(stringResource(option.label)) }
                    )
                }
                FilterChip(
                    selected = species == OTHER,
                    onClick = { species = OTHER; showSpeciesError = false },
                    label = { Text(stringResource(R.string.catch_species_other)) }
                )
            }
            if (species == OTHER) {
                FormField(otherSpecies, { otherSpecies = it }, R.string.catch_species_other_hint, capitalize = true, modifier = Modifier.fillMaxWidth())
            }
            if (showSpeciesError) {
                Text(stringResource(R.string.catch_species_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel(R.string.catch_bait_label)
            if (recentBaits.isNotEmpty()) {
                Text(stringResource(R.string.catch_bait_recent), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentBaits.forEach { recent ->
                        FilterChip(selected = bait == recent, onClick = { bait = recent }, label = { Text(recent) })
                    }
                }
            }
            FormField(bait, { bait = it }, R.string.catch_bait_hint, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(weight, { weight = it }, R.string.catch_weight_label, numeric = true, modifier = Modifier.weight(1f))
                FormField(length, { length = it }, R.string.catch_length_label, numeric = true, modifier = Modifier.weight(1f))
            }
            FormField(notes, { notes = it }, R.string.catch_notes_label, capitalize = true, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val current = photo
                if (current != null) {
                    val bitmap = rememberImageBitmap(current, maxSizePx = 320)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    TextButton(onClick = { photo = null }) { Text(stringResource(R.string.catch_photo_remove), color = TextMuted) }
                } else {
                    TextButton(onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(R.string.catch_photo_add), color = NauticalCyan) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.catch_cancel), color = TextMuted) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        val chosen = if (species == OTHER) otherSpecies.trim() else species
                        if (chosen.isNullOrEmpty()) {
                            showSpeciesError = true
                        } else {
                            onSave(chosen, bait, weight.toDecimal(), length.toDecimal(), photo, notes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NauticalCyan, contentColor = OceanMidnight),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(if (existing == null) R.string.catch_save else R.string.catch_edit_save), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ConditionsPanel(conditions: CatchConditions, locale: Locale, saved: Boolean) {
    val none = stringResource(R.string.catch_no_data)
    Surface(shape = RoundedCornerShape(16.dp), color = OceanSurface, border = CardBorder) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(stringResource(R.string.catch_autofill_title), style = MaterialTheme.typography.labelMedium, color = NauticalCyan)
            Spacer(Modifier.height(6.dp))
            ConditionLine(
                R.string.catch_time_label,
                Instant.ofEpochMilli(conditions.timestamp).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
            )
            ConditionLine(R.string.catch_spot_label, conditions.spotName ?: none)
            ConditionLine(
                R.string.catch_water_label,
                conditions.waterLevel?.let { stringResource(R.string.catch_water_value, String.format(locale, "%+.1f m", it)) } ?: none
            )
            ConditionLine(
                R.string.catch_tide_label,
                conditions.rising?.let { stringResource(if (it) R.string.tide_rising else R.string.tide_falling) } ?: none
            )
            ConditionLine(R.string.catch_moon_label, conditions.moonPhase?.let { stringResource(it.label) } ?: none)
            ConditionLine(R.string.catch_hijri_label, conditions.hijri?.let { hijriDateText(it) } ?: none)
            ConditionLine(R.string.catch_score_label, conditions.biteScore?.let { String.format(locale, "%.1f", it) } ?: none)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(if (saved) R.string.catch_autofill_note_saved else R.string.catch_autofill_note), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun ConditionLine(labelRes: Int, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
    }
}

@Composable
private fun SectionLabel(labelRes: Int) {
    Text(stringResource(labelRes), style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    labelRes: Int,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    capitalize: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
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
            unfocusedTextColor = TextHighContrast
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(top = 4.dp)
    )
}

/** 2.0 shows as "2" and 2.45 as "2.45", ready to edit. */
private fun Double?.toField(): String = this?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()

/** Accepts both "2.4" and "2,4". */
private fun String.toDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
