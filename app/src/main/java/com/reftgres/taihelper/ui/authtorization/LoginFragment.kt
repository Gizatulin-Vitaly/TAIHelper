package com.example.taihelper.ui.authorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.reftgres.taihelper.LoginActivity
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.LoginFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()

        binding.btnLogin.setOnClickListener {
            loginViewModel.loginUser(
                binding.etEmail.text.toString().trim(),
                binding.etPassword.text.toString().trim()
            )
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }
    }

    private fun observeViewModel() {
        loginViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginUiState.Loading -> {
                    binding.btnLogin.isEnabled = false
                }

                is LoginUiState.Success -> {
                    binding.btnLogin.isEnabled = true
                    navigateToMain()
                }

                is LoginUiState.Error -> {
                    binding.btnLogin.isEnabled = true
                    showSnackbar(state.message)
                }
            }
        }
    }

    private fun navigateToMain() {
        (activity as? LoginActivity)?.navigateToMainActivity()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
