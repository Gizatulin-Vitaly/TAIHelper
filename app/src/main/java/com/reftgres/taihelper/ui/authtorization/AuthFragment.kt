package com.reftgres.taihelper.ui.authorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.FragmentAuthBinding
import com.reftgres.taihelper.ui.authtorization.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            loginWithEmailAndPassword(email, password)
        }

        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_registerFragment)
        }

        binding.resetPasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_resetPasswordFragment)
        }

        // Наблюдаем за результатом авторизации
        authViewModel.authResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                // Успешная авторизация
                Toast.makeText(requireContext(), "Авторизация успешна!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_authFragment_to_converterFragment) // Переход в следующий экран
            }
            result.onFailure { exception ->
                // Ошибка при авторизации
                Toast.makeText(requireContext(), "Ошибка: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun loginWithEmailAndPassword(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Введите email и пароль", Toast.LENGTH_SHORT).show()
            return
        }
        authViewModel.signIn(email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
