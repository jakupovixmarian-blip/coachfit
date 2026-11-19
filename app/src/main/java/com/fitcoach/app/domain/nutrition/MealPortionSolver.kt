package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import kotlin.math.abs

/**
 * Risolve le grammature di una giornata alimentare a partire dagli alimenti
 * scelti dal Coach AI, centrando il target deterministico della giornata.
 *
 * Perche esiste: il target di ogni giornata non e negoziabile (deriva da
 * [CarbCyclingEngine] e viene imposto da [ProgramGenerationEngine.validate]),
 * ma l'AI non lo riceve e non puo risolvere a mano un sistema di 35 pasti. Il
 * risultato era un piano sistematicamente sotto il target (~60%) e bocciato.
 *
 * Divisione dei compiti: l'AI decide COSA mangiare (alimenti, pasti, ordine);
 * questo oggetto calcola QUANTO, sui valori reali del catalogo. Nessun valore
 * nutrizionale viene inventato e nessun alimento viene aggiunto di nascosto.
 */
object MealPortionSolver {

    /** Tolleranza del solver: molto piu stretta di quella del validatore (20-30%). */
    private const val TARGET_TOLERANCE = 0.03

    private const val MIN_GRAMS = 1.0
    private const val MAX_GRAMS = 2_000.0

    /** Ruolo di un alimento, usato per decidere quale grammatura muovere. */
    enum class Role { PROTEIN, CARBS, FAT, MIXED }

    data class SolvedEntry(
        val foodName: String,
        val grams: Double,
        val kcal: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    data class SolvedMeal(
        val slot: String,
        val entries: List<SolvedEntry>
    ) {
        val totals: MacroTotals
            get() = entries.fold(MacroTotals.ZERO) { sum, e ->
                sum + MacroTotals(e.kcal, e.protein, e.carbs, e.fat)
            }
    }

    data class Result(
        val meals: List<SolvedMeal>,
        val actual: MacroTotals,
        val target: MacroTotals,
        val deviations: MacroTotals,
        val withinTolerance: Boolean,
        val warnings: List<String>
    ) {
        val ok: Boolean get() = withinTolerance && warnings.none { it.startsWith("IMPOSSIBILE") }
    }

    /**
     * Classifica un alimento per il macro che lo caratterizza.
     *
     * Non basta il macro con piu kcal: l'uovo intero ha piu kcal da grassi ma
     * resta una fonte proteica. Servono densita (g/100 g) e quota calorica.
     */
    fun roleOf(food: FoodItem): Role {
        val pKcal = food.proteinPer100g * 4.0
        val cKcal = food.carbsPer100g * 4.0
        val fKcal = food.fatPer100g * 9.0
        val total = pKcal + cKcal + fKcal
        if (total <= 0.0) return Role.MIXED
        // Alimenti a bassissima densita (verdura, ortaggi): non possono trainare
        // nessun macro senza grammature assurde. Restano neutri e li muove il
        // solver solo come contorno, non come fonte.
        if (food.kcalPer100g < 50.0) return Role.MIXED

        val pShare = pKcal / total
        val cShare = cKcal / total
        val fShare = fKcal / total

        if (food.proteinPer100g >= 10.0 && pShare >= 0.30) return Role.PROTEIN
        if (food.fatPer100g >= 15.0 && fShare >= 0.50) return Role.FAT
        if (food.carbsPer100g >= 15.0 && cShare >= 0.40) return Role.CARBS

        return when (maxOf(pShare, cShare, fShare)) {
            pShare -> Role.PROTEIN
            cShare -> Role.CARBS
            else -> Role.FAT
        }
    }

    /**
     * Risolve le grammature.
     *
     * @param target target della giornata (kcal/proteine/carboidrati/grassi)
     * @param structure pasti con gli alimenti scelti dall'AI, gia risolti sul catalogo
     * @param initialGrams grammatura di partenza per alimento (default ragionevole)
     */
    fun solve(
        target: CarbDayTarget,
        structure: List<Pair<String, List<FoodItem>>>,
        initialGrams: Map<String, Double> = emptyMap()
    ): Result {
        val targetMacros = MacroTotals(target.kcal, target.protein, target.carbs, target.fat)
        val warnings = mutableListOf<String>()

        // Stato mutabile: grammatura corrente per ogni occorrenza (pasto, alimento).
        data class Slot(val mealIndex: Int, val food: FoodItem, val role: Role, var grams: Double)
        val slots = mutableListOf<Slot>()
        structure.forEachIndexed { mealIndex, (_, foods) ->
            foods.forEach { food ->
                val start = initialGrams[food.name]
                    ?: when (roleOf(food)) {
                        Role.PROTEIN -> 100.0
                        Role.CARBS -> 60.0
                        Role.FAT -> 10.0
                        Role.MIXED -> 50.0
                    }
                slots += Slot(mealIndex, food, roleOf(food), start.coerceIn(MIN_GRAMS, MAX_GRAMS))
            }
        }

        if (slots.isEmpty()) {
            return Result(
                meals = emptyList(),
                actual = MacroTotals.ZERO,
                target = targetMacros,
                deviations = targetMacros,
                withinTolerance = false,
                warnings = listOf("IMPOSSIBILE: nessun alimento nella giornata.")
            )
        }

        fun contribution(role: Role?, macro: Role): Double {
            val relevant = slots.filter { role == null || it.role == role }
            return relevant.sumOf { s ->
                val per100 = when (macro) {
                    Role.PROTEIN -> s.food.proteinPer100g
                    Role.CARBS -> s.food.carbsPer100g
                    Role.FAT -> s.food.fatPer100g
                    Role.MIXED -> 0.0
                }
                per100 * s.grams / 100.0
            }
        }

        // Aggiustamento iterativo: ogni passata riporta un macro sul target
        // scalando solo gli alimenti di quel ruolo. Le passate successive
        // assorbono l'effetto collaterale sui macro vicini.
        val macroTargets = mapOf(
            Role.PROTEIN to targetMacros.protein,
            Role.CARBS to targetMacros.carbs,
            Role.FAT to targetMacros.fat
        )
        repeat(24) {
            macroTargets.forEach { (role, goal) ->
                val own = slots.filter { it.role == role }
                if (own.isEmpty()) return@forEach
                val fromOwn = contribution(role, role)
                if (fromOwn <= 1e-6) return@forEach
                val fromOthers = contribution(null, role) - fromOwn
                val desiredOwn = goal - fromOthers
                if (desiredOwn <= 0.0) {
                    // Gli altri alimenti coprono gia il target: le fonti di questo
                    // ruolo vanno ridotte al minimo, non lasciate al valore iniziale.
                    own.forEach { s -> s.grams = MIN_GRAMS }
                    return@forEach
                }
                val factor = desiredOwn / fromOwn
                own.forEach { s ->
                    s.grams = (s.grams * factor).coerceIn(MIN_GRAMS, MAX_GRAMS)
                }
            }
        }

        // Arrotonda al grammo: e cio che l'utente legge e pesa.
        slots.forEach { it.grams = it.grams.coerceIn(MIN_GRAMS, MAX_GRAMS) }

        val solvedMeals = structure.mapIndexed { mealIndex, (slotName, _) ->
            val entries = slots.filter { it.mealIndex == mealIndex }.map { s ->
                val f = s.grams / 100.0
                SolvedEntry(
                    foodName = s.food.name,
                    grams = kotlin.math.round(s.grams),
                    kcal = s.food.kcalPer100g * f,
                    protein = s.food.proteinPer100g * f,
                    carbs = s.food.carbsPer100g * f,
                    fat = s.food.fatPer100g * f
                )
            }
            SolvedMeal(slotName, entries)
        }

        val actual = solvedMeals.fold(MacroTotals.ZERO) { sum, m -> sum + m.totals }
        val deviations = MacroTotals(
            actual.kcal - targetMacros.kcal,
            actual.protein - targetMacros.protein,
            actual.carbs - targetMacros.carbs,
            actual.fat - targetMacros.fat
        )

        // Diagnostica utile al modello: se manca un ruolo non c'e modo di
        // centrare quel macro, e va detto esplicitamente invece di lasciarlo
        // scoprire da un ok=false.
        val roles = slots.map { it.role }.toSet()
        if (Role.PROTEIN !in roles) warnings += "IMPOSSIBILE centrare le proteine: nessuna fonte proteica nella giornata."
        if (Role.CARBS !in roles && targetMacros.carbs > 5.0) {
            warnings += "IMPOSSIBILE centrare i carboidrati: nessuna fonte di carboidrati nella giornata."
        }
        if (Role.FAT !in roles && targetMacros.fat > 3.0) {
            warnings += "IMPOSSIBILE centrare i grassi: nessuna fonte di grassi nella giornata."
        }
        // Vincolo fisico non aggirabile: un alimento puo essere ridotto fino a 1 g,
        // ma non sotto. Se anche a 1 g il suo macro trascina oltre il target, la
        // giornata non e centrabile con QUESTI alimenti e va detto.
        val tight = slots.filter { it.grams <= MIN_GRAMS + 0.5 }
        if (tight.isNotEmpty()) {
            val excess = tight.joinToString { s ->
                val f = s.grams / 100.0
                "%s (%.0f g: %.0f kcal, %.0f g P, %.0f g C, %.0f g G)".format(
                    s.food.name, s.grams,
                    s.food.kcalPer100g * f, s.food.proteinPer100g * f,
                    s.food.carbsPer100g * f, s.food.fatPer100g * f
                )
            }
            warnings += "IMPOSSIBILE centrare il target: alimenti al minimo (1 g) gia troppo " +
                "densi per questa giornata -> $excess. Sostituisci le fonti con alternative piu " +
                "magre o a minor contenuto di carboidrati."
        }
        if (slots.any { it.grams >= MAX_GRAMS - 0.5 }) {
            warnings += "Alcune grammature sono al massimo (2000 g): servono piu alimenti o pasti."
        }

        val withinTolerance = abs(deviations.kcal) <= targetMacros.kcal * TARGET_TOLERANCE &&
            abs(deviations.protein) <= targetMacros.protein * TARGET_TOLERANCE &&
            abs(deviations.carbs) <= maxOf(targetMacros.carbs * TARGET_TOLERANCE, 5.0) &&
            abs(deviations.fat) <= maxOf(targetMacros.fat * TARGET_TOLERANCE, 3.0)

        return Result(
            meals = solvedMeals,
            actual = actual,
            target = targetMacros,
            deviations = deviations,
            withinTolerance = withinTolerance,
            warnings = warnings
        )
    }
}
