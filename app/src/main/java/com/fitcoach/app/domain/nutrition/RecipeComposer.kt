package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot

/**
 * Ruolo di un alimento nella composizione di una ricetta.
 *
 * Non si usa la categoria del database perche non e' omogenea (alcune voci
 * hanno il nome del piatto come categoria). Il ruolo si deduce dal profilo dei
 * macronutrienti e dal nome, che sono i due dati sempre presenti e coerenti.
 */
enum class RecipeRole(val label: String) {
    PROTEIN("fonte proteica"),
    CARB("carboidrati"),
    VEG("verdura"),
    FRUIT("frutta"),
    FAT("grassi"),
    DAIRY("latticini"),
    OTHER("altro")
}

/** Come deve essere composta la ricetta. */
enum class RecipeStyle(val label: String) {
    CLASSIC("Classica"),
    LIGHT("Leggera"),
    RICH("Ricca")
}

/** Ricetta composta dagli alimenti reali di un pasto del piano. */
data class ComposedRecipe(
    val slot: MealSlot,
    val name: String,
    val ingredients: List<MealEntry>,
    val style: RecipeStyle,
    val steps: List<String>
) {
    val kcal: Double get() = ingredients.sumOf { it.kcal }
    val protein: Double get() = ingredients.sumOf { it.protein }
    val carbs: Double get() = ingredients.sumOf { it.carbs }
    val fat: Double get() = ingredients.sumOf { it.fat }

    /** Firma stabile per riconoscere la stessa combinazione. */
    val signature: String
        get() = slot.name + "|" + ingredients.joinToString("|") { it.foodName }
}

/**
 * Compone una ricetta dagli alimenti gia' previsti dal piano per un pasto.
 *
 * Deterministico: stessi input, stesso output. Non inventa alimenti e non
 * modifica le grammature, che restano quelle calcolate dal motore nutrizionale.
 * Il risultato e' una riorganizzazione del pasto in un piatto, non un pasto
 * diverso: i totali coincidono con quelli degli alimenti usati.
 *
 * L'AI non partecipa: questo motore funziona offline e il suo risultato e'
 * verificabile. Le proposte creative restano un'opzione separata e dichiarata.
 */
object RecipeComposer {

    /** Quanti ingredienti al massimo, per non generare piatti ingestibili. */
    private const val MAX_INGREDIENTS = 4

    /**
     * Crea la ricetta per [slot] usando gli alimenti del pasto.
     *
     * Restituisce null quando il pasto non ha abbastanza alimenti per comporre
     * qualcosa di sensato: meglio dirlo che proporre un piatto inventato.
     */
    fun create(
        entries: List<MealEntry>,
        slot: MealSlot,
        foods: List<FoodItem>,
        style: RecipeStyle = RecipeStyle.CLASSIC,
        avoid: Set<String> = emptySet()
    ): ComposedRecipe? {
        if (entries.isEmpty()) return null
        val byName = foods.associateBy { it.normalizedName }

        val pool = entries
            .filterNot { avoid.contains(FoodItem.normalizeName(it.foodName)) }
            .map { it to roleOf(it, byName[FoodItem.normalizeName(it.foodName)]) }
        if (pool.isEmpty()) return null

        val byRole = pool.groupBy({ it.second }, { it.first })

        val chosen = when (slot) {
            MealSlot.BREAKFAST -> breakfast(byRole, style)
            MealSlot.MORNING_SNACK, MealSlot.AFTERNOON_SNACK, MealSlot.EVENING_SNACK ->
                snack(byRole, style)
            MealSlot.LUNCH, MealSlot.DINNER -> mainMeal(byRole, style)
        }

        if (chosen.size < 2) return null

        val ingredients = chosen
            .distinctBy { FoodItem.normalizeName(it.foodName) }
            .take(MAX_INGREDIENTS)

        return ComposedRecipe(
            slot = slot,
            name = nameFor(slot, ingredients),
            ingredients = ingredients,
            style = style,
            steps = stepsFor(slot, ingredients)
        )
    }

    private fun breakfast(
        byRole: Map<RecipeRole, List<MealEntry>>,
        style: RecipeStyle
    ): List<MealEntry> = buildList {
        byRole[RecipeRole.CARB]?.firstOrNull()?.let { add(it) }
        (byRole[RecipeRole.PROTEIN] ?: byRole[RecipeRole.DAIRY])?.firstOrNull()?.let { add(it) }
        byRole[RecipeRole.FRUIT]?.firstOrNull()?.let { add(it) }
        if (style != RecipeStyle.LIGHT) {
            byRole[RecipeRole.FAT]?.firstOrNull()?.let { add(it) }
        }
        if (style == RecipeStyle.RICH) {
            byRole[RecipeRole.DAIRY]?.drop(1)?.firstOrNull()?.let { add(it) }
        }
    }

    private fun snack(
        byRole: Map<RecipeRole, List<MealEntry>>,
        style: RecipeStyle
    ): List<MealEntry> = buildList {
        (byRole[RecipeRole.PROTEIN] ?: byRole[RecipeRole.DAIRY])?.firstOrNull()?.let { add(it) }
        byRole[RecipeRole.FRUIT]?.firstOrNull()?.let { add(it) }
        if (style != RecipeStyle.LIGHT) {
            byRole[RecipeRole.FAT]?.firstOrNull()?.let { add(it) }
        }
        if (style == RecipeStyle.RICH) {
            byRole[RecipeRole.CARB]?.firstOrNull()?.let { add(it) }
        }
    }

    private fun mainMeal(
        byRole: Map<RecipeRole, List<MealEntry>>,
        style: RecipeStyle
    ): List<MealEntry> = buildList {
        byRole[RecipeRole.PROTEIN]?.firstOrNull()?.let { add(it) }
        byRole[RecipeRole.CARB]?.firstOrNull()?.let { add(it) }
        byRole[RecipeRole.VEG]?.firstOrNull()?.let { add(it) }
        if (style != RecipeStyle.LIGHT) {
            byRole[RecipeRole.FAT]?.firstOrNull()?.let { add(it) }
        }
        if (style == RecipeStyle.RICH) {
            byRole[RecipeRole.VEG]?.drop(1)?.firstOrNull()?.let { add(it) }
        }
    }

    /**
     * Il ruolo si deduce dai macro per 100 g, che sono il dato sempre affidabile.
     * L'ordine dei controlli conta: una verdura proteica (legumi) e' proteica
     * prima che verdura.
     */
    internal fun roleOf(entry: MealEntry, food: FoodItem?): RecipeRole {
        val name = FoodItem.normalizeName(entry.foodName)
        // Fuori anagrafica restano i macro della porzione, che sono l'unico dato
        // reale disponibile: il nome da solo non basta a distinguere un piatto
        // proteico da uno di carboidrati.
        if (food == null) return roleFromPortion(entry, name)

        val kcal = food.kcalPer100g
        if (kcal <= 0.0) return roleFromName(name)
        val proteinKcal = food.proteinPer100g * 4.0
        val carbKcal = food.carbsPer100g * 4.0
        val fatKcal = food.fatPer100g * 9.0
        val proteinShare = proteinKcal / kcal
        val fatShare = fatKcal / kcal
        val carbShare = carbKcal / kcal

        return when {
            // Frutta: zuccheri alti e pochi grassi/proteine, riconosciuta per nome
            // perche le bacche hanno macro simili a certe verdure.
            isFruitName(name) -> RecipeRole.FRUIT
            // Grassi quasi puri: olio, burro, frutta secca, semi.
            fatShare > 0.70 -> RecipeRole.FAT
            // Latticini: proteine discrete ma grassi o zuccheri del latte.
            isDairyName(name) -> RecipeRole.DAIRY
            proteinShare > 0.35 -> RecipeRole.PROTEIN
            carbShare > 0.60 -> RecipeRole.CARB
            isVegName(name) -> RecipeRole.VEG
            proteinShare > 0.20 -> RecipeRole.PROTEIN
            else -> RecipeRole.OTHER
        }
    }

    /**
     * Classificazione per un alimento non presente in anagrafica: si usano i
     * macro della porzione effettiva. Sotto una soglia minima di calorie il
     * rapporto non e' significativo e si ricade sul nome.
     */
    private fun roleFromPortion(entry: MealEntry, name: String): RecipeRole {
        if (entry.kcal <= 0.0) return roleFromName(name)
        val proteinShare = entry.protein * 4.0 / entry.kcal
        val carbShare = entry.carbs * 4.0 / entry.kcal
        val fatShare = entry.fat * 9.0 / entry.kcal
        return when {
            isFruitName(name) -> RecipeRole.FRUIT
            fatShare > 0.70 -> RecipeRole.FAT
            isDairyName(name) -> RecipeRole.DAIRY
            proteinShare > 0.35 -> RecipeRole.PROTEIN
            carbShare > 0.60 -> RecipeRole.CARB
            isVegName(name) -> RecipeRole.VEG
            proteinShare > 0.20 -> RecipeRole.PROTEIN
            else -> roleFromName(name)
        }
    }

    private fun roleFromName(name: String): RecipeRole = when {
        isFruitName(name) -> RecipeRole.FRUIT
        isVegName(name) -> RecipeRole.VEG
        isDairyName(name) -> RecipeRole.DAIRY
        else -> RecipeRole.OTHER
    }

    private val fruitWords = listOf(
        "mela", "pera", "banana", "arancia", "mandarino", "kiwi", "fragola", "lampone",
        "mirtillo", "uva", "melone", "anguria", "ananas", "pesca", "albicocca", "pompelmo",
        "dattero", "fico", "frutt", "cilieg", "prugn", "susin", "ribes", "more", "cocomero"
    )

    private val vegWords = listOf(
        "zucchin", "broccoli", "spinac", "insalata", "lattuga", "pomodor", "cetriol",
        "peperon", "melanzan", "funghi", "asparag", "fagiolin", "carota", "carote",
        "finocchi", "radicchio", "rucola", "cavolo", "verza", "bietol", "zucca",
        "cipoll", "sedano", "olive", "cime di rapa"
    )

    private val dairyWords = listOf(
        "yogurt", "latte", "ricotta", "mozzarella", "stracchino", "parmigiano",
        "grana", "fiocchi di latte", "formaggio"
    )

    private fun isFruitName(name: String) = fruitWords.any { name.contains(it) }
    private fun isVegName(name: String) = vegWords.any { name.contains(it) }
    private fun isDairyName(name: String) = dairyWords.any { name.contains(it) }

    /** Nome del piatto costruito dagli ingredienti scelti. */
    private fun nameFor(slot: MealSlot, ingredients: List<MealEntry>): String {
        val labels = ingredients.map { it.foodName }
        val joined = when (labels.size) {
            2 -> "${labels[0]} con ${labels[1]}"
            3 -> "${labels[0]} con ${labels[1]} e ${labels[2]}"
            else -> "${labels[0]} con ${labels.drop(1).dropLast(1).joinToString(", ")} e ${labels.last()}"
        }
        val prefix = when (slot) {
            MealSlot.BREAKFAST -> "Colazione: "
            MealSlot.MORNING_SNACK -> "Spuntino mattutino: "
            MealSlot.AFTERNOON_SNACK -> "Spuntino pomeridiano: "
            MealSlot.EVENING_SNACK -> "Spuntino serale: "
            MealSlot.LUNCH -> "Pranzo: "
            MealSlot.DINNER -> "Cena: "
        }
        return prefix + joined.replaceFirstChar { it.uppercase() }
    }

    /**
     * Passi di preparazione coerenti con gli ingredienti scelti. Sono indicazioni
     * di assemblaggio, non dosaggi: le quantita' sono gia' quelle del piano.
     */
    private fun stepsFor(slot: MealSlot, ingredients: List<MealEntry>): List<String> {
        val names = ingredients.map { FoodItem.normalizeName(it.foodName) }
        val steps = mutableListOf<String>()

        val hasCarbToCook = names.any { it.contains("avena") || it.contains("riso") || it.contains("pasta") || it.contains("cous cous") || it.contains("farro") || it.contains("orzo") || it.contains("quinoa") || it.contains("polenta") }
        val hasEgg = names.any { it.contains("uovo") || it.contains("albume") || it.contains("frittata") }
        val hasMeatOrFish = names.any {
            it.contains("pollo") || it.contains("tacchino") || it.contains("manzo") || it.contains("maiale") ||
                it.contains("salmone") || it.contains("merluzzo") || it.contains("tonno") || it.contains("pesce") ||
                it.contains("gamberi") || it.contains("sarde") || it.contains("orata") || it.contains("branzino")
        }
        val hasVeg = names.any { isVegName(it) }

        if (hasCarbToCook) {
            steps += "Cuoci la fonte di carboidrati secondo i tempi indicati sulla confezione."
        }
        if (hasMeatOrFish) {
            steps += "Cuoci la fonte proteica alla piastra o in padella, girandola a meta cottura."
        }
        if (hasEgg) {
            steps += "Sbatti le uova e cuocile a fuoco basso mescolando."
        }
        if (hasVeg) {
            steps += "Salta la verdura in padella con l'olio previsto dal pasto, per 5-6 minuti."
        }
        if (steps.isEmpty()) {
            steps += when (slot) {
                MealSlot.BREAKFAST -> "Unisci gli ingredienti in una ciotola e mescola."
                MealSlot.MORNING_SNACK, MealSlot.AFTERNOON_SNACK, MealSlot.EVENING_SNACK ->
                    "Prepara gli ingredienti e consumali insieme."
                MealSlot.LUNCH, MealSlot.DINNER -> "Componi il piatto e condisci con l'olio previsto."
            }
        }
        steps += "Le grammature sono quelle del tuo piano: non vanno modificate."
        return steps
    }
}
