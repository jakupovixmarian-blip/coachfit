package dev.fitcoach.app.domain.model

enum class CoachChangeType(val label: String) {
    CALORIES("Calorie"), MACROS("Macronutrienti"), GRAMS("Grammature"),
    MEALS("Pasti"), EXERCISE("Esercizio"), SETS("Serie"), REPS("Ripetizioni"),
    LOADS("Carichi"), VOLUME("Volume"), FREQUENCY("Frequenza"), REST("Recuperi")
}

enum class CoachChangeStatus(val label: String) {
    PROPOSED("Proposta"), APPROVED("Approvata"), REJECTED("Rifiutata"), APPLIED("Applicata"), ROLLED_BACK("Annullata")
}

data class CoachProposal(
    val id: Long = 0L,
    val createdAt: Long,
    val changeType: CoachChangeType,
    val rationale: String,
    val previousValue: String,
    val newValue: String,
    val origin: String = "Coach AI",
    val status: CoachChangeStatus = CoachChangeStatus.PROPOSED,
    val appliedAt: Long? = null,
    val appliedTo: String = ""
)

data class CoachInsight(
    val title: String,
    val detail: String,
    val kind: InsightKind
)

enum class InsightKind { PROGRESS, STAGNATION, REGRESSION, BODY_COMP, INFO, ADHERENCE }
