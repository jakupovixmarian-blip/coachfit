package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.fitcoach.app.domain.nutrition.FoodAssetImporter
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.MealPortionSolver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Collaudo del catalogo alimentare mostrato al Coach AI.
 *
 * Il piano generato dall'AI usciva con pasti vuoti e "0 kcal" perche' il
 * catalogo inviato al modello non conteneva gli alimenti che il modello
 * sceglieva: le ~7.800 voci USDA in inglese occupavano 166 posizioni su 200 e
 * spingevano fuori verdura, frutta, cereali e olio. Da li' gli errori
 * "alimento 'Banane' non presente nel database" e "nessuna fonte di grassi".
 *
 * Questi test bloccano la regressione su tre fronti: lingua, completezza delle
 * categorie, e riconoscimento dei nomi scritti dal modello.
 */
class CatalogoItalianoCompletoCollaudoTest {

    private val usda = File("src/main/assets/food_usda_sr_legacy.tsv").bufferedReader().use {
        FoodAssetImporter.parseTsv(it, FoodAssetImporter.USDA_SOURCE)
    }

    /** Database come in produzione: seed curato + import USDA. */
    private val fullDatabase = FoodSeedData.foods + usda

    private fun catalog(liked: List<String> = emptyList()) =
        FoodCatalogSelector.forGeneration(
            fullDatabase,
            MarjanProfile.build().copy(likedFoods = liked)
        )

    @Test
    fun `il catalogo della generazione non contiene nomi inglesi`() {
        val english = catalog().filter { it.source == FoodAssetImporter.USDA_SOURCE }
        assertThat(english).isEmpty()

        val suspicious = catalog().map { it.name }.filter { name ->
            Regex("""\b[A-Z]{4,}""").containsMatchIn(name) ||
                name.contains(", raw") || name.contains("cooked")
        }
        assertThat(suspicious).isEmpty()
    }

    @Test
    fun `tutte le categorie nutrizionali sono rappresentate`() {
        val categories = catalog().map { it.category }.toSet()
        val required = setOf(
            "Carne", "Pesce", "Uova", "Latticini", "Legumi", "Cereali",
            "Verdura", "Frutta", "Frutta secca", "Grassi", "Tuberi", "Condimenti"
        )
        assertThat(categories).containsAtLeastElementsIn(required)
    }

    @Test
    fun `gli alimenti base che il modello sceglie sono sempre nel catalogo`() {
        val names = catalog().map { it.name }.toSet()
        val essentials = listOf(
            "Petto di pollo", "Uovo intero di gallina", "Merluzzo", "Yogurt greco 0% grassi",
            "Riso bianco (crudo)", "Pasta di semola (cruda)", "Fiocchi d'avena", "Patate",
            "Banana", "Mela", "Spinaci", "Broccoli", "Zucchine", "Pomodori",
            "Olio extravergine d'oliva", "Noci", "Mandorle", "Lenticchie cotte"
        )
        assertThat(names).containsAtLeastElementsIn(essentials)
    }

    @Test
    fun `esiste almeno una fonte di grassi pura per il solver`() {
        val fats = catalog().filter { MealPortionSolver.roleOf(it) == MealPortionSolver.Role.FAT }
        assertThat(fats).isNotEmpty()
        // L'olio e' la fonte di grassi che il piano usa per chiudere il target.
        assertThat(fats.map { it.name }).contains("Olio extravergine d'oliva")
    }

    @Test
    fun `le preferenze di gusto non svuotano il catalogo`() {
        val senzaGusti = catalog().map { it.name }.toSet()
        val conGusti = catalog(listOf("carne", "pesce", "uova")).map { it.name }.toSet()

        // Le preferenze cambiano l'ordine, non il contenuto: prima "carne" come
        // categoria trascinava dentro centinaia di derivati USDA e le verdure
        // sparivano dal catalogo.
        assertThat(conGusti).isEqualTo(senzaGusti)
        assertThat(conGusti).contains("Banana")
        assertThat(conGusti).contains("Olio extravergine d'oliva")
        assertThat(conGusti).contains("Spinaci")
    }

    @Test
    fun `i nomi scritti dal modello si risolvono sull'alimento giusto`() {
        val foods = catalog()
        // Nomi esattamente come comparivano nel payload reale che ha fallito.
        val attesi = mapOf(
            "Banane" to "Banana",
            "Spinaci" to "Spinaci",
            "Broccoli" to "Broccoli",
            "Riso" to "Riso bianco (crudo)",
            "Olio d'oliva" to "Olio extravergine d'oliva",
            "Uova" to "Uovo intero di gallina",
            "Petto di pollo" to "Petto di pollo",
            "Mela" to "Mela"
        )
        attesi.forEach { (scritto, atteso) ->
            val risolto = CatalogNameResolver.resolveFood(scritto, foods)
            assertThat(risolto?.name).isEqualTo(atteso)
        }
    }

    @Test
    fun `il catalogo resta entro il tetto di contesto`() {
        assertThat(catalog().size).isAtMost(FoodCatalogSelector.DEFAULT_LIMIT)
    }

    @Test
    fun `il catalogo curato e ampio e copre tutte le categorie`() {
        // Il catalogo non deve tornare a essere una lista di 50-100 voci: e'
        // quello che rendeva impossibile comporre pasti completi.
        assertThat(FoodSeedData.foods.size).isAtLeast(240)

        val perCategory = FoodSeedData.foods.groupingBy { it.category }.eachCount()
        val minime = mapOf(
            "Verdura" to 20, "Frutta" to 18, "Cereali" to 20, "Carne" to 15,
            "Pesce" to 20, "Latticini" to 18, "Legumi" to 12, "Frutta secca" to 12,
            "Grassi" to 6, "Salumi" to 6, "Uova" to 4
        )
        minime.forEach { (categoria, minimo) ->
            assertThat(perCategory[categoria] ?: 0).isAtLeast(minimo)
        }
    }

    @Test
    fun `ogni alimento ha kcal coerenti con i macro`() {
        // La validazione dei piani ammette il 20% di scarto fra kcal dichiarate e
        // 4/4/9: un alimento fuori tolleranza rende il piano impossibile da
        // chiudere. La tolleranza qui e' 25% per lasciare margine alla fibra.
        val incoerenti = FoodSeedData.foods.filter { food ->
            val calc = food.proteinPer100g * 4 + food.carbsPer100g * 4 + food.fatPer100g * 9
            food.kcalPer100g > 0 && kotlin.math.abs(calc - food.kcalPer100g) / food.kcalPer100g > 0.25
        }
        assertThat(incoerenti.map { it.name }).isEmpty()
    }
}
