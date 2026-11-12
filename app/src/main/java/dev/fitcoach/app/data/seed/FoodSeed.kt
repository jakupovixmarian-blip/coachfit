package dev.fitcoach.app.data.seed

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState

/**
 * Starter food catalog with *reference* values per 100 g taken from standard
 * food-composition tables. These are editable by the user, and are clearly
 * labelled as reference entries, NOT as verified database records. For verified
 * data the app queries Open Food Facts (live API, no key required).
 *
 * IMPORTANT: values are approximate averages and must be corrected by the user
 * when a package label is available.
 */
object FoodSeed {

    private fun food(
        name: String,
        category: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double? = null,
        state: FoodState = FoodState.RAW
    ) = Food(
        name = name,
        category = category,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        fiberPer100g = fiber,
        source = FoodSource.MANUAL,
        state = state,
        isCustom = false
    )

    fun all(): List<Food> = listOf(
        // Fonti proteiche
        food("Petto di pollo", "Carne", 165.0, 31.0, 0.0, 3.6, state = FoodState.COOKED),
        food("Petto di tacchino", "Carne", 135.0, 29.0, 0.0, 1.8, state = FoodState.COOKED),
        food("Manzo magro", "Carne", 187.0, 26.0, 0.0, 9.0, state = FoodState.COOKED),
        food("Merluzzo", "Pesce", 82.0, 18.0, 0.0, 0.7, state = FoodState.COOKED),
        food("Salmone", "Pesce", 208.0, 20.0, 0.0, 13.0, state = FoodState.COOKED),
        food("Tonno al naturale sgocciolato", "Pesce", 116.0, 26.0, 0.0, 1.0, state = FoodState.DRAINED),
        food("Uova intere", "Uova", 143.0, 12.6, 0.7, 9.5),
        food("Albume d'uovo", "Uova", 52.0, 11.0, 0.7, 0.2),
        food("Whey protein in polvere", "Integratori", 380.0, 78.0, 7.0, 5.0, state = FoodState.COMMERCIAL),
        food("Ricotta vaccina", "Latticini", 146.0, 8.8, 3.0, 10.9),
        food("Yogurt greco 0%", "Latticini", 57.0, 10.0, 3.6, 0.4, state = FoodState.COMMERCIAL),
        food("Fiocchi di latte (cottage)", "Latticini", 98.0, 11.0, 3.4, 4.3),

        // Carboidrati
        food("Riso bianco", "Cereali", 130.0, 2.7, 28.0, 0.3, state = FoodState.COOKED),
        food("Pasta di semola", "Cereali", 158.0, 5.8, 31.0, 0.9, state = FoodState.COOKED),
        food("Pane integrale", "Cereali", 247.0, 13.0, 41.0, 3.4, fiber = 7.0),
        food("Avena in fiocchi", "Cereali", 389.0, 16.9, 66.3, 6.9, fiber = 10.6),
        food("Patate", "Tuberi", 77.0, 2.0, 17.0, 0.1, state = FoodState.COOKED),
        food("Patate dolci", "Tuberi", 86.0, 1.6, 20.1, 0.1, state = FoodState.COOKED),
        food("Quinoa", "Cereali", 120.0, 4.4, 21.3, 1.9, state = FoodState.COOKED),
        food("Cous cous", "Cereali", 112.0, 3.8, 23.2, 0.2, state = FoodState.COOKED),

        // Legumi e vegetali
        food("Lenticchie cotte", "Legumi", 116.0, 9.0, 20.0, 0.4, fiber = 7.9, state = FoodState.COOKED),
        food("Ceci cotti", "Legumi", 164.0, 8.9, 27.4, 2.6, fiber = 7.6, state = FoodState.COOKED),
        food("Fagioli borlotti cotti", "Legumi", 127.0, 8.7, 22.8, 0.5, fiber = 6.4, state = FoodState.COOKED),
        food("Broccoli", "Verdura", 34.0, 2.8, 6.6, 0.4, fiber = 2.6, state = FoodState.COOKED),
        food("Spinaci", "Verdura", 23.0, 2.9, 3.6, 0.4, fiber = 2.2, state = FoodState.COOKED),
        food("Zucchine", "Verdura", 17.0, 1.2, 3.1, 0.3, fiber = 1.0, state = FoodState.COOKED),
        food("Insalata mista", "Verdura", 15.0, 1.4, 2.9, 0.2, fiber = 1.5),
        food("Pomodori", "Verdura", 18.0, 0.9, 3.9, 0.2, fiber = 1.2),

        // Grassi
        food("Olio extravergine d'oliva", "Condimenti", 884.0, 0.0, 0.0, 100.0),
        food("Burro d'arachidi", "Condimenti", 588.0, 25.0, 20.0, 50.0, fiber = 6.0, state = FoodState.COMMERCIAL),
        food("Mandorle", "Frutta secca", 579.0, 21.2, 21.6, 49.9, fiber = 12.5),
        food("Noci", "Frutta secca", 654.0, 15.2, 13.7, 65.2, fiber = 6.7),
        food("Avocado", "Frutta", 160.0, 2.0, 8.5, 14.7, fiber = 6.7),

        // Frutta
        food("Mela", "Frutta", 52.0, 0.3, 13.8, 0.2, fiber = 2.4),
        food("Banana", "Frutta", 89.0, 1.1, 22.8, 0.3, fiber = 2.6),
        food("Fragole", "Frutta", 32.0, 0.7, 7.7, 0.3, fiber = 2.0),
        food("Arance", "Frutta", 47.0, 0.9, 11.8, 0.1, fiber = 2.4),
        food("Mirtilli", "Frutta", 57.0, 0.7, 14.5, 0.3, fiber = 2.4),

        // Altro
        food("Passata di pomodoro", "Condimenti", 29.0, 1.6, 5.3, 0.2, state = FoodState.COMMERCIAL),
        food("Miele", "Dolcificanti", 304.0, 0.3, 82.4, 0.0),
        food("Cioccolato fondente 85%", "Dolci", 598.0, 9.0, 22.0, 46.0, state = FoodState.COMMERCIAL)
    )
}
