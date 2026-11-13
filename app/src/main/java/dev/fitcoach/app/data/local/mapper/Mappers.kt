package dev.fitcoach.app.data.local.mapper

import dev.fitcoach.app.data.local.entity.AiConfigEntity
import dev.fitcoach.app.data.local.entity.CoachMessageEntity
import dev.fitcoach.app.data.local.entity.CoachProposalEntity
import dev.fitcoach.app.data.local.entity.DiaryEntryEntity
import dev.fitcoach.app.data.local.entity.ExerciseEntity
import dev.fitcoach.app.data.local.entity.FoodEntity
import dev.fitcoach.app.data.local.entity.KnowledgeNoteEntity
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
import dev.fitcoach.app.domain.model.AiConfig
import dev.fitcoach.app.domain.model.AiProvider
import dev.fitcoach.app.domain.model.ChatRole
import dev.fitcoach.app.domain.model.CoachChangeStatus
import dev.fitcoach.app.domain.model.CoachChangeType
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.model.CoachProposal
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.KnowledgeNote
import dev.fitcoach.app.domain.model.KnowledgeTopic
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.Measurement
import dev.fitcoach.app.domain.model.MeasurementType
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.PhotoEntry
import dev.fitcoach.app.domain.model.PhotoPose
import dev.fitcoach.app.domain.model.PlanDay
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.ShoppingItem
import dev.fitcoach.app.domain.model.UnitSystem
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.model.WorkoutSession

fun ProfileEntity.toDomain() = UserProfile(
    id = id, name = name, age = age, sex = Sex.valueOf(sex), heightCm = heightCm,
    weightKg = weightKg, bodyFatPercent = bodyFatPercent, goal = Goal.valueOf(goal),
    experience = ExperienceLevel.valueOf(experience), trainingYears = trainingYears,
    consistencyWeeks = consistencyWeeks, knownLoads = knownLoads, jobActivity = jobActivity,
    dailySteps = dailySteps, sleepHours = sleepHours, stressLevel = stressLevel,
    recoveryLevel = recoveryLevel, injuries = injuries, equipment = equipment,
    availableDays = availableDays, sessionMinutes = sessionMinutes, dietPreference = dietPreference,
    allergies = allergies, mealsPerDay = mealsPerDay, mealTimes = mealTimes,
    weeklyBudget = weeklyBudget, cookingMinutes = cookingMinutes, likedFoods = likedFoods,
    dislikedFoods = dislikedFoods, unitSystem = UnitSystem.valueOf(unitSystem),
    onboardingCompleted = onboardingCompleted, updatedAt = updatedAt
)

fun UserProfile.toEntity() = ProfileEntity(
    id = id, name = name, age = age, sex = sex.name, heightCm = heightCm, weightKg = weightKg,
    bodyFatPercent = bodyFatPercent, goal = goal.name, experience = experience.name,
    trainingYears = trainingYears, consistencyWeeks = consistencyWeeks, knownLoads = knownLoads,
    jobActivity = jobActivity, dailySteps = dailySteps, sleepHours = sleepHours,
    stressLevel = stressLevel, recoveryLevel = recoveryLevel, injuries = injuries,
    equipment = equipment, availableDays = availableDays, sessionMinutes = sessionMinutes,
    dietPreference = dietPreference, allergies = allergies, mealsPerDay = mealsPerDay,
    mealTimes = mealTimes, weeklyBudget = weeklyBudget, cookingMinutes = cookingMinutes,
    likedFoods = likedFoods, dislikedFoods = dislikedFoods, unitSystem = unitSystem.name,
    onboardingCompleted = onboardingCompleted, updatedAt = updatedAt
)

fun MeasurementEntity.toDomain() = Measurement(
    id, MeasurementType.valueOf(type), customLabel, dateEpochDay, value, notes, createdAt
)
fun Measurement.toEntity() = MeasurementEntity(
    id, type.name, customLabel, dateEpochDay, value, notes, createdAt
)

fun PhotoEntity.toDomain() = PhotoEntry(
    id, dateEpochDay, PhotoPose.valueOf(pose), customLabel, filePath, notes, createdAt, analysis, analysisAt
)
fun PhotoEntry.toEntity() = PhotoEntity(
    id, dateEpochDay, pose.name, customLabel, filePath, notes, createdAt, analysis, analysisAt
)

/**
 * Tolerant enum lookup. A stored value written by an older build (or renamed later) must
 * never take down a whole list, which is what plain valueOf() did: one bad row threw and
 * every screen reading the catalog stayed empty with no message.
 */
private inline fun <reified T : Enum<T>> enumOrNull(raw: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }

/** Null when the stored primary muscle is unknown, so the caller can skip just that row. */
fun ExerciseEntity.toDomainOrNull(): Exercise? {
    val primary = enumOrNull<MuscleGroup>(primaryMuscle) ?: return null
    return Exercise(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondaryMuscles.split(",").mapNotNull { enumOrNull<MuscleGroup>(it) },
        equipment = enumOrNull<Equipment>(equipment) ?: Equipment.OTHER,
        pattern = enumOrNull<MovementPattern>(pattern) ?: MovementPattern.HORIZONTAL_PUSH,
        execution = execution,
        setup = setup,
        commonMistakes = commonMistakes,
        isCustom = isCustom,
        isFavorite = isFavorite,
        defaultRestSeconds = defaultRestSeconds
    )
}

/** Never throws: unknown optional fields fall back, an unknown primary muscle is kept as CORE. */
fun ExerciseEntity.toDomain(): Exercise = toDomainOrNull() ?: Exercise(
    id = id,
    name = name,
    primaryMuscle = MuscleGroup.CORE,
    equipment = Equipment.OTHER,
    pattern = MovementPattern.HORIZONTAL_PUSH,
    execution = execution,
    setup = setup,
    commonMistakes = commonMistakes,
    isCustom = isCustom,
    isFavorite = isFavorite,
    defaultRestSeconds = defaultRestSeconds
)
fun Exercise.toEntity() = ExerciseEntity(
    id, name, primaryMuscle.name, secondaryMuscles.joinToString(",") { it.name },
    equipment.name, pattern.name, execution, setup, commonMistakes, isCustom, isFavorite,
    defaultRestSeconds
)

fun RoutineEntity.toDomain() = Routine(
    id, name, description, isActive, createdAt, programId, dayIndex, dayLabel
)
fun Routine.toEntity() = RoutineEntity(
    id, name, description, isActive, createdAt, programId, dayIndex, dayLabel
)

fun RoutineExerciseEntity.toDomain() = RoutineExercise(
    id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRir, restSeconds,
    notes, targetWeightKg
)
fun RoutineExercise.toEntity() = RoutineExerciseEntity(
    id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRir, restSeconds,
    notes, targetWeightKg
)

fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id, routineId, name, dateEpochDay, startedAt, finishedAt, SessionStatus.valueOf(status), notes
)
fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id, routineId, name, dateEpochDay, startedAt, finishedAt, status.name, notes
)

fun LoggedSetEntity.toDomain() = LoggedSet(
    id, sessionId, exerciseId, orderIndex, setIndex, SetType.valueOf(setType), weightKg, reps, rir, rpe,
    restSeconds, pain, completed, notes, loggedAt
)
fun LoggedSet.toEntity() = LoggedSetEntity(
    id, sessionId, exerciseId, orderIndex, setIndex, setType.name, weightKg, reps, rir, rpe,
    restSeconds, pain, completed, notes, loggedAt
)

fun FoodEntity.toDomain() = Food(
    id = id,
    name = name,
    brand = brand,
    category = category,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    source = FoodSource.valueOf(source),
    barcode = barcode,
    state = FoodState.valueOf(state),
    pricePerKg = pricePerKg,
    isCustom = isCustom,
    provenance = dev.fitcoach.app.domain.model.FoodProvenance(
        sourceId = sourceId,
        acquiredAt = acquiredAt,
        sourceUpdatedAt = sourceUpdatedAt,
        reliability = reliability,
        verification = verification.asVerificationStatus(),
        limitations = limitations
    )
)

fun Food.toEntity() = FoodEntity(
    id = id,
    name = name,
    brand = brand,
    category = category,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    source = source.name,
    barcode = barcode,
    state = state.name,
    pricePerKg = pricePerKg,
    isCustom = isCustom,
    sourceId = provenance.sourceId,
    acquiredAt = provenance.acquiredAt,
    sourceUpdatedAt = provenance.sourceUpdatedAt,
    reliability = provenance.reliability,
    verification = provenance.verification.name,
    limitations = provenance.limitations
)

/** Unknown statuses from older rows degrade to the honest default, never to "verified". */
private fun String.asVerificationStatus(): dev.fitcoach.app.domain.model.VerificationStatus =
    dev.fitcoach.app.domain.model.VerificationStatus.entries.firstOrNull { it.name == this }
        ?: dev.fitcoach.app.domain.model.VerificationStatus.ESTIMATED

fun MealPlanEntity.toDomain() = MealPlan(
    id, name, isActive, targetKcal, targetProtein, targetCarbs, targetFat, createdAt, FoodSource.valueOf(source)
)
fun MealPlan.toEntity() = MealPlanEntity(
    id, name, isActive, targetKcal, targetProtein, targetCarbs, targetFat, createdAt, source.name
)

fun PlanDayEntity.toDomain() = PlanDay(id, planId, dayIndex)
fun PlanDay.toEntity() = PlanDayEntity(id, planId, dayIndex)

fun MealItemEntity.toDomain() = MealItem(id, planDayId, MealSlot.valueOf(mealSlot), foodId, grams, alternativeGroup, preparation, timeLabel)
fun MealItem.toEntity() = MealItemEntity(id, planDayId, mealSlot.name, foodId, grams, alternativeGroup, preparation, timeLabel)

fun DiaryEntryEntity.toDomain() = DiaryEntry(id, dateEpochDay, MealSlot.valueOf(mealSlot), foodId, grams, consumed, planItemId, loggedAt)
fun DiaryEntry.toEntity() = DiaryEntryEntity(id, dateEpochDay, mealSlot.name, foodId, grams, consumed, planItemId, loggedAt)

fun ShoppingItemEntity.toDomain() = ShoppingItem(id, name, quantityGrams, category, estimatedPrice, checked, weekStartEpochDay)
fun ShoppingItem.toEntity() = ShoppingItemEntity(id, name, quantityGrams, category, estimatedPrice, checked, weekStartEpochDay)

fun CoachProposalEntity.toDomain() = CoachProposal(
    id, createdAt, CoachChangeType.valueOf(changeType), rationale, previousValue, newValue,
    origin, CoachChangeStatus.valueOf(status), appliedAt, appliedTo
)

fun CoachProposal.toEntity() = CoachProposalEntity(
    id, createdAt, changeType.name, rationale, previousValue, newValue, origin, status.name, appliedAt, appliedTo
)

fun AiConfigEntity.toDomain() = AiConfig(
    AiProvider.valueOf(provider), baseUrl, modelId, maxOutputTokens, timeoutSeconds,
    temperature, topP, useJsonMode, sendPhotosToAi
)
fun AiConfig.toEntity() = AiConfigEntity(
    1L, provider.name, baseUrl, modelId, maxOutputTokens, timeoutSeconds, temperature, topP, useJsonMode, sendPhotosToAi
)

fun CoachMessageEntity.toDomain() = CoachChatMessage(
    id, ChatRole.fromWire(role), content, createdAt, actionSummary
)
fun CoachChatMessage.toEntity() = CoachMessageEntity(
    id, role.wire, content, createdAt, actionSummary
)

fun KnowledgeNoteEntity.toDomain() = KnowledgeNote(
    id,
    KnowledgeTopic.entries.firstOrNull { it.name == topic } ?: KnowledgeTopic.OTHER,
    title, content, source, builtIn, enabled, createdAt
)
fun KnowledgeNote.toEntity() = KnowledgeNoteEntity(
    id, topic.name, title, content, source, builtIn, enabled, createdAt
)
