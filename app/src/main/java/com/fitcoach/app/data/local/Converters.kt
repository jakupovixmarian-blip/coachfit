package com.fitcoach.app.data.local

import androidx.room.TypeConverter
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.TrainingExperience

/** Convertitori Room per enum e collezioni. I set/list sono salvati come CSV. */
class Converters {

    @TypeConverter fun sexToString(v: Sex): String = v.name
    @TypeConverter fun stringToSex(v: String): Sex = Sex.valueOf(v)

    @TypeConverter fun goalToString(v: Goal): String = v.name
    @TypeConverter fun stringToGoal(v: String): Goal = Goal.valueOf(v)

    @TypeConverter fun experienceToString(v: TrainingExperience): String = v.name
    @TypeConverter fun stringToExperience(v: String): TrainingExperience = TrainingExperience.valueOf(v)

    @TypeConverter fun equipmentSetToString(v: Set<Equipment>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToEquipmentSet(v: String): Set<Equipment> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { Equipment.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun prefsSetToString(v: Set<DietaryPreference>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToPrefsSet(v: String): Set<DietaryPreference> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { DietaryPreference.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun stringListToString(v: List<String>): String = v.joinToString("\u001F")
    @TypeConverter fun stringToStringList(v: String): List<String> =
        if (v.isBlank()) emptyList() else v.split("\u001F")

    @TypeConverter fun muscleSetToString(v: Set<MuscleGroup>): String = v.joinToString(",") { it.name }
    @TypeConverter fun stringToMuscleSet(v: String): Set<MuscleGroup> =
        if (v.isBlank()) emptySet() else v.split(",").mapNotNull { runCatching { MuscleGroup.valueOf(it) }.getOrNull() }.toSet()

    @TypeConverter fun muscleToString(v: MuscleGroup): String = v.name
    @TypeConverter fun stringToMuscle(v: String): MuscleGroup = MuscleGroup.valueOf(v)

    @TypeConverter fun carbDayToString(v: CarbDayType): String = v.name
    @TypeConverter fun stringToCarbDay(v: String): CarbDayType = CarbDayType.valueOf(v)

    @TypeConverter fun mealSlotToString(v: MealSlot): String = v.name
    @TypeConverter fun stringToMealSlot(v: String): MealSlot = MealSlot.valueOf(v)

    @TypeConverter fun foodUnitToString(v: FoodUnit): String = v.name
    @TypeConverter fun stringToFoodUnit(v: String): FoodUnit = FoodUnit.valueOf(v)

    @TypeConverter fun splitToString(v: SplitType): String = v.name
    @TypeConverter fun stringToSplit(v: String): SplitType = SplitType.valueOf(v)

    @TypeConverter fun categoryToString(v: ExerciseCategory): String = v.name
    @TypeConverter fun stringToCategory(v: String): ExerciseCategory = ExerciseCategory.valueOf(v)

    @TypeConverter fun sessionStatusToString(v: SessionStatus): String = v.name
    @TypeConverter fun stringToSessionStatus(v: String): SessionStatus = SessionStatus.valueOf(v)

    @TypeConverter fun changeStatusToString(v: ChangeStatus): String = v.name
    @TypeConverter fun stringToChangeStatus(v: String): ChangeStatus = ChangeStatus.valueOf(v)

    @TypeConverter fun changeDomainToString(v: ChangeDomain): String = v.name
    @TypeConverter fun stringToChangeDomain(v: String): ChangeDomain = ChangeDomain.valueOf(v)

    @TypeConverter fun chatRoleToString(v: ChatRole): String = v.name
    @TypeConverter fun stringToChatRole(v: String): ChatRole = ChatRole.valueOf(v)

    @TypeConverter fun providerToString(v: AiProvider): String = v.name
    @TypeConverter fun stringToProvider(v: String): AiProvider = AiProvider.valueOf(v)
}
