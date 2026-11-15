package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSubstitutionEngine
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calcola i grammi di un alimento necessari per raggiungere un obiettivo di
 * macro. E lo strumento con cui l'AI COSTRUISCE i pasti da sola, senza un
 * piano preimpostato: sceglie gli alimenti (con list_foods) e questo strumento
 * risolve le quantita reali sui valori nutrizionali del catalogo.
 */
@Singleton
class ComputePortionTool @Inject constructor(
    private val foodRepository: FoodRepository
) : CoachTool {
    override val name = "compute_portion"
    override val description =
        "Calcola i grammi di un alimento per centrare un obiettivo di macro (proteine, carboidrati " +
            "o grassi) e riporta kcal/macro risultanti. Usalo per costruire i pasti con quantita reali."

    override val parameters: JsonObject = Schema.obj(
        "foodName" to Schema.string("Nome dell'alimento (dal catalogo)."),
        "target" to Schema.enumOf("Macro da centrare.", "PROTEIN", "CARBS", "FAT", "KCAL"),
        "grammi" to Schema.number("Valore obiettivo del macro (g) o delle kcal.")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val name = a.string("foodName") ?: return err("foodName mancante")
        val target = a.string("target")?.uppercase() ?: "PROTEIN"
        val amount = a.double("grammi") ?: return err("grammi mancante")
        val food = foodRepository.getByName(name)
            ?: foodRepository.getAll().firstOrNull { it.normalizedName == FoodItem.normalizeName(name) }
            ?: return err("Alimento '$name' non presente nel catalogo.")

        val per100 = when (target) {
            "PROTEIN" -> food.proteinPer100g
            "CARBS" -> food.carbsPer100g
            "FAT" -> food.fatPer100g
            "KCAL" -> food.kcalPer100g
            else -> return err("target non valido: usa PROTEIN, CARBS, FAT o KCAL.")
        }
        if (per100 <= 0.0) {
            return err("'${food.name}' non contiene abbastanza $target per calcolare una porzione.")
        }
        val grams = (amount / per100 * 100.0).coerceIn(1.0, 2000.0)
        val factor = grams / 100.0
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "alimento" to JsonOut.str(food.name),
                "alimentoId" to JsonOut.num(food.id),
                "grammi" to JsonOut.num(grams),
                "unita" to JsonOut.str(food.defaultUnit.name),
                "kcal" to JsonOut.num(food.kcalPer100g * factor),
                "proteine" to JsonOut.num(food.proteinPer100g * factor),
                "carboidrati" to JsonOut.num(food.carbsPer100g * factor),
                "grassi" to JsonOut.num(food.fatPer100g * factor),
                "fonte" to JsonOut.str(food.source)
            )
        )
    }

    private fun err(m: String) = JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str(m)))
}

/**
 * Calcola la sostituzione di un alimento mantenendo il macro dominante e
 * verificando che pasto e giornata restino coerenti. L'AI lo usa per proporre
 * alternative corrette invece di indovinare le quantita.
 */
@Singleton
class ComputeSubstitutionTool @Inject constructor(
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository
) : CoachTool {
    override val name = "compute_substitution"
    override val description =
        "Calcola la quantita del sostituto per mantenere il macro dominante dell'alimento " +
            "originale e verifica la coerenza con il pasto e la giornata. Restituisce i grammi " +
            "corretti e le kcal/macro risultanti, oppure spiega perche la sostituzione non e equivalente."

    override val parameters: JsonObject = Schema.obj(
        "alimentoOriginale" to Schema.string("Nome dell'alimento da sostituire."),
        "grammiOriginali" to Schema.number("Grammi dell'alimento originale."),
        "alimentoSostituto" to Schema.string("Nome dell'alimento sostituto."),
        "macroPastoAltri" to Schema.string("Macro del resto del pasto, formato 'kcal,P,C,F' (opzionale)."),
        "targetGiorno" to Schema.string("Target giornaliero, formato 'kcal,P,C,F' (opzionale)."),
        required = listOf("alimentoOriginale", "grammiOriginali", "alimentoSostituto")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val originalName = a.string("alimentoOriginale") ?: return err("alimentoOriginale mancante")
        val grams = a.double("grammiOriginali") ?: return err("grammiOriginali mancante")
        val substituteName = a.string("alimentoSostituto") ?: return err("alimentoSostituto mancante")

        val all = foodRepository.getAll()
        val original = all.firstOrNull { it.normalizedName == FoodItem.normalizeName(originalName) }
            ?: return err("Alimento originale '$originalName' non trovato.")
        val substitute = all.firstOrNull { it.normalizedName == FoodItem.normalizeName(substituteName) }
            ?: return err("Alimento sostituto '$substituteName' non trovato.")

        val mealOther = parseMacros(a.string("macroPastoAltri"))
        val dayTarget = parseMacros(a.string("targetGiorno"))

        val result = FoodSubstitutionEngine.evaluate(
            FoodSubstitutionEngine.Request(
                original = original,
                originalGrams = grams,
                substitute = substitute,
                mealOther = mealOther,
                dayPlannedOther = com.fitcoach.app.domain.model.MacroTotals.ZERO,
                dayTarget = dayTarget,
                fatBalancers = all.filter { it.tags.any { t -> t.contains("grasso", true) } }.take(8),
                carbBalancers = all.filter { it.tags.any { t -> t.contains("carb", true) } }.take(8)
            )
        )
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "stato" to JsonOut.str(result.status.name),
                "grammiSostituto" to JsonOut.num(result.substituteGrams),
                "kcal" to JsonOut.num(result.substituteMacros.kcal),
                "proteine" to JsonOut.num(result.substituteMacros.protein),
                "carboidrati" to JsonOut.num(result.substituteMacros.carbs),
                "grassi" to JsonOut.num(result.substituteMacros.fat),
                "violazioni" to JsonOut.strings(result.violations),
                "note" to JsonOut.strings(result.notes)
            )
        )
    }

    private fun parseMacros(raw: String?): com.fitcoach.app.domain.model.MacroTotals {
        if (raw.isNullOrBlank()) return com.fitcoach.app.domain.model.MacroTotals.ZERO
        val parts = raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size < 4) return com.fitcoach.app.domain.model.MacroTotals.ZERO
        return com.fitcoach.app.domain.model.MacroTotals(parts[0], parts[1], parts[2], parts[3])
    }

    private fun err(m: String) = JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str(m)))
}

/**
 * Valida una giornata alimentare costruita dall'AI: verifica che kcal e macro
 * siano coerenti col target entro tolleranza e che non ci siano alimenti esclusi.
 */
@Singleton
class ValidateNutritionDayTool @Inject constructor(
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository
) : CoachTool {
    override val name = "validate_nutrition_day"
    override val description =
        "Valida una giornata alimentare proposta: somma kcal/macro delle voci, confronta con il " +
            "target e verifica allergie/esclusioni. Restituisce ok=true o gli scostamenti da correggere."

    override val parameters: JsonObject = Schema.obj(
        "targetKcal" to Schema.number("Obiettivo calorico della giornata."),
        "targetProteine" to Schema.number("Obiettivo proteine (g)."),
        "targetCarboidrati" to Schema.number("Obiettivo carboidrati (g)."),
        "targetGrassi" to Schema.number("Obiettivo grassi (g)."),
        "voci" to Schema.array(
            "Voci del pasto.",
            Schema.obj(
                "alimento" to Schema.string("Nome dell'alimento."),
                "grammi" to Schema.number("Grammi.")
            )
        ),
        required = listOf("targetKcal", "voci")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val a = JsonArgs(arguments)
        val targetKcal = a.double("targetKcal") ?: return err("targetKcal mancante")
        val targetP = a.double("targetProteine") ?: 0.0
        val targetC = a.double("targetCarboidrati") ?: 0.0
        val targetF = a.double("targetGrassi") ?: 0.0
        val entries = a.arrayAt("voci") ?: return err("voci mancanti")

        val profile = userRepository.get()
        val excluded = (profile?.excludedFoods.orEmpty() + profile?.allergies.orEmpty())
            .map { it.lowercase() }.filter { it.isNotBlank() }
        val all = foodRepository.getAll()

        var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        val unknown = mutableListOf<String>()
        val violations = mutableListOf<String>()
        entries.forEach { el ->
            val obj = el as? JsonObject ?: return@forEach
            val name = (obj["alimento"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@forEach
            val grams = (obj["grammi"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
            val food = all.firstOrNull { it.normalizedName == FoodItem.normalizeName(name) }
            if (food == null) { unknown += name; return@forEach }
            if (excluded.any { name.lowercase().contains(it) }) {
                violations += "'$name' e escluso o allergene per il soggetto."
            }
            val factor = grams / 100.0
            kcal += food.kcalPer100g * factor
            p += food.proteinPer100g * factor
            c += food.carbsPer100g * factor
            f += food.fatPer100g * factor
        }

        if (unknown.isNotEmpty()) violations += "Alimenti non trovati nel catalogo: ${unknown.joinToString()}."
        fun dev(actual: Double, target: Double, label: String, tol: Double) {
            if (target > 0 && kotlin.math.abs(actual - target) / target > tol) {
                violations += "$label: ${actual.toInt()} vs target ${target.toInt()} (fuori tolleranza ${(tol * 100).toInt()}%)"
            }
        }
        dev(kcal, targetKcal, "kcal", 0.10)
        dev(p, targetP, "proteine", 0.15)
        dev(c, targetC, "carboidrati", 0.15)
        dev(f, targetF, "grassi", 0.15)

        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(violations.isEmpty()),
                "kcal" to JsonOut.num(kcal),
                "proteine" to JsonOut.num(p),
                "carboidrati" to JsonOut.num(c),
                "grassi" to JsonOut.num(f),
                "violazioni" to JsonOut.strings(violations)
            )
        )
    }

    private fun err(m: String) = JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str(m)))
}
