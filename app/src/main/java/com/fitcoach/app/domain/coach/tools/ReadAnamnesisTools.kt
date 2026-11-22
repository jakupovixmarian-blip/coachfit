package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Restituisce l'anamnesi completa del soggetto in forma strutturata: e la base
 * su cui l'AI ragiona per decidere il piano alimentare e di allenamento.
 */
@Singleton
class GetAnamnesisTool @Inject constructor(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository
) : CoachTool {
    override val name = "get_anamnesis"

    override val description =
        "Restituisce l'anamnesi COMPLETA del soggetto: dati antropometrici, obiettivi, " +
            "stile di vita, recupero, preferenze e vincoli alimentari, dettagli di allenamento. " +
            "Usalo SEMPRE come primo passo per ragionare sul piano piu adatto. Include anche i " +
            "conteggi di alimenti ed esercizi disponibili nei cataloghi globali."

    override val parameters: JsonObject = Schema.obj()

    override suspend fun execute(arguments: JsonObject): String {
        val p = userRepository.get()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Nessun profilo.")))
        val foodCount = foodRepository.count()
        val exerciseCount = exerciseRepository.count()
        return JsonOut.encode(profileJson(p, foodCount, exerciseCount))
    }

    private fun profileJson(p: UserProfile, foodCount: Int, exerciseCount: Int) = JsonOut.obj(
        "ok" to JsonOut.bool(true),
        "anamnesiConfermata" to JsonOut.bool(p.anamnesisCompleted),
        "datiMancantiEssenziali" to JsonOut.strings(p.missingEssentialData),
        "contraddizioni" to JsonOut.strings(p.anamnesisContradictions),
        // Provenienza dei dati: senza questo elenco l'AI non puo distinguere una
        // risposta reale da un default del profilo (obiettivo RECOMP, lavoro
        // SEDENTARY, 5 pasti, esperienza BEGINNER) e deciderebbe su dati mai
        // forniti. I campi NON elencati qui vanno trattati come non disponibili.
        "campiDichiaratiDallUtente" to JsonOut.strings(
            AnamnesisEngine.answeredIds(p).sorted()
        ),
        "anagrafica" to JsonOut.obj(
            "nome" to JsonOut.str(p.name),
            "eta" to JsonOut.num(p.ageYears),
            "sesso" to JsonOut.str(p.sex.name),
            "altezzaCm" to JsonOut.num(p.heightCm),
            "pesoKg" to JsonOut.num(p.weightKg),
            "massaGrassaPerc" to JsonOut.num(p.bodyFatPercent),
            "bmi" to JsonOut.num(p.bmi)
        ),
        "obiettivi" to JsonOut.obj(
            "principale" to JsonOut.str(p.goal.name),
            "secondari" to JsonOut.strings(p.secondaryGoals.map { it.name }),
            "risultatoDesiderato" to JsonOut.str(p.desiredOutcome),
            "orizzonteSettimane" to JsonOut.num(p.targetTimelineWeeks)
        ),
        "stileDiVita" to JsonOut.obj(
            "tipoLavoro" to JsonOut.str(p.workType.name),
            "turni" to JsonOut.str(p.workShift.name),
            "oreLavoro" to JsonOut.num(p.workHoursPerDay),
            "oreSeduto" to JsonOut.num(p.seatedHoursPerDay),
            "passiGiornalieri" to JsonOut.num(p.dailySteps),
            "trasporto" to JsonOut.str(p.transportMode.name),
            "sportExtraSettimana" to JsonOut.num(p.extraSportSessionsPerWeek),
            "camminataMinutiSettimana" to JsonOut.num(p.walkingMinutesPerWeek)
        ),
        "recupero" to JsonOut.obj(
            "oreSonno" to JsonOut.num(p.sleepHours),
            "qualitaSonno" to JsonOut.str(p.sleepQuality.name),
            "orarioSonno" to JsonOut.str(p.bedtime),
            "orarioSveglia" to JsonOut.str(p.wakeTime),
            "stress" to JsonOut.str(p.stressLevel.name),
            "energia" to JsonOut.str(p.dailyEnergy.name),
            "capacitaRecupero" to JsonOut.str(p.recoveryCapacity.name),
            "idratazioneLitri" to JsonOut.num(p.hydrationLitersPerDay),
            "caffeinaTazze" to JsonOut.num(p.caffeineCupsPerDay),
            "alcolUnitaSettimana" to JsonOut.num(p.alcoholUnitsPerWeek)
        ),
        "alimentazione" to JsonOut.obj(
            "pastiAlGiorno" to JsonOut.num(p.mealsPerDay),
            "preferenzaColazione" to JsonOut.str(p.breakfastPreference.name),
            "preferenzeDietetiche" to JsonOut.strings(p.dietaryPreferences.map { it.name }),
            "cibiGraditi" to JsonOut.strings(p.likedFoods),
            "cibiNonGraditi" to JsonOut.strings(p.dislikedFoods),
            "alimentiEsclusi" to JsonOut.strings(p.excludedFoods),
            "allergie" to JsonOut.strings(p.allergies),
            "restrizioniDietetiche" to JsonOut.strings(p.dietaryRestrictions),
            "tempoCucina" to JsonOut.str(p.cookingTimeAvailable.name),
            "mealPrepPossibile" to JsonOut.bool(p.mealPrepPossible),
            "preferisceCibiSemplici" to JsonOut.bool(p.prefersSimpleFoods),
            "difficoltaDieta" to JsonOut.str(p.dietDifficulties),
            "budgetMensileEur" to JsonOut.num(p.monthlyFoodBudgetEur),
            "ambitoBudget" to JsonOut.str(p.budgetScope.name),
            "personeBudget" to JsonOut.num(p.budgetPersons)
        ),
        "allenamento" to JsonOut.obj(
            "esperienza" to JsonOut.str(p.experience.name),
            "statoAllenamento" to JsonOut.str(p.trainingStatus?.name),
            "anniAllenamento" to JsonOut.num(p.trainingYears),
            "giorniSettimana" to JsonOut.num(p.trainingDaysPerWeek),
            "frequenzaDichiarata" to JsonOut.num(p.trainingFrequencyPerWeek),
            "livelloTecnico" to JsonOut.str(p.technicalLevel.name),
            "attrezzatura" to JsonOut.strings(p.availableEquipment.map { it.name }),
            // Inventario reale: senza questo l'AI prescrive esercizi su
            // attrezzatura che l'utente non possiede (spec §5).
            "inventarioAttrezzatura" to JsonOut.obj(
                "totale" to JsonOut.num(p.gearInventory.usable.size),
                "riepilogo" to JsonOut.str(p.gearInventory.summary()),
                "voci" to JsonOut.arr(
                    p.gearInventory.usable.map { g ->
                        JsonOut.obj(
                            "nome" to JsonOut.str(g.name),
                            "categoria" to JsonOut.str(g.category.name),
                            "quantita" to JsonOut.num(g.quantity),
                            "marca" to JsonOut.str(g.brand.takeIf { it.isNotBlank() }),
                            "modello" to JsonOut.str(g.model.takeIf { it.isNotBlank() }),
                            "minKg" to JsonOut.num(g.minWeightKg),
                            "maxKg" to JsonOut.num(g.maxWeightKg),
                            "incrementiKg" to JsonOut.strings(g.incrementsKg.map { it.toString() }),
                            "note" to JsonOut.str(g.notes.takeIf { it.isNotBlank() })
                        )
                    }
                )
            ),
            "gruppiPrioritari" to JsonOut.strings(p.priorityMuscles.map { it.name }),
            "eserciziPreferiti" to JsonOut.strings(p.preferredExercises),
            "eserciziNonGraditi" to JsonOut.strings(p.dislikedExercises),
            "eserciziNonConosciuti" to JsonOut.strings(p.unknownExercises),
            "carichiAttuali" to JsonOut.obj(
                *p.currentLoads.map { (k, v) -> k to JsonOut.num(v) }.toTypedArray()
            ),
            "limitazioniFisiche" to JsonOut.strings(p.physicalLimitations),
            "movimentiDaEvitare" to JsonOut.strings(p.movementsToAvoid),
            "durataMaxSessioneMin" to JsonOut.num(p.maxSessionMinutes)
        ),
        "overrideManuali" to JsonOut.obj(
            "activityFactor" to JsonOut.num(p.activityFactorOverride),
            "targetCalorico" to JsonOut.num(p.calorieTargetOverride)
        ),
        "cataloghi" to JsonOut.obj(
            "alimentiDisponibili" to JsonOut.num(foodCount),
            "eserciziDisponibili" to JsonOut.num(exerciseCount)
        ),
        "riepilogoAnamnesi" to JsonOut.str(p.anamnesisSummary)
    )
}

/** Conteggio rapido dei cataloghi globali (alimenti ed esercizi). */
@Singleton
class GetCatalogSummaryTool @Inject constructor(
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository
) : CoachTool {
    override val name = "get_catalog_summary"
    override val description =
        "Riepilogo dei cataloghi globali: numero di alimenti (con fonti e categorie) e di esercizi " +
            "(con gruppi muscolari e attrezzatura). Usalo per sapere su quali dati globali puoi contare."

    override val parameters: JsonObject = Schema.obj()

    override suspend fun execute(arguments: JsonObject): String {
        val foods = foodRepository.getAll()
        val exercises = exerciseRepository.getAll()
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "alimenti" to JsonOut.obj(
                    "totale" to JsonOut.num(foods.size),
                    "categorie" to JsonOut.strings(foods.map { it.category }.distinct().sorted()),
                    "fonti" to JsonOut.strings(foods.map { it.source }.distinct().sorted())
                ),
                "esercizi" to JsonOut.obj(
                    "totale" to JsonOut.num(exercises.size),
                    "gruppiMuscolari" to JsonOut.strings(exercises.map { it.primaryMuscle.name }.distinct().sorted())
                )
            )
        )
    }
}
