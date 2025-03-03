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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val radioGroupTheme = view.findViewById<RadioGroup>(R.id.radioGroupTheme)

        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioLight -> AppTheme.LIGHT
                R.id.radioDark -> AppTheme.DARK
                R.id.radioSystem -> AppTheme.SYSTEM
                else -> AppTheme.SYSTEM
            }
            themeViewModel.setTheme(theme)
        }

        // Наблюдение за текущей темой
        viewLifecycleOwner.lifecycleScope.launch {
            themeViewModel.currentTheme.collect { theme ->
                // Установка выбранной радиокнопки
                val selectedRadioButtonId = when (theme) {
                    AppTheme.LIGHT -> R.id.radioLight
                    AppTheme.DARK -> R.id.radioDark
                    AppTheme.SYSTEM -> R.id.radioSystem
                }
                radioGroupTheme.check(selectedRadioButtonId)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Скрываем нижнее меню
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Возвращаем видимость нижнего меню при выходе из фрагмента
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }
}