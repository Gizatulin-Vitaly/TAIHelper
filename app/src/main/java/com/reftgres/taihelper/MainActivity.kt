package com.reftgres.taihelper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.reftgres.taihelper.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Правильный порядок инициализации binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Находим NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Получаем NavController
        navController = navHostFragment.navController

        // Привязываем BottomNavigationView к NavController
        binding.bottomNavigationView.setupWithNavController(navController)
    }
}
