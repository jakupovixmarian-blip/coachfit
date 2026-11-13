package dev.fitcoach.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import dev.fitcoach.app.ui.components.NeonBottomBar
import dev.fitcoach.app.ui.components.NeonNavItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.fitcoach.app.ui.screens.calendar.CalendarScreen
import dev.fitcoach.app.ui.screens.coach.CoachScreen
import dev.fitcoach.app.ui.screens.home.HomeScreen
import dev.fitcoach.app.ui.screens.nutrition.DiaryScreen
import dev.fitcoach.app.ui.screens.nutrition.PlanScreen
import dev.fitcoach.app.ui.screens.nutrition.ShoppingScreen
import dev.fitcoach.app.ui.screens.anamnesi.AnamnesiScreen
import dev.fitcoach.app.ui.screens.knowledge.KnowledgeScreen
import dev.fitcoach.app.ui.screens.workout.ManualProgramScreen
import dev.fitcoach.app.ui.screens.progress.MeasurementDetailScreen
import dev.fitcoach.app.ui.screens.progress.ProgressScreen
import dev.fitcoach.app.ui.screens.settings.AiSettingsScreen
import dev.fitcoach.app.ui.screens.settings.SettingsScreen
import dev.fitcoach.app.ui.screens.workout.ExerciseDetailScreen
import dev.fitcoach.app.ui.screens.workout.RoutineEditorScreen
import dev.fitcoach.app.ui.screens.workout.WorkoutScreen
import dev.fitcoach.app.ui.screens.workout.WorkoutSessionScreen

@Composable
fun FitCoachNavHost(
    startRoute: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopDestination.entries.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NeonBottomBar(
                    items = TopDestination.entries.map {
                        NeonNavItem(route = it.route, label = it.label, icon = it.icon)
                    },
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ANAMNESI) {
                AnamnesiScreen(
                    onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
                    onFinished = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ANAMNESI) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenWorkout = { navController.navigate(Routes.WORKOUT) },
                    onOpenDiary = { navController.navigate(Routes.DIARY) },
                    onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                    onOpenCoach = { navController.navigate(Routes.COACH) },
                    onOpenPlan = { navController.navigate(Routes.PLAN) },
                    onOpenShopping = { navController.navigate(Routes.SHOPPING) },
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onStartOnboarding = { navController.navigate(Routes.ANAMNESI) }
                )
            }
            composable(Routes.PLAN) { PlanScreen(onOpenDiary = { navController.navigate(Routes.DIARY) }) }
            composable(Routes.SHOPPING) { ShoppingScreen() }
            composable(Routes.DIARY) { DiaryScreen() }
            composable(Routes.WORKOUT) {
                WorkoutScreen(
                    onOpenSession = { navController.navigate(Routes.workoutSession(it)) },
                    onOpenRoutine = { navController.navigate(Routes.routineEditor(it)) },
                    onOpenExercise = { navController.navigate(Routes.exerciseDetail(it)) },
                    onOpenManualProgram = { navController.navigate(Routes.MANUAL_PROGRAM) }
                )
            }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.PROGRESS) {
                ProgressScreen(onOpenMeasurement = { navController.navigate(Routes.measurementDetail(it)) })
            }
            composable(Routes.COACH) {
                CoachScreen(onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenAi = { navController.navigate(Routes.AI_SETTINGS) },
                    onOpenKnowledge = { navController.navigate(Routes.KNOWLEDGE) }
                )
            }
            composable(Routes.AI_SETTINGS) { AiSettingsScreen() }
            composable(Routes.KNOWLEDGE) { KnowledgeScreen() }
            composable(Routes.MANUAL_PROGRAM) {
                ManualProgramScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoutine = { navController.navigate(Routes.routineEditor(it)) }
                )
            }

            composable(
                route = Routes.WORKOUT_SESSION,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
                WorkoutSessionScreen(sessionId = sessionId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ROUTINE_EDITOR,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { entry ->
                val routineId = entry.arguments?.getLong("routineId") ?: 0L
                RoutineEditorScreen(
                    routineId = routineId,
                    onBack = { navController.popBackStack() },
                    onOpenExercise = { navController.navigate(Routes.exerciseDetail(it)) }
                )
            }
            composable(
                route = Routes.EXERCISE_DETAIL,
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { entry ->
                val exerciseId = entry.arguments?.getLong("exerciseId") ?: 0L
                ExerciseDetailScreen(exerciseId = exerciseId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.MEASUREMENT_DETAIL,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "WEIGHT"
                MeasurementDetailScreen(type = type, onBack = { navController.popBackStack() })
            }
        }
    }
}
