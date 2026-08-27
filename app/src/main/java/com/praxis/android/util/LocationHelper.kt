package com.praxis.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * One-shot GPS fix through the framework [LocationManager] — no Play services
 * dependency, which keeps the APK free of Google's proprietary bits.
 *
 * Tries the freshest last-known fix from any provider first; if none is fresh
 * enough, waits a bounded moment for a live one. Returns null when permission
 * is missing or no provider answers — callers must treat location as optional
 * garnish, never as something to block a capture on.
 */
object LocationHelper {

    private const val STALE_MS = 5 * 60_000L
    private const val FRESH_TIMEOUT_MS = 15_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** True when at least one location provider (GPS/network/passive) is on. */
    fun isAnyProviderEnabled(context: Context): Boolean =
        (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.let { lm ->
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER).any { p ->
                runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)
            }
        } ?: false

    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Freshest cached fix wins; providers are probed in accuracy order.
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (provider in providers) {
            try {
                if (!lm.isProviderEnabled(provider)) continue
                @Suppress("MissingPermission")
                val last = lm.getLastKnownLocation(provider) ?: continue
                if (System.currentTimeMillis() - last.time <= STALE_MS &&
                    (best == null || last.accuracy < best.accuracy)
                ) best = last
            } catch (_: SecurityException) {
                // Permission revoked mid-flight; treat as missing.
                return null
            } catch (_: IllegalArgumentException) {
                // Provider unknown on this device.
            }
        }
        if (best != null) return best

        // Nothing fresh: wait briefly for a live fix.
        for (provider in providers) {
            try {
                if (!lm.isProviderEnabled(provider)) continue
                @Suppress("MissingPermission")
                val live = requestSingleUpdate(lm, provider)
                if (live != null) return live
            } catch (_: SecurityException) {
                return null
            } catch (_: IllegalArgumentException) {
            }
        }
        return null
    }

    @Suppress("MissingPermission")
    private suspend fun requestSingleUpdate(lm: LocationManager, provider: String): Location? =
        kotlinx.coroutines.withTimeoutOrNull(FRESH_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val listener = LocationListener { location ->
                    if (cont.isActive) cont.resumeWith(Result.success(location))
                }
                try {
                    lm.requestSingleUpdate(provider, listener, null)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWith(Result.success(null))
                }
                cont.invokeOnCancellation {
                    runCatching { lm.removeUpdates(listener) }
                }
            }
        }
}
