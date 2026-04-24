package com.praxis.app.integrations.services

import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.models.SyncResult
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all integration services
 */
interface IntegrationService {
    /**
     * The type of integration this service handles
     */
    val integrationType: IntegrationType

    /**
     * Check if the integration is available/authorized
     */
    suspend fun isAvailable(): Boolean

    /**
     * Initialize the integration (OAuth, permissions, etc.)
     */
    suspend fun initialize(): Boolean

    /**
     * Sync data from the external service and log to Praxis trackers
     */
    suspend fun sync(): SyncResult

    /**
     * Get the last sync time
     */
    suspend fun getLastSyncTime(): Long?

    /**
     * Observe sync results for real-time updates
     */
    fun observeSyncResults(): Flow<SyncResult> = kotlinx.coroutines.flow.emptyFlow()
}
