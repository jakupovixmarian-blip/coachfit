package dev.fitcoach.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = OnPrimaryInk,
    primaryContainer = GreenDark,
    onPrimaryContainer = OnSurfaceDark,
    secondary = GreenLight,
    onSecondary = OnPrimaryInk,
    tertiary = BlueAccent,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark2,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark3,
    onSurfaceVariant = OnSurfaceDarkVariant,
    surfaceContainerHighest = SurfaceDark4,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = RedAccent,
    onError = OnPrimaryInk
)

private val LightScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = SurfaceLight2,
    primaryContainer = GreenLight,
    onPrimaryContainer = OnSurfaceLight,
    secondary = GreenPrimary,
    onSecondary = OnPrimaryInk,
    tertiary = BlueAccent,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight2,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight3,
    onSurfaceVariant = OnSurfaceLightVariant,
    outline = OutlineLight,
    error = RedAccent,
    onError = SurfaceLight2
)

/** Cards are softly rounded; buttons a bit less, so they still read as controls. */
private val FitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = FitTypography, shapes = FitShapes, content = content)
}
