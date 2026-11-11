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
            "experience", sections[4], "Qual è il tuo livello di esperienza?",
            kind = Kind.CHOICE, options = TrainingExperience.entries.map { it.name }
        ),
        Question(
            "trainingYears", sections[4], "Da quanti anni ti alleni?", kind = Kind.NUMBER,
            required = false
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

    /** La prossima domanda da porre (prima non risposta), o null se completata. */
    fun nextQuestion(profile: UserProfile, skipOptional: Boolean = false): Question? =
        questions.firstOrNull { q ->
            !isAnswered(profile, q.id) && (q.required || !skipOptional)
        }

    /** Domande obbligatorie ancora senza risposta. */
    fun missingRequired(profile: UserProfile): List<Question> =
        questions.filter { it.required && !isAnswered(profile, it.id) }

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
        "experience" -> true
        "trainingYears" -> profile.trainingYears != null
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
            "experience" -> enumOrNull<TrainingExperience>(v)?.let { profile.copy(experience = it) } ?: profile
            "trainingYears" -> d()?.let { profile.copy(trainingYears = it) } ?: profile
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
        appendLine("- Esperienza: ${profile.experience.label}")
        profile.trainingYears?.let { appendLine("- Anni di allenamento: $it") }
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

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        T::class.java.enumConstants?.firstOrNull { it.name.equals(value, ignoreCase = true) }

    private inline fun <reified T : Enum<T>> enumSet(value: String): Set<T> =
        value.split(",", ";").mapNotNull { part -> enumOrNull<T>(part.trim()) }.toSet()

    private fun parseBool(v: String): Boolean =
        v.equals("si", true) || v.equals("sì", true) || v.equals("true", true) ||
            v.equals("yes", true) || v == "1" || v.equals("ok", true)
}
