package com.fishingcopilot.ui.spots

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.R
import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.ui.onboarding.SpotSelection
import com.fishingcopilot.ui.theme.AlertRed
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.PrimeGreen
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SpotsScreen(viewModel: SpotsViewModel, homeSpotId: Long?, snackbar: SnackbarHostState) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SpotEntity?>(null) }
    var deleting by remember { mutableStateOf<SpotItem?>(null) }
    val blockedMessage = stringResource(R.string.spots_delete_home_blocked)
    val homeChangedTemplate = stringResource(R.string.spots_home_changed)

    if (adding) {
        AddSpot(
            onSave = { selection, name ->
                viewModel.add(selection, name)
                adding = false
            },
            onCancel = { adding = false }
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.spots_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextHighContrast,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { adding = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NauticalCyan, contentColor = OceanMidnight),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.spots_add), fontWeight = FontWeight.Bold) }
            }
        }
        items(items, key = { it.spot.id }) { item ->
            SpotCard(
                item = item,
                locale = locale,
                onSetHome = {
                    viewModel.setHome(item.spot)
                    scope.launch { snackbar.showSnackbar(String.format(homeChangedTemplate, item.spot.name)) }
                },
                onRename = { renaming = item.spot },
                onDelete = {
                    if (item.isHome || item.spot.id == homeSpotId) scope.launch { snackbar.showSnackbar(blockedMessage) }
                    else deleting = item
                }
            )
        }
    }

    renaming?.let { spot ->
        var name by remember(spot.id) { mutableStateOf(spot.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.spots_rename_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text(stringResource(R.string.spots_rename_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(spot, name); renaming = null }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.spots_rename_save), color = NauticalCyan)
                }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.spots_rename_cancel)) } },
            containerColor = OceanSurface
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.spots_delete_title, item.spot.name)) },
            text = { Text(stringResource(R.string.spots_delete_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(item); deleting = null }) {
                    Text(stringResource(R.string.spots_delete_confirm), color = AlertRed)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.spots_delete_cancel)) } },
            containerColor = OceanSurface
        )
    }
}

@Composable
private fun SpotCard(item: SpotItem, locale: Locale, onSetHome: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = OceanSurface,
        border = BorderStroke(1.dp, if (item.isHome) PrimeGreen.copy(alpha = 0.6f) else OceanCardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.spot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighContrast,
                    modifier = Modifier.weight(1f)
                )
                if (item.isHome) {
                    Surface(shape = RoundedCornerShape(8.dp), color = PrimeGreen.copy(alpha = 0.15f), border = BorderStroke(1.dp, PrimeGreen)) {
                        Text(
                            stringResource(R.string.spots_home_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimeGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text(
                String.format(Locale.ROOT, "%.4f, %.4f", item.spot.latitude, item.spot.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Text(
                pluralStringResource(R.plurals.spots_catch_count, item.catchCount, item.catchCount),
                style = MaterialTheme.typography.bodySmall,
                color = TextHighContrast
            )
            Row {
                if (!item.isHome) TextButton(onClick = onSetHome) { Text(stringResource(R.string.spots_set_home), color = NauticalCyan) }
                TextButton(onClick = onRename) { Text(stringResource(R.string.spots_rename), color = NauticalCyan) }
                if (!item.isHome) TextButton(onClick = onDelete) { Text(stringResource(R.string.spots_delete), color = TextMuted) }
            }
        }
    }
}

/** Full-screen map picker, the same one onboarding uses. */
@Composable
private fun AddSpot(onSave: (SpotSelection, String) -> Unit, onCancel: () -> Unit) {
    var selection by remember { mutableStateOf<SpotSelection?>(null) }
    var name by remember { mutableStateOf("") }
    var nameEdited by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(stringResource(R.string.spots_add_title), style = MaterialTheme.typography.headlineMedium, color = TextHighContrast)
        Spacer(Modifier.height(12.dp))
        SpotPicker(
            selection = selection,
            spotName = name,
            onSelect = { picked, defaultName ->
                selection = picked
                if (!nameEdited) name = defaultName
            },
            onNameChange = { name = it.take(30); nameEdited = true }
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.spots_add_cancel), color = TextMuted) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { selection?.let { onSave(it, name) } },
                enabled = selection != null && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NauticalCyan, contentColor = OceanMidnight),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.spots_add_save), fontWeight = FontWeight.Bold) }
        }
    }
}
