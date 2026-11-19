package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.MealPortionSolver
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Costruisce una giornata alimentare con grammature calcolate.
 *
 * L'AI fornisce la struttura (quali alimenti, in quali pasti); lo strumento
 * risolve i grammi sul target reale della giornata e restituisce le voci pronte
 * da inserire in `submit_full_program`. Senza questo passaggio l'AI deve
 * centrare a mano quattro macro su ~35 pasti e il piano viene bocciato.
 */
@Singleton
class SolveMealPortionsTool @Inject constructor(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository
) : CoachTool {
    override val name = "solve_meal_portions"

    override val description =
        "Calcola le grammature esatte di una giornata alimentare per centrare il target della " +
            "giornata (kcal, proteine, carboidrati, grassi). Tu scegli gli alimenti e i pasti; lo " +
            "strumento calcola i grammi sui valori reali del catalogo e restituisce le voci pronte " +
            "per submit_full_program. Usalo per OGNI giornata: non calcolare i grammi a mano."

    override val parameters: JsonObject = Schema.obj(
        "dayIndex" to Schema.integer("Giorno 0-6."),
        "dayType" to Schema.enumOf("Tipologia della giornata.", "HIGH", "MEDIUM", "LOW"),
        "pasti" to Schema.array(
            "Pasti della giornata, ciascuno con gli alimenti scelti.",
            Schema.obj(
                "slot" to Schema.enumOf(
                    "Slot del pasto.",
                    "BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER", "EVENING_SNACK"
                ),
                "alimenti" to Schema.arrayOfStrings("Nomi degli alimenti (dal catalogo)."),
                "grammiIniziali" to Schema.string(
                    "Grammature di partenza opzionali, formato 'alimento=grammi;alimento=grammi'."
                )
            )
        ),
        required = listOf("dayIndex", "dayType", "pasti")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val profile = userRepository.get()
            ?: return err("Nessun profilo: completa prima l'anamnesi.")
        val energy = EnergyCalculator.compute(profile)
            ?: return err("Dati insufficienti (età/altezza/peso) per calcolare i target.")

        val a = JsonArgs(arguments)
        val dayIndex = a.int("dayIndex") ?: return err("dayIndex mancante")
        if (dayIndex !in 0..6) return err("dayIndex deve essere 0-6.")
        val dayType = a.string("dayType")?.let { raw ->
            runCatching { CarbDayType.valueOf(raw.trim().uppercase()) }.getOrNull()
        } ?: return err("dayType non valido: usa HIGH, MEDIUM o LOW.")

        val cycling = if (CarbCyclingEngine.shouldApplyCarbCycling(profile)) {
            CarbCyclingEngine.build(profile, energy)
        } else {
            CarbCyclingEngine.buildUniform(profile, energy)
        }
        val target = cycling.targets[dayType]
            ?: return err("Target non disponibile per la giornata $dayType.")

        val mealObjects = a.arrayAt("pasti") ?: return err("pasti mancanti")
        val all = foodRepository.getAll()
        val unknown = mutableListOf<String>()
        val structure = mutableListOf<Pair<String, List<FoodItem>>>()
        val initialGrams = mutableMapOf<String, Double>()

        mealObjects.forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            val slot = (obj["slot"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: return@forEach
            val names = (obj["alimenti"] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                .orEmpty()
            val foods = names.mapNotNull { name ->
                val food = all.firstOrNull { it.normalizedName == FoodItem.normalizeName(name) }
                if (food == null) unknown += name
                food
            }
            (obj["grammiIniziali"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { raw ->
                raw.split(";").forEach { pair ->
                    val parts = pair.split("=")
                    if (parts.size == 2) {
                        val g = parts[1].trim().toDoubleOrNull()
                        if (g != null) initialGrams[parts[0].trim()] = g
                    }
                }
            }
            if (foods.isNotEmpty()) structure += slot.uppercase() to foods
        }

        if (structure.isEmpty()) {
            return err("Nessun alimento valido nei pasti: " + unknown.joinToString())
        }

        val result = MealPortionSolver.solve(target, structure, initialGrams)

        val mealsJson = result.meals.map { meal ->
            JsonOut.obj(
                "slot" to JsonOut.str(meal.slot),
                "totaleKcal" to JsonOut.num(meal.totals.kcal),
                "totaleProteine" to JsonOut.num(meal.totals.protein),
                "totaleCarboidrati" to JsonOut.num(meal.totals.carbs),
                "totaleGrassi" to JsonOut.num(meal.totals.fat),
                "voci" to JsonOut.arr(
                    meal.entries.map { e ->
                        JsonOut.obj(
                            "foodName" to JsonOut.str(e.foodName),
                            "grams" to JsonOut.num(e.grams),
                            "kcal" to JsonOut.num(e.kcal),
                            "proteine" to JsonOut.num(e.protein),
                            "carboidrati" to JsonOut.num(e.carbs),
                            "grassi" to JsonOut.num(e.fat)
                        )
                    }
                )
            )
        }

        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(result.ok),
                "dayIndex" to JsonOut.num(dayIndex),
                "dayType" to JsonOut.str(dayType.name),
                "target" to JsonOut.obj(
                    "kcal" to JsonOut.num(target.kcal),
                    "proteine" to JsonOut.num(target.protein),
                    "carboidrati" to JsonOut.num(target.carbs),
                    "grassi" to JsonOut.num(target.fat)
                ),
                "ottenuto" to JsonOut.obj(
                    "kcal" to JsonOut.num(result.actual.kcal),
                    "proteine" to JsonOut.num(result.actual.protein),
                    "carboidrati" to JsonOut.num(result.actual.carbs),
                    "grassi" to JsonOut.num(result.actual.fat)
                ),
                "scostamento" to JsonOut.obj(
                    "kcal" to JsonOut.num(result.deviations.kcal),
                    "proteine" to JsonOut.num(result.deviations.protein),
                    "carboidrati" to JsonOut.num(result.deviations.carbs),
                    "grassi" to JsonOut.num(result.deviations.fat)
                ),
                "alimentiSconosciuti" to JsonOut.strings(unknown),
                "avvisi" to JsonOut.strings(result.warnings),
                "pasti" to JsonOut.arr(mealsJson),
                "nota" to JsonOut.str(
                    if (result.ok) {
                        "Usa esattamente questi 'foodName' e 'grams' per il giorno $dayIndex in " +
                            "submit_full_program."
                    } else {
                        "Non centrato: correggi la scelta degli alimenti (serve una fonte per ogni " +
                            "macro) e richiama solve_meal_portions."
                    }
                )
            )
        )
    }

    private fun err(m: String) = JsonOut.encode(
        JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str(m))
    )
}

/**
 * Espone i target nutrizionali che il validatore applichera.
 *
 * L'AI non puo rispettare vincoli che non conosce: senza questa tabella il
 * modello inventava target propri e ogni giornata veniva bocciata.
 */
@Singleton
class GetNutritionTargetsTool @Inject constructor(
    private val userRepository: UserRepository
) : CoachTool {
    override val name = "get_nutrition_targets"

    override val description =
        "Restituisce i target ESATTI (kcal, proteine, carboidrati, grassi) di ogni tipologia di " +
            "giornata HIGH/MEDIUM/LOW e quali giorni sono HIGH, MEDIUM o LOW. Questi valori sono " +
            "vincolanti: submit_full_program li verifica. Consultalo prima di costruire il piano."

    override val parameters: JsonObject = Schema.obj()

    override suspend fun execute(arguments: JsonObject): String {
        val profile = userRepository.get()
            ?: return err("Nessun profilo: completa prima l'anamnesi.")
        val energy = EnergyCalculator.compute(profile)
            ?: return err("Dati insufficienti (età/altezza/peso) per calcolare i target.")

        val applied = CarbCyclingEngine.shouldApplyCarbCycling(profile)
        val cycling = if (applied) CarbCyclingEngine.build(profile, energy)
        else CarbCyclingEngine.buildUniform(profile, energy)

        val targets = cycling.targets.map { (type, t) ->
            JsonOut.obj(
                "tipo" to JsonOut.str(type.name),
                "kcal" to JsonOut.num(t.kcal),
                "proteine" to JsonOut.num(t.protein),
                "carboidrati" to JsonOut.num(t.carbs),
                "grassi" to JsonOut.num(t.fat)
            )
        }

        val trainingDays = (0 until profile.trainingDaysPerWeek.coerceIn(0, 7)).toList()
        val assignment = (0..6).map { index ->
            JsonOut.obj(
                "dayIndex" to JsonOut.num(index),
                "allenamento" to JsonOut.bool(index in trainingDays),
                "dayTypeSuggerito" to JsonOut.str(suggestDayType(cycling, index in trainingDays))
            )
        }

        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "carbCyclingApplicato" to JsonOut.bool(applied),
                "giornateHigh" to JsonOut.num(cycling.highDays),
                "giornateMedium" to JsonOut.num(cycling.mediumDays),
                "giornateLow" to JsonOut.num(cycling.lowDays),
                "kcalSettimanali" to JsonOut.num(cycling.weeklyKcal),
                "target" to JsonOut.arr(targets),
                "calendario" to JsonOut.arr(assignment),
                "tolleranzaValidatore" to JsonOut.str(
                    "kcal ±20%, proteine ±25%, carboidrati ±30%, grassi ±30% sul target della giornata"
                ),
                "avvisi" to JsonOut.strings(cycling.warnings)
            )
        )
    }

    private fun suggestDayType(cycling: com.fitcoach.app.domain.model.CarbCyclingPlan, training: Boolean): String {
        if (!cycling.targets.containsKey(CarbDayType.HIGH)) return CarbDayType.MEDIUM.name
        return if (training) CarbDayType.HIGH.name else CarbDayType.LOW.name
    }

    private fun err(m: String) = JsonOut.encode(
        JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str(m))
    )
}
