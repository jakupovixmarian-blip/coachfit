package com.fitcoach.app.data.local

import com.fitcoach.app.data.local.entity.*
import com.fitcoach.app.domain.model.*

object Mappers {

    fun UserProfileEntity.toDomain() = UserProfile(
        id = id, name = name, ageYears = ageYears, sex = sex, heightCm = heightCm,
        weightKg = weightKg, bodyFatPercent = bodyFatPercent, experience = experience,
        trainingStatus = trainingStatus,
        goal = goal, trainingDaysPerWeek = trainingDaysPerWeek, availableEquipment = availableEquipment,
        dietaryPreferences = dietaryPreferences, excludedFoods = excludedFoods, allergies = allergies,
        physicalLimitations = physicalLimitations, movementsToAvoid = movementsToAvoid,
        weeklyFoodBudgetEur = weeklyFoodBudgetEur, activityFactorOverride = activityFactorOverride,
        calorieTargetOverride = calorieTargetOverride, mealsPerDay = mealsPerDay,
        workType = workType, workShift = workShift, workHoursPerDay = workHoursPerDay,
        seatedHoursPerDay = seatedHoursPerDay, dailySteps = dailySteps, transportMode = transportMode,
        extraSportSessionsPerWeek = extraSportSessionsPerWeek, walkingMinutesPerWeek = walkingMinutesPerWeek,
        maxSessionMinutes = maxSessionMinutes, maxMealPrepMinutes = maxMealPrepMinutes,
        sleepHours = sleepHours, sleepQuality = sleepQuality, bedtime = bedtime, wakeTime = wakeTime,
        stressLevel = stressLevel, dailyEnergy = dailyEnergy, recoveryCapacity = recoveryCapacity,
        hydrationLitersPerDay = hydrationLitersPerDay, caffeineCupsPerDay = caffeineCupsPerDay,
        alcoholUnitsPerWeek = alcoholUnitsPerWeek, breakfastPreference = breakfastPreference,
        likedFoods = likedFoods, dislikedFoods = dislikedFoods, dietaryRestrictions = dietaryRestrictions,
        cookingTimeAvailable = cookingTimeAvailable, mealPrepPossible = mealPrepPossible,
        prefersSimpleFoods = prefersSimpleFoods, dietDifficulties = dietDifficulties,
        trainingYears = trainingYears, trainingFrequencyPerWeek = trainingFrequencyPerWeek,
        preferredExercises = preferredExercises, dislikedExercises = dislikedExercises,
        unknownExercises = unknownExercises, technicalLevel = technicalLevel,
        priorityMuscles = priorityMuscles, currentLoads = decodeLoads(currentLoadsJson),
        secondaryGoals = secondaryGoals, desiredOutcome = desiredOutcome,
        targetTimelineWeeks = targetTimelineWeeks,
        monthlyFoodBudgetEur = monthlyFoodBudgetEur, budgetScope = budgetScope,
        budgetPersons = budgetPersons,
        anamnesisCompleted = anamnesisCompleted, anamnesisConfirmedAt = anamnesisConfirmedAt,
        anamnesisSummary = anamnesisSummary,
        answeredQuestionIds = answeredQuestionIds,
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun UserProfile.toEntity() = UserProfileEntity(
        id = id, name = name, ageYears = ageYears, sex = sex, heightCm = heightCm,
        weightKg = weightKg, bodyFatPercent = bodyFatPercent, experience = experience,
        trainingStatus = trainingStatus,
        goal = goal, trainingDaysPerWeek = trainingDaysPerWeek, availableEquipment = availableEquipment,
        dietaryPreferences = dietaryPreferences, excludedFoods = excludedFoods, allergies = allergies,
        physicalLimitations = physicalLimitations, movementsToAvoid = movementsToAvoid,
        weeklyFoodBudgetEur = weeklyFoodBudgetEur, activityFactorOverride = activityFactorOverride,
        calorieTargetOverride = calorieTargetOverride, mealsPerDay = mealsPerDay,
        workType = workType, workShift = workShift, workHoursPerDay = workHoursPerDay,
        seatedHoursPerDay = seatedHoursPerDay, dailySteps = dailySteps, transportMode = transportMode,
        extraSportSessionsPerWeek = extraSportSessionsPerWeek, walkingMinutesPerWeek = walkingMinutesPerWeek,
        maxSessionMinutes = maxSessionMinutes, maxMealPrepMinutes = maxMealPrepMinutes,
        sleepHours = sleepHours, sleepQuality = sleepQuality, bedtime = bedtime, wakeTime = wakeTime,
        stressLevel = stressLevel, dailyEnergy = dailyEnergy, recoveryCapacity = recoveryCapacity,
        hydrationLitersPerDay = hydrationLitersPerDay, caffeineCupsPerDay = caffeineCupsPerDay,
        alcoholUnitsPerWeek = alcoholUnitsPerWeek, breakfastPreference = breakfastPreference,
        likedFoods = likedFoods, dislikedFoods = dislikedFoods, dietaryRestrictions = dietaryRestrictions,
        cookingTimeAvailable = cookingTimeAvailable, mealPrepPossible = mealPrepPossible,
        prefersSimpleFoods = prefersSimpleFoods, dietDifficulties = dietDifficulties,
        trainingYears = trainingYears, trainingFrequencyPerWeek = trainingFrequencyPerWeek,
        preferredExercises = preferredExercises, dislikedExercises = dislikedExercises,
        unknownExercises = unknownExercises, technicalLevel = technicalLevel,
        priorityMuscles = priorityMuscles, currentLoadsJson = encodeLoads(currentLoads),
        secondaryGoals = secondaryGoals, desiredOutcome = desiredOutcome,
        targetTimelineWeeks = targetTimelineWeeks,
        monthlyFoodBudgetEur = monthlyFoodBudgetEur, budgetScope = budgetScope,
        budgetPersons = budgetPersons,
        anamnesisCompleted = anamnesisCompleted, anamnesisConfirmedAt = anamnesisConfirmedAt,
        anamnesisSummary = anamnesisSummary,
        answeredQuestionIds = answeredQuestionIds,
        createdAt = createdAt, updatedAt = System.currentTimeMillis()
    )

    /** Carichi attuali serializzati come "nomeEsercizio=kg;...". */
    fun encodeLoads(loads: Map<String, Double>): String =
        loads.entries.joinToString(";") { "${it.key}=${it.value}" }

    fun decodeLoads(raw: String): Map<String, Double> =
        if (raw.isBlank()) emptyMap()
        else raw.split(";").mapNotNull { part ->
            val idx = part.lastIndexOf('=')
            if (idx <= 0) null
            else part.substring(0, idx).trim().let { k ->
                part.substring(idx + 1).trim().toDoubleOrNull()?.let { v -> k to v }
            }
        }.toMap()

    fun FoodItemEntity.toDomain() = FoodItem(
        id = id, name = name, category = category, kcalPer100g = kcalPer100g,
        proteinPer100g = proteinPer100g, carbsPer100g = carbsPer100g, fatPer100g = fatPer100g,
        fiberPer100g = fiberPer100g, defaultUnit = defaultUnit, source = source,
        isCustom = isCustom, tags = tags, substituteNames = substituteNames,
        sourceId = sourceId, brand = brand, pricePerKgEur = pricePerKgEur
    )

    fun FoodItem.toEntity() = FoodItemEntity(
        id = id, name = name, category = category, kcalPer100g = kcalPer100g,
        proteinPer100g = proteinPer100g, carbsPer100g = carbsPer100g, fatPer100g = fatPer100g,
        fiberPer100g = fiberPer100g, defaultUnit = defaultUnit, source = source,
        isCustom = isCustom, tags = tags, substituteNames = substituteNames,
        sourceId = sourceId, normalizedName = normalizedName, brand = brand,
        pricePerKgEur = pricePerKgEur,
        acquiredAt = if (sourceId != null) System.currentTimeMillis() else 0L,
        updatedAt = System.currentTimeMillis()
    )

    fun ExerciseEntity.toDomain() = Exercise(
        id = id, name = name, primaryMuscle = primaryMuscle, secondaryMuscles = secondaryMuscles,
        category = category, equipment = equipment, technique = technique, defaultSets = defaultSets,
        minReps = minReps, maxReps = maxReps, restSeconds = restSeconds, targetRir = targetRir,
        isUnilateral = isUnilateral, substituteNames = substituteNames, bodyweightOnly = bodyweightOnly,
        movementPattern = movementPattern, difficulty = difficulty
    )

    fun Exercise.toEntity() = ExerciseEntity(
        name = name, primaryMuscle = primaryMuscle, secondaryMuscles = secondaryMuscles,
        category = category, equipment = equipment, technique = technique, defaultSets = defaultSets,
        minReps = minReps, maxReps = maxReps, restSeconds = restSeconds, targetRir = targetRir,
        isUnilateral = isUnilateral, substituteNames = substituteNames, bodyweightOnly = bodyweightOnly,
        movementPattern = movementPattern, difficulty = difficulty
    )

    fun WorkoutDayEntity.toDomain(exercises: List<PlannedExercise>) = WorkoutDay(
        id = id, mesocycleId = mesocycleId, dayIndex = dayIndex, title = title,
        muscleGroups = muscleGroups, exercises = exercises, isRestDay = isRestDay
    )

    fun PlannedExerciseEntity.toDomain() = PlannedExercise(
        id = id, workoutDayId = workoutDayId, exerciseId = exerciseId, exerciseName = exerciseName,
        primaryMuscle = primaryMuscle, orderIndex = orderIndex, sets = sets, minReps = minReps,
        maxReps = maxReps, restSeconds = restSeconds, targetRir = targetRir,
        targetWeightKg = targetWeightKg, targetReps = targetReps, notes = notes,
        technique = technique, category = category, selectionReason = selectionReason,
        restReason = restReason, intensityTechnique = intensityTechnique,
        techniqueReason = techniqueReason, movementPattern = movementPattern
    )

    fun WorkoutSetEntity.toDomain() = WorkoutSet(
        id = id, sessionId = sessionId, exerciseId = exerciseId, exerciseName = exerciseName,
        setIndex = setIndex, plannedReps = plannedReps, actualReps = actualReps, weightKg = weightKg,
        rir = rir, rpe = rpe, completed = completed, isWarmup = isWarmup, notes = notes
    )

    fun WorkoutSet.toEntity() = WorkoutSetEntity(
        id = id, sessionId = sessionId, exerciseId = exerciseId, exerciseName = exerciseName,
        setIndex = setIndex, plannedReps = plannedReps, actualReps = actualReps, weightKg = weightKg,
        rir = rir, rpe = rpe, completed = completed, isWarmup = isWarmup, notes = notes
    )

    fun WeightRecordEntity.toDomain() = WeightRecord(
        id = id, profileId = profileId, dateEpochDay = dateEpochDay, weightKg = weightKg,
        bodyFatPercent = bodyFatPercent, note = note
    )

    fun WeightRecord.toEntity() = WeightRecordEntity(
        id = id, profileId = profileId, dateEpochDay = dateEpochDay, weightKg = weightKg,
        bodyFatPercent = bodyFatPercent, note = note
    )

    fun CircumferenceRecordEntity.toDomain() = CircumferenceRecord(
        id = id, profileId = profileId, dateEpochDay = dateEpochDay, site = site, valueCm = valueCm
    )

    fun CircumferenceRecord.toEntity() = CircumferenceRecordEntity(
        id = id, profileId = profileId, dateEpochDay = dateEpochDay, site = site, valueCm = valueCm
    )

    fun ProgressRecordEntity.toDomain() = ProgressRecord(
        id = id, profileId = profileId, dateEpochDay = dateEpochDay, caloriesConsumed = caloriesConsumed,
        protein = protein, carbs = carbs, fat = fat, dietAdherencePercent = dietAdherencePercent,
        trainingVolumeKg = trainingVolumeKg, note = note
    )

    fun ProgressRecord.toEntity() = ProgressRecordEntity(
        id = id, profileId = profileId, dateEpochDay = dateEpochDay, caloriesConsumed = caloriesConsumed,
        protein = protein, carbs = carbs, fat = fat, dietAdherencePercent = dietAdherencePercent,
        trainingVolumeKg = trainingVolumeKg, note = note
    )

    fun CheckInEntity.toDomain() = CheckIn(
        id = id, profileId = profileId, weekStartEpochDay = weekStartEpochDay, createdAt = createdAt,
        avgWeightKg = avgWeightKg, weightTrendKgPerWeek = weightTrendKgPerWeek, avgCalories = avgCalories,
        dietAdherencePercent = dietAdherencePercent, trainingSessionsCompleted = trainingSessionsCompleted,
        trainingSessionsPlanned = trainingSessionsPlanned, performanceTrend = performanceTrend,
        sleepQualityAvg = sleepQualityAvg, painNotes = painNotes, summary = summary
    )

    fun CheckIn.toEntity() = CheckInEntity(
        id = id, profileId = profileId, weekStartEpochDay = weekStartEpochDay, createdAt = createdAt,
        avgWeightKg = avgWeightKg, weightTrendKgPerWeek = weightTrendKgPerWeek, avgCalories = avgCalories,
        dietAdherencePercent = dietAdherencePercent, trainingSessionsCompleted = trainingSessionsCompleted,
        trainingSessionsPlanned = trainingSessionsPlanned, performanceTrend = performanceTrend,
        sleepQualityAvg = sleepQualityAvg, painNotes = painNotes, summary = summary
    )

    fun ProgramChangeEntity.toDomain() = ProgramChange(
        id = id, profileId = profileId, createdAt = createdAt, domain = domain, status = status,
        title = title, situation = situation, problem = problem, dataUsed = dataUsed,
        proposedChange = proposedChange, rationale = rationale, expectedOutcome = expectedOutcome,
        contraindications = contraindications, payloadJson = payloadJson,
        revertPayloadJson = revertPayloadJson, appliedAt = appliedAt,
        outcomeStatus = outcomeStatus, evaluatedAt = evaluatedAt, actualOutcome = actualOutcome,
        followUpAction = followUpAction, source = source
    )

    fun ProgramChange.toEntity() = ProgramChangeEntity(
        id = id, profileId = profileId, createdAt = createdAt, domain = domain, status = status,
        title = title, situation = situation, problem = problem, dataUsed = dataUsed,
        proposedChange = proposedChange, rationale = rationale, expectedOutcome = expectedOutcome,
        contraindications = contraindications, payloadJson = payloadJson,
        revertPayloadJson = revertPayloadJson, appliedAt = appliedAt,
        outcomeStatus = outcomeStatus, evaluatedAt = evaluatedAt, actualOutcome = actualOutcome,
        followUpAction = followUpAction, source = source
    )

    fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id, profileId = profileId, role = role, content = content, createdAt = createdAt,
        pendingChangeId = pendingChangeId, isError = isError
    )

    fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id, profileId = profileId, role = role, content = content, createdAt = createdAt,
        pendingChangeId = pendingChangeId, isError = isError
    )

    fun AiProviderSettingsEntity.toDomain() = AiProviderSettings(
        id = id, provider = provider, baseUrl = baseUrl, model = model, useBackendProxy = useBackendProxy,
        backendBaseUrl = backendBaseUrl, temperature = temperature, maxTokens = maxTokens,
        lastTestOk = lastTestOk, lastTestMessage = lastTestMessage, lastTestAt = lastTestAt
    )

    fun AiProviderSettings.toEntity() = AiProviderSettingsEntity(
        id = id, provider = provider, baseUrl = baseUrl, model = model, useBackendProxy = useBackendProxy,
        backendBaseUrl = backendBaseUrl, temperature = temperature, maxTokens = maxTokens,
        lastTestOk = lastTestOk, lastTestMessage = lastTestMessage, lastTestAt = lastTestAt
    )

    fun NotificationRecordEntity.toDomain() = NotificationRecord(
        id = id, profileId = profileId, kind = kind, title = title, body = body,
        triggerTime = triggerTime, enabled = enabled, requestCode = requestCode
    )

    fun NotificationRecord.toEntity() = NotificationRecordEntity(
        id = id, profileId = profileId, kind = kind, title = title, body = body,
        triggerTime = triggerTime, enabled = enabled, requestCode = requestCode
    )

    fun DiaryEntryEntity.toDomain() = DiaryEntry(
        id = id, dateEpochDay = dateEpochDay, mealSlot = mealSlot, time = time,
        foodItemId = foodItemId, foodName = foodName, grams = grams, unit = unit,
        kcal = kcal, protein = protein, carbs = carbs, fat = fat, source = source,
        planEntryId = planEntryId, nutritionPlanId = nutritionPlanId,
        active = active, createdAt = createdAt
    )

    fun DiaryEntry.toEntity() = DiaryEntryEntity(
        id = id, dateEpochDay = dateEpochDay, mealSlot = mealSlot, time = time,
        foodItemId = foodItemId, foodName = foodName, grams = grams, unit = unit,
        kcal = kcal, protein = protein, carbs = carbs, fat = fat, source = source,
        planEntryId = planEntryId, nutritionPlanId = nutritionPlanId,
        active = active, createdAt = createdAt
    )

    fun ShoppingItemEntity.toDomain() = ShoppingItem(
        id = id, weekStartEpochDay = weekStartEpochDay, foodItemId = foodItemId,
        foodName = foodName, category = category, totalGrams = totalGrams, unit = unit,
        checked = checked, alreadyHave = alreadyHave, purchased = purchased,
        isCustom = isCustom, createdAt = createdAt
    )

    fun ShoppingItem.toEntity() = ShoppingItemEntity(
        id = id, weekStartEpochDay = weekStartEpochDay, foodItemId = foodItemId,
        foodName = foodName, category = category, totalGrams = totalGrams, unit = unit,
        checked = checked, alreadyHave = alreadyHave, purchased = purchased,
        isCustom = isCustom, createdAt = createdAt
    )

    fun SubstitutionLogEntity.toDomain() = SubstitutionLog(
        id = id, createdAt = createdAt, dateEpochDay = dateEpochDay, mealEntryId = mealEntryId,
        mealSlot = mealSlot, originalFoodId = originalFoodId, originalName = originalName,
        originalGrams = originalGrams, newFoodId = newFoodId, newName = newName, newGrams = newGrams,
        kcalBefore = kcalBefore, kcalAfter = kcalAfter, proteinBefore = proteinBefore,
        proteinAfter = proteinAfter, carbsBefore = carbsBefore, carbsAfter = carbsAfter,
        fatBefore = fatBefore, fatAfter = fatAfter, status = status, note = note
    )

    fun SubstitutionLog.toEntity() = SubstitutionLogEntity(
        id = id, createdAt = createdAt, dateEpochDay = dateEpochDay, mealEntryId = mealEntryId,
        mealSlot = mealSlot, originalFoodId = originalFoodId, originalName = originalName,
        originalGrams = originalGrams, newFoodId = newFoodId, newName = newName, newGrams = newGrams,
        kcalBefore = kcalBefore, kcalAfter = kcalAfter, proteinBefore = proteinBefore,
        proteinAfter = proteinAfter, carbsBefore = carbsBefore, carbsAfter = carbsAfter,
        fatBefore = fatBefore, fatAfter = fatAfter, status = status, note = note
    )
}
