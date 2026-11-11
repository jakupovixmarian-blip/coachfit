package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.ChatMessage
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.ProgramChange
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightTrend
import com.fitcoach.app.domain.model.WorkoutPlan

/**
 * Fotografia strutturata dello stato del programma, usata per costruire il
 * contesto della chat. Contiene SOLO dati reali presenti nel database: nessun
 * valore inventato.
 */
data class CoachContext(
    val profile: UserProfile?,
    val nutritionPlan: NutritionPlan?,
    val workoutPlan: WorkoutPlan?,
    val recentPerformances: List<ExercisePerformance>,
    val weightTrend: WeightTrend?,
    val recentChanges: List<ProgramChange>,
    val history: List<ChatMessage>
)
