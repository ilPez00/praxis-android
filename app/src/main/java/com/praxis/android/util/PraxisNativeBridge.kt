package com.praxis.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.praxis.android.health.HealthConnectBridge
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/** Shared DataStore name — MUST match SettingsViewModel / WebScreen. */
private val Context.appDataStore by preferencesDataStore(name = "praxis_settings")

/**
 * Generic phone/app monitor bridge.
 *
 * Exposes the two on-device truth sources the web app can't reach from a
 * browser — Android's [UsageStatsManager] (foreground minutes per app, the
 * real "screen time" including every goal-oriented app the user runs) and
 * Health Connect (the on-device aggregate of Google Fit, Fitbit, Samsung
 * Health, MyFitnessPal, Yazio — i.e. the "fitting apps" the user asked about).
 *
 * Injected into the WebView as `window.PraxisNative` (see WebScreen). Every
 * method returns a JSON string so the web bridge lib can `JSON.parse` it; every
 * read is a no-op-fallback object when the corresponding permission isn't
 * granted, so the web layer never throws.
 *
 * Consent: the bridge is only attached when the user has enabled the
 * "Share phone usage" toggle in native Settings (see SettingsViewModel). When
 * detached, `window.PraxisNative` is simply undefined and the web app falls
 * back to its other oracle paths.
 */
@SuppressLint("SetJavaScriptEnabled")
class PraxisNativeBridge(private val context: Context) {

    @JavascriptInterface
    fun isNative(): String = JSONObject().put("native", true).put("platform", "android").toString()

    /** True if the user has granted Usage Access (required for any screen-time read). */
    @JavascriptInterface
    fun isUsageGranted(): String =
        JSONObject().put("granted", ScreenTimeManager.hasPermission(context)).toString()

    /**
     * Consent switch for the whole monitor. Flipped by the web app's Settings
     * toggle (the UI users actually see inside the WebView). When disabled, the
     * next WebView rebuild drops the bridge object entirely, so no data is read.
     */
    @JavascriptInterface
    fun setSharePhoneUsage(enabledJson: String): String {
        val enabled = runCatching { JSONObject(enabledJson).optBoolean("enabled", false) }.getOrDefault(false)
        runCatching {
            val key = androidx.datastore.preferences.core.booleanPreferencesKey("share_phone_usage_enabled")
            val ds = context.appDataStore
            kotlinx.coroutines.runBlocking { ds.edit { it[key] = enabled } }
        }
        return JSONObject().put("enabled", enabled).toString()
    }

    /** Deep-link the user to Android's Usage Access settings screen. */
    @JavascriptInterface
    fun openUsageSettings(): String {
        runCatching { ScreenTimeManager.openSettings(context) }
        return "{}"
    }

    /**
     * Today's per-app foreground minutes since local midnight.
     *
     * Unlike the social-only [ScreenTimeManager.getTodayUsage], this returns the
     * FULL per-app breakdown (every app, not just the social set) so the web app
     * can build a generic "app monitor" view — Duolingo, any habit app, a reading
     * app, etc. all show up with real minutes. `granted:false` when permission
     * is missing; the web layer should then call [openUsageSettings].
     */
    @JavascriptInterface
    fun getScreenTime(): String {
        val usage = ScreenTimeManager.getTodayUsage(context)
        if (!usage.granted) {
            return JSONObject().put("granted", false).put("perApp", JSONObject()).toString()
        }
        val perApp = JSONObject()
        usage.perApp.forEach { (pkg, mins) -> perApp.put(pkg, mins) }
        return JSONObject()
            .put("granted", true)
            .put("socialMinutes", usage.socialMinutes)
            .put("totalMinutes", usage.totalMinutes)
            .put("perApp", perApp)
            .toString()
    }

    /** Whether Health Connect is available + granted on this device. */
    @JavascriptInterface
    fun isHealthAvailable(): String {
        val available = HealthConnectBridge.isAvailable(context)
        val granted = runCatching {
            available && kotlinx.coroutines.runBlocking { HealthConnectBridge.checkPermission(context) }
        }.getOrDefault(false)
        return JSONObject().put("available", available).put("granted", granted).toString()
    }

    /** Deep-link to the Health Connect permission screen. */
    @JavascriptInterface
    fun openHealthSettings(): String {
        runCatching {
            val intent = HealthConnectBridge.settingsIntent(context)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { context.startActivity(it) }
        }
        return "{}"
    }

    /**
     * Today's health metrics from Health Connect (steps / calories / weight).
     * Returns null fields when not granted or no data. The web layer posts this
     * to /oracle/health-sample.
     */
    @JavascriptInterface
    fun getHealth(): String {
        val health = runCatching {
            kotlinx.coroutines.runBlocking { HealthConnectBridge.readToday(context) }
        }.getOrNull()
        if (health == null || !health.available) {
            return JSONObject().put("available", false).toString()
        }
        val out = JSONObject()
            .put("available", true)
            .put("granted", health.granted)
        health.steps?.let { out.put("steps", it) }
        health.calories?.let { out.put("calories", it) }
        health.weightKg?.let { out.put("weightKg", it) }
        return out.toString()
    }
}
