package com.reftgres.taihelper.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun login(email: String, password: String): FirebaseUser? {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            if (result.user?.isEmailVerified == true) result.user else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(email: String, password: String, name: String): Boolean {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            user?.sendEmailVerification()

            val userData = hashMapOf("name" to name, "accessLevel" to "user")
            user?.let {
                firestore.collection("users").document(it.uid).set(userData).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
