package com.reftgres.taihelper.ui.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.FragmentRegisterBinding
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavDirections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.reftgres.taihelper.ui.registration.RegisterViewModel


class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            // Проверка одинаковости паролей
            if (password != confirmPassword) {
                Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Проверка на пустые поля
            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Регистрируем пользователя
            registerUser(name, email, password)
        }

        return binding.root
    }

    private fun registerUser(name: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                // Сохраняем имя пользователя в Firebase Database
                                saveUserName(user.uid, name)

                                Toast.makeText(context, "Письмо с подтверждением отправлено", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ошибка отправки письма с подтверждением", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserName(uid: String, name: String) {
        val userRef = database.child("users").child(uid)
        userRef.child("name").setValue(name)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Имя пользователя сохранено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка сохранения имени пользователя", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
