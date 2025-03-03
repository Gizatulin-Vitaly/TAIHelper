package com.reftgres.taihelper.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
    }

    val themeFlow: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
            AppTheme.valueOf(themeName)
        }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}