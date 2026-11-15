package com.fitcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.CookingTimeAvailable
import com.fitcoach.app.domain.model.DiarySource
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MovementPattern
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.ShoppingCategory
import com.fitcoach.app.domain.model.SourceKind
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.TechnicalLevel
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.TrainingStatus
import com.fitcoach.app.domain.model.TransportMode
import com.fitcoach.app.domain.model.WorkShift
import com.fitcoach.app.domain.model.WorkType

// -------------------- PROFILO --------------------

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "",
    val ageYears: Int? = null,
    val sex: Sex = Sex.UNSPECIFIED,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val experience: TrainingExperience = TrainingExperience.BEGINNER,
    val trainingStatus: TrainingStatus? = null,
    val goal: Goal = Goal.RECOMP,
    val trainingDaysPerWeek: Int = 3,
    val availableEquipment: Set<Equipment> = emptySet(),
    val dietaryPreferences: Set<DietaryPreference> = emptySet(),
    val excludedFoods: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val physicalLimitations: List<String> = emptyList(),
    val movementsToAvoid: List<String> = emptyList(),
    val weeklyFoodBudgetEur: Double? = null,
    val activityFactorOverride: Double? = null,
    val calorieTargetOverride: Double? = null,
    val mealsPerDay: Int = 5,

    // Stile di vita
    val workType: WorkType = WorkType.SEDENTARY,
    val workShift: WorkShift = WorkShift.NONE,
    val workHoursPerDay: Double? = null,
    val seatedHoursPerDay: Double? = null,
    val dailySteps: Int? = null,
    val transportMode: TransportMode = TransportMode.CAR,
    val extraSportSessionsPerWeek: Int = 0,
    val walkingMinutesPerWeek: Int = 0,
    val maxSessionMinutes: Int? = null,
    val maxMealPrepMinutes: Int? = null,

    // Recupero e abitudini
    val sleepHours: Double? = null,
    val sleepQuality: Level3 = Level3.MEDIUM,
    val bedtime: String = "",
    val wakeTime: String = "",
    val stressLevel: Level3 = Level3.MEDIUM,
    val dailyEnergy: Level3 = Level3.MEDIUM,
    val recoveryCapacity: Level3 = Level3.MEDIUM,
    val hydrationLitersPerDay: Double? = null,
    val caffeineCupsPerDay: Int? = null,
    val alcoholUnitsPerWeek: Int? = null,

    // Alimentazione e preferenze
    val breakfastPreference: BreakfastPreference = BreakfastPreference.SWEET,
    val likedFoods: List<String> = emptyList(),
    val dislikedFoods: List<String> = emptyList(),
    val dietaryRestrictions: List<String> = emptyList(),
    val cookingTimeAvailable: CookingTimeAvailable = CookingTimeAvailable.MEDIUM,
    val mealPrepPossible: Boolean = false,
    val prefersSimpleFoods: Boolean = true,
    val dietDifficulties: String = "",

    // Esperienza di allenamento (dettagli)
    val trainingYears: Double? = null,
    val trainingFrequencyPerWeek: Int? = null,
    val preferredExercises: List<String> = emptyList(),
    val dislikedExercises: List<String> = emptyList(),
    val unknownExercises: List<String> = emptyList(),
    val technicalLevel: TechnicalLevel = TechnicalLevel.NOVICE,
    val priorityMuscles: Set<MuscleGroup> = emptySet(),
    val currentLoadsJson: String = "",

    // Obiettivi estesi
    val secondaryGoals: Set<Goal> = emptySet(),
    val desiredOutcome: String = "",
    val targetTimelineWeeks: Int? = null,

    // Budget mensile
    val monthlyFoodBudgetEur: Double? = null,
    val budgetScope: BudgetScope = BudgetScope.PERSONAL_ONLY,
    val budgetPersons: Int = 1,

    // Anamnesi (gate di generazione)
    val anamnesisCompleted: Boolean = false,
    val anamnesisConfirmedAt: Long? = null,
    val anamnesisSummary: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val profileId: Long = 1L,
    val mealScheduleJson: String = "",
    val workoutTime: String = "18:00",
    val hydrationRemindersEnabled: Boolean = true,
    val hydrationIntervalHours: Int = 3,
    val hydrationStartTime: String = "08:00",
    val hydrationEndTime: String = "22:00",
    val weeklyCheckInEnabled: Boolean = true,
    val weeklyCheckInDay: Int = 1,
    val weeklyCheckInTime: String = "09:00",
    val weightReminderEnabled: Boolean = true,
    val weightReminderDay: Int = 1,
    val weightReminderTime: String = "07:00",
    val notificationsEnabled: Boolean = true,
    val deferMinutes: Int = 15,
    val coachAuthorizationMode: String = "CONFIRMATION",
    val locale: String = "it",
    val unitsMetric: Boolean = true
)

// -------------------- NUTRIZIONE --------------------

@Entity(
    tableName = "food_database",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["normalizedName"]),
        Index(value = ["sourceId"])
    ]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double = 0.0,
    val defaultUnit: FoodUnit = FoodUnit.RAW,
    val source: String,
    val isCustom: Boolean = false,
    val tags: List<String> = emptyList(),
    val substituteNames: List<String> = emptyList(),
    /** ID nella fonte esterna (codice OFF, fdcId USDA). Null per alimenti seed/creati a mano. */
    val sourceId: String? = null,
    /** Nome normalizzato per il riconoscimento duplicati. */
    val normalizedName: String = "",
    /** Marca commerciale (prodotti Open Food Facts). */
    val brand: String? = null,
    /** Prezzo indicativo EUR/kg. Null = non noto (il costo non viene inventato). */
    val pricePerKgEur: Double? = null,
    /** Timestamp di acquisizione (da fonte esterna) in millis. 0 per seed. */
    val acquiredAt: Long = 0,
    /** Timestamp di ultimo aggiornamento in millis. 0 per seed. */
    val updatedAt: Long = 0
)

@Entity(tableName = "nutrition_plans")
data class NutritionPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val bmr: Double,
    val tdee: Double,
    val weeklyCalories: Double,
    val weeklyKcalBalance: Double,
    val bmrFormula: String,
    val tdeeFormula: String,
    val highCarbDays: Int,
    val mediumCarbDays: Int,
    val lowCarbDays: Int,
    val proteinGPerKg: Double,
    val carbCyclingApplied: Boolean = true,
    val version: Int = 1
)

@Entity(
    tableName = "nutrition_days",
    foreignKeys = [ForeignKey(
        entity = NutritionPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("planId")]
)
data class NutritionDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayIndex: Int,
    val dayType: CarbDayType,
    val targetKcal: Double,
    val targetProtein: Double,
    val targetCarbs: Double,
    val targetFat: Double,
    val workoutDayIndex: Int? = null
)

@Entity(
    tableName = "meals",
    foreignKeys = [ForeignKey(
        entity = NutritionDayEntity::class,
        parentColumns = ["id"],
        childColumns = ["nutritionDayId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("nutritionDayId")]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nutritionDayId: Long,
    val slot: MealSlot,
    val time: String,
    val confirmed: Boolean = false
)

@Entity(
    tableName = "meal_entries",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mealId"), Index("foodItemId")]
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodItemId: Long,
    val foodName: String,
    val grams: Double,
    val unit: FoodUnit,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

// -------------------- ALLENAMENTO --------------------

@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val category: ExerciseCategory,
    val equipment: Set<Equipment> = emptySet(),
    val technique: String,
    val defaultSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int,
    val targetRir: Int,
    val isUnilateral: Boolean = false,
    val substituteNames: List<String> = emptyList(),
    val bodyweightOnly: Boolean = false,
    val movementPattern: MovementPattern = MovementPattern.OTHER,
    val difficulty: Int = 1
)

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val name: String,
    val splitType: SplitType,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val version: Int = 1
)

@Entity(
    tableName = "mesocycles",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("planId")]
)
data class MesocycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val focus: String,
    val weekCount: Int,
    val startWeekIndex: Int = 0,
    val deloadWeekIndex: Int? = null,
    val isDeload: Boolean = false,
    val progressionModel: String = "Double Progression"
)

@Entity(
    tableName = "workout_days",
    foreignKeys = [ForeignKey(
        entity = MesocycleEntity::class,
        parentColumns = ["id"],
        childColumns = ["mesocycleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mesocycleId")]
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mesocycleId: Long,
    val dayIndex: Int,
    val title: String,
    val muscleGroups: Set<MuscleGroup> = emptySet(),
    val isRestDay: Boolean = false
)

@Entity(
    tableName = "planned_exercises",
    foreignKeys = [ForeignKey(
        entity = WorkoutDayEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutDayId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutDayId"), Index("exerciseId")]
)
data class PlannedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutDayId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val orderIndex: Int,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int,
    val targetRir: Int,
    val targetWeightKg: Double? = null,
    val targetReps: Int? = null,
    val notes: String = "",
    val technique: String = "",
    val category: ExerciseCategory = ExerciseCategory.COMPOUND,
    val selectionReason: String = "",
    val restReason: String = "",
    val intensityTechnique: IntensityTechnique = IntensityTechnique.NONE,
    val techniqueReason: String = "",
    val movementPattern: MovementPattern = MovementPattern.OTHER
)


@Entity(tableName = "free_session_exercises", indices = [Index("sessionId")])
data class FreeSessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val orderIndex: Int,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int,
    val targetRir: Int,
    val technique: String = "",
    val category: ExerciseCategory = ExerciseCategory.COMPOUND,
    val movementPattern: MovementPattern = MovementPattern.OTHER
)

@Entity(tableName = "workout_sessions", indices = [Index("dateEpochDay"), Index("workoutDayId")])
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long = 0,
    val workoutDayId: Long? = null,
    val dayTitle: String,
    val dateEpochDay: Long,
    val status: SessionStatus,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val durationMinutes: Int? = null,
    val perceivedEffort: Double? = null,
    val sleepQuality: Int? = null,
    val painReported: Boolean = false,
    val painNotes: String = ""
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setIndex: Int,
    val plannedReps: Int,
    val actualReps: Int,
    val weightKg: Double,
    val rir: Int? = null,
    val rpe: Double? = null,
    val completed: Boolean = true,
    val isWarmup: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "exercise_performance", indices = [Index("exerciseId"), Index("dateEpochDay")])
data class ExercisePerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val sessionId: Long,
    val dateEpochDay: Long,
    val topWeightKg: Double,
    val topReps: Int,
    val totalSets: Int,
    val totalReps: Int,
    val volumeKg: Double,
    val avgRir: Double? = null,
    val estimated1Rm: Double? = null
)

// -------------------- PROGRESSI --------------------

@Entity(tableName = "weight_records", indices = [Index("dateEpochDay")])
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val dateEpochDay: Long,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val note: String = ""
)

@Entity(tableName = "circumference_records", indices = [Index("dateEpochDay")])
data class CircumferenceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val dateEpochDay: Long,
    val site: String,
    val valueCm: Double
)

@Entity(tableName = "progress_records", indices = [Index("dateEpochDay")])
data class ProgressRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val dateEpochDay: Long,
    val caloriesConsumed: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val dietAdherencePercent: Double? = null,
    val trainingVolumeKg: Double? = null,
    val note: String = ""
)

@Entity(tableName = "check_ins", indices = [Index("weekStartEpochDay")])
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val weekStartEpochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val avgWeightKg: Double? = null,
    val weightTrendKgPerWeek: Double? = null,
    val avgCalories: Double? = null,
    val dietAdherencePercent: Double? = null,
    val trainingSessionsCompleted: Int = 0,
    val trainingSessionsPlanned: Int = 0,
    val performanceTrend: String = "",
    val sleepQualityAvg: Double? = null,
    val painNotes: String = "",
    val summary: String = ""
)

// -------------------- MODIFICHE PROGRAMMA --------------------

@Entity(tableName = "program_changes", indices = [Index("status"), Index("createdAt")])
data class ProgramChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val domain: ChangeDomain,
    val status: ChangeStatus,
    val title: String,
    val situation: String,
    val problem: String,
    val dataUsed: String,
    val proposedChange: String,
    val rationale: String,
    val expectedOutcome: String,
    val contraindications: String,
    val payloadJson: String = "",
    val revertPayloadJson: String = "",
    val appliedAt: Long? = null,
    val outcomeStatus: String = "PENDING",
    val evaluatedAt: Long? = null,
    val actualOutcome: String = "",
    val followUpAction: String = "",
    val source: String = "ADAPTIVE_ENGINE"
)

// -------------------- CHAT / AI --------------------

@Entity(tableName = "chat_messages", indices = [Index("createdAt")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val role: ChatRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pendingChangeId: Long? = null,
    val isError: Boolean = false
)

@Entity(tableName = "ai_provider_settings")
data class AiProviderSettingsEntity(
    @PrimaryKey val id: Long = 1L,
    val provider: AiProvider = AiProvider.OPENAI,
    val baseUrl: String = AiProvider.OPENAI.defaultBaseUrl,
    val model: String = AiProvider.OPENAI.defaultModel,
    val useBackendProxy: Boolean = false,
    val backendBaseUrl: String = "",
    val temperature: Double = 0.4,
    val maxTokens: Int = 1200,
    val lastTestOk: Boolean? = null,
    val lastTestMessage: String = "",
    val lastTestAt: Long? = null
)

// -------------------- DIARIO NUTRIZIONALE / SPESA / SOSTITUZIONI --------------------

@Entity(
    tableName = "diary_entries",
    indices = [
        Index("dateEpochDay"),
        Index("planEntryId"),
        Index("mealSlot"),
        Index(value = ["dateEpochDay", "planEntryId"], unique = true)
    ]
)
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val mealSlot: MealSlot,
    val time: String = "",
    val foodItemId: Long,
    val foodName: String,
    val grams: Double,
    val unit: FoodUnit = FoodUnit.RAW,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val source: DiarySource = DiarySource.EXTRA,
    val planEntryId: Long? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_items",
    indices = [Index("weekStartEpochDay"), Index("foodItemId")]
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartEpochDay: Long,
    val foodItemId: Long,
    val foodName: String,
    val category: ShoppingCategory,
    val totalGrams: Double,
    val unit: FoodUnit = FoodUnit.RAW,
    val checked: Boolean = false,
    val alreadyHave: Boolean = false,
    val purchased: Boolean = false,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "substitution_log", indices = [Index("createdAt"), Index("dateEpochDay")])
data class SubstitutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val dateEpochDay: Long,
    val mealEntryId: Long,
    val mealSlot: MealSlot,
    val originalFoodId: Long,
    val originalName: String,
    val originalGrams: Double,
    val newFoodId: Long,
    val newName: String,
    val newGrams: Double,
    val kcalBefore: Double,
    val kcalAfter: Double,
    val proteinBefore: Double,
    val proteinAfter: Double,
    val carbsBefore: Double,
    val carbsAfter: Double,
    val fatBefore: Double,
    val fatAfter: Double,
    val status: String,
    val note: String = ""
)

// -------------------- NOTIFICHE --------------------

@Entity(tableName = "notification_records")
data class NotificationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val kind: String,
    val title: String,
    val body: String,
    val triggerTime: String,
    val enabled: Boolean = true,
    val requestCode: Int = 0
)

// -------------------- FONTI SCIENTIFICHE --------------------

/**
 * Base di conoscenza delle fonti. Ogni voce distingue il tipo di evidenza e
 * la regola derivata applicata dai motori. Nessuna citazione inventata: le voci
 * derivano da linee guida ufficiali, revisioni sistematiche, studi o articoli
 * tecnico-divulgativi, con indicazione dei limiti di applicabilita.
 */
@Entity(tableName = "scientific_sources", indices = [Index(value = ["url"], unique = true)])
data class ScientificSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: SourceKind,
    val title: String,
    val authors: String = "",
    val organization: String = "",
    val url: String,
    val publishedDate: String = "",
    val lastUpdated: String = "",
    val topic: String,
    val evidence: String,
    val derivedRule: String,
    val applicabilityLimits: String = ""
)

// -------------------- STORICO VERSIONI PIANI --------------------

/** Snapshot immutabile di una versione di piano (nutrizione o allenamento). */
@Entity(tableName = "plan_versions", indices = [Index("createdAt"), Index("domain")])
data class PlanVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val domain: ChangeDomain,
    val planId: Long,
    val version: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val label: String,
    val reason: String,
    val snapshotJson: String,
    val isActive: Boolean = false
)

// -------------------- CALENDARIO E FOTO PROGRESSI --------------------

@Entity(tableName = "workout_schedule_overrides", indices = [Index("weekStartEpochDay"), Index("planId")])
data class WorkoutScheduleOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val planId: Long,
    val workoutDayId: Long,
    val weekStartEpochDay: Long,
    val targetDayIndex: Int,
    val permanent: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_photos", indices = [Index("capturedAt")])
data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val uri: String,
    val capturedAt: Long = System.currentTimeMillis(),
    val pose: String = "",
    val note: String = "",
    val consentForAi: Boolean = false
)
