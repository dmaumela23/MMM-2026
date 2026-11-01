package com.maumela.magnummanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.navigation.MmmApp
import com.maumela.magnummanagement.ui.theme.MmmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MmmApplication).container
        setContent {
            // The saved theme preference (Light / Dark / System) applies instantly.
            val theme by container.preferencesStore.theme.collectAsStateWithLifecycle(initialValue = Theme.SYSTEM)
            MmmTheme(themeSetting = theme) {
                MmmApp(container)
            }
        }
    }
}
