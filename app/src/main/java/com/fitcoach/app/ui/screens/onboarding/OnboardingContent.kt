package com.fitcoach.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* ============================================================================
 * Onboarding — installazione pulita. Nessun profilo, nessuna scheda, nessun
 * piano: solo l'introduzione al prodotto e l'accesso all'Anamnesi, che è
 * l'unico punto in cui nasce il percorso.
 * ========================================================================== */

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = state.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill(text = page.eyebrow, color = Emerald)
                Text(
                    text = "FitCoach 2.0",
                    style = FitTextStyles.caption,
                    color = TextLow,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = page.title,
                style = FitTextStyles.display,
                color = TextPrimary,
            )
            Text(
                text = page.body,
                style = FitTextStyles.body,
                color = TextSecondary,
            )
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Emerald),
                    )
                    Text(
                        text = page.highlight,
                        style = FitTextStyles.bodySmall,
                        color = TextPrimary,
                    )
                }
            }

            state.errorMessage?.let { message ->
                Text(text = message, style = FitTextStyles.bodySmall, color = ErrorRed)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(state.pages.size) { index ->
                    val active = index <= state.page
                    Spacer(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (index == state.page) 26.dp else 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) Emerald else FitSurfaceVariant),
                    )
                }
            }

            if (state.isLastPage) {
                PrimaryButton(
                    text = if (state.isRestoring) "Preparazione…" else "Inizia l'anamnesi",
                    onClick = onStart,
                    enabled = !state.isRestoring,
                )
                GhostButton(
                    text = "Ho già un piano su questo dispositivo",
                    onClick = onRestore,
                    enabled = !state.isRestoring,
                )
            } else {
                PrimaryButton(text = "Continua", onClick = onNext)
                GhostButton(text = "Salta introduzione", onClick = onSkip)
            }
        }
    }
}
