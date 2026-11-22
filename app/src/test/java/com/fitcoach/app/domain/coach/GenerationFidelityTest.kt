package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.GearCategory
import com.fitcoach.app.domain.model.GearInventory
import com.fitcoach.app.domain.model.GearItem
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.EquipmentFit
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.SimulatedAiPrograms
import kotlinx.serialization.json.Json
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Fedelta' del programma generato ai dati dichiarati dall'utente.
 *
 * Ogni test qui corrisponde a un difetto osservato nell'app: la scheda
 * ignorava l'attrezzatura, non conteneva tecniche d'intensita', non conteneva
 * isolamento, e la chat dichiarava modifiche mai registrate.
 */
class GenerationFidelityTest {

    /** L'orchestratore non viene interrogato: qui serve solo la guardia testuale. */
    private fun orchestrator(): CoachOrchestrator {
        val json = Json { ignoreUnknownKeys = true }
        return CoachOrchestrator(
            AiClient(ScriptedApi(mutableListOf()), json),
            CoachToolRegistry(emptySet()),
            json
        )
    }

    private fun baseProfile() = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 3
    )

    private fun exercise(
        name: String,
        category: ExerciseCategory,
        equipment: Set<Equipment>,
        muscle: MuscleGroup = MuscleGroup.CHEST
    ) = Exercise(
        name = name, primaryMuscle = muscle, category = category,
        equipment = equipment, technique = "", defaultSets = 3,
        minReps = 8, maxReps = 12, restSeconds = 120, targetRir = 2
    )

    /**
     * Proposta valida costruita dai motori reali, con la tecnica imposta su ogni
     * esercizio. Serve a verificare la mappatura che la UI legge davvero
     * (`intensityTechnique`), non il DTO che conserva la stringa comunque.
     */
    private fun validProposal(technique: String): AiProgramProposal {
        val profile = baseProfile()
        val base = SimulatedAiPrograms.proposal(
            profile, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        return base.copy(
            workout = base.workout.copy(
                days = base.workout.days.map { day ->
                    day.copy(
                        exercises = day.exercises.map {
                            it.copy(technique = technique, techniqueReason = "Scelta del coach per il gruppo.")
                        }
                    )
                }
            )
        )
    }

    // ---------------- Attrezzatura ----------------

    @Test
    fun `l'inventario dichiarato vale come attrezzatura disponibile`() {
        val profile = baseProfile().copy(
            availableEquipment = emptySet(),
            gearInventory = GearInventory(
                presets = listOf(
                    GearItem(id = "Manubri regolabili", name = "Manubri regolabili", category = GearCategory.WEIGHTS),
                    GearItem(id = "Panca regolabile", name = "Panca regolabile", category = GearCategory.BENCH)
                )
            )
        )

        val derived = EquipmentFit.equipmentFor(profile)

        assertThat(derived).contains(Equipment.DUMBBELL)
        assertThat(derived).contains(Equipment.BENCH)
        // Nessun cavo dichiarato: non deve comparire per generosita'.
        assertThat(derived).doesNotContain(Equipment.CABLE)
    }

    @Test
    fun `un esercizio che richiede un attrezzo assente non e compatibile`() {
        val profile = baseProfile().copy(
            availableEquipment = emptySet(),
            gearInventory = GearInventory(
                presets = listOf(GearItem(id = "Manubri regolabili", name = "Manubri regolabili", category = GearCategory.WEIGHTS))
            )
        )
        val crociAiCavi = exercise("Croci ai cavi", ExerciseCategory.ISOLATION, setOf(Equipment.CABLE))

        assertThat(EquipmentFit.isAvailable(crociAiCavi, profile)).isFalse()
        assertThat(WorkoutPlanValidator.isCompatible(crociAiCavi, profile)).isFalse()
    }

    @Test
    fun `un esercizio compatibile resta permesso`() {
        val profile = baseProfile().copy(
            availableEquipment = emptySet(),
            gearInventory = GearInventory(
                presets = listOf(GearItem(id = "Manubri regolabili", name = "Manubri regolabili", category = GearCategory.WEIGHTS))
            )
        )
        val pancaManubri = exercise(
            "Panca piana con manubri", ExerciseCategory.COMPOUND,
            setOf(Equipment.DUMBBELL, Equipment.BENCH)
        )

        assertThat(EquipmentFit.isAvailable(pancaManubri, profile)).isTrue()
    }

    @Test
    fun `il corpo libero resta sempre eseguibile`() {
        val profile = baseProfile().copy(
            gearInventory = GearInventory(
                presets = listOf(GearItem(id = "Manubri regolabili", name = "Manubri regolabili", category = GearCategory.WEIGHTS))
            )
        )
        val pushUp = exercise(
            "Push-up", ExerciseCategory.BODYWEIGHT,
            setOf(Equipment.BODYWEIGHT), MuscleGroup.CHEST
        )

        assertThat(EquipmentFit.isAvailable(pushUp, profile)).isTrue()
    }

    @Test
    fun `senza alcuna dichiarazione non si inventa un vincolo`() {
        // Profilo senza chip e senza inventario: "non dichiarato" non significa
        // "non possiede nulla". Il blocco avviene a monte, sull'anamnesi.
        val profile = baseProfile().copy(availableEquipment = emptySet())
        val croci = exercise("Croci ai cavi", ExerciseCategory.ISOLATION, setOf(Equipment.CABLE))

        assertThat(EquipmentFit.equipmentFor(profile)).isEmpty()
        assertThat(EquipmentFit.isAvailable(croci, profile)).isTrue()
    }

    @Test
    fun `la mappatura dei nomi riconosce gli attrezzi del catalogo`() {
        val expected = mapOf(
            "Bilanciere olimpico standard (20 kg)" to Equipment.BARBELL,
            "Manubri fissi" to Equipment.DUMBBELL,
            "Panca regolabile" to Equipment.BENCH,
            "Sbarra per trazioni" to Equipment.PULLUP_BAR,
            "Dip station" to Equipment.DIP_STATION,
            "Lat machine" to Equipment.CABLE,
            "Pulley basso" to Equipment.CABLE,
            "Leg press" to Equipment.MACHINE,
            "Kettlebell" to Equipment.KETTLEBELL,
            "Elastici" to Equipment.RESISTANCE_BAND
        )
        expected.forEach { (name, equipment) ->
            assertWithMessage("mappatura di %s", name)
                .that(EquipmentFit.fromGearName(name)).isEqualTo(equipment)
        }
    }

    @Test
    fun `il nome sconosciuto non viene associato a un attrezzo a caso`() {
        assertThat(EquipmentFit.fromGearName("Attrezzo misterioso")).isNull()
        // "Pressa" non e' "leg press": senza una parola chiave certa non si deduce.
        assertThat(EquipmentFit.fromGearName("Attrezzo misterioso")).isNull()
    }

    // ---------------- Isolamento ----------------

    @Test
    fun `una seduta di soli multiarticolari viene respinta`() {
        val compound = ExerciseSeedData.exercises.filter { it.category == ExerciseCategory.COMPOUND }
        assertThat(compound.size).isAtLeast(3)

        val days = (0..2).map { index ->
            AiWorkoutDay(
                dayIndex = index,
                exercises = compound.take(3).map {
                    AiPlannedExercise(
                        exerciseName = it.name, sets = 3, minReps = 8, maxReps = 12,
                        restSeconds = 120, targetRir = 2
                    )
                }
            )
        }
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(split = "FULL_BODY", days = days),
            calendar = AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2),
                restDayIndices = listOf(3, 4, 5, 6)
            )
        )
        val profile = baseProfile().copy(availableEquipment = Equipment.entries.toSet())

        val violations = ProposalChecker
            .check(profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises)
            .violations

        assertThat(violations.any { it.contains("isolamento") }).isTrue()
    }

    @Test
    fun `una seduta mista multiarticolare piu isolamento passa il controllo di composizione`() {
        val compound = ExerciseSeedData.exercises.filter { it.category == ExerciseCategory.COMPOUND }
        val isolation = ExerciseSeedData.exercises.filter { it.category == ExerciseCategory.ISOLATION }
        assertThat(isolation).isNotEmpty()

        val chosen = compound.take(2) + isolation.take(1)
        val days = (0..2).map { index ->
            AiWorkoutDay(
                dayIndex = index,
                exercises = chosen.map {
                    AiPlannedExercise(
                        exerciseName = it.name, sets = 3, minReps = 8, maxReps = 12,
                        restSeconds = 120, targetRir = 2
                    )
                }
            )
        }
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(split = "FULL_BODY", days = days),
            calendar = AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2),
                restDayIndices = listOf(3, 4, 5, 6)
            )
        )
        val profile = baseProfile().copy(availableEquipment = Equipment.entries.toSet())

        val violations = ProposalChecker
            .check(profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises)
            .violations

        assertThat(violations.none { it.contains("nessun esercizio di isolamento") }).isTrue()
    }

    @Test
    fun `con solo corpo libero l'isolamento non e un obbligo impossibile`() {
        // Nessun esercizio di isolamento del catalogo e' a corpo libero: se
        // l'obbligo fosse incondizionato, chi ha solo il proprio corpo non
        // potrebbe mai generare una scheda valida e non avrebbe come rimediare.
        val bodyweightOnly = ExerciseSeedData.exercises.filter {
            it.bodyweightOnly || it.equipment.isEmpty() || Equipment.BODYWEIGHT in it.equipment
        }
        assertThat(bodyweightOnly.none { it.category == ExerciseCategory.ISOLATION }).isTrue()

        // Un profilo a corpo libero non ha alcun multiarticolare disponibile:
        // la scheda che puo' realmente comporre e' fatta di BODYWEIGHT.
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                split = "FULL_BODY",
                days = (0..2).map { index ->
                    AiWorkoutDay(
                        dayIndex = index,
                        exercises = bodyweightOnly.take(3).map {
                            AiPlannedExercise(
                                exerciseName = it.name, sets = 3, minReps = 8, maxReps = 12,
                                restSeconds = 120, targetRir = 2
                            )
                        }
                    )
                }
            ),
            calendar = AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2),
                restDayIndices = listOf(3, 4, 5, 6)
            )
        )
        val profile = baseProfile().copy(availableEquipment = setOf(Equipment.BODYWEIGHT))

        val violations = ProposalChecker
            .check(profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises)
            .violations

        assertThat(violations.none { it.contains("nessun esercizio di isolamento") }).isTrue()
    }

    @Test
    fun `chi esclude tutti gli isolamenti non resta bloccato senza via d'uscita`() {
        // Caso reale: l'utente dichiara "non mi piace" o "movimento da evitare"
        // su tutti gli esercizi di isolamento. Con l'obbligo incondizionato la
        // scheda non puo' mai essere valida: l'AI non ha un isolamento lecito da
        // inserire e l'utente non ha modo di correggere. Il vincolo deve
        // rassegnarsi quando non esiste un isolamento ammesso.
        val isolationNames = ExerciseSeedData.exercises
            .filter { it.category == ExerciseCategory.ISOLATION }
            .map { it.name }
        assertThat(isolationNames).isNotEmpty()

        val compound = ExerciseSeedData.exercises.filter { it.category == ExerciseCategory.COMPOUND }
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                split = "FULL_BODY",
                days = (0..2).map { index ->
                    AiWorkoutDay(
                        dayIndex = index,
                        exercises = compound.take(3).map {
                            AiPlannedExercise(
                                exerciseName = it.name, sets = 3, minReps = 8, maxReps = 12,
                                restSeconds = 120, targetRir = 2
                            )
                        }
                    )
                }
            ),
            calendar = AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2),
                restDayIndices = listOf(3, 4, 5, 6)
            )
        )
        val profile = baseProfile().copy(
            availableEquipment = Equipment.entries.toSet(),
            dislikedExercises = isolationNames
        )

        val violations = ProposalChecker
            .check(profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises)
            .violations

        assertThat(violations.none { it.contains("nessun esercizio di isolamento") }).isTrue()
    }

    @Test
    fun `con i manubri disponibili l'isolamento torna obbligatorio`() {
        // Il vincolo non e' stato indebolito: con l'attrezzatura giusta resta.
        val compound = ExerciseSeedData.exercises.filter { it.category == ExerciseCategory.COMPOUND }
        val days = (0..2).map { index ->
            AiWorkoutDay(
                dayIndex = index,
                exercises = compound.take(3).map {
                    AiPlannedExercise(
                        exerciseName = it.name, sets = 3, minReps = 8, maxReps = 12,
                        restSeconds = 120, targetRir = 2
                    )
                }
            )
        }
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(split = "FULL_BODY", days = days),
            calendar = AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2),
                restDayIndices = listOf(3, 4, 5, 6)
            )
        )
        val profile = baseProfile().copy(availableEquipment = Equipment.entries.toSet())

        val violations = ProposalChecker
            .check(profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises)
            .violations

        assertThat(violations.any { it.contains("nessun esercizio di isolamento") }).isTrue()
    }

    @Test
    fun `il database contiene isolamento per i gruppi scoperti dai multiarticolari`() {
        val isolationMuscles = ExerciseSeedData.exercises
            .filter { it.category == ExerciseCategory.ISOLATION }
            .map { it.primaryMuscle }
            .toSet()

        // Deltoidi, bicipiti, tricipiti, femorali e polpacci: i gruppi che un
        // multiarticolare non stimola a sufficienza.
        assertThat(isolationMuscles).containsAtLeast(
            MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS,
            MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES
        )
    }

    // ---------------- Tecniche d'intensita' ----------------

    @Test
    fun `il prompt di generazione prescrive le tecniche d'intensita`() {
        val prompt = AiProgramGenerationService.generationSystemPrompt

        assertThat(prompt).contains("DROP_SET")
        assertThat(prompt).contains("REST_PAUSE")
        assertThat(prompt).contains("MYO_REPS")
        assertThat(prompt).contains("technique")
        assertThat(prompt).contains("techniqueReason")
    }

    @Test
    fun `il prompt di generazione impone l'isolamento in ogni seduta`() {
        val prompt = AiProgramGenerationService.generationSystemPrompt

        assertThat(prompt).contains("ISOLATION")
        assertThat(prompt).contains("isolamento")
    }

    @Test
    fun `la tecnica scritta dall'AI arriva sul campo della scheda`() {
        // Il contratto JSON usa 'technique'; il campo mostrato dalla UI e'
        // intensityTechnique. La mappatura passa da validate(): testare il solo
        // DTO non provava nulla, perche' il DTO conserva la stringa comunque.
        val proposal = validProposal(technique = "DROP_SET")
        val validated = ProgramGenerationEngine.validate(
            baseProfile(), proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(validated).isNotNull()
        val planned = validated!!.workout!!.days.flatMap { it.exercises }
        assertThat(planned).isNotEmpty()
        assertThat(planned.map { it.intensityTechnique }.distinct())
            .containsExactly(IntensityTechnique.DROP_SET)
        assertThat(planned.map { it.techniqueReason }.filter { it.isNotBlank() }).isNotEmpty()
    }

    @Test
    fun `le varianti di scrittura della tecnica non si perdono`() {
        // Il modello scrive la stessa tecnica in forme diverse. Il valueOf esatto
        // scartava tutto cio' che non era maiuscolo con underscore, e il
        // fallback silenzioso mostrava "Nessuna" al posto della tecnica.
        assertThat(IntensityTechnique.parse("DROP SET")).isEqualTo(IntensityTechnique.DROP_SET)
        assertThat(IntensityTechnique.parse("drop-set")).isEqualTo(IntensityTechnique.DROP_SET)
        assertThat(IntensityTechnique.parse("Dropset")).isEqualTo(IntensityTechnique.DROP_SET)
        assertThat(IntensityTechnique.parse(" drop_set ")).isEqualTo(IntensityTechnique.DROP_SET)
        assertThat(IntensityTechnique.parse("Rest Pause")).isEqualTo(IntensityTechnique.REST_PAUSE)
        assertThat(IntensityTechnique.parse("myo reps")).isEqualTo(IntensityTechnique.MYO_REPS)
        assertThat(IntensityTechnique.parse("Nessuna")).isNull()
        assertThat(IntensityTechnique.parse("")).isNull()
    }

    @Test
    fun `una tecnica inventata viene respinta invece di sparire in silenzio`() {
        val proposal = validProposal(technique = "SUPER_TECNICA_SEGRETA")
        val report = ProposalChecker.check(
            baseProfile(), proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(report.isValid).isFalse()
        assertThat(report.violations.joinToString()).contains("non riconosciuta")
    }

    @Test
    fun `i valori di tecnica ammessi coincidono con l'enum`() {
        val expected = setOf(
            "NONE", "DROP_SET", "REST_PAUSE", "MYO_REPS", "SUPERSET",
            "BACK_OFF_SET", "CLUSTER_SET", "FAILURE", "ECCENTRIC_EMPHASIS"
        )
        assertThat(
            com.fitcoach.app.domain.model.IntensityTechnique.entries.map { it.name }.toSet()
        ).isEqualTo(expected)
    }

    // ---------------- Chat: nessuna modifica fantasma ----------------

    @Test
    fun `la chat corregge l'annuncio di una modifica mai registrata`() {
        val orchestrator = orchestrator()

        val corrected = orchestrator.clarifyPendingProposal(
            "Procedo con la modifica e aggiungo le tecniche d'intensita' alla scheda.", false
        )

        assertThat(corrected).contains("Non ho applicato nessuna modifica")
        assertThat(corrected).doesNotContain("Procedo con la modifica")
    }

    @Test
    fun `la chat corregge anche il presente progressivo`() {
        val orchestrator = orchestrator()

        val corrected = orchestrator.clarifyPendingProposal("Sto aggiornando la scheda.", false)

        assertThat(corrected).contains("Non ho applicato nessuna modifica")
    }

    @Test
    fun `una frase senza pretese di modifica non viene alterata`() {
        val orchestrator = orchestrator()
        val text = "La panca e' migliorata di due ripetizioni: ottimo lavoro."

        assertThat(orchestrator.clarifyPendingProposal(text, false)).isEqualTo(text)
    }

    @Test
    fun `con una proposta in sospeso la modifica non e dichiarata attiva`() {
        val orchestrator = orchestrator()

        val corrected = orchestrator.clarifyPendingProposal(
            "Ho modificato la scheda con il drop set.", true
        )

        assertThat(corrected).contains("serve la tua conferma")
        assertThat(corrected).doesNotContain("Ho modificato")
    }
}
