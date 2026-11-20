package com.fitcoach.app.collaudo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import com.fitcoach.app.MainActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Destinazione iniziale: senza profilo l'app deve aprire l'onboarding, con un
 * profilo esistente deve aprire la Dashboard.
 *
 * Senza questa distinzione il primo avvio mostrava la Dashboard vuota e l'unica
 * via per configurare l'app restava il pulsante dell'anamnesi, mentre
 * l'onboarding era registrato nel NavHost e irraggiungibile.
 *
 * Qui si usa l'Application reale (nessun seme): il database e' vuoto, come su
 * un telefono appena installato.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class FirstRunRoutingCollaudoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `al primo avvio senza profilo si apre l'onboarding`() {
        composeRule.waitForIdle()
        val onboarding = composeRule.onAllNodesWithText("Configura il tuo profilo")
            .fetchSemanticsNodes()
        assertThat(onboarding).isNotEmpty()

        // La Dashboard non deve essere quella iniziale.
        val dashboard = composeRule.onAllNodesWithContentDescription("Impostazioni")
            .fetchSemanticsNodes()
        assertThat(dashboard).isEmpty()
    }

    @Test
    fun `durante l'onboarding la barra di navigazione inferiore non e mostrata`() {
        composeRule.waitForIdle()
        // Le tab portano a schermate che presuppongono un profilo: mostrarle qui
        // permetterebbe di aggirare la configurazione iniziale.
        val homeTab = composeRule.onAllNodesWithText("Home").fetchSemanticsNodes()
        assertThat(homeTab).isEmpty()
    }
}
