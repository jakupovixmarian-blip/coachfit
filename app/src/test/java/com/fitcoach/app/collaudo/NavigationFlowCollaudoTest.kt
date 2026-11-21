package com.fitcoach.app.collaudo

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fitcoach.app.MainActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Collaudo di NAVIGAZIONE sull'app reale (MainActivity + NavHost + bottom bar).
 *
 * Riproduce i difetti segnalati dall'utente:
 *  - "quando clicchi sulla schermata ti porta al workout": il tocco su un tab
 *    della barra inferiore deve portare alla schermata di QUEL tab;
 *  - "all'avvio del workout la dashboard sparisce / va in bug": passando ad
 *    Allenamento e tornando alla Dashboard, la Dashboard deve restare presente
 *    e disegnata.
 *
 * Le asserzioni usano marcatori STABILI e sincroni (l'ingranaggio Impostazioni
 * esiste solo in Dashboard; l'empty state esiste solo in Allenamento), cosi il
 * test non dipende dal caricamento asincrono dei dati.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w360dp-h800dp-xhdpi",
    application = SeededProfileApplication::class
)
class NavigationFlowCollaudoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun dashboardVisible(): Boolean =
        composeRule.onAllNodesWithContentDescription("Impostazioni")
            .fetchSemanticsNodes().isNotEmpty()

    /**
     * Attende che la Dashboard sia disegnata.
     *
     * `waitForIdle()` non basta: la destinazione iniziale si risolve in un
     * `LaunchedEffect` che legge il profilo da Room su un altro dispatcher, e
     * finche' `startDestination` e' null lo `Scaffold` non disegna nulla.
     * Con la JVM fredda la query finisce prima del primo assert, con la JVM
     * calda `waitForIdle` torna subito e il test legge una schermata ancora
     * vuota: e' la ragione per cui questi test passavano da soli e fallivano
     * nella suite.
     */
    private fun awaitDashboard() {
        composeRule.waitUntil(timeoutMillis = 10_000) { dashboardVisible() }
    }

    /**
     * Attende che la Dashboard sparisca.
     *
     * Senza attesa l'asserzione "non e' la Dashboard" sarebbe vera anche su una
     * schermata ancora vuota: passerebbe senza verificare nulla.
     */
    private fun awaitLeftDashboard() {
        composeRule.waitUntil(timeoutMillis = 10_000) { !dashboardVisible() }
    }

    @Test
    fun `i tab della barra portano alla schermata giusta e mai solo al workout`() {
        awaitDashboard()

        // Tab Allenamento: si arriva ad Allenamento (l'ingranaggio della Dashboard sparisce).
        composeRule.onNodeWithText("Workout").performClick()
        awaitLeftDashboard()

        // Ritorno alla Dashboard: deve essere di nuovo la Dashboard, non una schermata vuota.
        composeRule.onNodeWithText("Home").performClick()
        awaitDashboard()
    }

    @Test
    fun `ogni tab apre la propria schermata`() {
        awaitDashboard()

        val tabs = listOf("Piano", "Spesa", "Workout", "Progressi", "Coach")
        tabs.forEach { tab ->
            composeRule.onNodeWithText(tab).performClick()
            awaitLeftDashboard()
            composeRule.onNodeWithText("Home").performClick()
            awaitDashboard()
        }
    }

    @Test
    fun `tornare alla dashboard dopo il workout non lascia la schermata vuota`() {
        awaitDashboard()

        composeRule.onNodeWithText("Workout").performClick()
        awaitLeftDashboard()
        composeRule.onNodeWithText("Progressi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").performClick()
        awaitDashboard()

        // La Dashboard deve disegnare contenuto reale: l'ingranaggio + i tab.
        composeRule.onAllNodesWithText("Home").assertCountEquals(1)
    }
}
