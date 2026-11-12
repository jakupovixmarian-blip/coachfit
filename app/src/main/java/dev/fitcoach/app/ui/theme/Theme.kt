package dev.fitcoach.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = OnPrimaryInk,
    primaryContainer = GreenDark,
    onPrimaryContainer = OnSurfaceDark,
    secondary = GreenLight,
    onSecondary = OnPrimaryInk,
    tertiary = AmberAccent,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark2,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark3,
    onSurfaceVariant = OnSurfaceDarkVariant,
    error = RedAccent,
    outline = SurfaceDark3
)

private val LightScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = SurfaceLight2,
    primaryContainer = GreenLight,
    onPrimaryContainer = OnSurfaceLight,
    secondary = GreenPrimary,
    onSecondary = SurfaceLight2,
    tertiary = AmberAccent,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight2,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = OnSurfaceLightVariant,
    error = RedAccent
)

@Composable
fun FitCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = FitTypography, content = content)
}
