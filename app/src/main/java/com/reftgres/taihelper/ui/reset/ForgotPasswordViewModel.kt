package com.reftgres.taihelper.ui.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordRepository: ForgotPasswordRepository
) : ViewModel() {

    private val _resetState = MutableLiveData<ForgotPasswordState>()
    val resetState: LiveData<ForgotPasswordState> get() = _resetState

    fun sendPasswordResetEmail(email: String) {
        if (email.isEmpty()) {
            _resetState.value = ForgotPasswordState.Error("Введите email")
            return
        }

        viewModelScope.launch {
            _resetState.value = ForgotPasswordState.Loading
            val result = forgotPasswordRepository.sendPasswordResetEmail(email)
            _resetState.value = result.fold(
                onSuccess = { ForgotPasswordState.Success },
                onFailure = { ForgotPasswordState.Error(it.message ?: "Неизвестная ошибка") }
            )
        }
    }
}
