package com.example.taihelper.ui.authorization

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<String>()
    val loginResult: LiveData<String> get() = _loginResult

    private val _registrationResult = MutableLiveData<String>()
    val registrationResult: LiveData<String> get() = _registrationResult

    fun loginUser(email: String, password: String) {
        authRepository.login(email, password, object : AuthRepository.AuthCallback {
            override fun onSuccess() {
                _loginResult.value = "success"
            }

            override fun onFailure(errorMessage: String) {
                _loginResult.value = errorMessage
            }
        })
    }

    fun registerUser(email: String, password: String) {
        authRepository.register(email, password, object : AuthRepository.AuthCallback {
            override fun onSuccess() {
                _registrationResult.value = "checkEmail"
            }

            override fun onFailure(errorMessage: String) {
                _registrationResult.value = errorMessage
            }
        })
    }
}
