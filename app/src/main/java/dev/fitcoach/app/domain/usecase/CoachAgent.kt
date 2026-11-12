package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.PlanDay
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.KnowledgeRepository
import dev.fitcoach.app.domain.repository.MealPlanRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a program generation, with what the app actually saved. */
data class ProgramOutcome(
    val name: String,
    val rationale: String,
    val sessionCount: Int,
    val exerciseCount: Int,
    val totalSets: Int
)

data class MealPlanOutcome(
    val name: String,
    val rationale: String,
    val dayCount: Int,
    val itemCount: Int
)

/**
 * The agent that runs the whole app: it conducts the intake, generates the
 * training program and the meal plan, and applies the actions the chat asks for.
 *
 * No training or nutrition value is produced here. The agent builds the context,
 * calls the model through [AiGateway] and only checks that the answer refers to
 * real catalog entries before persisting it.
 */
@Singleton
class CoachAgent @Inject constructor(
    private val gateway: AiGateway,
    private val prompts: CoachAgentPrompts,
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
    private val foodRepository: FoodRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val knowledgeRepository: KnowledgeRepository
) {

    private val systemPrompt =
        "Sei l'agente di FitCoach. Rispondi sempre in JSON valido, in italiano, senza testo fuori dal JSON."

    suspend fun isConfigured(): Boolean = gateway.isConfigured()

    // ---- anamnesi -------------------------------------------------------

    /** One turn of the intake conversation: asks, and reports what it learned. */
    suspend fun anamnesiReply(profile: UserProfile, conversation: List<CoachChatMessage>): AnamnesiReply {
        val turns = conversation.map { it.role.label to it.content }
        val prompt = prompts.anamnesiPrompt(profile, turns)
        val raw = gateway.complete(systemPrompt, prompt)
        return prompts.parseAnamnesi(raw)
    }

    // ---- training program -----------------------------------------------

    /** Generates a program from the profile and saves it as the active one. */
    suspend fun generateProgram(notes: String = ""): ProgramOutcome {
        val profile = profileRepository.getProfile()
            ?: throw AppError.Validation("Profilo non disponibile: completa prima l'anamnesi.")
        val catalog = exerciseRepository.getAll()
        if (catalog.isEmpty()) throw AppError.Validation("Catalogo esercizi vuoto.")
        val knowledge = knowledgeRepository.getEnabled()

        val raw = gateway.complete(systemPrompt, prompts.programPrompt(profile, catalog, knowledge, notes))
        val draft = prompts.parseProgram(raw, catalog.map { it.id }.toSet())

        val routines = draft.days.mapIndexed { index, day ->
            Routine(
                name = day.label,
                description = draft.rationale,
                isActive = true,
                programId = 0L,
                dayIndex = index,
                dayLabel = day.label
            )
        }
        val exercisesByRoutine = draft.days.map { day ->
            day.exercises.mapIndexed { order, planned ->
                RoutineExercise(
                    routineId = 0L,
                    exerciseId = planned.exerciseId,
                    orderIndex = order,
                    targetSets = planned.sets,
                    targetRepsMin = planned.repsMin,
                    targetRepsMax = planned.repsMax,
                    targetRir = planned.rir ?: 2,
                    restSeconds = planned.restSeconds,
                    notes = planned.notes
                )
            }
        }
        val ids = routineRepository.saveProgram(draft.name, routines, exercisesByRoutine)
        planWeek(ids, draft.days.map { it.label })

        return ProgramOutcome(
            name = draft.name,
            rationale = draft.rationale,
            sessionCount = draft.days.size,
            exerciseCount = draft.days.sumOf { it.exercises.size },
            totalSets = draft.days.sumOf { day -> day.exercises.sumOf { it.sets } }
        )
    }

    /** Places every session of the new program on a distinct day of the week. */
    private suspend fun planWeek(routineIds: List<Long>, labels: List<String>) {
        if (labels.isEmpty()) return
        val start = DateUtils.startOfWeek()
        val spacing = (7 / labels.size).coerceAtLeast(1)
        labels.forEachIndexed { index, label ->
            val routineId = routineIds.getOrNull(index) ?: return@forEachIndexed
            val date = start.plusDays((index * spacing).coerceAtMost(6).toLong())
            sessionRepository.createSession(
                WorkoutSession(
                    routineId = routineId,
                    name = label,
                    dateEpochDay = date.toEpochDay(),
                    startedAt = DateUtils.nowMillis(),
                    status = SessionStatus.PLANNED
                )
            )
        }
    }

    // ---- meal plan ------------------------------------------------------

    /** Generates a weekly meal plan and activates it. */
    suspend fun generateMealPlan(notes: String = ""): MealPlanOutcome {
        val profile = profileRepository.getProfile()
            ?: throw AppError.Validation("Profilo non disponibile: completa prima l'anamnesi.")
        val foods = foodRepository.getAll()
        if (foods.isEmpty()) throw AppError.Validation("Archivio alimenti vuoto.")
        val knowledge = knowledgeRepository.getEnabled()

        val raw = gateway.complete(systemPrompt, prompts.mealPlanPrompt(profile, foods, knowledge, notes))
        val draft = prompts.parseMealPlan(raw, foods.map { it.id }.toSet())

        val plan = MealPlan(
            name = draft.name,
            isActive = true,
            targetKcal = profile.targetCalories() ?: 0.0,
            targetProtein = profile.proteinTarget() ?: 0.0,
            targetCarbs = profile.carbsTarget() ?: 0.0,
            targetFat = profile.fatTarget() ?: 0.0,
            source = FoodSource.AI_PLAN
        )
        val days = draft.days.sortedBy { it.dayIndex }.map { PlanDay(planId = 0L, dayIndex = it.dayIndex) }
        val itemsByDay = draft.days.associate { day ->
            day.dayIndex to day.items.map { item ->
                MealItem(
                    planDayId = 0L,
                    mealSlot = item.slot,
                    foodId = item.foodId,
                    grams = item.grams,
                    preparation = item.preparation
                )
            }
        }
        val planId = mealPlanRepository.savePlanAtomic(plan, days, itemsByDay)
        mealPlanRepository.setActive(planId)

        return MealPlanOutcome(
            name = draft.name,
            rationale = draft.rationale,
            dayCount = days.size,
            itemCount = itemsByDay.values.sumOf { it.size }
        )
    }

    // ---- chat -----------------------------------------------------------

    /** Answers a chat message and reports the actions the app must run. */
    suspend fun chat(history: List<CoachChatMessage>, userMessage: String): AgentReply {
        val profile = profileRepository.getProfile()
        val program = routineRepository.getActiveProgram()
        val plan = mealPlanRepository.getActive()
        val catalog = exerciseRepository.getAll()
        val foods = foodRepository.getAll()
        val knowledge = knowledgeRepository.getEnabled()
        val prompt = prompts.chatPrompt(profile, program, plan, catalog, foods, knowledge)
        val raw = gateway.completeWithHistory(systemPrompt, history, userMessage + "\n\n" + prompt)
        return prompts.parseAgentReply(raw)
    }

    /**
     * Runs an action the chat asked for. Actions that cannot be resolved on real
     * data (unknown exercise, missing profile) are skipped, and the reason is
     * returned so the UI can report it.
     */
    suspend fun applyAction(action: CoachAction): String? = when (action) {
        is CoachAction.GenerateProgram -> {
            val outcome = generateProgram(action.notes)
            "Programma rigenerato: ${outcome.sessionCount} sedute, ${outcome.exerciseCount} esercizi."
        }
        is CoachAction.GenerateMealPlan -> {
            val outcome = generateMealPlan(action.notes)
            "Piano alimentare generato: ${outcome.dayCount} giorni, ${outcome.itemCount} voci."
        }
        is CoachAction.UpdateProfile -> {
            val profile = profileRepository.getProfile() ?: UserProfile()
            profileRepository.saveProfile(prompts.applyProfilePatch(profile, action.patch))
            "Profilo aggiornato: ${action.patch.keys.joinToString(", ")}."
        }
        is CoachAction.SetRest -> applyRest(action)
        is CoachAction.AddExercise -> applyAddExercise(action)
        is CoachAction.RemoveExercise -> applyRemoveExercise(action)
    }

    private suspend fun applyRest(action: CoachAction.SetRest): String? {
        val target = findRoutineExercise(action.exerciseName) ?: return null
        val (routine, item) = target
        val exercises = routineRepository.getExercises(routine.id).map { re ->
            if (re.id == item.id) re.copy(restSeconds = action.seconds) else re
        }
        routineRepository.saveRoutineWithExercises(routine, exercises)
        return "Recupero di ${item.exerciseId} impostato a ${action.seconds}s."
    }

    private suspend fun applyAddExercise(action: CoachAction.AddExercise): String? {
        val exercise = findExercise(action.exerciseName) ?: return null
        val routine = findRoutine(action.routineLabel)
            ?: routineRepository.getActiveProgram().firstOrNull()
            ?: return null
        val existing = routineRepository.getExercises(routine.id)
        if (existing.any { it.exerciseId == exercise.id }) return null
        val updated = existing + RoutineExercise(
            routineId = routine.id,
            exerciseId = exercise.id,
            orderIndex = existing.size,
            targetSets = action.sets ?: 3,
            restSeconds = exercise.defaultRestSeconds
        )
        routineRepository.saveRoutineWithExercises(routine, updated)
        return "${exercise.name} aggiunto a ${routine.dayLabel.ifBlank { routine.name }}."
    }

    private suspend fun applyRemoveExercise(action: CoachAction.RemoveExercise): String? {
        val exercise = findExercise(action.exerciseName) ?: return null
        val routine = findRoutine(action.routineLabel)
            ?: routineRepository.getActiveProgram().firstOrNull()
            ?: return null
        val existing = routineRepository.getExercises(routine.id)
        val updated = existing.filterNot { it.exerciseId == exercise.id }
        if (updated.size == existing.size) return null
        routineRepository.saveRoutineWithExercises(routine, updated)
        return "${exercise.name} rimosso da ${routine.dayLabel.ifBlank { routine.name }}."
    }

    private suspend fun findExercise(name: String): Exercise? {
        val needle = name.trim().lowercase()
        if (needle.isBlank()) return null
        val catalog = exerciseRepository.getAll()
        return catalog.firstOrNull { it.name.lowercase() == needle }
            ?: catalog.firstOrNull { it.name.lowercase().contains(needle) }
            ?: catalog.firstOrNull { needle.contains(it.name.lowercase()) }
    }

    private suspend fun findRoutine(label: String): Routine? {
        val needle = label.trim().lowercase()
        if (needle.isBlank()) return null
        val program = routineRepository.getActiveProgram()
        return program.firstOrNull { it.dayLabel.lowercase() == needle }
            ?: program.firstOrNull { it.dayLabel.lowercase().contains(needle) }
    }

    private suspend fun findRoutineExercise(exerciseName: String): Pair<Routine, RoutineExercise>? {
        val exercise = findExercise(exerciseName) ?: return null
        val program = routineRepository.getActiveProgram()
        program.forEach { routine ->
            val item = routineRepository.getExercises(routine.id).firstOrNull { it.exerciseId == exercise.id }
            if (item != null) return routine to item
        }
        return null
    }
}
