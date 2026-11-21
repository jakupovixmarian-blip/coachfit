package com.fitcoach.app.collaudo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.fitcoach.app.MainActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke test di avvio: l'app deve arrivare alla Dashboard senza crash.
 * Se l'avvio lancia un'eccezione, il test riporta lo stack trace reale.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w360dp-h800dp-xhdpi",
    application = SeededProfileApplication::class
)
class CrashSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app starts and dashboard renders`() {
        // La destinazione iniziale si risolve in un LaunchedEffect che legge il
        // profilo da Room su un altro dispatcher: fino ad allora lo Scaffold non
        // disegna nulla. `waitForIdle()` da solo non copre quella query, quindi
        // la Dashboard va attesa esplicitamente invece di leggerla una volta.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Impostazioni")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val nodes = composeRule.onAllNodesWithContentDescription("Impostazioni")
            .fetchSemanticsNodes()
        assertThat(nodes).isNotEmpty()
    }
}
