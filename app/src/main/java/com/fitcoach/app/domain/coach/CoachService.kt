package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.ShoppingRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.ChatMessage
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.nutrition.BudgetEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestratore del coach AI: costruisce il contesto reale dell'utente,
 * invia i messaggi al provider configurato e interpreta le risposte.
 *
 * L'AI non scrive mai sul database: le proposte di modifica tornano come
 * [CoachAction] e vengono applicate solo dopo conferma esplicita dell'utente
 * tramite [CoachActionExecutor].
 */
@Singleton
class CoachService @Inject constructor(
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val progressRepository: ProgressRepository,
    private val changeRepository: ChangeRepository,
    private val chatRepository: ChatRepository,
    private val diaryRepository: NutritionDiaryRepository,
    private val shoppingRepository: ShoppingRepository,
    private val substitutionRepository: SubstitutionRepository,
    private val foodRepository: FoodRepository,
    private val aiClient: AiClient,
    private val secureKeyStore: SecureKeyStore,
    private val orchestrator: CoachOrchestrator
) {

    suspend fun buildContext(historyLimit: Int = 12): CoachContext {
        val profile = userRepository.get()
        val nutrition = nutritionRepository.getActivePlan()
        val workout = workoutRepository.getActivePlan()
        val weights = progressRepository.getWeights()
        val changes = changeRepository.getAll()
        val history = chatRepository.getRecent(historyLimit)

        val recentPerf = workout?.days
            ?.flatMap { it.exercises }
            ?.flatMap { workoutRepository.getPerformanceFor(it.exerciseId) }
            ?.sortedByDescending { it.dateEpochDay }
            ?.take(15)
            ?: emptyList()

        val today = NutritionDiaryRepository.todayEpochDay()
        val weekStart = NutritionDiaryRepository.weekStart(today)
        val todayBalance = runCatching { diaryRepository.balanceForDay(today) }.getOrNull()
        val weekly = runCatching { diaryRepository.weeklySummary(weekStart) }.getOrNull()
        val shopping = runCatching { shoppingRepository.itemsForWeek(weekStart) }.getOrNull() ?: emptyList()
        val substitutions = runCatching { substitutionRepository.history() }.getOrNull() ?: emptyList()
        val budgetReport = runCatching {
            if (nutrition != null && profile != null) {
                val foodsById = foodRepository.getAll().associateBy { it.id }
                BudgetEngine.report(nutrition, foodsById, profile)
            } else null
        }.getOrNull()

        return CoachContext(
            profile = profile,
            nutritionPlan = nutrition,
            workoutPlan = workout,
            recentPerformances = recentPerf,
            weightTrend = if (weights.isEmpty()) null else WeightAnalyzer.trend(weights),
            recentChanges = changes.sortedByDescending { it.createdAt },
            history = history,
            todayBalance = todayBalance,
            weeklyNutrition = weekly,
            shoppingList = shopping,
            substitutions = substitutions,
            budgetReport = budgetReport
        )
    }

    /** Elabora un messaggio dell'utente e restituisce la risposta del coach. */
    suspend fun send(userMessage: String): CoachReply {
        val text = userMessage.trim()
        if (text.isEmpty()) return CoachReply(text = "", providerError = "Messaggio vuoto.")

        val config = resolveConfig()
        if (config == null || !config.isConfigured) {
            return CoachReply(
                text = "",
                providerError = "Configura un provider AI in Impostazioni: scegli provider, " +
                    "modello e inserisci la chiave API, poi prova la connessione."
            )
        }

        val context = buildContext()
        val history = buildHistory(context)
        val turn = orchestrator.run(
            config = config,
            systemPrompt = CoachPromptBuilder.systemPrompt,
            contextPrompt = CoachPromptBuilder.buildContext(context),
            history = history,
            userMessage = text
        )
        if (turn.providerError != null && turn.text.isBlank()) {
            return CoachReply(text = "", providerError = turn.providerError)
        }
        // Le modifiche proposte dagli strumenti di scrittura sono gia registrate come
        // PROPOSED. Un turno puo contenerne piu di una (per esempio tecnica su due
        // esercizi): mostrarne solo l'ultima lasciava le altre registrate ma
        // invisibili, quindi non confermabili.
        val pendingChanges = turn.pendingChangeIds.mapNotNull { changeRepository.getById(it) }
        return CoachReply(
            text = turn.text,
            proposedAction = null,
            pendingChange = pendingChanges.lastOrNull(),
            pendingChanges = pendingChanges,
            lastFinishReason = turn.lastFinishReason,
            toolTrace = turn.toolTrace
        )
    }

    /** Verifica la connessione al provider configurato. */
    suspend fun testConnection(): AiResult {
        val config = resolveConfig()
            ?: return AiResult.Failure("Provider non configurato.")
        if (!config.isConfigured) {
            return AiResult.Failure("Configura provider, modello e chiave API prima di testare.")
        }
        return aiClient.testConnection(config)
    }

    private suspend fun resolveConfig(): AiProviderConfig? {
        val settings = chatRepository.getAiSettings()
        val provider = settings.provider
        val baseUrl = when {
            settings.useBackendProxy && settings.backendBaseUrl.isNotBlank() -> settings.backendBaseUrl
            else -> settings.baseUrl.ifBlank { provider.defaultBaseUrl }
        }
        val model = settings.model.ifBlank { provider.defaultModel }
        val key = if (settings.useBackendProxy) {
            null
        } else {
            secureKeyStore.getApiKey(provider.name)
        }
        return AiProviderConfig(
            provider = provider,
            baseUrl = baseUrl,
            model = model,
            apiKey = key,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            usesBackendProxy = settings.useBackendProxy
        )
    }

    private fun buildHistory(context: CoachContext): List<ChatMessageDto> {
        val history = mutableListOf<ChatMessageDto>()
        context.history
            .filter { it.role != ChatRole.SYSTEM && !it.isError }
            .takeLast(10)
            .forEach { msg ->
                val role = if (msg.role == ChatRole.USER) "user" else "assistant"
                if (msg.content.isNotBlank()) history += ChatMessageDto(role = role, content = msg.content)
            }
        return history
    }
}
