package com.reftgres.taihelper.ui.registration


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reftgres.taihelper.data.RegisterState
import com.reftgres.taihelper.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.LiveData

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _registrationResult = MutableLiveData<RegisterState>()
    val registrationResult: LiveData<RegisterState> get() = _registrationResult

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> get() = _navigateToLogin

    fun registerUser(name: String, lastName: String, position: String, email: String, password: String) {
        _registrationResult.value = RegisterState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val userId = user?.uid ?: return@addOnCompleteListener

                    user.sendEmailVerification()
                        .addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                saveUserData(userId, name, lastName, position, email)
                            } else {
                                _registrationResult.value = RegisterState.Error("Ошибка отправки письма: ${verificationTask.exception?.message}")
                            }
                        }
                } else {
                    _registrationResult.value = RegisterState.Error(task.exception?.message ?: "Ошибка регистрации")
                }
            }
    }

    fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _registrationResult.value = RegisterState.Success
                } else {
                    _registrationResult.value = RegisterState.Error("Ошибка: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserData(userId: String, name: String, lastName: String, position: String, email: String) {
        val user = User(name = name, lastName = lastName, position = position, email = email)

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                _registrationResult.value = RegisterState.Success
            }
            .addOnFailureListener { e ->
                _registrationResult.value = RegisterState.Error("Ошибка сохранения данных: ${e.message}")
            }
    }

    fun onBackClicked() {
        _navigateToLogin.value = true
    }

    fun onBackHandled() {
        _navigateToLogin.value = false
    }

    fun getCurrentUser() = auth.currentUser
}
