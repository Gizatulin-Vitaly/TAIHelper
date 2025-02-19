package com.reftgres.taihelper.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    // Проверяем поддержку биометрии
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    // Получаем зашифрованные SharedPreferences
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,  // Передаём context первым аргументом
            "secure_prefs",  // Имя файла
            masterKey,  // Передаём сгенерированный MasterKey
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Сохранение токена после успешного входа
    fun saveAuthToken(context: Context, token: String) {
        getEncryptedPrefs(context).edit().putString("auth_token", token).apply()
    }

    // Получение токена
    fun getAuthToken(context: Context): String? {
        return getEncryptedPrefs(context).getString("auth_token", null)
    }

    // Удаление токена (например, при выходе пользователя)
    fun clearAuthToken(context: Context) {
        getEncryptedPrefs(context).edit().remove("auth_token").apply()
    }

    // Показ диалога биометрии
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                override fun onAuthenticationFailed() {
                    onError("Биометрия не распознана")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Вход по биометрии")
            .setSubtitle("Подтвердите личность")
            .setNegativeButtonText("Отмена")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
