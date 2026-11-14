package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Costruisce il contesto testuale fornito al modello. Il modello riceve dati
 * REALI del programma (profilo, piano attivo, storico) e istruzioni rigide su
 * limiti di sicurezza e formato delle proposte di modifica.
 */
object CoachPromptBuilder {

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val systemPrompt: String = """
        Sei FitCoach, un coach digitale esperto di allenamento con i pesi,
        ipertrofia, nutrizione sportiva e ricomposizione corporea.

        REGOLE FONDAMENTALI
        1. Rispondi SEMPRE in italiano, in modo chiaro, diretto e professionale.
        2. Usa ESCLUSIVAMENTE i dati del programma forniti qui sotto. NON inventare
           pesi, misure, alimenti, esercizi o numeri che non sono presenti nel contesto.
        3. Se un dato non e disponibile, dillo esplicitamente e chiedi all'utente di
           registrarlo, invece di ipotizzarlo.
        4. Non sei un professionista sanitario: non formulare diagnosi e non
           prescrivere farmaci, ormoni o sostanze dopanti.
        5. Rispetta sempre allergie, intolleranze, preferenze alimentari e limitazioni
           fisiche indicate nel profilo.
        6. Non proporre mai deficit calorici estremi, diete molto restrittive o volumi
           di allenamento irragionevoli.
        7. In presenza di dolore segnalato, invita a interrompere l'esercizio e a
           consultare un professionista se il dolore persiste.

        MODIFICHE AL PROGRAMMA
        - Quando l'utente chiede una modifica (sostituire un alimento o un esercizio,
          spostare un allenamento, cambiare calorie, programmare un deload) DEVI
          proporla come azione strutturata, non applicarla mai direttamente.
        - Includi sempre: situazione attuale, problema, dati usati, modifica proposta,
          motivazione, risultato atteso e possibili controindicazioni.
        - Spiega la proposta nel testo della risposta e allega un blocco JSON con
          l'azione, nel formato indicato. L'utente confermera prima dell'applicazione.

        FORMATO AZIONE (solo se serve una modifica, alla fine della risposta):
        ```json
        {"action":"SUBSTITUTE_FOOD","domain":"NUTRITION","title":"...","situation":"...",
         "problem":"...","data_used":"...","proposed_change":"...","rationale":"...",
         "expected_outcome":"...","contraindications":"...",
         "payload":{"entryId":"123","foodName":"..."},"revertPayload":{"entryId":"123"}}
        ```
        Azioni ammesse: SUBSTITUTE_FOOD, SUBSTITUTE_EXERCISE, MOVE_WORKOUT,
        ADJUST_CALORIES, DELOAD.
        Se non serve alcuna modifica, NON includere il blocco JSON.
    """.trimIndent()

    fun buildContext(context: CoachContext): String {
        val sb = StringBuilder()
        sb.appendLine("=== CONTESTO ATTUALE (${LocalDate.now().format(dateFmt)}) ===")

        val p = context.profile
        if (p == null) {
            sb.appendLine("Profilo: NON configurato. Chiedi all'utente di completare l'onboarding.")
        } else {
            sb.appendLine("PROFILO UTENTE")
            sb.appendLine("- Nome: ${p.name.ifBlank { "non indicato" }}")
            sb.appendLine("- Eta: ${p.ageYears ?: "non indicata"} anni")
            sb.appendLine("- Sesso: ${p.sex.name}")
            sb.appendLine("- Altezza: ${p.heightCm?.let { "%.0f cm".format(it) } ?: "non indicata"}")
            sb.appendLine("- Peso: ${p.weightKg?.let { "%.1f kg".format(it) } ?: "non indicato"}")
            sb.appendLine("- Massa grassa: ${p.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "non disponibile"}")
            sb.appendLine("- Esperienza: ${p.experience.label}")
            sb.appendLine("- Obiettivo: ${p.goal.label}")
            sb.appendLine("- Giorni di allenamento/settimana: ${p.trainingDaysPerWeek}")
            sb.appendLine("- Pasti al giorno: ${p.mealsPerDay}")
            if (p.dietaryPreferences.isNotEmpty()) {
                sb.appendLine("- Preferenze alimentari: ${p.dietaryPreferences.joinToString { it.label }}")
            }
            if (p.excludedFoods.isNotEmpty()) sb.appendLine("- Alimenti esclusi: ${p.excludedFoods.joinToString()}")
            if (p.allergies.isNotEmpty()) sb.appendLine("- Allergie/intolleranze: ${p.allergies.joinToString()}")
            if (p.movementsToAvoid.isNotEmpty()) sb.appendLine("- Movimenti da evitare: ${p.movementsToAvoid.joinToString()}")
            if (p.physicalLimitations.isNotEmpty()) sb.appendLine("- Limitazioni fisiche: ${p.physicalLimitations.joinToString()}")

            // Anamnesi estesa: stile di vita, recupero, allenamento.
            sb.appendLine("STILE DI VITA E RECUPERO")
            sb.appendLine("- Tipo di lavoro: ${p.workType.label} (turni: ${p.workShift.label})")
            p.dailySteps?.let { sb.appendLine("- Passi/giorno: ~$it") }
            sb.appendLine("- Trasporto: ${p.transportMode.label}")
            if (p.extraSportSessionsPerWeek > 0) sb.appendLine("- Sport extra/settimana: ${p.extraSportSessionsPerWeek}")
            sb.appendLine("- Sonno: ${p.sleepHours?.let { "%.1f h".format(it) } ?: "non indicato"} (qualita ${p.sleepQuality.label})")
            sb.appendLine("- Stress: ${p.stressLevel.label}; Energia quotidiana: ${p.dailyEnergy.label}; Recupero: ${p.recoveryCapacity.label}")
            if (p.priorityMuscles.isNotEmpty()) sb.appendLine("- Gruppi muscolari prioritari: ${p.priorityMuscles.joinToString { it.label }}")
            sb.appendLine("- Livello tecnico: ${p.technicalLevel.label}; Anni di allenamento: ${p.trainingYears?.let { "%.1f".format(it) } ?: "non indicati"}")
            sb.appendLine("- Colazione preferita: ${p.breakfastPreference.label}")
            if (p.likedFoods.isNotEmpty()) sb.appendLine("- Alimenti graditi: ${p.likedFoods.joinToString()}")
            if (p.dislikedFoods.isNotEmpty()) sb.appendLine("- Alimenti poco graditi: ${p.dislikedFoods.joinToString()}")
            if (p.weeklyFoodBudgetEur != null) sb.appendLine("- Budget alimentare: %.0f EUR/settimana".format(p.weeklyFoodBudgetEur))
            p.monthlyFoodBudgetEur?.let {
                sb.appendLine(
                    "- Budget alimentare mensile: %.0f EUR (ambito: ${p.budgetScope.label}, " +
                        "${p.budgetPersons} persone)".format(it)
                )
            }
            sb.appendLine(
                "- Anamnesi: ${if (p.anamnesisCompleted) "confermata" else "NON confermata " +
                    "(invita a completarla prima di proporre cambi strutturali)"}"
            )
            if (p.dietDifficulties.isNotBlank()) sb.appendLine("- Difficolta con la dieta: ${p.dietDifficulties}")
            if (p.secondaryGoals.isNotEmpty()) sb.appendLine("- Obiettivi secondari: ${p.secondaryGoals.joinToString { it.label }}")
        }

        val np = context.nutritionPlan
        sb.appendLine()
        if (np == null) {
            sb.appendLine("PIANO NUTRIZIONALE: nessun piano attivo.")
        } else {
            sb.appendLine("PIANO NUTRIZIONALE ATTIVO")
            sb.appendLine("- BMR: %.0f kcal (${np.bmrFormula})".format(np.bmr))
            sb.appendLine("- TDEE: %.0f kcal (${np.tdeeFormula})".format(np.tdee))
            sb.appendLine("- Calorie settimanali: %.0f kcal".format(np.weeklyCalories))
            sb.appendLine(
                "- Bilancio settimanale: %+.0f kcal (%s)".format(
                    np.weeklyKcalBalance,
                    if (np.weeklyKcalBalance >= 0) "surplus" else "deficit"
                )
            )
            sb.appendLine("- Giornate: HIGH=${np.highCarbDays}, MEDIUM=${np.mediumCarbDays}, LOW=${np.lowCarbDays}")
            sb.appendLine("- Carb cycling applicato: ${if (np.carbCyclingApplied) "si" else "no (distribuzione uniforme)"}")
            sb.appendLine("- Versione piano: v${np.version}")
            sb.appendLine("- Proteine: %.1f g/kg".format(np.proteinGPerKg))
            val targets = np.days.map { it.dayType }.distinct().sortedBy { it.ordinal }
            if (targets.isNotEmpty()) {
                sb.appendLine("Target per tipologia di giornata:")
                targets.forEach { type ->
                    np.days.firstOrNull { it.dayType == type }?.let { d ->
                        sb.appendLine(
                            "  - ${type.label}: %.0f kcal | P %.0f g | C %.0f g | F %.0f g".format(
                                d.targetKcal, d.targetProtein, d.targetCarbs, d.targetFat
                            )
                        )
                    }
                }
            }
            sb.appendLine("Giorni del piano (0=Lunedi):")
            np.days.forEach { d ->
                val names = d.meals.flatMap { it.entries }.joinToString { it.foodName }.takeIf { it.isNotBlank() } ?: "-"
                sb.appendLine("  - Giorno ${d.dayIndex + 1} [${d.dayType.label}] alimenti: $names")
            }
        }

        val wp = context.workoutPlan
        sb.appendLine()
        if (wp == null) {
            sb.appendLine("PIANO DI ALLENAMENTO: nessun piano attivo.")
        } else {
            sb.appendLine("PIANO DI ALLENAMENTO ATTIVO")
            sb.appendLine("- Split: ${wp.splitType.label}")
            wp.mesocycles.firstOrNull()?.let { m ->
                sb.appendLine("- Mesociclo: ${m.name} (${m.weekCount} settimane, ${m.progressionModel})")
            }
            wp.days.forEach { d ->
                if (d.isRestDay) return@forEach
                sb.appendLine("  - Giorno ${d.dayIndex + 1} ${d.title}:")
                d.exercises.forEach { ex ->
                    val tech = if (ex.intensityTechnique.name != "NONE") ", tecnica ${ex.intensityTechnique.name}" else ""
                    sb.appendLine(
                        "      ${ex.exerciseName}: ${ex.sets}x${ex.minReps}-${ex.maxReps} " +
                            "@RIR ${ex.targetRir}, rec ${ex.restSeconds}s$tech (id ${ex.id})"
                    )
                }
            }
        }

        if (context.recentPerformances.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("PRESTAZIONI RECENTI")
            context.recentPerformances.take(15).forEach { perf ->
                sb.appendLine(
                    "- ${perf.exerciseName}: ${"%.1f".format(perf.topWeightKg)} kg x ${perf.topReps} " +
                        "(volume %.0f kg, 1RM stimato ${perf.estimated1Rm?.let { "%.1f".format(it) } ?: "n/d"})"
                )
            }
        }

        val trend = context.weightTrend
        sb.appendLine()
        if (trend == null || trend.weeklyAverages.isEmpty()) {
            sb.appendLine("PESO: nessun dato sufficiente. Invita l'utente a registrare il peso.")
        } else {
            sb.appendLine("ANDAMENTO PESO")
            trend.weeklyAverages.takeLast(6).forEach { w ->
                sb.appendLine("- Settimana dal giorno ${w.weekStartEpochDay}: media ${w.avgWeightKg?.let { "%.2f kg".format(it) } ?: "n/d"} (${w.samples} misurazioni)")
            }
            sb.appendLine("- Tendenza: ${trend.trendKgPerWeek?.let { "%+.2f kg/settimana".format(it) } ?: "non calcolabile"} (affidabile: ${trend.isReliable})")
        }

        if (context.recentChanges.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("MODIFICHE RECENTI AL PROGRAMMA")
            context.recentChanges.take(8).forEach { c ->
                sb.appendLine("- [${c.status}] ${c.title}: ${c.proposedChange}")
            }
        }

        context.todayBalance?.let { b ->
            sb.appendLine()
            sb.appendLine("DIARIO NUTRIZIONALE - OGGI (obiettivo / consumato / rimanente)")
            sb.appendLine(
                "- Calorie: %.0f / %.0f / %s kcal".format(
                    b.targetKcal, b.consumedKcal,
                    if (b.kcalExceeded > 0.5) "+%.0f eccedenti".format(b.kcalExceeded)
                    else "%.0f rimanenti".format(b.remainingKcal)
                )
            )
            sb.appendLine(
                "- Proteine: %.0f / %.0f / %s g".format(
                    b.targetProtein, b.consumedProtein,
                    if (b.proteinExceeded > 0.5) "+%.0f eccedenti".format(b.proteinExceeded)
                    else "%.0f rimanenti".format(b.remainingProtein)
                )
            )
            sb.appendLine(
                "- Carboidrati: %.0f / %.0f / %s g".format(
                    b.targetCarbs, b.consumedCarbs,
                    if (b.carbsExceeded > 0.5) "+%.0f eccedenti".format(b.carbsExceeded)
                    else "%.0f rimanenti".format(b.remainingCarbs)
                )
            )
            sb.appendLine(
                "- Grassi: %.0f / %.0f / %s g".format(
                    b.targetFat, b.consumedFat,
                    if (b.fatExceeded > 0.5) "+%.0f eccedenti".format(b.fatExceeded)
                    else "%.0f rimanenti".format(b.remainingFat)
                )
            )
        }

        context.weeklyNutrition?.let { w ->
            sb.appendLine()
            sb.appendLine("RIEPILOGO NUTRIZIONALE SETTIMANALE (consumi reali)")
            sb.appendLine("- Giorni registrati: ${w.daysTracked}")
            sb.appendLine("- Media calorie: %.0f kcal (previste %.0f)".format(w.avgKcal, w.avgPlannedKcal))
            sb.appendLine("- Media P %.0f g | C %.0f g | G %.0f g".format(w.avgProtein, w.avgCarbs, w.avgFat))
            sb.appendLine("- Aderenza media: %.0f%%; giorni in target: ${w.daysOnTarget}".format(w.avgAdherencePercent))
            sb.appendLine("- Differenza previsto/consumato: %+.0f kcal".format(w.kcalDifference))
            sb.appendLine("- Alimenti extra registrati: ${w.extraEntries}")
        }

        if (context.substitutions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("SOSTITUZIONI ALIMENTARI RECENTI")
            context.substitutions.take(10).forEach { s ->
                sb.appendLine(
                    "- ${s.originalName} ${s.originalGrams.toInt()} g -> ${s.newName} ${s.newGrams.toInt()} g " +
                        "(${s.status}; P ${s.proteinBefore.toInt()}->${s.proteinAfter.toInt()} g)"
                )
            }
        }

        if (context.shoppingList.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("LISTA DELLA SPESA SETTIMANALE (dal piano effettivo)")
            context.shoppingList.groupBy { it.category }.forEach { (cat, items) ->
                sb.appendLine("  ${cat.label}: " + items.joinToString { "${it.foodName} ${it.totalGrams.toInt()} g" })
            }
        }

        context.budgetReport?.let { b ->
            sb.appendLine()
            sb.appendLine("COSTO DEL PIANO E BUDGET")
            sb.appendLine("- Costo stimato: %.2f EUR/settimana (%.2f EUR/mese)".format(b.weeklyCostEur, b.monthlyCostEur))
            b.monthlyBudgetEur?.let { budget ->
                sb.appendLine("- Budget mensile attribuibile: %.2f EUR".format(budget))
                sb.appendLine(
                    "- Stato: %s".format(
                        if (b.withinBudget) "entro il budget"
                        else "FUORI budget di %.2f EUR".format(b.overBudgetEur)
                    )
                )
            } ?: sb.appendLine("- Nessun budget mensile impostato.")
            if (b.partialEstimate) {
                sb.appendLine("- Nota: costo parziale, ${b.unpricedFoods.size} alimenti senza prezzo noto.")
            }
        }

        return sb.toString()
    }
}
