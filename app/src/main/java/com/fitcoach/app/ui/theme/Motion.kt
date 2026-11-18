package com.fitcoach.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/* =============================================================================
 * FitCoach 2.0 — identita "Hyperion" · Motion.kt
 * -----------------------------------------------------------------------------
 * Material3 risolto = 1.3.1: MotionScheme / ExpressiveMotionScheme NON esistono
 * (arrivano nella 1.4.x-alpha). Qui quindi solo:
 *   animate*AsState, updateTransition, CubicBezierEasing, tween, spring.
 * Nessuna dipendenza nuova, nessun cambio di versione di Material3 o della BOM.
 * ========================================================================== */

/** Durate ufficiali, in millisecondi. */
object FitDuration {
    /** Feedback di pressione, cambi di icona. */
    const val Instant = 90

    /** Uscite, fade brevi. */
    const val Fast = 120

    /** Transizioni standard di colore/alpha. */
    const val Base = 200

    /** Cambio tab, apparizione di blocchi. */
    const val Medium = 260

    /** Ingresso schermata, apertura sheet (sharedAxisX / slideUp). */
    const val Screen = 320

    /** Crescita delle barre dei grafici. */
    const val Bars = 420

    /** Disegno dei grafici a linea e sweep dei ring. */
    const val Chart = 520

    /** Ingresso schermata, disegno grafici, roll dei numeri. */
    const val Slow = 380

    /** Apertura bottom sheet. */
    const val Sheet = 320

    /** Ciclo dello shimmer di caricamento. */
    const val Skeleton = 1400

    /** Ciclo del "pensiero" dell'AI. */
    const val Pulse = 2400
}

/** Curve ufficiali. */
object FitEasing {
    /** Default per la maggior parte dei movimenti. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Ingressi: parte veloce, atterra morbido. */
    val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Uscite: parte lento, accelera. */
    val Accelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Transizioni importanti (ingresso schermata, sheet). */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * Specifiche pronte all'uso. Esempio:
 *   val alpha by animateFloatAsState(target, animationSpec = FitMotion.standard())
 */
object FitMotion {
    /** Colore, alpha, dimensione: default. */
    fun <T> standard(durationMillis: Int = FitDuration.Base): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FitEasing.Standard)

    /** Ingresso di un blocco o di una schermata. */
    fun <T> enter(durationMillis: Int = FitDuration.Sheet): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FitEasing.Emphasized)

    /** Roll di un valore numerico, disegno di un grafico. */
    fun <T> value(durationMillis: Int = FitDuration.Slow): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FitEasing.Decelerate)

    /** Uscita / scomparsa. */
    fun <T> exit(durationMillis: Int = FitDuration.Fast): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FitEasing.Accelerate)

    /** Feedback di pressione (molla, non tween). */
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = 400f)

    /** Fattore di scala quando un elemento è premuto. */
    const val PressScale = 0.97f
}

/**
 * true se le animazioni di sistema sono attive.
 * Con "Rimuovi animazioni" o animator_duration_scale = 0 restituisce false:
 * i componenti devono saltare i loop (shimmer, pulse) e usare uno stato statico.
 */
@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) > 0f
    }
}

/**
 * Feedback di pressione standard: scala a 0.97 con molla.
 * Esempio:
 *   val interaction = remember { MutableInteractionSource() }
 *   Modifier.pressFeedback(interaction).clickable(interaction, indication = null) { ... }
 */
fun Modifier.pressFeedback(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) FitMotion.PressScale else 1f,
        animationSpec = FitMotion.press(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
