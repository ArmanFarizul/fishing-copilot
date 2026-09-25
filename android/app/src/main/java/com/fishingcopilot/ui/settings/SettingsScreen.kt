package com.fishingcopilot.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fishingcopilot.FishingCopilotApp
import com.fishingcopilot.R
import com.fishingcopilot.maps.OfflineMaps
import com.fishingcopilot.maps.OfflineMapsState
import com.fishingcopilot.ui.theme.CautionYellow
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.OceanSurface
import com.fishingcopilot.ui.theme.TextHighContrast
import com.fishingcopilot.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(app: FishingCopilotApp, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings by app.alertSettings.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var permissionDenied by remember { mutableStateOf(false) }
    var exactAllowed by remember { mutableStateOf(app.goldenAlerts.canScheduleExact()) }

    // Coming back from the system "Alarms & reminders" screen: re-check and reschedule with the new mode.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = app.goldenAlerts.canScheduleExact()
                if (now != exactAllowed) {
                    exactAllowed = now
                    scope.launch { app.goldenAlerts.reschedule() }
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun setAlerts(enabled: Boolean) {
        scope.launch {
            app.alertSettings.setGoldenAlerts(enabled)
            app.goldenAlerts.reschedule()
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        setAlerts(granted)
    }

    val current = settings ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back), color = NauticalCyan) }
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = TextHighContrast)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.alerts_section).uppercase(), style = MaterialTheme.typography.labelMedium, color = NauticalCyan)
        Spacer(Modifier.height(8.dp))

        Surface(shape = RoundedCornerShape(18.dp), color = OceanSurface, border = BorderStroke(1.dp, OceanCardBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwitchRow(
                    title = stringResource(R.string.alerts_golden_title),
                    body = stringResource(R.string.alerts_golden_body),
                    checked = current.goldenAlerts,
                    onChange = { enabled ->
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (enabled && needsPermission) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissionDenied = false
                            setAlerts(enabled)
                        }
                    }
                )
                if (permissionDenied) {
                    Text(stringResource(R.string.alerts_permission_denied), style = MaterialTheme.typography.bodySmall, color = CautionYellow)
                }
                if (current.goldenAlerts) {
                    SwitchRow(stringResource(R.string.alerts_sound), null, current.sound) {
                        scope.launch { app.alertSettings.setSound(it) }
                    }
                    SwitchRow(stringResource(R.string.alerts_vibration), null, current.vibration) {
                        scope.launch { app.alertSettings.setVibration(it) }
                    }
                    val next = current.nextAlertAt
                    val start = current.nextWindowStart
                    val end = current.nextWindowEnd
                    Text(
                        text = if (next != null && start != null && end != null) {
                            stringResource(R.string.alerts_next, dayAndTime(next), dayAndTime(start), dayAndTime(end))
                        } else {
                            stringResource(R.string.alerts_none_scheduled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHighContrast
                    )
                    OutlinedButton(
                        onClick = { app.goldenAlerts.sendTest(current) },
                        border = BorderStroke(1.dp, NauticalCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.alerts_test_button), color = NauticalCyan) }
                }
            }
        }

        if (current.goldenAlerts && !exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = OceanSurface, border = BorderStroke(1.dp, CautionYellow.copy(alpha = 0.6f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.alerts_exact_title), style = MaterialTheme.typography.titleMedium, color = CautionYellow)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.alerts_exact_body), style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CautionYellow, contentColor = OceanMidnight),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.alerts_exact_button), fontWeight = FontWeight.Bold) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        OfflineMapsSection(app.offlineMaps)
    }
}

@Composable
private fun OfflineMapsSection(offline: OfflineMaps) {
    val context = LocalContext.current
    val state by offline.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { offline.refresh() }
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)

    Text(stringResource(R.string.offline_section).uppercase(), style = MaterialTheme.typography.labelMedium, color = NauticalCyan)
    Spacer(Modifier.height(8.dp))
    Surface(shape = RoundedCornerShape(18.dp), color = OceanSurface, border = BorderStroke(1.dp, OceanCardBorder)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.offline_body), style = MaterialTheme.typography.bodySmall, color = TextHighContrast)
            when (val current = state) {
                OfflineMapsState.Checking -> Unit
                is OfflineMapsState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { current.fraction },
                        color = NauticalCyan,
                        trackColor = OceanCardBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.offline_downloading, (current.fraction * 100).toInt(), size(current.bytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    OutlinedButton(onClick = offline::cancel, shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(R.string.offline_cancel), color = TextHighContrast)
                    }
                }
                is OfflineMapsState.Saved -> {
                    val status = when {
                        current.failed -> stringResource(R.string.offline_status_failed)
                        current.isEmpty -> stringResource(R.string.offline_status_none)
                        !current.complete -> stringResource(R.string.offline_status_incomplete, size(current.bytes))
                        !current.coversAllSpots -> stringResource(R.string.offline_status_new_spots, size(current.bytes))
                        else -> stringResource(R.string.offline_status_ready, size(current.bytes), dayAndTime(current.downloadedAt ?: 0L))
                    }
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (current.failed) CautionYellow else TextMuted
                    )
                    val action = when {
                        current.isEmpty -> R.string.offline_download
                        current.failed || !current.complete -> R.string.offline_resume
                        !current.coversAllSpots -> R.string.offline_download_new
                        else -> null
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (action != null) {
                            Button(
                                onClick = offline::download,
                                colors = ButtonDefaults.buttonColors(containerColor = NauticalCyan, contentColor = OceanMidnight),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(stringResource(action), fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.weight(1f))
                        if (!current.isEmpty) {
                            TextButton(onClick = offline::delete) { Text(stringResource(R.string.offline_delete), color = TextMuted) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, body: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextHighContrast)
            if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = NauticalCyan, checkedThumbColor = OceanMidnight)
        )
    }
}

/** Clock time, prefixed with the date when it is not today. */
@Composable
private fun dayAndTime(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val clock = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    return if (time.toLocalDate() == LocalDate.now()) clock
    else time.format(DateTimeFormatter.ofPattern("EEE", locale)) + " " + clock
}
