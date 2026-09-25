package com.fishingcopilot.ui.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * One-shot location via the platform LocationManager, so the app needs no Google Play services.
 * Calls back with null when permission is missing, location is switched off, or no fix arrives.
 */
@SuppressLint("MissingPermission") // Guarded by hasLocationPermission below.
fun fetchCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(LocationManager::class.java)
    if (!hasLocationPermission(context) || !LocationManagerCompat.isLocationEnabled(manager)) {
        onResult(null)
        return
    }
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val provider = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            LocationManagerCompat.hasProvider(manager, LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
        fineGranted && LocationManagerCompat.hasProvider(manager, LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        LocationManagerCompat.hasProvider(manager, LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> {
            onResult(null)
            return
        }
    }
    LocationManagerCompat.getCurrentLocation(
        manager,
        provider,
        null as CancellationSignal?,
        ContextCompat.getMainExecutor(context)
    ) { onResult(it) }
}

/**
 * Where the phone is, for the home screen. A fix under 15 minutes old is used straight away, since a fresh
 * GPS fix can take a while at sea; otherwise waits up to 30 seconds so the screen never hangs on "locating".
 */
@SuppressLint("MissingPermission") // Guarded by hasLocationPermission.
suspend fun awaitCurrentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(LocationManager::class.java)
    val recent = manager.getProviders(true)
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < RECENT_FIX_NANOS }
        .minByOrNull { it.accuracy }
    if (recent != null) return recent
    return withTimeoutOrNull(FIX_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont -> fetchCurrentLocation(context) { if (cont.isActive) cont.resume(it) } }
    }
}

private const val RECENT_FIX_NANOS = 15L * 60 * 1_000_000_000
private const val FIX_TIMEOUT_MS = 30_000L
