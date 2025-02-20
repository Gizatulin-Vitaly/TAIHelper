package com.reftgres.taihelper.ui.authorization

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.reftgres.taihelper.LoginActivity
import com.reftgres.taihelper.MainActivity
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.LoginFragmentBinding
import com.reftgres.taihelper.ui.authorization.LoginViewModel.LoginUiState
import com.reftgres.taihelper.ui.utils.SnackbarUtils
import com.reftgres.taihelper.utils.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            loginViewModel.loginUser(email, password)
        }

        binding.btnRegister.setOnClickListener {
            safeNavigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            safeNavigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.postDelayed({ checkBiometricAuth() }, 300)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.uiState.collectLatest { state ->
                when (state) {
                    is LoginUiState.Loading -> binding.btnLogin.isEnabled = false
                    is LoginUiState.Success -> {
                        binding.btnLogin.isEnabled = true
                        state.uid.let { BiometricHelper.saveAuthToken(requireContext(), it) }
                        if (isAdded) {
                            navigateToMain(state.name, state.status)
                        }
                    }
                    is LoginUiState.Error -> {
                        binding.btnLogin.isEnabled = true
                        SnackbarUtils.showSnackbar(binding.root, state.message, isError = true)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun checkBiometricAuth() {
        if (!isAdded || !isResumed) return
        if (BiometricHelper.isBiometricAvailable(requireContext())) {
            val savedUid = BiometricHelper.getAuthToken(requireContext())
            if (savedUid != null) {
                showBiometricLogin(savedUid)
            }
        }
    }

    private fun showBiometricLogin(uid: String) {
        BiometricHelper.showBiometricPrompt(
            requireActivity(),
            onSuccess = { loginViewModel.loginWithBiometric(uid) },
            onError = { error ->
                SnackbarUtils.showSnackbar(binding.root, "Ошибка биометрии: $error", isError = true)
            }
        )
    }

    private fun navigateToMain(name: String, status: String) {
        if (!isAdded || !isResumed) return
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            putExtra("USER_NAME", name)
            putExtra("USER_STATUS", status)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun safeNavigate(actionId: Int) {
        try {
            if (isAdded && isResumed && findNavController().currentDestination?.id == R.id.loginFragment) {
                findNavController().navigate(actionId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
