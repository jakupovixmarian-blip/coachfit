package com.fitcoach.app.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.fitcoach.app.ui.theme.AiAccent
import com.fitcoach.app.ui.theme.AiSoft
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitAnthracite
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextSecondary

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · FitCoachBottomBar.kt
 * -----------------------------------------------------------------------------
 * VINCOLI RISPETTATI:
 *  - La barra mostra 5 tab: Dashboard · Alimentazione · Allenamento ·
 *    Progressi · Coach AI (una mano sola, thumb zone).
 *  - L'enum TopLevelDestination NON viene ridefinito e NON viene svuotato:
 *    continua a contenere tutte e 7 le voci. Spesa e Diario restano voci
 *    dell'enum, restano raggiungibili come schermate di secondo livello e le
 *    loro route (Routes.SHOPPING, Routes.DIARY) restano intatte.
 *  - Nessuna stringa di route modificata: il filtro usa le stringhe esistenti.
 *  - Destinations.kt non va toccato da questo file.
 *
 * Se in Destinations.kt le route sono costanti di Routes (es. Routes.DASHBOARD),
 * puoi sostituire le private const qui sotto con quelle: sono gli stessi valori.
 * ========================================================================== */

private const val RouteDashboard = "dashboard"
private const val RouteNutrition = "nutrition"
private const val RouteWorkout = "workout"
private const val RouteProgress = "progress"
private const val RouteCoach = "coach"

/** Ordine dei 5 tab nella barra. */
private val PrimaryRouteOrder = listOf(
    RouteDashboard,
    RouteNutrition,
    RouteWorkout,
    RouteProgress,
    RouteCoach,
)

/**
 * Barra di navigazione principale.
 *
 * Il tab Coach AI usa l'accento dedicato (magenta) invece del brand Ion: è
 * l'unico punto della navigazione in cui l'AI si distingue dal resto dell'app.
 *
 * @param currentRoute rotta corrente; il tab corrispondente viene evidenziato.
 */
@Composable
fun FitCoachBottomBar(navController: NavHostController, currentRoute: String?) {
    val destinations = TopLevelDestination.values()
        .filter { it.route in PrimaryRouteOrder }
        .sortedBy { PrimaryRouteOrder.indexOf(it.route) }

    if (destinations.isEmpty()) return

    NavigationBar(
        containerColor = FitAnthracite,
        contentColor = TextSecondary,
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { destination ->
            val isAiTab = destination.route == RouteCoach
            val selected = currentRoute == destination.route
            val accent = if (isAiTab) AiAccent else Emerald

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = destination.shortLabel,
                        style = FitTextStyles.caption,
                        maxLines = 1,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = if (isAiTab) AiSoft else BrandSoft,
                    unselectedIconColor = TextLow,
                    unselectedTextColor = TextLow,
                ),
            )
        }
    }
}
