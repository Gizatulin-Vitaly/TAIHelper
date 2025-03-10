package com.reftgres.taihelper

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TAIHelperApp : Application(), Configuration.Provider {
    private val TAG = "TAIHelperApp"

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var networkService: NetworkConnectivityService

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Инициализация Firebase должна быть первой
        FirebaseApp.initializeApp(this)

        // Остальная настройка
        setupSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun setupSync() {
        Log.d(TAG, "Настройка синхронизации")

        // Настраиваем периодическую синхронизацию данных
        syncManager.setupPeriodicSync()

        // Запускаем синхронизацию при старте приложения
        syncManager.requestSync()

        Log.d(TAG, "Синхронизация настроена")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Завершение работы приложения")

        if (::networkService.isInitialized) {
            Log.d(TAG, "Остановка мониторинга сети")
            networkService.stopMonitoring()
        }

        if (::syncManager.isInitialized) {
            Log.d(TAG, "Очистка ресурсов SyncManager")
            syncManager.cleanup()
        }
    }
}