package com.reftgres.taihelper.ui.registration

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.reftgres.taihelper.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val registerViewModel: RegisterViewModel by viewModels()

    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPosition: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.etName)
        etLastName = view.findViewById(R.id.etLastName)
        etPosition = view.findViewById(R.id.etPosition)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnRegister = view.findViewById(R.id.btnRegister)
        btnBack = view.findViewById(R.id.btnBack)
        progressBar = view.findViewById(R.id.progressBar)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val position = etPosition.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || lastName.isEmpty() || position.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Некорректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            registerViewModel.registerUser(name, lastName, position, email, password)
        }

        btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        registerViewModel.registrationResult.observe(viewLifecycleOwner, Observer { result ->
            progressBar.visibility = View.GONE
            btnRegister.isEnabled = true

            if (result == "success") {
                val user = registerViewModel.getCurrentUser()
                user?.reload()?.addOnCompleteListener {
                    if (user.isEmailVerified) {
                        Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    } else {
                        Toast.makeText(requireContext(), "Подтвердите email, прежде чем войти!", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                }
            } else {
                Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
            }
        })
    }

}
