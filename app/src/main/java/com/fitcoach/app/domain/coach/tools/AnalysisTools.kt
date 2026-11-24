package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.engine.EngineParameters
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calcolo energetico deterministico come STRUMENTO subordinato: l'AI lo consulta
 * per decidere il target calorico, ma la decisione finale resta sua. Il calcolo
 * usa formule documentate e stile di vita reale (NEAT), senza inventare valori.
 */
@Singleton
class ComputeEnergyTargetsTool @Inject constructor(
    private val userRepository: UserRepository
) : CoachTool {
    override val name = "compute_energy_targets"
    override val description =
        "Calcola BMR, TDEE (con stile di vita reale), target calorico e fabbisogno proteico per " +
            "obiettivo, con la formula usata e la soglia di sicurezza. Usalo come base per decidere " +
            "la nutrizione. I valori sono STIME: distingui sempre stimato da misurato."

    override val parameters: JsonObject = Schema.obj(
        "obiettivo" to Schema.enumOf(
            "Obiettivo per il quale calcolare i target (default: quello del profilo).",
            "RECOMP", "HYPERTROPHY", "CUT", "MAINTENANCE", "STRENGTH"
        )
    )

    override suspend fun execute(arguments: JsonObject): String {
        val profile = userRepository.get()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Nessun profilo.")))
        val goalName = JsonArgs(arguments).string("obiettivo")
        val goal = goalName?.let { runCatching { com.fitcoach.app.domain.model.Goal.valueOf(it.uppercase()) }.getOrNull() }
        val effective = if (goal != null) profile.copy(goal = goal) else profile
        val energy = EnergyCalculator.compute(effective)
            ?: return JsonOut.encode(
                JsonOut.obj(
                    "ok" to JsonOut.bool(false),
                    "error" to JsonOut.str("Dati insufficienti (età/altezza/peso) per il calcolo energetico."),
                    "datiMancanti" to JsonOut.strings(profile.missingEssentialData)
                )
            )
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "obiettivoUsato" to JsonOut.str(effective.goal.name),
                "bmr" to JsonOut.num(energy.bmr),
                "tdee" to JsonOut.num(energy.tdee),
                "formulaBmr" to JsonOut.str(energy.bmrFormula),
                "formulaTdee" to JsonOut.str(energy.tdeeFormula),
                // Il fattore va esposto in chiaro. Prima il modello vedeva solo
                // il TDEE e lo attribuiva al fattore sbagliato: con 5 allenamenti
                // e 0 passi leggeva 3.348 kcal, poi proponeva di "correggerlo"
                // a 2.150 con un 1,275 inventato, sotto il pavimento FAO (1,40).
                "fattoreAttivita" to JsonOut.num(energy.activityFactor),
                // Limiti entro cui una stima resta plausibile: il pavimento e'
                // 1,2 x BMR (Black 1996, persona non deambulante), il tetto 2,40
                // (FAO). Un target fuori da qui non e' una correzione, e' un errore.
                "palUsatoInBanda" to JsonOut.bool(
                    energy.activityFactor >= EngineParameters.palPhysiologicalFloor.value &&
                        energy.activityFactor <= EngineParameters.palSustainableCeiling.value
                ),
                "palMinimoFisiologico" to JsonOut.num(EngineParameters.palPhysiologicalFloor.value),
                "palMassimoSostenibile" to JsonOut.num(EngineParameters.palSustainableCeiling.value),
                "massaMagraKg" to JsonOut.num(energy.leanMassKg),
                "kcalSettimanali" to JsonOut.num(energy.weeklyCalories),
                "proteineGPerKg" to JsonOut.num(CarbCyclingEngine.proteinPerKg(effective.goal)),
                "sogliaSicurezzaKcal" to JsonOut.num(CarbCyclingEngine.safetyFloor(effective, energy)),
                "note" to JsonOut.strings(energy.notes),
                "overridePresenti" to JsonOut.obj(
                    "activityFactor" to JsonOut.num(effective.activityFactorOverride),
                    "targetCalorico" to JsonOut.num(effective.calorieTargetOverride)
                )
            )
        )
    }
}

/**
 * Calcolo della distribuzione dei carboidrati (carb cycling o uniforme) come
 * strumento di supporto: l'AI decide SE e QUANTO applicarla, questo strumento
 * mostra i target coerenti per ogni tipologia di giornata.
 */
@Singleton
class ComputeCarbCyclingTool @Inject constructor(
    private val userRepository: UserRepository
) : CoachTool {
    override val name = "compute_carb_cycling"
    override val description =
        "Calcola la distribuzione settimanale dei carboidrati (HIGH/MEDIUM/LOW) o uniforme, con " +
            "target kcal/proteine/carboidrati/grassi per giornata e verifica di coerenza. " +
            "Usalo per decidere la ciclicita in base al carico di allenamento. Non crea deficit arbitrari."

    override val parameters: JsonObject = Schema.obj(
        "giornateHigh" to Schema.integer("Numero di giornate HIGH (opzionale: default dal profilo)."),
        "giornateMedium" to Schema.integer("Numero di giornate MEDIUM (opzionale)."),
        "giornateLow" to Schema.integer("Numero di giornate LOW (opzionale).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val profile = userRepository.get()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Nessun profilo.")))
        val energy = EnergyCalculator.compute(profile)
            ?: return JsonOut.encode(
                JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Dati insufficienti per il calcolo."))
            )
        val args = JsonArgs(arguments)
        // Le giornate richieste dal modello venivano solo ripetute nell'output:
        // il piano restava quello di default e il modello credeva di aver
        // personalizzato la settimana. Ora vengono applicate, ma solo se la
        // somma e' 7: una settimana di 5 giorni non esiste.
        val richieste = listOfNotNull(
            args.int("giornateHigh"), args.int("giornateMedium"), args.int("giornateLow")
        )
        val personalizzazione = if (richieste.size == 3 && richieste.sum() == 7 && richieste.all { it >= 0 }) {
            Triple(richieste[0], richieste[1], richieste[2])
        } else {
            null
        }
        val cycling = if (CarbCyclingEngine.shouldApplyCarbCycling(profile)) {
            if (personalizzazione != null) {
                CarbCyclingEngine.build(
                    profile, energy,
                    highDays = personalizzazione.first,
                    mediumDays = personalizzazione.second,
                    lowDays = personalizzazione.third
                )
            } else {
                CarbCyclingEngine.build(profile, energy)
            }
        } else {
            CarbCyclingEngine.buildUniform(profile, energy)
        }
        val targets = cycling.targets.map { (type, t) ->
            JsonOut.obj(
                "tipo" to JsonOut.str(type.name),
                "kcal" to JsonOut.num(t.kcal),
                "proteine" to JsonOut.num(t.protein),
                "carboidrati" to JsonOut.num(t.carbs),
                "grassi" to JsonOut.num(t.fat)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "consigliatoApplicare" to JsonOut.bool(CarbCyclingEngine.shouldApplyCarbCycling(profile)),
                "applicato" to JsonOut.bool(cycling.applied),
                "coerente" to JsonOut.bool(cycling.isValid),
                "giornateHigh" to JsonOut.num(cycling.highDays),
                "giornateMedium" to JsonOut.num(cycling.mediumDays),
                "giornateLow" to JsonOut.num(cycling.lowDays),
                "richiestaPersonalizzazione" to JsonOut.obj(
                    "high" to JsonOut.num(args.int("giornateHigh")),
                    "medium" to JsonOut.num(args.int("giornateMedium")),
                    "low" to JsonOut.num(args.int("giornateLow")),
                    "applicata" to JsonOut.bool(personalizzazione != null)
                ),
                "kcalSettimanali" to JsonOut.num(cycling.weeklyKcal),
                "target" to JsonOut.arr(targets),
                "avvisi" to JsonOut.strings(cycling.warnings)
            )
        )
    }
}

/** Calcolo del volume settimanale per gruppo muscolare (serie dirette + indirette). */
@Singleton
class ComputeWeeklyVolumeTool @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: com.fitcoach.app.data.repository.WorkoutRepository
) : CoachTool {
    override val name = "compute_weekly_volume"
    override val description =
        "Calcola il volume settimanale effettivo per gruppo muscolare (serie dirette a peso 1.0 e " +
            "indirette a peso ridotto) della scheda attiva. Restituisce anche il range " +
            "raccomandato per ogni gruppo, modulato sull'esperienza dichiarata, per valutare " +
            "equilibrio e recuperabilita."

    override val parameters: JsonObject = Schema.obj()

    override suspend fun execute(arguments: JsonObject): String {
        val plan = workoutRepository.getActivePlan()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(true), "pianoPresente" to JsonOut.bool(false)))
        val volume = plan.weeklyVolumePerGroup
        val total = volume.values.sum()
        val experience = userRepository.get()?.experience ?: com.fitcoach.app.domain.model.TrainingExperience.INTERMEDIATE
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "pianoPresente" to JsonOut.bool(true),
                "serieTotaliSettimana" to JsonOut.num(total),
                "volumePerGruppo" to JsonOut.obj(
                    *volume.map { (k, v) -> k.name to JsonOut.num(v) }.toTypedArray()
                ),
                // Gli intervalli arrivano da VolumeGuidelines, modulati sul gruppo
                // e sull'esperienza: una soglia unica per schiena e bicipiti
                // giudicava male entrambi.
                "rangePerGruppo" to JsonOut.obj(
                    *volume.map { (muscle, _) ->
                        val range = com.fitcoach.app.domain.workout.VolumeGuidelines
                            .forGroup(muscle, experience)
                        muscle.name to JsonOut.obj(
                            "min" to JsonOut.num(range.min),
                            "max" to JsonOut.num(range.max),
                            "mrv" to JsonOut.num(range.mrv)
                        )
                    }.toTypedArray()
                ),
                "sottoVolume" to JsonOut.strings(
                    volume.filter { (muscle, sets) ->
                        sets < com.fitcoach.app.domain.workout.VolumeGuidelines
                            .forGroup(muscle, experience).min
                    }.map { it.key.name }
                ),
                "sopraVolume" to JsonOut.strings(
                    volume.filter { (muscle, sets) ->
                        sets > com.fitcoach.app.domain.workout.VolumeGuidelines
                            .forGroup(muscle, experience).mrv
                    }.map { it.key.name }
                )
            )
        )
    }
}
