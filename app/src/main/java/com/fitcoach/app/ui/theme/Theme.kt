package com.fitcoach.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FitDarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = FitBlack,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldLight,
    onSecondary = FitBlack,
    background = FitBlack,
    onBackground = TextPrimary,
    surface = FitSurface,
    onSurface = TextPrimary,
    surfaceVariant = FitSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = FitOutline,
    outlineVariant = FitOutline,
    error = ErrorRed,
    onError = Color.White,
    tertiary = CarbsColor,
    onTertiary = FitBlack
)

@Composable
fun FitCoachTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = FitDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitTypography,
        content = content
    )
}
