package com.reftgres.taihelper.ui.authorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _uiState.emit(LoginUiState.Loading)
            try {
                val user = loginRepository.login(email, password)
                if (user.isEmailVerified) {
                    _uiState.emit(LoginUiState.Success(user.uid))
                } else {
                    loginRepository.logout()
                    _uiState.emit(LoginUiState.Error("Подтвердите email перед входом"))
                }
            } catch (e: Exception) {
                _uiState.emit(LoginUiState.Error(e.message ?: "Ошибка входа"))
            }
        }
    }

    fun registerUser(email: String, password: String) {
        viewModelScope.launch {
            _uiState.emit(LoginUiState.Loading)
            try {
                loginRepository.register(email, password)
                _uiState.emit(LoginUiState.Success("Регистрация успешна. Проверьте email!"))
            } catch (e: Exception) {
                _uiState.emit(LoginUiState.Error(e.message ?: "Ошибка регистрации"))
            }
        }
    }

    fun logout() {
        loginRepository.logout()
    }

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        data class Success(val token: String?) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }
}
