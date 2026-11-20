package com.fitcoach.app.domain.anamnesis

import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.CookingTimeAvailable
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TechnicalLevel
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.TrainingStatus
import com.fitcoach.app.domain.model.TransportMode
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkShift
import com.fitcoach.app.domain.model.WorkType

/**
 * Motore deterministico dell'anamnesi (spec. §3).
 *
 * Sceglie la prossima domanda in base ai dati MANCANTI del profilo, applica le
 * risposte, rileva le contraddizioni e costruisce il riepilogo. Il Coach AI puo
 * riformulare le domande e approfondire, ma la logica di completezza e
 * validazione e qui, ed e verificabile con test unitari.
 *
 * Regola: nessuna generazione di piani prima della conferma dell'anamnesi.
 */
object AnamnesisEngine {

    enum class Kind { NUMBER, INT, TEXT, CHOICE, MULTI, BOOL }

    data class Question(
        val id: String,
        val section: String,
        val prompt: String,
        val hint: String = "",
        val kind: Kind,
        val options: List<String> = emptyList(),
        /** Se false, la domanda e facoltativa e non blocca la conferma. */
        val required: Boolean = true
    )

    /** Sezioni in ordine, come richiesto dalla spec. */
    val sections: List<String> = listOf(
        "Profilo personale",
        "Stile di vita",
        "Sonno e recupero",
        "Alimentazione e budget",
        "Esperienza di allenamento",
        "Limitazioni"
    )

    /** Flusso completo delle domande (intervista progressiva). */
    val questions: List<Question> = listOf(
        // --- Profilo personale ---
        Question("age", sections[0], "Quanti anni hai?", kind = Kind.INT),
        Question(
            "sex", sections[0], "Sesso biologico (serve solo per le formule metaboliche).",
            kind = Kind.CHOICE, options = Sex.entries.map { it.name }
        ),
        Question("heightCm", sections[0], "Qual è la tua altezza in cm?", kind = Kind.NUMBER),
        Question("weightKg", sections[0], "Qual è il tuo peso attuale in kg?", kind = Kind.NUMBER),
        Question(
            "bodyFatPercent", sections[0],
            "Conosci la tua percentuale di massa grassa? (facoltativa)", kind = Kind.NUMBER,
            required = false
        ),
        Question(
            "goal", sections[0], "Qual è il tuo obiettivo principale?",
            kind = Kind.CHOICE, options = Goal.entries.map { it.name }
        ),
        Question(
            "secondaryGoals", sections[0], "Hai un obiettivo secondario? (facoltativo)",
            kind = Kind.MULTI, options = Goal.entries.map { it.name }, required = false
        ),
        Question(
            "targetTimelineWeeks", sections[0],
            "In quante settimane vorresti raggiungerlo? (facoltativo)", kind = Kind.INT,
            required = false
        ),

        // --- Stile di vita ---
        Question(
            "workType", sections[1], "Che tipo di lavoro svolgi?",
            kind = Kind.CHOICE, options = WorkType.entries.map { it.name }
        ),
        Question(
            "workHoursPerDay", sections[1], "Quante ore lavori al giorno?",
            kind = Kind.NUMBER, required = false
        ),
        Question(
            "dailySteps", sections[1], "Quanti passi fai in media al giorno? (facoltativo)",
            kind = Kind.INT, required = false
        ),
        Question(
            "transportMode", sections[1], "Come ti sposti abitualmente?",
            kind = Kind.CHOICE, options = TransportMode.entries.map { it.name }
        ),
        Question(
            "workShift", sections[1], "Lavori su turni?", kind = Kind.CHOICE,
            options = WorkShift.entries.map { it.name }, required = false
        ),
        Question(
            "extraSportSessionsPerWeek", sections[1],
            "Quante attività extra oltre alla palestra a settimana? (facoltativo)", kind = Kind.INT,
            required = false
        ),
        Question(
            "maxSessionMinutes", sections[1],
            "Quanto tempo hai a disposizione per ogni sessione? (minuti, facoltativo)", kind = Kind.INT,
            required = false
        ),

        // --- Sonno e recupero ---
        Question("sleepHours", sections[2], "Quante ore dormi in media per notte?", kind = Kind.NUMBER),
        Question(
            "sleepQuality", sections[2], "Come valuti la qualità del tuo sonno?",
            kind = Kind.CHOICE, options = Level3.entries.map { it.name }
        ),
        Question(
            "stressLevel", sections[2], "Quanto stress percepisci?",
            kind = Kind.CHOICE, options = Level3.entries.map { it.name }
        ),
        Question(
            "dailyEnergy", sections[2], "Come valuti la tua energia quotidiana?",
            kind = Kind.CHOICE, options = Level3.entries.map { it.name }
        ),
        Question(
            "recoveryCapacity", sections[2], "Come recuperi dopo gli allenamenti?",
            kind = Kind.CHOICE, options = Level3.entries.map { it.name }
        ),

        // --- Alimentazione e budget ---
        Question("mealsPerDay", sections[3], "Quanti pasti preferisci al giorno?", kind = Kind.INT),
        Question(
            "breakfastPreference", sections[3],
            "Preferisci una colazione dolce o salata?", kind = Kind.CHOICE,
            options = BreakfastPreference.entries.map { it.name }
        ),
        Question(
            "dietaryPreferences", sections[3], "Hai preferenze o restrizioni alimentari?",
            kind = Kind.MULTI, options = DietaryPreference.entries.map { it.name }
        ),
        Question(
            "likedFoods", sections[3], "Quali alimenti gradisci di più? (separali con virgola)",
            kind = Kind.TEXT, required = false
        ),
        Question(
            "dislikedFoods", sections[3], "Quali alimenti non gradisci? (separali con virgola)",
            kind = Kind.TEXT, required = false
        ),
        Question(
            "allergies", sections[3], "Hai allergie o intolleranze? (separali con virgola)",
            kind = Kind.TEXT, required = false
        ),
        Question(
            "cookingTimeAvailable", sections[3], "Quanto tempo hai per cucinare?",
            kind = Kind.CHOICE, options = CookingTimeAvailable.entries.map { it.name }
        ),
        Question(
            "mealPrepPossible", sections[3],
            "Puoi preparare i pasti in anticipo (meal prep)?", kind = Kind.BOOL
        ),
        Question(
            "monthlyFoodBudgetEur", sections[3],
            "Qual è il tuo budget alimentare mensile in euro? (0 se non lo vuoi considerare)",
            kind = Kind.NUMBER, required = false
        ),
        Question(
            "budgetScope", sections[3], "A cosa si riferisce questo budget?",
            kind = Kind.CHOICE, options = BudgetScope.entries.map { it.name }, required = false
        ),
        Question(
            "budgetPersons", sections[3],
            "Per quante persone è il budget? (1 se solo per te)", kind = Kind.INT, required = false
        ),

        // --- Esperienza di allenamento ---
        Question(
            "trainingStatus", sections[4], "Ti sei già allenato con i pesi in passato?",
            hint = "La risposta determina le domande successive.",
            kind = Kind.CHOICE, options = TrainingStatus.entries.map { it.name }
        ),
        Question(
            "experience", sections[4], "Qual è il tuo livello di esperienza?",
            kind = Kind.CHOICE, options = TrainingExperience.entries.map { it.name }
        ),
        Question(
            "trainingYears", sections[4], "Da quanti anni ti alleni?", kind = Kind.NUMBER,
            required = false
        ),
        Question(
            "currentLoads", sections[4],
            "Conosci carichi reali recenti? (es. panca=70, squat=100; scrivi 'non so' se non li conosci)",
            kind = Kind.TEXT, required = false
        ),
        Question(
            "trainingDaysPerWeek", sections[4], "Quanti giorni a settimana puoi allenarti?",
            kind = Kind.INT
        ),
        Question(
            "technicalLevel", sections[4], "Come valuti il tuo livello tecnico?",
            kind = Kind.CHOICE, options = TechnicalLevel.entries.map { it.name }
        ),
        Question(
            "availableEquipment", sections[4], "Quale attrezzatura hai a disposizione?",
            kind = Kind.MULTI, options = Equipment.entries.map { it.name }
        ),
        Question(
            "preferredExercises", sections[4],
            "Quali esercizi preferisci? (separali con virgola)", kind = Kind.TEXT, required = false
        ),
        Question(
            "dislikedExercises", sections[4],
            "Quali esercizi non gradisci? (separali con virgola)", kind = Kind.TEXT, required = false
        ),
        Question(
            "priorityMuscles", sections[4],
            "Quali gruppi muscolari vuoi prioritizzare? (facoltativo)",
            kind = Kind.MULTI, options = MuscleGroup.entries.map { it.name }, required = false
        ),

        // --- Limitazioni ---
        Question(
            "physicalLimitations", sections[5],
            "Hai limitazioni fisiche o interventi? (separali con virgola)", kind = Kind.TEXT,
            required = false
        ),
        Question(
            "movementsToAvoid", sections[5],
            "Ci sono movimenti o esercizi da evitare? (separali con virgola)", kind = Kind.TEXT,
            required = false
        )
    )

    /**
     * Domande con un default non nullable nel profilo: il valore esiste sempre,
     * quindi [isAnswered] non basta a dire che l'utente l'ha scelta.
     */
    private val defaultedIds = setOf(
        "goal", "workType", "transportMode", "workShift", "sleepQuality", "stressLevel",
        "dailyEnergy", "recoveryCapacity", "mealsPerDay", "breakfastPreference",
        "cookingTimeAvailable", "mealPrepPossible", "budgetScope", "budgetPersons",
        "experience", "technicalLevel", "trainingDaysPerWeek"
    )

    /**
     * La domanda va ancora posta: nessuna risposta, oppure risposta mai
     * dichiarata dall'utente (solo default del profilo).
     *
     * Senza il secondo controllo l'intervista saltava obiettivo, esperienza,
     * giorni di allenamento e tipo di lavoro: il programma veniva generato su
     * valori che l'utente non aveva mai visto.
     */
    fun needsAnswer(profile: UserProfile, id: String): Boolean =
        !isAnswered(profile, id) || (id in defaultedIds && id !in profile.answeredQuestionIds)

    /** La prossima domanda da porre (prima non risposta), o null se completata. */
    fun nextQuestion(profile: UserProfile, skipOptional: Boolean = false): Question? =
        questions.firstOrNull { q ->
            isRelevant(profile, q) && needsAnswer(profile, q.id) && (q.required || !skipOptional)
        }

    /** Domande di una sezione, per l'editor a sezioni. */
    fun questionsOf(section: String): List<Question> = questions.filter { it.section == section }

    /** La domanda successiva nel flusso COMPLETO (per la modalita modifica), senza saltare le risposte. */
    fun nextQuestionFull(afterId: String): Question? {
        val idx = questions.indexOfFirst { it.id == afterId }
        if (idx < 0) return questions.firstOrNull()
        return questions.getOrNull(idx + 1)
    }

    fun previousQuestion(beforeId: String): Question? {
        val idx = questions.indexOfFirst { it.id == beforeId }
        return if (idx <= 0) null else questions[idx - 1]
    }

    /**
     * Azzera l'anamnesi per ricompilarla da zero, PRESERVANDO i dati che non le
     * appartengono: nome, identificativo e data di creazione del profilo. Le
     * chiavi API (SecureKeyStore) e lo storico allenamenti (tabelle Room) non
     * sono toccati: non sono parte del profilo.
     */
    fun resetAnamnesis(profile: UserProfile): UserProfile = UserProfile(
        id = profile.id,
        name = profile.name,
        createdAt = profile.createdAt,
        updatedAt = System.currentTimeMillis()
    )

    /** Testo con cui precompilare una domanda a partire dal profilo (modalita modifica). */
    fun answerText(profile: UserProfile, id: String): String = when (id) {
        "age" -> profile.ageYears?.toString().orEmpty()
        "sex" -> profile.sex.takeIf { it != Sex.UNSPECIFIED }?.name.orEmpty()
        "heightCm" -> profile.heightCm?.toString().orEmpty()
        "weightKg" -> profile.weightKg?.toString().orEmpty()
        "bodyFatPercent" -> profile.bodyFatPercent?.toString().orEmpty()
        "goal" -> profile.goal.name
        "secondaryGoals" -> profile.secondaryGoals.joinToString(",") { it.name }
        "targetTimelineWeeks" -> profile.targetTimelineWeeks?.toString().orEmpty()
        "workType" -> profile.workType.name
        "workHoursPerDay" -> profile.workHoursPerDay?.toString().orEmpty()
        "dailySteps" -> profile.dailySteps?.toString().orEmpty()
        "transportMode" -> profile.transportMode.name
        "workShift" -> profile.workShift.name
        "extraSportSessionsPerWeek" -> profile.extraSportSessionsPerWeek.toString()
        "maxSessionMinutes" -> profile.maxSessionMinutes?.toString().orEmpty()
        "sleepHours" -> profile.sleepHours?.toString().orEmpty()
        "sleepQuality" -> profile.sleepQuality.name
        "stressLevel" -> profile.stressLevel.name
        "dailyEnergy" -> profile.dailyEnergy.name
        "recoveryCapacity" -> profile.recoveryCapacity.name
        "mealsPerDay" -> profile.mealsPerDay.toString()
        "breakfastPreference" -> profile.breakfastPreference.name
        "dietaryPreferences" -> profile.dietaryPreferences.joinToString(",") { it.name }
        "likedFoods" -> profile.likedFoods.joinToString(", ")
        "dislikedFoods" -> profile.dislikedFoods.joinToString(", ")
        "allergies" -> profile.allergies.joinToString(", ")
        "cookingTimeAvailable" -> profile.cookingTimeAvailable.name
        "mealPrepPossible" -> if (profile.mealPrepPossible) "Si" else "No"
        "monthlyFoodBudgetEur" -> profile.monthlyFoodBudgetEur?.toString().orEmpty()
        "budgetScope" -> profile.budgetScope.name
        "budgetPersons" -> profile.budgetPersons.toString()
        "trainingStatus" -> profile.trainingStatus?.name.orEmpty()
        "experience" -> profile.experience.name
        "trainingYears" -> profile.trainingYears?.toString().orEmpty()
        "currentLoads" -> profile.currentLoads.entries.joinToString(", ") { "${it.key}=${it.value}" }
        "trainingDaysPerWeek" -> profile.trainingDaysPerWeek.toString()
        "technicalLevel" -> profile.technicalLevel.name
        "availableEquipment" -> profile.availableEquipment.joinToString(",") { it.name }
        "preferredExercises" -> profile.preferredExercises.joinToString(", ")
        "dislikedExercises" -> profile.dislikedExercises.joinToString(", ")
        "priorityMuscles" -> profile.priorityMuscles.joinToString(",") { it.name }
        "physicalLimitations" -> profile.physicalLimitations.joinToString(", ")
        "movementsToAvoid" -> profile.movementsToAvoid.joinToString(", ")
        else -> ""
    }

    /** Selezione (chip) con cui precompilare una domanda a partire dal profilo. */
    fun answerSelection(profile: UserProfile, id: String): Set<String> = when (id) {
        "availableEquipment" -> profile.availableEquipment.map { it.name }.toSet()
        "priorityMuscles" -> profile.priorityMuscles.map { it.name }.toSet()
        "dietaryPreferences" -> profile.dietaryPreferences.map { it.name }.toSet()
        "secondaryGoals" -> profile.secondaryGoals.map { it.name }.toSet()
        else -> emptySet()
    }

    /**
     * Prossima domanda non ancora risposta DOPO quella indicata. L'avanzamento e
     * monotono: saltare una domanda facoltativa non deve riportare l'utente
     * indietro sulla domanda saltata (la prima senza risposta globale), che
     * causava un ciclo infinito tra "Salta" e "Avanti".
     */
    fun nextQuestionAfter(profile: UserProfile, afterId: String): Question? {
        val idx = questions.indexOfFirst { it.id == afterId }
        if (idx < 0) return nextQuestion(profile)
        return questions.drop(idx + 1).firstOrNull { isRelevant(profile, it) && needsAnswer(profile, it.id) }
    }

    /** Domande obbligatorie ancora senza risposta. */
    fun missingRequired(profile: UserProfile): List<Question> =
        questions.filter { isRelevant(profile, it) && it.required && !isAnswered(profile, it.id) }

    private fun isRelevant(profile: UserProfile, question: Question): Boolean =
        question.id != "currentLoads" || profile.trainingStatus != TrainingStatus.NEVER_TRAINED

    /**
     * Id delle domande a cui l'utente ha realmente risposto.
     *
     * [isAnswered] risponde `true` anche per i campi con default non nullable
     * (obiettivo, tipo di lavoro, esperienza): e corretto per il flusso
     * dell'intervista, ma non significa che l'utente abbia dichiarato quel
     * valore. L'insieme persistito e quindi l'unica fonte attendibile; i profili
     * creati prima di questo campo vengono riempiti dalla migrazione 13->14.
     */
    fun answeredIds(profile: UserProfile): Set<String> = profile.answeredQuestionIds

    fun isAnswered(profile: UserProfile, id: String): Boolean = when (id) {
        "age" -> profile.ageYears != null
        "sex" -> profile.sex != Sex.UNSPECIFIED
        "heightCm" -> profile.heightCm != null
        "weightKg" -> profile.weightKg != null
        "bodyFatPercent" -> profile.bodyFatPercent != null
        "goal" -> true
        "secondaryGoals" -> profile.secondaryGoals.isNotEmpty()
        "targetTimelineWeeks" -> profile.targetTimelineWeeks != null
        "workType" -> true
        "workHoursPerDay" -> profile.workHoursPerDay != null
        "dailySteps" -> profile.dailySteps != null
        "transportMode" -> true
        "workShift" -> true
        "extraSportSessionsPerWeek" -> profile.extraSportSessionsPerWeek > 0
        "maxSessionMinutes" -> profile.maxSessionMinutes != null
        "sleepHours" -> profile.sleepHours != null
        "sleepQuality" -> true
        "stressLevel" -> true
        "dailyEnergy" -> true
        "recoveryCapacity" -> true
        "mealsPerDay" -> profile.mealsPerDay > 0
        "breakfastPreference" -> true
        "dietaryPreferences" -> profile.dietaryPreferences.isNotEmpty()
        "likedFoods" -> profile.likedFoods.isNotEmpty()
        "dislikedFoods" -> profile.dislikedFoods.isNotEmpty()
        "allergies" -> profile.allergies.isNotEmpty()
        "cookingTimeAvailable" -> true
        "mealPrepPossible" -> true
        "monthlyFoodBudgetEur" -> profile.monthlyFoodBudgetEur != null
        "budgetScope" -> true
        "budgetPersons" -> profile.budgetPersons >= 1
        "trainingStatus" -> profile.trainingStatus != null
        "experience" -> true
        "trainingYears" -> profile.trainingYears != null
        "currentLoads" -> profile.currentLoads.isNotEmpty() || profile.trainingStatus == TrainingStatus.NEVER_TRAINED
        "trainingDaysPerWeek" -> profile.trainingDaysPerWeek > 0
        "technicalLevel" -> true
        "availableEquipment" -> profile.availableEquipment.isNotEmpty()
        "preferredExercises" -> profile.preferredExercises.isNotEmpty()
        "dislikedExercises" -> profile.dislikedExercises.isNotEmpty()
        "priorityMuscles" -> profile.priorityMuscles.isNotEmpty()
        "physicalLimitations" -> profile.physicalLimitations.isNotEmpty()
        "movementsToAvoid" -> profile.movementsToAvoid.isNotEmpty()
        else -> false
    }

    /**
     * Applica una risposta testuale alla domanda [id]. Ritorna il profilo
     * aggiornato. Valori non parsabili vengono ignorati (il profilo resta valido).
     */
    fun applyAnswer(profile: UserProfile, id: String, raw: String): UserProfile {
        val v = raw.trim()
        fun d() = v.replace(",", ".").toDoubleOrNull()
        fun i() = v.toIntOrNull()
        fun list() = v.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
        return when (id) {
            "age" -> i()?.let { profile.copy(ageYears = it) } ?: profile
            "sex" -> enumOrNull<Sex>(v)?.let { profile.copy(sex = it) } ?: profile
            "heightCm" -> d()?.let { profile.copy(heightCm = it) } ?: profile
            "weightKg" -> d()?.let { profile.copy(weightKg = it) } ?: profile
            "bodyFatPercent" -> d()?.let { profile.copy(bodyFatPercent = it) } ?: profile
            "goal" -> enumOrNull<Goal>(v)?.let { profile.copy(goal = it) } ?: profile
            "secondaryGoals" -> profile.copy(secondaryGoals = enumSet<Goal>(v))
            "targetTimelineWeeks" -> i()?.let { profile.copy(targetTimelineWeeks = it) } ?: profile
            "workType" -> enumOrNull<WorkType>(v)?.let { profile.copy(workType = it) } ?: profile
            "workHoursPerDay" -> d()?.let { profile.copy(workHoursPerDay = it) } ?: profile
            "dailySteps" -> i()?.let { profile.copy(dailySteps = it) } ?: profile
            "transportMode" -> enumOrNull<TransportMode>(v)?.let { profile.copy(transportMode = it) } ?: profile
            "workShift" -> enumOrNull<WorkShift>(v)?.let { profile.copy(workShift = it) } ?: profile
            "extraSportSessionsPerWeek" -> i()?.let { profile.copy(extraSportSessionsPerWeek = it) } ?: profile
            "maxSessionMinutes" -> i()?.let { profile.copy(maxSessionMinutes = it) } ?: profile
            "sleepHours" -> d()?.let { profile.copy(sleepHours = it) } ?: profile
            "sleepQuality" -> enumOrNull<Level3>(v)?.let { profile.copy(sleepQuality = it) } ?: profile
            "stressLevel" -> enumOrNull<Level3>(v)?.let { profile.copy(stressLevel = it) } ?: profile
            "dailyEnergy" -> enumOrNull<Level3>(v)?.let { profile.copy(dailyEnergy = it) } ?: profile
            "recoveryCapacity" -> enumOrNull<Level3>(v)?.let { profile.copy(recoveryCapacity = it) } ?: profile
            "mealsPerDay" -> i()?.let { profile.copy(mealsPerDay = it) } ?: profile
            "breakfastPreference" -> enumOrNull<BreakfastPreference>(v)
                ?.let { profile.copy(breakfastPreference = it) } ?: profile
            "dietaryPreferences" -> profile.copy(dietaryPreferences = enumSet<DietaryPreference>(v))
            "likedFoods" -> profile.copy(likedFoods = list())
            "dislikedFoods" -> profile.copy(dislikedFoods = list())
            "allergies" -> profile.copy(allergies = list())
            "cookingTimeAvailable" -> enumOrNull<CookingTimeAvailable>(v)
                ?.let { profile.copy(cookingTimeAvailable = it) } ?: profile
            "mealPrepPossible" -> profile.copy(mealPrepPossible = parseBool(v))
            "monthlyFoodBudgetEur" -> d()?.let {
                profile.copy(monthlyFoodBudgetEur = it.takeIf { e -> e > 0 })
            } ?: profile
            "budgetScope" -> enumOrNull<BudgetScope>(v)?.let { profile.copy(budgetScope = it) } ?: profile
            "budgetPersons" -> i()?.let { profile.copy(budgetPersons = it.coerceAtLeast(1)) } ?: profile
            "trainingStatus" -> enumOrNull<TrainingStatus>(v)?.let { profile.copy(trainingStatus = it) } ?: profile
            "experience" -> enumOrNull<TrainingExperience>(v)?.let { profile.copy(experience = it) } ?: profile
            "trainingYears" -> d()?.let { profile.copy(trainingYears = it) } ?: profile
            "currentLoads" -> profile.copy(currentLoads = parseLoads(v))
            "trainingDaysPerWeek" -> i()?.let { profile.copy(trainingDaysPerWeek = it) } ?: profile
            "technicalLevel" -> enumOrNull<TechnicalLevel>(v)?.let { profile.copy(technicalLevel = it) } ?: profile
            "availableEquipment" -> profile.copy(availableEquipment = enumSet<Equipment>(v))
            "preferredExercises" -> profile.copy(preferredExercises = list())
            "dislikedExercises" -> profile.copy(dislikedExercises = list())
            "priorityMuscles" -> profile.copy(priorityMuscles = enumSet<MuscleGroup>(v))
            "physicalLimitations" -> profile.copy(physicalLimitations = list())
            "movementsToAvoid" -> profile.copy(movementsToAvoid = list())
            else -> profile
        }
    }

    /** Etichette leggibili per il riepilogo. */
    fun summary(profile: UserProfile): String = buildString {
        appendLine("PROFILO PERSONALE")
        appendLine("- Età: ${profile.ageYears ?: "—"} anni")
        appendLine("- Sesso: ${profile.sex.name}")
        appendLine("- Altezza: ${profile.heightCm ?: "—"} cm")
        appendLine("- Peso: ${profile.weightKg ?: "—"} kg")
        profile.bodyFatPercent?.let { appendLine("- Massa grassa: $it%") }
        appendLine("- Obiettivo: ${profile.goal.label}")
        profile.targetTimelineWeeks?.let { appendLine("- Tempi desiderati: $it settimane") }
        appendLine("STILE DI VITA")
        appendLine("- Lavoro: ${profile.workType.label}")
        profile.workHoursPerDay?.let { appendLine("- Ore di lavoro: $it h/giorno") }
        profile.dailySteps?.let { appendLine("- Passi giornalieri: $it") }
        appendLine("- Spostamenti: ${profile.transportMode.label}")
        appendLine("SONNO E RECUPERO")
        profile.sleepHours?.let { appendLine("- Sonno: $it h/notte (${profile.sleepQuality.name})") }
        appendLine(
            "- Stress: ${profile.stressLevel.name} · Energia: ${profile.dailyEnergy.name} · " +
                "Recupero: ${profile.recoveryCapacity.name}"
        )
        appendLine("ALIMENTAZIONE E BUDGET")
        appendLine("- Pasti/giorno: ${profile.mealsPerDay}")
        appendLine("- Colazione: ${profile.breakfastPreference.label}")
        if (profile.dietaryPreferences.isNotEmpty()) {
            appendLine("- Preferenze: ${profile.dietaryPreferences.joinToString { it.label }}")
        }
        if (profile.allergies.isNotEmpty()) appendLine("- Allergie/intolleranze: ${profile.allergies.joinToString()}")
        profile.monthlyFoodBudgetEur?.let {
            appendLine(
                "- Budget mensile: %.0f € (${profile.budgetScope.label}, ${profile.budgetPersons} pers.)".format(it)
            )
        }
        appendLine("ESPERIENZA DI ALLENAMENTO")
        profile.trainingStatus?.let { appendLine("- Stato: ${it.label}") }
        appendLine("- Esperienza: ${profile.experience.label}")
        profile.trainingYears?.let { appendLine("- Anni di allenamento: $it") }
        if (profile.currentLoads.isNotEmpty()) {
            appendLine("- Carichi dichiarati: ${profile.currentLoads.entries.joinToString { "${it.key} ${it.value} kg" }}")
        }
        appendLine("- Giorni/settimana: ${profile.trainingDaysPerWeek}")
        appendLine("- Livello tecnico: ${profile.technicalLevel.label}")
        appendLine("- Attrezzatura: ${profile.availableEquipment.joinToString { it.label }.ifBlank { "—" }}")
        if (profile.priorityMuscles.isNotEmpty()) {
            appendLine("- Priorità muscolari: ${profile.priorityMuscles.joinToString { it.name }}")
        }
        appendLine("LIMITAZIONI")
        appendLine("- Limitazioni: ${profile.physicalLimitations.ifEmpty { listOf("nessuna") }.joinToString()}")
        appendLine("- Movimenti da evitare: ${profile.movementsToAvoid.ifEmpty { listOf("nessuno") }.joinToString()}")
    }

    private fun parseLoads(value: String): Map<String, Double> =
        value.split(",", ";", "\n").mapNotNull { token ->
            val match = Regex("^\\s*(.+?)\\s*[=:]\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:kg)?\\s*$")
                .matchEntire(token) ?: return@mapNotNull null
            val name = match.groupValues[1].trim()
            val kg = match.groupValues[2].replace(',', '.').toDoubleOrNull()
            if (name.isBlank() || kg == null || kg <= 0.0) null else name to kg
        }.toMap()

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        T::class.java.enumConstants?.firstOrNull { it.name.equals(value, ignoreCase = true) }

    private inline fun <reified T : Enum<T>> enumSet(value: String): Set<T> =
        value.split(",", ";").mapNotNull { part -> enumOrNull<T>(part.trim()) }.toSet()

    private fun parseBool(v: String): Boolean =
        v.equals("si", true) || v.equals("sì", true) || v.equals("true", true) ||
            v.equals("yes", true) || v == "1" || v.equals("ok", true)
}
