package com.fitcoach.app.domain.coach.tools

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiProgramProposalJson
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.nutrition.FoodFilter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strumento con cui l'AI consegna il PROGRAMMA COMPLETO (nutrizione + scheda).
 *
 * E il punto unico in cui un percorso viene creato: sia la generazione iniziale
 * sia le rigenerazioni successive passano da qui, dentro il ciclo agentico di
 * CoachOrchestrator. L'AI decide la struttura; questo strumento verifica con
 * [ProgramGenerationEngine] e salva solo se la proposta e valida. Se non lo e,
 * restituisce la diagnosi cosi il modello puo correggersi (nessuna logica di
 * generazione duplicata: un'unica autorita, un unico insieme di regole).
 */
@Singleton
class SubmitFullProgramTool @Inject constructor(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val planVersionRepository: PlanVersionRepository,
    private val db: FitCoachDatabase,
    private val json: Json
) : CoachTool {

    override val name = "submit_full_program"

    override val description =
        "Consegna e salva il PROGRAMMA COMPLETO (7 giorni nutrizionali con carb cycling + scheda " +
            "periodizzata) dopo averlo verificato contro i calcoli matematici, il database alimentare " +
            "ed esercizi e i vincoli del profilo. Chiamalo UNA sola volta, quando la struttura e " +
            "completa. Se la risposta contiene ok=false, correggi i valori indicati in 'violazioni' e " +
            "richiamalo. Non inventare alimenti o esercizi: usa solo quelli dei cataloghi."

    override val parameters: JsonObject = Schema.obj(
        "scope" to Schema.enumOf(
            "Cosa generare: BOTH (nutrizione e allenamento), NUTRITION (solo piano alimentare) " +
                "o WORKOUT (solo scheda). Usa il valore richiesto nel contesto.",
            "BOTH", "NUTRITION", "WORKOUT"
        ),
        "schemaVersion" to Schema.integer("Versione dello schema (1)."),
        "strategy" to Schema.string("Strategia complessiva in una frase."),
        "objectives" to Schema.arrayOfStrings("Obiettivi operativi."),
        "monitoring" to Schema.arrayOfStrings("Cosa monitorare."),
        "nutrition" to Schema.freeObject(
            "Oggetto {days:[{dayIndex (0-6), dayType (HIGH|MEDIUM|LOW), meals:[{slot, time, " +
                "entries:[{foodName, grams, unit}]}]}]}."
        ),
        "workout" to Schema.freeObject(
            "Oggetto {split (FULL_BODY|UPPER_LOWER|PUSH_PULL_LEGS|CUSTOM), rationale, " +
                "days:[{dayIndex, title, muscleGroups:[...], exercises:[{exerciseName, sets, minReps, " +
                "maxReps, restSeconds, targetRir, technique, techniqueReason}]}]}."
        ),
        "calendar" to Schema.freeObject(
            "Oggetto {workoutDayIndices:[...], restDayIndices:[...]} coerente con i giorni allenamento."
        ),
        required = listOf("nutrition", "workout", "calendar"),
        additionalProperties = true
    )

    override suspend fun execute(arguments: JsonObject): String {
        val profile = userRepository.get()
            ?: return fail("Nessun profilo: completa prima l'anamnesi.")
        if (!profile.isAnamnesisReady) {
            return fail("Anamnesi non confermata: impossibile salvare il programma.")
        }
        val scope = runCatching {
            ProgramGenerationEngine.Scope.valueOf(
                (JsonArgs(arguments).string("scope") ?: "BOTH").trim().uppercase()
            )
        }.getOrDefault(ProgramGenerationEngine.Scope.BOTH)
        val proposal = AiProgramProposalJson.decode(json, json.encodeToString(JsonObject.serializer(), arguments))
            ?: return fail("Struttura non valida: invia un singolo oggetto conforme allo schema.")

        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        if (foods.isEmpty()) return fail("Nessun alimento compatibile disponibile nel database.")
        if (exercises.isEmpty()) return fail("Database esercizi vuoto: impossibile validare la scheda.")

        val validated = ProgramGenerationEngine.validate(profile, proposal, foods, exercises, scope)
            ?: return JsonOut.encode(
                JsonOut.obj(
                    "ok" to JsonOut.bool(false),
                    "violazioni" to JsonOut.strings(
                        ProgramGenerationEngine.diagnose(profile, proposal, foods, exercises, scope)
                    ),
                    "nota" to JsonOut.str(
                        "Correggi SOLO i valori indicati e richiama submit_full_program con l'oggetto completo."
                    )
                )
            )

        val saved = runCatching {
            db.withTransaction {
                var nutritionId = 0L
                var workoutId = 0L
                validated.nutrition?.let { n ->
                    nutritionId = nutritionRepository.savePlan(
                        profile = profile,
                        energy = n.energy,
                        cycling = n.cycling,
                        generatedDays = n.days,
                        dayTypes = n.dayTypes,
                        trainingDayIndices = n.trainingDays,
                        carbCyclingApplied = n.carbCyclingApplied
                    )
                    nutritionRepository.getActivePlan()?.let {
                        planVersionRepository.snapshotNutrition(it, "Piano generato dal Coach AI e validato dai motori matematici.")
                    }
                }
                validated.workout?.let { w ->
                    workoutId = workoutRepository.savePlan(profile, w)
                    workoutRepository.getActivePlan()?.let {
                        planVersionRepository.snapshotWorkout(it, "Scheda generata dal Coach AI e validata dai vincoli di sicurezza.")
                    }
                }
                nutritionId to workoutId
            }
        }.getOrElse { error ->
            return fail("Programma non salvato: ${error.message ?: "errore di persistenza"}")
        }

        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "stato" to JsonOut.str("PROGRAMMA_SALVATO"),
                "ambito" to JsonOut.str(scope.name),
                "nutritionPlanId" to JsonOut.num(saved.first),
                "workoutPlanId" to JsonOut.num(saved.second),
                "avvisi" to JsonOut.strings(validated.warnings)
            )
        )
    }

    private fun fail(message: String) =
        JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str(message)))
}
