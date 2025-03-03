package com.reftgres.taihelper

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.ui.authorization.LoginViewModel
import com.reftgres.taihelper.ui.settings.AppTheme
import com.reftgres.taihelper.ui.settings.ThemePreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var themeRepository: ThemePreferencesRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        val userName = intent.getStringExtra("USER_NAME") ?: "Неизвестный"
        val userStatus = intent.getStringExtra("USER_STATUS") ?: "Обычный"

        findViewById<TextView>(R.id.tvUserName).text = userName
        findViewById<TextView>(R.id.tvUserStatus).text = userStatus
        findViewById<TextView>(R.id.toolbarTitle).text = userName
        supportActionBar?.title = ""
        toolbar.alpha = 0.8f

        lifecycleScope.launch {
            themeRepository.themeFlow.collect { theme ->
                when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    AppTheme.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    AppTheme.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
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
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.settingsFragment)
                onBackPressedDispatcher.onBackPressed() // Более "мягкий" способ возврата
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
