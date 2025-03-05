package com.reftgres.taihelper

import android.app.Application
import com.google.firebase.FirebaseApp
import com.reftgres.taihelper.service.SyncManager
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TAIHelperApp : Application() {

    @Inject
    lateinit var syncManager: SyncManager
    lateinit var networkService: NetworkConnectivityService

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Настраиваем периодическую синхронизацию
        setupSync()
    }

    private fun setupSync() {
        // Настраиваем периодическую синхронизацию данных
        syncManager.setupPeriodicSync()
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::networkService.isInitialized) {
            networkService.stopMonitoring()
        }
    }
}