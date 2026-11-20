package com.fitcoach.app.di

import com.fitcoach.app.domain.coach.tools.AdjustCaloriesTool
import com.fitcoach.app.domain.coach.tools.AdjustVolumeTool
import com.fitcoach.app.domain.coach.tools.AnalyzeWeeklyCheckInTool
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.ComputeCarbCyclingTool
import com.fitcoach.app.domain.coach.tools.ComputeEnergyTargetsTool
import com.fitcoach.app.domain.coach.tools.ComputeWeeklyVolumeTool
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.EstimateOneRepMaxTool
import com.fitcoach.app.domain.coach.tools.GetActiveNutritionPlanTool
import com.fitcoach.app.domain.coach.tools.GetActiveWorkoutPlanTool
import com.fitcoach.app.domain.coach.tools.GetAnamnesisTool
import com.fitcoach.app.domain.coach.tools.GetCatalogSummaryTool
import com.fitcoach.app.domain.coach.tools.GetNutritionDiaryTool
import com.fitcoach.app.domain.coach.tools.GetProgressPhotosTool
import com.fitcoach.app.domain.coach.tools.GetProgressTool
import com.fitcoach.app.domain.coach.tools.GetWorkoutHistoryTool
import com.fitcoach.app.domain.coach.tools.ListExercisesTool
import com.fitcoach.app.domain.coach.tools.ListFoodsTool
import com.fitcoach.app.domain.coach.tools.MoveWorkoutTool
import com.fitcoach.app.domain.coach.tools.ProposeDeloadTool
import com.fitcoach.app.domain.coach.tools.ProposeRegeneratePlanTool
import com.fitcoach.app.domain.coach.tools.SubstituteExerciseTool
import com.fitcoach.app.domain.coach.tools.SubstituteFoodTool
import com.fitcoach.app.domain.coach.tools.ValidateWorkoutPlanTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registra tutti gli strumenti disponibili per l'AI nel set iniettato in
 * CoachToolRegistry. Aggiungere uno strumento significa aggiungere un binding qui.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoachToolsModule {

    // ---- Lettura: anamnesi e cataloghi ----
    @Provides @IntoSet fun anamnesis(t: GetAnamnesisTool): CoachTool = t
    @Provides @IntoSet fun catalog(t: GetCatalogSummaryTool): CoachTool = t
    @Provides @IntoSet fun foods(t: ListFoodsTool): CoachTool = t
    @Provides @IntoSet fun exercises(t: ListExercisesTool): CoachTool = t

    // La metodologia resta nei dati: il modello la interroga, non la legge tutta.
    @Provides @IntoSet fun invictus(t: ConsultInvictusTool): CoachTool = t

    // ---- Lettura: stato corrente ----
    @Provides @IntoSet fun nutritionPlan(t: GetActiveNutritionPlanTool): CoachTool = t
    @Provides @IntoSet fun workoutPlan(t: GetActiveWorkoutPlanTool): CoachTool = t
    @Provides @IntoSet fun history(t: GetWorkoutHistoryTool): CoachTool = t
    @Provides @IntoSet fun progress(t: GetProgressTool): CoachTool = t
    @Provides @IntoSet fun diary(t: GetNutritionDiaryTool): CoachTool = t
    @Provides @IntoSet fun photos(t: GetProgressPhotosTool): CoachTool = t

    // ---- Analisi (strumenti deterministici subordinati) ----
    @Provides @IntoSet fun energy(t: ComputeEnergyTargetsTool): CoachTool = t
    @Provides @IntoSet fun carbCycling(t: ComputeCarbCyclingTool): CoachTool = t
    @Provides @IntoSet fun volume(t: ComputeWeeklyVolumeTool): CoachTool = t
    @Provides @IntoSet fun validate(t: ValidateWorkoutPlanTool): CoachTool = t
    @Provides @IntoSet fun oneRm(t: EstimateOneRepMaxTool): CoachTool = t
    @Provides @IntoSet fun portion(t: com.fitcoach.app.domain.coach.tools.ComputePortionTool): CoachTool = t
    @Provides @IntoSet fun substitution(t: com.fitcoach.app.domain.coach.tools.ComputeSubstitutionTool): CoachTool = t
    @Provides @IntoSet fun validateDay(t: com.fitcoach.app.domain.coach.tools.ValidateNutritionDayTool): CoachTool = t
    @Provides @IntoSet fun nutritionTargets(
        t: com.fitcoach.app.domain.coach.tools.GetNutritionTargetsTool
    ): CoachTool = t
    @Provides @IntoSet fun solvePortions(
        t: com.fitcoach.app.domain.coach.tools.SolveMealPortionsTool
    ): CoachTool = t
    @Provides @IntoSet fun submitProgram(t: com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool): CoachTool = t
    @Provides @IntoSet fun weeklyCheckIn(t: AnalyzeWeeklyCheckInTool): CoachTool = t

    // ---- Scrittura controllata (propongono, non applicano) ----
    @Provides @IntoSet fun substituteFood(t: SubstituteFoodTool): CoachTool = t
    @Provides @IntoSet fun substituteExercise(t: SubstituteExerciseTool): CoachTool = t
    @Provides @IntoSet fun moveWorkout(t: MoveWorkoutTool): CoachTool = t
    @Provides @IntoSet fun adjustCalories(t: AdjustCaloriesTool): CoachTool = t
    @Provides @IntoSet fun adjustVolume(t: AdjustVolumeTool): CoachTool = t
    @Provides @IntoSet fun deload(t: ProposeDeloadTool): CoachTool = t
    @Provides @IntoSet fun regenerate(t: ProposeRegeneratePlanTool): CoachTool = t
}
