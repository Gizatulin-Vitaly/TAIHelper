package com.example.taihelper.ui.authorization

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    interface AuthCallback {
        fun onSuccess()
        fun onFailure(errorMessage: String)
    }

    fun login(email: String, password: String, callback: AuthCallback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onFailure(task.exception?.message ?: "Ошибка авторизации")
                }
            }
    }

    fun register(email: String, password: String, callback: AuthCallback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onFailure(task.exception?.message ?: "Ошибка регистрации")
                }
            }
    }
}
