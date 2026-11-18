package com.fitcoach.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", "Home", Icons.Filled.Dashboard),
    NUTRITION("nutrition", "Alimentazione", "Piano", Icons.Filled.Restaurant),
    SHOPPING("shopping", "Spesa", "Spesa", Icons.Filled.ShoppingCart),
    DIARY("diary", "Diario", "Diario", Icons.Filled.EventNote),
    WORKOUT("workout", "Allenamento", "Workout", Icons.Filled.FitnessCenter),
    PROGRESS("progress", "Progressi", "Progressi", Icons.Filled.Insights),
    COACH("coach", "Coach AI", "Coach", Icons.Filled.AutoAwesome)
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val ANAMNESIS = "anamnesis"
    const val ANAMNESIS_EDIT = "anamnesis_edit"
    const val DASHBOARD = "dashboard"
    const val NUTRITION = "nutrition"
    const val SHOPPING = "shopping"
    const val DIARY = "diary"
    const val WORKOUT = "workout"
    const val CUSTOM_WORKOUT = "custom_workout"
    const val CUSTOM_WORKOUT_EDIT = "custom_workout/{planId}"
    const val PROGRESS = "progress"
    const val COACH = "coach"
    const val SETTINGS = "settings"
    const val AI_PROVIDERS = "ai_providers"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val PROFILE_EDIT = "profile_edit"
    const val KNOWLEDGE = "knowledge"
    const val DEVICES = "devices"
}
