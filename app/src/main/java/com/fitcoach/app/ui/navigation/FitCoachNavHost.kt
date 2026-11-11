package com.fitcoach.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.fitcoach.app.ui.coach.CoachScreen
import com.fitcoach.app.ui.dashboard.DashboardScreen
import com.fitcoach.app.ui.nutrition.NutritionScreen
import com.fitcoach.app.ui.onboarding.OnboardingScreen
import com.fitcoach.app.ui.progress.ProgressScreen
import com.fitcoach.app.ui.workout.WorkoutScreen

@Composable
fun FitCoachNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FitCoachBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(180)) }
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(navController) }
            composable(Routes.NUTRITION) { NutritionScreen(navController) }
            composable(Routes.WORKOUT) { WorkoutScreen(navController) }
            composable(Routes.PROGRESS) { ProgressScreen(navController) }
            composable(Routes.COACH) { CoachScreen(navController) }
            composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
        }
    }
}
