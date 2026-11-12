package dev.fitcoach.app.domain.repository

import dev.fitcoach.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun setOnboardingAnswer(key: String, value: String)
    suspend fun getOnboardingAnswers(): Map<String, String>
    fun observeOnboardingAnswers(): Flow<Map<String, String>>
    suspend fun clearOnboarding()
}

interface MeasurementRepository {
    fun observeAll(): Flow<List<Measurement>>
    fun observeByType(type: MeasurementType): Flow<List<Measurement>>
    suspend fun getAll(): List<Measurement>
    suspend fun add(measurement: Measurement): Long
    suspend fun update(measurement: Measurement)
    suspend fun delete(id: Long)
}

interface PhotoRepository {
    fun observeAll(): Flow<List<PhotoEntry>>
    suspend fun getAll(): List<PhotoEntry>
    suspend fun add(photo: PhotoEntry): Long
    suspend fun updateNotes(id: Long, notes: String)
    suspend fun delete(id: Long)
}

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>
    suspend fun getAll(): List<Exercise>
    suspend fun getById(id: Long): Exercise?
    suspend fun ensureSeeded()
    suspend fun addCustom(exercise: Exercise): Long
    suspend fun update(exercise: Exercise)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun deleteCustom(id: Long)
}

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    fun observeActive(): Flow<Routine?>

    /** Sessions of the active program, ordered by day. Empty when no program is active. */
    fun observeActiveProgram(): Flow<List<Routine>>
    suspend fun getActiveProgram(): List<Routine>
    suspend fun getProgram(programId: Long): List<Routine>

    /**
     * Persists every session of a program in one transaction: deactivates the
     * previous program, replaces its sessions and activates the new one.
     */
    suspend fun saveProgram(
        programName: String,
        routines: List<Routine>,
        exercisesByRoutineIndex: List<List<RoutineExercise>>
    ): List<Long>
    suspend fun getActive(): Routine?
    suspend fun getById(id: Long): Routine?
    fun observeExercises(routineId: Long): Flow<List<RoutineExercise>>
    suspend fun getExercises(routineId: Long): List<RoutineExercise>
    suspend fun saveRoutineWithExercises(routine: Routine, exercises: List<RoutineExercise>): Long
    suspend fun setActive(routineId: Long)
    suspend fun delete(routineId: Long)
    suspend fun duplicate(routineId: Long): Long?
}

interface SessionRepository {
    fun observeSessions(): Flow<List<WorkoutSession>>
    fun observeByDate(epochDay: Long): Flow<List<WorkoutSession>>
    fun observeRange(from: Long, to: Long): Flow<List<WorkoutSession>>
    suspend fun getAll(): List<WorkoutSession>
    suspend fun getById(id: Long): WorkoutSession?
    suspend fun getLastCompleted(): WorkoutSession?
    suspend fun getInProgress(): WorkoutSession?
    suspend fun createSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(id: Long)
    fun observeSets(sessionId: Long): Flow<List<LoggedSet>>
    suspend fun getSets(sessionId: Long): List<LoggedSet>
    suspend fun upsertSet(set: LoggedSet): Long
    suspend fun deleteSet(id: Long)
    suspend fun lastPerformance(exerciseId: Long): ExerciseHistoryEntry?
    suspend fun historyFor(exerciseId: Long): List<LoggedSet>
}

interface FoodRepository {
    fun observeAll(): Flow<List<Food>>
    suspend fun getAll(): List<Food>
    suspend fun getById(id: Long): Food?
    suspend fun ensureSeeded()
    suspend fun add(food: Food): Long
    suspend fun update(food: Food)
    suspend fun delete(id: Long)
}

interface MealPlanRepository {
    fun observePlans(): Flow<List<MealPlan>>
    fun observeActive(): Flow<MealPlan?>
    suspend fun getActive(): MealPlan?
    suspend fun getById(id: Long): MealPlan?
    fun observeDays(planId: Long): Flow<List<PlanDay>>
    suspend fun getDays(planId: Long): List<PlanDay>
    fun observeItems(planDayId: Long): Flow<List<MealItem>>
    suspend fun getItems(planDayId: Long): List<MealItem>
    suspend fun savePlanAtomic(plan: MealPlan, days: List<PlanDay>, itemsByDayIndex: Map<Int, List<MealItem>>): Long
    suspend fun updateItem(item: MealItem)
    suspend fun deleteItem(id: Long)
    suspend fun setActive(planId: Long)
    suspend fun deletePlan(planId: Long)
}

interface DiaryRepository {
    fun observeByDate(epochDay: Long): Flow<List<DiaryEntry>>
    fun observeAll(): Flow<List<DiaryEntry>>
    suspend fun getByDate(epochDay: Long): List<DiaryEntry>
    suspend fun log(entry: DiaryEntry): Long
    suspend fun update(entry: DiaryEntry)
    suspend fun delete(id: Long)
    suspend fun removeByPlanItem(epochDay: Long, slot: MealSlot, planItemId: Long)
}

interface ShoppingRepository {
    fun observeWeek(weekStart: Long): Flow<List<ShoppingItem>>
    suspend fun getWeek(weekStart: Long): List<ShoppingItem>
    suspend fun add(item: ShoppingItem): Long
    suspend fun addAll(items: List<ShoppingItem>)
    suspend fun update(item: ShoppingItem)
    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun delete(id: Long)
    suspend fun replaceWeek(weekStart: Long, items: List<ShoppingItem>)
}

interface CoachRepository {
    fun observeProposals(): Flow<List<CoachProposal>>
    suspend fun getProposals(): List<CoachProposal>
    suspend fun addProposal(proposal: CoachProposal): Long
    suspend fun updateProposal(proposal: CoachProposal)
    suspend fun deleteProposal(id: Long)
}

interface AiConfigRepository {
    fun observeConfig(): Flow<AiConfig>
    suspend fun getConfig(): AiConfig
    suspend fun saveConfig(config: AiConfig)
    fun hasApiKey(provider: AiProvider): Boolean
    fun saveApiKey(provider: AiProvider, key: String)
    fun getApiKey(provider: AiProvider): String?
    fun clearApiKey(provider: AiProvider)
}
