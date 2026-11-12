package com.fitcoach.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitcoach.app.ui.home.HomeScreen
import com.fitcoach.app.ui.home.HomeViewModel
import com.fitcoach.app.ui.onboarding.OnboardingScreen
import com.fitcoach.app.ui.onboarding.OnboardingViewModel
import com.fitcoach.app.ui.nutrition.NutritionScreen
import com.fitcoach.app.ui.theme.FitCoachTheme

@Composable
fun FitCoachApp() {
    FitCoachTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showNavigation = currentRoute != Destination.Onboarding.route

        Scaffold(
            bottomBar = {
                if (showNavigation) {
                    NavigationBar {
                        Destination.primary.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { androidx.compose.material3.Icon(destination.icon, destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Home.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Destination.Home.route) {
                    HomeScreen(
                        viewModel = hiltViewModel<HomeViewModel>(),
                        onOpenOnboarding = { navController.navigate(Destination.Onboarding.route) }
                    )
                }
                composable(Destination.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = hiltViewModel<OnboardingViewModel>(),
                        onCompleted = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(Destination.Nutrition.route) { NutritionScreen() }
                composable(Destination.Shopping.route) { SectionScreen(Destination.Shopping) }
                composable(Destination.Diary.route) { SectionScreen(Destination.Diary) }
                composable(Destination.Workout.route) { SectionScreen(Destination.Workout) }
                composable(Destination.Progress.route) { SectionScreen(Destination.Progress) }
                composable(Destination.Coach.route) { SectionScreen(Destination.Coach) }
                composable(Destination.Settings.route) { SectionScreen(Destination.Settings) }
            }
        }
    }
}
