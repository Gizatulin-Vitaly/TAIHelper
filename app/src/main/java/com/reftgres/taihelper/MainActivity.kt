package com.reftgres.taihelper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.ui.authtorization.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null) {
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigationView.setupWithNavController(navController)

            // 🔹 Проверяем, авторизован ли пользователь
            authViewModel.isUserLoggedIn.observe(this) { isLoggedIn ->
                if (!isLoggedIn) {
                    navController.navigate(R.id.authFragment)
                }
            }
        } else {
            throw IllegalStateException("NavController не был найден!")
        }
    }
}
