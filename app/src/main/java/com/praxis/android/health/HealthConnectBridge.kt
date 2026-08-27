package com.praxis.android.health

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Native Health Connect reader — the on-device aggregate of MyFitnessPal, Yazio,
 * Fitbit, Google Fit and Samsung Health (MFP's public API is gone, Yazio's is
 * partner-only; Health Connect is the realistic path). One read per metric covers
 * them all.
 *
 * The result is POSTed to /oracle/health-sample (today/yesterday only server-side),
 * which backs the `health_connect` oracle at trust 0.7.
 */
object HealthConnectBridge {

    data class TodayHealth(
        val available: Boolean,
        val granted: Boolean,
        val steps: Long? = null,
        val calories: Double? = null,
        val weightKg: Double? = null,
        val error: String? = null,
    ) {
        fun hasData(): Boolean = steps != null || calories != null || weightKg != null
    }

    private val _state = MutableStateFlow(TodayHealth(available = false, granted = false))
    val state: StateFlow<TodayHealth> = _state

    private val readPerms = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    /** Permission strings for the request contract (see HealthScreens). */
    val readPermissions: Set<String> get() = readPerms

    /** HC SDK availability — requires API 26+ on device plus the provider package. */
    fun isAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 26 && HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun clientOrNull(context: Context): HealthConnectClient? =
        if (isAvailable(context)) HealthConnectClient.getOrCreate(context) else null

    /**
     * Intent that opens Health Connect's permission/data screen, or the Play
     * Store listing when the provider isn't installed. Returns null only on
     * API < 26 (HC unsupported there).
     */
    fun settingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < 26) return null
        val providerPkg = "com.google.android.apps.healthdata"
        // HC exposes a deep-link action to its permission screen.
        val action = "androidx.health.action.SHOW_PERMISSIONS"
        val intent = Intent(action).apply { `package` = providerPkg }
        val pm = context.packageManager
        return if (intent.resolveActivity(pm) != null) {
            intent
        } else {
            // Fall back to the Play Store so the user can install it.
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://details?id=$providerPkg")
            }
        }
    }

    suspend fun checkPermission(context: Context): Boolean {
        val client = clientOrNull(context) ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(readPerms)
    }

    /**
     * Read today's (or yesterday's when today has no samples yet — the common
     * early-morning case) steps / calories / latest weight. Never throws.
     */
    suspend fun readToday(context: Context): TodayHealth {
        val client = clientOrNull(context)
        if (client == null) {
            _state.value = TodayHealth(available = false, granted = false, error = "Health Connect unavailable")
            return _state.value
        }
        return try {
            val granted = client.permissionController.getGrantedPermissions().containsAll(readPerms)
            if (!granted) {
                _state.value = TodayHealth(available = true, granted = false, error = "Permissions not granted")
                return _state.value
            }
            val zone = ZoneId.systemDefault()
            var day = LocalDate.now(zone)

            // Today first; if it's empty (morning), fall back to yesterday so the
            // sample reflects the most recent complete day of data.
            var health = readDay(client, day, zone)
            if (!health.hasData()) {
                day = day.minusDays(1)
                health = readDay(client, day, zone)
            }
            _state.value = health.copy(available = true, granted = true)
            _state.value
        } catch (e: Exception) {
            _state.value = TodayHealth(available = true, granted = false, error = e.message ?: "read failed")
            _state.value
        }
    }

    private suspend fun readDay(client: HealthConnectClient, day: LocalDate, zone: ZoneId): TodayHealth {
        val startOfDay = day.atStartOfDay(zone).toInstant()
        val endOfDayExclusive = day.plusDays(1).atStartOfDay(zone).toInstant()
        val range = TimeRangeFilter.between(startOfDay, endOfDayExclusive)

        var steps: Long? = null
        var calories: Double? = null
        var weightKg: Double? = null

        try {
            val stepRes = client.readRecords(ReadRecordsRequest(StepsRecord::class, range))
            if (stepRes.records.isNotEmpty()) steps = stepRes.records.sumOf { it.count }
        } catch (_: Exception) { /* metric optional */ }

        try {
            val calRes = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, range))
            if (calRes.records.isNotEmpty()) calories = calRes.records.sumOf { it.energy.inKilocalories }
        } catch (_: Exception) { /* metric optional */ }

        try {
            // Latest weight recorded during the window (not a sum).
            val weightRes = client.readRecords(ReadRecordsRequest(WeightRecord::class, range))
            weightKg = weightRes.records.maxByOrNull { it.time }?.weight?.inKilograms
        } catch (_: Exception) { /* metric optional */ }

        return TodayHealth(
            available = true,
            granted = true,
            steps = steps?.takeIf { it > 0 },
            calories = calories?.takeIf { it > 0.0 },
            weightKg = weightKg,
        )
    }
}
