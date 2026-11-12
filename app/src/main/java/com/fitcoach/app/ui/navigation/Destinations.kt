package com.fitcoach.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Destination("home", "Home", Icons.Default.Dashboard)
    data object Nutrition : Destination("nutrition", "Piano", Icons.Default.Restaurant)
    data object Shopping : Destination("shopping", "Spesa", Icons.Default.LocalGroceryStore)
    data object Diary : Destination("diary", "Diario", Icons.Default.CalendarMonth)
    data object Workout : Destination("workout", "Workout", Icons.Default.FitnessCenter)
    data object Progress : Destination("progress", "Progressi", Icons.Default.ShowChart)
    data object Coach : Destination("coach", "Coach", Icons.Default.Groups)
    data object Settings : Destination("settings", "Impostazioni", Icons.Default.Settings)
    data object Onboarding : Destination("onboarding", "Profilo", Icons.Default.Groups)

    companion object {
        val primary = listOf(Home, Nutrition, Shopping, Diary, Workout, Progress, Coach, Settings)
    }
}
