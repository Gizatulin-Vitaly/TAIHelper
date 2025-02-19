package com.example.taihelper.ui.authorization

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taihelper.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>()
    val uiState: LiveData<LoginUiState> get() = _uiState

    fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = LoginUiState.Error("Введите email и пароль")
            return
        }

        _uiState.value = LoginUiState.Loading

        authRepository.login(email, password, object : AuthRepository.AuthCallback {
            override fun onSuccess() {
                val user = authRepository.getCurrentUser()
                if (user != null && user.isEmailVerified) {
                    _uiState.value = LoginUiState.Success
                } else {
                    authRepository.logout()
                    _uiState.value = LoginUiState.Error("Подтвердите email перед входом")
                }
            }

            override fun onFailure(errorMessage: String) {
                _uiState.value = LoginUiState.Error(errorMessage)
            }
        })
    }
}

sealed class LoginUiState {
    object Success : LoginUiState()
    object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
