package com.reftgres.taihelper

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.ui.authorization.LoginViewModel
import com.reftgres.taihelper.ui.settings.AppTheme
import com.reftgres.taihelper.ui.settings.ThemePreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.google.android.material.snackbar.Snackbar
import com.reftgres.taihelper.di.dataStore
import com.reftgres.taihelper.ui.settings.LanguagePreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val loginViewModel: LoginViewModel by viewModels()
    private var networkSnackbar: Snackbar? = null


    @Inject
    lateinit var themeRepository: ThemePreferencesRepository

    @Inject
    lateinit var networkService: NetworkConnectivityService

    private lateinit var networkIndicator: View
    private lateinit var networkStatusText: TextView
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var userStatus: String


    override fun onCreate(savedInstanceState: Bundle?) {
        val languageRepository = LanguagePreferencesRepository(dataStore)
        runBlocking {
            val language = languageRepository.languageFlow.first()
            setAppLocale(language)
        }
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
        navController = navHostFragment.navController
        // Создаем AppBarConfiguration и определяем верхний уровень навигации
        // Здесь указываем id фрагментов, которые считаются корневыми (на них не показываем кнопку назад)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.converterFragment,
                R.id.oxygenMeasurementFragment,
                R.id.verificationAjkFragment,
                R.id.referenceFragment
            )
        )

        // Настраиваем ActionBar с NavController и AppBarConfiguration
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Настраиваем белую стрелку назад независимо от темы
        setupWhiteBackArrow()

        // Слушатель для повторной установки белой стрелки при изменении темы
        lifecycleScope.launch {
            themeRepository.themeFlow.collect { theme ->
                // Обновляем стрелку каждый раз при изменении темы
                setupWhiteBackArrow()
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        // Наблюдение за изменениями пунктов назначения навигации
        navController.addOnDestinationChangedListener { _, destination, _ ->
            setupWhiteBackArrow()

            val isTopLevel = appBarConfiguration.topLevelDestinations.contains(destination.id)

            supportActionBar?.setDisplayHomeAsUpEnabled(!isTopLevel)

            findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility =
                if (isTopLevel) View.VISIBLE else View.GONE

            supportActionBar?.title = if (destination.id == R.id.settingsFragment) "" else ""
        }

        // Получение информации о пользователе из intent
        val userName = intent.getStringExtra("USER_NAME") ?: "Неизвестный"
        userStatus = intent.getStringExtra("USER_STATUS") ?: "Обычный"


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
                setupWhiteBackArrow()
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

    fun hasFullAccess(): Boolean {
        return userStatus != "3"
    }

    private fun setAppLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Специальный метод для установки белой стрелки назад
    private fun setupWhiteBackArrow() {
        try {
            // Получаем стандартную стрелку назад
            val upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)?.mutate()

            val surfaceColor = ContextCompat.getColor(this, R.color.surface)
            upArrow?.colorFilter = PorterDuffColorFilter(surfaceColor, PorterDuff.Mode.SRC_ATOP)


            // Устанавливаем модифицированную стрелку
            supportActionBar?.setHomeAsUpIndicator(upArrow)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке белой стрелки: ${e.message}")
        }
    }

    // Обработка нажатия кнопки "Назад" в ActionBar
    override fun onSupportNavigateUp(): Boolean {
        // Сначала пробуем безопасно навигироваться назад
        try {
            return navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при навигации назад: ${e.message}")
            // При ошибке возвращаемся на экран верхнего уровня (например, converterFragment)
            if (navController.currentDestination?.id != R.id.converterFragment) {
                navController.navigate(R.id.converterFragment)
            }
            return true
        }
    }

    // Обрабатываем физическую кнопку назад с учетом возможных ошибок
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            // Проверяем есть ли куда возвращаться в стеке навигации
            if (!navController.popBackStack()) {
                if (isTaskRoot) {
                    // Показываем диалог подтверждения выхода из приложения
                    showExitConfirmationDialog()
                } else {
                    super.onBackPressed()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при нажатии кнопки назад: ${e.message}")
            // При ошибке возвращаемся на экран верхнего уровня
            try {
                navController.navigate(R.id.converterFragment)
            } catch (e2: Exception) {
                // В случае повторной ошибки просто финишируем активность
                Log.e(TAG, "Критическая ошибка навигации: ${e2.message}")
                finish()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Выход из приложения")
            .setMessage("Вы действительно хотите выйти из приложения?")
            .setPositiveButton("Да") { _, _ -> finish() }
            .setNegativeButton("Нет", null)
            .show()
    }


    private fun updateNetworkIndicator(isConnected: Boolean) {
        Log.d(TAG, "updateNetworkIndicator: $isConnected")
        runOnUiThread {
            networkIndicator.isActivated = isConnected
            networkStatusText.text = if (isConnected) "Онлайн" else "Офлайн"
            networkStatusText.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isConnected) R.color.success else R.color.error
                )
            )
            Log.d(TAG, "UI обновлен: сеть ${if (isConnected) "доступна" else "недоступна"}")

            // 👉 Показать Snackbar если сеть пропала
            if (!isConnected) {
                if (networkSnackbar == null || !networkSnackbar!!.isShown) {
                    networkSnackbar = Snackbar.make(findViewById(android.R.id.content), "Нет подключения к сети", Snackbar.LENGTH_INDEFINITE)
                    networkSnackbar?.show()
                }
            } else {
                networkSnackbar?.dismiss()
            }
        }
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        if (!hasFullAccess()) {
            menu.findItem(R.id.action_add_sensor)?.isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Сначала пробуем обработать с помощью NavController
        try {
            val handled = NavigationUI.onNavDestinationSelected(item, navController)
            if (handled) return true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выборе пункта меню: ${e.message}")
        }

        return when (item.itemId) {
            R.id.action_add_sensor -> {
                try {
                    navController.navigate(R.id.addSensorFragment)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при навигации к addSensorFragment: ${e.message}")
                    false
                }
            }
            R.id.action_settings -> {
                try {
                    navController.navigate(R.id.settingsFragment)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при навигации к settingsFragment: ${e.message}")
                    false
                }
            }
            R.id.action_about -> {
                try {
                    navController.navigate(R.id.about)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при навигации к aboutFragment: ${e.message}")
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    // При возобновлении активности заново устанавливаем белую стрелку
    override fun onResume() {
        super.onResume()
        setupWhiteBackArrow()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}