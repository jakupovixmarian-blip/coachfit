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
    /** Quota degli ingredienti della ricetta presenti nel pasto (0..1). */
    val coverage: Double = 1.0,
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
            // Prima le ricette che usano tutti gli ingredienti previsti, poi le
            // piu caloriche: cosi la proposta in cima e quella che aderisce
            // meglio al pasto.
            .sortedWith(compareByDescending<Recipe> { it.coverage }.thenByDescending { it.kcal })
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
            coverage = coverage,
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


    // ---------------------------------------------------------------------
    // COLAZIONE
    // ---------------------------------------------------------------------
    private val breakfastRecipes: List<Template> = listOf(
        Template(
            id = "porridge_avena_whey",
            name = "Porridge di avena con whey",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Proteine del siero (whey, polvere)"),
            steps = listOf(
                "Cuoci i fiocchi d'avena con acqua per 4-5 minuti, mescolando.",
                "Togli dal fuoco e aggiungi le whey quando il composto e tiepido.",
                "Completa con la frutta prevista dal pasto."
            ),
            notes = "Le whey a caldo diventano grumose: aspettare che il porridge si intiepidisca."
        ),
        Template(
            id = "porridge_avena_banana",
            name = "Porridge di avena e banana",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Banana"),
            steps = listOf(
                "Cuoci l'avena con acqua o latte per 5 minuti.",
                "Schiaccia mezza banana nel porridge e usa l'altra a fette sopra."
            )
        ),
        Template(
            id = "porridge_avena_datteri",
            name = "Porridge con datteri e mandorle",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Datteri secchi", "Mandorle"),
            steps = listOf(
                "Cuoci l'avena con acqua per 5 minuti.",
                "Taglia i datteri a pezzetti e uniscili a fine cottura.",
                "Completa con le mandorle tritate."
            )
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
            id = "yogurt_mirtilli_mandorle",
            name = "Yogurt con mirtilli e mandorle",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Mirtilli", "Mandorle"),
            steps = listOf("Unisci yogurt, mirtilli e mandorle in una ciotola.")
        ),
        Template(
            id = "yogurt_greco_cacao",
            name = "Yogurt greco con cioccolato fondente",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Cioccolato fondente 85%"),
            steps = listOf(
                "Grattugia il cioccolato fondente sullo yogurt.",
                "Mescola e lascia riposare un minuto prima di mangiare."
            )
        ),
        Template(
            id = "yogurt_kiwi_chia",
            name = "Yogurt con kiwi e semi di chia",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Kiwi", "Semi di chia"),
            steps = listOf(
                "Taglia il kiwi a cubetti.",
                "Unisci allo yogurt e completa con i semi di chia."
            )
        ),
        Template(
            id = "yogurt_banana_arachidi",
            name = "Yogurt con banana e burro d'arachidi",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Yogurt greco 2% grassi", "Banana", "Burro d'arachidi (100% arachidi)"),
            steps = listOf(
                "Taglia la banana a rondelle.",
                "Unisci allo yogurt con un cucchiaio di burro d'arachidi."
            )
        ),
        Template(
            id = "muesli_yogurt",
            name = "Muesli con yogurt",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Muesli senza zuccheri aggiunti", "Yogurt greco 2% grassi"),
            steps = listOf("Versa il muesli nello yogurt e lascia ammorbidire 2 minuti.")
        ),
        Template(
            id = "cornflakes_latte",
            name = "Cornflakes con latte",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Cornflakes (senza zuccheri aggiunti)", "Latte parzialmente scremato"),
            steps = listOf("Versa il latte freddo sui cornflakes e consuma subito.")
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
            id = "toast_avocado_uovo",
            name = "Toast con avocado e uovo",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Pane integrale", "Avocado", "Uovo intero di gallina"),
            steps = listOf(
                "Tosta il pane.",
                "Schiaccia l'avocado sul pane con sale e pepe.",
                "Cuoci l'uovo in padella e appoggialo sopra."
            )
        ),
        Template(
            id = "frittata_pane",
            name = "Frittata con pane",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Frittata (uova intere cotte)", "Pane integrale"),
            steps = listOf("Scalda la frittata in padella e accompagnala con il pane tostato.")
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
        Template(
            id = "fette_biscottate_ricotta",
            name = "Fette biscottate con ricotta",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fette biscottate", "Ricotta vaccina"),
            steps = listOf("Spalma la ricotta sulle fette biscottate.")
        ),
        Template(
            id = "ricotta_miele_noci",
            name = "Ricotta con miele e noci",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Ricotta vaccina", "Miele", "Noci"),
            steps = listOf(
                "Disponi la ricotta in una ciotola.",
                "Aggiungi il miele e le noci spezzettate."
            )
        ),
        Template(
            id = "pane_miele_ricotta",
            name = "Pane di segale con ricotta e miele",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Pane di segale", "Ricotta vaccina", "Miele"),
            steps = listOf("Spalma la ricotta sul pane e completa con il miele.")
        ),
        Template(
            id = "pane_segale_stracchino",
            name = "Pane di segale con stracchino",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Pane di segale", "Stracchino"),
            steps = listOf("Spalma lo stracchino sul pane di segale.")
        ),
        Template(
            id = "gallette_bresaola",
            name = "Gallette con bresaola",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Gallette di riso", "Bresaola"),
            steps = listOf("Disponi la bresaola sulle gallette.")
        ),
        Template(
            id = "pane_pomodoro_prosciutto",
            name = "Pane con pomodoro e prosciutto crudo",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Pane integrale", "Pomodori", "Prosciutto crudo sgrassato"),
            steps = listOf(
                "Tosta il pane.",
                "Appoggia le fette di pomodoro e il prosciutto."
            )
        ),
        Template(
            id = "pane_grana_pomodoro",
            name = "Pane con grana e pomodoro",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Pane integrale", "Formaggio grana padano", "Pomodori"),
            steps = listOf(
                "Tosta il pane.",
                "Aggiungi il grana a scaglie e il pomodoro a fette."
            )
        ),
        Template(
            id = "pane_ricotta_pomodori",
            name = "Pane con ricotta e pomodorini",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Pane bianco", "Ricotta vaccina", "Pomodori"),
            steps = listOf("Spalma la ricotta sul pane e aggiungi il pomodoro a fette.")
        ),
        Template(
            id = "smoothie_whey_fragole",
            name = "Smoothie di whey e fragole",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Proteine del siero (whey, polvere)", "Fragole"),
            steps = listOf(
                "Frulla le fragole con le whey e qualche cubetto di ghiaccio.",
                "Bevi subito, senza filtrare."
            )
        ),
        Template(
            id = "frullato_vegetale_banana",
            name = "Frullato vegetale con banana",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Proteine vegetali (polvere)", "Banana", "Latte di mandorla non zuccherato"),
            steps = listOf(
                "Frulla banana, proteine vegetali e latte di mandorla.",
                "Regola la densita con acqua o ghiaccio."
            )
        ),
        Template(
            id = "avena_soia_cacao",
            name = "Porridge di avena con bevanda di soia",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Bevanda di soia non zuccherata"),
            steps = listOf(
                "Cuoci l'avena nella bevanda di soia per 5 minuti.",
                "Mescola finche non diventa cremoso."
            )
        ),
        Template(
            id = "avena_latte_avena",
            name = "Porridge con latte d'avena e noci",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Latte d'avena", "Noci"),
            steps = listOf(
                "Cuoci l'avena nel latte d'avena.",
                "Completa con le noci spezzettate."
            )
        ),
        Template(
            id = "omelette_albume_funghi",
            name = "Omelette di albumi con funghi",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Albume d'uovo", "Funghi champignon"),
            steps = listOf(
                "Salta i funghi in padella finche non perdono l'acqua.",
                "Versa gli albumi sbattuti e cuoci a fuoco medio."
            )
        ),
        Template(
            id = "yogurt_mela_arachidi",
            name = "Yogurt con mela e burro d'arachidi",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Mela", "Burro d'arachidi (100% arachidi)"),
            steps = listOf(
                "Taglia la mela a cubetti.",
                "Unisci allo yogurt con il burro d'arachidi."
            )
        ),
        Template(
            id = "fiocchi_latte_miele",
            name = "Fiocchi di latte con miele",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi di latte (cottage)", "Miele"),
            steps = listOf("Unisci i fiocchi di latte al miele e mescola.")
        ),
        Template(
            id = "cottage_pere_noci",
            name = "Fiocchi di latte con pera e noci",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi di latte (light)", "Pera", "Noci"),
            steps = listOf(
                "Taglia la pera a cubetti.",
                "Unisci ai fiocchi di latte e completa con le noci."
            )
        ),
        Template(
            id = "porridge_whey_noci",
            name = "Porridge con whey e noci",
            slot = MealSlot.BREAKFAST,
            requiredFoods = listOf("Fiocchi d'avena", "Proteine del siero (whey, polvere)", "Noci"),
            steps = listOf(
                "Cuoci l'avena con acqua.",
                "Fuori dal fuoco aggiungi le whey.",
                "Completa con le noci."
            )
        )
    )

    // ---------------------------- SPUNTINI ----------------------------
    private val snackRecipes: List<Template> = listOf(
        Template(
            id = "yogurt_frutta_secca",
            name = "Yogurt con frutta secca",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Mandorle"),
            steps = listOf("Unisci lo yogurt e le mandorle, meglio se tritate."),
        ),
        Template(
            id = "yogurt_nocciole",
            name = "Yogurt con nocciole",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 2% grassi", "Nocciole"),
            steps = listOf("Trita le nocciole e uniscile allo yogurt."),
        ),
        Template(
            id = "yogurt_arance",
            name = "Yogurt con spicchi d'arancia",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt bianco magro", "Arance"),
            steps = listOf("Sbuccia l'arancia e unisci gli spicchi allo yogurt."),
        ),
        Template(
            id = "yogurt_mandarini",
            name = "Yogurt con mandarini",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt bianco magro", "Mandarini"),
            steps = listOf("Sbuccia i mandarini e dividili a spicchi sopra lo yogurt."),
        ),
        Template(
            id = "yogurt_anguria",
            name = "Yogurt con anguria",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Anguria"),
            steps = listOf("Taglia l'anguria a cubetti e uniscila allo yogurt."),
        ),
        Template(
            id = "yogurt_kiwi_mandorle",
            name = "Yogurt con kiwi e mandorle",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Kiwi", "Mandorle"),
            steps = listOf("Taglia il kiwi e completa lo yogurt con le mandorle."),
        ),
        Template(
            id = "yogurt_pera_noci",
            name = "Yogurt con pera e noci",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 2% grassi", "Pera", "Noci"),
            steps = listOf("Taglia la pera a cubetti e aggiungi le noci spezzettate."),
        ),
        Template(
            id = "banana_mandorle",
            name = "Banana con mandorle",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Banana", "Mandorle"),
            steps = listOf("Accompagna la banana con una manciata di mandorle."),
        ),
        Template(
            id = "mela_noci",
            name = "Mela con noci",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Mela", "Noci"),
            steps = listOf("Taglia la mela a spicchi e accompagnala con le noci."),
        ),
        Template(
            id = "pere_mandorle",
            name = "Pera con mandorle",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Pera", "Mandorle"),
            steps = listOf("Taglia la pera e accompagnala con le mandorle."),
        ),
        Template(
            id = "arance_nocciole",
            name = "Arance con nocciole",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Arance", "Nocciole"),
            steps = listOf("Sbuccia le arance e accompagnala con le nocciole."),
        ),
        Template(
            id = "gallette_miele",
            name = "Gallette con miele",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Gallette di riso", "Miele"),
            steps = listOf("Spalma un velo di miele sulle gallette."),
        ),
        Template(
            id = "gallette_confettura",
            name = "Gallette con confettura",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Gallette di riso", "Confettura di frutta (senza zuccheri aggiunti)"),
            steps = listOf("Spalma la confettura sulle gallette."),
        ),
        Template(
            id = "cottage_fragole",
            name = "Fiocchi di latte con fragole",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Fiocchi di latte (cottage)", "Fragole"),
            steps = listOf("Taglia le fragole e uniscile ai fiocchi di latte."),
        ),
        Template(
            id = "ricotta_pere",
            name = "Ricotta con pera",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Ricotta vaccina", "Pera"),
            steps = listOf("Taglia la pera a cubetti e uniscila alla ricotta."),
        ),
        Template(
            id = "latte_miele",
            name = "Latte caldo con miele",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Latte parzialmente scremato", "Miele"),
            steps = listOf("Scalda il latte e aggiungi il miele fuori dal fuoco."),
        ),
        Template(
            id = "latte_mandorla_caffe",
            name = "Caffè con latte di mandorla",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Caffè espresso (senza zucchero)", "Latte di mandorla non zuccherato"),
            steps = listOf("Allunga il caffè con il latte di mandorla caldo."),
        ),
        Template(
            id = "succo_arancia_noci",
            name = "Succo d'arancia con noci",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Succo d'arancia (100%)", "Noci"),
            steps = listOf("Accompagna il succo con le noci."),
        ),
        Template(
            id = "frutta_mandorle",
            name = "Frutta fresca con mandorle",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Mela", "Mandorle"),
            steps = listOf("Taglia la frutta e accompagnala con le mandorle."),
        ),
        Template(
            id = "mela_arachidi",
            name = "Mela con burro d'arachidi",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Mela", "Burro d'arachidi (100% arachidi)"),
            steps = listOf("Taglia la mela a spicchi e spalmaci il burro d'arachidi."),
        ),
        Template(
            id = "banana_noci",
            name = "Banana con noci",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Banana", "Noci"),
            steps = listOf("Spezza le noci e accompagnala con la banana."),
        ),
        Template(
            id = "yogurt_fragole",
            name = "Yogurt con fragole",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Fragole"),
            steps = listOf("Taglia le fragole a pezzi e uniscile allo yogurt."),
        ),
        Template(
            id = "yogurt_lamponi_chia",
            name = "Yogurt con lamponi e semi di chia",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Lamponi", "Semi di chia"),
            steps = listOf("Unisci i lamponi allo yogurt e completa con i semi di chia."),
        ),
        Template(
            id = "yogurt_uva",
            name = "Yogurt con uva",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Uva"),
            steps = listOf("Unisci gli acini d'uva tagliati a meta allo yogurt."),
        ),
        Template(
            id = "yogurt_melone",
            name = "Yogurt con melone",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt bianco magro", "Melone"),
            steps = listOf("Taglia il melone a cubetti e uniscilo allo yogurt."),
        ),
        Template(
            id = "yogurt_pesche",
            name = "Yogurt con pesche",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt greco 0% grassi", "Pesche"),
            steps = listOf("Taglia le pesche a fette e uniscile allo yogurt."),
        ),
        Template(
            id = "yogurt_albicocche",
            name = "Yogurt con albicocche",
            slot = MealSlot.MORNING_SNACK,
            requiredFoods = listOf("Yogurt bianco magro", "Albicocche"),
            steps = listOf("Taglia le albicocche e uniscile allo yogurt."),
        ),
        Template(
            id = "yogurt_ananas",
            name = "Yogurt con ananas",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Yogurt greco 2% grassi", "Ananas"),
            steps = listOf("Taglia l'ananas a cubetti e uniscilo allo yogurt."),
        ),
        Template(
            id = "shake_whey_banana",
            name = "Frullato di whey e banana",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Proteine del siero (whey, polvere)", "Banana"),
            steps = listOf("Frulla la banana con le whey e acqua o ghiaccio.", "Consumare subito."),
        ),
        Template(
            id = "shake_whey_latte",
            name = "Shake di whey con latte",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Proteine del siero (whey, polvere)", "Latte scremato"),
            steps = listOf("Sciogli le whey nel latte freddo, mescolando con una forchetta."),
        ),
        Template(
            id = "shake_whey_mirtilli",
            name = "Shake di whey e mirtilli",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Proteine del siero (whey, polvere)", "Mirtilli"),
            steps = listOf("Frulla i mirtilli con le whey e acqua."),
        ),
        Template(
            id = "bresaola_rucola",
            name = "Bresaola con rucola",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Bresaola", "Rucola"),
            steps = listOf("Disponi la bresaola sul letto di rucola, con l'olio previsto."),
        ),
        Template(
            id = "bresaola_grana",
            name = "Bresaola con grana",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Bresaola", "Formaggio grana padano"),
            steps = listOf("Disponi la bresaola e completa con il grana a scaglie."),
        ),
        Template(
            id = "cottage_pomodori",
            name = "Fiocchi di latte con pomodori",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Fiocchi di latte (cottage)", "Pomodori"),
            steps = listOf("Unisci i fiocchi di latte ai pomodori a cubetti."),
        ),
        Template(
            id = "cottage_cetrioli",
            name = "Fiocchi di latte con cetrioli",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Fiocchi di latte (light)", "Cetrioli"),
            steps = listOf("Taglia i cetrioli a rondelle e uniscili ai fiocchi di latte."),
        ),
        Template(
            id = "pane_bresaola",
            name = "Panino con bresaola",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Pane integrale", "Bresaola"),
            steps = listOf("Farcisci il pane con la bresaola."),
        ),
        Template(
            id = "pane_prosciutto",
            name = "Panino con prosciutto cotto",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Pane bianco", "Prosciutto cotto (sgrassato)"),
            steps = listOf("Farcisci il pane con il prosciutto cotto."),
        ),
        Template(
            id = "pane_tacchino",
            name = "Panino con fesa di tacchino",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Pane di segale", "Fesa di tacchino a fette"),
            steps = listOf("Farcisci il pane di segale con la fesa di tacchino."),
        ),
        Template(
            id = "gallette_cottage",
            name = "Gallette con fiocchi di latte",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Gallette di riso", "Fiocchi di latte (cottage)"),
            steps = listOf("Spalma i fiocchi di latte sulle gallette."),
        ),
        Template(
            id = "gallette_avocado",
            name = "Gallette con avocado",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Gallette di riso", "Avocado"),
            steps = listOf("Schiaccia l'avocado sulle gallette con sale e limone."),
        ),
        Template(
            id = "crackers_tonno",
            name = "Crackers con tonno",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Crackers salati", "Tonno in scatola al naturale"),
            steps = listOf("Scola il tonno e distribuiscilo sui crackers."),
        ),
        Template(
            id = "mela_arachidi_snack",
            name = "Mela con arachidi",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Mela", "Arachidi"),
            steps = listOf("Accompagna la mela con le arachidi."),
        ),
        Template(
            id = "yogurt_miele_noci",
            name = "Yogurt con miele e noci",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Yogurt bianco magro", "Miele", "Noci"),
            steps = listOf("Unisci il miele allo yogurt e completa con le noci."),
        ),
        Template(
            id = "ricotta_miele",
            name = "Ricotta con miele",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Ricotta di pecora", "Miele"),
            steps = listOf("Disponi la ricotta in una ciotola e aggiungi il miele."),
        ),
        Template(
            id = "proteine_vegetali_mela",
            name = "Frullato di proteine vegetali e mela",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Proteine vegetali (polvere)", "Mela"),
            steps = listOf("Frulla la mela con le proteine vegetali e acqua."),
        ),
        Template(
            id = "uova_sode",
            name = "Uova sode",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Uovo intero di gallina"),
            steps = listOf("Cuoci le uova in acqua bollente per 8 minuti.", "Raffreddale subito in acqua fredda."),
        ),
        Template(
            id = "edamame_snack",
            name = "Edamame lessati",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Edamame (cotti)"),
            steps = listOf("Lessate gli edamame e salali leggermente."),
        ),
        Template(
            id = "parmigiano_pomodori",
            name = "Parmigiano con pomodorini",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Parmigiano reggiano", "Pomodori"),
            steps = listOf("Taglia il parmigiano a scaglie e servilo con i pomodori."),
        ),
        Template(
            id = "prosciutto_melone",
            name = "Prosciutto crudo e melone",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Prosciutto crudo sgrassato", "Melone"),
            steps = listOf("Avvolgi le fette di prosciutto intorno ai pezzi di melone."),
        ),
        Template(
            id = "speck_funghi",
            name = "Speck con funghi",
            slot = MealSlot.AFTERNOON_SNACK,
            requiredFoods = listOf("Speck", "Funghi champignon"),
            steps = listOf("Salta i funghi in padella e servili con lo speck a fette."),
        ),
    )

    // ---------------------------- PRANZO ----------------------------
    private val lunchRecipes: List<Template> = listOf(
        Template(
            id = "pollo_riso_verdure",
            name = "Pollo alla piastra con riso e verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Petto di pollo", "Riso basmati (crudo)", "Zucchine"),
            steps = listOf("Cuoci il riso secondo i tempi indicati.", "Griglia il pollo a fette spesse, 3-4 minuti per lato.", "Salta le zucchine in padella con l'olio previsto dal pasto."),
        ),
        Template(
            id = "pasta_pollo_pomodoro",
            name = "Pasta al pomodoro con pollo",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta di semola (cruda)", "Petto di pollo", "Passata di pomodoro"),
            steps = listOf("Cuoci la pasta al dente.", "Rosola il pollo a cubetti, aggiungi la passata e cuoci 10 minuti.", "Manteca la pasta nel sugo."),
        ),
        Template(
            id = "merluzzo_patate",
            name = "Merluzzo al forno con patate",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Merluzzo", "Patate", "Olio extravergine d'oliva"),
            steps = listOf("Taglia le patate a spicchi e cuocile in forno a 200 gradi per 20 minuti.", "Aggiungi il merluzzo e prosegui 12-15 minuti.", "Condisci con l'olio previsto a crudo."),
        ),
        Template(
            id = "riso_uova_verdure",
            name = "Riso con uova e verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Riso bianco (crudo)", "Uovo intero di gallina", "Broccoli"),
            steps = listOf("Cuoci il riso e sbollenta i broccoli.", "Salta il riso in padella con le uova sbattute.", "Unisci i broccoli e servi."),
        ),
        Template(
            id = "insalata_tonno",
            name = "Insalata di tonno e cannellini",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Tonno in scatola al naturale", "Fagioli cannellini cotti", "Insalata lattuga"),
            steps = listOf("Scola tonno e cannellini.", "Unisci alla lattuga e condisci con l'olio previsto."),
        ),
        Template(
            id = "tacchino_cous_cous",
            name = "Tacchino con cous cous e peperoni",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Fesa di tacchino", "Cous cous (crudo)", "Peperoni"),
            steps = listOf("Reidrata il cous cous con acqua bollente.", "Griglia il tacchino e salta i peperoni.", "Unisci tutto in una ciotola."),
        ),
        Template(
            id = "pasta_integrale_tonno",
            name = "Pasta integrale con tonno e pomodoro",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta integrale (cruda)", "Tonno in scatola al naturale", "Pomodori pelati"),
            steps = listOf("Cuoci la pasta integrale al dente.", "Scalda i pelati con il tonno sgocciolato per 5 minuti.", "Manteca la pasta nel sugo."),
        ),
        Template(
            id = "pasta_integrale_pesto",
            name = "Pasta integrale al pesto",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta integrale (cruda)", "Pesto alla genovese"),
            steps = listOf("Cuoci la pasta.", "Condiscila con il pesto fuori dal fuoco per non scurirlo."),
        ),
        Template(
            id = "pasta_all_uovo_ragu",
            name = "Pasta all'uovo con macinato di manzo",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta all'uovo (cruda)", "Carne macinata di manzo (magra)", "Passata di pomodoro"),
            steps = listOf("Rosola la carne in padella.", "Aggiungi la passata e cuoci 15 minuti.", "Condisci la pasta scolata."),
        ),
        Template(
            id = "riso_gamberi",
            name = "Riso con gamberi e zucchine",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Riso bianco (crudo)", "Gamberi", "Zucchine"),
            steps = listOf("Cuoci il riso.", "Salta gamberi e zucchine in padella per 5 minuti.", "Unisci al riso."),
        ),
        Template(
            id = "riso_venere_salmone",
            name = "Riso venere con salmone",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Riso venere (crudo)", "Salmone", "Zucchine"),
            steps = listOf("Cuoci il riso venere (circa 40 minuti).", "Griglia il salmone.", "Salta le zucchine e componi il piatto."),
        ),
        Template(
            id = "farro_verdure",
            name = "Farro con verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Farro perlato (crudo)", "Melanzane", "Peperoni"),
            steps = listOf("Cuoci il farro.", "Griglia melanzane e peperoni.", "Unisci tutto con l'olio previsto."),
        ),
        Template(
            id = "quinoa_pollo",
            name = "Quinoa con pollo e spinaci",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Quinoa (cruda)", "Petto di pollo", "Spinaci"),
            steps = listOf("Sciacqua e cuoci la quinoa per 15 minuti.", "Griglia il pollo a striscioline.", "Salta gli spinaci e unisci tutto."),
        ),
        Template(
            id = "orzo_verdure",
            name = "Orzo con verdure e grana",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Orzo perlato (crudo)", "Zucchine", "Formaggio grana padano"),
            steps = listOf("Cuoci l'orzo.", "Salta le zucchine.", "Unisci e completa con il grana a scaglie."),
        ),
        Template(
            id = "polenta_funghi",
            name = "Polenta con funghi",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Polenta (farina di mais cruda)", "Funghi champignon", "Formaggio grana padano"),
            steps = listOf("Cuoci la polenta mescolando per 40 minuti.", "Salta i funghi in padella.", "Servi la polenta con funghi e grana."),
        ),
        Template(
            id = "gnocchi_pomodoro",
            name = "Gnocchi al pomodoro",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Gnocchi di patate", "Pomodori pelati"),
            steps = listOf("Cuoci gli gnocchi finche non salgono a galla.", "Condiscili con i pelati scaldati."),
        ),
        Template(
            id = "patate_tonno",
            name = "Patate con tonno",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Patate", "Tonno in scatola al naturale"),
            steps = listOf("Lessate le patate e tagliale a cubetti.", "Unisci il tonno sgocciolato e l'olio previsto."),
        ),
        Template(
            id = "pollo_patate_dolci",
            name = "Pollo con patate dolci al forno",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Petto di pollo", "Patate dolci"),
            steps = listOf("Taglia le patate dolci a cubetti e inforna a 200 gradi per 25 minuti.", "Aggiungi il pollo e prosegui 15 minuti."),
        ),
        Template(
            id = "manzo_riso",
            name = "Manzo con riso e broccoli",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Fesa di manzo", "Riso basmati (crudo)", "Broccoli"),
            steps = listOf("Cuoci il riso e sbollenta i broccoli.", "Salta la carne a striscioline per 3 minuti.", "Unisci tutto."),
        ),
        Template(
            id = "vitello_patate",
            name = "Vitello con patate",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Fesa di vitello", "Patate"),
            steps = listOf("Rosola la fesa di vitello in padella.", "Accompagna con le patate lessate o al forno."),
        ),
        Template(
            id = "sogliola_patate",
            name = "Sogliola con patate",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Sogliola", "Patate"),
            steps = listOf("Cuoci la sogliola in padella con un filo d'olio.", "Servi con le patate lessate."),
        ),
        Template(
            id = "platessa_riso",
            name = "Platessa con riso e zucchine",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Platessa", "Riso bianco (crudo)", "Zucchine"),
            steps = listOf("Cuoci il riso.", "Cuoci la platessa in padella.", "Salta le zucchine e servi."),
        ),
        Template(
            id = "branzino_patate",
            name = "Branzino al forno con patate",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Branzino", "Patate"),
            steps = listOf("Inforna il branzino con le patate a fette a 200 gradi per 25 minuti."),
        ),
        Template(
            id = "orata_riso",
            name = "Orata con riso e pomodori",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Orata", "Riso bianco (crudo)", "Pomodori"),
            steps = listOf("Cuoci il riso.", "Inforna l'orata con i pomodori per 20 minuti."),
        ),
        Template(
            id = "tonno_fresco_riso",
            name = "Tonno fresco con riso",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Tonno fresco", "Riso basmati (crudo)", "Rucola"),
            steps = listOf("Cuoci il riso.", "Scotta il tonno in padella 2 minuti per lato.", "Servi con la rucola."),
        ),
        Template(
            id = "seppie_patate",
            name = "Seppie con patate",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Seppie", "Patate"),
            steps = listOf("Cuoci le seppie in padella con aglio e prezzemolo.", "Accompagna con le patate lessate."),
        ),
        Template(
            id = "cozze_pasta",
            name = "Pasta con cozze",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta di semola (cruda)", "Cozze", "Pomodori pelati"),
            steps = listOf("Apri le cozze in padella coperta.", "Unisci i pelati e cuoci 5 minuti.", "Condisci la pasta."),
        ),
        Template(
            id = "lenticchie_pasta",
            name = "Pasta con lenticchie",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta di semola (cruda)", "Lenticchie cotte"),
            steps = listOf("Scalda le lenticchie con un po' di acqua.", "Condisci la pasta scolata."),
        ),
        Template(
            id = "ceci_riso",
            name = "Riso con ceci",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Riso bianco (crudo)", "Ceci cotti"),
            steps = listOf("Cuoci il riso.", "Scalda i ceci e uniscili al riso con l'olio previsto."),
        ),
        Template(
            id = "fagioli_pasta",
            name = "Pasta e fagioli",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pasta di semola (cruda)", "Fagioli borlotti cotti"),
            steps = listOf("Scalda i fagioli con acqua.", "Cuoci la pasta e condiscila con i fagioli."),
        ),
        Template(
            id = "tofu_riso_verdure",
            name = "Tofu con riso e verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Tofu", "Riso integrale (crudo)", "Broccoli"),
            steps = listOf("Cuoci il riso integrale.", "Salta il tofu a cubetti.", "Unisci i broccoli e servi."),
        ),
        Template(
            id = "seitan_pasta",
            name = "Seitan con pasta e pomodoro",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Seitan", "Pasta di semola (cruda)", "Passata di pomodoro"),
            steps = listOf("Rosola il seitan a fette.", "Scalda la passata.", "Condisci la pasta con seitan e sugo."),
        ),
        Template(
            id = "tempeh_riso",
            name = "Tempeh con riso e verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Tempeh", "Riso basmati (crudo)", "Peperoni"),
            steps = listOf("Cuoci il riso.", "Salta il tempeh a cubetti e i peperoni.", "Unisci e servi."),
        ),
        Template(
            id = "piadina_tacchino",
            name = "Piadina con tacchino e rucola",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Piadina", "Fesa di tacchino a fette", "Rucola"),
            steps = listOf("Scalda la piadina in padella.", "Farciscila con tacchino e rucola."),
        ),
        Template(
            id = "pane_roast_beef",
            name = "Panino con roast beef",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Pane integrale", "Roast beef (fesa cotta)", "Rucola"),
            steps = listOf("Farcisci il pane con il roast beef e la rucola."),
        ),
        Template(
            id = "cous_cous_verdure",
            name = "Cous cous con verdure",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Cous cous (crudo)", "Melanzane", "Zucchine"),
            steps = listOf("Griglia le verdure.", "Reidrata il cous cous e unisci le verdure."),
        ),
        Template(
            id = "insalata_pollo",
            name = "Insalata con pollo e pomodori",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Petto di pollo", "Insalata lattuga", "Pomodori"),
            steps = listOf("Griglia il pollo a striscioline.", "Unisci lattuga e pomodori, condisci con l'olio previsto."),
        ),
        Template(
            id = "insalata_gamberi",
            name = "Insalata con gamberi e avocado",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Gamberi", "Avocado", "Insalata lattuga"),
            steps = listOf("Scottate i gamberi 2 minuti.", "Unisci avocado a fette e lattuga."),
        ),
        Template(
            id = "girello_patate",
            name = "Girello con patate e carote",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Girello di manzo", "Patate", "Carote"),
            steps = listOf("Rosola il girello.", "Cuoci patate e carote a tocchetti in padella o al forno."),
        ),
        Template(
            id = "maiale_finocchi",
            name = "Filetto di maiale con finocchi",
            slot = MealSlot.LUNCH,
            requiredFoods = listOf("Filetto di maiale", "Finocchi"),
            steps = listOf("Rosola il filetto a fette.", "Gratina i finocchi in padella con l'olio previsto."),
        ),
    )

    // ---------------------------- CENA ----------------------------
    private val dinnerRecipes: List<Template> = listOf(
        Template(
            id = "salmone_riso_broccoli",
            name = "Salmone al forno con riso e broccoli",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Salmone", "Riso basmati (crudo)", "Broccoli"),
            steps = listOf("Inforna il salmone a 190 gradi per 12-15 minuti.", "Cuoci il riso e sbollenta i broccoli.", "Servi con l'olio previsto a crudo."),
        ),
        Template(
            id = "frittata_spinaci",
            name = "Frittata con spinaci",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Uovo intero di gallina", "Spinaci"),
            steps = listOf("Sbatti le uova e unisci gli spinaci gia saltati.", "Cuoci in padella a fuoco medio, girando a meta cottura."),
        ),
        Template(
            id = "manzo_insalata",
            name = "Manzo alla griglia con insalata",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Fesa di manzo", "Insalata lattuga", "Olio extravergine d'oliva"),
            steps = listOf("Griglia la fesa 3 minuti per lato.", "Servi con l'insalata e l'olio previsto."),
            notes = "Lasciare riposare la carne 2 minuti prima di tagliarla mantiene i succhi."
        ),
        Template(
            id = "orata_verdure",
            name = "Orata al forno con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Orata", "Zucchine", "Pomodori"),
            steps = listOf("Disponi le verdure a fette in teglia.", "Adagia l'orata sopra e inforna a 200 gradi per 20 minuti."),
        ),
        Template(
            id = "tofu_verdure_riso",
            name = "Tofu saltato con verdure e riso",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Tofu", "Riso integrale (crudo)", "Broccoli"),
            steps = listOf("Cuoci il riso integrale.", "Salta il tofu a cubetti finche non dorato.", "Unisci i broccoli e prosegui 5 minuti."),
        ),
        Template(
            id = "pollo_patate_forno",
            name = "Pollo e patate al forno",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Petto di pollo", "Patate", "Olio extravergine d'oliva"),
            steps = listOf("Taglia patate e pollo a pezzi.", "Inforna a 200 gradi per 30 minuti, girando a meta cottura."),
        ),
        Template(
            id = "merluzzo_verdure",
            name = "Merluzzo con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Merluzzo", "Zucchine", "Carote"),
            steps = listOf("Cuoci il merluzzo al vapore o in padella.", "Salta zucchine e carote a julienne."),
        ),
        Template(
            id = "nasello_pomodori",
            name = "Nasello con pomodori e olive",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Nasello", "Pomodori", "Olive nere"),
            steps = listOf("Cuoci il nasello in padella con i pomodori.", "Completa con le olive."),
        ),
        Template(
            id = "sgombro_patate",
            name = "Sgombro con patate",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Sgombro", "Patate"),
            steps = listOf("Inforna lo sgombro con le patate a fette per 25 minuti."),
        ),
        Template(
            id = "trota_riso",
            name = "Trota con riso e zucchine",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Trota", "Riso bianco (crudo)", "Zucchine"),
            steps = listOf("Cuoci la trota in padella o al forno.", "Cuoci il riso e salta le zucchine."),
        ),
        Template(
            id = "pesce_spada_verdure",
            name = "Pesce spada con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Pesce spada", "Melanzane", "Peperoni"),
            steps = listOf("Griglia il pesce spada 3 minuti per lato.", "Griglia le verdure e servi."),
        ),
        Template(
            id = "branzino_forno",
            name = "Branzino al forno con pomodorini",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Branzino", "Pomodori", "Olive verdi"),
            steps = listOf("Inforna il branzino con pomodori e olive a 200 gradi per 20 minuti."),
        ),
        Template(
            id = "sarde_pasta",
            name = "Pasta con sarde",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Pasta di semola (cruda)", "Sarde", "Pomodori pelati"),
            steps = listOf("Scalda i pelati con le sarde pulite.", "Cuoci la pasta e condiscila."),
        ),
        Template(
            id = "alici_riso",
            name = "Alici con riso e limone",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Alici", "Riso bianco (crudo)", "Rucola"),
            steps = listOf("Cuoci il riso.", "Scotta le alici in padella 2 minuti per lato.", "Servi con rucola e limone."),
        ),
        Template(
            id = "polpo_patate",
            name = "Polpo con patate",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Polpo", "Patate"),
            steps = listOf("Lessate il polpo e le patate.", "Taglia a pezzi e condisci con l'olio previsto."),
        ),
        Template(
            id = "calamari_riso",
            name = "Calamari con riso",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Calamari", "Riso basmati (crudo)", "Pomodori"),
            steps = listOf("Salta i calamari a anelli per 5 minuti.", "Cuoci il riso e unisci i pomodori a cubetti."),
        ),
        Template(
            id = "vongole_pasta",
            name = "Pasta con vongole",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Pasta di semola (cruda)", "Vongole", "Pomodori"),
            steps = listOf("Apri le vongole in padella coperta.", "Unisci i pomodori e condisci la pasta."),
        ),
        Template(
            id = "frittata_zucchine",
            name = "Frittata con zucchine",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Uovo intero di gallina", "Zucchine"),
            steps = listOf("Salta le zucchine a rondelle.", "Versa le uova sbattute e cuoci a fuoco medio."),
        ),
        Template(
            id = "uova_funghi",
            name = "Uova con funghi",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Uovo intero di gallina", "Funghi champignon"),
            steps = listOf("Salta i funghi in padella.", "Aggiungi le uova e strapazza a fuoco basso."),
        ),
        Template(
            id = "pollo_broccoli",
            name = "Pollo con broccoli",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Petto di pollo", "Broccoli"),
            steps = listOf("Griglia il pollo a fette.", "Sbollenta i broccoli e condisci con l'olio previsto."),
        ),
        Template(
            id = "tacchino_verdure",
            name = "Tacchino con verdure al forno",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Fesa di tacchino", "Zucchine", "Peperoni"),
            steps = listOf("Taglia le verdure e inforna a 200 gradi per 20 minuti.", "Aggiungi il tacchino e prosegui 15 minuti."),
        ),
        Template(
            id = "coscia_pollo_patate",
            name = "Coscia di pollo con patate",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Coscia di pollo (senza pelle)", "Patate"),
            steps = listOf("Inforna coscia e patate a 200 gradi per 35 minuti."),
        ),
        Template(
            id = "sovracoscia_verdure",
            name = "Sovracoscia con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Sovracoscia di pollo (senza pelle)", "Finocchi"),
            steps = listOf("Rosola la sovracoscia.", "Gratina i finocchi in padella."),
        ),
        Template(
            id = "macinato_pollo_peperoni",
            name = "Macinato di pollo con peperoni",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Carne macinata di pollo (magra)", "Peperoni"),
            steps = listOf("Rosola la carne macinata.", "Unisci i peperoni a striscioline e cuoci 10 minuti."),
        ),
        Template(
            id = "bistecca_insalata",
            name = "Bistecca con insalata",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Bistecca di manzo (magra)", "Insalata lattuga"),
            steps = listOf("Griglia la bistecca 3 minuti per lato.", "Servi con l'insalata."),
        ),
        Template(
            id = "roast_beef_rucola",
            name = "Roast beef con rucola e grana",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Roast beef (fesa cotta)", "Rucola", "Formaggio grana padano"),
            steps = listOf("Disponi il roast beef sulla rucola.", "Completa con il grana a scaglie."),
        ),
        Template(
            id = "maiale_verza",
            name = "Filetto di maiale con verza",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Filetto di maiale", "Cavolo verza"),
            steps = listOf("Rosola il filetto.", "Stufa la verza in padella con poca acqua."),
        ),
        Template(
            id = "seitan_verdure",
            name = "Seitan con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Seitan", "Melanzane", "Zucchine"),
            steps = listOf("Rosola il seitan a fette.", "Griglia le verdure e servi."),
        ),
        Template(
            id = "tempeh_broccoli",
            name = "Tempeh con broccoli",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Tempeh", "Broccoli"),
            steps = listOf("Salta il tempeh a cubetti.", "Unisci i broccoli lessati e prosegui 3 minuti."),
        ),
        Template(
            id = "tofu_spinaci",
            name = "Tofu con spinaci",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Tofu", "Spinaci"),
            steps = listOf("Salta il tofu a cubetti.", "Unisci gli spinaci e cuoci finche appassiscono."),
        ),
        Template(
            id = "gamberi_verdure",
            name = "Gamberi con verdure",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Gamberi", "Zucchine", "Peperoni"),
            steps = listOf("Salta gamberi e verdure a fette per 6 minuti."),
        ),
        Template(
            id = "mozzarella_pomodori",
            name = "Caprese con mozzarella",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Mozzarella vaccina", "Pomodori"),
            steps = listOf("Affetta mozzarella e pomodori.", "Alterna le fette e condisci con l'olio previsto."),
        ),
        Template(
            id = "bufala_pomodori",
            name = "Caprese con mozzarella di bufala",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Mozzarella di bufala", "Pomodori"),
            steps = listOf("Alterna bufala e pomodori a fette.", "Aggiungi l'olio previsto a crudo."),
        ),
        Template(
            id = "ricotta_spinaci",
            name = "Ricotta con spinaci",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Ricotta vaccina", "Spinaci"),
            steps = listOf("Salta gli spinaci.", "Servili con la ricotta a cucchiaiate."),
        ),
        Template(
            id = "minestrone_grana",
            name = "Verdure con grana",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Zucca", "Cavolo nero", "Formaggio grana padano"),
            steps = listOf("Cuoci zucca e cavolo nero in padella con poca acqua.", "Completa con il grana."),
        ),
        Template(
            id = "funghi_grana",
            name = "Funghi con grana",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Funghi champignon", "Formaggio grana padano"),
            steps = listOf("Salta i funghi finche dorati.", "Completa con il grana a scaglie."),
        ),
        Template(
            id = "uova_avocado",
            name = "Uova con avocado",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Uovo intero di gallina", "Avocado"),
            steps = listOf("Cuoci le uova in padella.", "Servi con avocado a fette."),
        ),
        Template(
            id = "pollo_fagiolini",
            name = "Pollo con fagiolini",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Petto di pollo", "Fagiolini"),
            steps = listOf("Griglia il pollo.", "Sbollenta i fagiolini e condisci con l'olio previsto."),
        ),
        Template(
            id = "salmone_avocado",
            name = "Salmone con avocado",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Salmone", "Avocado", "Rucola"),
            steps = listOf("Scotta il salmone in padella 3 minuti per lato.", "Servi con avocado e rucola."),
        ),
        Template(
            id = "merluzzo_pomodori",
            name = "Merluzzo con pomodori e olive",
            slot = MealSlot.DINNER,
            requiredFoods = listOf("Merluzzo", "Pomodori", "Olive verdi"),
            steps = listOf("Cuoci il merluzzo con pomodori e olive in padella per 12 minuti."),
        ),
    )

    /**
     * I nomi devono corrispondere esattamente a FoodSeedData: un nome errato non
     * produce errori, semplicemente la ricetta non viene mai proposta. Il test
     * `ogni ingrediente delle ricette esiste nel database alimentare` lo verifica.
     */
    private val templates: List<Template> = buildList {
        addAll(breakfastRecipes)
        addAll(snackRecipes)
        addAll(lunchRecipes)
        addAll(dinnerRecipes)
    }
}
