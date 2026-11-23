package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Collaudo del catalogo alimentare usato per la generazione dei piani.
 *
 * Riproduce due difetti segnalati dall'utente:
 *  - "genera programmi errati": il contesto passato al Coach AI conteneva i
 *    primi 90 alimenti in ordine alfabetico, cioe fast food americani in
 *    inglese ("APPLEBEE'S...", "ARBY'S..."): il modello non poteva costruire un
 *    piano italiano sensato;
 *  - "tutti gli alimenti sono solo in inglese": le liste mostrate all'utente
 *    devono anteporre il catalogo italiano CREA.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FoodCatalogCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var foodRepository: FoodRepository

    private val profile = UserProfile(
        id = 1L, name = "Marjan", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, goal = Goal.RECOMP,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata",
    )

    /** Database realistico: seed italiano CREA + alimenti USDA in inglese. */
    private val englishFoods = listOf(
        FoodItem(
            id = 0, name = "APPLEBEE'S, french fries", category = "Ristorazione",
            kcalPer100g = 290.0, proteinPer100g = 3.3, carbsPer100g = 39.5,
            fatPer100g = 13.2, source = "USDA FoodData Central",
        ),
        FoodItem(
            id = 0, name = "ARBY'S, roast beef sandwich", category = "Fast food",
            kcalPer100g = 242.0, proteinPer100g = 15.2, carbsPer100g = 22.2,
            fatPer100g = 10.3, source = "USDA FoodData Central",
        ),
        FoodItem(
            id = 0, name = "Chicken, broilers or fryers, breast, meat only",
            category = "Carni", kcalPer100g = 120.0, proteinPer100g = 22.5,
            carbsPer100g = 0.0, fatPer100g = 2.6, source = "USDA FoodData Central",
        ),
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        foodRepository = FoodRepository(db.foodDao())
        db.foodDao().insertAll((FoodSeedData.foods + englishFoods).map { it.toEntity() })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `il catalogo passato all'AI non e composto da fast food in inglese`() = runBlocking {
        val all = foodRepository.getAll()

        val selected = FoodCatalogSelector.forGeneration(all, profile, limit = 90)

        assertThat(selected).isNotEmpty()
        // Nessun alimento da ristorazione o fast food.
        assertThat(selected.map { it.category }).doesNotContain("Ristorazione")
        assertThat(selected.map { it.category }).doesNotContain("Fast food")
        // Tutti gli alimenti hanno nome italiano: le voci USDA dell'import
        // globale sono escluse dalla generazione (vedi FoodCatalogSelector).
        val italianNamed = selected.count { !it.source.startsWith("USDA FoodData Central (SR Legacy)") }
        assertThat(italianNamed).isEqualTo(selected.size)
        // Il pollo italiano e presente; il nome brandizzato americano no.
        assertThat(selected.map { it.name }).contains("Petto di pollo")
        assertThat(selected.map { it.name }).doesNotContain("APPLEBEE'S, french fries")
    }

    @Test
    fun `il catalogo rispetta le allergie dichiarate`() = runBlocking {
        val allergic = profile.copy(allergies = listOf("Pesce"))
        val all = foodRepository.getAll()

        val selected = FoodCatalogSelector.forGeneration(all, allergic, limit = 90)

        assertThat(selected.map { it.category }).doesNotContain("Pesce")
        assertThat(selected.map { it.name }).doesNotContain("Salmone")
    }

    @Test
    fun `le liste mostrate all'utente antepongono gli alimenti italiani`() = runBlocking {
        val all = foodRepository.getAll()

        // Il primo blocco deve essere italiano: le migliaia di voci USDA inglesi
        // non devono piu comparire prima del catalogo CREA.
        val first = all.first()
        assertThat(first.source).startsWith("CREA")

        val firstItalian = all.indexOfFirst { it.source.startsWith("CREA") }
        val firstEnglish = all.indexOfFirst { !it.source.startsWith("CREA") }
        assertThat(firstItalian).isEqualTo(0)
        assertThat(firstEnglish).isGreaterThan(firstItalian)
    }

    @Test
    fun `un profilo vegano non riceve alimenti di origine animale`() = runBlocking {
        val vegan = profile.copy(dietaryPreferences = setOf(DietaryPreference.VEGAN))
        val all = foodRepository.getAll()

        val selected = FoodCatalogSelector.forGeneration(all, vegan, limit = 90)

        assertThat(selected.map { it.category })
            .containsNoneIn(listOf("Carne", "Pesce", "Latticini", "Uova", "Salumi"))
    }
}
