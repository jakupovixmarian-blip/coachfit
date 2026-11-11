package com.fitcoach.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NotificationRecord
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightTrend
import com.fitcoach.app.domain.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val hasProfile: Boolean = false,
    val profileName: String = "",
    val goalLabel: String = "",
    val dayName: String = "",
    val dayType: CarbDayType? = null,
    val targetKcal: Double = 0.0,
    val targetProtein: Double = 0.0,
    val targetCarbs: Double = 0.0,
    val targetFat: Double = 0.0,
    val meals: List<Meal> = emptyList(),
    val workoutTitle: String? = null,
    val workoutExercises: Int = 0,
    val workoutDone: Boolean = false,
    val nextNotification: String? = null,
    val weightTrend: WeightTrend? = null,
    val latestWeight: Double? = null,
    val warnings: List<String> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val progressRepository: ProgressRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val profile = userRepository.get()
            val today = LocalDate.now()
            val weekdayIndex = today.dayOfWeek.value - 1 // 0=Lunedi

            val plan = nutritionRepository.getActivePlan()
            val todayDay: NutritionDay? = plan?.days?.firstOrNull { it.dayIndex == weekdayIndex }
            val workoutPlan = workoutRepository.getActivePlan()
            val todayWorkout: WorkoutDay? = workoutPlan?.days?.firstOrNull {
                it.dayIndex == weekdayIndex && !it.isRestDay
            }
            val session = workoutRepository.getSessionForDay(today.toEpochDay())
            val weights = progressRepository.getWeights()
            val trend = if (weights.size >= 2) WeightAnalyzer.trend(weights) else null
            val notifications = notificationRepository.getEnabled()

            _state.value = DashboardUiState(
                loading = false,
                hasProfile = profile?.isComplete == true,
                profileName = profile?.name.orEmpty(),
                goalLabel = profile?.goal?.label.orEmpty(),
                dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
                    .replaceFirstChar { it.uppercase() },
                dayType = todayDay?.dayType,
                targetKcal = todayDay?.targetKcal ?: 0.0,
                targetProtein = todayDay?.targetProtein ?: 0.0,
                targetCarbs = todayDay?.targetCarbs ?: 0.0,
                targetFat = todayDay?.targetFat ?: 0.0,
                meals = todayDay?.meals ?: emptyList(),
                workoutTitle = todayWorkout?.title,
                workoutExercises = todayWorkout?.exercises?.size ?: 0,
                workoutDone = session?.status == SessionStatus.COMPLETED,
                nextNotification = nextNotification(notifications),
                weightTrend = trend,
                latestWeight = weights.firstOrNull()?.weightKg,
                warnings = plan?.let { p ->
                    if (p.days.any { it.actualKcal == 0.0 }) listOf("Piano nutrizionale incompleto.") else emptyList()
                } ?: emptyList()
            )
        }
    }

    private fun nextNotification(notifications: List<NotificationRecord>): String? {
        if (notifications.isEmpty()) return null
        val now = java.time.LocalTime.now()
        val upcoming = notifications
            .mapNotNull { n -> runCatching { java.time.LocalTime.parse(n.triggerTime) to n }.getOrNull() }
            .sortedBy { it.first }
        val next = upcoming.firstOrNull { it.first >= now } ?: upcoming.firstOrNull()
        return next?.let { "${it.second.title} alle ${it.second.triggerTime}" }
    }
}
