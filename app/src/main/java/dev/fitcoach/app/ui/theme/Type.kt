package dev.fitcoach.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Numbers carry the meaning in a training app, so the display sizes are large
 * and tight; labels are small and spaced to stay readable on dark surfaces.
 */
val FitTypography = Typography(
    displayMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp)
)
