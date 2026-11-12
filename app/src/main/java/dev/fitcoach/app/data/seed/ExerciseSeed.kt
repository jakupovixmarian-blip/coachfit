package dev.fitcoach.app.data.seed

import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup

/**
 * Curated exercise catalog. Names, muscles and movement patterns are taxonomy
 * data (not measured values), so they are safe to ship.
 */
object ExerciseSeed {

    private fun ex(
        name: String,
        primary: MuscleGroup,
        secondary: List<MuscleGroup>,
        equipment: Equipment,
        pattern: MovementPattern,
        setup: String,
        execution: String,
        mistakes: String
    ) = Exercise(
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
        pattern = pattern,
        setup = setup,
        execution = execution,
        commonMistakes = mistakes,
        isCustom = false
    )

    fun all(): List<Exercise> = listOf(
        // ---- PETTO ----
        ex(
            "Panca piana con bilanciere", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            Equipment.BARBELL, MovementPattern.HORIZONTAL_PUSH,
            "Sdraiati sulla panca, piedi stabili a terra, scapole addotte e depresse, presa poco piu ampia delle spalle.",
            "Scendi controllando fino al petto, poi spingi il bilanciere verso l'alto senza perdere l'assetto delle scapole.",
            "Rimbalzare sul petto, perdere l'adduzione delle scapole, gomiti troppo aperti a 90 gradi."
        ),
        ex(
            "Panca inclinata con manubri", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            Equipment.DUMBBELL, MovementPattern.HORIZONTAL_PUSH,
            "Panca inclinata 30 gradi, manubri all'altezza del petto, scapole stabili.",
            "Spingi i manubri verso l'alto convergendo leggermente, controlla la discesa.",
            "Inclinazione eccessiva che sposta il carico sulle spalle, escursione parziale."
        ),
        ex(
            "Croci ai cavi", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS),
            Equipment.CABLE, MovementPattern.ISOLATION,
            "Cavi alti o medi, leggera inclinazione del busto, gomiti leggermente flessi.",
            "Porta le mani verso il centro contraendo il petto, torna lentamente.",
            "Piegare troppo i gomiti trasformandola in una spinta, usare slancio."
        ),
        ex(
            "Piegamenti a terra", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            Equipment.BODYWEIGHT, MovementPattern.HORIZONTAL_PUSH,
            "Mani poco piu larghe delle spalle, corpo in linea, core attivo.",
            "Scendi fino a sfiorare il suolo con il petto, spingi mantenendo il corpo rigido.",
            "Bacino che cade, collo in avanti, escursione ridotta."
        ),

        // ---- SCHIENA ----
        ex(
            "Trazioni alla sbarra", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            Equipment.BODYWEIGHT, MovementPattern.VERTICAL_PULL,
            "Presa prona poco piu ampia delle spalle, scapole attive, corpo stabile.",
            "Tira portando il petto verso la sbarra, scendi controllando fino a distendere le braccia.",
            "Usare slancio, non completare l'escursione, spalle che salgono verso le orecchie."
        ),
        ex(
            "Lat machine presa prona", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS),
            Equipment.MACHINE, MovementPattern.VERTICAL_PULL,
            "Seduto con cosce bloccate, presa prona ampia, busto leggermente indietro.",
            "Tira la sbarra verso l'alto del petto guidando con i gomiti, torna controllando.",
            "Tirare con le braccia invece che con la schiena, dondolare il busto."
        ),
        ex(
            "Rematore con bilanciere", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS, MuscleGroup.CORE),
            Equipment.BARBELL, MovementPattern.HORIZONTAL_PULL,
            "Busto inclinato circa 45 gradi, schiena neutra, presa prona.",
            "Tira il bilanciere verso l'ombelico, gomiti vicini al corpo, scendi controllando.",
            "Schiena curva, slancio del busto, tirare verso il petto alto."
        ),
        ex(
            "Pulley basso", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS),
            Equipment.CABLE, MovementPattern.HORIZONTAL_PULL,
            "Seduto con piedi appoggiati, schiena neutra, presa neutra o prona.",
            "Tira verso l'addome portando indietro le spalle, torna in allungamento controllato.",
            "Curvare la schiena in avanti, tirare con i gomiti larghi."
        ),
        ex(
            "Stacco da terra", MuscleGroup.BACK, listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
            Equipment.BARBELL, MovementPattern.HINGE,
            "Bilanciere sopra il centro del piede, tibie vicine, schiena neutra, core attivo.",
            "Spingi con le gambe estendendo anche le anche, mantieni il bilanciere vicino al corpo.",
            "Schiena curva, bilanciere lontano, iperestensione lombare in chiusura."
        ),

        // ---- SPALLE ----
        ex(
            "Military press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            Equipment.BARBELL, MovementPattern.VERTICAL_PUSH,
            "In piedi, bilanciere all'altezza delle clavicole, core attivo, glutei contratti.",
            "Spingi sopra la testa fino a estendere le braccia, scendi controllando.",
            "Inarcare la lombare, spingere in avanti, escursione parziale."
        ),
        ex(
            "Alzate laterali", MuscleGroup.SHOULDERS, emptyList(),
            Equipment.DUMBBELL, MovementPattern.ISOLATION,
            "In piedi o seduto, manubri lungo i fianchi, gomiti leggermente flessi.",
            "Alza le braccia lateralmente fino all'altezza delle spalle, scendi lentamente.",
            "Usare slancio, alzare sopra le spalle, scrollare le spalle."
        ),
        ex(
            "Alzate posteriori", MuscleGroup.SHOULDERS, listOf(MuscleGroup.BACK),
            Equipment.DUMBBELL, MovementPattern.ISOLATION,
            "Busto inclinato in avanti, manubri sotto il petto, gomiti leggermente flessi.",
            "Apri le braccia lateralmente contraendo il deltoide posteriore, torna controllando.",
            "Estendere la schiena per aiutarsi, usare carico eccessivo."
        ),

        // ---- BRACCIA ----
        ex(
            "Curl con bilanciere", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            Equipment.BARBELL, MovementPattern.ISOLATION,
            "In piedi, gomiti lungo i fianchi, presa supina.",
            "Fletti i gomiti portando il bilanciere verso le spalle, scendi controllando.",
            "Dondolare il busto, gomiti che si spostano in avanti, escursione parziale."
        ),
        ex(
            "Curl alternato con manubri", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            Equipment.DUMBBELL, MovementPattern.ISOLATION,
            "In piedi o seduto, gomiti stabili, presa supina con supinazione.",
            "Fletti un braccio alla volta contraendo il bicipite, scendi controllando.",
            "Slancio del busto, spalle che si alzano."
        ),
        ex(
            "French press", MuscleGroup.TRICEPS, emptyList(),
            Equipment.BARBELL, MovementPattern.ISOLATION,
            "Sdraiati o seduto, bilanciere sopra il petto, gomiti vicini alla testa.",
            "Fletti i gomiti portando il bilanciere verso la fronte, estendi senza muovere le spalle.",
            "Gomiti che si aprono, movimento delle spalle, carico eccessivo."
        ),
        ex(
            "Push down ai cavi", MuscleGroup.TRICEPS, emptyList(),
            Equipment.CABLE, MovementPattern.ISOLATION,
            "In piedi davanti al cavo alto, gomiti bloccati ai fianchi.",
            "Estendi i gomiti spingendo verso il basso, torna controllando.",
            "Spostare i gomiti, usare il busto per spingere."
        ),

        // ---- GAMBE ----
        ex(
            "Squat con bilanciere", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            Equipment.BARBELL, MovementPattern.SQUAT,
            "Bilanciere sul trapezio, piedi larghezza spalle, punte leggermente esterne.",
            "Scendi controllando con ginocchia in linea con le punte, risali spingendo a terra.",
            "Ginocchia che collassano verso l'interno, talloni che si alzano, schiena che si flette."
        ),
        ex(
            "Leg press", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            Equipment.MACHINE, MovementPattern.SQUAT,
            "Seduto con schiena e bacino aderenti, piedi al centro pedana.",
            "Scendi controllando senza staccare il bacino, spingi senza bloccare le ginocchia.",
            "Escursione eccessiva che stacca il bacino, ginocchia bloccate in chiusura."
        ),
        ex(
            "Affondi con manubri", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            Equipment.DUMBBELL, MovementPattern.LUNGE,
            "In piedi, manubri lungo i fianchi, sguardo avanti.",
            "Fai un passo avanti scendendo con il ginocchio posteriore, risali spingendo col piede avanti.",
            "Passo troppo corto, ginocchio che supera eccessivamente la punta, busto che cade."
        ),
        ex(
            "Stacco rumeno", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
            Equipment.BARBELL, MovementPattern.HINGE,
            "In piedi, bilanciere lungo le cosce, ginocchia leggermente flesse.",
            "Spingi le anche indietro scendendo con la schiena neutra, risali contraendo i glutei.",
            "Flettere le ginocchia come in uno squat, schiena che si curva."
        ),
        ex(
            "Leg curl", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.CALVES),
            Equipment.MACHINE, MovementPattern.ISOLATION,
            "Sdraiato sulla macchina con ginocchia allineate al perno.",
            "Fletti le gambe portando il cuscinetto verso i glutei, torna controllando.",
            "Sollevare il bacino, usare slancio."
        ),
        ex(
            "Hip thrust", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS),
            Equipment.BARBELL, MovementPattern.HINGE,
            "Schiena alta appoggiata alla panca, bilanciere sul bacino protetto.",
            "Spingi le anche verso l'alto contraendo i glutei, scendi controllando.",
            "Iperestensione lombare, spinta con le punte dei piedi."
        ),
        ex(
            "Calf raise in piedi", MuscleGroup.CALVES, emptyList(),
            Equipment.MACHINE, MovementPattern.ISOLATION,
            "Avampiede sulla pedana, talloni liberi di scendere.",
            "Sali il piu possibile sulle punte, scendi in allungamento controllato.",
            "Escursione parziale, rimbalzo sul fondo."
        ),

        ex(
            "Squat a corpo libero", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            Equipment.BODYWEIGHT, MovementPattern.SQUAT,
            "Piedi larghezza spalle, punte leggermente esterne, braccia avanti per equilibrio.",
            "Scendi controllando con le ginocchia in linea con le punte, risali spingendo a terra.",
            "Talloni che si alzano, ginocchia che collassano verso l'interno, schiena che si flette."
        ),
        ex(
            "Affondi a corpo libero", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            Equipment.BODYWEIGHT, MovementPattern.LUNGE,
            "In piedi, mani ai fianchi o davanti, sguardo avanti.",
            "Fai un passo avanti e scendi col ginocchio posteriore, risali spingendo col piede avanti.",
            "Passo troppo corto, ginocchio che supera eccessivamente la punta, busto che cade."
        ),
        ex(
            "Ponte glutei", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS),
            Equipment.BODYWEIGHT, MovementPattern.HINGE,
            "Sdraiato supino, ginocchia piegate, piedi a terra larghezza bacino.",
            "Spingi le anche verso l'alto contraendo i glutei, scendi controllando.",
            "Iperestensione lombare, spinta con le punte dei piedi."
        ),
        ex(
            "Calf raise a corpo libero", MuscleGroup.CALVES, emptyList(),
            Equipment.BODYWEIGHT, MovementPattern.ISOLATION,
            "In piedi su un gradino, avampiede appoggiato, talloni liberi di scendere.",
            "Sali il piu possibile sulle punte, scendi in allungamento controllato.",
            "Escursione parziale, rimbalzo sul fondo."
        ),
        ex(
            "Rematore con manubrio", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            Equipment.DUMBBELL, MovementPattern.HORIZONTAL_PULL,
            "Un ginocchio e una mano sulla panca, schiena neutra, manubrio a braccio disteso.",
            "Tira il manubrio verso il fianco guidando con il gomito, scendi controllando.",
            "Ruotare il busto, tirare con il bicipite, escursione parziale."
        ),
        ex(
            "Stacco rumeno con manubri", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
            Equipment.DUMBBELL, MovementPattern.HINGE,
            "In piedi, manubri davanti alle cosce, ginocchia leggermente flesse.",
            "Spingi le anche indietro scendendo con la schiena neutra, risali contraendo i glutei.",
            "Flettere le ginocchia come in uno squat, schiena che si curva."
        ),
        ex(
            "Estensioni per tricipiti con manubrio", MuscleGroup.TRICEPS, emptyList(),
            Equipment.DUMBBELL, MovementPattern.ISOLATION,
            "Seduto o in piedi, manubrio sopra la testa impugnato a due mani.",
            "Fletti i gomiti portando il manubrio dietro la testa, estendi senza muovere le spalle.",
            "Gomiti che si aprono, movimento delle spalle, carico eccessivo."
        ),
        ex(
            "Curl con elastici", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            Equipment.BAND, MovementPattern.ISOLATION,
            "In piedi sull'elastico, gomiti lungo i fianchi, presa supina.",
            "Fletti i gomiti portando le mani verso le spalle, torna controllando.",
            "Dondolare il busto, gomiti che si spostano in avanti."
        ),

        // ---- CORE ----
        ex(
            "Plank", MuscleGroup.CORE, emptyList(),
            Equipment.BODYWEIGHT, MovementPattern.CORE,
            "Appoggio sugli avambracci, corpo in linea, glutei contratti.",
            "Mantieni la posizione senza far cadere il bacino, respira regolarmente.",
            "Bacino che cade, testa in avanti, trattenere il respiro."
        ),
        ex(
            "Crunch inverso", MuscleGroup.CORE, emptyList(),
            Equipment.BODYWEIGHT, MovementPattern.CORE,
            "Sdraiato, ginocchia piegate, mani a terra lungo i fianchi.",
            "Porta le ginocchia verso il petto sollevando il bacino, scendi controllando.",
            "Usare slancio, tirare con le braccia."
        ),

        // ---- CARDIO ----
        ex(
            "Tapis roulant", MuscleGroup.CARDIO, listOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            Equipment.CARDIO_MACHINE, MovementPattern.CARDIO,
            "Imposta velocita e inclinazione, postura eretta.",
            "Mantieni un passo regolare e controlla la respirazione.",
            "Appoggiarsi alle maniglie, inclinazione eccessiva senza controllo."
        ),
        ex(
            "Cyclette", MuscleGroup.CARDIO, listOf(MuscleGroup.QUADS),
            Equipment.CARDIO_MACHINE, MovementPattern.CARDIO,
            "Regola il sellino all'altezza dell'anca, schiena neutra.",
            "Pedala con ritmo costante, regola la resistenza in base all'obiettivo.",
            "Sellino troppo basso, ginocchia che si aprono."
        ),
        ex(
            "Burpee", MuscleGroup.FULL_BODY, listOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE),
            Equipment.BODYWEIGHT, MovementPattern.CARDIO,
            "In piedi, pronto a scendere in appoggio.",
            "Scendi in plank, eventuale piegamento, riporta i piedi e salta in alto.",
            "Perdere l'allineamento lombare, atterrare con le ginocchia rigide."
        )
    )
}
