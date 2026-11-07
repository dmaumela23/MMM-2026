package com.maumela.magnummanagement

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.navigation.MmmApp
import com.maumela.magnummanagement.ui.theme.MmmTheme
import com.maumela.magnummanagement.utils.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MmmApplication).container

        // Load and apply the saved language preference on cold start if different
        lifecycleScope.launch {
            val savedLanguage = container.preferencesStore.language.first()
            if (savedLanguage != LocaleHelper.getCurrentLocale()) {
                val appLocale = LocaleListCompat.forLanguageTags(savedLanguage)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }

        setContent {
            // The saved theme preference (Light / Dark / System) applies instantly.
            val theme by container.preferencesStore.theme.collectAsStateWithLifecycle(initialValue = Theme.SYSTEM)
            MmmTheme(themeSetting = theme) {
                MmmApp(container)
            }
        }
    }
}