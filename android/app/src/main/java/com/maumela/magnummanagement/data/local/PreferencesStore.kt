package com.maumela.magnummanagement.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maumela.magnummanagement.data.model.Theme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Theme and notification preference, saved on the device so they apply instantly
 * (even before the server answers). The server copy (PUT /settings) is kept in sync
 * by SettingsRepository.
 */
class PreferencesStore(private val dataStore: DataStore<Preferences>) {

    private val safeData: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val theme: Flow<Theme> = safeData.map { prefs ->
        Theme.entries.firstOrNull { it.name == prefs[THEME_KEY] } ?: Theme.SYSTEM
    }

    val notificationsEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[NOTIFICATIONS_KEY] ?: true
    }

    suspend fun setTheme(theme: Theme) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
    }

    /** Called on logout so the next account starts from defaults. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    }
}