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
                    val userData = loginRepository.getUserData(user.uid)
                    _uiState.emit(LoginUiState.Success(user.uid, userData.name, userData.status))
                } else {
                    loginRepository.logout()
                    _uiState.emit(LoginUiState.Error("Подтвердите email перед входом"))
                }
            } catch (e: Exception) {
                _uiState.emit(LoginUiState.Error(e.message ?: "Ошибка входа"))
            }
        }
    }

    fun loginWithBiometric(uid: String) {
        viewModelScope.launch {
            _uiState.emit(LoginUiState.Loading)
            try {
                val userData = loginRepository.getUserData(uid)
                _uiState.emit(LoginUiState.Success(uid, userData.name, userData.status))
            } catch (e: Exception) {
                _uiState.emit(LoginUiState.Error("Ошибка биометрической авторизации: ${e.message}"))
            }
        }
    }

    fun logout() {
        loginRepository.logout()
    }

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        data class Success(val uid: String, val name: String, val status: String) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }
}
