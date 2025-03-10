package com.reftgres.taihelper.service

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_NAME = "data_sync_work"
        private const val SYNC_PERIODIC_WORK_NAME = "periodic_data_sync_work"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    init {
        // Подписываемся на изменения статуса сети
        scope.launch {
            Log.d(TAG, "Инициализация мониторинга сети в SyncManager")

            var previousStatus = false // Переменная для хранения предыдущего состояния

            // Собираем изменения статуса сети
            networkConnectivityService.networkStatus.collect { isAvailable ->
                Log.d(TAG, "Изменение статуса сети: доступна = $isAvailable")

                // Запускаем синхронизацию только при переходе из offline в online
                if (isAvailable && !previousStatus) {
                    Log.d(TAG, "Сеть восстановлена (переход из offline в online), запускаем синхронизацию")
                    requestSync()
                }

                previousStatus = isAvailable
            }
        }
    }

    // Запускает немедленную синхронизацию
    fun requestSync() {
        Log.d(TAG, "Запрос на выполнение синхронизации")

        // Определяем требуемый тип сети в зависимости от текущей доступности
        val networkType = if (networkConnectivityService.isNetworkAvailable()) {
            Log.d(TAG, "Сеть доступна, планируем немедленную синхронизацию")
            NetworkType.CONNECTED
        } else {
            Log.d(TAG, "Сеть недоступна, планируем синхронизацию при появлении сети")
            NetworkType.CONNECTED  // По-прежнему требуем соединение
        }

        val syncWorkRequest = OneTimeWorkRequestBuilder<DirectSyncWorker>() // ИЗМЕНЕНО: используем DirectSyncWorker
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )

        Log.d(TAG, "Задача синхронизации запланирована (запустится когда будет сеть)")
    }

    // Настраивает периодическую синхронизацию
    fun setupPeriodicSync() {
        Log.d(TAG, "Настройка периодической синхронизации")

        val periodicSyncWorkRequest = PeriodicWorkRequestBuilder<DirectSyncWorker>( // ИЗМЕНЕНО: используем DirectSyncWorker
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

        Log.d(TAG, "Периодическая синхронизация настроена")
    }

    // Вызывается при завершении работы приложения
    fun cleanup() {
        Log.d(TAG, "Очистка ресурсов SyncManager")
        job.cancel()
    }
}