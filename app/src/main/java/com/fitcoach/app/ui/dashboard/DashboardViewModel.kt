package com.fitcoach.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.model.CarbDayType as DomainCarbDayType
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NotificationRecord
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WeightTrend
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.fitcoach.app.ui.screens.dashboard.CarbDayType
import com.fitcoach.app.ui.screens.dashboard.DashboardMeals
import com.fitcoach.app.ui.screens.dashboard.DashboardNotification
import com.fitcoach.app.ui.screens.dashboard.DashboardUiState
import com.fitcoach.app.ui.screens.dashboard.DashboardWeight
import com.fitcoach.app.ui.screens.dashboard.DashboardWorkout
import com.fitcoach.app.ui.screens.dashboard.WorkoutState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fitcoach.app.diagnostics.CrashLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository,
    private val nutritionDiaryRepository: NutritionDiaryRepository,
    private val workoutRepository: WorkoutRepository,
    private val progressRepository: ProgressRepository,
    private val notificationRepository: NotificationRepository,
    private val chatRepository: ChatRepository,
    private val generationCoordinator: ProgramGenerationCoordinator,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState(isLoading = true))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    /** Messaggi una-tantum per le azioni (generazione, spostamento seduta). */
    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    init {
        refresh()
    }

    fun consumeEvent() { _events.value = null }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { buildState() }
                .onSuccess { _state.value = it }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = it.message)
                }
        }
    }

    private suspend fun buildState(): DashboardUiState {
        val profile = userRepository.get()
        val today = LocalDate.now()
        val weekdayIndex = today.dayOfWeek.value - 1

        val plan = nutritionRepository.getActivePlan()
        val todayDay: NutritionDay? = plan?.days?.firstOrNull { it.dayIndex == weekdayIndex }
        val balance = nutritionDiaryRepository.balanceForDay(today.toEpochDay())
        val entries = nutritionDiaryRepository.entriesForDay(today.toEpochDay())

        val workoutPlan = workoutRepository.getActivePlan()
        val mesocycle = workoutPlan?.mesocycles?.firstOrNull()
        val todayWorkout: WorkoutDay? = workoutPlan?.days?.firstOrNull {
            it.dayIndex == weekdayIndex && !it.isRestDay
        }
        val session = workoutRepository.getSessionForDay(today.toEpochDay())

        val weights = progressRepository.getWeights()
        val trend: WeightTrend? = if (weights.size >= 2) WeightAnalyzer.trend(weights) else null
        val notifications = notificationRepository.getEnabled()
        val lastCoach = chatRepository.getRecent(20).lastOrNull { it.role == ChatRole.ASSISTANT }
        val weekSessions = workoutRepository.getSessionsBetween(today.toEpochDay() - 6, today.toEpochDay())

        val loggedMeals = todayDay?.meals?.count { meal ->
            entries.any { it.active && it.mealSlot == meal.slot }
        } ?: 0

        return DashboardUiState(
            isLoading = false,
            errorMessage = null,
            hasPlan = plan != null || workoutPlan != null,
            goalLabel = profile?.goal?.label,
            needsAnamnesis = profile?.isComplete == true && !profile.isAnamnesisReady,
            anamnesisSummary = profile?.anamnesisSummary?.takeIf { it.isNotBlank() },
            warnings = buildWarnings(profile, plan?.days),
            greeting = greeting(profile?.name),
            weekdayLabel = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN))
                .replaceFirstChar { it.uppercase() },
            dayType = todayDay?.dayType.toPresentation(),
            kcalTarget = (todayDay?.targetKcal ?: 0.0).toInt(),
            kcalConsumed = balance.consumedKcal.toInt(),
            proteinTargetGrams = todayDay?.targetProtein ?: 0.0,
            proteinConsumedGrams = balance.consumedProtein,
            carbsTargetGrams = todayDay?.targetCarbs ?: 0.0,
            carbsConsumedGrams = balance.consumedCarbs,
            fatTargetGrams = todayDay?.targetFat ?: 0.0,
            fatConsumedGrams = balance.consumedFat,
            meals = DashboardMeals(
                logged = loggedMeals,
                planned = todayDay?.meals?.size ?: 0
            ),
            hydrationLiters = null,
            workout = todayWorkout?.let { day ->
                DashboardWorkout(
                    name = day.title,
                    focus = day.muscleGroups.joinToString(", ") { it.label },
                    weekIndex = currentWeekIndex(mesocycle?.startWeekIndex, workoutPlan?.createdAt, today),
                    weekTotal = mesocycle?.weekCount ?: 0,
                    exerciseCount = day.exercises.size,
                    state = session.toWorkoutState(),
                    canSkip = session?.status != SessionStatus.COMPLETED
                )
            },
            weight = weights.firstOrNull()?.let { latest ->
                DashboardWeight(
                    currentKg = latest.weightKg,
                    deltaKg = weightDelta(weights),
                    recentKg = weights.take(8).map { it.weightKg }.reversed()
                )
            },
            sleepHours = profile?.sleepHours,
            weeklyVolumeKg = weeklyVolume(weekSessions),
            lastCoachMessage = lastCoach?.content?.takeIf { it.isNotBlank() },
            nextNotification = nextNotification(notifications)
        )
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            progressRepository.addWeight(
                WeightRecord(dateEpochDay = LocalDate.now().toEpochDay(), weightKg = weightKg)
            )
            refresh()
        }
    }

    fun skipTodayWorkout() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val weekdayIndex = today.dayOfWeek.value - 1
            val plan = workoutRepository.getActivePlan()
            val day = plan?.days?.firstOrNull { it.dayIndex == weekdayIndex && !it.isRestDay }
            if (day == null) {
                _events.value = "Nessuna seduta da spostare oggi."
                return@launch
            }
            val weekStart = today.toEpochDay() - (today.dayOfWeek.value - 1).toLong()
            val moved = workoutRepository.saveScheduleOverride(
                dayId = day.id,
                targetDayIndex = (weekdayIndex + 1) % 7,
                weekStartEpochDay = weekStart,
                permanent = false,
                note = "Spostata dalla Dashboard"
            )
            _events.value = if (moved) "Seduta spostata a domani." else "Impossibile spostare la seduta."
            refresh()
        }
    }

    fun generateProgram() {
        viewModelScope.launch {
            val profile = userRepository.get()
            if (profile == null) {
                _events.value = "Completa prima il profilo."
                return@launch
            }
            // Un errore nella generazione non deve chiudere l'app: viene
            // registrato e mostrato come messaggio.
            _events.value = try {
                when (val result = generationCoordinator.generate(
                    profile = profile,
                    scope = ProgramGenerationCoordinator.Scope.BOTH
                )) {
                    is ProgramGenerationResult.Success -> "Programma generato."
                    is ProgramGenerationResult.MissingData ->
                        "Dati mancanti: ${result.missing.joinToString()}."
                    is ProgramGenerationResult.AnamnesisNotConfirmed ->
                        "Completa l'anamnesi per generare il programma."
                    is ProgramGenerationResult.Error -> "Errore: ${result.message}"
                }
            } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
                CrashLog.record(context, t, "Generazione programma completo")
                "Generazione non riuscita: ${t.message ?: t::class.simpleName}. " +
                    "Dettagli in Impostazioni > Diagnostica."
            }
            runCatching { refresh() }
        }
    }

    private fun buildWarnings(profile: UserProfile?, days: List<NutritionDay>?): List<String> = buildList {
        profile?.allergies?.takeIf { it.isNotEmpty() }
            ?.let { add("Allergie registrate: ${it.joinToString()}.") }
        profile?.physicalLimitations?.takeIf { it.isNotEmpty() }
            ?.let { add("Limitazioni: ${it.joinToString()}.") }
        if (days != null && days.any { it.actualKcal == 0.0 }) add("Piano nutrizionale incompleto.")
    }

    private fun greeting(name: String?): String {
        val part = when (LocalTime.now().hour) {
            in 5..12 -> "Buongiorno"
            in 13..17 -> "Buon pomeriggio"
            else -> "Buonasera"
        }
        return if (name.isNullOrBlank()) part else "$part, $name"
    }

    private fun weightDelta(weights: List<WeightRecord>): Double =
        if (weights.size >= 2) weights[0].weightKg - weights[1].weightKg else 0.0

    private fun weeklyVolume(sessions: List<WorkoutSession>): Double? {
        val completed = sessions.filter { it.status == SessionStatus.COMPLETED }
        if (completed.isEmpty()) return null
        return completed.sumOf { session ->
            session.sets.filter { it.completed && !it.isWarmup }
                .sumOf { it.weightKg * it.actualReps }
        }
    }

    private fun currentWeekIndex(startWeekIndex: Int?, planCreatedAt: Long?, today: LocalDate): Int {
        val start = planCreatedAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: return (startWeekIndex ?: 0) + 1
        val weeks = ChronoUnit.WEEKS.between(start, today).toInt()
        return ((startWeekIndex ?: 0) + weeks).coerceAtLeast(0) + 1
    }

    private fun nextNotification(notifications: List<NotificationRecord>): DashboardNotification? {
        if (notifications.isEmpty()) return null
        val now = LocalTime.now()
        val upcoming = notifications
            .mapNotNull { n -> runCatching { LocalTime.parse(n.triggerTime) to n }.getOrNull() }
            .sortedBy { it.first }
        val next = upcoming.firstOrNull { it.first >= now } ?: upcoming.firstOrNull()
        return next?.let { DashboardNotification(timeLabel = it.second.triggerTime, title = it.second.title) }
    }
}

private fun WorkoutSession?.toWorkoutState(): WorkoutState = when (this?.status) {
    SessionStatus.COMPLETED -> WorkoutState.COMPLETED
    SessionStatus.IN_PROGRESS -> WorkoutState.IN_PROGRESS
    else -> WorkoutState.PLANNED
}

private fun DomainCarbDayType?.toPresentation(): CarbDayType = when (this) {
    DomainCarbDayType.HIGH -> CarbDayType.HIGH
    DomainCarbDayType.MEDIUM -> CarbDayType.MEDIUM
    DomainCarbDayType.LOW -> CarbDayType.LOW
    null -> CarbDayType.MEDIUM
}
