package com.reftgres.taihelper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageRepository: LanguagePreferencesRepository
) : ViewModel() {
    val currentLanguage = languageRepository.languageFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "ru"
    )

    fun setLanguage(language: String) {
        viewModelScope.launch {
            languageRepository.setLanguage(language)
        }
    }
}
