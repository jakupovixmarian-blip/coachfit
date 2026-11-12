package dev.fitcoach.app.domain.model

enum class MeasurementType(val label: String, val unit: String, val group: String) {
    WEIGHT("Peso", "kg", "Generale"),
    BODY_FAT("Massa grassa", "%", "Generale"),
    WAIST("Vita", "cm", "Torso"),
    ABDOMEN("Addome", "cm", "Torso"),
    CHEST("Torace", "cm", "Torso"),
    SHOULDERS("Spalle", "cm", "Torso"),
    NECK("Collo", "cm", "Torso"),
    ARM_R("Braccio destro", "cm", "Braccia"),
    ARM_L("Braccio sinistro", "cm", "Braccia"),
    FOREARM_R("Avambraccio destro", "cm", "Braccia"),
    FOREARM_L("Avambraccio sinistro", "cm", "Braccia"),
    THIGH_R("Coscia destra", "cm", "Gambe"),
    THIGH_L("Coscia sinistra", "cm", "Gambe"),
    CALF_R("Polpaccio destro", "cm", "Gambe"),
    CALF_L("Polpaccio sinistro", "cm", "Gambe"),
    CUSTOM("Personalizzata", "", "Altre")
}

data class Measurement(
    val id: Long = 0L,
    val type: MeasurementType,
    val customLabel: String = "",
    val dateEpochDay: Long,
    val value: Double,
    val notes: String = "",
    val createdAt: Long = 0L
) {
    val label: String get() = if (type == MeasurementType.CUSTOM && customLabel.isNotBlank()) customLabel else type.label
    val unit: String get() = type.unit.ifBlank { "" }
}

enum class PhotoPose(val label: String) {
    FRONT("Frontale"), SIDE("Laterale"), BACK("Posteriore"), CUSTOM("Personalizzata")
}

data class PhotoEntry(
    val id: Long = 0L,
    val dateEpochDay: Long,
    val pose: PhotoPose,
    val customLabel: String = "",
    val filePath: String,
    val notes: String = "",
    val createdAt: Long = 0L,
    /** Text the AI coach returned for this photo. Empty until an analysis is run. */
    val analysis: String = "",
    val analysisAt: Long? = null
) {
    val hasAnalysis: Boolean get() = analysis.isNotBlank()
}
