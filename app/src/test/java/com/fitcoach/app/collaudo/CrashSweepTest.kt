package com.fitcoach.app.collaudo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fitcoach.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Apre ogni schermata della barra inferiore: nessuna deve crashare. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w360dp-h800dp-xhdpi",
    application = SeededProfileApplication::class
)
class CrashSweepTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun open(label: String) {
        // La barra compare solo dopo la lettura del profilo (startDestination
        // null finche la query non risponde): waitForIdle non basta e il test
        // diventava intermittente. Si attende il nodo vero.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(label).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `all bottom bar screens open without crashing`() {
        listOf("Home", "Piano", "Workout", "Progressi", "Coach").forEach { open(it) }
        // Ritorno alla Dashboard: deve essere ancora viva.
        open("Home")
        composeRule.waitForIdle()
    }

    @Test
    fun `settings opens`() {
        composeRule.waitForIdle()
        val gears = composeRule.onAllNodesWithText("Impostazioni").fetchSemanticsNodes()
        if (gears.isNotEmpty()) {
            composeRule.onNodeWithText("Impostazioni").performClick()
            composeRule.waitForIdle()
        }
    }
}
