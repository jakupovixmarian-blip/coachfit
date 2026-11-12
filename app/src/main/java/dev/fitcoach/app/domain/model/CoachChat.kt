package dev.fitcoach.app.domain.model

enum class ChatRole(val wire: String, val label: String) {
    USER("user", "Tu"),
    ASSISTANT("assistant", "Coach AI"),
    SYSTEM("system", "Sistema");

    companion object {
        fun fromWire(value: String): ChatRole = entries.firstOrNull { it.wire == value } ?: ASSISTANT
    }
}

/**
 * One turn of the coach chat. [actionSummary] records what the coach actually
 * changed in the app, so a user can see why the program or the plan moved.
 */
data class CoachChatMessage(
    val id: Long = 0L,
    val role: ChatRole,
    val content: String,
    val createdAt: Long = 0L,
    val actionSummary: String = ""
) {
    val isUser: Boolean get() = role == ChatRole.USER
}

/**
 * A reference the coach may reason on. Built-in notes carry a verifiable source;
 * user notes are the material the user brings in (for example from Project
 * Invictus). The coach is instructed to use only the supplied notes and to say
 * when a request is not covered, instead of inventing values.
 */
data class KnowledgeNote(
    val id: Long = 0L,
    val topic: KnowledgeTopic,
    val title: String,
    val content: String,
    val source: String,
    val builtIn: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = 0L
)

enum class KnowledgeTopic(val label: String) {
    VOLUME("Volume"),
    FREQUENCY("Frequenza"),
    INTENSITY("Intensita"),
    PROGRESSION("Progressione"),
    TECHNIQUE("Tecnica"),
    DELOAD("Scarico"),
    WARMUP("Riscaldamento"),
    CARDIO("Cardio"),
    RECOVERY("Recupero"),
    NUTRITION("Alimentazione"),
    OTHER("Altro")
}
