package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.ProgramChange
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import dagger.Lazy
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class NutritionScaleSnapshot(
    val planId: Long,
    val weeklyCalories: Double,
    val weeklyKcalBalance: Double,
    val days: List<NutritionDaySnapshot>,
    val entries: List<NutritionEntrySnapshot>
)

@Serializable
private data class NutritionDaySnapshot(
    val id: Long,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Serializable
private data class NutritionEntrySnapshot(
    val id: Long,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * Snapshot dei soli riferimenti ai piani attivi prima di una rigenerazione
 * strutturale. La rigenerazione crea NUOVI piani (nuove versioni) e disattiva i
 * precedenti, quindi il rollback deve riattivare l'id esatto di prima.
 */
@Serializable
private data class PlanIdSnapshot(
    val scope: String,
    val nutritionPlanId: Long?,
    val workoutPlanId: Long?
)


/** Esito dell'applicazione (o dell'annullamento) di una proposta di modifica. */
sealed interface ApplyResult {
    data class Applied(val message: String) : ApplyResult
    data class Reverted(val message: String) : ApplyResult
    data class Failed(val message: String) : ApplyResult
}

/**
 * Applica le modifiche proposte dal coach SOLO dopo la conferma dell'utente.
 * Ogni modifica e transazionale e registrata in [ProgramChange] con un payload
 * di ripristino, cosi e possibile annullarla e tornare allo stato precedente.
 */
@Singleton
class CoachActionExecutor @Inject constructor(
    private val db: FitCoachDatabase,
    private val changeRepository: ChangeRepository,
    private val userRepository: UserRepository,
    private val generationCoordinator: Lazy<ProgramGenerationCoordinator>,
    private val json: Json
) {
    private val entryDao = db.mealEntryDao()
    private val mealDao = db.mealDao()
    private val plannedDao = db.plannedExerciseDao()
    private val dayDao = db.nutritionDayDao()
    private val planDao = db.nutritionPlanDao()
    private val mesoDao = db.mesocycleDao()
    private val workoutDayDao = db.workoutDayDao()
    private val workoutPlanDao = db.workoutPlanDao()

    private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

    /** Registra la proposta come PROPOSED senza applicarla. Ritorna l'id della modifica. */
    suspend fun registerProposal(action: CoachAction): Long =
        changeRepository.add(action.toProgramChange(ChangeStatus.PROPOSED))

    suspend fun apply(changeId: Long): ApplyResult {
        val change = changeRepository.getById(changeId)
            ?: return ApplyResult.Failed("Modifica non trovata.")
        if (change.status != ChangeStatus.PROPOSED) {
            return ApplyResult.Failed("Questa modifica non e piu in attesa di conferma.")
        }
        val action = decodeAction(change)
            ?: return ApplyResult.Failed("La proposta non contiene un'azione applicabile.")

        // La rigenerazione strutturale non e una modifica puntuale: non puo stare in
        // una transazione Room perche il motore unico apre la propria e puo richiedere
        // una chiamata al provider AI. La gestiamo a parte, mantenendo la stessa
        // registrazione di esito e lo stesso payload di ripristino delle altre azioni.
        if (action.action == CoachAction.ACTION_REGENERATE_PLAN) {
            return applyRegeneration(change, action)
        }

        return try {
            val revert = db.withTransaction {
                when (action.action) {
                    CoachAction.ACTION_SUBSTITUTE_FOOD -> applyFoodSubstitution(action)
                    CoachAction.ACTION_SUBSTITUTE_EXERCISE -> applyExerciseSubstitution(action)
                    CoachAction.ACTION_SET_INTENSITY -> applyIntensityTechnique(action)
                    CoachAction.ACTION_MOVE_WORKOUT -> applyMoveWorkout(action)
                    CoachAction.ACTION_ADJUST_CALORIES -> applyCalorieAdjustment(action)
                    CoachAction.ACTION_ADJUST_VOLUME -> applyVolumeAdjustment(action)
                    CoachAction.ACTION_DELOAD -> applyDeload(action)
                    else -> throw IllegalArgumentException("Azione non supportata: ${action.action}")
                }
            }
            changeRepository.update(
                change.copy(
                    status = ChangeStatus.APPLIED,
                    appliedAt = System.currentTimeMillis(),
                    revertPayloadJson = json.encodeToString(stringMapSerializer, revert)
                )
            )
            ApplyResult.Applied(change.title.ifBlank { "Modifica applicata." })
        } catch (e: Exception) {
            ApplyResult.Failed("Impossibile applicare la modifica: ${e.message ?: e::class.simpleName}")
        }
    }

    /**
     * Applica una rigenerazione strutturale: delega al motore unico
     * ([ProgramGenerationCoordinator]) con l'ambito richiesto (WORKOUT/NUTRITION/BOTH).
     * Non esiste un secondo motore: la proposta dell'AI viene verificata e persistita
     * esattamente come nella generazione iniziale.
     */
    private suspend fun applyRegeneration(change: ProgramChange, action: CoachAction): ApplyResult {
        val scope = parseScope(action.payload["scope"])
            ?: return ApplyResult.Failed("Ambito di rigenerazione non valido.")
        val profile = userRepository.get()
            ?: return ApplyResult.Failed("Nessun profilo: completa prima l'anamnesi.")

        // Riferimenti ai piani attivi PRIMA della rigenerazione, per il rollback.
        val before = PlanIdSnapshot(
            scope = scope.name,
            nutritionPlanId = planDao.getActive()?.id,
            workoutPlanId = workoutPlanDao.getActive()?.id
        )

        val result = try {
            generationCoordinator.get().generate(profile, scope)
        } catch (e: Exception) {
            return ApplyResult.Failed("Rigenerazione non riuscita: ${e.message ?: e::class.simpleName}")
        }

        return when (result) {
            is ProgramGenerationResult.Success -> {
                val revert = mapOf(
                    "kind" to "PLAN_REGENERATED",
                    "snapshot" to json.encodeToString(PlanIdSnapshot.serializer(), before)
                )
                changeRepository.update(
                    change.copy(
                        status = ChangeStatus.APPLIED,
                        appliedAt = System.currentTimeMillis(),
                        revertPayloadJson = json.encodeToString(stringMapSerializer, revert)
                    )
                )
                val extra = result.warnings.firstOrNull()?.let { " $it" } ?: ""
                ApplyResult.Applied("Programma rigenerato (${scope.name}).$extra")
            }
            is ProgramGenerationResult.MissingData ->
                ApplyResult.Failed("Dati mancanti per rigenerare: ${result.missing.joinToString()}.")
            is ProgramGenerationResult.AnamnesisNotConfirmed ->
                ApplyResult.Failed("Anamnesi non confermata: impossibile rigenerare il programma.")
            is ProgramGenerationResult.Error -> ApplyResult.Failed(result.message)
        }
    }

    private fun parseScope(raw: String?): ProgramGenerationCoordinator.Scope? =
        runCatching { ProgramGenerationCoordinator.Scope.valueOf(raw?.trim()?.uppercase() ?: "BOTH") }
            .getOrNull()

    suspend fun revert(changeId: Long): ApplyResult {
        val change = changeRepository.getById(changeId)
            ?: return ApplyResult.Failed("Modifica non trovata.")
        if (change.status != ChangeStatus.APPLIED) {
            return ApplyResult.Failed("Solo una modifica applicata puo essere annullata.")
        }
        val revert = decodeRevertPayload(change)?.takeIf { it.isNotEmpty() }
            ?: return ApplyResult.Failed("Nessun dato di ripristino disponibile per questa modifica.")

        return try {
            db.withTransaction { applyRevert(revert) }
            changeRepository.updateStatus(changeId, ChangeStatus.REVERTED)
            ApplyResult.Reverted("Modifica annullata: programma ripristinato.")
        } catch (e: Exception) {
            ApplyResult.Failed("Impossibile annullare la modifica: ${e.message ?: e::class.simpleName}")
        }
    }

    private suspend fun applyRevert(revert: Map<String, String>) {
        when (revert["kind"]) {
            "FOOD_ENTRY" -> entryDao.updateEntry(
                id = revert.require("entryId").toLong(),
                grams = revert.require("grams").toDouble(),
                kcal = revert.require("kcal").toDouble(),
                protein = revert.require("protein").toDouble(),
                carbs = revert.require("carbs").toDouble(),
                fat = revert.require("fat").toDouble()
            )
            "PLANNED_EXERCISE" -> {
                val plannedId = revert.require("plannedId").toLong()
                // Una sostituzione cambia esercizio e tecnica, non solo il
                // carico: annullarla deve riportare la scheda com'era, altrimenti
                // la tecnica d'intensita' resta addosso all'esercizio nuovo.
                val previousExerciseId = revert["exerciseId"]?.toLongOrNull()
                val previousName = revert["exerciseName"].orEmpty()
                if (previousExerciseId != null && previousName.isNotBlank()) {
                    plannedDao.substituteExercise(
                        id = plannedId,
                        exerciseId = previousExerciseId,
                        name = previousName,
                        technique = revert["technique"].orEmpty(),
                        intensityTechnique = revert["intensityTechnique"]
                            ?.let { runCatching { IntensityTechnique.valueOf(it) }.getOrNull() }
                            ?: IntensityTechnique.NONE,
                        techniqueReason = revert["techniqueReason"].orEmpty()
                    )
                }
                plannedDao.updateTarget(
                    id = plannedId,
                    weight = revert["weight"]?.toDoubleOrNull(),
                    reps = revert["reps"]?.toIntOrNull()
                )
            }
            "PLANNED_EXERCISE_TECHNIQUE" -> {
                val plannedId = revert.require("plannedId").toLong()
                plannedDao.getById(plannedId)?.let { current ->
                    plannedDao.update(
                        current.copy(
                            intensityTechnique = revert["intensityTechnique"]
                                ?.let { runCatching { IntensityTechnique.valueOf(it) }.getOrNull() }
                                ?: IntensityTechnique.NONE,
                            techniqueReason = revert["techniqueReason"].orEmpty()
                        )
                    )
                } ?: throw IllegalStateException("Esercizio pianificato non trovato.")
            }
            "PLANNED_SETS" -> revert.filterKeys { it.startsWith("sets_") }.forEach { (key, value) ->
                val plannedId = key.removePrefix("sets_").toLong()
                plannedDao.updateSets(plannedId, value.toInt())
            }
            "PLAN_CALORIES" -> planDao.getById(revert.require("planId").toLong())?.let { plan ->
                planDao.update(
                    plan.copy(
                        weeklyCalories = revert.require("weeklyCalories").toDouble(),
                        weeklyKcalBalance = revert.require("weeklyKcalBalance").toDouble()
                    )
                )
            } ?: throw IllegalStateException("Piano non trovato.")
            "PLAN_SCALE" -> {
                val snapshot = json.decodeFromString<NutritionScaleSnapshot>(revert.require("snapshot"))
                val plan = planDao.getById(snapshot.planId)
                    ?: throw IllegalStateException("Piano non trovato.")
                planDao.update(plan.copy(
                    weeklyCalories = snapshot.weeklyCalories,
                    weeklyKcalBalance = snapshot.weeklyKcalBalance
                ))
                snapshot.days.forEach { day ->
                    dayDao.updateTargets(day.id, day.kcal, day.protein, day.carbs, day.fat)
                }
                snapshot.entries.forEach { entry ->
                    entryDao.updateEntry(entry.id, entry.grams, entry.kcal, entry.protein, entry.carbs, entry.fat)
                }
            }
            "WORKOUT_DAY_INDEX" -> dayDao.updateWorkoutDayIndex(
                id = revert.require("dayId").toLong(),
                workoutDayIndex = revert["workoutDayIndex"]?.toIntOrNull()
            )
            "PLAN_REGENERATED" -> restoreRegeneratedPlans(revert.require("snapshot"))
            else -> throw IllegalArgumentException("Ripristino non supportato.")
        }
    }

    /**
     * Ripristina i piani attivi precedenti a una rigenerazione strutturale,
     * riattivando l'id esatto di prima (le nuove versioni restano nello storico,
     * consultabili, ma non piu attive).
     */
    private suspend fun restoreRegeneratedPlans(snapshotJson: String) {
        val snapshot = json.decodeFromString<PlanIdSnapshot>(snapshotJson)
        snapshot.nutritionPlanId?.let { id ->
            planDao.getById(id)?.let { previous ->
                planDao.deactivateAll()
                planDao.update(previous.copy(isActive = true))
            }
        }
        snapshot.workoutPlanId?.let { id ->
            workoutPlanDao.getById(id)?.let { previous ->
                workoutPlanDao.deactivateAll()
                workoutPlanDao.insert(previous.copy(isActive = true))
            }
        }
    }

    // ---- Applicazioni (ognuna ritorna il payload di ripristino) ----

    private suspend fun applyFoodSubstitution(action: CoachAction): Map<String, String> {
        val entryId = action.payload.require("entryId").toLong()
        val existing = entryDao.getById(entryId)
            ?: throw IllegalArgumentException("Voce pasto non trovata.")

        val newFoodId = action.payload["newFoodId"]?.toLongOrNull() ?: 0L
        val newFoodName = action.payload["newFoodName"] ?: action.payload["foodName"]
            ?: throw IllegalArgumentException("newFoodName mancante.")
        val grams = action.payload.require("grams").toDouble()
        val kcal = action.payload.require("kcal").toDouble()
        val protein = action.payload.require("protein").toDouble()
        val carbs = action.payload.require("carbs").toDouble()
        val fat = action.payload.require("fat").toDouble()

        entryDao.replaceEntry(entryId, newFoodId, newFoodName, grams, kcal, protein, carbs, fat)

        return mapOf(
            "kind" to "FOOD_ENTRY",
            "entryId" to entryId.toString(),
            "grams" to existing.grams.toString(),
            "kcal" to existing.kcal.toString(),
            "protein" to existing.protein.toString(),
            "carbs" to existing.carbs.toString(),
            "fat" to existing.fat.toString()
        )
    }

    private suspend fun applyExerciseSubstitution(action: CoachAction): Map<String, String> {
        val plannedId = action.payload.require("plannedId").toLong()
        val exerciseId = action.payload.require("exerciseId").toLong()
        val name = action.payload.require("exerciseName")
        val technique = action.payload["technique"] ?: ""
        val intensity = action.payload["intensityTechnique"]
            ?.let { runCatching { IntensityTechnique.valueOf(it.uppercase()) }.getOrNull() }
            ?: IntensityTechnique.NONE
        val techniqueReason = action.payload["techniqueReason"] ?: ""

        // Lo snapshot serve SEMPRE: senza, l'annullamento non sa cosa ripristinare.
        // Prima veniva letto solo se il payload conteneva dayId, che il tool non
        // invia, quindi annullare una sostituzione non rimetteva l'esercizio.
        val existing = plannedDao.getById(plannedId)
        plannedDao.substituteExercise(
            plannedId, exerciseId, name, technique, intensity, techniqueReason
        )
        return mapOf(
            "kind" to "PLANNED_EXERCISE",
            "plannedId" to plannedId.toString(),
            "weight" to (existing?.targetWeightKg?.toString() ?: ""),
            "reps" to (existing?.targetReps?.toString() ?: ""),
            "exerciseId" to (existing?.exerciseId?.toString() ?: ""),
            "exerciseName" to (existing?.exerciseName ?: ""),
            "technique" to (existing?.technique ?: ""),
            "intensityTechnique" to (existing?.intensityTechnique?.name ?: "NONE"),
            "techniqueReason" to (existing?.techniqueReason ?: "")
        )
    }

    /**
     * Applica una tecnica d'intensita' a un esercizio gia' in scheda.
     *
     * Tocca solo i tre campi della tecnica: esercizio, serie, range e carico
     * restano quelli decisi dal programma. Il revert ripristina i valori
     * precedenti, quindi annullare la modifica rimette la scheda com'era.
     */
    private suspend fun applyIntensityTechnique(action: CoachAction): Map<String, String> {
        val plannedId = action.payload.require("plannedId").toLong()
        val technique = action.payload["intensityTechnique"]
            ?.trim()?.uppercase()
            ?.let { runCatching { IntensityTechnique.valueOf(it) }.getOrNull() }
            ?: throw IllegalArgumentException(
                "Tecnica d'intensita' non riconosciuta: '${action.payload["intensityTechnique"]}'."
            )
        val reason = action.payload["techniqueReason"].orEmpty()
        if (technique == IntensityTechnique.NONE && reason.isBlank()) {
            throw IllegalArgumentException("Nessuna tecnica indicata da applicare.")
        }

        val existing = plannedDao.getById(plannedId)
            ?: throw IllegalArgumentException("Esercizio pianificato $plannedId non trovato.")
        plannedDao.update(
            existing.copy(
                intensityTechnique = technique,
                techniqueReason = reason
            )
        )
        return mapOf(
            "kind" to "PLANNED_EXERCISE_TECHNIQUE",
            "plannedId" to plannedId.toString(),
            "intensityTechnique" to existing.intensityTechnique.name,
            "techniqueReason" to existing.techniqueReason
        )
    }

    private suspend fun applyMoveWorkout(action: CoachAction): Map<String, String> {
        val dayId = action.payload.require("dayId").toLong()
        val newIndex = action.payload.require("newDayIndex").toInt()
        val day = dayDao.getById(dayId) ?: throw IllegalArgumentException("Giorno nutrizionale non trovato.")
        val previous = day.workoutDayIndex
        dayDao.updateWorkoutDayIndex(dayId, newIndex)
        return mapOf(
            "kind" to "WORKOUT_DAY_INDEX",
            "dayId" to dayId.toString(),
            "workoutDayIndex" to (previous?.toString() ?: "")
        )
    }

    /**
     * Risolve il piano nutrizionale indicato dalla proposta.
     *
     * L'AI puo sbagliare o inventare l'id: se l'id non esiste ma un piano attivo
     * c'e, la proposta si applica al piano attivo invece di fallire. Il fallimento
     * capita esattamente quando l'utente vede il piano a schermo, ed e il caso in
     * cui l'errore sarebbe piu fuorviante.
     */
    private suspend fun resolveNutritionPlanId(requested: Long?): Long {
        val active = planDao.getActive()
            ?: throw IllegalArgumentException("Nessun piano alimentare attivo da modificare.")
        // Il piano attivo ha sempre la precedenza. Un id diverso indica una
        // proposta costruita su una versione vecchia, oppure inventata: in
        // entrambi i casi modificare quel piano non avrebbe effetto visibile,
        // perche l'utente segue quello attivo.
        if (requested != null && requested != active.id &&
            planDao.getById(requested)?.isActive == true
        ) {
            return requested
        }
        return active.id
    }

    private suspend fun applyCalorieAdjustment(action: CoachAction): Map<String, String> {
        val planId = resolveNutritionPlanId(action.payload["planId"]?.toLongOrNull())
        val plan = planDao.getById(planId)
            ?: throw IllegalArgumentException("Piano alimentare non trovato.")
        val newWeekly = action.payload.require("weeklyKcal").toDouble()
        require(newWeekly > 0.0) { "Il target calorico deve essere positivo." }
        val ratio = newWeekly / plan.weeklyCalories
        require(ratio in 0.90..1.10) { "La variazione calorica massima è del 10%." }
        if (plan.bmr > 0.0) {
            require(newWeekly / 7.0 >= plan.bmr * 0.85) { "Il target scenderebbe sotto la soglia di sicurezza." }
        }

        val days = dayDao.getForPlan(planId)
        val entries = days.flatMap { day ->
            mealDao.getForDay(day.id).flatMap { meal -> entryDao.getForMeal(meal.id) }
        }
        val snapshot = NutritionScaleSnapshot(
            planId = planId,
            weeklyCalories = plan.weeklyCalories,
            weeklyKcalBalance = plan.weeklyKcalBalance,
            days = days.map { NutritionDaySnapshot(it.id, it.targetKcal, it.targetProtein, it.targetCarbs, it.targetFat) },
            entries = entries.map { NutritionEntrySnapshot(it.id, it.grams, it.kcal, it.protein, it.carbs, it.fat) }
        )
        val newBalance = newWeekly - plan.tdee * 7.0
        planDao.update(plan.copy(weeklyCalories = newWeekly, weeklyKcalBalance = newBalance))
        days.forEach { day ->
            dayDao.updateTargets(
                day.id,
                day.targetKcal * ratio,
                day.targetProtein * ratio,
                day.targetCarbs * ratio,
                day.targetFat * ratio
            )
        }
        entries.forEach { entry ->
            entryDao.updateEntry(
                entry.id,
                entry.grams * ratio,
                entry.kcal * ratio,
                entry.protein * ratio,
                entry.carbs * ratio,
                entry.fat * ratio
            )
        }
        return mapOf(
            "kind" to "PLAN_SCALE",
            "snapshot" to json.encodeToString(NutritionScaleSnapshot.serializer(), snapshot)
        )
    }

    /**
     * Risolve il mesociclo indicato dalla proposta sull'id reale.
     *
     * Come per il piano, l'id puo essere inventato dall'AI: si ricade sul primo
     * mesociclo del piano di allenamento attivo, che e quello che l'utente sta
     * seguendo. Se non c'e alcun mesociclo attivo la modifica non ha senso e
     * viene rifiutata con un messaggio esplicito.
     */
    private suspend fun resolveMesocycleId(requested: Long?): Long {
        if (requested != null) {
            val activePlanId = workoutPlanDao.getActive()?.id
            if (activePlanId != null) {
                val found = mesoDao.getForPlan(activePlanId).firstOrNull { it.id == requested }
                if (found != null) return found.id
            }
            val anyPlan = workoutPlanDao.getAll().firstNotNullOfOrNull { plan ->
                mesoDao.getForPlan(plan.id).firstOrNull { it.id == requested }
            }
            if (anyPlan != null) return anyPlan.id
        }
        val active = workoutPlanDao.getActive()
            ?: throw IllegalArgumentException("Nessun piano di allenamento attivo da modificare.")
        return mesoDao.getForPlan(active.id).firstOrNull()?.id
            ?: throw IllegalArgumentException("Il piano attivo non ha un mesociclo da modificare.")
    }

    private suspend fun applyVolumeAdjustment(action: CoachAction): Map<String, String> {
        val mesocycleId = resolveMesocycleId(action.payload["mesocycleId"]?.toLongOrNull())
        val reduction = action.payload["volumeReductionPercent"]?.toDoubleOrNull() ?: 20.0
        require(reduction in -30.0..80.0) { "Variazione volume fuori dai limiti." }
        val result = mutableMapOf("kind" to "PLANNED_SETS", "mesocycleId" to mesocycleId.toString())
        workoutDayDao.getForMesocycle(mesocycleId).forEach { day ->
            plannedDao.getForDay(day.id).forEach { exercise ->
                val updated = (exercise.sets * (1.0 - reduction / 100.0)).roundToInt().coerceIn(1, 30)
                result["sets_${exercise.id}"] = exercise.sets.toString()
                plannedDao.updateSets(exercise.id, updated)
            }
        }
        return result
    }

    private suspend fun applyDeload(action: CoachAction): Map<String, String> {
        val percent = action.payload["volumeReductionPercent"]?.toDoubleOrNull() ?: 40.0
        val mesocycleId = resolveMesocycleId(action.payload["mesocycleId"]?.toLongOrNull())
        val planId = action.payload["planId"]?.toLongOrNull()
        val meso = (planId?.let { mesoDao.getForPlan(it) }
            ?: workoutPlanDao.getActive()?.let { mesoDao.getForPlan(it.id) })
            ?.firstOrNull { it.id == mesocycleId }
            ?: throw IllegalArgumentException("Mesociclo non trovato.")
        mesoDao.insert(meso.copy(isDeload = true, focus = "${meso.focus} (deload)"))

        val result = mutableMapOf(
            "kind" to "PLANNED_SETS",
            "mesocycleId" to mesocycleId.toString()
        )
        workoutDayDao.getForMesocycle(mesocycleId).forEach { day ->
            plannedDao.getForDay(day.id).forEach { ex ->
                val reduced = (ex.sets * (1 - percent / 100.0)).toInt().coerceAtLeast(1)
                result["sets_${ex.id}"] = ex.sets.toString()
                plannedDao.updateSets(ex.id, reduced)
            }
        }
        return result
    }

    // ---- Serializzazione ----

    private fun CoachAction.toProgramChange(status: ChangeStatus) = ProgramChange(
        domain = changeDomain,
        status = status,
        title = title.ifBlank { "Proposta del coach" },
        situation = situation,
        problem = problem,
        dataUsed = dataUsed,
        proposedChange = proposedChange,
        rationale = rationale,
        expectedOutcome = expectedOutcome,
        contraindications = contraindications,
        payloadJson = json.encodeToString(CoachAction.serializer(), this),
        source = "AI_COACH"
    )

    private fun decodeAction(change: ProgramChange): CoachAction? =
        runCatching { json.decodeFromString(CoachAction.serializer(), change.payloadJson) }.getOrNull()

    private fun decodeRevertPayload(change: ProgramChange): Map<String, String>? =
        runCatching { json.decodeFromString(stringMapSerializer, change.revertPayloadJson) }.getOrNull()

    private fun Map<String, String>.require(key: String): String =
        this[key]?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Parametro '$key' mancante nella proposta.")
}
