package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validatore deterministico di una scheda proposta. E il vincolo che l'AI DEVE
 * soddisfare: verifica copertura dei gruppi previsti, volume, attrezzatura,
 * limitazioni e sicurezza. L'AI usa l'esito per correggere la propria proposta.
 *
 * Questo strumento non genera la scheda: la giudica soltanto, delegando alla
 * logica pura di [WorkoutPlanValidator].
 */
@Singleton
class ValidateWorkoutPlanTool @Inject constructor(
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository
) : CoachTool {
    override val name = "validate_workout_plan"

    override val description =
        "Valida una scheda di allenamento proposta PRIMA di salvarla. Controlla: copertura dei " +
            "gruppi muscolari previsti, volume settimanale per gruppo, compatibilita con attrezzatura " +
            "e limitazioni, uso di esercizi realmente esistenti nel catalogo, sicurezza (numero di " +
            "serie/giorno). Restituisce ok=true o una lista di violazioni da correggere."

    override val parameters: JsonObject = Schema.obj(
        "giornate" to Schema.array(
            "Giornate di allenamento. Ogni giornata: {dayIndex (0-6), gruppiMuscolari:[nomi enum], esercizi:[{exerciseId, serie}]}.",
            Schema.obj(
                "dayIndex" to Schema.integer("Giorno 0..6 (0=lunedi)."),
                "gruppiMuscolari" to Schema.arrayOfStrings("Gruppi muscolari previsti per la giornata."),
                "esercizi" to Schema.array(
                    "Esercizi della giornata.",
                    Schema.obj(
                        "exerciseId" to Schema.integer("Id esercizio dal catalogo globale."),
                        "serie" to Schema.integer("Numero di serie.")
                    )
                )
            )
        )
    )

    override suspend fun execute(arguments: JsonObject): String {
        val profile = userRepository.get()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Nessun profilo.")))
        val catalog = exerciseRepository.getAll()
        val days = JsonArgs(arguments).arrayAt("giornate")
            ?: return JsonOut.encode(
                JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Campo 'giornate' mancante o non valido."))
            )

        val proposals = days.mapNotNull { dayEl ->
            val day = dayEl as? JsonObject ?: return@mapNotNull null
            val dayIndex = (day["dayIndex"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val groups = (day["gruppiMuscolari"] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
                ?.mapNotNull { runCatching { MuscleGroup.valueOf(it.uppercase()) }.getOrNull() }
                ?.toSet()
                ?: emptySet()
            val exercises = (day["esercizi"] as? kotlinx.serialization.json.JsonArray).orEmpty()
                .mapNotNull { exEl ->
                    val exObj = exEl as? JsonObject ?: return@mapNotNull null
                    val id = (exObj["exerciseId"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
                    val sets = (exObj["serie"] as? JsonPrimitive)?.intOrNull ?: 0
                    WorkoutPlanValidator.ExerciseProposal(id.toLong(), sets)
                }
            WorkoutPlanValidator.DayProposal(dayIndex, groups, exercises)
        }

        val report = WorkoutPlanValidator.validate(profile, catalog, proposals)
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(report.ok),
                "violazioni" to JsonOut.strings(report.violations),
                "numeroViolazioni" to JsonOut.num(report.violations.size),
                // Gli avvisi non impediscono il salvataggio: sono spunti da
                // motivare nel rationale citando le fonti del corpus.
                "avvisi" to JsonOut.strings(report.warnings),
                "numeroAvvisi" to JsonOut.num(report.warnings.size),
                "volumeEffettivoPerGruppo" to JsonOut.obj(
                    *report.effectiveVolume.map { (k, v) -> k.name to JsonOut.num(v) }.toTypedArray()
                ),
                "gruppiCoperti" to JsonOut.strings(report.coveredGroups.map { it.name })
            )
        )
    }
}

/** Calcolo dello stimatore 1RM (Epley) come strumento di supporto alla progressione. */
@Singleton
class EstimateOneRepMaxTool @Inject constructor() : CoachTool {
    override val name = "estimate_1rm"
    override val description =
        "Stima il massimale teorico (1RM) da un carico e dalle ripetizioni eseguite (formula di " +
            "Epley) e propone il carico per un range di ripetizioni. Usalo per valutare la progressione."

    override val parameters: JsonObject = Schema.obj(
        "caricoKg" to Schema.number("Carico sollevato in kg."),
        "ripetizioni" to Schema.integer("Ripetizioni eseguite con quel carico."),
        "ripTarget" to Schema.integer("Ripetizioni target per il nuovo carico (opzionale).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val args = JsonArgs(arguments)
        val load = args.double("caricoKg")
        val reps = args.int("ripetizioni")
        if (load == null || reps == null || load <= 0 || reps <= 0) {
            return JsonOut.encode(
                JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Servono caricoKg e ripetizioni positivi."))
            )
        }
        val oneRm = load * (1 + reps / 30.0)
        val target = args.int("ripTarget")
        val suggested = target?.takeIf { it > 0 }?.let { oneRm / (1 + it / 30.0) }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "stima1Rm" to JsonOut.num(oneRm),
                "caricoProposto" to JsonOut.num(suggested),
                "ripTarget" to JsonOut.num(target),
                "formula" to JsonOut.str("Epley: 1RM = carico * (1 + rip/30)")
            )
        )
    }
}
