package com.reftgres.taihelper.ui.reset

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reftgres.taihelper.R
import com.reftgres.taihelper.ui.utils.SnackbarUtils
import com.reftgres.taihelper.databinding.FragmentForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private val forgotPasswordViewModel: ForgotPasswordViewModel by viewModels()
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                SnackbarUtils.showSnackbar(binding.root, "Введите email", isError = true, anchorView = binding.btnResetPassword)
                return@setOnClickListener
            }

            forgotPasswordViewModel.sendPasswordResetEmail(email)
        }


        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }

        forgotPasswordViewModel.resetState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ForgotPasswordState.Loading -> showLoading(true)
                is ForgotPasswordState.Success -> {
                    showLoading(false)
                    showToast("Письмо для сброса пароля отправлено!")
                    findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
                }
                is ForgotPasswordState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnResetPassword.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
