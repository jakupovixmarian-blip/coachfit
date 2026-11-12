package dev.fitcoach.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ANAMNESI = "anamnesi"
    const val HOME = "home"
    const val PLAN = "plan"
    const val SHOPPING = "shopping"
    const val DIARY = "diary"
    const val WORKOUT = "workout"
    const val CALENDAR = "calendar"
    const val PROGRESS = "progress"
    const val COACH = "coach"
    const val SETTINGS = "settings"

    // Detail routes
    const val WORKOUT_SESSION = "workout/session/{sessionId}"
    const val ROUTINE_EDITOR = "workout/routine/{routineId}"
    const val EXERCISE_DETAIL = "workout/exercise/{exerciseId}"
    const val MEASUREMENT_DETAIL = "progress/measurement/{type}"
    const val AI_SETTINGS = "settings/ai"
    const val KNOWLEDGE = "settings/knowledge"
    const val MANUAL_PROGRAM = "workout/manual-program"

    fun workoutSession(sessionId: Long) = "workout/session/$sessionId"
    fun routineEditor(routineId: Long) = "workout/routine/$routineId"
    fun exerciseDetail(exerciseId: Long) = "workout/exercise/$exerciseId"
    fun measurementDetail(type: String) = "progress/measurement/$type"
}

enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home),
    PLAN(Routes.PLAN, "Piano", Icons.Filled.Restaurant),
    SHOPPING(Routes.SHOPPING, "Spesa", Icons.Filled.ShoppingCart),
    WORKOUT(Routes.WORKOUT, "Workout", Icons.Filled.FitnessCenter),
    CALENDAR(Routes.CALENDAR, "Calendario", Icons.Filled.CalendarMonth),
    DIARY(Routes.DIARY, "Diario", Icons.Filled.RestaurantMenu),
    PROGRESS(Routes.PROGRESS, "Progressi", Icons.Filled.ShowChart),
    COACH(Routes.COACH, "Coach", Icons.Filled.SmartToy),
    SETTINGS(Routes.SETTINGS, "Impostazioni", Icons.Filled.Settings)
}
