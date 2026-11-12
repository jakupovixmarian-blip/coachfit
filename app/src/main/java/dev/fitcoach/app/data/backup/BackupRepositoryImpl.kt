package dev.fitcoach.app.data.backup

import androidx.room.withTransaction
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.local.entity.CoachProposalEntity
import dev.fitcoach.app.data.local.entity.DiaryEntryEntity
import dev.fitcoach.app.data.local.entity.ExerciseEntity
import dev.fitcoach.app.data.local.entity.FoodEntity
import dev.fitcoach.app.data.local.entity.LoggedSetEntity
import dev.fitcoach.app.data.local.entity.MealItemEntity
import dev.fitcoach.app.data.local.entity.MealPlanEntity
import dev.fitcoach.app.data.local.entity.MeasurementEntity
import dev.fitcoach.app.data.local.entity.PhotoEntity
import dev.fitcoach.app.data.local.entity.PlanDayEntity
import dev.fitcoach.app.data.local.entity.ProfileEntity
import dev.fitcoach.app.data.local.entity.RoutineEntity
import dev.fitcoach.app.data.local.entity.RoutineExerciseEntity
import dev.fitcoach.app.data.local.entity.ShoppingItemEntity
import dev.fitcoach.app.data.local.entity.WorkoutSessionEntity
import dev.fitcoach.app.domain.repository.BackupRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val db: FitCoachDatabase,
    private val json: Json
) : BackupRepository {

    override suspend fun exportSnapshot(): String {
        val snapshot = db.withTransaction {
            val routines = db.routineDao().getAll()
            val plans = db.mealPlanDao().getAll()
            val sessions = db.sessionDao().getAll()
            val exerciseNames = db.exerciseDao().getAll().associate { it.id to it.name }
            val foodNames = db.foodDao().getAll().associate { it.id to it.name }
            val planDayById = db.planDayDao().getAll().associateBy { it.id }

            BackupSnapshot(
                exportedAt = DateUtils.nowMillis(),
                profile = db.profileDao().get()?.let { p ->
                    ProfileBackup(
                        p.name, p.age, p.sex, p.heightCm, p.weightKg, p.bodyFatPercent, p.goal, p.experience,
                        p.trainingYears, p.consistencyWeeks, p.knownLoads, p.jobActivity, p.dailySteps,
                        p.sleepHours, p.stressLevel, p.recoveryLevel, p.injuries, p.equipment, p.availableDays,
                        p.sessionMinutes, p.dietPreference, p.allergies, p.mealsPerDay, p.mealTimes,
                        p.weeklyBudget, p.cookingMinutes, p.likedFoods, p.dislikedFoods, p.unitSystem,
                        p.onboardingCompleted, p.updatedAt
                    )
                },
                measurements = db.measurementDao().getAll().map {
                    MeasurementBackup(it.type, it.customLabel, it.dateEpochDay, it.value, it.notes, it.createdAt)
                },
                photos = db.photoDao().getAll().map {
                    PhotoBackup(it.dateEpochDay, it.pose, it.customLabel, it.filePath, it.notes, it.createdAt)
                },
                exercises = db.exerciseDao().getAll().map {
                    ExerciseBackup(it.name, it.primaryMuscle, it.secondaryMuscles, it.equipment, it.pattern,
                        it.execution, it.setup, it.commonMistakes, it.isCustom, it.isFavorite)
                },
                routines = routines.map {
                    RoutineBackup(it.id, it.name, it.description, it.isActive, it.createdAt)
                },
                routineExercises = db.routineExerciseDao().getAll().map {
                    RoutineExerciseBackup(it.routineId, exerciseNames[it.exerciseId].orEmpty(), it.orderIndex,
                        it.targetSets, it.targetRepsMin, it.targetRepsMax, it.targetRir, it.restSeconds, it.notes)
                },
                sessions = sessions.map {
                    SessionBackup(it.routineId, it.name, it.dateEpochDay, it.startedAt, it.finishedAt, it.status, it.notes)
                },
                loggedSets = db.loggedSetDao().getAll().map {
                    LoggedSetBackup(
                        sessionIndex = sessions.indexOfFirst { s -> s.id == it.sessionId },
                        exerciseName = exerciseNames[it.exerciseId].orEmpty(),
                        orderIndex = it.orderIndex, setIndex = it.setIndex, setType = it.setType,
                        weightKg = it.weightKg, reps = it.reps, rir = it.rir, rpe = it.rpe,
                        restSeconds = it.restSeconds, pain = it.pain, completed = it.completed,
                        notes = it.notes, loggedAt = it.loggedAt
                    )
                },
                foods = db.foodDao().getAll().map {
                    FoodBackup(it.name, it.brand, it.category, it.kcalPer100g, it.proteinPer100g,
                        it.carbsPer100g, it.fatPer100g, it.fiberPer100g, it.source, it.barcode, it.state,
                        it.pricePerKg, it.isCustom)
                },
                mealPlans = plans.map {
                    MealPlanBackup(it.id, it.name, it.isActive, it.targetKcal, it.targetProtein, it.targetCarbs,
                        it.targetFat, it.createdAt, it.source)
                },
                planDays = db.planDayDao().getAll().map { PlanDayBackup(it.planId, it.dayIndex) },
                mealItems = db.mealItemDao().getAll().mapNotNull { item ->
                    val day = planDayById[item.planDayId] ?: return@mapNotNull null
                    MealItemBackup(day.planId, day.dayIndex, item.mealSlot, foodNames[item.foodId].orEmpty(),
                        item.grams, item.preparation, item.timeLabel)
                },
                diaryEntries = db.diaryDao().getAll().map {
                    DiaryBackup(it.dateEpochDay, it.mealSlot, foodNames[it.foodId].orEmpty(), it.grams,
                        it.consumed, it.loggedAt)
                },
                shoppingItems = db.shoppingDao().getAll().map {
                    ShoppingBackup(it.name, it.quantityGrams, it.category, it.estimatedPrice, it.checked,
                        it.weekStartEpochDay)
                },
                coachProposals = db.coachDao().getAll().map {
                    CoachProposalBackup(it.createdAt, it.changeType, it.rationale, it.previousValue,
                        it.newValue, it.origin, it.status, it.appliedAt, it.appliedTo)
                }
            )
        }
        return json.encodeToString(BackupSnapshot.serializer(), snapshot)
    }

    override suspend fun importSnapshot(content: String): Result<Int> = runCatching {
        val snapshot = json.decodeFromString(BackupSnapshot.serializer(), content)
        db.withTransaction {
            clearAll()
            snapshot.profile?.let { p ->
                db.profileDao().upsert(
                    ProfileEntity(
                        id = 1L, name = p.name, age = p.age, sex = p.sex, heightCm = p.heightCm,
                        weightKg = p.weightKg, bodyFatPercent = p.bodyFatPercent, goal = p.goal,
                        experience = p.experience, trainingYears = p.trainingYears,
                        consistencyWeeks = p.consistencyWeeks, knownLoads = p.knownLoads,
                        jobActivity = p.jobActivity, dailySteps = p.dailySteps, sleepHours = p.sleepHours,
                        stressLevel = p.stressLevel, recoveryLevel = p.recoveryLevel, injuries = p.injuries,
                        equipment = p.equipment, availableDays = p.availableDays, sessionMinutes = p.sessionMinutes,
                        dietPreference = p.dietPreference, allergies = p.allergies, mealsPerDay = p.mealsPerDay,
                        mealTimes = p.mealTimes, weeklyBudget = p.weeklyBudget, cookingMinutes = p.cookingMinutes,
                        likedFoods = p.likedFoods, dislikedFoods = p.dislikedFoods, unitSystem = p.unitSystem,
                        onboardingCompleted = p.onboardingCompleted, updatedAt = p.updatedAt
                    )
                )
            }

            val exerciseIds = mutableMapOf<String, Long>()
            snapshot.exercises.forEach { e ->
                val id = db.exerciseDao().insert(
                    ExerciseEntity(
                        name = e.name, primaryMuscle = e.primaryMuscle, secondaryMuscles = e.secondaryMuscles,
                        equipment = e.equipment, pattern = e.pattern, execution = e.execution, setup = e.setup,
                        commonMistakes = e.commonMistakes, isCustom = e.isCustom, isFavorite = e.isFavorite
                    )
                )
                if (id > 0) exerciseIds[e.name] = id
            }
            db.exerciseDao().getAll().forEach { exerciseIds.putIfAbsent(it.name, it.id) }

            val foodIds = mutableMapOf<String, Long>()
            snapshot.foods.forEach { f ->
                val id = db.foodDao().insert(
                    FoodEntity(
                        name = f.name, brand = f.brand, category = f.category, kcalPer100g = f.kcalPer100g,
                        proteinPer100g = f.proteinPer100g, carbsPer100g = f.carbsPer100g, fatPer100g = f.fatPer100g,
                        fiberPer100g = f.fiberPer100g, source = f.source, barcode = f.barcode, state = f.state,
                        pricePerKg = f.pricePerKg, isCustom = f.isCustom
                    )
                )
                if (id > 0) foodIds[f.name] = id
            }
            db.foodDao().getAll().forEach { foodIds.putIfAbsent(it.name, it.id) }

            snapshot.measurements.forEach { m ->
                db.measurementDao().insert(
                    MeasurementEntity(type = m.type, customLabel = m.customLabel, dateEpochDay = m.dateEpochDay,
                        value = m.value, notes = m.notes, createdAt = m.createdAt)
                )
            }
            snapshot.photos.forEach { p ->
                db.photoDao().insert(
                    PhotoEntity(dateEpochDay = p.dateEpochDay, pose = p.pose, customLabel = p.customLabel,
                        filePath = p.filePath, notes = p.notes, createdAt = p.createdAt)
                )
            }

            val routineIds = mutableMapOf<Long, Long>()
            snapshot.routines.forEach { r ->
                val id = db.routineDao().insert(
                    RoutineEntity(name = r.name, description = r.description, isActive = r.isActive, createdAt = r.createdAt)
                )
                if (id > 0) routineIds[r.localId] = id
            }
            snapshot.routineExercises.forEach { re ->
                val routineId = routineIds[re.routineLocalId] ?: return@forEach
                val exerciseId = exerciseIds[re.exerciseName] ?: return@forEach
                db.routineExerciseDao().insert(
                    RoutineExerciseEntity(routineId = routineId, exerciseId = exerciseId, orderIndex = re.orderIndex,
                        targetSets = re.targetSets, targetRepsMin = re.targetRepsMin, targetRepsMax = re.targetRepsMax,
                        targetRir = re.targetRir, restSeconds = re.restSeconds, notes = re.notes)
                )
            }

            val sessionIds = mutableListOf<Long>()
            snapshot.sessions.forEach { s ->
                val id = db.sessionDao().insert(
                    WorkoutSessionEntity(routineId = s.routineLocalId?.let { routineIds[it] }, name = s.name,
                        dateEpochDay = s.dateEpochDay, startedAt = s.startedAt, finishedAt = s.finishedAt,
                        status = s.status, notes = s.notes)
                )
                sessionIds += id
            }
            snapshot.loggedSets.forEach { set ->
                val sessionId = sessionIds.getOrNull(set.sessionIndex) ?: return@forEach
                val exerciseId = exerciseIds[set.exerciseName] ?: return@forEach
                db.loggedSetDao().insert(
                    LoggedSetEntity(sessionId = sessionId, exerciseId = exerciseId, orderIndex = set.orderIndex,
                        setIndex = set.setIndex, setType = set.setType, weightKg = set.weightKg, reps = set.reps,
                        rir = set.rir, rpe = set.rpe, restSeconds = set.restSeconds, pain = set.pain,
                        completed = set.completed, notes = set.notes, loggedAt = set.loggedAt)
                )
            }

            val planIds = mutableMapOf<Long, Long>()
            snapshot.mealPlans.forEach { p ->
                val id = db.mealPlanDao().insert(
                    MealPlanEntity(name = p.name, isActive = p.isActive, targetKcal = p.targetKcal,
                        targetProtein = p.targetProtein, targetCarbs = p.targetCarbs, targetFat = p.targetFat,
                        createdAt = p.createdAt, source = p.source)
                )
                if (id > 0) planIds[p.localId] = id
            }
            val planDayIds = mutableMapOf<Pair<Long, Int>, Long>()
            snapshot.planDays.forEach { pd ->
                val planId = planIds[pd.planLocalId] ?: return@forEach
                val id = db.planDayDao().insert(PlanDayEntity(planId = planId, dayIndex = pd.dayIndex))
                if (id > 0) planDayIds[planId to pd.dayIndex] = id
            }
            snapshot.mealItems.forEach { mi ->
                val planId = planIds[mi.planLocalId] ?: return@forEach
                val dayId = planDayIds[planId to mi.dayIndex] ?: return@forEach
                val foodId = foodIds[mi.foodName] ?: return@forEach
                db.mealItemDao().insert(
                    MealItemEntity(planDayId = dayId, mealSlot = mi.mealSlot, foodId = foodId, grams = mi.grams,
                        alternativeGroup = null, preparation = mi.preparation, timeLabel = mi.timeLabel)
                )
            }
            snapshot.diaryEntries.forEach { d ->
                val foodId = foodIds[d.foodName] ?: return@forEach
                db.diaryDao().insert(
                    DiaryEntryEntity(dateEpochDay = d.dateEpochDay, mealSlot = d.mealSlot, foodId = foodId,
                        grams = d.grams, consumed = d.consumed, planItemId = null, loggedAt = d.loggedAt)
                )
            }
            snapshot.shoppingItems.forEach { s ->
                db.shoppingDao().insert(
                    ShoppingItemEntity(name = s.name, quantityGrams = s.quantityGrams, category = s.category,
                        estimatedPrice = s.estimatedPrice, checked = s.checked, weekStartEpochDay = s.weekStartEpochDay)
                )
            }
            snapshot.coachProposals.forEach { c ->
                db.coachDao().insert(
                    CoachProposalEntity(createdAt = c.createdAt, changeType = c.changeType, rationale = c.rationale,
                        previousValue = c.previousValue, newValue = c.newValue, origin = c.origin,
                        status = c.status, appliedAt = c.appliedAt, appliedTo = c.appliedTo)
                )
            }
        }
        snapshot.let {
            it.measurements.size + it.sessions.size + it.diaryEntries.size + it.exercises.size
        }
    }

    private suspend fun clearAll() {
        db.coachDao().clear()
        db.shoppingDao().clear()
        db.diaryDao().clear()
        db.mealItemDao().clear()
        db.planDayDao().clear()
        db.mealPlanDao().clear()
        db.loggedSetDao().clear()
        db.sessionDao().clear()
        db.routineExerciseDao().clear()
        db.routineDao().clear()
        db.foodDao().clear()
        db.exerciseDao().clear()
        db.photoDao().clear()
        db.measurementDao().clear()
    }
}
