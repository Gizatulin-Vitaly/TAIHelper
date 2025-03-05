package com.reftgres.taihelper

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.ui.authorization.LoginViewModel
import com.reftgres.taihelper.ui.settings.AppTheme
import com.reftgres.taihelper.ui.settings.ThemePreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val loginViewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var themeRepository: ThemePreferencesRepository

    @Inject
    lateinit var networkService: NetworkConnectivityService

    private lateinit var networkIndicator: View
    private lateinit var networkStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Инициализация индикатора сети в шапке
        networkIndicator = findViewById(R.id.networkIndicator)
        networkStatusText = findViewById(R.id.networkStatusText)

        // Настройка навигации
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        // Получение информации о пользователе из intent
        val userName = intent.getStringExtra("USER_NAME") ?: "Неизвестный"
        val userStatus = intent.getStringExtra("USER_STATUS") ?: "Обычный"

        // Установка информации о пользователе
        findViewById<TextView>(R.id.tvUserName).text = userName
        findViewById<TextView>(R.id.tvUserStatus).text = userStatus
        findViewById<TextView>(R.id.toolbarTitle).text = userName
        toolbar.alpha = 0.8f

        // Установка начального состояния индикатора
        updateNetworkIndicator(networkService.isNetworkAvailable())

        // Наблюдение за темой
        lifecycleScope.launch {
            themeRepository.themeFlow.collect { theme ->
                when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    AppTheme.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    AppTheme.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }

        // Наблюдение за статусом соединения с учетом жизненного цикла
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Начинаем наблюдение за статусом сети")
                networkService.networkStatus.collect { isConnected ->
                    Log.d(TAG, "Получено новое состояние сети: $isConnected")
                    updateNetworkIndicator(isConnected)
                }
            }
        }
    }

    private fun updateNetworkIndicator(isConnected: Boolean) {
        Log.d(TAG, "updateNetworkIndicator: $isConnected")

        // Используем runOnUiThread для безопасного обновления UI
        runOnUiThread {
            // Обновляем индикатор
            networkIndicator.isActivated = isConnected

            // Обновляем текст
            networkStatusText.text = if (isConnected) "Онлайн" else "Офлайн"

            // Обновляем цвет текста
            networkStatusText.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isConnected) R.color.success_green else R.color.error_red
                )
            )

            Log.d(TAG, "UI обновлен: сеть ${if (isConnected) "доступна" else "недоступна"}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_sensor -> {
                val navController = findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.addSensorFragment)
                true
            }
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.settingsFragment)
                true
            }
            R.id.action_about -> {
                Toast.makeText(this, "О приложении", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ничего не делаем здесь, сервис управляет своими ресурсами
    }
}