package com.maumela.magnummanagement.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.maumela.magnummanagement.data.model.Theme

private val LightColors = lightColorScheme(
    primary = Navy800,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Navy800,
    secondary = ElectricBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E5FF),
    onSecondaryContainer = Navy800,
    tertiary = GoldDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBEFC4),
    onTertiaryContainer = Color(0xFF3D2F00),
    background = Grey50,
    onBackground = Color(0xFF0B1220),
    surface = Color.White,
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey600,
    outline = Grey400,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = ElectricBlueLight,
    onPrimary = Navy900,
    primaryContainer = Color(0xFF1B3A7A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Gold,
    onSecondary = Navy900,
    secondaryContainer = Color(0xFF4A3B07),
    onSecondaryContainer = GoldLight,
    tertiary = ElectricBlue,
    onTertiary = Navy900,
    background = Navy900,
    onBackground = Color(0xFFE8ECF5),
    surface = Navy800,
    onSurface = Color(0xFFE8ECF5),
    surfaceVariant = Navy600,
    onSurfaceVariant = Grey200Dark,
    outline = Color(0xFF5C6A86),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * The app theme. [themeSetting] is the user's saved choice (Light / Dark / System).
 * Dynamic (wallpaper) colour is deliberately not used so the MMM brand colours stay consistent.
 */
@Composable
fun MmmTheme(themeSetting: Theme = Theme.SYSTEM, content: @Composable () -> Unit) {
    val darkTheme = when (themeSetting) {
        Theme.DARK -> true
        Theme.LIGHT -> false
        Theme.SYSTEM -> isSystemInDarkTheme()
    }

    // Keep status/navigation bar icons readable when the app theme differs from the system theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MmmTypography,
        content = content,
    )
}