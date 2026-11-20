package com.fitcoach.app.ui.anamnesis

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.fitcoach.app.MainActivity
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.theme.FitCoachTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Dopo la conferma l'anamnesi non genera da sola: mostra il comando esplicito.
 *
 * Difetto osservato: alla conferma partiva subito il turno del Coach AI. Con il
 * provider non configurato la schermata restava su "Generazione..." senza che
 * l'utente avesse scelto nulla.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AnamnesisGenerateCommandTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun confirmedState() = AnamnesisUiState(
        loading = false,
        profile = UserProfile(
            name = "Marco", ageYears = 32, heightCm = 180.0, weightKg = 82.0,
            anamnesisCompleted = true, anamnesisConfirmedAt = 1L
        ),
        current = null,
        answeredCount = AnamnesisEngine.questions.size,
        totalRequired = AnamnesisEngine.questions.count { it.required },
        confirmed = true,
        generatingPrograms = false,
        generationFinished = false
    )

    private fun render(state: AnamnesisUiState, onGenerate: () -> Unit = {}) {
        composeRule.activity.setContent {
            FitCoachTheme {
                Surface(Modifier.fillMaxSize()) {
                    AnamnesisContent(
                        state = state,
                        onNavigateDashboard = {},
                        onOpenSummary = {},
                        onCloseSummary = {},
                        onConfirm = {},
                        onRetryGeneration = onGenerate,
                        onSubmit = {},
                        onBack = {},
                        onSkip = {},
                        onToggleOption = { _, _ -> },
                        onUpdateInput = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `confermata e ferma mostra il comando di generazione`() {
        var generated = 0
        render(confirmedState()) { generated++ }

        // Nessuna attesa in corso: la barra non c'e e il pulsante e' cliccabile.
        val button = composeRule.onAllNodesWithText("Genera piano alimentare e scheda")
            .fetchSemanticsNodes()
        assertThat(button).isNotEmpty()

        composeRule.onAllNodesWithText("Genera piano alimentare e scheda")[0].performClick()
        composeRule.waitForIdle()
        assertThat(generated).isEqualTo(1)
    }

    @Test
    fun `durante la generazione non si puo lanciare un secondo turno`() {
        render(confirmedState().copy(generatingPrograms = true))

        // Con la generazione in corso il comando sparisce: due turni in
        // parallelo scriverebbero lo stesso programma.
        val button = composeRule.onAllNodesWithText("Genera piano alimentare e scheda")
            .fetchSemanticsNodes()
        assertThat(button).isEmpty()
    }

    @Test
    fun `in errore il comando diventa riprova`() {
        render(confirmedState().copy(error = "Provider non raggiungibile."))

        val retry = composeRule.onAllNodesWithText("Riprova generazione AI")
            .fetchSemanticsNodes()
        assertThat(retry).isNotEmpty()
    }
}
