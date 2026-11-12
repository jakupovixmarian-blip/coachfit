package dev.fitcoach.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.DiaryRepository
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.MealPlanRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MacroTotals(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double)

data class HomeState(
    val profile: UserProfile? = null,
    val plan: MealPlan? = null,
    val routine: Routine? = null,
    val today: MacroTotals = MacroTotals(0.0, 0.0, 0.0, 0.0),
    val lastSession: WorkoutSession? = null,
    val nextSessionName: String? = null,
    val weeklyVolumeKg: Double = 0.0,
    val weeklySessions: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    mealPlanRepository: MealPlanRepository,
    routineRepository: RoutineRepository,
    diaryRepository: DiaryRepository,
    sessionRepository: SessionRepository,
    foodRepository: FoodRepository
) : ViewModel() {

    private val today = DateUtils.today().toEpochDay()

    private val foodsFlow = foodRepository.observeAll()
    private val diaryFlow = diaryRepository.observeByDate(today)
    private val weekStart = DateUtils.startOfWeek().toEpochDay()
    private val weekSessionsFlow = sessionRepository.observeRange(weekStart, weekStart + 6)

    private val _weeklyVolume = MutableStateFlow(0.0)

    val state: StateFlow<HomeState> = combine(
        profileRepository.observeProfile(),
        mealPlanRepository.observeActive(),
        routineRepository.observeActiveProgram(),
        combine(
            combine(diaryFlow, foodsFlow) { entries, foods -> entries to foods },
            combine(weekSessionsFlow, sessionRepository.observeSessions()) { week, all -> week to all },
            _weeklyVolume
        ) { diaryPair, sessionPair, volume -> Triple(diaryPair, sessionPair, volume) }
    ) { profile, plan, program, bundle ->
        val (diaryPair, sessionPair, volume) = bundle
        val (entries, foods) = diaryPair
        val (week, all) = sessionPair
        val completedThisWeek = week.filter { it.status == SessionStatus.COMPLETED }
        HomeState(
            profile = profile,
            plan = plan,
            routine = program.firstOrNull(),
            today = computeTotals(entries, foods),
            lastSession = all.firstOrNull { it.status == SessionStatus.COMPLETED },
            nextSessionName = program.firstOrNull()?.dayLabel?.ifBlank { program.firstOrNull()?.name },
            weeklySessions = completedThisWeek.size,
            weeklyVolumeKg = volume
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    init {
        // Volume must come from the sets actually recorded, not from a constant.
        viewModelScope.launch {
            combine(weekSessionsFlow, sessionRepository.observeSessions()) { week, _ -> week }
                .collect { week ->
                    val completed = week.filter { it.status == SessionStatus.COMPLETED }
                    val total = completed.sumOf { session ->
                        ProgressionEngine.volume(sessionRepository.getSets(session.id))
                    }
                    _weeklyVolume.value = total
                }
        }
    }

    private fun computeTotals(entries: List<DiaryEntry>, foods: List<Food>): MacroTotals {
        val foodById = foods.associateBy { it.id }
        var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        entries.filter { it.consumed }.forEach { entry ->
            val food = foodById[entry.foodId] ?: return@forEach
            val factor = entry.grams / 100.0
            kcal += food.kcalPer100g * factor
            p += food.proteinPer100g * factor
            c += food.carbsPer100g * factor
            f += food.fatPer100g * factor
        }
        return MacroTotals(kcal, p, c, f)
    }
}
