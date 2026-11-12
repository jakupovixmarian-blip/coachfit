package dev.fitcoach.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.data.ai.AiClient
import dev.fitcoach.app.data.ai.ChatMessage
import dev.fitcoach.app.domain.model.CoachChangeStatus
import dev.fitcoach.app.domain.model.CoachChangeType
import dev.fitcoach.app.domain.model.CoachInsight
import dev.fitcoach.app.domain.model.CoachProposal
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.PlanDay
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.repository.AiConfigRepository
import dev.fitcoach.app.domain.repository.CoachRepository
import dev.fitcoach.app.domain.repository.DiaryRepository
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.MealPlanRepository
import dev.fitcoach.app.domain.repository.MeasurementRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.CoachEngine
import dev.fitcoach.app.domain.usecase.CoachPromptBuilder
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val insights: List<CoachInsight> = emptyList(),
    val proposals: List<CoachProposal> = emptyList(),
    val profile: UserProfile? = null,
    val hasApiKey: Boolean = false,
    val generating: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val measurementRepository: MeasurementRepository,
    private val diaryRepository: DiaryRepository,
    private val foodRepository: FoodRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val aiConfigRepository: AiConfigRepository,
    private val aiClient: AiClient,
    private val promptBuilder: CoachPromptBuilder
) : ViewModel() {

    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    init {
        refreshInsights()
        viewModelScope.launch {
            coachRepository.observeProposals().collect { proposals ->
                _state.value = _state.value.copy(proposals = proposals)
            }
        }
    }

    fun refreshInsights() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            val sessions = sessionRepository.getAll()
            val measurements = measurementRepository.getAll()
            val diary = diaryRepository.observeAll().first()
            val foods = foodRepository.getAll()
            val setsBySession = sessions.associate { it.id to sessionRepository.getSets(it.id) }
            val insights = CoachEngine.insights(profile, sessions, measurements, diary, foods, setsBySession)
            _state.value = _state.value.copy(
                insights = insights,
                profile = profile,
                hasApiKey = aiConfigRepository.hasApiKey(aiConfigRepository.getConfig().provider)
            )
        }
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    /** Calls the configured provider and stores the answer as a pending proposal. */
    fun generatePlanWithAi() {
        viewModelScope.launch {
            val config = aiConfigRepository.getConfig()
            val apiKey = aiConfigRepository.getApiKey(config.provider)
            if (apiKey.isNullOrBlank()) {
                _state.value = _state.value.copy(error = "Configura una chiave API nelle impostazioni AI.")
                return@launch
            }
            val profile = profileRepository.getProfile()
            if (profile == null) {
                _state.value = _state.value.copy(error = "Completa prima l'anamnesi.")
                return@launch
            }
            val foods = foodRepository.getAll()
            if (foods.isEmpty()) {
                _state.value = _state.value.copy(error = "Archivio alimenti vuoto.")
                return@launch
            }
            _state.value = _state.value.copy(generating = true, error = null, message = null)
            try {
                val prompt = promptBuilder.planPrompt(
                    profile = profile,
                    foods = foods,
                    targetKcal = profile.targetCalories(),
                    targetProtein = profile.proteinTarget()
                )
                val response = aiClient.chat(
                    config = config,
                    apiKey = apiKey,
                    messages = listOf(
                        ChatMessage("system", "Rispondi sempre in JSON valido secondo lo schema richiesto."),
                        ChatMessage("user", prompt)
                    ),
                    jsonMode = true
                )
                val parsed = promptBuilder.parsePlan(response.content, foods.map { it.id }.toSet())

                // Validate against real targets and the real catalog.
                val foodById = foods.associateBy { it.id }
                val validation = CoachEngine.validatePlan(
                    items = parsed.items.map { it.foodId to it.grams },
                    foodById = foodById,
                    targetKcal = profile.targetCalories(),
                    targetProtein = profile.proteinTarget(),
                    targetCarbs = profile.carbsTarget(),
                    targetFat = profile.fatTarget()
                )
                val summary = buildString {
                    append("Calorie %.0f, proteine %.0f g, carboidrati %.0f g, grassi %.0f g. ".format(
                        validation.totalKcal, validation.totalProtein, validation.totalCarbs, validation.totalFat
                    ))
                    if (validation.issues.isNotEmpty()) {
                        append("Rilievi: ")
                        append(validation.issues.joinToString("; "))
                    }
                }
                val proposal = promptBuilder.proposalFor(
                    type = CoachChangeType.MEALS,
                    rationale = parsed.rationale.ifBlank { "Piano generato dal modello." },
                    previous = "Nessun piano AI applicato",
                    new = summary
                )
                coachRepository.addProposal(proposal)
                _state.value = _state.value.copy(
                    generating = false,
                    message = "Proposta creata. Controlla i numeri e approva per applicarla.",
                    error = if (validation.issues.isEmpty()) null else validation.issues.joinToString(" ")
                )
            } catch (error: AppError) {
                _state.value = _state.value.copy(generating = false, error = error.message)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(generating = false, error = t.message ?: "Errore sconosciuto")
            }
        }
    }

    fun approveProposal(proposal: CoachProposal) {
        viewModelScope.launch {
            val applied = proposal.copy(status = CoachChangeStatus.APPLIED, appliedAt = DateUtils.nowMillis())
            coachRepository.updateProposal(applied)
            _state.value = _state.value.copy(message = "Proposta approvata")
        }
    }

    fun rejectProposal(proposal: CoachProposal) {
        viewModelScope.launch {
            coachRepository.updateProposal(proposal.copy(status = CoachChangeStatus.REJECTED))
            _state.value = _state.value.copy(message = "Proposta rifiutata")
        }
    }

    fun deleteProposal(proposal: CoachProposal) {
        viewModelScope.launch { coachRepository.deleteProposal(proposal.id) }
    }
}
