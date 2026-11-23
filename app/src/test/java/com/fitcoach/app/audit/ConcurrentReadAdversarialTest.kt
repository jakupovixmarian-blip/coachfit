package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.NutritionPlanEntity
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.SplitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `getActivePlan` viene chiamata da ogni schermata, spesso piu' volte in
 * parallelo (dashboard, progressi, coach). Aprire una transazione di scrittura
 * dentro un percorso di sola lettura le fa contendere il database: la lettura
 * che arriva mentre un'altra scrittura e' aperta fallisce con
 * "Cannot perform this operation because there is no current transaction".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConcurrentReadAdversarialTest {

    private lateinit var db: FitCoachDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `letture concorrenti della scheda attiva non devono fallire`() = runTest {
        val workout = WorkoutRepository(db)
        val nutrition = NutritionRepository(db)
        // Stato incoerente di proposito: due piani attivi. E' la condizione in
        // cui il vecchio normalizeActivePlan eseguiva davvero la scrittura.
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(profileId = 1L, name = "Scheda A", splitType = SplitType.UPPER_LOWER, isActive = true, createdAt = 1L)
        )
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(profileId = 1L, name = "Scheda B", splitType = SplitType.UPPER_LOWER, isActive = true, createdAt = 2L)
        )
        db.nutritionPlanDao().insert(
            NutritionPlanEntity(
                profileId = 1L, name = "Piano A", isActive = true, createdAt = 1L,
                bmr = 1700.0, tdee = 2600.0, weeklyCalories = 18200.0,
                weeklyKcalBalance = -1400.0, bmrFormula = "Mifflin-St Jeor",
                tdeeFormula = "Mifflin x 1.55", highCarbDays = 3,
                mediumCarbDays = 2, lowCarbDays = 2, proteinGPerKg = 2.0
            )
        )
        db.nutritionPlanDao().insert(
            NutritionPlanEntity(
                profileId = 1L, name = "Piano B", isActive = true, createdAt = 2L,
                bmr = 1700.0, tdee = 2600.0, weeklyCalories = 18200.0,
                weeklyKcalBalance = -1400.0, bmrFormula = "Mifflin-St Jeor",
                tdeeFormula = "Mifflin x 1.55", highCarbDays = 3,
                mediumCarbDays = 2, lowCarbDays = 2, proteinGPerKg = 2.0
            )
        )

        // Piu' schermate leggono nello stesso istante, su un dispatcher reale.
        val errori = withContext(Dispatchers.Default) {
            (1..24).map { i ->
                async {
                    runCatching {
                        if (i % 2 == 0) workout.getActivePlan() else nutrition.getActivePlan()
                    }.exceptionOrNull()
                }
            }.awaitAll().filterNotNull()
        }

        assertEquals(
            "Letture concorrenti in errore: " + errori.map { it.message }.distinct(),
            0, errori.size
        )
    }

    @Test
    fun `la riparazione esplicita lascia una sola scheda attiva`() = runTest {
        val workout = WorkoutRepository(db)
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(profileId = 1L, name = "Vecchia", splitType = SplitType.UPPER_LOWER, isActive = true, createdAt = 1L)
        )
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(profileId = 1L, name = "Recente", splitType = SplitType.UPPER_LOWER, isActive = true, createdAt = 2L)
        )

        workout.normalizeActivePlanState()

        val attivi = db.workoutPlanDao().getAll().filter { it.isActive }
        assertEquals("Deve restare una sola scheda attiva", 1, attivi.size)
        assertEquals("Deve restare la piu recente", "Recente", attivi.first().name)
    }

    @Test
    fun `leggere la scheda attiva non deve modificare il database`() = runTest {
        val workout = WorkoutRepository(db)
        // Stato incoerente di proposito: due piani attivi. E' la condizione in
        // cui il vecchio normalizeActivePlan eseguiva davvero la scrittura.
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(profileId = 1L, name = "Scheda A", splitType = SplitType.UPPER_LOWER, isActive = true, createdAt = 1L)
        )
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(profileId = 1L, name = "Scheda B", splitType = SplitType.UPPER_LOWER, isActive = true, createdAt = 2L)
        )

        // Lo stato va letto PRIMA della chiamata: catturarlo dopo misura solo
        // il risultato della normalizzazione e il test non puo' fallire.
        val prima = db.workoutPlanDao().getAll().map { it.id to it.isActive }

        workout.getActivePlan()

        assertEquals(
            "getActivePlan e' una lettura: non deve riattivare o disattivare piani. " +
                "La scrittura dentro il percorso di lettura apriva una transazione " +
                "che, con piu' schermate in lettura, falliva con 'no current transaction'.",
            prima, db.workoutPlanDao().getAll().map { it.id to it.isActive }
        )
    }
}
