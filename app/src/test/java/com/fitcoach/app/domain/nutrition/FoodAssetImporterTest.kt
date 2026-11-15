package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Verifica l'asset del database alimentare globale (USDA FoodData Central,
 * SR Legacy) confezionato nell'APK e importato a runtime da [FoodAssetImporter].
 *
 * Il test legge il file reale presente in `app/src/main/assets`, quindi valida
 * esattamente i dati che l'app installata caricherà.
 */
class FoodAssetImporterTest {

    private val assetFile = File("src/main/assets/food_usda_sr_legacy.tsv")

    private val foods: List<FoodItem> by lazy {
        assetFile.bufferedReader().use { reader ->
            FoodAssetImporter.parseTsv(reader, FoodAssetImporter.USDA_SOURCE)
        }
    }

    @Test
    fun `l asset esiste ed e confezionato`() {
        assertThat(assetFile.exists()).isTrue()
        assertThat(assetFile.length()).isGreaterThan(100_000L)
    }

    @Test
    fun `contiene un numero ampio di alimenti`() {
        assertThat(foods.size).isAtLeast(7000)
    }

    @Test
    fun `ogni alimento ha un nome non vuoto e fonte dichiarata`() {
        val invalid = foods.filter { it.name.isBlank() || it.source != FoodAssetImporter.USDA_SOURCE }
        assertThat(invalid).isEmpty()
    }

    @Test
    fun `nessun valore negativo`() {
        val invalid = foods.filter {
            it.kcalPer100g < 0 || it.proteinPer100g < 0 ||
                it.carbsPer100g < 0 || it.fatPer100g < 0 || it.fiberPer100g < 0
        }
        assertThat(invalid).isEmpty()
    }

    @Test
    fun `ogni alimento ha un identificativo di fonte univoco`() {
        val ids = foods.mapNotNull { it.sourceId }
        assertThat(ids).hasSize(foods.size)
        assertThat(ids.toSet()).hasSize(ids.size)
    }

    @Test
    fun `calorie coerenti con i macronutrienti per la grande maggioranza`() {
        // Le tabelle reali non seguono 4/4/9 in modo esatto: la fibra (~2 kcal/g),
        // i polialcoli e l'etanolo (7 kcal/g, non modellato da Atwater) producono
        // scostamenti legittimi. Si escludono le bevande alcoliche e si richiede
        // che almeno il 95% delle voci resti entro il 25%.
        val comparable = foods.filter { food ->
            val n = food.name.lowercase()
            "alcohol" !in n && "alcolic" !in n
        }
        val within = comparable.count { food ->
            val theoretical = 4.0 * food.proteinPer100g +
                4.0 * food.carbsPer100g +
                9.0 * food.fatPer100g
            theoretical <= 5.0 || abs(theoretical - food.kcalPer100g) / theoretical <= 0.25
        }
        val ratio = within.toDouble() / comparable.size
        assertThat(ratio).isAtLeast(0.95)
    }

    @Test
    fun `la ricerca copre alimenti comuni italiani per nome`() {
        // Almeno un alimento noto deve essere presente e ricercabile.
        val names = foods.map { FoodItem.normalizeName(it.name) }
        assertThat(names.any { it.contains("chicken") }).isTrue()
        assertThat(names.any { it.contains("apple") }).isTrue()
        assertThat(names.any { it.contains("rice") }).isTrue()
    }
}
