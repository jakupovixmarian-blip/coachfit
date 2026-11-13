package dev.fitcoach.app.data.repository

import androidx.room.withTransaction
import dev.fitcoach.app.core.security.SecureKeyStore
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.data.local.dao.*
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.local.entity.DiaryEntryEntity
import dev.fitcoach.app.data.local.entity.MealItemEntity
import dev.fitcoach.app.data.local.entity.OnboardingAnswerEntity
import dev.fitcoach.app.data.local.entity.PlanDayEntity
import dev.fitcoach.app.data.local.entity.RoutineExerciseEntity
import dev.fitcoach.app.data.local.mapper.*
import dev.fitcoach.app.data.seed.FoodSeed
import dev.fitcoach.app.data.seed.TrainingScience
import dev.fitcoach.app.data.seed.ExerciseSeed
import dev.fitcoach.app.domain.model.*
import dev.fitcoach.app.domain.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : ProfileRepository {
    private val dao = db.profileDao()
    private val onboarding = db.onboardingDao()

    override fun observeProfile(): Flow<UserProfile?> = dao.observe().map { it?.toDomain() }
    override suspend fun getProfile(): UserProfile? = dao.get()?.toDomain()
    override suspend fun saveProfile(profile: UserProfile) =
        dao.upsert(profile.copy(updatedAt = DateUtils.nowMillis()).toEntity())

    override suspend fun setOnboardingAnswer(key: String, value: String) =
        onboarding.upsert(OnboardingAnswerEntity(key, value, DateUtils.nowMillis()))

    override suspend fun getOnboardingAnswers(): Map<String, String> =
        onboarding.observeAll().first().associate { answer -> answer.key to answer.value }

    override fun observeOnboardingAnswers(): Flow<Map<String, String>> =
        onboarding.observeAll().map { list -> list.associate { it.key to it.value } }

    override suspend fun clearOnboarding() = onboarding.clear()
}

@Singleton
class MeasurementRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : MeasurementRepository {
    private val dao = db.measurementDao()
    override fun observeAll(): Flow<List<Measurement>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeByType(type: MeasurementType): Flow<List<Measurement>> =
        dao.observeByType(type.name).map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<Measurement> = dao.getAll().map { it.toDomain() }
    override suspend fun add(measurement: Measurement): Long =
        dao.insert(measurement.copy(createdAt = DateUtils.nowMillis()).toEntity())
    override suspend fun update(measurement: Measurement) = dao.update(measurement.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : PhotoRepository {
    private val dao = db.photoDao()
    override fun observeAll(): Flow<List<PhotoEntry>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<PhotoEntry> = dao.getAll().map { it.toDomain() }
    override suspend fun add(photo: PhotoEntry): Long =
        dao.insert(photo.copy(createdAt = DateUtils.nowMillis()).toEntity())
    override suspend fun updateNotes(id: Long, notes: String) = dao.updateNotes(id, notes)
    override suspend fun saveAnalysis(id: Long, analysis: String) =
        dao.updateAnalysis(id, analysis, DateUtils.nowMillis())
    override suspend fun delete(id: Long) = dao.delete(id)
}

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : ExerciseRepository {
    private val dao = db.exerciseDao()
    // Rows whose stored enum values are no longer recognisable are skipped one by one
    // instead of failing the whole query, so a single bad row cannot empty the catalog.
    override fun observeAll(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.mapNotNull { it.toDomainOrNull() } }
    override suspend fun getAll(): List<Exercise> = dao.getAll().mapNotNull { it.toDomainOrNull() }
    override suspend fun getById(id: Long): Exercise? = dao.getById(id)?.toDomainOrNull()

    /**
     * Inserts every curated exercise that is not stored yet, matched by name. Seeding used
     * to run only when the table was completely empty, so a partially seeded or restored
     * database stayed without a catalog and the editable lists opened with no exercises.
     */
    override suspend fun ensureSeeded() {
        val stored = dao.getAll().map { it.name.trim().lowercase() }.toSet()
        val missing = ExerciseSeed.all().filter { it.name.trim().lowercase() !in stored }
        if (missing.isNotEmpty()) dao.insertAll(missing.map { it.toEntity() })
    }
    override suspend fun addCustom(exercise: Exercise): Long = dao.insert(exercise.copy(isCustom = true).toEntity())
    override suspend fun update(exercise: Exercise) = dao.update(exercise.toEntity())
    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    override suspend fun setDefaultRest(id: Long, seconds: Int) = dao.setDefaultRest(id, seconds)
    override suspend fun deleteCustom(id: Long) = dao.deleteCustom(id)
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : RoutineRepository {
    private val dao = db.routineDao()
    private val exDao = db.routineExerciseDao()
    private val sessionDao = db.sessionDao()

    override fun observeRoutines(): Flow<List<Routine>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeActive(): Flow<Routine?> = dao.observeActive().map { it?.toDomain() }
    override suspend fun getActive(): Routine? = dao.getActive()?.toDomain()
    override suspend fun getById(id: Long): Routine? = dao.getById(id)?.toDomain()
    override fun observeExercises(routineId: Long): Flow<List<RoutineExercise>> =
        exDao.observeForRoutine(routineId).map { list -> list.map { it.toDomain() } }
    override suspend fun getExercises(routineId: Long): List<RoutineExercise> =
        exDao.getForRoutine(routineId).map { it.toDomain() }

    override suspend fun saveRoutineWithExercises(routine: Routine, exercises: List<RoutineExercise>): Long =
        db.withTransaction {
            val existing = routine.id.takeIf { it != 0L }
            val id = if (existing != null) {
                dao.update(routine.toEntity())
                exDao.deleteForRoutine(existing)
                existing
            } else {
                dao.insert(routine.copy(createdAt = DateUtils.nowMillis()).toEntity())
            }
            exercises.forEachIndexed { index, ex ->
                exDao.insert(ex.copy(routineId = id, orderIndex = index).toEntity())
            }
            id
        }

    override suspend fun setActive(routineId: Long) {
        db.withTransaction {
            val routine = dao.getById(routineId) ?: return@withTransaction
            // Every session of a program is active by definition: activating one
            // must not deactivate its sibling days.
            if (routine.programId != 0L) return@withTransaction
            dao.deactivateAll()
            dao.update(routine.copy(isActive = true))
        }
    }

    override suspend fun delete(routineId: Long) {
        db.withTransaction {
            exDao.deleteForRoutine(routineId)
            dao.delete(routineId)
        }
    }

    override fun observeActiveProgram(): Flow<List<Routine>> =
        dao.observeActiveProgramId().flatMapLatest { programId ->
            if (programId == null || programId == 0L) {
                flowOf(emptyList())
            } else {
                dao.observeProgram(programId).map { list -> list.map { it.toDomain() } }
            }
        }

    override suspend fun getActiveProgram(): List<Routine> {
        val programId = dao.getActiveProgramId() ?: return emptyList()
        return dao.getProgram(programId).map { it.toDomain() }
    }

    override suspend fun getProgram(programId: Long): List<Routine> =
        dao.getProgram(programId).map { it.toDomain() }

    override suspend fun saveProgram(
        programName: String,
        routines: List<Routine>,
        exercisesByRoutineIndex: List<List<RoutineExercise>>
    ): List<Long> = db.withTransaction {
        // Deactivate and drop whatever program was active before, together with
        // the sessions it had planned but not yet performed.
        val previousProgramId = dao.getActiveProgramId()
        if (previousProgramId != null && previousProgramId != 0L) {
            dao.deactivateAll()
            val staleRoutines = dao.getProgram(previousProgramId).map { it.id }
            if (staleRoutines.isNotEmpty()) sessionDao.deletePlannedForRoutines(staleRoutines)
            dao.deleteProgram(previousProgramId)
        }
        val programId = dao.maxProgramId() + 1
        val createdIds = mutableListOf<Long>()
        routines.forEachIndexed { index, routine ->
            val routineId = dao.insert(
                routine.copy(
                    id = 0L,
                    name = routine.name,
                    programId = programId,
                    dayIndex = index,
                    dayLabel = routine.name,
                    isActive = true,
                    createdAt = DateUtils.nowMillis()
                ).toEntity()
            )
            createdIds += routineId
            exercisesByRoutineIndex.getOrNull(index).orEmpty().forEachIndexed { orderIndex, item ->
                exDao.insert(item.copy(id = 0L, routineId = routineId, orderIndex = orderIndex).toEntity())
            }
        }
        createdIds
    }

    override suspend fun duplicate(routineId: Long): Long? = db.withTransaction {
        val routine = dao.getById(routineId) ?: return@withTransaction null
        val newId = dao.insert(routine.copy(id = 0L, name = routine.name + " (copia)", isActive = false, createdAt = DateUtils.nowMillis()))
        exDao.getForRoutine(routineId).forEach { item -> exDao.insert(item.copy(id = 0L, routineId = newId)) }
        newId
    }
}

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : SessionRepository {
    private val dao = db.sessionDao()
    private val setDao = db.loggedSetDao()

    override fun observeSessions(): Flow<List<WorkoutSession>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeByDate(epochDay: Long): Flow<List<WorkoutSession>> =
        dao.observeByDate(epochDay).map { list -> list.map { it.toDomain() } }
    override fun observeRange(from: Long, to: Long): Flow<List<WorkoutSession>> =
        dao.observeRange(from, to).map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<WorkoutSession> = dao.getAll().map { it.toDomain() }
    override suspend fun getById(id: Long): WorkoutSession? = dao.getById(id)?.toDomain()
    override suspend fun getLastCompleted(): WorkoutSession? = dao.getLastCompleted()?.toDomain()
    override suspend fun getInProgress(): WorkoutSession? = dao.getInProgress()?.toDomain()
    override suspend fun createSession(session: WorkoutSession): Long = dao.insert(session.toEntity())
    override suspend fun updateSession(session: WorkoutSession) = dao.update(session.toEntity())
    override suspend fun deleteSession(id: Long) {
        db.withTransaction {
            setDao.deleteForSession(id)
            dao.delete(id)
        }
    }

    override fun observeSets(sessionId: Long): Flow<List<LoggedSet>> =
        setDao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }
    override suspend fun getSets(sessionId: Long): List<LoggedSet> = setDao.getForSession(sessionId).map { it.toDomain() }
    override suspend fun upsertSet(set: LoggedSet): Long =
        setDao.insert(set.copy(loggedAt = if (set.loggedAt == 0L) DateUtils.nowMillis() else set.loggedAt).toEntity())
    override suspend fun deleteSet(id: Long) = setDao.delete(id)

    override suspend fun lastPerformance(exerciseId: Long): ExerciseHistoryEntry? {
        val recent = setDao.getRecentForExercise(exerciseId).map { it.toDomain() }
        val newest = recent.firstOrNull() ?: return null
        val sameSession = recent.filter { it.sessionId == newest.sessionId }.sortedBy { it.setIndex }
        val day = DateUtils.fromEpochDay(java.time.Instant.ofEpochMilli(newest.loggedAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay())
        return ExerciseHistoryEntry(exerciseId, day.toEpochDay(), sameSession)
    }

    override suspend fun historyFor(exerciseId: Long): List<LoggedSet> =
        setDao.getAllForExercise(exerciseId).map { it.toDomain() }
}

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : FoodRepository {
    private val dao = db.foodDao()
    override fun observeAll(): Flow<List<Food>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<Food> = dao.getAll().map { it.toDomain() }
    override suspend fun getById(id: Long): Food? = dao.getById(id)?.toDomain()
    override suspend fun ensureSeeded() {
        if (dao.count() == 0) dao.insertAll(FoodSeed.all().map { it.toEntity() })
    }
    override suspend fun add(food: Food): Long = dao.insert(food.toEntity())
    override suspend fun update(food: Food) = dao.update(food.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

@Singleton
class MealPlanRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : MealPlanRepository {
    private val dao = db.mealPlanDao()
    private val dayDao = db.planDayDao()
    private val itemDao = db.mealItemDao()

    override fun observePlans(): Flow<List<MealPlan>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeActive(): Flow<MealPlan?> = dao.observeActive().map { it?.toDomain() }
    override suspend fun getActive(): MealPlan? = dao.getActive()?.toDomain()
    override suspend fun getById(id: Long): MealPlan? = dao.getById(id)?.toDomain()
    override fun observeDays(planId: Long): Flow<List<PlanDay>> = dayDao.observeForPlan(planId).map { list -> list.map { it.toDomain() } }
    override suspend fun getDays(planId: Long): List<PlanDay> = dayDao.getForPlan(planId).map { it.toDomain() }
    override fun observeItems(planDayId: Long): Flow<List<MealItem>> = itemDao.observeForDay(planDayId).map { list -> list.map { it.toDomain() } }
    override suspend fun getItems(planDayId: Long): List<MealItem> = itemDao.getForDay(planDayId).map { it.toDomain() }
    override suspend fun updateItem(item: MealItem) = itemDao.update(item.toEntity())
    override suspend fun deleteItem(id: Long) = itemDao.delete(id)

    override suspend fun savePlanAtomic(
        plan: MealPlan,
        days: List<PlanDay>,
        itemsByDayIndex: Map<Int, List<MealItem>>
    ): Long = db.withTransaction {
        val existingId = plan.id.takeIf { it != 0L }
        val planId = if (existingId != null) {
            dao.update(plan.copy(isActive = false).toEntity())
            dayDao.deleteForPlan(existingId)
            existingId
        } else {
            dao.insert(plan.copy(isActive = false, createdAt = DateUtils.nowMillis()).toEntity())
        }
        days.sortedBy { it.dayIndex }.forEach { day ->
            val dayId = dayDao.insert(PlanDayEntity(0L, planId, day.dayIndex))
            itemsByDayIndex[day.dayIndex]?.forEach { item ->
                itemDao.insert(item.copy(planDayId = dayId).toEntity())
            }
        }
        planId
    }

    override suspend fun setActive(planId: Long) {
        db.withTransaction {
            dao.deactivateAll()
            dao.getById(planId)?.let { dao.update(it.copy(isActive = true)) }
        }
    }

    override suspend fun deletePlan(planId: Long) {
        db.withTransaction {
            dayDao.deleteForPlan(planId)
            dao.delete(planId)
        }
    }
}

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : DiaryRepository {
    private val dao = db.diaryDao()
    override fun observeByDate(epochDay: Long): Flow<List<DiaryEntry>> =
        dao.observeByDate(epochDay).map { list -> list.map { it.toDomain() } }
    override fun observeAll(): Flow<List<DiaryEntry>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getByDate(epochDay: Long): List<DiaryEntry> = dao.getByDate(epochDay).map { it.toDomain() }
    override suspend fun log(entry: DiaryEntry): Long = dao.insert(entry.copy(loggedAt = DateUtils.nowMillis()).toEntity())
    override suspend fun update(entry: DiaryEntry) = dao.update(entry.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun removeByPlanItem(epochDay: Long, slot: MealSlot, planItemId: Long) =
        dao.deleteByPlanItem(epochDay, slot.name, planItemId)
}

@Singleton
class ShoppingRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : ShoppingRepository {
    private val dao = db.shoppingDao()
    override fun observeWeek(weekStart: Long): Flow<List<ShoppingItem>> =
        dao.observeWeek(weekStart).map { list -> list.map { it.toDomain() } }
    override suspend fun getWeek(weekStart: Long): List<ShoppingItem> = dao.getWeek(weekStart).map { it.toDomain() }
    override suspend fun add(item: ShoppingItem): Long = dao.insert(item.toEntity())
    override suspend fun addAll(items: List<ShoppingItem>) = dao.insertAll(items.map { it.toEntity() })
    override suspend fun update(item: ShoppingItem) = dao.update(item.toEntity())
    override suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun replaceWeek(weekStart: Long, items: List<ShoppingItem>) {
        db.withTransaction {
            dao.deleteWeek(weekStart)
            dao.insertAll(items.map { item -> item.copy(weekStartEpochDay = weekStart).toEntity() })
        }
    }
}

@Singleton
class CoachRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : CoachRepository {
    private val dao = db.coachDao()
    override fun observeProposals(): Flow<List<CoachProposal>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getProposals(): List<CoachProposal> = dao.getAll().map { it.toDomain() }
    override suspend fun addProposal(proposal: CoachProposal): Long =
        dao.insert(proposal.copy(createdAt = if (proposal.createdAt == 0L) DateUtils.nowMillis() else proposal.createdAt).toEntity())
    override suspend fun updateProposal(proposal: CoachProposal) = dao.update(proposal.toEntity())
    override suspend fun deleteProposal(id: Long) = dao.delete(id)
}

@Singleton
class CoachChatRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : CoachChatRepository {
    private val dao = db.coachChatDao()
    override fun observeMessages(): Flow<List<CoachChatMessage>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getMessages(): List<CoachChatMessage> = dao.getAll().map { it.toDomain() }
    override suspend fun add(message: CoachChatMessage): Long =
        dao.insert(message.copy(createdAt = if (message.createdAt == 0L) DateUtils.nowMillis() else message.createdAt).toEntity())
    override suspend fun clear() = dao.clear()
}

@Singleton
class KnowledgeRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase
) : KnowledgeRepository {
    private val dao = db.knowledgeDao()
    override fun observeAll(): Flow<List<KnowledgeNote>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getEnabled(): List<KnowledgeNote> = dao.getEnabled().map { it.toDomain() }
    override suspend fun getAll(): List<KnowledgeNote> = dao.getAll().map { it.toDomain() }
    override suspend fun add(note: KnowledgeNote): Long =
        dao.insert(note.copy(createdAt = DateUtils.nowMillis()).toEntity())
    override suspend fun update(note: KnowledgeNote) = dao.update(note.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun ensureSeeded() {
        if (dao.builtInCount() == 0) {
            dao.insertAll(TrainingScience.builtIn().map { it.toEntity() })
        }
    }
}

@Singleton
class AiConfigRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase,
    private val secureKeyStore: SecureKeyStore
) : AiConfigRepository {
    private val dao = db.aiConfigDao()

    override fun observeConfig(): Flow<AiConfig> =
        dao.observe().map { it?.toDomain() ?: AiConfig() }

    override suspend fun getConfig(): AiConfig = dao.get()?.toDomain() ?: AiConfig()
    override suspend fun saveConfig(config: AiConfig) = dao.upsert(config.toEntity())
    override fun hasApiKey(provider: AiProvider): Boolean = secureKeyStore.hasApiKey(provider.id)
    override fun saveApiKey(provider: AiProvider, key: String) = secureKeyStore.putApiKey(provider.id, key)
    override fun getApiKey(provider: AiProvider): String? = secureKeyStore.getApiKey(provider.id)
    override fun clearApiKey(provider: AiProvider) = secureKeyStore.clear(provider.id)
}
