package dev.fitcoach.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.ui.navigation.FitCoachNavHost
import dev.fitcoach.app.ui.navigation.Routes
import dev.fitcoach.app.ui.theme.FitCoachTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    profileRepository: ProfileRepository
) : ViewModel() {
    val startRoute: StateFlow<String?> = profileRepository.observeProfile()
        .map { profile -> if (profile?.onboardingCompleted == true) Routes.HOME else Routes.ANAMNESI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            FitCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vm: AppStartViewModel = hiltViewModel()
                    val start by vm.startRoute.collectAsStateWithLifecycle()
                    start?.let { route ->
                        FitCoachNavHost(startRoute = route)
                    }
                }
            }
        }
    }

    /** The rest timer runs in a foreground service, which needs this on Android 13+. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    .launch(permission)
            }
        }
    }
}
