package dev.fitcoach.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Schema scuro del mockup: ciano come colore d'azione, viola per l'AI, card
 * slate su fondo blu-notte.
 */
private val DarkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = InkOnNeon,
    primaryContainer = Color(0xFF063A47),
    onPrimaryContainer = NeonCyanSoft,
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A1140),
    onSecondaryContainer = Color(0xFFE2C9FF),
    tertiary = NeonGreen,
    onTertiary = InkOnNeon,
    tertiaryContainer = Color(0xFF04301C),
    onTertiaryContainer = NeonGreen,
    background = BgDeep,
    onBackground = TextHi,
    surface = CardNavy,
    onSurface = TextHi,
    surfaceVariant = CardNavyHigh,
    onSurfaceVariant = TextMid,
    surfaceContainer = BgNavy,
    surfaceContainerHigh = CardNavy,
    surfaceContainerHighest = CardNavyHigh,
    outline = StrokeNavy,
    outlineVariant = StrokeNavySoft,
    error = NeonRed,
    onError = Color.White,
    scrim = Color(0xCC05070C)
)

/** Variante chiara: stesso impianto, inchiostro scuro e accenti più profondi. */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF0091A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF7FF),
    onPrimaryContainer = Color(0xFF00343D),
    secondary = Color(0xFF7A2BD9),
    onSecondary = Color.White,
    tertiary = Color(0xFF00A34A),
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight2,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight3,
    onSurfaceVariant = OnSurfaceLightVariant,
    outline = OutlineLight,
    error = Color(0xFFD32F3F),
    onError = Color.White
)

/** Card e bottoni molto arrotondati, come nel mockup (16-28dp). */
private val FitShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FitCoachTheme(
    darkTheme: Boolean = true,
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
