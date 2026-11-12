package com.fitcoach.app.domain.model

enum class Sex { MALE, FEMALE, UNSPECIFIED }

enum class Goal(val label: String, val description: String) {
    RECOMP("Ricomposizione corporea", "Perdere grasso e costruire muscolo contemporaneamente"),
    HYPERTROPHY("Ipertrofia", "Massimizzare la crescita muscolare"),
    CUT("Definizione", "Ridurre il grasso mantenendo la massa"),
    MAINTENANCE("Mantenimento", "Conservare composizione e prestazioni"),
    STRENGTH("Forza", "Aumentare la forza massimale sui grandi sollevamenti")
}

/**
 * Tipo di lavoro. Il fattore NEAT (attivita non sportiva) si somma al fattore
 * di attivita base per stimare il dispendio energetico reale.
 */
enum class WorkType(val label: String, val neatAdjustment: Double) {
    SEDENTARY("Sedentario (ufficio, scrivania)", -0.05),
    STANDING("In piedi (commesso, cameriere)", 0.05),
    MANUAL("Manuale (magazziniere, artigiano)", 0.15),
    HEAVY("Fisicamente pesante (edile, agricoltura)", 0.28),
    MIXED("Misto (alterno scrivania e movimento)", 0.05)
}

enum class WorkShift(val label: String) {
    NONE("Nessun turno"),
    MORNING("Turno mattina"),
    AFTERNOON("Turno pomeriggio"),
    NIGHT("Turno notte"),
    ROTATING("Turni a rotazione")
}

enum class TransportMode(val label: String) {
    CAR("Auto/moto"),
    PUBLIC("Mezzi pubblici"),
    BIKE("Bicicletta"),
    WALK("A piedi"),
    NONE("Non mi sposto / lavoro da casa")
}

/** Preferenza obbligatoria per la colazione. Influenza la generazione dei pasti. */
enum class BreakfastPreference(val label: String) {
    SWEET("Dolce"),
    SAVORY("Salata"),
    BOTH("Entrambe, alternandole"),
    NONE("Non faccio colazione")
}

enum class Level3(val label: String) {
    LOW("Basso"), MEDIUM("Medio"), HIGH("Alto")
}

enum class CookingTimeAvailable(val label: String) {
    LOW("Poco tempo (< 15 min)"),
    MEDIUM("Tempo medio (15-40 min)"),
    HIGH("Molto tempo (> 40 min)")
}

enum class TechnicalLevel(val label: String) {
    NOVICE("Base (tecnica da consolidare)"),
    COMPETENT("Intermedio (tecnica discreta)"),
    PROFICIENT("Avanzato (tecnica solida)")
}

/** Tecniche d'intensita gestite dal motore. */
enum class IntensityTechnique(val label: String) {
    NONE("Nessuna"),
    DROP_SET("Drop set"),
    REST_PAUSE("Rest-pause"),
    MYO_REPS("Myo-reps"),
    SUPERSET("Superset"),
    BACK_OFF_SET("Back-off set"),
    CLUSTER_SET("Cluster set"),
    FAILURE("Serie a cedimento"),
    ECCENTRIC_EMPHASIS("Enfasi eccentrica")
}

/** Tipo di fonte scientifica, per distinguere l'autorevolezza. */
enum class SourceKind(val label: String) {
    OFFICIAL_GUIDELINE("Linea guida ufficiale"),
    SYSTEMATIC_REVIEW("Revisione sistematica / meta-analisi"),
    INDIVIDUAL_STUDY("Studio individuale"),
    DIVULGATIVE("Articolo tecnico divulgativo")
}

enum class TrainingExperience(val label: String) {
    BEGINNER("Principiante (0-1 anno)"),
    INTERMEDIATE("Intermedio (1-3 anni)"),
    ADVANCED("Avanzato (3+ anni)")
}

enum class TrainingStatus(val label: String) {
    NEVER_TRAINED("Non mi sono mai allenato"),
    FORMERLY_TRAINED("Mi sono allenato in passato ma ho smesso"),
    CURRENTLY_TRAINING("Mi alleno attualmente"),
    OCCASIONAL("Mi alleno saltuariamente")
}

enum class Equipment(val label: String) {
    BARBELL("Bilanciere e dischi"),
    DUMBBELL("Manubri"),
    MACHINE("Macchine"),
    CABLE("Cavi"),
    PULLUP_BAR("Sbarra"),
    DIP_STATION("Parallele"),
    BENCH("Panca"),
    RESISTANCE_BAND("Elastici"),
    KETTLEBELL("Kettlebell"),
    BODYWEIGHT("Solo corpo libero")
}

enum class DietaryPreference(val label: String) {
    NONE("Nessuna restrizione"),
    MEDITERRANEAN("Mediterranea"),
    VEGETARIAN("Vegetariana"),
    VEGAN("Vegana"),
    PESCATARIAN("Pescatariana"),
    LOW_LACTOSE("A basso contenuto di lattosio"),
    GLUTEN_FREE("Senza glutine"),
    HALAL("Halal")
}

enum class CarbDayType(val label: String) {
    HIGH("HIGH CARB"),
    MEDIUM("MEDIUM CARB"),
    LOW("LOW CARB")
}

enum class MuscleGroup(val label: String, val isLargeGroup: Boolean) {
    CHEST("Petto", true),
    BACK("Schiena", true),
    SHOULDERS("Spalle", false),
    BICEPS("Bicipiti", false),
    TRICEPS("Tricipiti", false),
    QUADS("Quadricipiti", true),
    HAMSTRINGS("Femorali", true),
    GLUTES("Glutei", true),
    CALVES("Polpacci", false),
    CORE("Core", false)
}

enum class SplitType(val label: String) {
    UPPER_LOWER("Upper / Lower"),
    PUSH_PULL_LEGS("Push / Pull / Legs"),
    FULL_BODY("Full Body"),
    CUSTOM("Split personalizzata")
}

enum class ExerciseCategory { COMPOUND, ISOLATION, BODYWEIGHT }

/** Pattern di movimento biomeccanico dell'esercizio. */
enum class MovementPattern(val label: String) {
    HORIZONTAL_PUSH("Spinta orizzontale"),
    VERTICAL_PUSH("Spinta verticale"),
    HORIZONTAL_PULL("Trazione orizzontale"),
    VERTICAL_PULL("Trazione verticale"),
    SQUAT("Accosciata"),
    HINGE("Flessione anca (hinge)"),
    LUNGE("Affondo"),
    KNEE_FLEXION("Flessione ginocchio"),
    HIP_EXTENSION("Estensione anca"),
    ELBOW_FLEXION("Flessione gomito"),
    ELBOW_EXTENSION("Estensione gomito"),
    SHOULDER_ABDUCTION("Abduzione spalla"),
    CALF_RAISE("Estensione caviglia"),
    CORE_FLEXION("Flessione/core"),
    OTHER("Altro")
}

enum class SessionStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED }

enum class ChangeStatus { PROPOSED, APPROVED, APPLIED, REJECTED, REVERTED }

enum class ChangeDomain { NUTRITION, WORKOUT, BOTH }

enum class MealSlot(val label: String, val defaultTime: String) {
    BREAKFAST("Colazione", "07:30"),
    MORNING_SNACK("Spuntino mattutino", "10:30"),
    LUNCH("Pranzo", "13:00"),
    AFTERNOON_SNACK("Spuntino pomeridiano", "16:30"),
    DINNER("Cena", "20:00"),
    EVENING_SNACK("Spuntino serale", "22:30")
}

enum class FoodUnit { RAW, COOKED, AS_SOLD }

/** Ambito del budget alimentare: solo la persona o l'intera spesa familiare. */
enum class BudgetScope(val label: String) {
    PERSONAL_ONLY("Solo la mia alimentazione"),
    SHARED_FOOD("Spesa alimentare condivisa"),
    WHOLE_SHOPPING("Tutta la spesa (alimentare e non)")
}


enum class ChatRole { USER, ASSISTANT, SYSTEM }

enum class AiProvider(val label: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1/", "gpt-5.6"),
    GROQ("Groq", "https://api.groq.com/openai/v1/", "llama-3.3-70b-versatile"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/", "meta-llama/llama-3.3-70b-instruct:free"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
    CUSTOM("Custom (OpenAI-compatible)", "", "")
}
