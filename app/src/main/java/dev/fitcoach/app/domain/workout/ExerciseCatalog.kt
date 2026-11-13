package dev.fitcoach.app.domain.workout

/**
 * Exercises available offline.
 *
 * This is a starting catalogue, not a scientific database: each entry carries the
 * muscle group, the equipment and the joints it loads, which is what the generator
 * needs to respect the user's limitations. Entries imported from external sources
 * are added on top of it, with their own provenance.
 */
object DefaultExerciseCatalog {

    val exercises: List<Exercise> = listOf(
        Exercise("bench-press", "Panca piana bilanciere", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS), Equipment.BARBELL, compound = true, technicalDemand = 2, contraindications = listOf("spalla")),
        Exercise("db-bench-press", "Panca piana manubri", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS), Equipment.DUMBBELL, compound = true, technicalDemand = 2, contraindications = listOf("spalla")),
        Exercise("push-up", "Piegamenti sulle braccia", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS), Equipment.BODYWEIGHT, compound = true),
        Exercise("incline-db-press", "Panca inclinata manubri", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS), Equipment.DUMBBELL, compound = true, technicalDemand = 2, contraindications = listOf("spalla")),
        Exercise("cable-fly", "Croci ai cavi", MuscleGroup.CHEST, emptyList(), Equipment.CABLE, compound = false),

        Exercise("pull-up", "Trazioni alla sbarra", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), Equipment.BODYWEIGHT, compound = true, technicalDemand = 3, contraindications = listOf("spalla", "gomito")),
        Exercise("lat-pulldown", "Lat machine", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), Equipment.MACHINE, compound = true, technicalDemand = 2),
        Exercise("barbell-row", "Rematore bilanciere", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), Equipment.BARBELL, compound = true, technicalDemand = 3, contraindications = listOf("lombare")),
        Exercise("seated-row", "Pulley basso", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), Equipment.CABLE, compound = true, technicalDemand = 2),

        Exercise("overhead-press", "Military press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS), Equipment.BARBELL, compound = true, technicalDemand = 3, contraindications = listOf("spalla")),
        Exercise("lateral-raise", "Alzate laterali", MuscleGroup.SHOULDERS, emptyList(), Equipment.DUMBBELL, compound = false),
        Exercise("face-pull", "Face pull", MuscleGroup.SHOULDERS, listOf(MuscleGroup.BACK), Equipment.CABLE, compound = false),

        Exercise("barbell-curl", "Curl bilanciere", MuscleGroup.BICEPS, emptyList(), Equipment.BARBELL, compound = false, contraindications = listOf("gomito")),
        Exercise("db-curl", "Curl manubri", MuscleGroup.BICEPS, emptyList(), Equipment.DUMBBELL, compound = false),
        Exercise("cable-curl", "Curl ai cavi", MuscleGroup.BICEPS, emptyList(), Equipment.CABLE, compound = false),

        Exercise("close-grip-bench", "Panca presa stretta", MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST), Equipment.BARBELL, compound = true, technicalDemand = 2, contraindications = listOf("spalla", "gomito")),
        Exercise("triceps-pushdown", "Pushdown tricipiti", MuscleGroup.TRICEPS, emptyList(), Equipment.CABLE, compound = false),
        Exercise("overhead-db-extension", "Estensioni sopra la testa", MuscleGroup.TRICEPS, emptyList(), Equipment.DUMBBELL, compound = false, contraindications = listOf("gomito", "spalla")),

        Exercise("back-squat", "Squat bilanciere", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.CORE), Equipment.BARBELL, compound = true, technicalDemand = 3, contraindications = listOf("ginocchio", "lombare")),
        Exercise("leg-press", "Leg press", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), Equipment.MACHINE, compound = true, technicalDemand = 2),
        Exercise("split-squat", "Affondi bulgari", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), Equipment.DUMBBELL, compound = true, technicalDemand = 3, contraindications = listOf("ginocchio")),
        Exercise("leg-extension", "Leg extension", MuscleGroup.QUADS, emptyList(), Equipment.MACHINE, compound = false),

        Exercise("romanian-deadlift", "Stacco rumeno", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.BACK), Equipment.BARBELL, compound = true, technicalDemand = 3, contraindications = listOf("lombare")),
        Exercise("leg-curl", "Leg curl", MuscleGroup.HAMSTRINGS, emptyList(), Equipment.MACHINE, compound = false),

        Exercise("hip-thrust", "Hip thrust", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), Equipment.BARBELL, compound = true, technicalDemand = 2),
        Exercise("glute-bridge", "Ponte glutei", MuscleGroup.GLUTES, emptyList(), Equipment.BODYWEIGHT, compound = false),

        Exercise("calf-raise", "Calf raise in piedi", MuscleGroup.CALVES, emptyList(), Equipment.MACHINE, compound = false),

        Exercise("plank", "Plank", MuscleGroup.CORE, emptyList(), Equipment.BODYWEIGHT, compound = false),
        Exercise("hanging-leg-raise", "Sollevamento gambe alla sbarra", MuscleGroup.CORE, emptyList(), Equipment.BODYWEIGHT, compound = false, technicalDemand = 2, contraindications = listOf("spalla")),
        Exercise("cable-crunch", "Crunch ai cavi", MuscleGroup.CORE, emptyList(), Equipment.CABLE, compound = false)
    )

    fun byId(id: String): Exercise? = exercises.firstOrNull { it.id == id }
}
