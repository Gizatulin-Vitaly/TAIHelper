package com.reftgres.taihelper.service

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager,
    private val networkConnectivityService: NetworkConnectivityService
) {
    companion object {
        private const val SYNC_WORK_NAME = "data_sync_work"
        private const val SYNC_PERIODIC_WORK_NAME = "periodic_data_sync_work"
    }

    // Запускает немедленную синхронизацию
    fun requestSync() {
        if (networkConnectivityService.isNetworkAvailable()) {
            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
        }
    }

    // Настраивает периодическую синхронизацию
    fun setupPeriodicSync() {
        val periodicSyncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,  // Минимальный интервал - 15 минут
            5, TimeUnit.MINUTES    // Гибкость - 5 минут
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWorkRequest
        )
    }
}