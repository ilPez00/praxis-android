package com.praxis.app.integrations.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.praxis.app.integrations.IntegrationManager
import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.services.*

/**
 * Background worker for syncing all enabled integrations
 * 
 * Runs periodically via WorkManager to automatically sync data
 * from external apps (Health Connect, Strava, Fitbit, etc.)
 */
class IntegrationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "IntegrationSyncWorker"
        const val WORK_NAME = "integration_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting integration sync...")
            
            // Note: In production, IntegrationManager should be initialized in Application class
            // This is simplified for demonstration
            
            val results = IntegrationManager.syncAllEnabled()
            
            val successCount = results.count { it.value.success }
            val failCount = results.size - successCount
            
            Log.d(TAG, "Integration sync complete: $successCount succeeded, $failCount failed")
            
            if (failCount > 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Integration sync failed", e)
            Result.retry()
        }
    }
}

/**
 * Helper to schedule periodic integration sync
 */
object IntegrationSyncScheduler {
    private const val TAG = "SyncScheduler"
    
    /**
     * Schedule periodic sync for all integrations
     * Call this from Application.onCreate() or when integrations are enabled
     */
    fun schedulePeriodicSync(context: Context) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        
        // Create work request for periodic sync
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<IntegrationSyncWorker>(
            6, // Repeat interval
            java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag(IntegrationSyncWorker.WORK_NAME)
            .build()
        
        // Enqueue unique work to ensure only one sync schedule exists
        workManager.enqueueUniquePeriodicWork(
            IntegrationSyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        Log.d(TAG, "Scheduled periodic integration sync every 6 hours")
    }
    
    /**
     * Cancel all scheduled syncs
     */
    fun cancelSync(context: Context) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.cancelUniqueWork(IntegrationSyncWorker.WORK_NAME)
        Log.d(TAG, "Cancelled integration sync")
    }
    
    /**
     * Trigger immediate one-time sync
     */
    fun triggerImmediateSync(context: Context) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<IntegrationSyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueue(workRequest)
        Log.d(TAG, "Triggered immediate integration sync")
    }
}
