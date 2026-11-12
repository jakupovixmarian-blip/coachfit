package dev.fitcoach.app.data.local.db

import androidx.room.TypeConverter
import dev.fitcoach.app.domain.model.AiProvider
import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.PhotoPose
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UnitSystem
import dev.fitcoach.app.domain.model.CoachChangeStatus
import dev.fitcoach.app.domain.model.CoachChangeType
import dev.fitcoach.app.domain.model.MeasurementType

class Converters {
    @TypeConverter fun muscleListToString(value: List<MuscleGroup>): String = value.joinToString(",") { it.name }
    @TypeConverter fun stringToMuscleList(value: String): List<MuscleGroup> =
        value.split(",").mapNotNull { name -> MuscleGroup.entries.firstOrNull { it.name == name } }

    @TypeConverter fun sexToString(v: Sex): String = v.name
    @TypeConverter fun stringToSex(v: String): Sex = enumOr(v, Sex.UNSPECIFIED)
    @TypeConverter fun goalToString(v: Goal): String = v.name
    @TypeConverter fun stringToGoal(v: String): Goal = enumOr(v, Goal.RECOMP)
    @TypeConverter fun experienceToString(v: ExperienceLevel): String = v.name
    @TypeConverter fun stringToExperience(v: String): ExperienceLevel = enumOr(v, ExperienceLevel.BEGINNER)
    @TypeConverter fun unitToString(v: UnitSystem): String = v.name
    @TypeConverter fun stringToUnit(v: String): UnitSystem = enumOr(v, UnitSystem.METRIC)
    @TypeConverter fun measurementTypeToString(v: MeasurementType): String = v.name
    @TypeConverter fun stringToMeasurementType(v: String): MeasurementType = enumOr(v, MeasurementType.CUSTOM)
    @TypeConverter fun photoPoseToString(v: PhotoPose): String = v.name
    @TypeConverter fun stringToPhotoPose(v: String): PhotoPose = enumOr(v, PhotoPose.FRONT)
    @TypeConverter fun muscleToString(v: MuscleGroup): String = v.name
    @TypeConverter fun stringToMuscle(v: String): MuscleGroup = enumOr(v, MuscleGroup.FULL_BODY)
    @TypeConverter fun equipmentToString(v: Equipment): String = v.name
    @TypeConverter fun stringToEquipment(v: String): Equipment = enumOr(v, Equipment.OTHER)
    @TypeConverter fun patternToString(v: MovementPattern): String = v.name
    @TypeConverter fun stringToPattern(v: String): MovementPattern = enumOr(v, MovementPattern.ISOLATION)
    @TypeConverter fun setTypeToString(v: SetType): String = v.name
    @TypeConverter fun stringToSetType(v: String): SetType = enumOr(v, SetType.WORKING)
    @TypeConverter fun sessionStatusToString(v: SessionStatus): String = v.name
    @TypeConverter fun stringToSessionStatus(v: String): SessionStatus = enumOr(v, SessionStatus.PLANNED)
    @TypeConverter fun foodSourceToString(v: FoodSource): String = v.name
    @TypeConverter fun stringToFoodSource(v: String): FoodSource = enumOr(v, FoodSource.MANUAL)
    @TypeConverter fun foodStateToString(v: FoodState): String = v.name
    @TypeConverter fun stringToFoodState(v: String): FoodState = enumOr(v, FoodState.RAW)
    @TypeConverter fun mealSlotToString(v: MealSlot): String = v.name
    @TypeConverter fun stringToMealSlot(v: String): MealSlot = enumOr(v, MealSlot.BREAKFAST)
    @TypeConverter fun providerToString(v: AiProvider): String = v.name
    @TypeConverter fun stringToProvider(v: String): AiProvider = enumOr(v, AiProvider.OPENAI)
    @TypeConverter fun changeTypeToString(v: CoachChangeType): String = v.name
    @TypeConverter fun stringToChangeType(v: String): CoachChangeType = enumOr(v, CoachChangeType.CALORIES)
    @TypeConverter fun changeStatusToString(v: CoachChangeStatus): String = v.name
    @TypeConverter fun stringToChangeStatus(v: String): CoachChangeStatus = enumOr(v, CoachChangeStatus.PROPOSED)

    private inline fun <reified T : Enum<T>> enumOr(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
