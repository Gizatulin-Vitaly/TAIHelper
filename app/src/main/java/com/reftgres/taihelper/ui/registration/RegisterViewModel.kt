package com.reftgres.taihelper.ui.registration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val registrationResult = MutableLiveData<String>()

    // Метод для регистрации пользователя
    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                // Регистрация пользователя
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.let {
                                // Здесь можно сохранить имя в базе данных, если нужно
                                it.sendEmailVerification()
                                    .addOnCompleteListener { verificationTask ->
                                        if (verificationTask.isSuccessful) {
                                            registrationResult.value = "Verification email sent."
                                        } else {
                                            registrationResult.value = "Failed to send verification email."
                                        }
                                    }
                            }
                        } else {
                            registrationResult.value = "Registration failed: ${task.exception?.message}"
                        }
                    }
            } catch (e: Exception) {
                registrationResult.value = "Error: ${e.message}"
            }
        }
    }
}
