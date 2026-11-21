package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.NutritionDiaryEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sincronizzazione dei target nutrizionali tra Home (Dashboard) e Alimentazione.
 *
 * Il bug segnalato: con un piano a ciclizzazione attiva la Home mostrava un
 * obiettivo diverso da quello del piano, e spuntando tutti gli alimenti il
 * "rimanente" andava a -399 kcal. La causa non era un numero da correggere:
 * Alimentazione registrava sul diario di OGGI le voci del giorno che l'utente
 * stava guardando, che non e' necessariamente oggi.
 *
 * Questi test usano il database reale e i repository reali: nessun mock del
 * motore, cosi' il percorso verificato e' quello che gira nell'app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionTargetSyncTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var diaryRepository: NutritionDiaryRepository

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 32, sex = Sex.MALE,
        heightCm = 180.0, weightKg = 82.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = com.fitcoach.app.domain.model.Goal.RECOMP,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db.userProfileDao())
        nutritionRepository = NutritionRepository(db)
        foodRepository = FoodRepository(db.foodDao())
        diaryRepository = NutritionDiaryRepository(db)
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        userRepository.save(profile)
    }

    @After
    fun tearDown() = db.close()

    /** Aggiunge alimenti ai pasti di ogni giorno, cosi' il piano e' spuntabile. */
    private suspend fun riempiPiano() {
        val piano = nutritionRepository.getActivePlan()!!
        val alimenti = foodRepository.getAll()
        piano.days.forEach { giorno ->
            giorno.meals.forEachIndexed { indicePasto, pasto ->
                val cibo = alimenti[(giorno.dayIndex * 5 + indicePasto) % alimenti.size]
                nutritionRepository.addEntryToMeal(
                    mealId = pasto.id,
                    food = cibo,
                    grams = 100.0,
                    unit = com.fitcoach.app.domain.model.FoodUnit.RAW
                )
            }
        }
    }

    /** Crea un piano a ciclizzazione con i target dei motori reali. */
    private suspend fun creaPiano(): Long {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        val dayTypes = CarbCyclingEngine.assignDayTypes(
            highDays = cycling.highDays,
            mediumDays = cycling.mediumDays,
            lowDays = cycling.lowDays,
            trainingDayIndices = (0 until profile.trainingDaysPerWeek).toList()
        )
        return nutritionRepository.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = dayTypes,
            trainingDayIndices = (0 until profile.trainingDaysPerWeek).toList()
        )
    }

    /**
     * Il difetto centrale: spuntando gli alimenti di un giorno diverso da oggi,
     * le voci finivano sul diario di oggi. La spunta deve restare sul giorno
     * che l'utente sta guardando.
     */
    @Test
    fun `gli alimenti spuntati su un altro giorno non finiscono sul diario di oggi`() = runTest {
        creaPiano()
        riempiPiano()
        val oggi = NutritionDiaryRepository.todayEpochDay()
        val giornoOggi = NutritionDiaryEngine.dayIndexFor(oggi)
        val giornoDiverso = (giornoOggi + 1) % 7

        // Data calcolata qui, senza usare la funzione che il test deve validare:
        // altrimenti un errore nella funzione renderebbe il test cieco.
        val lunedi = LocalDate.ofEpochDay(oggi).minusDays(
            (LocalDate.ofEpochDay(oggi).dayOfWeek.value - 1).toLong()
        )
        val dataGiornoDiverso = lunedi.plusDays(giornoDiverso.toLong()).toEpochDay()

        val piano = nutritionRepository.getActivePlan()!!
        val giornoGuardato = piano.days.first { it.dayIndex == giornoDiverso }
        val pasto = giornoGuardato.meals.first { it.entries.isNotEmpty() }
        val entry = pasto.entries.first()

        assertThat(diaryRepository.balanceForDay(oggi).consumedKcal).isEqualTo(0.0)

        // La spunta viene registrata sulla data del giorno guardato, non su oggi.
        diaryRepository.togglePlanEntry(entry, pasto.slot, pasto.time, dataGiornoDiverso)

        assertThat(diaryRepository.balanceForDay(oggi).consumedKcal).isEqualTo(0.0)
        assertThat(diaryRepository.balanceForDay(dataGiornoDiverso).consumedKcal)
            .isEqualTo(entry.kcal)
    }

    /**
     * La funzione che lega un giorno del piano alla data reale deve essere
     * coerente con il giorno della settimana: 0 = lunedi.
     */
    @Test
    fun `il giorno del piano corrisponde al giorno della settimana`() = runTest {
        val oggi = NutritionDiaryRepository.todayEpochDay()
        (0..6).forEach { indice ->
            val data = NutritionDiaryEngine.epochDayForDayIndex(indice, oggi)
            assertThat(NutritionDiaryEngine.dayIndexFor(data)).isEqualTo(indice)
        }
    }

    /** Un giorno del piano e' sempre nella settimana corrente, non in quella dopo. */
    @Test
    fun `la data di un giorno del piano cade nella stessa settimana di oggi`() = runTest {
        val oggi = NutritionDiaryRepository.todayEpochDay()
        val lunediOggi = NutritionDiaryRepository.weekStart(oggi)

        (0..6).forEach { indice ->
            val data = NutritionDiaryEngine.epochDayForDayIndex(indice, oggi)
            assertThat(NutritionDiaryRepository.weekStart(data)).isEqualTo(lunediOggi)
        }
    }

    /** La Home deve leggere il target del giorno corrente del piano attivo. */
    @Test
    fun `il bilancio di oggi usa il target del giorno di oggi, non quello del giorno 1`() = runTest {
        creaPiano()
        val oggi = NutritionDiaryRepository.todayEpochDay()
        val giornoOggi = NutritionDiaryEngine.dayIndexFor(oggi)
        val piano = nutritionRepository.getActivePlan()!!
        val targetAtteso = piano.days.first { it.dayIndex == giornoOggi }

        val bilancio = diaryRepository.balanceForDay(oggi)

        assertThat(bilancio.targetKcal).isEqualTo(targetAtteso.targetKcal)
        assertThat(bilancio.targetProtein).isEqualTo(targetAtteso.targetProtein)
        assertThat(bilancio.targetCarbs).isEqualTo(targetAtteso.targetCarbs)
        assertThat(bilancio.targetFat).isEqualTo(targetAtteso.targetFat)
    }

    /** Con ciclizzazione attiva, HIGH e LOW devono avere target diversi. */
    @Test
    fun `con la ciclizzazione attiva i giorni HIGH e LOW hanno target distinti`() = runTest {
        creaPiano()
        val piano = nutritionRepository.getActivePlan()!!
        val high = piano.days.firstOrNull { it.dayType == CarbDayType.HIGH }
        val low = piano.days.firstOrNull { it.dayType == CarbDayType.LOW }

        assertThat(high).isNotNull()
        assertThat(low).isNotNull()
        assertThat(high!!.targetKcal).isNotEqualTo(low!!.targetKcal)
        assertThat(high.targetCarbs).isGreaterThan(low.targetCarbs)
    }
}
