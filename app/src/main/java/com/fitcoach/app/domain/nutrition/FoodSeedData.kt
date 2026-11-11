package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit

/**
 * Database alimentare seed.
 *
 * FONTI dei valori nutrizionali (per 100 g di parte edibile):
 *  - CREA – Consiglio per la ricerca in agricoltura e l'analisi dell'economia agraria
 *    (ex INRAN), Tabelle di composizione degli alimenti.
 *  - USDA FoodData Central per gli alimenti non presenti nelle tabelle CREA.
 *
 * Ogni voce dichiara la fonte in [FoodItem.source]. I valori sono per 100 g di
 * prodotto CRUDO salvo diversa indicazione nella colonna unit.
 */
object FoodSeedData {

    const val SRC_CREA = "CREA – Tabelle di composizione degli alimenti"
    const val SRC_USDA = "USDA FoodData Central"

    val foods: List<FoodItem> = listOf(
        // ---------- Fonti proteiche ----------
        f("Petto di pollo", "Carne", 110.0, 23.3, 0.0, 1.5, SRC_CREA,
            subs = listOf("Fesa di tacchino", "Merluzzo", "Tonno al naturale")),
        f("Fesa di tacchino", "Carne", 108.0, 24.0, 0.0, 0.8, SRC_CREA,
            subs = listOf("Petto di pollo", "Merluzzo", "Lonza di maiale")),
        f("Lonza di maiale", "Carne", 143.0, 21.2, 0.0, 6.4, SRC_CREA,
            subs = listOf("Fesa di tacchino", "Petto di pollo")),
        f("Fesa di manzo", "Carne", 130.0, 21.5, 0.0, 4.5, SRC_CREA,
            subs = listOf("Lonza di maiale", "Petto di pollo")),
        f("Carne macinata di manzo (magra)", "Carne", 152.0, 20.0, 0.0, 8.0, SRC_USDA,
            subs = listOf("Fesa di manzo", "Fesa di tacchino")),
        f("Uovo intero di gallina", "Uova", 128.0, 12.4, 0.7, 8.7, SRC_CREA,
            subs = listOf("Albume d'uovo")),
        f("Albume d'uovo", "Uova", 43.0, 10.7, 0.7, 0.2, SRC_CREA,
            subs = listOf("Uovo intero di gallina")),
        f("Merluzzo", "Pesce", 82.0, 17.7, 0.0, 0.8, SRC_CREA,
            subs = listOf("Nasello", "Platessa", "Orata")),
        f("Nasello", "Pesce", 71.0, 16.8, 0.0, 0.3, SRC_CREA,
            subs = listOf("Merluzzo", "Platessa")),
        f("Salmone", "Pesce", 185.0, 18.4, 0.0, 12.3, SRC_USDA,
            subs = listOf("Sgombro", "Trota")),
        f("Orata", "Pesce", 121.0, 18.2, 0.0, 5.6, SRC_CREA,
            subs = listOf("Branzino", "Merluzzo")),
        f("Branzino", "Pesce", 97.0, 17.5, 0.0, 3.5, SRC_CREA,
            subs = listOf("Orata", "Merluzzo")),
        f("Tonno in scatola al naturale", "Pesce", 116.0, 25.5, 0.0, 1.0, SRC_CREA,
            subs = listOf("Petto di pollo", "Merluzzo")),
        f("Gamberi", "Pesce", 85.0, 16.8, 0.5, 1.3, SRC_USDA,
            subs = listOf("Merluzzo", "Calamari")),
        f("Yogurt greco 0% grassi", "Latticini", 59.0, 10.0, 3.6, 0.4, SRC_USDA,
            subs = listOf("Yogurt bianco magro", "Fiocchi di latte")),
        f("Yogurt bianco magro", "Latticini", 56.0, 5.2, 4.3, 1.5, SRC_CREA,
            subs = listOf("Yogurt greco 0% grassi")),
        f("Fiocchi di latte (cottage)", "Latticini", 98.0, 11.1, 3.4, 4.3, SRC_USDA,
            subs = listOf("Ricotta vaccina", "Yogurt greco 0% grassi")),
        f("Ricotta vaccina", "Latticini", 146.0, 8.8, 2.8, 10.9, SRC_CREA,
            subs = listOf("Fiocchi di latte (cottage)")),
        f("Latte parzialmente scremato", "Latticini", 46.0, 3.4, 4.8, 1.6, SRC_CREA,
            subs = listOf("Bevanda di soia non zuccherata")),
        f("Latte scremato", "Latticini", 34.0, 3.4, 5.0, 0.1, SRC_CREA,
            subs = listOf("Latte parzialmente scremato")),
        f("Bevanda di soia non zuccherata", "Bevande vegetali", 33.0, 3.3, 0.7, 1.8, SRC_USDA,
            subs = listOf("Latte scremato", "Latte parzialmente scremato")),
        f("Proteine del siero (whey, polvere)", "Integratori", 380.0, 80.0, 7.0, 5.0, SRC_USDA,
            subs = listOf("Proteine vegetali (polvere)")),
        f("Proteine vegetali (polvere)", "Integratori", 360.0, 75.0, 10.0, 4.0, SRC_USDA,
            subs = listOf("Proteine del siero (whey, polvere)")),
        f("Tofu", "Legumi", 133.0, 12.0, 3.5, 8.1, SRC_USDA,
            subs = listOf("Tempeh", "Seitan")),
        f("Tempeh", "Legumi", 195.0, 19.9, 7.6, 11.4, SRC_USDA,
            subs = listOf("Tofu", "Seitan")),
        f("Seitan", "Cereali", 141.0, 25.0, 4.0, 2.0, SRC_USDA,
            subs = listOf("Tofu", "Tempeh")),
        f("Prosciutto crudo sgrassato", "Salumi", 165.0, 27.0, 0.0, 6.0, SRC_CREA,
            subs = listOf("Bresaola")),
        f("Bresaola", "Salumi", 151.0, 32.0, 0.4, 2.6, SRC_CREA,
            subs = listOf("Prosciutto crudo sgrassato")),

        // ---------- Cereali e amidacei ----------
        f("Riso bianco (crudo)", "Cereali", 332.0, 6.7, 80.4, 0.4, SRC_CREA,
            subs = listOf("Riso basmati (crudo)", "Cous cous (crudo)")),
        f("Riso basmati (crudo)", "Cereali", 349.0, 8.0, 77.0, 1.0, SRC_USDA,
            subs = listOf("Riso bianco (crudo)")),
        f("Riso integrale (crudo)", "Cereali", 337.0, 7.5, 72.0, 2.5, SRC_CREA,
            subs = listOf("Riso bianco (crudo)")),
        f("Pasta di semola (cruda)", "Cereali", 353.0, 11.7, 79.1, 1.5, SRC_CREA,
            subs = listOf("Pasta integrale (cruda)", "Riso bianco (crudo)")),
        f("Pasta integrale (cruda)", "Cereali", 348.0, 13.0, 71.0, 2.5, SRC_CREA,
            subs = listOf("Pasta di semola (cruda)")),
        f("Cous cous (crudo)", "Cereali", 358.0, 12.0, 72.0, 1.5, SRC_USDA,
            subs = listOf("Riso bianco (crudo)")),
        f("Fiocchi d'avena", "Cereali", 367.0, 13.0, 66.3, 6.5, SRC_USDA,
            subs = listOf("Muesli senza zuccheri")),
        f("Pane bianco", "Cereali", 275.0, 8.6, 55.0, 1.5, SRC_CREA,
            subs = listOf("Pane integrale")),
        f("Pane integrale", "Cereali", 240.0, 8.0, 45.0, 2.5, SRC_CREA,
            subs = listOf("Pane bianco")),
        f("Fette biscottate", "Cereali", 408.0, 10.7, 74.0, 6.0, SRC_CREA,
            subs = listOf("Pane bianco")),
        f("Patate", "Tuberi", 79.0, 2.0, 17.5, 0.1, SRC_CREA,
            subs = listOf("Patate dolci")),
        f("Patate dolci", "Tuberi", 86.0, 1.6, 20.1, 0.1, SRC_USDA,
            subs = listOf("Patate")),
        f("Polenta (farina di mais cruda)", "Cereali", 362.0, 8.0, 76.0, 3.0, SRC_CREA,
            subs = listOf("Riso bianco (crudo)")),

        // ---------- Legumi ----------
        f("Lenticchie secche (crude)", "Legumi", 325.0, 25.0, 51.0, 2.0, SRC_CREA,
            subs = listOf("Ceci secchi (crudi)", "Fagioli secchi (crudi)")),
        f("Ceci secchi (crudi)", "Legumi", 343.0, 20.0, 56.0, 5.0, SRC_CREA,
            subs = listOf("Lenticchie secche (crude)")),
        f("Fagioli secchi (crudi)", "Legumi", 311.0, 23.0, 47.0, 2.0, SRC_CREA,
            subs = listOf("Lenticchie secche (crude)")),
        f("Fagioli cannellini cotti", "Legumi", 91.0, 7.0, 13.0, 0.5, SRC_USDA,
            subs = listOf("Ceci cotti")),
        f("Ceci cotti", "Legumi", 120.0, 6.5, 18.0, 2.0, SRC_USDA,
            subs = listOf("Fagioli cannellini cotti")),

        // ---------- Grassi ----------
        f("Olio extravergine d'oliva", "Grassi", 899.0, 0.0, 0.0, 99.9, SRC_CREA,
            subs = listOf("Olio di semi")),
        f("Olio di semi", "Grassi", 900.0, 0.0, 0.0, 100.0, SRC_CREA,
            subs = listOf("Olio extravergine d'oliva")),
        f("Mandorle", "Frutta secca", 603.0, 22.0, 4.6, 55.3, SRC_CREA,
            subs = listOf("Noci", "Nocciole")),
        f("Noci", "Frutta secca", 689.0, 15.0, 3.3, 68.0, SRC_CREA,
            subs = listOf("Mandorle")),
        f("Nocciole", "Frutta secca", 655.0, 14.0, 6.0, 62.0, SRC_CREA,
            subs = listOf("Mandorle")),
        f("Burro d'arachidi (100% arachidi)", "Frutta secca", 588.0, 25.0, 20.0, 50.0, SRC_USDA,
            subs = listOf("Mandorle")),
        f("Avocado", "Frutta", 160.0, 2.0, 8.5, 14.7, SRC_USDA,
            subs = listOf("Olio extravergine d'oliva")),
        f("Semi di lino", "Frutta secca", 534.0, 18.3, 28.9, 42.2, SRC_USDA,
            subs = listOf("Semi di chia")),

        // ---------- Frutta ----------
        f("Mela", "Frutta", 52.0, 0.3, 13.8, 0.2, SRC_CREA,
            subs = listOf("Pera")),
        f("Pera", "Frutta", 57.0, 0.4, 15.2, 0.1, SRC_CREA,
            subs = listOf("Mela")),
        f("Banana", "Frutta", 89.0, 1.1, 22.8, 0.3, SRC_CREA,
            subs = listOf("Mela")),
        f("Arance", "Frutta", 47.0, 0.9, 11.7, 0.1, SRC_CREA,
            subs = listOf("Mandarini")),
        f("Fragole", "Frutta", 32.0, 0.7, 7.7, 0.3, SRC_CREA,
            subs = listOf("Lamponi")),
        f("Mirtilli", "Frutta", 57.0, 0.7, 14.5, 0.3, SRC_USDA,
            subs = listOf("Fragole")),
        f("Kiwi", "Frutta", 61.0, 1.1, 14.7, 0.5, SRC_CREA,
            subs = listOf("Arance")),

        // ---------- Verdura ----------
        f("Broccoli", "Verdura", 34.0, 2.8, 6.6, 0.4, SRC_CREA,
            subs = listOf("Cavolfiore", "Cime di rapa")),
        f("Cavolfiore", "Verdura", 25.0, 1.9, 5.0, 0.3, SRC_CREA,
            subs = listOf("Broccoli")),
        f("Spinaci", "Verdura", 31.0, 2.9, 3.6, 0.4, SRC_CREA,
            subs = listOf("Bietole")),
        f("Zucchine", "Verdura", 16.0, 1.2, 3.1, 0.2, SRC_CREA,
            subs = listOf("Melanzane")),
        f("Pomodori", "Verdura", 19.0, 1.0, 3.9, 0.2, SRC_CREA,
            subs = listOf("Pomodori pelati")),
        f("Insalata lattuga", "Verdura", 19.0, 1.4, 2.9, 0.3, SRC_CREA,
            subs = listOf("Rucola")),
        f("Rucola", "Verdura", 25.0, 2.6, 3.7, 0.7, SRC_CREA,
            subs = listOf("Insalata lattuga")),
        f("Peperoni", "Verdura", 26.0, 1.0, 5.0, 0.3, SRC_CREA,
            subs = listOf("Pomodori")),
        f("Carote", "Verdura", 41.0, 0.9, 9.6, 0.2, SRC_CREA,
            subs = listOf("Zucchine")),
        f("Funghi champignon", "Verdura", 22.0, 3.1, 3.3, 0.3, SRC_CREA,
            subs = listOf("Zucchine")),
        f("Fagiolini", "Verdura", 31.0, 1.8, 7.0, 0.2, SRC_CREA,
            subs = listOf("Broccoli"))
    )

    private fun f(
        name: String,
        category: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        source: String,
        fiber: Double = 0.0,
        unit: FoodUnit = FoodUnit.RAW,
        subs: List<String> = emptyList(),
        tags: List<String> = emptyList()
    ) = FoodItem(
        name = name,
        category = category,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        fiberPer100g = fiber,
        defaultUnit = unit,
        source = source,
        tags = tags,
        substituteNames = subs
    )
}
