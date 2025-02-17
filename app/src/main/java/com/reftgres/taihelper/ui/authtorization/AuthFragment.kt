package com.reftgres.taihelper.ui.authorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.reftgres.taihelper.R
import com.reftgres.taihelper.ui.authtorization.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels() // Подключаем ViewModel
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()

        val binding = inflater.inflate(R.layout.fragment_auth, container, false)

        val loginButton: Button = binding.findViewById(R.id.login_button)
        val registerButton: Button = binding.findViewById(R.id.register_button)
        val resetPasswordButton: Button = binding.findViewById(R.id.resetPasswordButton)

        // Кнопка для логина
        loginButton.setOnClickListener {
            val email = binding.findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = binding.findViewById<EditText>(R.id.passwordEditText).text.toString()
            loginWithEmailAndPassword(email, password)
        }

        // Кнопка для перехода на экран регистрации
        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_registerFragment)
        }

        // Кнопка для перехода на экран восстановления пароля
        resetPasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_resetPasswordFragment)
        }

        return binding
    }

    // Логин с использованием Firebase
    private fun loginWithEmailAndPassword(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both email and password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        // Переход в главный экран
                        Toast.makeText(requireContext(), "Welcome ${user.email}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Please verify your email.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
