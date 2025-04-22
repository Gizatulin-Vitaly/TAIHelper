package com.reftgres.taihelper

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.datastore.preferences.core.Preferences
import com.reftgres.taihelper.ui.settings.LanguagePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.*


@HiltAndroidApp
class TAIHelperApp : Application(), Configuration.Provider {
    @Inject
    lateinit var dataStore: DataStore<Preferences>
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    private val TAG = "TAIHelperApp"
    @Inject
    lateinit var syncManager: SyncManager
    @Inject
    lateinit var networkService: NetworkConnectivityService


    override fun onCreate() {
        super.onCreate()

        // 🗣 Загрузка локали
        CoroutineScope(Dispatchers.Default).launch {
            val prefs = dataStore.data.first()
            val langTag = prefs[LanguagePreferencesRepository.LANGUAGE_KEY] ?: "ru"
            val locales = LocaleListCompat.forLanguageTags(langTag)
            withContext(Dispatchers.Main) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
            // 🎨 Загрузка темы
            val themeName = prefs[com.reftgres.taihelper.ui.settings.ThemePreferencesRepository.THEME_KEY]
                ?: com.reftgres.taihelper.ui.settings.AppTheme.SYSTEM.name
            val mode = when (com.reftgres.taihelper.ui.settings.AppTheme.valueOf(themeName)) {
                com.reftgres.taihelper.ui.settings.AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                com.reftgres.taihelper.ui.settings.AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                com.reftgres.taihelper.ui.settings.AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(mode)
                setupSync()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            return if (::workerFactory.isInitialized) {
                // Используем Hilt factory, если она инициализирована
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build()
            } else {
                // Запасной вариант, если workerFactory еще не инициализирован
                Log.w(TAG, "WorkerFactory не инициализирован, используем стандартную конфигурацию")
                Configuration.Builder().build()
            }
        }

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