package com.fitcoach.app.collaudo

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.ui.onboarding.OnboardingScreen
import com.fitcoach.app.ui.onboarding.OnboardingViewModel
import com.fitcoach.app.ui.theme.FitCoachTheme
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.navigation.compose.rememberNavController

/**
 * Onboarding COMPILATO A TOCCHI, non riempito via `AnamnesisEngine`.
 *
 * `OpenRouterLiveProbe` costruisce il profilo applicando le risposte al motore:
 * verifica la generazione, non il form. Restava non provato proprio cio' che
 * l'utente fa per primo, cioe' scrivere nei campi, scegliere le chip e premere
 * "Salva profilo". Un campo con l'etichetta sbagliata, un passo che non avanza
 * o una validazione che rifiuta un valore valido non sarebbero emersi.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class OnboardingFormCollaudoTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: FitCoachDatabase
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = OnboardingViewModel(
            UserRepository(db.userProfileDao()),
            PreferencesRepository(db.userPreferencesDao()),
            ProgressRepository(db.progressDao(), db.checkInDao()),
        )
        composeRule.setContent {
            FitCoachTheme {
                OnboardingScreen(navController = rememberNavController(), viewModel = viewModel)
            }
        }
        // Lo stato parte `loading = true` e si azzera in una coroutine del
        // ViewModel: waitForIdle non attende quel lavoro, e leggere subito
        // significherebbe leggere la schermata di caricamento.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Passo 1 di 8").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun avanti() {
        composeRule.onNodeWithText("Avanti").performClick()
        composeRule.waitForIdle()
    }

    private fun scrivi(label: String, testo: String) {
        composeRule.onNodeWithText(label).performTextReplacement(testo)
        composeRule.waitForIdle()
    }

    /**
     * Il percorso reale: 8 passi, campi scritti a mano, chip selezionate,
     * infine "Salva profilo". Se un passo non avanza o un campo non accetta il
     * valore, il profilo finale non e' quello scritto qui.
     */
    @Test
    fun `compilare l onboarding a tocchi salva il profilo scritto nei campi`() {
        // Passo 1 — anagrafica.
        composeRule.onNodeWithText("Passo 1 di 8").assertIsDisplayed()
        scrivi("Nome", "Giulia")
        scrivi("Eta", "29")
        scrivi("Altezza (cm)", "168")
        scrivi("Peso (kg)", "63,5")
        scrivi("Massa grassa % (facolt.)", "24")
        avanti()

        // Passo 2 — obiettivo.
        composeRule.onNodeWithText("Passo 2 di 8").assertIsDisplayed()
        composeRule.onNodeWithText(Goal.HYPERTROPHY.label).performClick()
        composeRule.waitForIdle()
        avanti()

        // Passi 3-7: si attraversano davvero, uno per uno, senza saltare.
        for (passo in 3..7) {
            composeRule.onNodeWithText("Passo $passo di 8").assertIsDisplayed()
            avanti()
        }

        // Passo 8 — riepilogo: qui si salva.
        composeRule.onNodeWithText("Passo 8 di 8").assertIsDisplayed()
        composeRule.onNodeWithText("Salva profilo").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runBlocking { db.userProfileDao().get() != null }
        }

        val salvato = runBlocking { db.userProfileDao().get()!! }
        assertWithMessage("il nome scritto nel campo deve finire nel profilo")
            .that(salvato.name).isEqualTo("Giulia")
        assertWithMessage("l'eta scritta nel campo deve finire nel profilo")
            .that(salvato.ageYears).isEqualTo(29)
        assertWithMessage("l'altezza scritta nel campo deve finire nel profilo")
            .that(salvato.heightCm).isEqualTo(168.0)
        assertWithMessage("il peso con la virgola deve essere interpretato come decimale")
            .that(salvato.weightKg).isEqualTo(63.5)
        assertWithMessage("la massa grassa deve finire nel profilo")
            .that(salvato.bodyFatPercent).isEqualTo(24.0)
        assertWithMessage("l'obiettivo scelto con la chip deve finire nel profilo")
            .that(salvato.goal).isEqualTo(Goal.HYPERTROPHY)
    }

    /**
     * La validazione deve rifiutare i valori fuori scala e dirlo, invece di
     * salvare un profilo inutilizzabile per i calcoli energetici.
     */
    @Test
    fun `un peso fuori scala non viene salvato e l errore viene mostrato`() {
        scrivi("Nome", "Luca")
        scrivi("Eta", "30")
        scrivi("Altezza (cm)", "178")
        scrivi("Peso (kg)", "900")
        avanti()
        for (passo in 2..8) {
            composeRule.onNodeWithText("Passo $passo di 8").assertIsDisplayed()
            if (passo == 8) {
                composeRule.onNodeWithText("Salva profilo").performClick()
            } else {
                avanti()
            }
        }
        composeRule.waitForIdle()

        val errore = composeRule.onAllNodesWithText("Inserisci: peso valido.", substring = true)
            .fetchSemanticsNodes()
        assertWithMessage("il peso 900 kg deve produrre un errore visibile")
            .that(errore).isNotEmpty()
        assertWithMessage("nessun profilo deve essere salvato con un peso non valido")
            .that(runBlocking { db.userProfileDao().get() }).isNull()
    }

    /** Il sesso si sceglie dalle chip e deve arrivare al profilo: serve alle formule. */
    @Test
    fun `il sesso scelto con le chip arriva al profilo`() {
        scrivi("Nome", "Sara")
        scrivi("Eta", "27")
        scrivi("Altezza (cm)", "170")
        scrivi("Peso (kg)", "60")
        composeRule.onNodeWithText("Donna").performClick()
        composeRule.waitForIdle()
        avanti()
        for (passo in 2..8) {
            composeRule.onNodeWithText("Passo $passo di 8").assertIsDisplayed()
            if (passo == 8) {
                composeRule.onNodeWithText("Salva profilo").performClick()
            } else {
                avanti()
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runBlocking { db.userProfileDao().get() != null }
        }
        assertWithMessage("il sesso scelto con la chip deve finire nel profilo")
            .that(runBlocking { db.userProfileDao().get()!! }.sex)
            .isEqualTo(Sex.FEMALE)
    }
}
