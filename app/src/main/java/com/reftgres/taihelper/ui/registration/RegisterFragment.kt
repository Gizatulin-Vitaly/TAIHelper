package com.reftgres.taihelper.ui.registration

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reftgres.taihelper.R
import com.reftgres.taihelper.data.RegisterState
import com.reftgres.taihelper.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val registerViewModel: RegisterViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRegisterBinding.bind(view)

        binding.btnRegister.setOnClickListener {
            registerViewModel.registerUser(
                binding.etName.text.toString().trim(),
                binding.etLastName.text.toString().trim(),
                binding.etPosition.text.toString().trim(),
                binding.etEmail.text.toString().trim(),
                binding.etPassword.text.toString().trim()
            )
        }

        binding.btnBack.setOnClickListener {
            registerViewModel.onBackClicked()
        }

        registerViewModel.registrationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is RegisterState.Loading -> showLoading(true)
                is RegisterState.Success -> navigateToLogin()
                is RegisterState.Error -> showError(result.message)
            }
        }

        registerViewModel.navigateToLogin.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                registerViewModel.onBackHandled() // Сброс события, чтобы не повторялось
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }

    private fun navigateToLogin() {
        Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
