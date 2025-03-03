package com.reftgres.taihelper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemePreferencesRepository
) : ViewModel() {
    val currentTheme = themeRepository.themeFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppTheme.SYSTEM
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
        }
    }
}