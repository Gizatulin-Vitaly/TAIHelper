package com.reftgres.taihelper.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.reftgres.taihelper.R

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val themeViewModel: ThemeViewModel by viewModels()
    private val languageViewModel: LanguageViewModel by viewModels()

    private var radioListener: RadioGroup.OnCheckedChangeListener? = null
    private var languageListener: RadioGroup.OnCheckedChangeListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val radioGroupTheme = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val radioGroupLanguage = view.findViewById<RadioGroup>(R.id.radioGroupLanguage)

        // Обработка изменения темы
        radioListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioLight -> AppTheme.LIGHT
                R.id.radioDark -> AppTheme.DARK
                R.id.radioSystem -> AppTheme.SYSTEM
                else -> AppTheme.SYSTEM
            }
            themeViewModel.setTheme(theme)
        }
        radioGroupTheme.setOnCheckedChangeListener(radioListener)

        // Обработка изменения языка
        languageListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                R.id.radioRussian -> "ru"
                R.id.radioEnglish -> "en"
                R.id.radioAlbanian -> "sq"
                else -> "ru"
            }
            languageViewModel.setLanguage(language)
            requireActivity().recreate() // Перезапуск активности для применения языка
        }
        radioGroupLanguage.setOnCheckedChangeListener(languageListener)

        // Устанавливаем текущие значения
        viewLifecycleOwner.lifecycleScope.launch {
            themeViewModel.currentTheme.collect { theme ->
                val selectedRadioButtonId = when (theme) {
                    AppTheme.LIGHT -> R.id.radioLight
                    AppTheme.DARK -> R.id.radioDark
                    AppTheme.SYSTEM -> R.id.radioSystem
                }
                if (radioGroupTheme.checkedRadioButtonId != selectedRadioButtonId) {
                    radioGroupTheme.setOnCheckedChangeListener(null)
                    radioGroupTheme.check(selectedRadioButtonId)
                    radioGroupTheme.setOnCheckedChangeListener(radioListener)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            languageViewModel.currentLanguage.collect { language ->
                val selectedLanguageId = when (language) {
                    "ru" -> R.id.radioRussian
                    "en" -> R.id.radioEnglish
                    "sq" -> R.id.radioAlbanian
                    else -> R.id.radioRussian
                }
                if (radioGroupLanguage.checkedRadioButtonId != selectedLanguageId) {
                    radioGroupLanguage.setOnCheckedChangeListener(null)
                    radioGroupLanguage.check(selectedLanguageId)
                    radioGroupLanguage.setOnCheckedChangeListener(languageListener)
                }
            }
        }

        return view
    }
}
