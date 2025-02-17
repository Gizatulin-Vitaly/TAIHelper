package com.reftgres.taihelper.ui.authtorization

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Закрытая переменная для хранения результата аутентификации
    private val _authResult = MutableLiveData<Boolean>()

    // Публичная LiveData для наблюдения в Fragment
    val authResult: LiveData<Boolean> get() = _authResult

    // Дополнительно создаем переменную для состояния авторизации
    private val _isUserLoggedIn = MutableLiveData<Boolean>()

    // Публичная LiveData для состояния авторизации
    val isUserLoggedIn: LiveData<Boolean> get() = _isUserLoggedIn

    init {
        // Инициализация состояния, проверяем авторизован ли пользователь при запуске
        _isUserLoggedIn.value = firebaseAuth.currentUser != null
    }

    // Функция для входа по email и паролю
    fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _authResult.value = task.isSuccessful
                if (task.isSuccessful) {
                    _isUserLoggedIn.value = true
                }
            }
            .addOnFailureListener {
                _authResult.value = false
            }
    }

    // Функция для выхода
    fun signOut() {
        firebaseAuth.signOut()
        _isUserLoggedIn.value = false
    }
}
