package com.praxis.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.praxis.android.data.repository.PraxisRepository

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = PraxisRepository(applicationContext)
        return try {
            repository.getPosts("general")
            repository.getGoals("me")
            repository.getNotebookEntries("me")
            repository.getMyProfile()

            // Uploads drain BEFORE mutations: an entry queued offline may
            // reference a media URL that only exists once its file has landed.
            repository.processPendingUploads()
            repository.processPendingMutations()

            // Daily health attestation: read Health Connect (aggregate of
            // MyFitnessPal, Yazio, Fitbit, Google Fit, Samsung Health) and push
            // one sample. Soft-fail — never blocks the rest of the sync.
            try {
                val health = com.praxis.android.health.HealthConnectBridge.readToday(applicationContext)
                if (health.hasData()) {
                    repository.submitHealthSample(health.steps, health.calories, health.weightKg)
                }
            } catch (_: Exception) { /* oracle sync is best-effort */ }

            com.praxis.android.widget.WidgetUpdateManager.requestWidgetUpdate()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
