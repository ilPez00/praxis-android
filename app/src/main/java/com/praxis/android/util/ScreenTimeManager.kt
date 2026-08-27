package com.praxis.android.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Native screen-time source — Android's UsageStatsManager is the real
 * on-device source RescueTime can't reach on phones.
 *
 * Powers mobile detox proof: detox commitments can be verified against actual
 * social-media foreground minutes rather than self-reported numbers, and the
 * numbers reach the server through the oracle endpoints (the same usage sample
 * the Capacitor-era bridge fed).
 *
 * Requires the PACKAGE_USAGE_STATS special permission, which the user grants
 * manually in system settings — [openSettings] deep-links there, since there
 * is no runtime dialog for it.
 */
object ScreenTimeManager {

    /** The packages whose foreground minutes count as social media time. */
    val SOCIAL_PACKAGES = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.twitter.android",          // X
        "com.zhiliaoapp.musically",     // TikTok
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.linkedin.android",
        "com.pinterest",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.google.android.youtube"
    )

    data class Usage(
        val granted: Boolean,
        val socialMinutes: Long = 0,
        val totalMinutes: Long = 0,
        val perApp: Map<String, Long> = emptyMap()
    )

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Deep-link the user to the system Usage Access settings screen. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Foreground minutes since local midnight. [Usage.socialMinutes] covers
     * [SOCIAL_PACKAGES]; [Usage.totalMinutes] is all apps.
     */
    fun getTodayUsage(context: Context): Usage {
        if (!hasPermission(context)) return Usage(granted = false)

        val midnight = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = midnight.timeInMillis
        val end = System.currentTimeMillis()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        // queryAndAggregateUsageStats merges duplicate buckets for us.
        val stats = usm.queryAndAggregateUsageStats(start, end)

        var socialMs = 0L
        var totalMs = 0L
        val perApp = mutableMapOf<String, Long>()
        for ((pkg, usage) in stats) {
            val fg = usage.totalTimeInForeground
            if (fg <= 0) continue
            totalMs += fg
            if (pkg in SOCIAL_PACKAGES) {
                socialMs += fg
                perApp[pkg] = Math.round(fg / 60000.0)
            }
        }

        return Usage(
            granted = true,
            socialMinutes = Math.round(socialMs / 60000.0),
            totalMinutes = Math.round(totalMs / 60000.0),
            perApp = perApp
        )
    }
}
