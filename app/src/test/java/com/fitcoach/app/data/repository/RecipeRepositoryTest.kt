package com.fitcoach.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.SavedRecipeEntity
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.ComposedRecipe
import com.fitcoach.app.domain.nutrition.RecipeStyle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Persistenza delle ricette create: serializzazione, rilettura e cancellazione
 * su un database Room reale in memoria.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeRepositoryTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var repository: RecipeRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RecipeRepository(db.savedRecipeDao())
    }

    @After
    fun tearDown() = db.close()

    private fun entry(name: String, grams: Double) = MealEntry(
        foodItemId = name.hashCode().toLong(),
        foodName = name,
        grams = grams,
        unit = FoodUnit.RAW,
        kcal = grams * 1.5,
        protein = grams * 0.1,
        carbs = grams * 0.2,
        fat = grams * 0.05
    )

    private fun recipe(name: String = "Bowl proteica") = ComposedRecipe(
        slot = MealSlot.LUNCH,
        name = name,
        ingredients = listOf(entry("Petto di pollo", 180.0), entry("Riso basmati", 90.0)),
        style = RecipeStyle.CLASSIC,
        steps = listOf("Cuoci il riso.", "Griglia il pollo.", "Unisci.")
    )

    @Test
    fun `salva e rilegge gli ingredienti con le grammature`() = runTest {
        val id = repository.save(recipe(), dayIndex = 2)
        assertThat(id).isGreaterThan(0L)

        val stored = db.savedRecipeDao().getAll().single()
        val ingredients = repository.ingredientsOf(stored)

        assertThat(ingredients).hasSize(2)
        assertThat(ingredients[0].name).isEqualTo("Petto di pollo")
        assertThat(ingredients[0].grams).isEqualTo(180.0)
        assertThat(ingredients[1].name).isEqualTo("Riso basmati")
        assertThat(ingredients[1].grams).isEqualTo(90.0)
    }

    @Test
    fun `i totali salvati coincidono con la ricetta di origine`() = runTest {
        val original = recipe()
        repository.save(original, dayIndex = 0)
        val stored = db.savedRecipeDao().getAll().single()

        assertThat(stored.kcal).isWithin(0.001).of(original.kcal)
        assertThat(stored.protein).isWithin(0.001).of(original.protein)
        assertThat(stored.carbs).isWithin(0.001).of(original.carbs)
        assertThat(stored.fat).isWithin(0.001).of(original.fat)
        assertThat(stored.dayIndex).isEqualTo(0)
        assertThat(stored.slot).isEqualTo("LUNCH")
    }

    @Test
    fun `i passi di preparazione vengono conservati in ordine`() = runTest {
        repository.save(recipe(), dayIndex = 0)
        val stored = db.savedRecipeDao().getAll().single()
        assertThat(repository.stepsOf(stored))
            .containsExactly("Cuoci il riso.", "Griglia il pollo.", "Unisci.").inOrder()
    }

    @Test
    fun `observeSaved emette le ricette salvate`() = runTest {
        repository.save(recipe("Prima"), dayIndex = 0)
        repository.save(recipe("Seconda"), dayIndex = 1)

        val saved = repository.observeSaved().first()
        assertThat(saved.map { it.name }).containsExactly("Prima", "Seconda")
    }

    @Test
    fun `eliminare una ricetta la rimuove dall'elenco`() = runTest {
        val id = repository.save(recipe(), dayIndex = 0)
        assertThat(repository.count()).isEqualTo(1)

        repository.delete(id)

        assertThat(repository.count()).isEqualTo(0)
        assertThat(repository.observeSaved().first()).isEmpty()
    }

    @Test
    fun `una riga corrotta non fa fallire la lettura degli ingredienti`() {
        val broken = SavedRecipeEntity(
            id = 1,
            name = "Rotta",
            slot = "LUNCH",
            dayIndex = 0,
            style = "CLASSIC",
            ingredients = "solo un nome\u001Fsenza grammi",
            steps = "",
            kcal = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0,
            createdAt = 0
        )
        assertThat(repository.ingredientsOf(broken)).isEmpty()
    }

    @Test
    fun `il nome con separatori non rompe la serializzazione`() = runTest {
        repository.save(recipe("Pollo e riso, con verdure"), dayIndex = 0)
        val stored = db.savedRecipeDao().getAll().single()
        assertThat(stored.name).isEqualTo("Pollo e riso, con verdure")
        assertThat(repository.ingredientsOf(stored)).hasSize(2)
    }
}
