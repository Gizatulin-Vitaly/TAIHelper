package com.example.taihelper.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val auth: FirebaseAuth) {

    interface AuthCallback {
        fun onSuccess()
        fun onFailure(errorMessage: String)
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
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

    fun saveUserToFirestore(uid: String, email: String, callback: AuthCallback) {
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "emailVerified" to false // Можно обновить позже
        )

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                callback.onSuccess()
            }
            .addOnFailureListener { e ->
                callback.onFailure("Ошибка записи в Firestore: ${e.message}")
            }
    }

    fun register(email: String, password: String, callback: AuthCallback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                saveUserToFirestore(user.uid, email, callback)
                            } else {
                                Log.e("AuthError", "Ошибка отправки письма: ${verificationTask.exception?.message}")
                                callback.onFailure("Ошибка отправки письма: ${verificationTask.exception?.message}")
                            }
                        }
                } else {
                    callback.onFailure(task.exception?.message ?: "Ошибка регистрации")
                }
            }
    }




    fun sendEmailVerification(callback: AuthCallback) {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onFailure(task.exception?.message ?: "Ошибка отправки письма")
                }
            }
    }

    fun resetPassword(email: String, callback: AuthCallback) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onFailure(task.exception?.message ?: "Ошибка отправки письма")
                }
            }
    }

    fun logout() {
        auth.signOut()
    }
}
