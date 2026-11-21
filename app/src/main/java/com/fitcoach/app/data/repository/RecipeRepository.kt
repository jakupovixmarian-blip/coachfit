package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.dao.SavedRecipeDao
import com.fitcoach.app.data.local.entity.SavedRecipeEntity
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.ComposedRecipe
import com.fitcoach.app.domain.nutrition.RecipeStyle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val dao: SavedRecipeDao
) {

    private companion object {
        const val FIELD = "\u001F"
        const val ROW = "\u001E"
    }

    fun observeSaved(): Flow<List<SavedRecipeEntity>> = dao.observeAll()

    suspend fun save(recipe: ComposedRecipe, dayIndex: Int): Long {
        val ingredients = recipe.ingredients.joinToString(ROW) { e ->
            listOf(e.foodName, e.grams.toString(), e.kcal.toString(),
                e.protein.toString(), e.carbs.toString(), e.fat.toString()).joinToString(FIELD)
        }
        return dao.insert(
            SavedRecipeEntity(
                name = recipe.name,
                slot = recipe.slot.name,
                dayIndex = dayIndex,
                style = recipe.style.name,
                ingredients = ingredients,
                steps = recipe.steps.joinToString(ROW),
                kcal = recipe.kcal,
                protein = recipe.protein,
                carbs = recipe.carbs,
                fat = recipe.fat,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun count(): Int = dao.count()

    /** Ricostruisce gli ingredienti salvati come triple nome/grammi/macro. */
    fun ingredientsOf(entity: SavedRecipeEntity): List<SavedIngredient> =
        entity.ingredients.split(ROW).filter { it.isNotBlank() }.mapNotNull { row ->
            val p = row.split(FIELD)
            if (p.size < 6) return@mapNotNull null
            SavedIngredient(
                name = p[0],
                grams = p[1].toDoubleOrNull() ?: return@mapNotNull null,
                kcal = p[2].toDoubleOrNull() ?: 0.0,
                protein = p[3].toDoubleOrNull() ?: 0.0,
                carbs = p[4].toDoubleOrNull() ?: 0.0,
                fat = p[5].toDoubleOrNull() ?: 0.0
            )
        }

    fun stepsOf(entity: SavedRecipeEntity): List<String> =
        entity.steps.split(ROW).filter { it.isNotBlank() }

    fun slotOf(entity: SavedRecipeEntity): MealSlot? =
        MealSlot.entries.firstOrNull { it.name == entity.slot }

    fun styleOf(entity: SavedRecipeEntity): RecipeStyle =
        RecipeStyle.entries.firstOrNull { it.name == entity.style } ?: RecipeStyle.CLASSIC
}

data class SavedIngredient(
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
