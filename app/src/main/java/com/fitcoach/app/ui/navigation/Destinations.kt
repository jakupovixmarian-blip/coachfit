package com.fitcoach.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Dashboard),
    NUTRITION("nutrition", "Alimentazione", Icons.Filled.Restaurant),
    WORKOUT("workout", "Allenamento", Icons.Filled.FitnessCenter),
    PROGRESS("progress", "Progressi", Icons.Filled.Insights),
    COACH("coach", "Coach AI", Icons.Filled.AutoAwesome)
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val NUTRITION = "nutrition"
    const val WORKOUT = "workout"
    const val PROGRESS = "progress"
    const val COACH = "coach"
    const val SETTINGS = "settings"
    const val AI_PROVIDERS = "ai_providers"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val FOOD_SUBSTITUTION = "food_substitution"
    const val EXERCISE_SUBSTITUTION = "exercise_substitution"
    const val SESSION_LOG = "session_log"
    const val CHECKIN = "checkin"
    const val PROFILE_EDIT = "profile_edit"
    const val CHANGE_HISTORY = "change_history"
    const val FOOD_SEARCH = "food_search"
    const val WORKOUT_DAY_DETAIL = "workout_day_detail"
}
