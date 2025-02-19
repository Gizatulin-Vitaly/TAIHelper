package com.reftgres.taihelper.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reftgres.taihelper.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {

    fun registerUser(
        name: String, lastName: String, position: String, email: String, password: String,
        callback: (RegisterState) -> Unit
    ) {
        callback(RegisterState.Loading)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user ?: return@addOnCompleteListener
                    user.sendEmailVerification()
                        .addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                saveUserData(user.uid, name, lastName, position, email, callback)
                            } else {
                                callback(RegisterState.Error("Ошибка отправки письма: ${verificationTask.exception?.message}"))
                            }
                        }
                } else {
                    callback(RegisterState.Error(task.exception?.message ?: "Ошибка регистрации"))
                }
            }
    }

    private fun saveUserData(userId: String, name: String, lastName: String, position: String, email: String, callback: (RegisterState) -> Unit) {
        val user = User(name = name, lastName = lastName, position = position, email = email)

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                callback(RegisterState.Success)
            }
            .addOnFailureListener { e ->
                callback(RegisterState.Error("Ошибка сохранения данных: ${e.message}"))
            }
    }
}
