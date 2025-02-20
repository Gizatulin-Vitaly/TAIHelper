package com.reftgres.taihelper.ui.authorization

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class LoginRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {

    suspend fun login(email: String, password: String): FirebaseUser {
        return suspendCoroutine { continuation ->
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

    suspend fun register(email: String, password: String) {
        suspendCoroutine<Unit> { continuation ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(task.exception ?: Exception("Ошибка регистрации"))
                    }
                }
        }
    }

    fun logout() {
        auth.signOut()
    }

    // 🔹 Получение данных пользователя по UID
    suspend fun getUserData(uid: String): UserData {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            val name = snapshot.getString("name") ?: "Неизвестный"
            val status = snapshot.getString("status") ?: "Обычный"
            UserData(name, status)
        } catch (e: Exception) {
            throw Exception("Ошибка загрузки данных: ${e.message}")
        }
    }
}

// 🔹 Модель данных пользователя
data class UserData(val name: String, val status: String)
