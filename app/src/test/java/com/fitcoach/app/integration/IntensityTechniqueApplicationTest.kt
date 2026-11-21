package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.coach.tools.SubstituteExerciseTool
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La chat che applica una tecnica d'intensita' aggiornava il piano ma la sezione
 * non compariva nella scheda, perche' il tool scriveva solo il campo testuale
 * `technique` mentre la UI legge `intensityTechnique`. Questo test fissa il
 * percorso completo: tool AI, proposta, applicazione, ripristino.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IntensityTechniqueApplicationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var executor: CoachActionExecutor
    private lateinit var tool: SubstituteExerciseTool

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val userRepository = UserRepository(db.userProfileDao())
        userRepository.save(
            UserProfile(
                id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
                heightCm = 175.0, weightKg = 80.0
            )
        )
        executor = CoachActionExecutor(
            db = db,
            changeRepository = ChangeRepository(db.programChangeDao()),
            userRepository = userRepository,
            generationCoordinator = Lazy { error("non usato in questo test") },
            json = json
        )
        tool = SubstituteExerciseTool(executor)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedPlannedExercise(): Long {
        // La catena di chiavi esterne e' reale: piano -> mesociclo -> giorno.
        val planId = db.workoutPlanDao().insert(
            com.fitcoach.app.data.local.entity.WorkoutPlanEntity(
                name = "Test", splitType = com.fitcoach.app.domain.model.SplitType.UPPER_LOWER
            )
        )
        val mesocycleId = db.mesocycleDao().insert(
            com.fitcoach.app.data.local.entity.MesocycleEntity(
                planId = planId, name = "Ipertrofia", focus = "Ipertrofia", weekCount = 5
            )
        )
        val dayId = db.workoutDayDao().insertAll(
            listOf(
                com.fitcoach.app.data.local.entity.WorkoutDayEntity(
                    mesocycleId = mesocycleId, dayIndex = 0, title = "Upper A"
                )
            )
        ).first()
        val plannedId = db.plannedExerciseDao().insertAll(
            listOf(
                com.fitcoach.app.data.local.entity.PlannedExerciseEntity(
                    workoutDayId = dayId,
                    exerciseId = 10L,
                    exerciseName = "Panca piana",
                    primaryMuscle = MuscleGroup.CHEST,
                    orderIndex = 0,
                    sets = 4,
                    minReps = 8,
                    maxReps = 10,
                    restSeconds = 120,
                    targetRir = 2,
                    targetWeightKg = 70.0,
                    targetReps = 8
                )
            )
        ).first()
        return plannedId
    }

    @Test
    fun `la tecnica proposta dalla chat arriva nella scheda`() = runTest {
        val plannedId = seedPlannedExercise()

        val args = JsonObject(
            mapOf(
                "plannedId" to JsonPrimitive(plannedId),
                "exerciseId" to JsonPrimitive(11L),
                "exerciseName" to JsonPrimitive("Chest press"),
                "intensityTechnique" to JsonPrimitive("DROP_SET"),
                "techniqueReason" to JsonPrimitive("Ultima serie a cedimento con drop."),
                "title" to JsonPrimitive("Sostituzione con drop set")
            )
        )
        val response = tool.execute(args)
        assertThat(response).contains("\"ok\":true")

        val proposed = db.programChangeDao().getAll().first()
        assertThat(executor.apply(proposed.id)).isInstanceOf(
            com.fitcoach.app.data.repository.ApplyResult.Applied::class.java
        )

        val updated = db.plannedExerciseDao().getById(plannedId)!!
        assertThat(updated.exerciseName).isEqualTo("Chest press")
        // I due campi che la scheda legge per mostrare il riquadro della tecnica.
        assertThat(updated.intensityTechnique).isEqualTo(IntensityTechnique.DROP_SET)
        assertThat(updated.techniqueReason).contains("drop")
    }

    @Test
    fun `annullare la sostituzione riporta esercizio e tecnica precedenti`() = runTest {
        val plannedId = seedPlannedExercise()
        val args = JsonObject(
            mapOf(
                "plannedId" to JsonPrimitive(plannedId),
                "exerciseId" to JsonPrimitive(11L),
                "exerciseName" to JsonPrimitive("Chest press"),
                "intensityTechnique" to JsonPrimitive("MYO_REPS")
            )
        )
        tool.execute(args)
        val proposed = db.programChangeDao().getAll().first()
        executor.apply(proposed.id)

        assertThat(executor.revert(proposed.id)).isInstanceOf(
            com.fitcoach.app.data.repository.ApplyResult.Reverted::class.java
        )

        val restored = db.plannedExerciseDao().getById(plannedId)!!
        assertThat(restored.exerciseName).isEqualTo("Panca piana")
        assertThat(restored.exerciseId).isEqualTo(10L)
        // La tecnica non deve restare addosso all'esercizio ripristinato.
        assertThat(restored.intensityTechnique).isEqualTo(IntensityTechnique.NONE)
        assertThat(restored.techniqueReason).isEmpty()
    }

    @Test
    fun `una tecnica sconosciuta non viene applicata come se fosse valida`() = runTest {
        val plannedId = seedPlannedExercise()
        tool.execute(
            JsonObject(
                mapOf(
                    "plannedId" to JsonPrimitive(plannedId),
                    "exerciseId" to JsonPrimitive(11L),
                    "exerciseName" to JsonPrimitive("Chest press"),
                    "intensityTechnique" to JsonPrimitive("SUPER_TECNICA_INVENTATA")
                )
            )
        )
        val proposed = db.programChangeDao().getAll().first()
        executor.apply(proposed.id)

        val updated = db.plannedExerciseDao().getById(plannedId)!!
        assertThat(updated.intensityTechnique).isEqualTo(IntensityTechnique.NONE)
    }
}
