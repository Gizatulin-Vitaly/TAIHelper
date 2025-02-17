package com.reftgres.taihelper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer // Импортируем Observer
import androidx.fragment.app.commit
import com.reftgres.taihelper.ui.authtorization.AuthFragment
import com.reftgres.taihelper.ui.authtorization.AuthViewModel

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Наблюдаем за состоянием авторизации через LiveData
        authViewModel.isUserLoggedIn.observe(this, Observer { isLoggedIn ->
            if (isLoggedIn) {
                // Если пользователь авторизован, загружаем основной экран
                setContentView(R.layout.activity_main)
            } else {
                // Если не авторизован, заменяем на `AuthFragment`
                supportFragmentManager.commit {
                    replace(android.R.id.content, AuthFragment())
                }
            }
        })
    }
}
