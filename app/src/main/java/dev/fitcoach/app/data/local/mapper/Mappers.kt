package dev.fitcoach.app.data.local.mapper

import dev.fitcoach.app.data.local.entity.AiConfigEntity
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
import dev.fitcoach.app.domain.model.AiConfig
import dev.fitcoach.app.domain.model.AiProvider
import dev.fitcoach.app.domain.model.CoachChangeStatus
import dev.fitcoach.app.domain.model.CoachChangeType
import dev.fitcoach.app.domain.model.CoachProposal
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import dev.fitcoach.app.domain.model.Goal
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

fun PhotoEntity.toDomain() = PhotoEntry(id, dateEpochDay, PhotoPose.valueOf(pose), customLabel, filePath, notes, createdAt)
fun PhotoEntry.toEntity() = PhotoEntity(id, dateEpochDay, pose.name, customLabel, filePath, notes, createdAt)

fun ExerciseEntity.toDomain() = Exercise(
    id, name, MuscleGroup.valueOf(primaryMuscle),
    secondaryMuscles.split(",").filter { it.isNotBlank() }.map { MuscleGroup.valueOf(it) },
    Equipment.valueOf(equipment), MovementPattern.valueOf(pattern), execution, setup,
    commonMistakes, isCustom, isFavorite
)
fun Exercise.toEntity() = ExerciseEntity(
    id, name, primaryMuscle.name, secondaryMuscles.joinToString(",") { it.name },
    equipment.name, pattern.name, execution, setup, commonMistakes, isCustom, isFavorite
)

fun RoutineEntity.toDomain() = Routine(
    id, name, description, isActive, createdAt, programId, dayIndex, dayLabel
)
fun Routine.toEntity() = RoutineEntity(
    id, name, description, isActive, createdAt, programId, dayIndex, dayLabel
)

fun RoutineExerciseEntity.toDomain() = RoutineExercise(
    id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRir, restSeconds, notes
)
fun RoutineExercise.toEntity() = RoutineExerciseEntity(
    id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRir, restSeconds, notes
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
    id, name, brand, category, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g,
    fiberPer100g, FoodSource.valueOf(source), barcode, FoodState.valueOf(state), pricePerKg, isCustom
)
fun Food.toEntity() = FoodEntity(
    id, name, brand, category, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g,
    fiberPer100g, source.name, barcode, state.name, pricePerKg, isCustom
)

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
