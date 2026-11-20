package com.fitcoach.app.ui.anamnesis

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import com.fitcoach.app.MainActivity
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.theme.FitCoachTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Preme DAVVERO il pulsante "Genera piano alimentare e scheda" e osserva cosa
 * cambia sullo schermo.
 *
 * Nasce dalla segnalazione: "quando clicchi su genera piano alimentare e scheda
 * non fa nulla come se non premesse l'input del bottone". Un test che chiama
 * direttamente il ViewModel non copre quel sintomo: qui il tocco passa dalla
 * gerarchia Compose reale, come il dito dell'utente.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class GenerateButtonClickTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun stateAfterConfirm(generating: Boolean, message: String? = null) = AnamnesisUiState(
        loading = false,
        profile = UserProfile(
            name = "Marco", ageYears = 32, heightCm = 180.0, weightKg = 82.0,
            anamnesisCompleted = true, anamnesisConfirmedAt = 1L
        ),
        current = null,
        answeredCount = AnamnesisEngine.questions.size,
        totalRequired = AnamnesisEngine.questions.count { it.required },
        confirmed = true,
        summary = AnamnesisEngine.summary(
            UserProfile(
                name = "Marco", ageYears = 32, heightCm = 180.0, weightKg = 82.0,
                anamnesisCompleted = true, anamnesisConfirmedAt = 1L
            )
        ),
        generatingPrograms = generating,
        generationMessage = message
    )

    @Test
    fun il_tocco_sul_pulsante_arriva_al_callback() {
        var premuto = 0
        composeRule.activity.setContent {
            FitCoachTheme {
                Surface(Modifier.fillMaxSize()) {
                    AnamnesisContent(
                        state = stateAfterConfirm(generating = false),
                        onNavigateDashboard = {}, onOpenSummary = {}, onCloseSummary = {},
                        onConfirm = {}, onSubmit = {}, onBack = {}, onSkip = {},
                        onToggleOption = { _, _ -> }, onUpdateInput = {},
                        onRetryGeneration = { premuto++ }
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Genera piano alimentare e scheda").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue("il tocco sul pulsante non ha raggiunto il callback", premuto == 1)
    }

    @Test
    fun durante_la_generazione_lo_schermo_mostra_avanzamento_e_nessun_pulsante_muto() {
        composeRule.activity.setContent {
            FitCoachTheme {
                Surface(Modifier.fillMaxSize()) {
                    AnamnesisContent(
                        state = stateAfterConfirm(
                            generating = true,
                            message = "Tentativo 2 di 8: il Coach AI sta correggendo il programma..."
                        ),
                        onNavigateDashboard = {}, onOpenSummary = {}, onCloseSummary = {},
                        onConfirm = {}, onSubmit = {}, onBack = {}, onSkip = {},
                        onToggleOption = { _, _ -> }, onUpdateInput = {},
                        onRetryGeneration = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // L'avanzamento reale deve essere visibile: e' cio che distingue
        // "sta lavorando" da "si e bloccata".
        composeRule.onNodeWithText("Tentativo 2 di 8: il Coach AI sta correggendo il programma...")
            .assertIsDisplayed()
        // Mentre genera, il pulsante non deve restare premibile: un secondo tocco
        // non deve accodare una seconda generazione.
        composeRule.onNodeWithText("Genera piano alimentare e scheda").assertDoesNotExist()

        // Prova di cosa vede l'utente: l'albero visivo reale, scritto su file.
        val albero = composeRule.onRoot().printToString(maxDepth = 12)
        val out = File("build/reports/ui/generazione-in-corso.txt")
        out.parentFile?.mkdirs()
        out.writeText(albero)
        assertTrue(
            "l'avanzamento non compare nell'albero visivo",
            albero.contains("Tentativo 2 di 8")
        )
    }
}
