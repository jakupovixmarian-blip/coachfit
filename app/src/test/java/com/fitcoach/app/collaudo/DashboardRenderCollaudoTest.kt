package com.fitcoach.app.collaudo

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.fitcoach.app.MainActivity
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
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class DashboardRenderCollaudoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
        composeRule.activity.setContent {
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
                    )
                }
            }
        }
        composeRule.waitForIdle()
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
}
