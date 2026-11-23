package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.domain.coach.CoachAction
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strumenti di SCRITTURA controllata. Non applicano mai le modifiche: registrano
 * una proposta (PROPOSED) con motivazione, dati usati, risultato atteso e
 * controindicazioni. L'utente conferma e l'app applica in transazione con
 * possibilita di annullamento (vedi CoachActionExecutor).
 */
abstract class WriteTool : CoachTool {
    override val isWrite: Boolean get() = true

    protected fun proposalJson(changeId: Long, message: String): String = JsonOut.encode(
        JsonOut.obj(
            "ok" to JsonOut.bool(true),
            "stato" to JsonOut.str("PROPOSTA_REGISTRATA"),
            "changeId" to JsonOut.num(changeId),
            "messaggio" to JsonOut.str(message),
            "nota" to JsonOut.str(
                "La modifica NON e applicata: verra applicata solo dopo la conferma dell'utente."
            )
        )
    )
}

/** Propone la sostituzione di un alimento in una voce del piano, a parita di macro. */
@Singleton
class SubstituteFoodTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_food_substitution"
    override val description =
        "Propone di sostituire l'alimento di una voce del piano alimentare. Registra una proposta " +
            "che l'utente conferma. Calcola prima la quantita del sostituto per mantenere kcal e macro " +
            "(usa list_foods per i valori reali). Fornisci sempre motivazione e dati usati."

    override val parameters: JsonObject = Schema.obj(
        "entryId" to Schema.integer("Id della voce del pasto (da get_active_nutrition_plan)."),
        "newFoodId" to Schema.integer("Id del nuovo alimento (da list_foods)."),
        "newFoodName" to Schema.string("Nome del nuovo alimento."),
        "grams" to Schema.number("Grammi del sostituto calcolati per mantenere i macro."),
        "kcal" to Schema.number("Kcal della nuova quantita."),
        "protein" to Schema.number("Proteine della nuova quantita."),
        "carbs" to Schema.number("Carboidrati della nuova quantita."),
        "fat" to Schema.number("Grassi della nuova quantita."),
        "title" to Schema.string("Titolo breve della proposta."),
        "situation" to Schema.string("Situazione attuale."),
        "problem" to Schema.string("Problema identificato."),
        "dataUsed" to Schema.string("Dati utilizzati per la decisione."),
        "rationale" to Schema.string("Motivazione."),
        "expectedOutcome" to Schema.string("Risultato atteso."),
        "contraindications" to Schema.string("Possibili controindicazioni."),
        required = listOf("entryId", "newFoodName", "grams", "kcal", "protein", "carbs", "fat")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val entryId = a.int("entryId") ?: return err("entryId mancante")
        val payload = mutableMapOf(
            "entryId" to entryId.toString(),
            "newFoodName" to (a.string("newFoodName") ?: return err("newFoodName mancante")),
            "grams" to (a.double("grams")?.toString() ?: return err("grams mancante")),
            "kcal" to (a.double("kcal")?.toString() ?: return err("kcal mancante")),
            "protein" to (a.double("protein")?.toString() ?: return err("protein mancante")),
            "carbs" to (a.double("carbs")?.toString() ?: return err("carbs mancante")),
            "fat" to (a.double("fat")?.toString() ?: return err("fat mancante"))
        )
        a.int("newFoodId")?.let { payload["newFoodId"] = it.toString() }
        val action = CoachAction(
            action = CoachAction.ACTION_SUBSTITUTE_FOOD,
            domain = "NUTRITION",
            title = a.string("title") ?: "Sostituzione alimento",
            situation = a.string("situation") ?: "",
            problem = a.string("problem") ?: "",
            dataUsed = a.string("dataUsed") ?: "",
            proposedChange = "Sostituire la voce $entryId con ${a.string("newFoodName")}",
            rationale = a.string("rationale") ?: "",
            expectedOutcome = a.string("expectedOutcome") ?: "",
            contraindications = a.string("contraindications") ?: "",
            payload = payload
        )
        val id = executor.registerProposal(action)
        return proposalJson(id, "Sostituzione alimento proposta.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""
}

/** Propone la sostituzione di un esercizio mantenendo lo stimolo muscolare. */
@Singleton
class SubstituteExerciseTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_exercise_substitution"
    override val description =
        "Propone di sostituire un esercizio della scheda mantenendo lo stimolo del gruppo muscolare. " +
            "Usa list_exercises per scegliere un sostituto compatibile con l'attrezzatura."

    override val parameters: JsonObject = Schema.obj(
        "plannedId" to Schema.integer("Id dell'esercizio pianificato (plannedId da get_active_workout_plan)."),
        "exerciseId" to Schema.integer("Id del nuovo esercizio dal catalogo."),
        "exerciseName" to Schema.string("Nome del nuovo esercizio."),
        "technique" to Schema.string("Nota tecnica libera (opzionale)."),
        "intensityTechnique" to Schema.string(
            "Tecnica d'intensita' da applicare: NONE, DROP_SET, REST_PAUSE, MYO_REPS, " +
                "SUPERSET, BACK_OFF_SET, CLUSTER_SET, FAILURE, ECCENTRIC_EMPHASIS."
        ),
        "techniqueReason" to Schema.string(
            "Perche' quella tecnica su questo esercizio: viene mostrato nella scheda."
        ),
        "title" to Schema.string("Titolo breve della proposta."),
        "rationale" to Schema.string("Motivazione."),
        required = listOf("plannedId", "exerciseId", "exerciseName")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val payload = mapOf(
            "plannedId" to (a.int("plannedId")?.toString() ?: return err("plannedId mancante")),
            "exerciseId" to (a.int("exerciseId")?.toString() ?: return err("exerciseId mancante")),
            "exerciseName" to (a.string("exerciseName") ?: return err("exerciseName mancante")),
            "technique" to (a.string("technique") ?: ""),
            "intensityTechnique" to (
                a.string("intensityTechnique")
                    ?.trim()?.uppercase()
                    ?.takeIf { it in INTENSITY_TECHNIQUES } ?: "NONE"
                ),
            "techniqueReason" to (a.string("techniqueReason") ?: "")
        )
        val action = CoachAction(
            action = CoachAction.ACTION_SUBSTITUTE_EXERCISE,
            domain = "WORKOUT",
            title = a.string("title") ?: "Sostituzione esercizio",
            rationale = a.string("rationale") ?: "",
            proposedChange = "Sostituire con ${a.string("exerciseName")}",
            payload = payload
        )
        return proposalJson(executor.registerProposal(action), "Sostituzione esercizio proposta.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""

    private companion object {
        /** Valori allineati a IntensityTechnique: altro non e' applicabile. */
        val INTENSITY_TECHNIQUES = setOf(
            "NONE", "DROP_SET", "REST_PAUSE", "MYO_REPS", "SUPERSET",
            "BACK_OFF_SET", "CLUSTER_SET", "FAILURE", "ECCENTRIC_EMPHASIS"
        )
    }
}

/**
 * Propone di applicare (o togliere) una tecnica d'intensita' su un esercizio
 * gia' in scheda, senza sostituire l'esercizio.
 *
 * Prima questo strumento non esisteva: chiedendo in chat "aggiungi un drop set
 * sul curl", il modello non aveva modo di registrare la modifica e si limitava
 * a rispondere che l'avrebbe fatta.
 */
@Singleton
class SetIntensityTechniqueTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_intensity_technique"
    override val description =
        "Propone di applicare una tecnica d'intensita' (drop set, rest-pause, myo-reps, superset, " +
            "back-off, cluster, cedimento, enfasi eccentrica) a un esercizio gia' presente nella " +
            "scheda. Non cambia esercizio, serie ne' carico. Usa get_active_workout_plan per il " +
            "plannedId. Consulta prima consult_invictus con area=tecniche per la metodologia."

    override val parameters: JsonObject = Schema.obj(
        "plannedId" to Schema.integer("Id dell'esercizio pianificato (da get_active_workout_plan)."),
        "intensityTechnique" to Schema.enumOf(
            "Tecnica da applicare.",
            "DROP_SET", "REST_PAUSE", "MYO_REPS", "SUPERSET", "BACK_OFF_SET",
            "CLUSTER_SET", "FAILURE", "ECCENTRIC_EMPHASIS", "NONE"
        ),
        "techniqueReason" to Schema.string(
            "Perche' quella tecnica su quel gruppo muscolare: viene mostrata nella scheda."
        ),
        "title" to Schema.string("Titolo breve della proposta."),
        "rationale" to Schema.string("Motivazione estesa."),
        "expectedOutcome" to Schema.string("Risultato atteso."),
        "contraindications" to Schema.string("Controindicazioni."),
        required = listOf("plannedId", "intensityTechnique", "techniqueReason")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val raw = a.string("intensityTechnique")?.trim()?.uppercase()
            ?: return err("intensityTechnique mancante")
        if (raw !in INTENSITY_TECHNIQUES) {
            return err(
                "Tecnica '$raw' non valida. Valori ammessi: ${INTENSITY_TECHNIQUES.joinToString()}"
            )
        }
        val reason = a.string("techniqueReason")?.trim().orEmpty()
        if (reason.isBlank()) {
            return err(
                "techniqueReason obbligatorio: senza la motivazione la tecnica non e' verificabile."
            )
        }
        val action = CoachAction(
            action = CoachAction.ACTION_SET_INTENSITY,
            domain = "WORKOUT",
            title = a.string("title") ?: "Tecnica d'intensita'",
            rationale = a.string("rationale") ?: reason,
            proposedChange = "Applicare $raw",
            expectedOutcome = a.string("expectedOutcome") ?: "",
            contraindications = a.string("contraindications") ?: "",
            payload = mapOf(
                "plannedId" to (a.int("plannedId")?.toString() ?: return err("plannedId mancante")),
                "intensityTechnique" to raw,
                "techniqueReason" to reason
            )
        )
        return proposalJson(executor.registerProposal(action), "Tecnica d'intensita' proposta.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""

    private companion object {
        /** Allineati a IntensityTechnique: altro non sarebbe applicabile. */
        val INTENSITY_TECHNIQUES = setOf(
            "NONE", "DROP_SET", "REST_PAUSE", "MYO_REPS", "SUPERSET",
            "BACK_OFF_SET", "CLUSTER_SET", "FAILURE", "ECCENTRIC_EMPHASIS"
        )
    }
}

/** Propone lo spostamento di una seduta a un altro giorno della settimana. */
@Singleton
class MoveWorkoutTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_move_workout"
    override val description =
        "Propone di spostare una seduta a un altro giorno della settimana " +
            "(0=lunedi .. 6=domenica). Se il giorno e' occupato da un'altra seduta, le due " +
            "si scambiano. Usa get_active_workout_plan per leggere il campo 'giorno' di " +
            "ogni seduta."

    override val parameters: JsonObject = Schema.obj(
        "dayId" to Schema.integer(
            "Id della seduta da spostare (plannedId no: e' l'id della workout_day, " +
                "cioe' il campo 'giorno' di get_active_workout_plan)."
        ),
        "newDayIndex" to Schema.integer("Nuovo giorno, 0 (lunedi) .. 6 (domenica)."),
        "title" to Schema.string("Titolo breve della proposta."),
        "rationale" to Schema.string("Motivazione."),
        required = listOf("dayId", "newDayIndex")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val payload = mapOf(
            "dayId" to (a.int("dayId")?.toString() ?: return err("dayId mancante")),
            "newDayIndex" to (a.int("newDayIndex")?.toString() ?: return err("newDayIndex mancante"))
        )
        val action = CoachAction(
            action = CoachAction.ACTION_MOVE_WORKOUT,
            domain = "WORKOUT",
            title = a.string("title") ?: "Spostamento seduta",
            rationale = a.string("rationale") ?: "",
            proposedChange = "Spostare la seduta al giorno ${a.int("newDayIndex")}",
            payload = payload
        )
        return proposalJson(executor.registerProposal(action), "Spostamento seduta proposto.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""
}

/** Propone un aggiustamento calorico settimanale entro i limiti di sicurezza. */
@Singleton
class AdjustCaloriesTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_calorie_adjustment"
    override val description =
        "Propone di modificare il target calorico settimanale. La variazione e limitata al +/-10% " +
            "e non puo scendere sotto la soglia di sicurezza (85% del BMR). Usa compute_energy_targets " +
            "per i valori di riferimento e giustifica con dati reali (peso, aderenza, prestazioni)."

    override val parameters: JsonObject = Schema.obj(
        "planId" to Schema.integer("Id del piano alimentare attivo."),
        "weeklyKcal" to Schema.number("Nuovo totale calorico settimanale."),
        "title" to Schema.string("Titolo breve della proposta."),
        "situation" to Schema.string("Situazione attuale."),
        "problem" to Schema.string("Problema identificato."),
        "dataUsed" to Schema.string("Dati utilizzati."),
        "rationale" to Schema.string("Motivazione."),
        "expectedOutcome" to Schema.string("Risultato atteso."),
        "contraindications" to Schema.string("Controindicazioni."),
        required = listOf("planId", "weeklyKcal")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val payload = mapOf(
            "planId" to (a.int("planId")?.toString() ?: return err("planId mancante")),
            "weeklyKcal" to (a.double("weeklyKcal")?.toString() ?: return err("weeklyKcal mancante"))
        )
        val action = CoachAction(
            action = CoachAction.ACTION_ADJUST_CALORIES,
            domain = "NUTRITION",
            title = a.string("title") ?: "Aggiustamento calorico",
            situation = a.string("situation") ?: "",
            problem = a.string("problem") ?: "",
            dataUsed = a.string("dataUsed") ?: "",
            proposedChange = "Nuovo target settimanale: ${a.double("weeklyKcal")} kcal",
            rationale = a.string("rationale") ?: "",
            expectedOutcome = a.string("expectedOutcome") ?: "",
            contraindications = a.string("contraindications") ?: "",
            payload = payload
        )
        return proposalJson(executor.registerProposal(action), "Aggiustamento calorico proposto.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""
}

/** Propone una variazione del volume (riduzione del numero di serie). */
@Singleton
class AdjustVolumeTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_volume_adjustment"
    override val description =
        "Propone di modificare il volume (numero di serie) di un mesociclo. Usalo quando i dati " +
            "mostrano affaticamento eccessivo o stagnazione con volume alto. Non aumentare il volume " +
            "automaticamente ad ogni stagnazione."

    override val parameters: JsonObject = Schema.obj(
        "mesocycleId" to Schema.integer("Id del mesociclo."),
        "volumeReductionPercent" to Schema.number("Percentuale di variazione (positivo riduce, default 20)."),
        "title" to Schema.string("Titolo breve."),
        "rationale" to Schema.string("Motivazione."),
        required = listOf("mesocycleId")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val payload = mapOf(
            "mesocycleId" to (a.int("mesocycleId")?.toString() ?: return err("mesocycleId mancante")),
            "volumeReductionPercent" to (a.double("volumeReductionPercent")?.toString() ?: "20.0")
        )
        val action = CoachAction(
            action = CoachAction.ACTION_ADJUST_VOLUME,
            domain = "WORKOUT",
            title = a.string("title") ?: "Aggiustamento volume",
            rationale = a.string("rationale") ?: "",
            proposedChange = "Riduzione volume del ${payload["volumeReductionPercent"]}%",
            payload = payload
        )
        return proposalJson(executor.registerProposal(action), "Aggiustamento volume proposto.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""
}

/** Propone un deload (settimana di scarico) giustificato da calo di prestazioni/affaticamento. */
@Singleton
class ProposeDeloadTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_deload"
    override val description =
        "Propone una settimana di deload (riduzione del volume) su un mesociclo, quando i dati " +
            "mostrano calo di prestazioni, RIR troppo basso o recupero insufficiente."

    override val parameters: JsonObject = Schema.obj(
        "mesocycleId" to Schema.integer("Id del mesociclo."),
        "planId" to Schema.integer("Id del piano di allenamento."),
        "volumeReductionPercent" to Schema.number("Riduzione volume (default 40)."),
        "title" to Schema.string("Titolo breve."),
        "rationale" to Schema.string("Motivazione."),
        required = listOf("mesocycleId")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val payload = buildMap {
            put("mesocycleId", a.int("mesocycleId")?.toString() ?: return err("mesocycleId mancante"))
            a.int("planId")?.let { put("planId", it.toString()) }
            put("volumeReductionPercent", a.double("volumeReductionPercent")?.toString() ?: "40.0")
        }
        val action = CoachAction(
            action = CoachAction.ACTION_DELOAD,
            domain = "WORKOUT",
            title = a.string("title") ?: "Deload",
            rationale = a.string("rationale") ?: "",
            proposedChange = "Settimana di scarico con volume ridotto",
            payload = payload
        )
        return proposalJson(executor.registerProposal(action), "Deload proposto.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""
}

/**
 * Propone la RIGENERAZIONE STRUTTURALE di una parte del programma (o di entrambe)
 * attraverso il motore unico (ProgramGenerationCoordinator + submit_full_program).
 *
 * Va usato SOLO quando la richiesta cambia la struttura del programma (nuova split,
 * numero di giorni di allenamento, giorni HIGH/MEDIUM/LOW, nuova strategia) e non
 * basta una modifica puntuale. Per una singola sostituzione, uno spostamento o un
 * aggiustamento usare gli strumenti "propose_*" specifici: sono piu mirati.
 *
 * Come tutti gli strumenti di scrittura, NON applica nulla: registra una proposta
 * che l'utente conferma. Alla conferma la rigenerazione passa dallo stesso motore
 * AI usato per la generazione iniziale (nessun secondo motore).
 */
@Singleton
class ProposeRegeneratePlanTool @Inject constructor(
    private val executor: CoachActionExecutor
) : WriteTool() {
    override val name = "propose_regenerate_plan"
    override val description =
        "Propone di rigenerare la struttura del programma tramite il motore unico: scegli l'ambito " +
            "WORKOUT (solo scheda), NUTRITION (solo piano alimentare) o BOTH (entrambi). Usalo quando " +
            "la richiesta cambia la struttura (split, giorni di allenamento, carb cycling, strategia), " +
            "non per una singola sostituzione o spostamento. Registra una proposta che l'utente conferma: " +
            "la rigenerazione avviene solo dopo la conferma, con possibilita di annullamento."

    override val parameters: JsonObject = Schema.obj(
        "scope" to Schema.enumOf(
            "Cosa rigenerare: WORKOUT (solo allenamento), NUTRITION (solo nutrizione) o BOTH (entrambi).",
            "WORKOUT", "NUTRITION", "BOTH"
        ),
        "title" to Schema.string("Titolo breve della proposta."),
        "situation" to Schema.string("Situazione attuale."),
        "problem" to Schema.string("Problema identificato."),
        "dataUsed" to Schema.string("Dati reali utilizzati (anamnesi, piani, storico)."),
        "proposedChange" to Schema.string("Cosa cambiera nella struttura."),
        "rationale" to Schema.string("Motivazione."),
        "expectedOutcome" to Schema.string("Risultato atteso."),
        "contraindications" to Schema.string("Possibili controindicazioni."),
        required = listOf("scope")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val scope = (a.string("scope") ?: "BOTH").trim().uppercase()
        if (scope !in REGENERATE_SCOPES) return err("scope non valido: usa WORKOUT, NUTRITION o BOTH.")
        val payload = mapOf("scope" to scope)
        val action = CoachAction(
            action = CoachAction.ACTION_REGENERATE_PLAN,
            domain = scope,
            title = a.string("title") ?: "Rigenerazione programma ($scope)",
            situation = a.string("situation") ?: "",
            problem = a.string("problem") ?: "",
            dataUsed = a.string("dataUsed") ?: "",
            proposedChange = a.string("proposedChange") ?: "Rigenerare la parte richiesta del programma.",
            rationale = a.string("rationale") ?: "",
            expectedOutcome = a.string("expectedOutcome") ?: "",
            contraindications = a.string("contraindications") ?: "",
            payload = payload
        )
        return proposalJson(executor.registerProposal(action), "Rigenerazione proposta.")
    }

    private fun err(m: String) = """{"ok":false,"error":"$m"}"""

    private companion object {
        val REGENERATE_SCOPES = setOf("WORKOUT", "NUTRITION", "BOTH")
    }
}
