package com.maumela.magnummanagement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.model.Theme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "prefs.preferences_pb") }

    // Verifies new users get the system theme and notifications on.
    @Test
    fun defaults_areSystemThemeAndNotificationsOn() = runTest {
        val store = PreferencesStore(newDataStore())
        assertEquals(Theme.SYSTEM, store.theme.first())
        assertTrue(store.notificationsEnabled.first())
    }

    // Verifies the chosen theme is saved and read back.
    @Test
    fun setTheme_persists() = runTest {
        val store = PreferencesStore(newDataStore())
        store.setTheme(Theme.DARK)
        assertEquals(Theme.DARK, store.theme.first())
    }

    // Verifies the notification preference is saved and read back.
    @Test
    fun setNotificationsEnabled_persists() = runTest {
        val store = PreferencesStore(newDataStore())
        store.setNotificationsEnabled(false)
        assertFalse(store.notificationsEnabled.first())
    }

    // Verifies logout resets preferences to defaults.
    @Test
    fun clear_restoresDefaults() = runTest {
        val store = PreferencesStore(newDataStore())
        store.setTheme(Theme.LIGHT)
        store.setNotificationsEnabled(false)
        store.clear()
        assertEquals(Theme.SYSTEM, store.theme.first())
        assertTrue(store.notificationsEnabled.first())
    }

    // Verifies an unknown stored theme falls back to SYSTEM instead of crashing.
    @Test
    fun unknownStoredTheme_fallsBackToSystem() = runTest {
        val dataStore = newDataStore()
        dataStore.edit { it[PreferencesStore.THEME_KEY] = "PURPLE" }
        assertEquals(Theme.SYSTEM, PreferencesStore(dataStore).theme.first())
    }
}