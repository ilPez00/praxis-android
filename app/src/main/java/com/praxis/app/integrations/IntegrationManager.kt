package com.praxis.app.integrations

import android.content.Context
import android.util.Log
import com.praxis.app.integrations.models.IntegrationConfig
import com.praxis.app.integrations.models.SyncResult
import com.praxis.app.integrations.models.TrackerEntry
import com.praxis.app.integrations.services.IntegrationService
import com.praxis.app.integrations.api.ApiClient
import com.praxis.app.integrations.api.TrackerLogRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Central manager for all external app integrations
 * 
 * Responsibilities:
 * - Register and manage integration services
 * - Coordinate sync operations
 * - Log tracker entries to backend
 * - Track sync status
 */
object IntegrationManager {
    private const val TAG = "IntegrationManager"

    private val integrations = ConcurrentHashMap<IntegrationType, IntegrationService>()
    private val configs = mutableMapOf<IntegrationType, IntegrationConfig>()
    
    private val _syncStatus = MutableStateFlow<Map<IntegrationType, SyncStatus>>(emptyMap())
    val syncStatus: StateFlow<Map<IntegrationType, SyncStatus>> = _syncStatus.asStateFlow()

    private lateinit var appContext: Context
    private var authToken: String? = null

    /**
     * Initialize the integration manager
     */
    fun initialize(context: Context, token: String) {
        appContext = context.applicationContext
        authToken = token
        loadConfigs()
    }

    /**
     * Register an integration service
     */
    fun registerIntegration(type: IntegrationType, service: IntegrationService) {
        integrations[type] = service
        Log.d(TAG, "Registered integration: ${type.displayName}")
    }

    /**
     * Get all registered integrations
     */
    fun getRegisteredIntegrations(): Map<IntegrationType, IntegrationService> = integrations.toMap()

    /**
     * Check if an integration is connected
     */
    fun isConnected(type: IntegrationType): Boolean {
        return configs[type]?.enabled == true && configs[type]?.connectedAt != null
    }

    /**
     * Enable an integration
     */
    suspend fun enableIntegration(type: IntegrationType): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = integrations[type] ?: run {
                    Log.e(TAG, "Service not registered for: ${type.displayName}")
                    return@withContext false
                }

                val success = service.initialize()
                if (success) {
                    configs[type] = IntegrationConfig(
                        type = type.id,
                        enabled = true,
                        connectedAt = System.currentTimeMillis()
                    )
                    saveConfigs()
                    Log.d(TAG, "Enabled integration: ${type.displayName}")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable integration: ${type.displayName}", e)
                false
            }
        }
    }

    /**
     * Disable an integration
     */
    suspend fun disableIntegration(type: IntegrationType) {
        withContext(Dispatchers.IO) {
            configs[type] = configs[type]?.copy(enabled = false) ?: IntegrationConfig(
                type = type.id,
                enabled = false
            )
            saveConfigs()
            Log.d(TAG, "Disabled integration: ${type.displayName}")
        }
    }

    /**
     * Sync a specific integration
     */
    suspend fun syncIntegration(type: IntegrationType): SyncResult {
        return withContext(Dispatchers.IO) {
            val service = integrations[type] ?: run {
                return@withContext SyncResult(
                    success = false,
                    error = "Service not registered"
                )
            }

            try {
                updateSyncStatus(type, SyncStatus.SYNCING)
                val result = service.sync()
                
                if (result.success) {
                    configs[type] = configs[type]?.copy(
                        lastSync = System.currentTimeMillis()
                    ) ?: IntegrationConfig(
                        type = type.id,
                        lastSync = System.currentTimeMillis()
                    )
                    saveConfigs()
                    updateSyncStatus(type, SyncStatus.IDLE)
                } else {
                    updateSyncStatus(type, SyncStatus.ERROR(result.error))
                }

                result
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed for: ${type.displayName}", e)
                updateSyncStatus(type, SyncStatus.ERROR(e.message))
                SyncResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Sync all enabled integrations
     */
    suspend fun syncAllEnabled(): Map<IntegrationType, SyncResult> {
        val results = mutableMapOf<IntegrationType, SyncResult>()
        
        integrations.keys.forEach { type ->
            if (isConnected(type)) {
                results[type] = syncIntegration(type)
            }
        }

        return results
    }

    /**
     * Log a tracker entry to the backend
     */
    suspend fun logTrackerEntry(entry: TrackerEntry): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = authToken ?: run {
                    return@withContext SyncResult(
                        success = false,
                        error = "No auth token available"
                    )
                }

                val api = ApiClient.createAuthenticatedApi(token)
                val request = TrackerLogRequest(
                    type = entry.type,
                    data = entry.data,
                    source = entry.source
                )

                val response = api.logTrackerEntry("Bearer $token", request)
                
                if (response.success) {
                    Log.d(TAG, "Logged tracker entry: ${entry.type} from ${entry.source}")
                    SyncResult(
                        success = true,
                        entriesLogged = 1
                    )
                } else {
                    SyncResult(
                        success = false,
                        error = "Backend rejected entry"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log tracker entry", e)
                SyncResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Get config for an integration
     */
    fun getConfig(type: IntegrationType): IntegrationConfig? = configs[type]

    /**
     * Update sync status and emit to flow
     */
    private fun updateSyncStatus(type: IntegrationType, status: SyncStatus) {
        val currentStatus = _syncStatus.value.toMutableMap()
        currentStatus[type] = status
        _syncStatus.value = currentStatus
    }

    /**
     * Load integration configs from SharedPreferences
     */
    private fun loadConfigs() {
        val prefs = appContext.getSharedPreferences("integration_configs", Context.MODE_PRIVATE)
        // Simplified - in production use Gson/serialization
        Log.d(TAG, "Loaded integration configs")
    }

    /**
     * Save integration configs to SharedPreferences
     */
    private fun saveConfigs() {
        val prefs = appContext.getSharedPreferences("integration_configs", Context.MODE_PRIVATE)
        // Simplified - in production use Gson/serialization
        Log.d(TAG, "Saved integration configs")
    }
}

/**
 * Sync status states
 */
sealed class SyncStatus {
    object IDLE : SyncStatus()
    object SYNCING : SyncStatus()
    data class ERROR(val message: String?) : SyncStatus()
}
