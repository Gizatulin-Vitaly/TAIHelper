package com.reftgres.taihelper.ui.authtorization

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.reftgres.taihelper.MainActivity
import com.reftgres.taihelper.databinding.FragmentAuthBinding
import com.reftgres.taihelper.R

import java.util.concurrent.Executor

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private lateinit var binding: FragmentAuthBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAuthBinding.bind(view)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Наблюдаем за состоянием авторизации
        authViewModel.isUserLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn) {
                // Если пользователь уже авторизован, перенаправляем на MainActivity
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim() // Убираем пробелы
            val password = binding.etPassword.text.toString().trim() // Убираем пробелы

            // Проверка на пустое значение
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                // Вход, если данные валидны
                authViewModel.signIn(email, password)
            }
        }


        authViewModel.authResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Ошибка входа!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFingerprint.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(requireContext(), "Аутентификация не удалась", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(requireContext(), "Ошибка: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Вход по отпечатку")
            .setNegativeButtonText("Отмена")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
