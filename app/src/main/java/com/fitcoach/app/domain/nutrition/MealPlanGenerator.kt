package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generatore di piani alimentari.
 *
 * Dato un piano di carb cycling e un database alimentare, costruisce 7 giorni
 * di pasti con grammature reali. Il sistema:
 *  1. distribuisce i macro sui pasti secondo una ripartizione per slot;
 *  2. per ogni pasto sceglie gli alimenti in base al ruolo (proteina, carboidrato,
 *     grasso, verdura/frutta);
 *  3. calcola i grammi per centrare il macro bersaglio del pasto;
 *  4. valida che il totale del giorno sia coerente col target.
 */
object MealPlanGenerator {

    /** Ripartizione dei macro sui 5 pasti (frazione di P, C, F). */
    private data class SlotShare(val slot: MealSlot, val p: Double, val c: Double, val f: Double)

    private val defaultShares = listOf(
        SlotShare(MealSlot.BREAKFAST, 0.20, 0.25, 0.30),
        SlotShare(MealSlot.MORNING_SNACK, 0.15, 0.10, 0.10),
        SlotShare(MealSlot.LUNCH, 0.30, 0.30, 0.25),
        SlotShare(MealSlot.AFTERNOON_SNACK, 0.15, 0.10, 0.10),
        SlotShare(MealSlot.DINNER, 0.20, 0.25, 0.25)
    )

    /** Ripartizione per 3 pasti (colazione, pranzo, cena). */
    private val threeMealShares = listOf(
        SlotShare(MealSlot.BREAKFAST, 0.30, 0.30, 0.30),
        SlotShare(MealSlot.LUNCH, 0.40, 0.40, 0.35),
        SlotShare(MealSlot.DINNER, 0.30, 0.30, 0.35)
    )

    data class GeneratedMeal(
        val slot: MealSlot,
        val entries: List<MealEntry>
    ) {
        val kcal: Double get() = entries.sumOf { it.kcal }
        val protein: Double get() = entries.sumOf { it.protein }
        val carbs: Double get() = entries.sumOf { it.carbs }
        val fat: Double get() = entries.sumOf { it.fat }
        val totals: MacroTotals get() = MacroTotals(kcal, protein, carbs, fat)
    }

    data class GeneratedDay(
        val dayIndex: Int,
        val dayType: CarbDayType,
        val target: CarbDayTarget,
        val meals: List<GeneratedMeal>,
        val actual: MacroTotals,
        val deviations: MacroTotals,
        val isCoherent: Boolean
    )

    /** Seleziona alimenti per ruolo, gia filtrati per restrizioni. */
    private data class FoodBank(
        val proteins: List<FoodItem>,
        val carbs: List<FoodItem>,
        val fats: List<FoodItem>,
        val veggies: List<FoodItem>,
        val fruits: List<FoodItem>,
        val dairy: List<FoodItem>
    )

    /** Densita minima di carboidrati (g/100g) per la fonte glucidica. */
    private const val MIN_CARB_DENSITY = 30.0

    /** Densita minima di grassi (g/100g) per la fonte lipidica. */
    private const val MIN_FAT_DENSITY = 60.0

    /** Soglia di carboidrati per una fonte proteica "magra". */
    private const val LEAN_PROTEIN_MAX_CARBS = 15.0

    /** Soglia di grassi per una fonte proteica "magra". */
    private const val LEAN_PROTEIN_MAX_FAT = 10.0

    /** Densita proteica minima per usare un alimento come fonte proteica principale. */
    private const val MIN_PROTEIN_DENSITY = 15.0

    /** Numero massimo di fonti proteiche principali (varieta senza perdere densita). */
    private const val MAX_PRIMARY_PROTEINS = 8

    private fun buildBank(allowed: List<FoodItem>): FoodBank {
        fun byCat(vararg cats: String) = allowed.filter { it.category in cats }
        val allCarbs = byCat("Cereali", "Tuberi")
        val allFats = byCat("Grassi", "Frutta secca")
        // Fonti "pure": evitiamo alimenti proteici tra carboidrati e grassi
        // (es. seitan, legumi, frutta secca proteica) per non sbilanciare le proteine.
        val denseCarbs = allCarbs.filter { it.carbsPer100g >= MIN_CARB_DENSITY }
        val denseFats = allFats.filter { it.fatPer100g >= MIN_FAT_DENSITY && it.proteinPer100g <= 20 }

        val proteinPool = byCat("Carne", "Pesce", "Uova", "Integratori") +
            allowed.filter { it.category == "Latticini" && it.proteinPer100g >= 8 } +
            byCat("Legumi")
        // Preferiamo fonti proteiche magre e dense: i legumi secchi (50 g carbo/100 g),
        // i latticini grassi (ricotta, 11 g grassi/100 g) o alimenti poco densi (tofu)
        // sbilancerebbero il pasto o richiederebbero porzioni troppo grandi.
        val leanProteins = proteinPool.filter {
            it.carbsPer100g <= LEAN_PROTEIN_MAX_CARBS &&
                it.fatPer100g <= LEAN_PROTEIN_MAX_FAT &&
                it.proteinPer100g >= MIN_PROTEIN_DENSITY
        }
        val moderateProteins = proteinPool.filter { it.carbsPer100g <= LEAN_PROTEIN_MAX_CARBS }
        // Limitiamo il numero di fonti principali per garantire varieta senza
        // includere alimenti troppo grassi o poco densi.
        val primaryProteins = (leanProteins.ifEmpty { moderateProteins }.ifEmpty { proteinPool })
            .sortedByDescending { it.proteinPer100g / (1 + it.fatPer100g * 0.1) }
            .take(MAX_PRIMARY_PROTEINS)

        // I legumi non compaiono come fonte glucidica principale se esistono cereali/tuberi,
        // perche apportano anche proteine che sbilanciano il pasto.
        val carbsPool = denseCarbs.filter { it.category != "Legumi" }

        return FoodBank(
            proteins = primaryProteins,
            carbs = carbsPool.ifEmpty { denseCarbs }.ifEmpty { allCarbs },
            fats = denseFats.ifEmpty { allFats },
            veggies = byCat("Verdura"),
            fruits = byCat("Frutta"),
            dairy = byCat("Latticini")
        )
    }

    fun generate(
        profile: UserProfile,
        cycling: CarbCyclingPlan,
        foodDatabase: List<FoodItem>,
        dayTypes: List<CarbDayType>,
        startDayIndex: Int = 0
    ): List<GeneratedDay> {
        val allowed = FoodFilter.allowedFoods(foodDatabase, profile)
        val bank = buildBank(allowed)
        val shares = if (profile.mealsPerDay <= 3) threeMealShares else defaultShares

        return (0 until 7).map { d ->
            val type = dayTypes.getOrElse(d) { CarbDayType.MEDIUM }
            val target = cycling.targets[type] ?: cycling.targets[CarbDayType.MEDIUM]!!
            val meals = shares.mapIndexed { i, share ->
                buildMeal(share, target, bank, i, d)
            }
            val actual = MacroTotals(
                kcal = meals.sumOf { it.kcal },
                protein = meals.sumOf { it.protein },
                carbs = meals.sumOf { it.carbs },
                fat = meals.sumOf { it.fat }
            )
            val deviations = MacroTotals(
                kcal = actual.kcal - target.kcal,
                protein = actual.protein - target.protein,
                carbs = actual.carbs - target.carbs,
                fat = actual.fat - target.fat
            )
            GeneratedDay(
                dayIndex = d,
                dayType = type,
                target = target,
                meals = meals,
                actual = actual,
                deviations = deviations,
                isCoherent = abs(deviations.kcal) <= 120 &&
                    abs(deviations.protein) <= 12 &&
                    abs(deviations.carbs) <= 25 &&
                    abs(deviations.fat) <= 12
            )
        }
    }

    private fun buildMeal(
        share: SlotShare,
        target: CarbDayTarget,
        bank: FoodBank,
        mealIndex: Int,
        dayIndex: Int
    ): GeneratedMeal {
        val pTarget = target.protein * share.p
        val cTarget = target.carbs * share.c
        val fTarget = target.fat * share.f
        val entries = mutableListOf<MealEntry>()

        // 0) Verdura o frutta: quantita fissa ragionevole, contata nei macro del pasto.
        if (mealIndex % 2 == 0) {
            pick(bank.veggies, dayIndex + mealIndex)?.let { entries += entry(it, 150.0) }
        } else {
            pick(bank.fruits, dayIndex + mealIndex)?.let { entries += entry(it, 120.0) }
        }

        // 1) Fonte di carboidrati: grammi per il target glucidico residuo.
        val carbItem = pick(bank.carbs, dayIndex * 3 + mealIndex)
        val carbsAlready = entries.sumOf { it.carbs }
        val remainingCarbs = (cTarget - carbsAlready).coerceAtLeast(0.0)
        if (carbItem != null && remainingCarbs >= 5.0) {
            val grams = gramsForMacro(remainingCarbs, carbItem.carbsPer100g).coerceIn(15.0, 350.0)
            entries += entry(carbItem, grams)
        }

        // 2) Proteina principale: grammi per il target proteico residuo,
        //    tenendo conto delle proteine gia apportate da verdura/frutta e carboidrati.
        val protein = pick(bank.proteins, dayIndex * 5 + mealIndex)
        val proteinAlready = entries.sumOf { it.protein }
        val remainingProtein = (pTarget - proteinAlready).coerceAtLeast(0.0)
        if (protein != null && remainingProtein > 1.0) {
            val grams = gramsForMacro(remainingProtein, protein.proteinPer100g).coerceIn(20.0, 400.0)
            entries += entry(protein, grams)
        }

        // 3) Fonte di grassi: grammi per il target lipidico residuo.
        val fatItem = pick(bank.fats, dayIndex * 2 + mealIndex)
        val fatAlready = entries.sumOf { it.fat }
        val remainingFat = (fTarget - fatAlready).coerceAtLeast(0.0)
        if (fatItem != null && remainingFat >= 1.0) {
            val grams = gramsForMacro(remainingFat, fatItem.fatPer100g).coerceIn(2.0, 60.0)
            entries += entry(fatItem, grams)
        }

        return GeneratedMeal(share.slot, entries)
    }

    private fun pick(list: List<FoodItem>, seed: Int): FoodItem? {
        if (list.isEmpty()) return null
        val idx = ((seed % list.size) + list.size) % list.size
        return list[idx]
    }

    fun gramsForMacro(targetGrams: Double, per100g: Double): Double {
        if (per100g <= 0.0) return 0.0
        return (targetGrams / per100g * 100.0)
    }

    private fun entry(food: FoodItem, grams: Double): MealEntry {
        val factor = grams / 100.0
        val roundedGrams = grams.roundToInt().toDouble()
        return MealEntry(
            foodItemId = food.id,
            foodName = food.name,
            grams = roundedGrams,
            unit = food.defaultUnit,
            kcal = food.kcalPer100g * factor,
            protein = food.proteinPer100g * factor,
            carbs = food.carbsPer100g * factor,
            fat = food.fatPer100g * factor
        )
    }

    /**
     * Calcola i grammi di un sostituto per mantenere lo stesso apporto del
     * macro dominante dell'alimento originale.
     */
    fun substituteGrams(
        original: FoodItem,
        originalGrams: Double,
        substitute: FoodItem
    ): Double {
        val dominant = dominantMacro(original)
        val originalMacroAmount = macroValue(original, dominant) * originalGrams / 100.0
        val per100 = macroValue(substitute, dominant)
        return if (per100 <= 0.0) originalGrams else (originalMacroAmount / per100 * 100.0)
    }

    enum class DominantMacro { PROTEIN, CARBS, FAT }

    fun dominantMacro(food: FoodItem): DominantMacro {
        val pKcal = food.proteinPer100g * 4
        val cKcal = food.carbsPer100g * 4
        val fKcal = food.fatPer100g * 9
        return when (maxOf(pKcal, cKcal, fKcal)) {
            pKcal -> DominantMacro.PROTEIN
            cKcal -> DominantMacro.CARBS
            else -> DominantMacro.FAT
        }
    }

    private fun macroValue(food: FoodItem, macro: DominantMacro): Double = when (macro) {
        DominantMacro.PROTEIN -> food.proteinPer100g
        DominantMacro.CARBS -> food.carbsPer100g
        DominantMacro.FAT -> food.fatPer100g
    }

    fun unitLabel(unit: FoodUnit): String = when (unit) {
        FoodUnit.RAW -> "crudo"
        FoodUnit.COOKED -> "cotto"
        FoodUnit.AS_SOLD -> "tal quale"
    }
}
