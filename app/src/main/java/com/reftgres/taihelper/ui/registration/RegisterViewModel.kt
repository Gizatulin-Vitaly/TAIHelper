package com.reftgres.taihelper.ui.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reftgres.taihelper.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _registrationResult = MutableLiveData<String>()
    val registrationResult: LiveData<String> get() = _registrationResult

    fun registerUser(name: String, lastName: String, position: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val userId = user?.uid ?: return@addOnCompleteListener

                    // Отправка письма с подтверждением
                    user.sendEmailVerification()
                        .addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                saveUserData(userId, name, lastName, position, email)
                            } else {
                                _registrationResult.value = "Ошибка отправки письма: ${verificationTask.exception?.message}"
                            }
                        }
                } else {
                    _registrationResult.value = task.exception?.message ?: "Ошибка регистрации"
                }
            }
    }

    fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _registrationResult.value = "reset_success"
                } else {
                    _registrationResult.value = "Ошибка: ${task.exception?.message}"
                }
            }
    }



    private fun saveUserData(userId: String, name: String, lastName: String, position: String, email: String) {
        val user = User(name = name, lastName = lastName, position = position, email = email)

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                _registrationResult.value = "success"
            }
            .addOnFailureListener { e ->
                _registrationResult.value = "Ошибка сохранения данных: ${e.message}"
            }
    }
    fun getCurrentUser() = auth.currentUser
}
