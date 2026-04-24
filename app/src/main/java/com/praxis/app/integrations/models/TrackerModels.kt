package com.praxis.app.integrations.models

/**
 * Data class representing a tracker entry to be logged
 */
data class TrackerEntry(
    val type: String,              // e.g., 'steps', 'cardio', 'meal'
    val data: Map<String, Any>,    // Flexible field data
    val source: String = "manual", // 'manual', 'auto_health_connect', etc.
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Configuration for an integration
 */
data class IntegrationConfig(
    val type: String,
    val enabled: Boolean = true,
    val syncFrequency: SyncFrequency = SyncFrequency.HOURLY,
    val lastSync: Long? = null,
    val configData: Map<String, String> = emptyMap(), // User preferences, tokens, etc.
    val connectedAt: Long? = null
)

/**
 * Sync frequency options
 */
enum class SyncFrequency(val minutes: Int) {
    REAL_TIME(0),      // Immediate (push-based)
    HOURLY(60),
    EVERY_6_HOURS(360),
    DAILY(1440)
}

/**
 * Result of a sync operation
 */
data class SyncResult(
    val success: Boolean,
    val entriesLogged: Int = 0,
    val error: String? = null
)
