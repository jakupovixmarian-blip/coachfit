package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt

/** Ricetta concreta per un pasto del piano. */
data class Recipe(
    val id: String,
    val name: String,
    val slot: MealSlot,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String> = emptyList(),
    val notes: String? = null
) {
    val kcal: Double get() = ingredients.sumOf { it.kcal }
    val protein: Double get() = ingredients.sumOf { it.protein }
    val carbs: Double get() = ingredients.sumOf { it.carbs }
    val fat: Double get() = ingredients.sumOf { it.fat }
}

data class RecipeIngredient(
    val foodName: String,
    val grams: Double,
    val unit: FoodUnit,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * Genera ricette a partire dagli alimenti che il piano assegna a un pasto.
 *
 * Le ricette sono definite su nomi di alimento del database, non su ingredienti
 * arbitrari: una ricetta compare solo se gli alimenti che le servono sono
 * davvero presenti in quel pasto. Cosi non viene mai proposta una ricetta con
 * ingredienti che l'utente non deve mangiare, e le grammature restano quelle
 * calcolate dal motore nutrizionale.
 */
object RecipeEngine {

    private const val MAX_INGREDIENTS = 5

    /** Quota minima di alimenti della ricetta presenti nel pasto. */
    private const val MIN_COVERAGE = 0.5

    fun forMeal(
        meal: Meal,
        profile: UserProfile? = null,
        foods: List<FoodItem> = emptyList()
    ): List<Recipe> {
        if (meal.entries.isEmpty()) return emptyList()
        val allowed = meal.entries.filterNot { isExcluded(it.foodName, profile) }
        if (allowed.isEmpty()) return emptyList()

        val available = allowed.associateBy { FoodItem.normalizeName(it.foodName) }
        val byName = foods.associateBy { it.normalizedName }

        return templates
            .filter { it.slot == meal.slot }
            .mapNotNull { template ->
                // Se un ingrediente della ricetta e escluso per allergia o
                // preferenza, la ricetta non si propone affatto: una versione
                // "ridotta" la farebbe sembrare conforme al piano quando non lo e.
                if (template.requiredFoods.any { isExcluded(it, profile) }) return@mapNotNull null
                val matched = template.requiredFoods.mapNotNull { required ->
                    available[FoodItem.normalizeName(required)]
                }
                if (matched.isEmpty()) return@mapNotNull null
                val coverage = matched.size.toDouble() / template.requiredFoods.size
                if (coverage < MIN_COVERAGE) return@mapNotNull null

                val ingredients = matched.take(MAX_INGREDIENTS).map { entry ->
                    RecipeIngredient(
                        foodName = entry.foodName,
                        grams = entry.grams,
                        unit = byName[FoodItem.normalizeName(entry.foodName)]?.defaultUnit ?: entry.unit,
                        kcal = entry.kcal,
                        protein = entry.protein,
                        carbs = entry.carbs,
                        fat = entry.fat
                    )
                }
                template.toRecipe(ingredients, coverage)
            }
            .sortedByDescending { it.kcal }
    }

    /** Ricette per tutti i pasti di una giornata, solo per gli slot che ne hanno. */
    fun forDay(
        meals: List<Meal>,
        profile: UserProfile? = null,
        foods: List<FoodItem> = emptyList()
    ): Map<MealSlot, List<Recipe>> =
        meals.associate { it.slot to forMeal(it, profile, foods) }
            .filterValues { it.isNotEmpty() }

    /**
     * Scarta un alimento se compare tra esclusi o allergie. Il confronto e per
     * sottostringa normalizzata: "latte" esclude anche "latte parzialmente
     * scremato", che e il caso reale di un'intolleranza.
     */
    internal fun isExcluded(foodName: String, profile: UserProfile?): Boolean {
        if (profile == null) return false
        val target = FoodItem.normalizeName(foodName)
        val banned = (profile.excludedFoods + profile.allergies).map { FoodItem.normalizeName(it) }
        return banned.any { it.isNotBlank() && target.contains(it) }
    }

    private fun Template.toRecipe(ingredients: List<RecipeIngredient>, coverage: Double): Recipe {
        val partial = if (coverage < 1.0) {
            "Copre ${(coverage * 100).roundToInt()}% degli alimenti previsti per questo pasto."
        } else {
            null
        }
        return Recipe(
            id = id,
            name = name,
            slot = slot,
            ingredients = ingredients,
            steps = steps,
            notes = listOfNotNull(notes, partial).joinToString(" ").ifBlank { null }
        )
    }

    private data class Template(
        val id: String,
        val name: String,
        val slot: MealSlot,
        /** Nomi che devono corrispondere agli alimenti del pasto. */
        val requiredFoods: List<String>,
        val steps: List<String> = emptyList(),
        val notes: String? = null
    )

    /**
     * I nomi devono corrispondere esattamente a FoodSeedData: un nome errato non
     * produce errori, semplicemente la ricetta non viene mai proposta.
     */
    private val templates: List<Template> = listOf(
        // ---------------- COLAZIONE ----------------
        Template(
            id = "porridge_avena_whey",
            name = "Porridge di avena con whey",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Proteine del siero (whey, polvere)"),
            steps = listOf(
                "Cuoci i fiocchi d'avena con acqua o latte per 4-5 minuti, mescolando.",
                "Togli dal fuoco e aggiungi le whey quando il composto e tiepido.",
                "Se previsto, completa con la frutta del pasto."
            ),
            notes = "Le whey a caldo diventano grumose: aspettare che il porridge si intiepidisca."
        ),
        Template(
            id = "yogurt_greco_avena",
            name = "Yogurt greco con avena e frutta",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Fiocchi d'avena"),
            steps = listOf(
                "Versa lo yogurt in una ciotola.",
                "Aggiungi i fiocchi d'avena e lascia riposare 5 minuti.",
                "Completa con la frutta prevista dal pasto."
            )
        ),
        Template(
            id = "uova_pane",
            name = "Uova strapazzate con pane tostato",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Uovo intero di gallina", "Pane integrale"),
            steps = listOf(
                "Sbatti le uova con un pizzico di sale.",
                "Cuoci a fuoco basso mescolando, finche non rapprendono.",
                "Tosta il pane e servi subito."
            )
        ),
        Template(
            id = "fette_biscottate_ricotta",
            name = "Fette biscottate con ricotta",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fette biscottate", "Ricotta vaccina"),
            steps = listOf("Spalma la ricotta sulle fette biscottate.")
        ),
        Template(
            id = "pancake_avena_uova",
            name = "Pancake proteici di avena",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Uovo intero di gallina", "Albume d'uovo"),
            steps = listOf(
                "Frulla avena, uova e albumi fino a ottenere una pastella.",
                "Cuoci in padella antiaderente a fuoco medio, 2 minuti per lato."
            )
        ),

        // ---------------- SPUNTINI ----------------
        Template(
            id = "yogurt_frutta_secca",
            name = "Yogurt con frutta secca",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Mandorle"),
            steps = listOf("Unisci lo yogurt e le mandorle, meglio se tritate.")
        ),
        Template(
            id = "frutta_mandorle",
            name = "Frutta fresca con mandorle",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Mela", "Mandorle"),
            steps = listOf("Taglia la frutta e accompagnala con le mandorle.")
        ),
        Template(
            id = "bresaola_rucola",
            name = "Bresaola con rucola",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Bresaola", "Rucola"),
            steps = listOf("Disponi la bresaola sul letto di rucola, con l'olio previsto.")
        ),
        Template(
            id = "shake_whey_banana",
            name = "Frullato di whey e banana",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Proteine del siero (whey, polvere)", "Banana"),
            steps = listOf("Frulla la banana con le whey e acqua o ghiaccio.", "Consumare subito.")
        ),
        Template(
            id = "cottage_pomodori",
            name = "Fiocchi di latte con pomodori",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Fiocchi di latte (cottage)", "Pomodori"),
            steps = listOf("Unisci i fiocchi di latte ai pomodori a cubetti.")
        ),
        Template(
            id = "pane_bresaola",
            name = "Panino con bresaola",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Pane integrale", "Bresaola"),
            steps = listOf("Farcisci il pane con la bresaola.")
        ),

        // ---------------- PRANZO ----------------
        Template(
            id = "pollo_riso_verdure",
            name = "Pollo alla piastra con riso e verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Petto di pollo", "Riso basmati (crudo)", "Zucchine"),
            steps = listOf(
                "Cuoci il riso secondo i tempi indicati.",
                "Griglia il pollo a fette spesse, 3-4 minuti per lato.",
                "Salta le zucchine in padella con l'olio previsto dal pasto."
            )
        ),
        Template(
            id = "pasta_pollo_pomodoro",
            name = "Pasta al pomodoro con pollo",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta di semola (cruda)", "Petto di pollo", "Passata di pomodoro"),
            steps = listOf(
                "Cuoci la pasta al dente.",
                "Rosola il pollo a cubetti, aggiungi la passata e cuoci 10 minuti.",
                "Manteca la pasta nel sugo."
            )
        ),
        Template(
            id = "merluzzo_patate",
            name = "Merluzzo al forno con patate",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Merluzzo", "Patate", "Olio extravergine d'oliva"),
            steps = listOf(
                "Taglia le patate a spicchi e cuocile in forno a 200 gradi per 20 minuti.",
                "Aggiungi il merluzzo e prosegui 12-15 minuti.",
                "Condisci con l'olio previsto a crudo."
            )
        ),
        Template(
            id = "riso_uova_verdure",
            name = "Riso con uova e verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Riso bianco (crudo)", "Uovo intero di gallina", "Broccoli"),
            steps = listOf(
                "Cuoci il riso e sbollenta i broccoli.",
                "Salta il riso in padella con le uova sbattute.",
                "Unisci i broccoli e servi."
            )
        ),
        Template(
            id = "insalata_tonno",
            name = "Insalata di tonno e cannellini",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Tonno in scatola al naturale", "Fagioli cannellini cotti", "Insalata lattuga"),
            steps = listOf(
                "Scola tonno e cannellini.",
                "Unisci alla lattuga e condisci con l'olio previsto."
            )
        ),
        Template(
            id = "tacchino_cous_cous",
            name = "Tacchino con cous cous e peperoni",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Fesa di tacchino", "Cous cous (crudo)", "Peperoni"),
            steps = listOf(
                "Reidrata il cous cous con acqua bollente.",
                "Griglia il tacchino e salta i peperoni.",
                "Unisci tutto in una ciotola."
            )
        ),

        // ---------------- CENA ----------------
        Template(
            id = "salmone_riso_broccoli",
            name = "Salmone al forno con riso e broccoli",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Salmone", "Riso basmati (crudo)", "Broccoli"),
            steps = listOf(
                "Inforna il salmone a 190 gradi per 12-15 minuti.",
                "Cuoci il riso e sbollenta i broccoli.",
                "Servi con l'olio previsto a crudo."
            )
        ),
        Template(
            id = "frittata_spinaci",
            name = "Frittata con spinaci",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Uovo intero di gallina", "Spinaci"),
            steps = listOf(
                "Sbatti le uova e unisci gli spinaci gia saltati.",
                "Cuoci in padella a fuoco medio, girando a meta cottura."
            )
        ),
        Template(
            id = "manzo_insalata",
            name = "Manzo alla griglia con insalata",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Fesa di manzo", "Insalata lattuga", "Olio extravergine d'oliva"),
            steps = listOf(
                "Griglia la fesa 3 minuti per lato.",
                "Servi con l'insalata e l'olio previsto."
            ),
            notes = "Lasciare riposare la carne 2 minuti prima di tagliarla mantiene i succhi."
        ),
        Template(
            id = "orata_verdure",
            name = "Orata al forno con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Orata", "Zucchine", "Pomodori"),
            steps = listOf(
                "Disponi le verdure a fette in teglia.",
                "Adagia l'orata sopra e inforna a 200 gradi per 20 minuti."
            )
        ),
        Template(
            id = "tofu_verdure_riso",
            name = "Tofu saltato con verdure e riso",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Tofu", "Riso integrale (crudo)", "Broccoli"),
            steps = listOf(
                "Cuoci il riso integrale.",
                "Salta il tofu a cubetti finche non dorato.",
                "Unisci i broccoli e prosegui 5 minuti."
            )
        ),
        Template(
            id = "pollo_patate_forno",
            name = "Pollo e patate al forno",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Petto di pollo", "Patate", "Olio extravergine d'oliva"),
            steps = listOf(
                "Taglia patate e pollo a pezzi.",
                "Inforna a 200 gradi per 30 minuti, girando a meta cottura."
            )
        )
    )
}
