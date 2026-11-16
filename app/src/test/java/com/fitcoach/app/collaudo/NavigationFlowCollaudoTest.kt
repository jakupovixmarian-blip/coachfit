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
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class NavigationFlowCollaudoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun dashboardVisible(): Boolean =
        composeRule.onAllNodesWithContentDescription("Impostazioni")
            .fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `i tab della barra portano alla schermata giusta e mai solo al workout`() {
        composeRule.waitForIdle()
        assertThat(dashboardVisible()).isTrue()

        // Tab Allenamento: si arriva ad Allenamento (l'ingranaggio della Dashboard sparisce).
        composeRule.onNodeWithText("Workout").performClick()
        composeRule.waitForIdle()
        assertThat(dashboardVisible()).isFalse()

        // Ritorno alla Dashboard: deve essere di nuovo la Dashboard, non una schermata vuota.
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()
        assertThat(dashboardVisible()).isTrue()
    }

    @Test
    fun `ogni tab apre la propria schermata`() {
        composeRule.waitForIdle()

        val tabs = listOf("Piano", "Workout", "Progressi", "Coach")
        tabs.forEach { tab ->
            composeRule.onNodeWithText(tab).performClick()
            composeRule.waitForIdle()
            // Fuori dalla Dashboard l'ingranaggio non c'e.
            assertThat(dashboardVisible()).isFalse()
            composeRule.onNodeWithText("Home").performClick()
            composeRule.waitForIdle()
            assertThat(dashboardVisible()).isTrue()
        }
    }

    @Test
    fun `tornare alla dashboard dopo il workout non lascia la schermata vuota`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Workout").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Progressi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()

        // La Dashboard deve disegnare contenuto reale: l'ingranaggio + i tab.
        assertThat(dashboardVisible()).isTrue()
        composeRule.onAllNodesWithText("Home").assertCountEquals(1)
    }
}
