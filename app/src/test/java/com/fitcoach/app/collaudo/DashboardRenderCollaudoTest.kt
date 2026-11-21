package com.fitcoach.app.collaudo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.fitcoach.app.ui.screens.dashboard.CarbDayType
import com.fitcoach.app.ui.screens.dashboard.DashboardMeals
import com.fitcoach.app.ui.screens.dashboard.DashboardUiState
import com.fitcoach.app.ui.screens.dashboard.DashboardWorkout
import com.fitcoach.app.ui.screens.dashboard.DashboardWeight
import com.fitcoach.app.ui.screens.dashboard.WorkoutState
import com.fitcoach.app.ui.screens.dashboard.DashboardContent
import com.fitcoach.app.ui.theme.FitCoachTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Collaudo VISIVO della Dashboard agganciata alla grafica AppJet.
 *
 * Riproduce i difetti segnalati dall'utente:
 *  - "quando si inizia il workout la pagina dashboard sparisce / va in bug":
 *    la Dashboard deve restare disegnata anche con una sessione IN CORSO;
 *  - "quando clicchi sulla schermata ti porta al workout": il tocco deve
 *    attivare SOLO i comandi previsti, non un'area generica che porta alla
 *    schermata Allenamento.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w360dp-h800dp-xhdpi",
    application = SeededProfileApplication::class
)
class DashboardRenderCollaudoTest {

    /**
     * `createComposeRule()` e non `createAndroidComposeRule<MainActivity>()`.
     *
     * Questi test sostituiscono il contenuto per disegnare solo `DashboardContent`:
     * con MainActivity come host, la NavHost reale partiva comunque e la sua
     * lettura del profilo da Room correva contro la composizione del test. La
     * Dashboard veniva quindi letta a volte prima che il contenuto di test fosse
     * composto, e il test falliva nella suite ma non da solo.
     */
    @get:Rule
    val composeRule = createComposeRule()

    private class Clicks {
        var nutrition = 0
        var workout = 0
        var progress = 0
        var coach = 0
        var anamnesis = 0
        var logWeight = 0
        var skipWorkout = 0
        var generate = 0
        var retry = 0
        var recipes = 0
        var health = 0
        var settings = 0
    }

    private fun baseState(
        workout: DashboardWorkout? = DashboardWorkout(
            name = "Upper A",
            focus = "Petto · Spalle · Tricipiti",
            weekIndex = 1,
            weekTotal = 5,
            exerciseCount = 6,
            state = WorkoutState.PLANNED,
            canSkip = true,
        ),
        weight: DashboardWeight? = DashboardWeight(82.4, -0.3, listOf(83.1, 82.9, 82.4)),
    ) = DashboardUiState(
        isLoading = false,
        hasPlan = true,
        goalLabel = "Ricomposizione",
        greeting = "Buonasera, Marjan",
        weekdayLabel = "Mercoledì 12",
        dayType = CarbDayType.HIGH,
        kcalTarget = 2600,
        kcalConsumed = 960,
        proteinTargetGrams = 165.0,
        proteinConsumedGrams = 60.0,
        carbsTargetGrams = 300.0,
        carbsConsumedGrams = 110.0,
        fatTargetGrams = 70.0,
        fatConsumedGrams = 25.0,
        meals = DashboardMeals(logged = 2, planned = 5),
        workout = workout,
        weight = weight,
        sleepHours = 7.0,
        weeklyVolumeKg = 12500.0,
    )

    private fun render(state: DashboardUiState, clicks: Clicks) {
        composeRule.setContent {
            FitCoachTheme {
                Surface(Modifier.fillMaxSize()) {
                    DashboardContent(
                        state = state,
                        onOpenNutrition = { clicks.nutrition++ },
                        onOpenWorkout = { clicks.workout++ },
                        onOpenProgress = { clicks.progress++ },
                        onOpenCoach = { clicks.coach++ },
                        onOpenAnamnesis = { clicks.anamnesis++ },
                        onLogWeight = { clicks.logWeight++ },
                        onSkipWorkout = { clicks.skipWorkout++ },
                        onGenerateProgram = { clicks.generate++ },
                        onRetry = { clicks.retry++ },
                        onOpenRecipes = { clicks.recipes++ },
                        onOpenHealth = { clicks.health++ },
                        onOpenSettings = { clicks.settings++ },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `primo avvio senza anamnesi la CTA porta all'anamnesi e non alla generazione`() {
        val clicks = Clicks()
        render(
            DashboardUiState(isLoading = false, hasPlan = false, needsAnamnesis = true),
            clicks,
        )
        composeRule.onNodeWithText("Completa l'anamnesi").assertExists()
        composeRule.onNodeWithText("Inizia l'anamnesi").performClick()
        // La generazione senza profilo risponde "Completa prima il profilo":
        // il primo avvio non deve passare da li'.
        assertThat(clicks.anamnesis).isEqualTo(1)
        assertThat(clicks.generate).isEqualTo(0)
    }

    @Test
    fun `anamnesi confermata senza piano la CTA genera il programma`() {
        val clicks = Clicks()
        render(
            DashboardUiState(isLoading = false, hasPlan = false, needsAnamnesis = false),
            clicks,
        )
        composeRule.onNodeWithText("Genera il programma").performClick()
        assertThat(clicks.generate).isEqualTo(1)
        assertThat(clicks.anamnesis).isEqualTo(0)
    }

    @Test
    fun `la dashboard resta disegnata con una sessione in corso`() {
        val clicks = Clicks()
        render(
            baseState(
                workout = DashboardWorkout(
                    name = "Upper A", focus = "Petto · Spalle · Tricipiti",
                    weekIndex = 1, weekTotal = 5, exerciseCount = 6,
                    state = WorkoutState.IN_PROGRESS, canSkip = true,
                )
            ),
            clicks,
        )
        // Il contenuto della Dashboard deve essere presente (non una schermata vuota).
        composeRule.onNodeWithText("Allenamento di oggi").assertExists()
        composeRule.onNodeWithText("Upper A").assertExists()
        assertThat(clicks.workout).isEqualTo(0)
    }

    @Test
    fun `il tocco sul saluto non naviga da nessuna parte`() {
        val clicks = Clicks()
        render(baseState(), clicks)
        composeRule.onNodeWithText("Buonasera, Marjan").performClick()
        assertThat(clicks.workout).isEqualTo(0)
        assertThat(clicks.nutrition).isEqualTo(0)
        assertThat(clicks.coach).isEqualTo(0)
        assertThat(clicks.progress).isEqualTo(0)
    }

    @Test
    fun `solo le card previste reagiscono e mai verso il workout`() {
        val clicks = Clicks()
        render(baseState(), clicks)

        // Card "Progressi": porta a Progressi, non ad Allenamento.
        composeRule.onNodeWithText("Vedi tutti i progressi").performScrollTo().performClick()
        assertThat(clicks.progress).isEqualTo(1)
        assertThat(clicks.workout).isEqualTo(0)

        // Card Coach: porta al Coach, non ad Allenamento.
        composeRule.onNodeWithText("Chiedi al Coach").performScrollTo().performClick()
        assertThat(clicks.coach).isEqualTo(1)
        assertThat(clicks.workout).isEqualTo(0)
    }

    @Test
    fun `aprire allenamento richiede il tocco esplicito del pulsante`() {
        val clicks = Clicks()
        render(baseState(), clicks)
        composeRule.onNodeWithText("Apri allenamento").performClick()
        assertThat(clicks.workout).isEqualTo(1)
        assertThat(clicks.nutrition).isEqualTo(0)
    }

    /**
     * Riproduce il difetto "quando clicchi sulla schermata ti porta al workout":
     * tocca molti punti della Dashboard (sfondo, testi, aree neutre) e verifica
     * che NESSUN tocco generico porti ad Allenamento. Solo il pulsante esplicito
     * "Apri allenamento" deve farlo.
     */
    @Test
    fun `nessun tocco generico sulla dashboard porta al workout`() {
        val clicks = Clicks()
        render(baseState(), clicks)

        val root = composeRule.onRoot()
        val positions = listOf(
            10f to 10f, 180f to 300f, 180f to 500f, 180f to 700f,
            20f to 400f, 340f to 600f, 180f to 900f,
        )
        positions.forEach { (x, y) ->
            root.performTouchInput {
                click(androidx.compose.ui.geometry.Offset(x, y))
            }
            composeRule.waitForIdle()
        }

        assertThat(clicks.workout).isEqualTo(0)
    }

    /**
     * Difetto segnalato: "Ricette e Salute non si vedono installando l'app".
     * Le due sezioni esistevano ma erano raggiungibili solo da icone senza
     * etichetta sovrapposte al badge del giorno. Ora sono voci visibili.
     */
    @Test
    fun `ricette e salute sono voci visibili e cliccabili con un piano attivo`() {
        val clicks = Clicks()
        render(baseState(), clicks)

        composeRule.onNodeWithText("Ricette").assertExists()
        composeRule.onNodeWithText("Salute").assertExists()

        composeRule.onNodeWithText("Ricette").performClick()
        composeRule.onNodeWithText("Salute").performClick()

        assertThat(clicks.recipes).isEqualTo(1)
        assertThat(clicks.health).isEqualTo(1)
    }

    /**
     * L'ingresso alle Impostazioni era un'icona sovrapposta all'header, sopra il
     * badge del giorno. Ora e' in linea nell'header e deve restare raggiungibile.
     */
    @Test
    fun `le impostazioni restano raggiungibili dall'header`() {
        val clicks = Clicks()
        render(baseState(), clicks)

        composeRule.onNodeWithContentDescription("Impostazioni").performClick()
        assertThat(clicks.settings).isEqualTo(1)
    }

    /**
     * Le stesse due voci devono restare raggiungibili al primo avvio, quando
     * non c'e' ancora un piano: era il caso in cui l'utente non trovava nulla.
     */
    @Test
    fun `ricette e salute sono raggiungibili anche senza piano attivo`() {
        val clicks = Clicks()
        render(
            DashboardUiState(isLoading = false, hasPlan = false, needsAnamnesis = true),
            clicks,
        )

        composeRule.onNodeWithText("Ricette").assertExists()
        composeRule.onNodeWithText("Salute").assertExists()

        composeRule.onNodeWithText("Ricette").performClick()
        composeRule.onNodeWithText("Salute").performClick()

        assertThat(clicks.recipes).isEqualTo(1)
        assertThat(clicks.health).isEqualTo(1)
    }
}
