package com.reftgres.taihelper.ui.authorization

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LoginRepository @Inject constructor(private val auth: FirebaseAuth) {

    // Вход в аккаунт
    suspend fun login(email: String, password: String): FirebaseUser {
        return suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            continuation.resume(user)
                        } else {
                            continuation.resumeWithException(Exception("Ошибка: пользователь не найден"))
                        }
                    } else {
                        continuation.resumeWithException(task.exception ?: Exception("Ошибка входа"))
                    }
                }
        }
    }

    // Регистрация нового пользователя
    suspend fun register(email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.sendEmailVerification()?.await() // Отправка подтверждения email
    }

    // Выход из аккаунта
    fun logout() {
        auth.signOut()
    }

    // Получение текущего пользователя
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Обновление имени пользователя
    suspend fun updateUserName(name: String) {
        val user = auth.currentUser ?: throw Exception("Пользователь не найден")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()
    }
}
