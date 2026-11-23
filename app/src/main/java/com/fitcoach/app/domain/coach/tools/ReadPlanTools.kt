package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stato corrente del piano alimentare attivo, in forma strutturata: target
 * energetici, ciclicita dei carboidrati, giorni, pasti e voci con grammature.
 */
@Singleton
class GetActiveNutritionPlanTool @Inject constructor(
    private val nutritionRepository: NutritionRepository
) : CoachTool {
    override val name = "get_active_nutrition_plan"
    override val description =
        "Restituisce il piano alimentare ATTIVO: target kcal/macro, giornate HIGH/MEDIUM/LOW, " +
            "pasti (slot, orario) e voci (alimento, grammi, crudo/cotto, kcal e macro). " +
            "Usalo per analizzare, spiegare o modificare la nutrizione. Ritorna vuoto se non esiste."

    override val parameters: JsonObject = Schema.obj(
        "includiPasti" to Schema.boolean("Se true include il dettaglio dei pasti (default false).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val withMeals = JsonArgs(arguments).boolean("includiPasti") ?: false
        val plan = nutritionRepository.getActivePlan()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(true), "pianoPresente" to JsonOut.bool(false)))
        val days = plan.days.map { d ->
            JsonOut.obj(
                "giorno" to JsonOut.num(d.dayIndex),
                "tipoGiornata" to JsonOut.str(d.dayType.name),
                "targetKcal" to JsonOut.num(d.targetKcal),
                "targetProteine" to JsonOut.num(d.targetProtein),
                "targetCarboidrati" to JsonOut.num(d.targetCarbs),
                "targetGrassi" to JsonOut.num(d.targetFat),
                "effettiveKcal" to JsonOut.num(d.actualKcal),
                "effettiveProteine" to JsonOut.num(d.actualProtein),
                "effettiveCarboidrati" to JsonOut.num(d.actualCarbs),
                "effettiveGrassi" to JsonOut.num(d.actualFat),
                "pasti" to if (withMeals) JsonOut.arr(
                    d.meals.map { m ->
                        JsonOut.obj(
                            "slot" to JsonOut.str(m.slot.name),
                            "orario" to JsonOut.str(m.time),
                            "kcal" to JsonOut.num(m.kcal),
                            "voci" to JsonOut.arr(
                                m.entries.map { e ->
                                    JsonOut.obj(
                                        "alimentoId" to JsonOut.num(e.foodItemId),
                                        "alimento" to JsonOut.str(e.foodName),
                                        "grammi" to JsonOut.num(e.grams),
                                        "unita" to JsonOut.str(e.unit.name),
                                        "kcal" to JsonOut.num(e.kcal),
                                        "proteine" to JsonOut.num(e.protein),
                                        "carboidrati" to JsonOut.num(e.carbs),
                                        "grassi" to JsonOut.num(e.fat)
                                    )
                                }
                            )
                        )
                    }
                ) else null
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "pianoPresente" to JsonOut.bool(true),
                "nome" to JsonOut.str(plan.name),
                "versione" to JsonOut.num(plan.version),
                "bmr" to JsonOut.num(plan.bmr),
                "tdee" to JsonOut.num(plan.tdee),
                "formulaBmr" to JsonOut.str(plan.bmrFormula),
                "formulaTdee" to JsonOut.str(plan.tdeeFormula),
                "kcalSettimanali" to JsonOut.num(plan.weeklyCalories),
                "bilancioSettimanale" to JsonOut.num(plan.weeklyKcalBalance),
                "carbCyclingApplicato" to JsonOut.bool(plan.carbCyclingApplied),
                "giornateHigh" to JsonOut.num(plan.highCarbDays),
                "giornateMedium" to JsonOut.num(plan.mediumCarbDays),
                "giornateLow" to JsonOut.num(plan.lowCarbDays),
                "proteineGPerKg" to JsonOut.num(plan.proteinGPerKg),
                "giorni" to JsonOut.arr(days)
            )
        )
    }
}

/** Stato corrente del piano di allenamento attivo. */
@Singleton
class GetActiveWorkoutPlanTool @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : CoachTool {
    override val name = "get_active_workout_plan"
    override val description =
        "Restituisce la scheda di allenamento ATTIVA: split, mesocicli, giornate (gruppi " +
            "muscolari, esercizi con serie/ripetizioni/recupero/RIR) e volume settimanale per gruppo. " +
            "Usalo per analizzare, spiegare o modificare l'allenamento."

    override val parameters: JsonObject = Schema.obj(
        "includiRiposo" to Schema.boolean("Se true include anche le giornate di riposo (default false).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val withRest = JsonArgs(arguments).boolean("includiRiposo") ?: false
        val plan = workoutRepository.getActivePlan()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(true), "pianoPresente" to JsonOut.bool(false)))
        val days = plan.days.filter { withRest || !it.isRestDay }.map { d ->
            JsonOut.obj(
                "dayId" to JsonOut.num(d.id),
                "giorno" to JsonOut.num(d.dayIndex),
                "titolo" to JsonOut.str(d.title),
                "riposo" to JsonOut.bool(d.isRestDay),
                "gruppiMuscolari" to JsonOut.strings(d.muscleGroups.map { it.name }),
                "serieTotali" to JsonOut.num(d.totalSets),
                "esercizi" to JsonOut.arr(
                    d.exercises.map { e ->
                        JsonOut.obj(
                            "plannedId" to JsonOut.num(e.id),
                            "exerciseId" to JsonOut.num(e.exerciseId),
                            "nome" to JsonOut.str(e.exerciseName),
                            "gruppoPrincipale" to JsonOut.str(e.primaryMuscle.name),
                            "serie" to JsonOut.num(e.sets),
                            "ripMin" to JsonOut.num(e.minReps),
                            "ripMax" to JsonOut.num(e.maxReps),
                            "recuperoSec" to JsonOut.num(e.restSeconds),
                            "rirTarget" to JsonOut.num(e.targetRir),
                            "categoria" to JsonOut.str(e.category.name),
                            "pattern" to JsonOut.str(e.movementPattern.name),
                            "tecnicaIntensita" to JsonOut.str(e.intensityTechnique.name),
                            "caricoTargetKg" to JsonOut.num(e.targetWeightKg),
                            "motivazione" to JsonOut.str(e.selectionReason)
                        )
                    }
                )
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "pianoPresente" to JsonOut.bool(true),
                "nome" to JsonOut.str(plan.name),
                "split" to JsonOut.str(plan.splitType.name),
                "versione" to JsonOut.num(plan.version),
                "mesocicli" to JsonOut.arr(
                    plan.mesocycles.map { m ->
                        JsonOut.obj(
                            "nome" to JsonOut.str(m.name),
                            "focus" to JsonOut.str(m.focus),
                            "settimane" to JsonOut.num(m.weekCount),
                            "deload" to JsonOut.bool(m.isDeload),
                            "modelloProgressione" to JsonOut.str(m.progressionModel)
                        )
                    }
                ),
                "volumeSettimanalePerGruppo" to JsonOut.obj(
                    *plan.weeklyVolumePerGroup.map { (k, v) -> k.name to JsonOut.num(v) }.toTypedArray()
                ),
                "giornate" to JsonOut.arr(days)
            )
        )
    }
}
