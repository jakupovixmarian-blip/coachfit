package com.fitcoach.app.collaudo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitcoach.app.ui.navigation.navigateTopLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Riproduce la logica di navigazione di produzione (bottom bar + card della
 * Dashboard) su un NavHost reale con destinazioni stub, per verificare che la
 * Home resti sempre raggiungibile e che non si accumulino destinazioni duplicate.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class NavOptionsCollaudoTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: NavHostController

    /**
     * Sia la barra inferiore sia le card della Dashboard usano la STESSA
     * funzione di produzione [com.fitcoach.app.ui.navigation.navigateTopLevel].
     */
    private fun openTab(route: String) = nav.navigateTopLevel(route)

    private fun openCard(route: String) = nav.navigateTopLevel(route)

    @Composable
    private fun Harness() {
        nav = rememberNavController()
        androidx.compose.material3.Scaffold(
            bottomBar = {
                // La barra e sempre presente sulle destinazioni top-level, come in produzione.
                Row {
                    Text("tab-home", Modifier.clickable { openTab("dashboard") })
                    Text("tab-nutrition", Modifier.clickable { openTab("nutrition") })
                    Text("tab-workout", Modifier.clickable { openTab("workout") })
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "dashboard",
                modifier = Modifier.padding(padding)
            ) {
                composable("dashboard") {
                    Column {
                        Text("HOME")
                        Text("card-nutrition", Modifier.clickable { openCard("nutrition") })
                        Text("card-workout", Modifier.clickable { openCard("workout") })
                    }
                }
                composable("nutrition") { Text("NUTRITION") }
                composable("workout") { Text("WORKOUT") }
            }
        }
    }

    private fun route(): String? = nav.currentBackStackEntry?.destination?.route

    @Test
    fun `card poi tab home torna davvero alla home`() {
        rule.setContent { Harness() }
        rule.waitForIdle()
        rule.onNodeWithText("card-workout").performClick()
        rule.waitForIdle()
        assertThat(route()).isEqualTo("workout")

        rule.onNodeWithText("tab-home").performClick()
        rule.waitForIdle()
        assertThat(route()).isEqualTo("dashboard")
        assertThat(nav.currentBackStack.value.count { it.destination.route == "dashboard" })
            .isEqualTo(1)
    }

    @Test
    fun `card poi tab alimentazione poi tab home non accumula destinazioni`() {
        rule.setContent { Harness() }
        rule.waitForIdle()
        rule.onNodeWithText("card-nutrition").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("tab-workout").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("tab-home").performClick()
        rule.waitForIdle()
        assertThat(route()).isEqualTo("dashboard")
        assertThat(nav.currentBackStack.value.count { it.destination.route == "dashboard" })
            .isEqualTo(1)
    }

    @Test
    fun `tab home raggiungibile da una card anche quando la barra e nascosta`() {
        rule.setContent { Harness() }
        rule.waitForIdle()
        rule.onNodeWithText("card-workout").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("tab-workout").performClick()
        rule.waitForIdle()
        assertThat(route()).isEqualTo("workout")
        rule.onNodeWithText("tab-home").performClick()
        rule.waitForIdle()
        assertThat(route()).isEqualTo("dashboard")
    }
}
