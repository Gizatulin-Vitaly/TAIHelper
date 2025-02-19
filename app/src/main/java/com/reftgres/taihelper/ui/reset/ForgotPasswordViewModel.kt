package com.reftgres.taihelper.ui.authorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Функция для сброса пароля
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isEmpty()) {
            onResult(false, "Please enter an email address.")
            return
        }

        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onResult(true, "Password reset email sent.")
                        } else {
                            onResult(false, "Failed to send password reset email.")
                        }
                    }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }
}
