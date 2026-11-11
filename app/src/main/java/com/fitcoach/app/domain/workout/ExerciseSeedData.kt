package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MuscleGroup

/**
 * Database esercizi per bodybuilding / ipertrofia.
 *
 * Le descrizioni tecniche sono redatte secondo le linee guida di
 * strength & conditioning. I range di ripetizioni e i recuperi riflettono
 * le raccomandazioni per ipertrofia (Schoenfeld et al., ACSM).
 */
object ExerciseSeedData {

    val exercises: List<Exercise> = listOf(
        // ---------------- PETTO ----------------
        e(
            "Panca piana con bilanciere", MuscleGroup.CHEST,
            setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL, Equipment.BENCH),
            "Sdraiati sulla panca, scapole addotte e depresse, piedi stabili a terra. " +
                "Discesa controllata al petto, gomiti a circa 45-60 gradi. Spinta senza rimbalzo.",
            4, 6, 10, 150, 2,
            subs = listOf("Panca piana con manubri", "Chest press")
        ),
        e(
            "Panca piana con manubri", MuscleGroup.CHEST,
            setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            ExerciseCategory.COMPOUND, setOf(Equipment.DUMBBELL, Equipment.BENCH),
            "Manubri allineati, discesa fino ad ampio allungamento, gomiti sotto i polsi. " +
                "Range completo, controllo in fase eccentrica.",
            4, 8, 12, 120, 2,
            subs = listOf("Panca piana con bilanciere", "Chest press")
        ),
        e(
            "Panca inclinata con manubri", MuscleGroup.CHEST,
            setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            ExerciseCategory.COMPOUND, setOf(Equipment.DUMBBELL, Equipment.BENCH),
            "Inclinazione panca 30-40 gradi. Focus sul fascio clavicolare. " +
                "Evita di inarcare eccessivamente la lombare.",
            3, 8, 12, 120, 2,
            subs = listOf("Panca inclinata con bilanciere")
        ),
        e(
            "Croci ai cavi", MuscleGroup.CHEST, setOf(MuscleGroup.SHOULDERS),
            ExerciseCategory.ISOLATION, setOf(Equipment.CABLE),
            "Movimento ad arco, gomiti leggermente flessi e fissi. " +
                "Contrazione di picco al centro, allungamento controllato in apertura.",
            3, 12, 15, 75, 1,
            subs = listOf("Pectoral machine", "Croci con manubri")
        ),
        e(
            "Dip alle parallele", MuscleGroup.CHEST,
            setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            ExerciseCategory.BODYWEIGHT, setOf(Equipment.DIP_STATION, Equipment.BODYWEIGHT),
            "Torso leggermente inclinato in avanti per enfasi pettorale. " +
                "Discesa fino a circa 90 gradi al gomito, senza dolore alla spalla.",
            3, 6, 12, 120, 2,
            subs = listOf("Panca piana con bilanciere")
        ),

        // ---------------- SCHIENA ----------------
        e(
            "Trazioni alla sbarra", MuscleGroup.BACK,
            setOf(MuscleGroup.BICEPS, MuscleGroup.CORE),
            ExerciseCategory.BODYWEIGHT, setOf(Equipment.PULLUP_BAR, Equipment.BODYWEIGHT),
            "Presa prona poco piu ampia delle spalle. Tira i gomiti verso il basso, " +
                "petto verso la sbarra. Evita slanci; se necessario usa assistenza elastica.",
            4, 5, 12, 150, 2,
            subs = listOf("Lat machine", "Pulley alto")
        ),
        e(
            "Lat machine", MuscleGroup.BACK, setOf(MuscleGroup.BICEPS),
            ExerciseCategory.COMPOUND, setOf(Equipment.MACHINE, Equipment.CABLE),
            "Presa prona, trazione del bilanciere verso il petto alto, " +
                "scapole depresse. Ritorno controllato con allungamento del gran dorsale.",
            4, 8, 12, 120, 2,
            subs = listOf("Trazioni alla sbarra", "Pulley alto")
        ),
        e(
            "Rematore con bilanciere", MuscleGroup.BACK,
            setOf(MuscleGroup.BICEPS, MuscleGroup.CORE),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL),
            "Busto inclinato circa 45 gradi, schiena neutra, core attivo. " +
                "Tira verso l'ombelico, gomiti vicini al corpo.",
            4, 6, 10, 150, 2,
            subs = listOf("Rematore con manubrio", "Rematore alla macchina")
        ),
        e(
            "Rematore con manubrio", MuscleGroup.BACK, setOf(MuscleGroup.BICEPS),
            ExerciseCategory.COMPOUND, setOf(Equipment.DUMBBELL, Equipment.BENCH),
            "Un ginocchio e una mano sulla panca, schiena parallela al suolo. " +
                "Trazione del manubrio verso il fianco, senza rotazione del busto.",
            3, 8, 12, 105, 2, unilateral = true,
            subs = listOf("Rematore con bilanciere", "Rematore alla macchina")
        ),
        e(
            "Pulley basso", MuscleGroup.BACK, setOf(MuscleGroup.BICEPS),
            ExerciseCategory.COMPOUND, setOf(Equipment.CABLE),
            "Seduto, ginocchia leggermente flesse, busto stabile. " +
                "Tira la maniglia all'addome, scapole addotte, ritorno controllato.",
            3, 8, 12, 105, 2,
            subs = listOf("Rematore alla macchina")
        ),
        e(
            "Pulldown a braccia tese", MuscleGroup.BACK, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.CABLE),
            "Braccia quasi tese, spinta verso il basso a partire dalle spalle. " +
                "Focus sull'allungamento del gran dorsale, senza coinvolgere i gomiti.",
            3, 12, 15, 75, 1,
            subs = listOf("Lat machine")
        ),

        // ---------------- SPALLE ----------------
        e(
            "Military press", MuscleGroup.SHOULDERS, setOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL),
            "In piedi o seduto, core attivo. Spinta sopra la testa, " +
                "bilanciere in linea con le orecchie. Evita di inarcare la lombare.",
            4, 5, 10, 150, 2,
            subs = listOf("Shoulder press con manubri", "Shoulder press alla macchina")
        ),
        e(
            "Shoulder press con manubri", MuscleGroup.SHOULDERS, setOf(MuscleGroup.TRICEPS),
            ExerciseCategory.COMPOUND, setOf(Equipment.DUMBBELL, Equipment.BENCH),
            "Seduto con schienale, manubri all'altezza delle orecchie. " +
                "Spinta verso l'alto senza far ruotare i gomiti troppo in fuori.",
            3, 8, 12, 120, 2,
            subs = listOf("Military press", "Shoulder press alla macchina")
        ),
        e(
            "Alzate laterali", MuscleGroup.SHOULDERS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.DUMBBELL, Equipment.CABLE),
            "Manubri ai fianchi, alzata laterale fino all'altezza delle spalle. " +
                "Gomiti guida, senza slancio del busto. Focus sul fascio medio.",
            4, 12, 20, 60, 1,
            subs = listOf("Alzate laterali ai cavi")
        ),
        e(
            "Alzate posteriori (reverse fly)", MuscleGroup.SHOULDERS, setOf(MuscleGroup.BACK),
            ExerciseCategory.ISOLATION, setOf(Equipment.DUMBBELL, Equipment.MACHINE),
            "Busto inclinato, braccia quasi tese. Apri verso l'esterno " +
                "con focus sul deltoide posteriore, senza coinvolgere il trapezio.",
            3, 12, 20, 60, 1,
            subs = listOf("Pec-deck inversa")
        ),

        // ---------------- BICIPITI ----------------
        e(
            "Curl con bilanciere", MuscleGroup.BICEPS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.BARBELL),
            "Gomiti fissi ai fianchi, flessione senza slancio del busto. " +
                "Discesa controllata, estensione quasi completa.",
            3, 8, 12, 90, 1,
            subs = listOf("Curl con manubri", "Curl ai cavi")
        ),
        e(
            "Curl alternato con manubri", MuscleGroup.BICEPS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.DUMBBELL),
            "Un braccio alla volta, supinazione progressiva del polso. " +
                "Gomito fisso, contrazione di picco in alto.",
            3, 10, 15, 75, 1,
            subs = listOf("Curl con bilanciere")
        ),
        e(
            "Curl a martello", MuscleGroup.BICEPS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.DUMBBELL),
            "Presa neutra, gomiti fissi. Coinvolge brachiale e brachioradiale.",
            3, 10, 15, 75, 1,
            subs = listOf("Curl alternato con manubri")
        ),

        // ---------------- TRICIPITI ----------------
        e(
            "Panca stretta (close grip)", MuscleGroup.TRICEPS, setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL, Equipment.BENCH),
            "Presa alla larghezza delle spalle. Gomiti vicini al corpo in discesa. " +
                "Spinta concentrandosi sull'estensione del gomito.",
            3, 6, 10, 120, 2,
            subs = listOf("Dip alle parallele", "French press")
        ),
        e(
            "French press", MuscleGroup.TRICEPS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.DUMBBELL, Equipment.BENCH),
            "Bilanciere o manubri sopra la testa o da sdraiati. " +
                "Flessione del gomito con gomiti fissi, estensione controllata.",
            3, 8, 12, 90, 1,
            subs = listOf("Estensioni al cavo alto")
        ),
        e(
            "Estensioni al cavo alto", MuscleGroup.TRICEPS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.CABLE),
            "Gomiti fissi ai fianchi, estensione completa verso il basso. " +
                "Focus sulla contrazione del tricipite.",
            3, 12, 15, 60, 1,
            subs = listOf("French press")
        ),

        // ---------------- QUADRICIPITI ----------------
        e(
            "Squat con bilanciere", MuscleGroup.QUADS,
            setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL),
            "Bilanciere sul trapezio, piedi alla larghezza spalle. " +
                "Discesa fino a cosce parallele o poco sotto, ginocchia in linea con i piedi. " +
                "Schiena neutra, spinta dai talloni.",
            4, 6, 10, 180, 2,
            subs = listOf("Leg press", "Hack squat")
        ),
        e(
            "Leg press", MuscleGroup.QUADS, setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            ExerciseCategory.COMPOUND, setOf(Equipment.MACHINE),
            "Piedi alla larghezza spalle sulla pedana. " +
                "Discesa controllata senza staccare la lombare dallo schienale.",
            4, 10, 15, 120, 2,
            subs = listOf("Squat con bilanciere", "Hack squat")
        ),
        e(
            "Affondi con manubri", MuscleGroup.QUADS, setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            ExerciseCategory.COMPOUND, setOf(Equipment.DUMBBELL),
            "Passo avanti, ginocchio posteriore verso il suolo, busto eretto. " +
                "Spinta dal tallone anteriore. Alterna o completa un lato per volta.",
            3, 10, 12, 105, 2, unilateral = true,
            subs = listOf("Leg press")
        ),
        e(
            "Leg extension", MuscleGroup.QUADS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.MACHINE),
            "Seduto, schienale regolato, estensione del ginocchio. " +
                "Contrazione di picco in alto, discesa controllata.",
            3, 12, 15, 60, 1,
            subs = listOf()
        ),

        // ---------------- FEMORALI ----------------
        e(
            "Stacco da terra", MuscleGroup.HAMSTRINGS,
            setOf(MuscleGroup.GLUTES, MuscleGroup.BACK, MuscleGroup.CORE),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL),
            "Bilanciere sopra la meta del piede, schiena neutra, core attivo. " +
                "Spinta a terra con le gambe, il bilanciere scorre vicino alle gambe. " +
                "Non iperestendere la lombare in chiusura.",
            4, 4, 8, 180, 2,
            subs = listOf("Stacco rumeno", "Stacco sumo")
        ),
        e(
            "Stacco rumeno", MuscleGroup.HAMSTRINGS, setOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL),
            "Ginocchia leggermente flesse e fisse. Discesa del bilanciere " +
                "con anche indietro fino ad allungamento femorale, schiena neutra.",
            3, 8, 12, 150, 2,
            subs = listOf("Stacco da terra", "Leg curl")
        ),
        e(
            "Leg curl sdraiato", MuscleGroup.HAMSTRINGS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.MACHINE),
            "Sdraiato, cuscinetto sopra il tallone. Flessione del ginocchio " +
                "senza sollevare il bacino. Contrazione di picco.",
            3, 10, 15, 75, 1,
            subs = listOf("Leg curl seduto")
        ),
        e(
            "Leg curl seduto", MuscleGroup.HAMSTRINGS, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.MACHINE),
            "Seduto, schienale regolato. Flessione del ginocchio contro resistenza.",
            3, 10, 15, 75, 1,
            subs = listOf("Leg curl sdraiato")
        ),

        // ---------------- GLUTEI ----------------
        e(
            "Hip thrust", MuscleGroup.GLUTES, setOf(MuscleGroup.HAMSTRINGS),
            ExerciseCategory.COMPOUND, setOf(Equipment.BARBELL, Equipment.BENCH),
            "Schiena alta sulla panca, bilanciere sul bacino. " +
                "Estensione dell'anca fino alla contrazione dei glutei, senza inarcare la lombare.",
            3, 8, 12, 120, 2,
            subs = listOf("Ponte glutei a corpo libero")
        ),
        e(
            "Ponte glutei a corpo libero", MuscleGroup.GLUTES, setOf(MuscleGroup.HAMSTRINGS),
            ExerciseCategory.BODYWEIGHT, setOf(Equipment.BODYWEIGHT),
            "Sdraiato supino, piedi a terra. Solleva il bacino contraendo i glutei. " +
                "Pausa in alto, discesa controllata.",
            3, 15, 20, 60, 1,
            subs = listOf("Hip thrust")
        ),

        // ---------------- POLPACCI ----------------
        e(
            "Calf raise in piedi", MuscleGroup.CALVES, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.MACHINE, Equipment.DUMBBELL),
            "Estensione completa del piede su appoggio. " +
                "Range completo con pausa in alto, discesa controllata in allungamento.",
            4, 12, 20, 60, 1,
            subs = listOf("Calf raise seduto")
        ),
        e(
            "Calf raise seduto", MuscleGroup.CALVES, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.MACHINE),
            "Ginocchia flesse per enfasi sul soleo. Range completo.",
            3, 15, 20, 60, 1,
            subs = listOf("Calf raise in piedi")
        ),

        // ---------------- CORE ----------------
        e(
            "Plank", MuscleGroup.CORE, setOf(),
            ExerciseCategory.BODYWEIGHT, setOf(Equipment.BODYWEIGHT),
            "Corpo in linea dalla testa ai talloni, core attivo, glutei contratti. " +
                "Non trattenere il respiro; mantieni la posizione per il tempo previsto.",
            3, 30, 60, 60, 1, bodyweight = true,
            subs = listOf("Crunch inverso")
        ),
        e(
            "Crunch ai cavi", MuscleGroup.CORE, setOf(),
            ExerciseCategory.ISOLATION, setOf(Equipment.CABLE),
            "Inginocchiato davanti al cavo, flessione del busto con focus sull'addome. " +
                "Non tirare con le braccia.",
            3, 12, 20, 60, 1,
            subs = listOf("Crunch inverso")
        ),
        e(
            "Crunch inverso", MuscleGroup.CORE, setOf(),
            ExerciseCategory.BODYWEIGHT, setOf(Equipment.BODYWEIGHT),
            "Sdraiato, solleva il bacino verso il petto flettendo il busto. " +
                "Discesa controllata, senza slancio.",
            3, 12, 20, 60, 1, bodyweight = true,
            subs = listOf("Plank")
        )
    )

    private fun e(
        name: String,
        primary: MuscleGroup,
        secondary: Set<MuscleGroup>,
        category: ExerciseCategory,
        equipment: Set<Equipment>,
        technique: String,
        sets: Int,
        minReps: Int,
        maxReps: Int,
        rest: Int,
        rir: Int,
        unilateral: Boolean = false,
        bodyweight: Boolean = false,
        subs: List<String> = emptyList()
    ) = Exercise(
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        category = category,
        equipment = equipment,
        technique = technique,
        defaultSets = sets,
        minReps = minReps,
        maxReps = maxReps,
        restSeconds = rest,
        targetRir = rir,
        isUnilateral = unilateral,
        substituteNames = subs,
        bodyweightOnly = bodyweight
    )
}
