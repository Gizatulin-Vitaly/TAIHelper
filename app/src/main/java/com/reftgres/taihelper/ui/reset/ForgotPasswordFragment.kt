package com.reftgres.taihelper.ui.reset
import com.google.firebase.auth.FirebaseAuth
import com.reftgres.taihelper.databinding.FragmentResetPasswordBinding
import android.view.ViewGroup
import android.view.LayoutInflater
import com.reftgres.taihelper.ui.registration.RegisterViewModel
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.reftgres.taihelper.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import android.widget.EditText
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private val registerViewModel: RegisterViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var btnBack: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etEmail = view.findViewById(R.id.etEmail)
        btnResetPassword = view.findViewById(R.id.btnResetPassword)
        btnBack = view.findViewById(R.id.btnBack)

        btnResetPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerViewModel.resetPassword(email)
        }

        btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }

        registerViewModel.registrationResult.observe(viewLifecycleOwner, Observer { result ->
            if (result == "reset_success") {
                Toast.makeText(requireContext(), "Письмо для сброса пароля отправлено!", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
            } else if (result.startsWith("Ошибка")) {
                Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
            }
        })
    }
}
