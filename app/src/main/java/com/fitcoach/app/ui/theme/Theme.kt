package com.fitcoach.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FitCoachDarkColors = darkColorScheme(
    primary = Emerald,
    onPrimary = Ink,
    secondary = Mint,
    background = Ink,
    surface = Charcoal,
    surfaceVariant = Slate,
    onBackground = Cloud,
    onSurface = Cloud,
    onSurfaceVariant = Fog
)

@Composable
fun FitCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitCoachDarkColors,
        typography = FitCoachTypography,
        content = content
    )
}
