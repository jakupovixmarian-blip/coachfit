package com.fitcoach.app.data.local

import androidx.room.TypeConverter
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

/** Convertitori Room per enum e collezioni. I set/list sono salvati come CSV. */
class Converters {

    @TypeConverter fun sexToString(v: Sex): String = v.name
    // I convertitori tolleranti evitano che un singolo valore enum non piu
    // valido (rinominato tra versioni) faccia fallire la lettura dell'intera riga
    // e quindi la generazione del piano.
    @TypeConverter fun stringToSex(v: String): Sex =
        runCatching { Sex.valueOf(v) }.getOrDefault(Sex.entries.first())

    @TypeConverter fun goalToString(v: Goal): String = v.name
    @TypeConverter fun stringToGoal(v: String): Goal =
        runCatching { Goal.valueOf(v) }.getOrDefault(Goal.RECOMP)

    @TypeConverter fun experienceToString(v: TrainingExperience): String = v.name
    @TypeConverter fun stringToExperience(v: String): TrainingExperience =
        runCatching { TrainingExperience.valueOf(v) }.getOrDefault(TrainingExperience.BEGINNER)

    @TypeConverter fun trainingStatusToString(v: TrainingStatus?): String? = v?.name
    @TypeConverter fun stringToTrainingStatus(v: String?): TrainingStatus? =
        v?.let { runCatching { TrainingStatus.valueOf(it) }.getOrNull() }

    @TypeConverter fun equipmentSetToString(v: Set<Equipment>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToEquipmentSet(v: String): Set<Equipment> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { Equipment.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun prefsSetToString(v: Set<DietaryPreference>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToPrefsSet(v: String): Set<DietaryPreference> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { DietaryPreference.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun stringListToString(v: List<String>): String = v.joinToString("\u001F")
    @TypeConverter fun stringToStringList(v: String): List<String> =
        if (v.isBlank()) emptyList() else v.split("\u001F")

    @TypeConverter fun stringSetToString(v: Set<String>): String = v.joinToString("\u001F")
    @TypeConverter fun stringToStringSet(v: String): Set<String> =
        if (v.isBlank()) emptySet() else v.split("\u001F").filter { it.isNotBlank() }.toSet()

    @TypeConverter fun muscleSetToString(v: Set<MuscleGroup>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToMuscleSet(v: String): Set<MuscleGroup> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { MuscleGroup.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun goalSetToString(v: Set<Goal>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToGoalSet(v: String): Set<Goal> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { Goal.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun muscleToString(v: MuscleGroup): String = v.name
    @TypeConverter fun stringToMuscle(v: String): MuscleGroup =
        runCatching { MuscleGroup.valueOf(v) }.getOrDefault(MuscleGroup.entries.first())

    @TypeConverter fun carbDayToString(v: CarbDayType): String = v.name
    @TypeConverter fun stringToCarbDay(v: String): CarbDayType =
        runCatching { CarbDayType.valueOf(v) }.getOrDefault(CarbDayType.MEDIUM)

    @TypeConverter fun budgetScopeToString(v: BudgetScope): String = v.name
    @TypeConverter fun stringToBudgetScope(v: String): BudgetScope =
        runCatching { BudgetScope.valueOf(v) }.getOrDefault(BudgetScope.PERSONAL_ONLY)

    @TypeConverter fun mealSlotToString(v: MealSlot): String = v.name
    @TypeConverter fun stringToMealSlot(v: String): MealSlot =
        runCatching { MealSlot.valueOf(v) }.getOrDefault(MealSlot.BREAKFAST)

    @TypeConverter fun foodUnitToString(v: FoodUnit): String = v.name
    @TypeConverter fun stringToFoodUnit(v: String): FoodUnit =
        runCatching { FoodUnit.valueOf(v) }.getOrDefault(FoodUnit.RAW)

    @TypeConverter fun splitToString(v: SplitType): String = v.name
    @TypeConverter fun stringToSplit(v: String): SplitType =
        runCatching { SplitType.valueOf(v) }.getOrDefault(SplitType.FULL_BODY)

    @TypeConverter fun categoryToString(v: ExerciseCategory): String = v.name
    @TypeConverter fun stringToCategory(v: String): ExerciseCategory =
        runCatching { ExerciseCategory.valueOf(v) }.getOrDefault(ExerciseCategory.entries.first())

    @TypeConverter fun sessionStatusToString(v: SessionStatus): String = v.name
    @TypeConverter fun stringToSessionStatus(v: String): SessionStatus =
        runCatching { SessionStatus.valueOf(v) }.getOrDefault(SessionStatus.entries.first())

    @TypeConverter fun changeStatusToString(v: ChangeStatus): String = v.name
    @TypeConverter fun stringToChangeStatus(v: String): ChangeStatus =
        runCatching { ChangeStatus.valueOf(v) }.getOrDefault(ChangeStatus.entries.first())

    @TypeConverter fun changeDomainToString(v: ChangeDomain): String = v.name
    @TypeConverter fun stringToChangeDomain(v: String): ChangeDomain =
        runCatching { ChangeDomain.valueOf(v) }.getOrDefault(ChangeDomain.entries.first())

    @TypeConverter fun chatRoleToString(v: ChatRole): String = v.name
    @TypeConverter fun stringToChatRole(v: String): ChatRole =
        runCatching { ChatRole.valueOf(v) }.getOrDefault(ChatRole.USER)

    @TypeConverter fun providerToString(v: AiProvider): String = v.name
    @TypeConverter fun stringToProvider(v: String): AiProvider =
        runCatching { AiProvider.valueOf(v) }.getOrDefault(AiProvider.OPENAI)

    @TypeConverter fun workTypeToString(v: WorkType): String = v.name
    @TypeConverter fun stringToWorkType(v: String): WorkType = runCatching { WorkType.valueOf(v) }.getOrDefault(WorkType.SEDENTARY)

    @TypeConverter fun workShiftToString(v: WorkShift): String = v.name
    @TypeConverter fun stringToWorkShift(v: String): WorkShift = runCatching { WorkShift.valueOf(v) }.getOrDefault(WorkShift.NONE)

    @TypeConverter fun transportToString(v: TransportMode): String = v.name
    @TypeConverter fun stringToTransport(v: String): TransportMode = runCatching { TransportMode.valueOf(v) }.getOrDefault(TransportMode.CAR)

    @TypeConverter fun breakfastToString(v: BreakfastPreference): String = v.name
    @TypeConverter fun stringToBreakfast(v: String): BreakfastPreference = runCatching { BreakfastPreference.valueOf(v) }.getOrDefault(BreakfastPreference.SWEET)

    @TypeConverter fun level3ToString(v: Level3): String = v.name
    @TypeConverter fun stringToLevel3(v: String): Level3 = runCatching { Level3.valueOf(v) }.getOrDefault(Level3.MEDIUM)

    @TypeConverter fun cookingTimeToString(v: CookingTimeAvailable): String = v.name
    @TypeConverter fun stringToCookingTime(v: String): CookingTimeAvailable = runCatching { CookingTimeAvailable.valueOf(v) }.getOrDefault(CookingTimeAvailable.MEDIUM)

    @TypeConverter fun technicalLevelToString(v: TechnicalLevel): String = v.name
    @TypeConverter fun stringToTechnicalLevel(v: String): TechnicalLevel = runCatching { TechnicalLevel.valueOf(v) }.getOrDefault(TechnicalLevel.NOVICE)

    @TypeConverter fun sourceKindToString(v: SourceKind): String = v.name
    @TypeConverter fun stringToSourceKind(v: String): SourceKind = runCatching { SourceKind.valueOf(v) }.getOrDefault(SourceKind.DIVULGATIVE)

    @TypeConverter fun intensityTechniqueToString(v: IntensityTechnique): String = v.name
    @TypeConverter fun stringToIntensityTechnique(v: String): IntensityTechnique = runCatching { IntensityTechnique.valueOf(v) }.getOrDefault(IntensityTechnique.NONE)

    @TypeConverter fun movementPatternToString(v: MovementPattern): String = v.name
    @TypeConverter fun stringToMovementPattern(v: String): MovementPattern = runCatching { MovementPattern.valueOf(v) }.getOrDefault(MovementPattern.OTHER)

    @TypeConverter fun diarySourceToString(v: DiarySource): String = v.name
    @TypeConverter fun stringToDiarySource(v: String): DiarySource = runCatching { DiarySource.valueOf(v) }.getOrDefault(DiarySource.EXTRA)

    @TypeConverter fun shoppingCategoryToString(v: ShoppingCategory): String = v.name
    @TypeConverter fun stringToShoppingCategory(v: String): ShoppingCategory = runCatching { ShoppingCategory.valueOf(v) }.getOrDefault(ShoppingCategory.OTHER)
}
