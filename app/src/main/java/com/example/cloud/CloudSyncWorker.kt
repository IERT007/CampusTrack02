package com.example.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.DataStoreManager
import com.example.data.local.AppDatabase
import com.example.data.repository.CampusRepository
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker for offline-first Supabase cloud auto-sync.
 * Enqueued with ExistingWorkPolicy.REPLACE on every local Room write so rapid edits
 * coalesce cleanly and execute automatically once a network connection is available.
 */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncManager = SupabaseSyncManager(applicationContext)
        if (!syncManager.isConfigured()) {
            return Result.success()
        }

        val dataStore = DataStoreManager(applicationContext)
        val session = dataStore.getSupabaseSessionDirect() ?: return Result.success()
        if (session.accessToken.isBlank()) {
            return Result.success()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = CampusRepository(applicationContext, db)
        val result = syncManager.pushBackupToCloud(repository)

        return if (result.isSuccess) {
            Result.success()
        } else if (runAttemptCount < 3) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_SYNC_WORK_NAME = "caliper_supabase_cloud_sync"

        fun enqueueDebouncedSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(3, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(
                        UNIQUE_SYNC_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        syncRequest
                    )
            } catch (_: Exception) {
                // Keep offline-first writes resilient even if WorkManager is unavailable
            }
        }
    }
}
