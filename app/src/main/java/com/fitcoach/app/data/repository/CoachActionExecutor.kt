package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.nutrition.FoodFilter
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

    private companion object {
        /**
         * Tolleranza sul rapporto calorico. Copre l'arrotondamento di un +/-10%
         * calcolato su un piano con decimali: senza, il confine inclusivo non basta.
         */
        const val RATIO_EPSILON = 0.001

        /** Limite di una singola voce di pasto: oltre non e' una porzione. */
        const val MAX_ENTRY_GRAMS = 5_000.0
        const val MAX_ENTRY_KCAL = 10_000.0

        /**
         * Scarto ammesso fra kcal dichiarate e 4-4-9 dei macro. Sul catalogo
         * reale la deviazione massima e' del 24% (vegetali, per le fibre).
         */
        const val MACRO_KCAL_REL_TOLERANCE = 0.25
        const val MACRO_KCAL_ABS_TOLERANCE = 30.0

        /** Tolleranza in kcal sul floor di sicurezza (arrotondamenti del modello). */
        const val KCAL_EPSILON = 1.0
    }

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

        // La direttiva e' il motivo per cui l'utente ha chiesto la modifica:
        // senza di essa la rigenerazione riparte dagli stessi dati di anamnesi
        // e restituisce la stessa scheda, pur risultando "applicata".
        val directive = action.payload["directive"]?.takeIf { it.isNotBlank() }
            ?: return ApplyResult.Failed(
                "Questa proposta non contiene la richiesta da applicare (directive): " +
                    "rigenerare senza istruzioni produrrebbe lo stesso programma. " +
                    "Rifai la richiesta al Coach indicando cosa deve cambiare."
            )

        val beforeSignature = workoutSignature(before.workoutPlanId)

        val result = try {
            generationCoordinator.get().generate(profile, scope, directive)
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
                // Una rigenerazione che restituisce la stessa scheda non e' una
                // modifica: dirlo "applicata" e' esattamente il falso successo
                // che l'utente vedeva. Meglio segnalarlo, cosi' la richiesta si
                // puo' riformulare invece di credere che sia andata.
                val afterSignature = workoutSignature(result.workoutPlanId)
                val unchangedWorkout = scope != ProgramGenerationCoordinator.Scope.NUTRITION &&
                    beforeSignature != null && beforeSignature == afterSignature
                if (unchangedWorkout) {
                    ApplyResult.Failed(
                        "La rigenerazione ha prodotto una scheda identica alla precedente: la richiesta " +
                            "non e' stata applicata. Riformulala indicando cosa deve cambiare " +
                            "(esercizi, serie, tecniche d'intensita' per gruppo muscolare).$extra"
                    )
                } else {
                    ApplyResult.Applied("Programma rigenerato (${scope.name}).$extra")
                }
            }
            is ProgramGenerationResult.MissingData ->
                ApplyResult.Failed("Dati mancanti per rigenerare: ${result.missing.joinToString()}.")
            is ProgramGenerationResult.AnamnesisNotConfirmed ->
                ApplyResult.Failed("Anamnesi non confermata: impossibile rigenerare il programma.")
            is ProgramGenerationResult.Error -> ApplyResult.Failed(result.message)
        }
    }

    /**
     * Firma strutturale della scheda attiva: esercizi pianificati con serie,
     * ripetizioni, RIR e tecnica d'intensita'. Serve a distinguere una
     * rigenerazione reale da una copia identica.
     */
    private suspend fun workoutSignature(planId: Long?): String? {
        val id = planId ?: return null
        val mesocycles = mesoDao.getForPlan(id)
        if (mesocycles.isEmpty()) return null
        val days = workoutDayDao.getForMesocycles(mesocycles.map { it.id }).sortedBy { it.dayIndex }
        // L'id del giorno cambia a ogni rigenerazione: la firma deve usare
        // l'indice settimanale, altrimenti due schede identiche risultano sempre
        // diverse e il confronto non segnala nulla.
        val dayIndexById = days.associate { it.id to it.dayIndex }
        val exercises = plannedDao.getForDays(days.map { it.id })
            .sortedWith(compareBy({ dayIndexById[it.workoutDayId] ?: Int.MAX_VALUE }, { it.orderIndex }))
        if (exercises.isEmpty()) return null
        return exercises.joinToString("|") { item ->
            "${dayIndexById[item.workoutDayId]}:${item.exerciseName}:" +
                "${item.sets}x${item.minReps}-${item.maxReps}" +
                ":rir${item.targetRir}:${item.intensityTechnique}"
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
            "PLANNED_SETS" -> {
                revert.filterKeys { it.startsWith("sets_") }.forEach { (key, value) ->
                    val plannedId = key.removePrefix("sets_").toLong()
                    plannedDao.updateSets(plannedId, value.toInt())
                }
                // Un deload marca anche il mesociclo: annullarlo senza togliere
                // il flag lascerebbe la scheda per sempre "in deload".
                val focusPrecedente = revert["previousFocus"]
                val mesocycleId = revert["mesocycleId"]?.toLongOrNull()
                if (focusPrecedente != null && mesocycleId != null) {
                    mesoDao.updateDeload(
                        id = mesocycleId,
                        isDeload = revert["wasDeload"]?.toBoolean() ?: false,
                        focus = focusPrecedente
                    )
                }
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
            "WORKOUT_DAY_MOVE" -> db.withTransaction {
                val dayId = revert.require("dayId").toLong()
                val previous = revert.require("previousDayIndex").toInt()
                val day = workoutDayDao.getById(dayId)
                    ?: throw IllegalStateException("Seduta da ripristinare non trovata.")
                val swappedId = revert["swappedDayId"]?.toLongOrNull()
                val swappedIndex = revert["swappedToIndex"]?.toIntOrNull()
                if (swappedId != null && swappedIndex != null) {
                    workoutDayDao.updateDayIndex(dayId, 7)
                    workoutDayDao.updateDayIndex(dayId, previous)
                    workoutDayDao.getById(swappedId)?.let {
                        workoutDayDao.updateDayIndex(it.id, swappedIndex)
                    }
                } else {
                    workoutDayDao.updateDayIndex(dayId, previous)
                }
            }
            // Ripristino del vecchio formato, ancora presente nelle modifiche
            // registrate prima della correzione: non va perso nello storico.
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
                // `insert` usa REPLACE e workout_plans ha onDelete CASCADE su
                // mesocycles: riattivare il piano con REPLACE ne cancellava
                // l'intera struttura, lasciando l'utente con una scheda vuota.
                workoutPlanDao.update(previous.copy(isActive = true))
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

        // Allergie ed esclusioni valgono anche in chat. La generazione filtra il
        // catalogo, ma la sostituzione puntuale no: un allergico al lattosio
        // riceveva lo yogurt perche' il modello lo ha proposto.
        val profilo = userRepository.get()
        if (profilo != null) {
            val candidato = db.foodDao().getAll().firstOrNull { it.name == newFoodName }
                ?.let { com.fitcoach.app.data.local.Mappers.run { it.toDomain() } }
            require(candidato == null || FoodFilter.isAllowed(candidato, profilo)) {
                "$newFoodName non e' compatibile con allergie, esclusioni o preferenze del profilo."
            }
        }
        val grams = action.payload.require("grams").toDouble()
        val kcal = action.payload.require("kcal").toDouble()
        val protein = action.payload.require("protein").toDouble()
        val carbs = action.payload.require("carbs").toDouble()
        val fat = action.payload.require("fat").toDouble()

        // I numeri della proposta li scrive il modello, ma li conferma l'utente:
        // se non sono validati, l'utente approva una porzione da -250 g o da
        // -500 kcal senza poterlo vedere. Il resto del file valida gia' (vedi
        // applyCalorieAdjustment): qui mancava.
        require(grams.isFinite() && kcal.isFinite() && protein.isFinite() &&
            carbs.isFinite() && fat.isFinite()) {
            "La sostituzione contiene valori non finiti."
        }
        require(grams > 0.0) { "La quantita' deve essere positiva." }
        require(grams in 1.0..MAX_ENTRY_GRAMS) {
            "La quantita' deve stare fra 1 e ${MAX_ENTRY_GRAMS.toInt()} g."
        }
        require(kcal >= 0.0 && protein >= 0.0 && carbs >= 0.0 && fat >= 0.0) {
            "Calorie e macronutrienti non possono essere negativi."
        }
        require(kcal <= MAX_ENTRY_KCAL) {
            "Una singola voce non puo' superare ${MAX_ENTRY_KCAL.toInt()} kcal."
        }
        // 4-4-9 sul catalogo reale non torna per i vegetali: le fibre pesano
        // nelle kcal ma non nei carboidrati, e broccoli e zucchine arrivano al
        // 24% di scarto. Serve quindi una tolleranza assoluta oltre a quella
        // relativa, altrimenti si rifiutano alimenti veri. Il controllo resta
        // utile per il caso che conta: 500 kcal con zero macro.
        val kcalDaMacro = protein * 4.0 + carbs * 4.0 + fat * 9.0
        val tolleranza = maxOf(MACRO_KCAL_ABS_TOLERANCE, kcalDaMacro * MACRO_KCAL_REL_TOLERANCE)
        require(kcalDaMacro > 0.0 && kotlin.math.abs(kcalDaMacro - kcal) <= tolleranza) {
            "Le calorie dichiarate ($kcal) non corrispondono ai macronutrienti (%.0f kcal)."
                .format(kcalDaMacro)
        }

        val updated = entryDao.replaceEntry(
            entryId, newFoodId, newFoodName, grams, kcal, protein, carbs, fat
        )
        check(updated > 0) { "Il pasto non e' stato aggiornato: voce $entryId inesistente." }

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
            ?: throw IllegalArgumentException("Esercizio pianificato $plannedId non trovato.")
        // L'id puo essere inventato dal modello. Se l'UPDATE non tocca righe la
        // scheda resta identica: senza questo controllo apply() rispondeva
        // "Modifica applicata" e l'utente vedeva la scheda invariata.
        val updated = plannedDao.substituteExercise(
            plannedId, exerciseId, name, technique, intensity, techniqueReason
        )
        check(updated > 0) { "La scheda non e' stata aggiornata: esercizio $plannedId inesistente." }
        return mapOf(
            "kind" to "PLANNED_EXERCISE",
            "plannedId" to plannedId.toString(),
            "weight" to (existing.targetWeightKg?.toString() ?: ""),
            "reps" to (existing.targetReps?.toString() ?: ""),
            "exerciseId" to existing.exerciseId.toString(),
            "exerciseName" to existing.exerciseName,
            "technique" to existing.technique,
            "intensityTechnique" to existing.intensityTechnique.name,
            "techniqueReason" to existing.techniqueReason
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
        // Stesso parser tollerante della generazione: qui il valueOf esatto
        // rifiutava "DROP SET" con un'eccezione che l'utente vedeva come
        // "impossibile applicare la modifica" dopo averla confermata.
        val technique = IntensityTechnique.parse(action.payload["intensityTechnique"])
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

    /**
     * Sposta una seduta nel calendario scrivendo `workout_days.dayIndex`, che e'
     * il campo che dashboard e scheda leggono davvero. Scrivere
     * `nutrition_days.workoutDayIndex` produceva una conferma senza effetto
     * visibile, perche' nessuna schermata legge quel campo.
     *
     * Se il giorno di destinazione e' occupato da un'altra seduta, le due si
     * scambiano: altrimenti la scheda mostrerebbe due allenamenti lo stesso
     * giorno e il conteggio delle sedute settimanali salirebbe.
     */
    private suspend fun applyMoveWorkout(action: CoachAction): Map<String, String> {
        val dayId = action.payload.require("dayId").toLong()
        val newIndex = action.payload.require("newDayIndex").toIntOrNull()
            ?: throw IllegalArgumentException("Giorno di destinazione non valido.")
        if (newIndex !in 0..6) {
            throw IllegalArgumentException(
                "Giorno $newIndex fuori settimana: i valori ammessi sono 0 (lunedi) .. 6 (domenica)."
            )
        }
        return db.withTransaction {
            val day = workoutDayDao.getById(dayId)
                ?: throw IllegalArgumentException("Seduta $dayId non trovata nella scheda attiva.")
            val previous = day.dayIndex
            if (previous == newIndex) return@withTransaction mapOf(
                "kind" to "WORKOUT_DAY_MOVE",
                "dayId" to dayId.toString(),
                "previousDayIndex" to previous.toString(),
                "swappedDayId" to "",
                "swappedToIndex" to ""
            )

            val target = workoutDayDao.getForMesocycle(day.mesocycleId)
                .firstOrNull { it.dayIndex == newIndex && it.id != dayId }

            // Lo scambio passa da un indice provvisorio (7) per non violare
            // nessun vincolo di unicita' durante l'aggiornamento intermedio.
            // Si usa updateDayIndex: insertAll e' un REPLACE, e la cascata su
            // planned_exercises cancellerebbe gli esercizi delle due sedute.
            target?.let { workoutDayDao.updateDayIndex(it.id, 7) }
            workoutDayDao.updateDayIndex(dayId, newIndex)
            target?.let { workoutDayDao.updateDayIndex(it.id, previous) }

            mapOf(
                "kind" to "WORKOUT_DAY_MOVE",
                "dayId" to dayId.toString(),
                "previousDayIndex" to previous.toString(),
                "swappedDayId" to (target?.id?.toString() ?: ""),
                "swappedToIndex" to (if (target != null) previous.toString() else "")
            )
        }
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
        // Il confine e' inclusivo ma va assorbito l'arrotondamento di chi calcola il
        // 10% a mano. Il piano ha decimali (es. 17637.769604): un "-10%" arrotondato
        // a 15873 da' 0.899944 e veniva rifiutato con "la variazione massima e del
        // 10%", cioe' l'utente si vedeva negare esattamente cio' che aveva chiesto.
        require(ratio in (0.90 - RATIO_EPSILON)..(1.10 + RATIO_EPSILON)) {
            "La variazione calorica massima è del 10%."
        }
        if (plan.bmr > 0.0) {
            require(newWeekly / 7.0 >= plan.bmr * 0.85 - KCAL_EPSILON) {
                "Il target scenderebbe sotto la soglia di sicurezza."
            }
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
        var touched = 0
        workoutDayDao.getForMesocycle(mesocycleId).forEach { day ->
            plannedDao.getForDay(day.id).forEach { exercise ->
                val updated = (exercise.sets * (1.0 - reduction / 100.0)).roundToInt().coerceIn(1, 30)
                result["sets_${exercise.id}"] = exercise.sets.toString()
                touched += plannedDao.updateSets(exercise.id, updated)
            }
        }
        check(touched > 0) { "Nessuna serie aggiornata: il mesociclo $mesocycleId non ha esercizi." }
        return result
    }

    private suspend fun applyDeload(action: CoachAction): Map<String, String> {
        val percent = action.payload["volumeReductionPercent"]?.toDoubleOrNull() ?: 40.0
        // Stesso limite di applyVolumeAdjustment: una percentuale negativa
        // AUMENTEREBBE il volume in un deload, cioe' nel momento in cui
        // l'utente e' piu' affaticato.
        require(percent in 0.0..80.0) {
            "Riduzione volume del $percent% fuori dai limiti (0..80)."
        }
        val mesocycleId = resolveMesocycleId(action.payload["mesocycleId"]?.toLongOrNull())
        val planId = action.payload["planId"]?.toLongOrNull()
        val meso = (planId?.let { mesoDao.getForPlan(it) }
            ?: workoutPlanDao.getActive()?.let { mesoDao.getForPlan(it.id) })
            ?.firstOrNull { it.id == mesocycleId }
            ?: throw IllegalArgumentException("Mesociclo non trovato.")
        mesoDao.updateDeload(mesocycleId, isDeload = true, focus = "${meso.focus} (deload)")

        val result = mutableMapOf(
            "kind" to "PLANNED_SETS",
            "mesocycleId" to mesocycleId.toString(),
            "previousFocus" to meso.focus,
            "wasDeload" to meso.isDeload.toString()
        )
        var touched = 0
        workoutDayDao.getForMesocycle(mesocycleId).forEach { day ->
            plannedDao.getForDay(day.id).forEach { ex ->
                val reduced = (ex.sets * (1 - percent / 100.0)).toInt().coerceAtLeast(1)
                result["sets_${ex.id}"] = ex.sets.toString()
                touched += plannedDao.updateSets(ex.id, reduced)
            }
        }
        check(touched > 0) { "Nessuna serie aggiornata: il mesociclo $mesocycleId non ha esercizi." }
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
