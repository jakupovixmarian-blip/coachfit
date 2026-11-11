package com.fitcoach.app.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun ProgressScreen(navController: NavHostController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("ProgressScreen", color = MaterialTheme.colorScheme.onBackground)
    }
}
