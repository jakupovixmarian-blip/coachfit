package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.domain.model.SplitType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `workout_plans` e' padre di `mesocycles` con onDelete CASCADE, e
 * `WorkoutPlanDao.insert` usa OnConflictStrategy.REPLACE. In SQLite un
 * REPLACE su una riga esistente esegue DELETE + INSERT: la DELETE attiva la
 * cascata.
 *
 * Questo test isola il comportamento del DAO, senza coach di mezzo: se
 * riscrivere un piano esistente ne azzera la struttura, ogni percorso che lo
 * fa (riattivazione, rollback) distrugge la scheda dell'utente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CascadeReplaceAdversarialTest {

    private lateinit var db: FitCoachDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun pianoConStruttura(): Long {
        val planDao = db.workoutPlanDao()
        val planId = planDao.insert(
            WorkoutPlanEntity(profileId = 1L, name = "Scheda", splitType = SplitType.UPPER_LOWER, isActive = true)
        )
        val mesoId = db.mesocycleDao().insert(
            com.fitcoach.app.data.local.entity.MesocycleEntity(
                planId = planId, name = "Ipertrofia", focus = "ipertrofia",
                weekCount = 6, startWeekIndex = 0, deloadWeekIndex = 5,
                progressionModel = "DOUBLE_PROGRESSION"
            )
        )
        db.workoutDayDao().insertAll(listOf(
            com.fitcoach.app.data.local.entity.WorkoutDayEntity(
                mesocycleId = mesoId, dayIndex = 0, title = "Upper",
                muscleGroups = emptySet(), isRestDay = false
            )
        ))
        return planId
    }

    @Test
    fun `riattivare un piano con update conserva i mesocicli`() = runTest {
        val planId = pianoConStruttura()
        assertTrue(db.mesocycleDao().getForPlan(planId).isNotEmpty())

        // Il percorso corretto: UPDATE dei soli campi che cambiano.
        db.workoutPlanDao().deactivateAll()
        db.workoutPlanDao().update(db.workoutPlanDao().getById(planId)!!.copy(isActive = true))

        assertEquals(
            "Un UPDATE non deve toccare i figli: la cascata parte solo da un DELETE.",
            1, db.mesocycleDao().getForPlan(planId).size
        )
    }

    @Test
    fun `REPLACE su un padre con figli CASCADE distrugge i figli`() = runTest {
        val planId = pianoConStruttura()
        val esistente = db.workoutPlanDao().getById(planId)!!

        // Guardia sul perche' i percorsi di scrittura usano update invece di
        // insert: se un giorno Room cambiasse semantica, questo test lo dice.
        db.workoutPlanDao().insert(esistente.copy(isActive = true))

        assertTrue(
            "REPLACE su workout_plans ha cancellato i mesocicli: e' il motivo per cui " +
                "riattivare o annullare un piano non deve mai passare da insert.",
            db.mesocycleDao().getForPlan(planId).isEmpty()
        )
    }

    @Test
    fun `aggiornare il giorno di una seduta non ne cancella gli esercizi`() = runTest {
        val planId = pianoConStruttura()
        val meso = db.mesocycleDao().getForPlan(planId).first()
        val giorno = db.workoutDayDao().getForMesocycle(meso.id).first()
        db.plannedExerciseDao().insertAll(listOf(
            com.fitcoach.app.data.local.entity.PlannedExerciseEntity(
                workoutDayId = giorno.id, exerciseId = 1L, exerciseName = "Panca piana",
                primaryMuscle = com.fitcoach.app.domain.model.MuscleGroup.CHEST,
                orderIndex = 0, sets = 4, minReps = 8, maxReps = 10, restSeconds = 120,
                targetRir = 2, category = com.fitcoach.app.domain.model.ExerciseCategory.COMPOUND
            )
        ))

        db.workoutDayDao().updateDayIndex(giorno.id, (giorno.dayIndex + 2) % 7)

        val dopo = db.workoutDayDao().getById(giorno.id)!!
        assertEquals((giorno.dayIndex + 2) % 7, dopo.dayIndex)
        assertTrue(
            "Spostare la seduta con updateDayIndex deve conservare gli esercizi",
            db.plannedExerciseDao().getForDay(giorno.id).isNotEmpty()
        )
    }
}
