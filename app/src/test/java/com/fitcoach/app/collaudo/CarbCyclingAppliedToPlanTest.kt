package com.fitcoach.app.collaudo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * La distribuzione personalizzata deve cambiare il piano ATTIVO, non solo il
 * profilo. Prima di questo test l'utente salvava la terna e il piano restava
 * identico fino alla rigenerazione con AI, cioe' la funzione era di fatto
 * inutilizzabile offline.
 */
@RunWith(RobolectricTestRunner::class)
class CarbCyclingAppliedToPlanTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var nutrition: NutritionRepository
    private lateinit var users: UserRepository

    private val profilo = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 178.0, weightKg = 78.0, bodyFatPercent = 15.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "ok"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        nutrition = NutritionRepository(db)
        users = UserRepository(db.userProfileDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun creaPiano(profile: UserProfile): Long {
        db.userProfileDao().upsert(Mappers.run { profile.toEntity() })
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        return nutrition.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = List(7) { CarbDayType.MEDIUM },
            trainingDayIndices = (0..3).toList()
        )
    }

    @Test
    fun `applicare la ciclicita aggiorna tipo e target dei sette giorni`() = runTest {
        val planId = creaPiano(profilo)
        val custom = profilo.copy(
            carbHighDaysOverride = 2,
            carbMediumDaysOverride = 3,
            carbLowDaysOverride = 2
        )
        users.save(custom)
        val energy = EnergyCalculator.compute(custom)!!
        val cycling = CarbCyclingEngine.build(custom, energy)

        val ok = nutrition.applyCarbCyclingAsNewPlanVersion(
            planId = planId,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(2, 3, 2, (0..3).toList()),
            tdee = energy.tdee,
            diaryEpochDay = 20_000L
        )
        assertThat(ok).isTrue()

        val plan = nutrition.getActivePlan()!!
        assertThat(plan.days).hasSize(7)
        assertThat(plan.highCarbDays).isEqualTo(2)
        assertThat(plan.mediumCarbDays).isEqualTo(3)
        assertThat(plan.lowCarbDays).isEqualTo(2)
        assertThat(plan.days.count { it.dayType == CarbDayType.HIGH }).isEqualTo(2)
        assertThat(plan.days.count { it.dayType == CarbDayType.LOW }).isEqualTo(2)

        // I target seguono la tipologia: HIGH e LOW non possono avere le stesse
        // calorie, altrimenti il carb cycling non e' applicato davvero.
        val high = plan.days.first { it.dayType == CarbDayType.HIGH }
        val low = plan.days.first { it.dayType == CarbDayType.LOW }
        assertThat(high.targetKcal).isGreaterThan(low.targetKcal)
        assertThat(high.targetCarbs).isGreaterThan(low.targetCarbs)
    }

    @Test
    fun `l'applicazione conserva i pasti gia' presenti nel piano`() = runTest {
        val planId = creaPiano(profilo)
        val prima = nutrition.getActivePlan()!!
        val pastiPrima = prima.days.sumOf { it.meals.size }
        assertThat(pastiPrima).isGreaterThan(0)

        val energy = EnergyCalculator.compute(profilo)!!
        val cycling = CarbCyclingEngine.build(profilo, energy)
        nutrition.applyCarbCyclingAsNewPlanVersion(
            planId = planId,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(3, 3, 1, (0..3).toList()),
            tdee = energy.tdee,
            diaryEpochDay = 20_000L
        )

        val dopo = nutrition.getActivePlan()!!
        assertThat(dopo.days.sumOf { it.meals.size }).isEqualTo(pastiPrima)
    }

    @Test
    fun `il bilancio settimanale riflette i nuovi target`() = runTest {
        val planId = creaPiano(profilo)
        val energy = EnergyCalculator.compute(profilo)!!
        val cycling = CarbCyclingEngine.build(profilo, energy)

        nutrition.applyCarbCyclingAsNewPlanVersion(
            planId = planId,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(
                cycling.highDays, cycling.mediumDays, cycling.lowDays, (0..3).toList()
            ),
            tdee = energy.tdee,
            diaryEpochDay = 20_000L
        )

        val plan = nutrition.getActivePlan()!!
        val somma = plan.days.sumOf { it.targetKcal }
        // I macro sono in grammi interi: la somma dei 7 giorni non puo' coincidere
        // al decimale col target teorico (granularita' 4 kcal/giorno). Il piano
        // deve dichiarare cio' che contiene, cioe' weeklyKcalFromTarget.
        assertThat(plan.weeklyCalories).isWithin(1.0).of(cycling.weeklyKcalFromTarget)
        assertThat(plan.weeklyCalories).isWithin(1.0).of(somma)
        // Lo scarto dal target teorico resta rumore di arrotondamento, non un errore.
        assertThat(abs(cycling.weeklyKcalFromTarget - cycling.weeklyKcal))
            .isLessThan(30.0)
        assertThat(plan.weeklyKcalBalance).isWithin(1.0).of(somma - energy.tdee * 7)
    }
}
