package com.reftgres.taihelper

import android.app.Application
import com.google.firebase.FirebaseApp
import com.reftgres.taihelper.service.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TAIHelperApp : Application() {

    @Inject
    lateinit var syncManager: SyncManager

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
}