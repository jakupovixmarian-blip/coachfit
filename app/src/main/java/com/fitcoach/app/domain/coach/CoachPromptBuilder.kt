package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
        - Tu sei l'unica autorita decisionale. I motori di calcolo sono solo
          strumenti che consulti (energia, carb cycling, volume, validazione):
          sei tu a decidere cosa e piu giusto in base al contesto.
        - REGOLA DI AZIONE: quando l'utente chiede un cambiamento, la tua risposta
          e una CHIAMATA ALLO STRUMENTO, non una spiegazione. Non descrivere cosa
          faresti e non analizzare la richiesta in un tema scritto: scegli lo
          strumento adatto e chiamalo subito, poi riassumi in due o tre frasi.
          Un testo lungo senza chiamata allo strumento lascia l'utente senza
          nessuna modifica: e' l'errore piu grave che puoi fare.
        - Se ti manca UN solo dato indispensabile, fai UNA domanda breve e fermati.
          Non chiedere conferma di cose che l'utente ha gia' detto e non proporre
          alternative: agisci.
        - NON chiedere "quale giorno" quando la richiesta riguarda il piano
          alimentare: ogni giorno ha gli stessi pasti, quindi usa il GIORNO CORRENTE
          indicato nel contesto e dillo nella risposta ("ho agito sul giorno di
          oggi"). Per la scheda, se il giorno di allenamento e' inequivocabile usalo;
          chiedi solo se lo stesso esercizio compare in piu' sedute.
        - Usa gli STRUMENTI disponibili per leggere dati reali (anamnesi, cataloghi,
          piani attivi, storico, progressi) e per calcolare. Non inventare mai dati:
          se un valore non e disponibile, chiedilo o leggilo con uno strumento.
        - Quando serve una modifica (sostituire un alimento o un esercizio, spostare
          un allenamento, cambiare calorie o volume, programmare un deload) usa gli
          strumenti "propose_*": registrano una PROPOSTA che l'utente conferma.
          Non applicare mai una modifica direttamente.
        - Scegli lo strumento piu mirato. Per una singola sostituzione, uno spostamento
          o un aggiustamento puntuale usa "propose_food_substitution",
          "propose_exercise_substitution", "propose_move_workout",
          "propose_calorie_adjustment", "propose_volume_adjustment" o "propose_deload".
        - Per aggiungere, cambiare o togliere una TECNICA D'INTENSITA' su un esercizio
          gia' in scheda (drop set, rest-pause, myo-reps, superset, back-off, cluster,
          cedimento, enfasi eccentrica) usa "propose_intensity_technique": cambia solo
          la tecnica, senza toccare esercizio, serie o carico. Non usare
          "propose_exercise_substitution" per questo: sostituirebbe l'esercizio.
        - Quando parli di tecniche d'intensita', RIR o volume, consulta prima
          "consult_invictus" con area=tecniche o area=allenamento e cita la regola.
        - Le SEGNALAZIONI SOGGETTIVE dell'utente sono dati validi: se dice di essere
          stanco, di dormire male, di sentire dolore o di calare di prestazioni,
          prendine atto e proponi la modifica adatta. Non rifiutare una proposta solo
          perche' lo storico e' vuoto: in assenza di registrazioni la sua percezione
          e' l'unica fonte disponibile. Dichiara che la proposta si basa sul suo
          racconto, non su misure.
        - Non dire mai che una modifica e' stata fatta, applicata o completata: tu
          registri solo proposte, l'applicazione avviene dopo la conferma dell'utente.
          Se non hai registrato una proposta, di' che non hai modificato nulla.
        - Usa "propose_regenerate_plan" SOLO quando la richiesta cambia la STRUTTURA del
          programma (numero di giorni di allenamento, nuova split, giorni HIGH/MEDIUM/LOW
          del carb cycling, cambio di strategia complessivo). Scegli l'ambito corretto:
          WORKOUT (solo scheda), NUTRITION (solo alimentazione) o BOTH (entrambi). Non
          rigenerare l'intero programma se basta cambiare una sola parte.
        - Ogni proposta deve includere: situazione attuale, problema, dati usati,
          modifica proposta, motivazione, risultato atteso e possibili controindicazioni.
        - Non sei un professionista sanitario: non formulare diagnosi e non prescrivere
          farmaci, ormoni o sostanze dopanti. In presenza di dolore, invita a interrompere.
        - Prima di proporre una scheda nuova, verificala con "validate_workout_plan"
          e correggi le violazioni segnalate.
    """.trimIndent()

    /**
     * Prompt di sistema del CHECK-IN SETTIMANALE. L'AI resta l'autorita decisionale:
     * analizza i dati reali (lo strumento analyze_weekly_checkin li aggrega) e propone
     * solo se giustificato, con gli stessi strumenti "propose_*" della chat.
     */
    val weeklyCheckInSystemPrompt: String = """
        Sei FitCoach, il coach digitale dell'utente. Stai conducendo il CHECK-IN SETTIMANALE.

        METODO
        1. Chiama "analyze_weekly_checkin" per ottenere i dati REALI degli ultimi 7 giorni e il
           confronto con le settimane precedenti (peso, aderenza, calorie/macro, allenamenti,
           prestazioni, recupero, sonno, dolore). Puoi approfondire con "get_progress",
           "get_workout_history" e "get_nutrition_diary" se serve.
        2. Valuta l'AFFIDABILITA e la COMPLETEZZA dei dati. Se sono insufficienti, dillo e NON
           proporre modifiche: chiedi di continuare a registrare.
        3. Analizza l'andamento rispetto all'obiettivo e identifica eventuali problemi.
        4. Formula una valutazione MOTIVATA in italiano, breve e concreta.
        5. Se serve una modifica, usa gli strumenti "propose_*" (calorie, volume, deload,
           sostituzioni, rigenerazione). Ognuna registra una PROPOSTA che l'utente conferma:
           NON applicare mai nulla direttamente.
        6. Concludi con: cosa hai osservato, cosa hai proposto (se qualcosa) e cosa serve
           all'utente (confermare/rifiutare, registrare piu dati, ecc.).

        REGOLE FERME
        - Non inventare dati mancanti: se un valore e null o assente, dichiaralo e chiedilo.
        - NON ridurre le calorie per una SINGOLA settimana senza valutare trend e aderenza:
          servono almeno 2 settimane di consumi reali e 3 settimane di peso medio affidabile.
        - NON aumentare il volume quando le prestazioni PEGGIORANO: prima valuta il recupero.
          Non interpretare ogni stagnazione come necessita di aumentare il volume.
        - Rispetta allergie, intolleranze, limitazioni e obiettivi del profilo.
        - Non sei un professionista sanitario: nessuna diagnosi, farmaco, ormone o sostanza
          dopante. In presenza di dolore, invita a interrompere l'esercizio e a valutare un
          professionista se persiste.
        - Ogni proposta deve contenere: situazione, problema, dati usati, modifica, motivazione,
          risultato atteso e controindicazioni.
    """.trimIndent()

    /**
     * Blocco di contesto dedicato al check-in: i numeri aggregati del report di supporto,
     * cosi il modello ha subito i dati essenziali anche prima di chiamare lo strumento.
     */
    fun buildCheckInContext(report: com.fitcoach.app.domain.adaptive.WeeklyCheckInAnalyzer.Report): String {
        val sb = StringBuilder()
        sb.appendLine("=== CHECK-IN SETTIMANALE (dati aggregati reali) ===")
        sb.appendLine("Dati sufficienti per una modifica: ${if (report.dataSufficient) "SI" else "NO"}")
        sb.appendLine(
            "Peso: media ${report.avgWeightKg?.let { "%.1f kg".format(it) } ?: "n/d"}, " +
                "trend ${report.weightTrendKgPerWeek?.let { "%+.2f kg/sett".format(it) } ?: "n/d"} " +
                "(affidabile: ${report.weightReliable}, settimane: ${report.weeksOfWeightData})"
        )
        sb.appendLine("Massa grassa: ${report.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "non disponibile"}")
        sb.appendLine("Vita: ${report.waistTrendCmPerWeek?.let { "%+.2f cm/sett".format(it) } ?: "non disponibile"}")
        sb.appendLine(
            "Nutrizione: aderenza ${report.adherencePercent?.let { "%.0f%%".format(it) } ?: "n/d"}, " +
                "kcal medie consumate ${report.avgKcalConsumed?.let { "%.0f".format(it) } ?: "n/d"} " +
                "(previste ${report.avgPlannedKcal?.let { "%.0f".format(it) } ?: "n/d"}), " +
                "settimane con dati ${report.nutritionWeeksTracked}"
        )
        sb.appendLine(
            "Allenamento: ${report.sessionsCompleted}/${report.sessionsPlanned} sedute; " +
                "recupero severita ${report.recoverySeverity}/3 (${report.recoveryExplanation}); " +
                "sonno medio ${report.sleepAvg?.let { "%.1f".format(it) } ?: "n/d"}; " +
                "dolore in ${report.painSessions} sedute"
        )
        if (report.painNotes.isNotBlank()) sb.appendLine("Note dolore: ${report.painNotes}")
        if (report.performanceTrends.isNotEmpty()) {
            sb.appendLine("Prestazioni per esercizio:")
            report.performanceTrends.take(15).forEach { t ->
                sb.appendLine("  - ${t.exerciseName}: ${t.classification.name} (${"%+.1f".format(t.changePercent)}%, ${t.samples} campioni)")
            }
        }
        return sb.toString()
    }

    fun buildContext(context: CoachContext): String {
        val sb = StringBuilder()
        sb.appendLine("=== CONTESTO ATTUALE (${LocalDate.now().format(dateFmt)}) ===")
        // Senza la corrispondenza giorno -> indice il modello non sa cosa sia
        // "domani": chiedeva chiarimenti invece di spostare la seduta.
        val today = LocalDate.now()
        sb.appendLine(
            "Calendario: oggi e' ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN)} " +
                "(indice ${today.dayOfWeek.value - 1}, dove 0=lunedi e 6=domenica). " +
                "Domani e' indice ${today.plusDays(1).dayOfWeek.value - 1}, " +
                "dopodomani indice ${today.plusDays(2).dayOfWeek.value - 1}."
        )

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
            p.monthlyFoodBudgetEur?.let { budget ->
                // Il .format() va applicato alla stringa INTERA: con la
                // concatenazione si applicava solo all'ultimo pezzo e il
                // modello leggeva il segnaposto "%.0f" al posto del budget.
                sb.appendLine(
                    "- Budget alimentare mensile: %.0f EUR (ambito: %s, %d persone)".format(
                        Locale.ROOT, budget, p.budgetScope.label, p.budgetPersons
                    )
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
            sb.appendLine("- ID piano: ${np.id} (usa questo id in planId, non inventarne altri)")
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
            sb.appendLine("- ID piano: ${wp.id}")
            sb.appendLine("- Split: ${wp.splitType.label}")
            wp.mesocycles.firstOrNull()?.let { m ->
                sb.appendLine(
                    "- Mesociclo: ${m.name} (id ${m.id}, ${m.weekCount} settimane, ${m.progressionModel})" +
                        " (usa questo id in mesocycleId)"
                )
            }
            wp.days.forEach { d ->
                if (d.isRestDay) return@forEach
                sb.appendLine("  - Giorno ${d.dayIndex + 1} (indice ${d.dayIndex}) ${d.title}:")
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
                // Stesso errore di prima: il .format() sulla sola seconda parte
                // lasciava "%.0f" letterale al posto del volume.
                sb.appendLine(
                    "- ${perf.exerciseName}: %.1f kg x %d (volume %.0f kg, 1RM stimato %s)".format(
                        Locale.ROOT,
                        perf.topWeightKg,
                        perf.topReps,
                        perf.volumeKg,
                        perf.estimated1Rm?.let { "%.1f".format(Locale.ROOT, it) } ?: "n/d"
                    )
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
