package com.fitcoach.app.domain.model

enum class Sex { MALE, FEMALE, UNSPECIFIED }

enum class Goal(val label: String, val description: String) {
    RECOMP("Ricomposizione corporea", "Perdere grasso e costruire muscolo contemporaneamente"),
    HYPERTROPHY("Ipertrofia", "Massimizzare la crescita muscolare"),
    CUT("Definizione", "Ridurre il grasso mantenendo la massa"),
    MAINTENANCE("Mantenimento", "Conservare composizione e prestazioni")
}

enum class TrainingExperience(val label: String) {
    BEGINNER("Principiante (0-1 anno)"),
    INTERMEDIATE("Intermedio (1-3 anni)"),
    ADVANCED("Avanzato (3+ anni)")
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

enum class SessionStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED }

enum class ChangeStatus { PROPOSED, APPROVED, APPLIED, REJECTED, REVERTED }

enum class ChangeDomain { NUTRITION, WORKOUT, BOTH }

enum class MealSlot(val label: String, val defaultTime: String) {
    BREAKFAST("Colazione", "07:30"),
    MORNING_SNACK("Spuntino mattutino", "10:30"),
    LUNCH("Pranzo", "13:00"),
    AFTERNOON_SNACK("Spuntino pomeridiano", "16:30"),
    DINNER("Cena", "20:00")
}

enum class FoodUnit { RAW, COOKED, AS_SOLD }

enum class ChatRole { USER, ASSISTANT, SYSTEM }

enum class AiProvider(val label: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1/", "gpt-4o-mini"),
    GROQ("Groq", "https://api.groq.com/openai/v1/", "llama-3.3-70b-versatile"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/", "meta-llama/llama-3.3-70b-instruct:free"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
    CUSTOM("Custom (OpenAI-compatible)", "", "")
}
