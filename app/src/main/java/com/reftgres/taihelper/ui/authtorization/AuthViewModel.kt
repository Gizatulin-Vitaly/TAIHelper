package com.reftgres.taihelper.ui.authtorization

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authResult = MutableLiveData<Result<Boolean>>()
    val authResult: LiveData<Result<Boolean>> get() = _authResult

    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> get() = _isUserLoggedIn

    init {
        _isUserLoggedIn.value = firebaseAuth.currentUser != null
    }

    fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user?.isEmailVerified == true) {
                        _isUserLoggedIn.value = true
                        _authResult.value = Result.success(true)
                    } else {
                        _authResult.value = Result.failure(Exception("Подтвердите email перед входом"))
                        firebaseAuth.signOut()
                    }
                } else {
                    _authResult.value = Result.failure(task.exception ?: Exception("Ошибка входа"))
                }
            }
    }

    fun signUp(email: String, password: String, name: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.sendEmailVerification()

                    val userData = hashMapOf("name" to name, "accessLevel" to "user")
                    firestore.collection("users").document(user!!.uid).set(userData)

                    _authResult.value = Result.success(true)
                } else {
                    _authResult.value = Result.failure(task.exception ?: Exception("Ошибка регистрации"))
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _isUserLoggedIn.value = false
    }
}
