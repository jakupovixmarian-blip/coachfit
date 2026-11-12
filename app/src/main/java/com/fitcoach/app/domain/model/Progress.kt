package com.fitcoach.app.domain.model

enum class CoachAuthorizationMode(val label: String, val description: String) {
    CONSULTATIVE("Consulenziale", "Il coach analizza e propone. Nulla viene applicato automaticamente."),
    ADAPTIVE("Adattiva", "Aggiustamenti ordinari autorizzati possono essere applicati automaticamente."),
    CONFIRMATION("Con conferma", "Le modifiche richiedono sempre la tua approvazione.")
}


data class WeightRecord(
    val id: Long = 0,
    val profileId: Long = 1L,
    val dateEpochDay: Long,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val note: String = ""
)

data class CircumferenceRecord(
    val id: Long = 0,
    val profileId: Long = 1L,
    val dateEpochDay: Long,
    val site: String, // es. "vita", "torace", "braccio dx"
    val valueCm: Double
)

data class ProgressRecord(
    val id: Long = 0,
    val profileId: Long = 1L,
    val dateEpochDay: Long,
    val caloriesConsumed: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val dietAdherencePercent: Double? = null,
    val trainingVolumeKg: Double? = null,
    val note: String = ""
)

data class CheckIn(
    val id: Long = 0,
    val profileId: Long = 1L,
    val weekStartEpochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val avgWeightKg: Double? = null,
    val weightTrendKgPerWeek: Double? = null,
    val avgCalories: Double? = null,
    val dietAdherencePercent: Double? = null,
    val trainingSessionsCompleted: Int = 0,
    val trainingSessionsPlanned: Int = 0,
    val performanceTrend: String = "",
    val sleepQualityAvg: Double? = null,
    val painNotes: String = "",
    val summary: String = ""
)

data class ProgramChange(
    val id: Long = 0,
    val profileId: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val domain: ChangeDomain,
    val status: ChangeStatus,
    val title: String,
    val situation: String,
    val problem: String,
    val dataUsed: String,
    val proposedChange: String,
    val rationale: String,
    val expectedOutcome: String,
    val contraindications: String,
    val payloadJson: String = "",
    val revertPayloadJson: String = "",
    val appliedAt: Long? = null,
    val source: String = "ADAPTIVE_ENGINE"
)

data class ChatMessage(
    val id: Long = 0,
    val profileId: Long = 1L,
    val role: ChatRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pendingChangeId: Long? = null,
    val isError: Boolean = false
)

data class AiProviderSettings(
    val id: Long = 1L,
    val provider: AiProvider = AiProvider.OPENAI,
    val baseUrl: String = AiProvider.OPENAI.defaultBaseUrl,
    val model: String = AiProvider.OPENAI.defaultModel,
    val useBackendProxy: Boolean = false,
    val backendBaseUrl: String = "",
    val temperature: Double = 0.4,
    val maxTokens: Int = 1200,
    val lastTestOk: Boolean? = null,
    val lastTestMessage: String = "",
    val lastTestAt: Long? = null
)

data class NotificationRecord(
    val id: Long = 0,
    val profileId: Long = 1L,
    val kind: String, // MEAL_BREAKFAST, WORKOUT, HYDRATION, CHECKIN, WEIGHT
    val title: String,
    val body: String,
    val triggerTime: String, // HH:mm
    val enabled: Boolean = true,
    val requestCode: Int = 0
)

data class WeeklyWeightStats(
    val weekStartEpochDay: Long,
    val avgWeightKg: Double?,
    val samples: Int,
    val minWeightKg: Double?,
    val maxWeightKg: Double?
)

data class ProgressPhoto(
    val id: Long = 0,
    val uri: String,
    val capturedAt: Long,
    val pose: String = "",
    val note: String = "",
    val consentForAi: Boolean = false
)

data class WeightTrend(
    val weeklyAverages: List<WeeklyWeightStats>,
    val trendKgPerWeek: Double?,
    val weeksOfData: Int,
    val isReliable: Boolean
)
