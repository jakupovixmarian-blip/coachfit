package com.fitcoach.app.v2.domain.anamnesis

/** Campi raccolti dall'intervista 2.0. Nessun campo riceve un valore implicito. */
enum class AnamnesisField(val question: String, val requiredForGeneration: Boolean = false) {
    AGE("Quanti anni hai?", true),
    BIOLOGICAL_SEX("Qual è il tuo sesso biologico?", true),
    HEIGHT_CM("Qual è la tua altezza in centimetri?", true),
    WEIGHT_KG("Qual è il tuo peso attuale in chilogrammi?", true),
    BODY_FAT_PERCENT("Conosci la tua percentuale di massa grassa?"),
    PRIMARY_GOAL("Qual è il tuo obiettivo principale?", true),
    TRAINING_EXPERIENCE("Qual è la tua esperienza con i pesi?", true),
    TRAINING_YEARS("Da quanti anni ti alleni con i pesi?"),
    TRAINING_CONTINUITY("Con quale continuità ti sei allenato?"),
    CURRENT_SPLIT("Quale split o schema stai usando ora?"),
    KNOWN_EXERCISES("Quali esercizi conosci o esegui già?"),
    AVAILABLE_LOADS("Quali carichi utilizzi attualmente?"),
    WORK_TYPE("Che tipo di lavoro svolgi?"),
    DAILY_STEPS("Quanti passi fai mediamente ogni giorno?"),
    WORK_SHIFTS("Lavori su turni? Indica gli orari rilevanti."),
    SLEEP("Quante ore dormi e con quale qualità?"),
    STRESS("Come valuti lo stress attuale?"),
    RECOVERY("Come valuti il recupero tra gli allenamenti?"),
    INJURIES_LIMITATIONS("Hai infortuni, dolori o movimenti da evitare?"),
    EQUIPMENT("Quale attrezzatura hai a disposizione?", true),
    TRAINING_DAYS("Quanti giorni alla settimana puoi allenarti?", true),
    SESSION_DURATION("Quanto può durare una seduta?"),
    DIETARY_PREFERENCES("Quali preferenze o restrizioni alimentari hai?"),
    ALLERGIES_INTOLERANCES("Hai allergie o intolleranze?"),
    MEALS_PER_DAY("Quanti pasti preferisci fare?"),
    MEAL_TIMES("Quali sono gli orari abituali dei pasti?"),
    FOOD_BUDGET("Qual è il budget alimentare indicativo?"),
    HOUSEHOLD_SIZE("Per quante persone fai la spesa?"),
    COOKING_TIME("Quanto tempo hai per cucinare?"),
    AVAILABLE_FOODS("Quali alimenti hai già disponibili o preferisci?"),
    BREAKFAST_PREFERENCE("A colazione preferisci dolce, salato, entrambe o niente?", true)
}

enum class AnswerStatus { PROVIDED, UNKNOWN }

data class AnamnesisAnswer(
    val status: AnswerStatus,
    val value: String? = null,
    val updatedAt: Long
) {
    init {
        require(status == AnswerStatus.UNKNOWN || !value.isNullOrBlank())
        require(status == AnswerStatus.UNKNOWN || value!!.length <= MAX_ANSWER_LENGTH)
    }

    companion object {
        const val MAX_ANSWER_LENGTH = 2_000
        fun unknown(now: Long) = AnamnesisAnswer(AnswerStatus.UNKNOWN, null, now)
        fun provided(value: String, now: Long) = AnamnesisAnswer(AnswerStatus.PROVIDED, value.trim(), now)
    }
}

data class AnamnesisSession(
    val id: String,
    val answers: Map<AnamnesisField, AnamnesisAnswer> = emptyMap(),
    val confirmedAt: Long? = null
)

data class AnamnesisReview(
    val missingAnswers: List<AnamnesisField>,
    val unresolvedRequired: List<AnamnesisField>,
    val contradictions: List<String>
) {
    val canConfirm: Boolean
        get() = missingAnswers.isEmpty() && unresolvedRequired.isEmpty() && contradictions.isEmpty()
}

object AnamnesisEngine {
    fun nextQuestion(session: AnamnesisSession): AnamnesisField? =
        AnamnesisField.entries.firstOrNull { it !in session.answers }

    fun answer(session: AnamnesisSession, field: AnamnesisField, answer: AnamnesisAnswer): AnamnesisSession =
        session.copy(answers = session.answers + (field to answer), confirmedAt = null)

    fun review(session: AnamnesisSession): AnamnesisReview {
        val missing = AnamnesisField.entries.filter { it.requiredForGeneration && it !in session.answers }
        val unresolved = AnamnesisField.entries.filter {
            it.requiredForGeneration && session.answers[it]?.status == AnswerStatus.UNKNOWN
        }
        return AnamnesisReview(missing, unresolved, detectContradictions(session))
    }

    fun confirm(session: AnamnesisSession, now: Long): AnamnesisSession {
        require(review(session).canConfirm) { "L'anamnesi non può essere confermata" }
        return session.copy(confirmedAt = now)
    }

    fun summary(session: AnamnesisSession): List<String> = AnamnesisField.entries.mapNotNull { field ->
        val answer = session.answers[field] ?: return@mapNotNull null
        val value = answer.value ?: "Non lo so"
        "${field.name}: $value"
    }

    private fun detectContradictions(session: AnamnesisSession): List<String> = buildList {
        numeric(session, AnamnesisField.AGE)?.let { if (it !in 13.0..100.0) add("L'età deve essere tra 13 e 100 anni.") }
        numeric(session, AnamnesisField.HEIGHT_CM)?.let { if (it !in 100.0..250.0) add("L'altezza deve essere tra 100 e 250 cm.") }
        numeric(session, AnamnesisField.WEIGHT_KG)?.let { if (it !in 30.0..400.0) add("Il peso deve essere tra 30 e 400 kg.") }
        numeric(session, AnamnesisField.TRAINING_DAYS)?.let { if (it !in 1.0..7.0) add("I giorni di allenamento devono essere tra 1 e 7.") }
        numeric(session, AnamnesisField.BODY_FAT_PERCENT)?.let { if (it !in 2.0..70.0) add("La massa grassa deve essere tra 2% e 70%.") }
        numericFields.forEach { field ->
            val answer = session.answers[field]
            if (answer?.status == AnswerStatus.PROVIDED && numeric(session, field) == null) {
                add("Il valore per ${field.name} deve essere numerico.")
            }
        }
    }

    private fun numeric(session: AnamnesisSession, field: AnamnesisField): Double? =
        session.answers[field]?.value?.replace(',', '.')?.toDoubleOrNull()

    private val numericFields = setOf(
        AnamnesisField.AGE,
        AnamnesisField.HEIGHT_CM,
        AnamnesisField.WEIGHT_KG,
        AnamnesisField.BODY_FAT_PERCENT,
        AnamnesisField.TRAINING_YEARS,
        AnamnesisField.DAILY_STEPS,
        AnamnesisField.TRAINING_DAYS,
        AnamnesisField.MEALS_PER_DAY,
        AnamnesisField.FOOD_BUDGET,
        AnamnesisField.HOUSEHOLD_SIZE,
        AnamnesisField.SESSION_DURATION
    )
}
