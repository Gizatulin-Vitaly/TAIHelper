package com.reftgres.taihelper.ui.reset
import com.google.firebase.auth.FirebaseAuth
import com.reftgres.taihelper.databinding.FragmentResetPasswordBinding
import android.view.ViewGroup
import android.view.LayoutInflater
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

class ResetPasswordFragment : Fragment() {

    private lateinit var binding: FragmentResetPasswordBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false)

        binding.resetPasswordButton.setOnClickListener {
            val email = binding.emailResetPasswordEditText.text.toString()

            // Проверка на пустое поле
            if (email.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, введите ваш email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Отправка запроса на сброс пароля
            resetPassword(email)
        }

        return binding.root
    }

    private fun resetPassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Инструкция для восстановления пароля отправлена на email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
