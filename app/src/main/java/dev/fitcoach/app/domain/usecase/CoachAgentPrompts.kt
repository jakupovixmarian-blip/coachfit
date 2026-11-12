package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.KnowledgeNote
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

// ---- Contracts --------------------------------------------------------------

/** One exercise of one session, exactly as the model proposed it. */
data class ProgramDraftExercise(
    val exerciseId: Long,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val rir: Int?,
    val restSeconds: Int,
    val notes: String
)

data class ProgramDraftDay(val label: String, val exercises: List<ProgramDraftExercise>)

/** A full training program proposed by the model. The app only checks coherence. */
data class ProgramDraft(
    val name: String,
    val rationale: String,
    val days: List<ProgramDraftDay>,
    val raw: String
)

data class MealPlanDraftItem(
    val slot: MealSlot,
    val foodId: Long,
    val grams: Double,
    val preparation: String
)

data class MealPlanDraftDay(val dayIndex: Int, val items: List<MealPlanDraftItem>)

data class MealPlanDraft(
    val name: String,
    val rationale: String,
    val days: List<MealPlanDraftDay>,
    val raw: String
)

/** An answer of the intake conversation: what it says, and what it learned. */
data class AnamnesiReply(
    val reply: String,
    val profilePatch: Map<String, String>,
    val complete: Boolean
)

/** An action the chat asked the app to perform on its behalf. */
sealed interface CoachAction {
    data class GenerateProgram(val notes: String) : CoachAction
    data class GenerateMealPlan(val notes: String) : CoachAction
    data class UpdateProfile(val patch: Map<String, String>) : CoachAction
    data class SetRest(val exerciseName: String, val seconds: Int) : CoachAction
    data class AddExercise(val routineLabel: String, val exerciseName: String, val sets: Int?) : CoachAction
    data class RemoveExercise(val routineLabel: String, val exerciseName: String) : CoachAction
}

data class AgentReply(val reply: String, val actions: List<CoachAction>)

/**
 * Builds every prompt the agent needs and parses its answers.
 *
 * The app never supplies training or nutrition values on its own: prompts carry
 * the user's data, the real catalogs and the declared knowledge notes, and the
 * parsers only reject answers that are structurally incoherent (unknown ids,
 * impossible numbers). Deciding sets, exercises and grams is the model's job.
 */
@Singleton
class CoachAgentPrompts @Inject constructor(
    private val json: Json
) {

    // ---- shared blocks --------------------------------------------------

    /** The rules that hold for every call, including the no-invention rule. */
    private fun commonRules(): String = """
        REGOLE NON NEGOZIABILI:
        - Non inventare mai valori nutrizionali: puoi riferirti solo agli alimenti
          e agli id presenti nell'archivio fornito, il resto lo calcola l'app.
        - Non inventare esercizi: puoi riferirti solo agli id del catalogo fornito.
        - Puoi usare solo l'attrezzatura dichiarata dall'utente e rispettare
          infortuni, allergie, cibi non graditi, giorni e minuti disponibili.
        - Se un'informazione manca, chiedila invece di inventarla.
        - Rispondi sempre in italiano.
    """.trimIndent()

    private fun knowledgeBlock(notes: List<KnowledgeNote>): String {
        if (notes.isEmpty()) {
            return "FONTI: nessuna nota caricata. Basati sulle tue conoscenze generali e dillo all'utente."
        }
        val body = notes.joinToString("\n") { note ->
            "- [${note.topic.label}] ${note.title}: ${note.content} (Fonte: ${note.source})"
        }
        return """
            FONTI E PRINCIPI DA USARE (sono l'unico riferimento scientifico ammesso):
            $body
        """.trimIndent()
    }

    fun profileBlock(profile: UserProfile): String = """
        PROFILO UTENTE:
        nome=${profile.name.ifBlank { "non indicato" }}, eta=${profile.age ?: "?"},
        sesso=${profile.sex}, altezza=${profile.heightCm ?: "?"} cm, peso=${profile.weightKg ?: "?"} kg,
        massa grassa=${profile.bodyFatPercent ?: "?"} %, obiettivo=${profile.goal},
        esperienza=${profile.experience}, anni di allenamento=${profile.trainingYears ?: "?"},
        attrezzatura="${profile.equipment.ifBlank { "non indicata" }}",
        giorni disponibili=${profile.availableDays ?: "?"}, minuti per seduta=${profile.sessionMinutes ?: "?"},
        infortuni="${profile.injuries.ifBlank { "nessuno" }}", lavoro="${profile.jobActivity}",
        sonno=${profile.sleepHours ?: "?"} h, stress=${profile.stressLevel ?: "?"}/10,
        recupero=${profile.recoveryLevel ?: "?"}/10,
        alimentazione="${profile.dietPreference.ifBlank { "nessuna" }}",
        allergie="${profile.allergies.ifBlank { "nessuna" }}",
        pasti al giorno=${profile.mealsPerDay ?: "?"}, orari="${profile.mealTimes}",
        budget settimanale=${profile.weeklyBudget ?: "?"}, minuti cucina=${profile.cookingMinutes ?: "?"},
        cibi graditi="${profile.likedFoods}", cibi non graditi="${profile.dislikedFoods}"
    """.trimIndent()

    private fun exerciseCatalogBlock(catalog: List<Exercise>): String =
        catalog.joinToString("\n") { ex ->
            "- id=${ex.id} | ${ex.name} | muscolo ${ex.primaryMuscle.label} | " +
                "attrezzo ${ex.equipment.label} | schema ${ex.pattern.label}"
        }

    private fun foodCatalogBlock(foods: List<Food>): String =
        foods.joinToString("\n") { food ->
            "- id=${food.id} | ${food.name}${if (food.brand.isNotBlank()) " (${food.brand})" else ""} | " +
                "${food.kcalPer100g.toInt()} kcal/100g | P ${food.proteinPer100g} C ${food.carbsPer100g} G ${food.fatPer100g} | ${food.state.label}"
        }

    // ---- anamnesi -------------------------------------------------------

    fun anamnesiPrompt(profile: UserProfile, conversation: List<Pair<String, String>>): String {
        val history = conversation.joinToString("\n") { (role, text) -> "$role: $text" }
        return """
            Sei un coach che raccoglie l'anamnesi di un nuovo utente con una conversazione.
            ${commonRules()}

            Devi raccogliere almeno: nome, eta, sesso, altezza, peso, obiettivo,
            esperienza e anni di allenamento, attrezzatura disponibile, giorni a settimana,
            minuti per seduta, infortuni o limitazioni, sonno, stress, recupero,
            preferenze alimentari, allergie, pasti al giorno, budget e tempo di cucina.

            Fai poche domande per volta, in modo naturale. Quando hai abbastanza dati,
            imposta "complete": true.

            DATI GIA RACCOLTI:
            ${profileBlock(profile)}

            CONVERSAZIONE FINORA:
            $history

            Rispondi SOLO con JSON valido:
            {"reply":"la tua domanda o commento in italiano","complete":false,
             "profile":{"name":"","age":30,"sex":"MALE","heightCm":180,"weightKg":80,
             "goal":"MUSCLE_GAIN","experience":"BEGINNER","trainingYears":0.5,
             "equipment":"BARBELL,DUMBBELL","availableDays":3,"sessionMinutes":60,
             "injuries":"","sleepHours":7,"stressLevel":5,"recoveryLevel":6,
             "dietPreference":"","allergies":"","mealsPerDay":4,"mealTimes":"08:00,13:00,17:00,20:30",
             "weeklyBudget":60,"cookingMinutes":30}}
            Inserisci nel campo "profile" solo i campi che hai appreso in questo turno.
        """.trimIndent()
    }

    fun parseAnamnesi(raw: String): AnamnesiReply {
        val root = rootObject(raw)
        val reply = root["reply"]?.jsonPrimitive?.content.orEmpty()
        val complete = root["complete"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val patch = (root["profile"] as? JsonObject)?.let { obj ->
            obj.entries.mapNotNull { (key, value) ->
                val text = value.jsonPrimitive.contentOrNullSafe() ?: return@mapNotNull null
                key to text
            }.toMap()
        }.orEmpty()
        if (reply.isBlank() && patch.isEmpty()) {
            throw AppError.InvalidResponse("L'agente non ha risposto in modo utile")
        }
        return AnamnesiReply(reply, patch, complete)
    }

    // ---- program --------------------------------------------------------

    fun programPrompt(profile: UserProfile, catalog: List<Exercise>, knowledge: List<KnowledgeNote>, notes: String): String = """
        Sei il programmatore di allenamento dell'app. Devi progettare un programma
        completo di allenamento per questo utente, decidendo tu split, esercizi,
        serie, ripetizioni, RIR e recuperi in base all'analisi dei suoi dati.
        ${commonRules()}

        ${knowledgeBlock(knowledge)}

        ${profileBlock(profile)}

        RICHIESTE AGGIUNTIVE DELL'UTENTE (se presenti):
        ${notes.ifBlank { "nessuna" }}

        CATALOGO ESERCIZI (usa solo questi id):
        ${exerciseCatalogBlock(catalog)}

        Il numero di sedute deve rispettare i giorni disponibili. Ogni esercizio
        deve essere compatibile con l'attrezzatura dichiarata.

        Rispondi SOLO con JSON valido:
        {"name":"nome del programma","rationale":"spiegazione breve in italiano",
         "days":[{"label":"Spinta","exercises":[
           {"exerciseId":12,"sets":4,"repsMin":6,"repsMax":8,"rir":2,"restSeconds":150,"notes":""}]}]}
    """.trimIndent()

    fun parseProgram(raw: String, knownExerciseIds: Set<Long>): ProgramDraft {
        val root = rootObject(raw)
        val name = root["name"]?.jsonPrimitive?.content.orEmpty().ifBlank { "Programma AI" }
        val rationale = root["rationale"]?.jsonPrimitive?.content.orEmpty()
        val days = (root["days"] as? JsonArray)?.mapNotNull { element ->
            val day = element as? JsonObject ?: return@mapNotNull null
            val label = day["label"]?.jsonPrimitive?.content.orEmpty().ifBlank { "Seduta" }
            val exercises = (day["exercises"] as? JsonArray)?.mapNotNull { exElement ->
                val obj = exElement as? JsonObject ?: return@mapNotNull null
                val id = obj["exerciseId"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                if (id !in knownExerciseIds) return@mapNotNull null
                val sets = obj["sets"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                if (sets < 1 || sets > 20) return@mapNotNull null
                val repsMin = obj["repsMin"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
                val repsMax = obj["repsMax"]?.jsonPrimitive?.content?.toIntOrNull() ?: repsMin
                val rest = obj["restSeconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 90
                if (rest < 0 || rest > 900) return@mapNotNull null
                ProgramDraftExercise(
                    exerciseId = id,
                    sets = sets,
                    repsMin = repsMin.coerceAtMost(repsMax).coerceAtLeast(1),
                    repsMax = repsMax.coerceAtLeast(repsMin).coerceAtMost(100),
                    rir = obj["rir"]?.jsonPrimitive?.content?.toIntOrNull(),
                    restSeconds = rest,
                    notes = obj["notes"]?.jsonPrimitive?.content.orEmpty()
                )
            }.orEmpty()
            if (exercises.isEmpty()) return@mapNotNull null
            ProgramDraftDay(label, exercises)
        }.orEmpty()

        if (days.isEmpty()) {
            throw AppError.InvalidResponse("Il modello non ha proposto nessuna seduta valida")
        }
        return ProgramDraft(name, rationale, days, raw)
    }

    // ---- meal plan ------------------------------------------------------

    /**
     * One day of the meal plan, asked on its own.
     *
     * A whole week in a single answer overflows the output budget of small models
     * and comes back truncated, which is why the plan used to fail while the
     * program (a much shorter answer) succeeded. Asking one day at a time keeps
     * every answer small; the app joins the days afterwards.
     */
    fun mealPlanDayPrompt(
        profile: UserProfile,
        foods: List<Food>,
        knowledge: List<KnowledgeNote>,
        notes: String,
        dayIndex: Int,
        dayCount: Int,
        previousDays: String
    ): String = """
        Sei il nutrizionista dell'app. Costruisci il giorno ${dayIndex + 1} di $dayCount
        di un piano alimentare settimanale, decidendo tu alimenti, grammature e
        distribuzione dei pasti in base all'analisi dei dati dell'utente.
        ${commonRules()}

        ${knowledgeBlock(knowledge)}

        ${profileBlock(profile)}

        RICHIESTE AGGIUNTIVE DELL'UTENTE (se presenti):
        ${notes.ifBlank { "nessuna" }}

        GIORNI GIA COSTRUITI (mantieni uno stile coerente, non ripetere gli stessi
        identici pasti se non ha senso):
        ${previousDays.ifBlank { "nessuno: questo e il primo giorno" }}

        ARCHIVIO ALIMENTI (usa solo questi id):
        ${foodCatalogBlock(foods)}

        Rispondi SOLO con JSON valido per QUESTO SOLO GIORNO:
        {"name":"Piano alimentare","rationale":"spiegazione breve in italiano",
         "dayIndex":$dayIndex,
         "meals":[{"slot":"BREAKFAST","foodId":12,"grams":80,"preparation":""}]}
        Gli slot ammessi sono: ${MealSlot.entries.joinToString(", ") { it.name }}.
        Il campo dayIndex deve valere esattamente $dayIndex.
    """.trimIndent()

    /** Parses the answer of [mealPlanDayPrompt]: one day, or a whole "days" array. */
    fun parseMealPlanDay(raw: String, knownFoodIds: Set<Long>, expectedDayIndex: Int): MealPlanDraftDay {
        val root = rootObject(raw)
        val meals = root["meals"] as? JsonArray
            ?: (root["days"] as? JsonArray)?.firstOrNull()?.jsonObject?.get("meals") as? JsonArray
            ?: throw AppError.InvalidResponse("Il modello non ha proposto pasti per il giorno ${expectedDayIndex + 1}")
        val index = root["dayIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: expectedDayIndex
        val items = meals.mapNotNull { itemElement ->
            val obj = itemElement as? JsonObject ?: return@mapNotNull null
            val slotName = obj["slot"]?.jsonPrimitive?.content.orEmpty().uppercase()
            val slot = MealSlot.entries.firstOrNull { it.name == slotName } ?: return@mapNotNull null
            val foodId = obj["foodId"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
            if (foodId !in knownFoodIds) return@mapNotNull null
            val grams = obj["grams"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
            if (grams <= 0 || grams > 2000) return@mapNotNull null
            MealPlanDraftItem(slot, foodId, grams, obj["preparation"]?.jsonPrimitive?.content.orEmpty())
        }
        if (items.isEmpty()) {
            throw AppError.InvalidResponse("Nessun pasto valido per il giorno ${expectedDayIndex + 1}")
        }
        return MealPlanDraftDay(index, items)
    }

    /** Reads name and rationale from any of the per-day answers. */
    fun parseMealPlanHeader(raw: String): Pair<String, String> {
        val root = runCatching { rootObject(raw) }.getOrNull() ?: return "" to ""
        val name = root["name"]?.jsonPrimitive?.content.orEmpty()
        val rationale = root["rationale"]?.jsonPrimitive?.content.orEmpty()
        return name to rationale
    }

    // ---- chat -----------------------------------------------------------

    fun chatPrompt(
        profile: UserProfile?,
        program: List<Routine>,
        plan: MealPlan?,
        catalog: List<Exercise>,
        foods: List<Food>,
        knowledge: List<KnowledgeNote>
    ): String = """
        Sei l'agente che gestisce in completa autonomia il programma di allenamento e
        il piano alimentare di questa persona. Rispondi ai suoi messaggi e, quando
        serve, richiedi azioni che l'app eseguira per te.
        ${commonRules()}

        ${knowledgeBlock(knowledge)}

        ${profile?.let { profileBlock(it) } ?: "PROFILO: non ancora compilato."}

        PROGRAMMA ATTUALE:
        ${program.joinToString("\n") { routine ->
            "- ${routine.dayLabel.ifBlank { routine.name }} (id=${routine.id})"
        }.ifBlank { "nessun programma attivo" }}

        PIANO ALIMENTARE ATTUALE: ${plan?.name ?: "nessun piano attivo"}

        CATALOGO ESERCIZI (id usabili):
        ${exerciseCatalogBlock(catalog)}

        ARCHIVIO ALIMENTI (id usabili):
        ${foodCatalogBlock(foods.take(120))}

        AZIONI DISPONIBILI:
        - generate_program: rigenera da zero il programma di allenamento.
        - generate_meal_plan: rigenera da zero il piano alimentare.
        - update_profile: aggiorna i dati del profilo (es. attrezzatura, giorni, infortuni).
        - set_rest: cambia il recupero di un esercizio del programma attuale.
        - add_exercise / remove_exercise: aggiungi o togli un esercizio da una seduta.

        Rispondi SOLO con JSON valido:
        {"reply":"la tua risposta in italiano",
         "actions":[{"type":"set_rest","exercise":"Panca piana","seconds":150}]}
        Il campo "actions" puo essere vuoto: [] se non serve nessuna azione.
    """.trimIndent()

    fun parseAgentReply(raw: String): AgentReply {
        val root = rootObject(raw)
        val reply = root["reply"]?.jsonPrimitive?.content.orEmpty()
        val actions = (root["actions"] as? JsonArray)?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            when (obj["type"]?.jsonPrimitive?.content.orEmpty()) {
                "generate_program" -> CoachAction.GenerateProgram(
                    obj["notes"]?.jsonPrimitive?.content.orEmpty()
                )
                "generate_meal_plan" -> CoachAction.GenerateMealPlan(
                    obj["notes"]?.jsonPrimitive?.content.orEmpty()
                )
                "update_profile" -> {
                    val patch = (obj["fields"] as? JsonObject)?.entries?.mapNotNull { (k, v) ->
                        v.jsonPrimitive.contentOrNullSafe()?.let { k to it }
                    }?.toMap().orEmpty()
                    if (patch.isEmpty()) null else CoachAction.UpdateProfile(patch)
                }
                "set_rest" -> {
                    val name = obj["exercise"]?.jsonPrimitive?.content.orEmpty()
                    val seconds = obj["seconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                    if (name.isBlank() || seconds <= 0) null else CoachAction.SetRest(name, seconds)
                }
                "add_exercise" -> {
                    val routine = obj["routine"]?.jsonPrimitive?.content.orEmpty()
                    val exercise = obj["exercise"]?.jsonPrimitive?.content.orEmpty()
                    if (exercise.isBlank()) null else CoachAction.AddExercise(
                        routine, exercise, obj["sets"]?.jsonPrimitive?.content?.toIntOrNull()
                    )
                }
                "remove_exercise" -> {
                    val routine = obj["routine"]?.jsonPrimitive?.content.orEmpty()
                    val exercise = obj["exercise"]?.jsonPrimitive?.content.orEmpty()
                    if (exercise.isBlank()) null else CoachAction.RemoveExercise(routine, exercise)
                }
                else -> null
            }
        }.orEmpty()
        if (reply.isBlank() && actions.isEmpty()) {
            throw AppError.InvalidResponse("Risposta dell'agente non valida")
        }
        return AgentReply(reply, actions)
    }

    // ---- profile patch --------------------------------------------------

    /**
     * Applies the fields the model learned, leaving every other value untouched.
     * Unknown keys and unparsable numbers are ignored, never guessed.
     */
    fun applyProfilePatch(profile: UserProfile, patch: Map<String, String>): UserProfile {
        fun str(key: String) = patch[key]?.trim()?.takeIf { it.isNotEmpty() }
        fun int(key: String) = str(key)?.toIntOrNull()
        fun dbl(key: String) = str(key)?.replace(',', '.')?.toDoubleOrNull()
        fun sex(key: String) = str(key)?.uppercase()?.let { value ->
            Sex.entries.firstOrNull { it.name == value }
        }
        fun goal(key: String) = str(key)?.uppercase()?.let { value ->
            Goal.entries.firstOrNull { it.name == value }
        }
        fun experience(key: String) = str(key)?.uppercase()?.let { value ->
            ExperienceLevel.entries.firstOrNull { it.name == value }
        }

        return profile.copy(
            name = str("name") ?: profile.name,
            age = int("age") ?: profile.age,
            sex = sex("sex") ?: profile.sex,
            heightCm = dbl("heightCm") ?: profile.heightCm,
            weightKg = dbl("weightKg") ?: profile.weightKg,
            bodyFatPercent = dbl("bodyFatPercent") ?: profile.bodyFatPercent,
            goal = goal("goal") ?: profile.goal,
            experience = experience("experience") ?: profile.experience,
            trainingYears = dbl("trainingYears") ?: profile.trainingYears,
            consistencyWeeks = int("consistencyWeeks") ?: profile.consistencyWeeks,
            knownLoads = str("knownLoads") ?: profile.knownLoads,
            jobActivity = str("jobActivity") ?: profile.jobActivity,
            dailySteps = int("dailySteps") ?: profile.dailySteps,
            sleepHours = dbl("sleepHours") ?: profile.sleepHours,
            stressLevel = int("stressLevel") ?: profile.stressLevel,
            recoveryLevel = int("recoveryLevel") ?: profile.recoveryLevel,
            injuries = str("injuries") ?: profile.injuries,
            equipment = str("equipment") ?: profile.equipment,
            availableDays = int("availableDays") ?: profile.availableDays,
            sessionMinutes = int("sessionMinutes") ?: profile.sessionMinutes,
            dietPreference = str("dietPreference") ?: profile.dietPreference,
            allergies = str("allergies") ?: profile.allergies,
            mealsPerDay = int("mealsPerDay") ?: profile.mealsPerDay,
            mealTimes = str("mealTimes") ?: profile.mealTimes,
            weeklyBudget = dbl("weeklyBudget") ?: profile.weeklyBudget,
            cookingMinutes = int("cookingMinutes") ?: profile.cookingMinutes,
            likedFoods = str("likedFoods") ?: profile.likedFoods,
            dislikedFoods = str("dislikedFoods") ?: profile.dislikedFoods
        )
    }

    // ---- helpers --------------------------------------------------------

    private fun rootObject(raw: String): JsonObject =
        runCatching { json.parseToJsonElement(extractJson(raw)).jsonObject }
            .getOrElse { throw AppError.InvalidResponse("Il modello non ha restituito JSON valido") }

    fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start >= 0 && end > start) return raw.substring(start, end + 1)
        throw AppError.InvalidResponse("Nessun oggetto JSON nella risposta del modello")
    }
}

/** Reads a JSON scalar as text, or null when it is not a scalar. */
private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
    runCatching { content }.getOrNull()
