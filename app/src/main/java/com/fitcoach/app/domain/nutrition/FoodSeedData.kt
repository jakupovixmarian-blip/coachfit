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

    private val rawFoods: List<FoodItem> = listOf(
        // ---------- Fonti proteiche ----------
        f("Petto di pollo", "Carne", 110.0, 23.3, 0.0, 1.5, SRC_CREA,
            subs = listOf("Fesa di tacchino", "Merluzzo", "Tonno in scatola al naturale")),
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
            subs = listOf("Yogurt bianco magro", "Fiocchi di latte (cottage)")),
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
            subs = listOf("Muesli senza zuccheri aggiunti")),
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
        f("Rucola", "Verdura", 25.0, 2.6, 2.1, 0.7, SRC_CREA,
            subs = listOf("Insalata lattuga")),
        f("Peperoni", "Verdura", 26.0, 1.0, 5.0, 0.3, SRC_CREA,
            subs = listOf("Pomodori")),
        f("Carote", "Verdura", 41.0, 0.9, 9.6, 0.2, SRC_CREA,
            subs = listOf("Zucchine")),
        f("Funghi champignon", "Verdura", 22.0, 3.1, 2.3, 0.3, SRC_CREA,
            subs = listOf("Zucchine")),
        f("Fagiolini", "Verdura", 31.0, 1.8, 7.0, 0.2, SRC_CREA,
            subs = listOf("Broccoli")),

        // ---------- Carni e salumi (ampliamento) ----------
        f("Petto di tacchino", "Carne", 111.0, 24.5, 0.0, 1.2, SRC_CREA,
            subs = listOf("Petto di pollo", "Fesa di tacchino")),
        f("Coscia di pollo (senza pelle)", "Carne", 118.0, 20.0, 0.0, 4.0, SRC_CREA,
            subs = listOf("Sovracoscia di pollo (senza pelle)")),
        f("Sovracoscia di pollo (senza pelle)", "Carne", 125.0, 19.0, 0.0, 5.5, SRC_CREA,
            subs = listOf("Coscia di pollo (senza pelle)")),
        f("Fesa di vitello", "Carne", 107.0, 20.5, 0.0, 2.5, SRC_CREA,
            subs = listOf("Fesa di manzo")),
        f("Girello di manzo", "Carne", 116.0, 21.0, 0.0, 3.4, SRC_CREA,
            subs = listOf("Fesa di manzo")),
        f("Roast beef (fesa cotta)", "Carne", 149.0, 27.0, 0.0, 4.5, SRC_USDA,
            subs = listOf("Bresaola", "Fesa di manzo")),
        f("Filetto di maiale", "Carne", 122.0, 21.0, 0.0, 3.8, SRC_CREA,
            subs = listOf("Lonza di maiale")),
        f("Bistecca di manzo (magra)", "Carne", 158.0, 21.0, 0.0, 8.0, SRC_CREA,
            subs = listOf("Fesa di manzo")),
        f("Carne macinata di pollo (magra)", "Carne", 143.0, 20.0, 0.0, 7.0, SRC_USDA,
            subs = listOf("Carne macinata di manzo (magra)")),
        f("Prosciutto cotto (sgrassato)", "Salumi", 108.0, 19.0, 0.5, 3.0, SRC_CREA,
            subs = listOf("Prosciutto crudo sgrassato", "Bresaola")),
        f("Speck", "Salumi", 300.0, 29.0, 0.5, 20.0, SRC_CREA,
            subs = listOf("Prosciutto crudo sgrassato")),
        f("Fesa di tacchino a fette", "Salumi", 104.0, 18.0, 1.0, 3.0, SRC_CREA,
            subs = listOf("Prosciutto cotto (sgrassato)")),

        // ---------- Pesce e frutti di mare (ampliamento) ----------
        f("Platessa", "Pesce", 82.0, 17.0, 0.0, 1.5, SRC_CREA,
            subs = listOf("Merluzzo", "Nasello")),
        f("Sogliola", "Pesce", 80.0, 17.5, 0.0, 1.2, SRC_CREA,
            subs = listOf("Platessa", "Merluzzo")),
        f("Sgombro", "Pesce", 205.0, 18.5, 0.0, 14.0, SRC_CREA,
            subs = listOf("Salmone")),
        f("Trota", "Pesce", 121.0, 19.0, 0.0, 4.5, SRC_CREA,
            subs = listOf("Salmone")),
        f("Tonno fresco", "Pesce", 144.0, 24.0, 0.0, 5.0, SRC_CREA,
            subs = listOf("Salmone", "Pesce spada")),
        f("Pesce spada", "Pesce", 121.0, 19.8, 0.0, 4.5, SRC_CREA,
            subs = listOf("Tonno fresco")),
        f("Polpo", "Pesce", 82.0, 16.5, 0.0, 1.0, SRC_CREA,
            subs = listOf("Seppie")),
        f("Seppie", "Pesce", 72.0, 15.0, 0.0, 1.0, SRC_CREA,
            subs = listOf("Polpo", "Calamari")),
        f("Calamari", "Pesce", 72.0, 15.0, 0.0, 1.0, SRC_CREA,
            subs = listOf("Seppie")),
        f("Cozze", "Pesce", 67.0, 11.7, 1.8, 1.7, SRC_CREA,
            subs = listOf("Vongole")),
        f("Vongole", "Pesce", 57.0, 12.0, 1.5, 0.5, SRC_CREA,
            subs = listOf("Cozze")),
        f("Alici", "Pesce", 96.0, 16.8, 0.0, 3.0, SRC_CREA,
            subs = listOf("Sarde")),
        f("Sarde", "Pesce", 130.0, 17.0, 0.0, 7.0, SRC_CREA,
            subs = listOf("Alici", "Sgombro")),

        // ---------- Latticini e uova (ampliamento) ----------
        f("Latte intero", "Latticini", 64.0, 3.3, 4.9, 3.6, SRC_CREA,
            subs = listOf("Latte parzialmente scremato")),
        f("Yogurt greco 2% grassi", "Latticini", 73.0, 9.0, 3.9, 2.0, SRC_USDA,
            subs = listOf("Yogurt greco 0% grassi")),
        f("Yogurt bianco intero", "Latticini", 66.0, 3.9, 4.3, 3.5, SRC_CREA,
            subs = listOf("Yogurt bianco magro")),
        f("Fiocchi di latte (light)", "Latticini", 72.0, 12.0, 2.7, 1.0, SRC_USDA,
            subs = listOf("Fiocchi di latte (cottage)")),
        f("Formaggio grana padano", "Latticini", 392.0, 33.0, 0.0, 28.0, SRC_CREA,
            subs = listOf("Parmigiano reggiano")),
        f("Parmigiano reggiano", "Latticini", 402.0, 33.0, 0.0, 29.0, SRC_CREA,
            subs = listOf("Formaggio grana padano")),
        f("Mozzarella vaccina", "Latticini", 253.0, 18.0, 1.0, 19.0, SRC_CREA,
            subs = listOf("Mozzarella di bufala")),
        f("Mozzarella di bufala", "Latticini", 288.0, 16.7, 0.4, 24.0, SRC_CREA,
            subs = listOf("Mozzarella vaccina")),
        f("Ricotta di pecora", "Latticini", 172.0, 9.5, 3.0, 13.5, SRC_CREA,
            subs = listOf("Ricotta vaccina")),
        f("Stracchino", "Latticini", 300.0, 18.0, 0.8, 24.0, SRC_CREA,
            subs = listOf("Mozzarella vaccina")),
        f("Uovo di gallina (solo tuorlo)", "Uova", 322.0, 16.0, 0.6, 28.0, SRC_CREA,
            subs = listOf("Uovo intero di gallina")),
        f("Frittata (uova intere cotte)", "Uova", 197.0, 13.0, 1.0, 15.0, SRC_USDA,
            subs = listOf("Uovo intero di gallina")),

        // ---------- Cereali e derivati (ampliamento) ----------
        f("Riso venere (crudo)", "Cereali", 342.0, 8.0, 72.0, 2.5, SRC_USDA,
            subs = listOf("Riso integrale (crudo)")),
        f("Farro perlato (crudo)", "Cereali", 335.0, 12.0, 67.0, 2.5, SRC_CREA,
            subs = listOf("Riso integrale (crudo)")),
        f("Orzo perlato (crudo)", "Cereali", 350.0, 10.0, 73.0, 1.5, SRC_CREA,
            subs = listOf("Farro perlato (crudo)")),
        f("Quinoa (cruda)", "Cereali", 368.0, 14.1, 64.2, 6.1, SRC_USDA,
            subs = listOf("Cous cous (crudo)")),
        f("Gnocchi di patate", "Cereali", 133.0, 3.0, 28.0, 0.5, SRC_CREA,
            subs = listOf("Patate")),
        f("Pasta all'uovo (cruda)", "Cereali", 356.0, 13.0, 71.0, 3.0, SRC_CREA,
            subs = listOf("Pasta di semola (cruda)")),
        f("Pane di segale", "Cereali", 243.0, 8.0, 48.0, 1.5, SRC_USDA,
            subs = listOf("Pane integrale")),
        f("Gallette di riso", "Cereali", 387.0, 8.0, 82.0, 3.0, SRC_USDA,
            subs = listOf("Fette biscottate")),
        f("Cornflakes (senza zuccheri aggiunti)", "Cereali", 357.0, 7.0, 84.0, 0.4, SRC_USDA,
            subs = listOf("Fiocchi d'avena")),
        f("Muesli senza zuccheri aggiunti", "Cereali", 368.0, 11.0, 66.0, 6.0, SRC_USDA,
            subs = listOf("Fiocchi d'avena")),
        f("Crackers salati", "Cereali", 435.0, 10.0, 68.0, 13.0, SRC_CREA,
            subs = listOf("Fette biscottate")),
        f("Piadina", "Cereali", 340.0, 8.0, 55.0, 9.0, SRC_CREA,
            subs = listOf("Pane bianco")),

        // ---------- Legumi e derivati (ampliamento) ----------
        f("Piselli secchi (crudi)", "Legumi", 340.0, 24.0, 55.0, 2.0, SRC_CREA,
            subs = listOf("Ceci secchi (crudi)")),
        f("Fave secche (crude)", "Legumi", 305.0, 26.0, 45.0, 1.5, SRC_CREA,
            subs = listOf("Fagioli secchi (crudi)")),
        f("Lenticchie cotte", "Legumi", 116.0, 9.0, 20.0, 0.4, SRC_USDA,
            subs = listOf("Ceci cotti")),
        f("Fagioli borlotti cotti", "Legumi", 110.0, 7.5, 19.0, 0.5, SRC_USDA,
            subs = listOf("Fagioli cannellini cotti")),
        f("Edamame (cotti)", "Legumi", 121.0, 11.9, 8.9, 5.2, SRC_USDA,
            subs = listOf("Ceci cotti")),

        // ---------- Frutta (ampliamento) ----------
        f("Mandarini", "Frutta", 53.0, 0.8, 13.0, 0.3, SRC_CREA,
            subs = listOf("Arance")),
        f("Pompelmo", "Frutta", 42.0, 0.8, 10.0, 0.1, SRC_CREA,
            subs = listOf("Arance")),
        f("Pesche", "Frutta", 39.0, 0.9, 9.5, 0.1, SRC_CREA,
            subs = listOf("Albicocche")),
        f("Albicocche", "Frutta", 48.0, 1.4, 11.0, 0.4, SRC_CREA,
            subs = listOf("Pesche")),
        f("Lamponi", "Frutta", 52.0, 1.2, 12.0, 0.7, SRC_USDA,
            subs = listOf("Fragole")),
        f("Ananas", "Frutta", 50.0, 0.5, 12.0, 0.1, SRC_CREA,
            subs = listOf("Mela")),
        f("Melone", "Frutta", 34.0, 0.8, 7.4, 0.2, SRC_CREA,
            subs = listOf("Anguria")),
        f("Anguria", "Frutta", 30.0, 0.6, 7.2, 0.2, SRC_CREA,
            subs = listOf("Melone")),
        f("Uva", "Frutta", 69.0, 0.7, 17.0, 0.2, SRC_CREA,
            subs = listOf("Banana")),
        f("Datteri secchi", "Frutta", 282.0, 2.5, 75.0, 0.4, SRC_USDA,
            subs = listOf("Fichi secchi")),
        f("Fichi secchi", "Frutta", 249.0, 3.3, 64.0, 0.9, SRC_USDA,
            subs = listOf("Datteri secchi")),

        // ---------- Verdura (ampliamento) ----------
        f("Melanzane", "Verdura", 25.0, 1.0, 5.0, 0.2, SRC_CREA,
            subs = listOf("Zucchine")),
        f("Bietole", "Verdura", 19.0, 1.8, 2.2, 0.2, SRC_CREA,
            subs = listOf("Spinaci")),
        f("Cime di rapa", "Verdura", 33.0, 3.0, 4.0, 0.5, SRC_CREA,
            subs = listOf("Broccoli")),
        f("Cavolo verza", "Verdura", 27.0, 1.8, 5.4, 0.2, SRC_CREA,
            subs = listOf("Cavolfiore")),
        f("Cavolo nero", "Verdura", 49.0, 4.3, 8.8, 0.9, SRC_USDA,
            subs = listOf("Cavolo verza")),
        f("Finocchi", "Verdura", 15.0, 1.2, 2.6, 0.1, SRC_CREA,
            subs = listOf("Sedano")),
        f("Sedano", "Verdura", 16.0, 0.7, 3.0, 0.2, SRC_CREA,
            subs = listOf("Finocchi")),
        f("Cetrioli", "Verdura", 12.0, 0.6, 2.2, 0.1, SRC_CREA,
            subs = listOf("Zucchine")),
        f("Pomodori pelati", "Verdura", 21.0, 1.1, 4.0, 0.2, SRC_CREA,
            subs = listOf("Pomodori")),
        f("Piselli (freschi)", "Verdura", 81.0, 5.4, 14.0, 0.4, SRC_CREA,
            subs = listOf("Fagiolini")),
        f("Asparagi", "Verdura", 20.0, 2.2, 1.8, 0.1, SRC_CREA,
            subs = listOf("Fagiolini")),
        f("Radicchio", "Verdura", 23.0, 1.4, 4.5, 0.2, SRC_CREA,
            subs = listOf("Insalata lattuga")),
        f("Zucca", "Verdura", 26.0, 1.0, 6.0, 0.1, SRC_CREA,
            subs = listOf("Carote")),

        // ---------- Grassi, frutta secca e condimenti (ampliamento) ----------
        f("Olio di cocco", "Grassi", 862.0, 0.0, 0.0, 100.0, SRC_USDA,
            subs = listOf("Olio di semi")),
        f("Burro", "Grassi", 758.0, 0.7, 0.0, 85.0, SRC_CREA,
            subs = listOf("Burro chiarificato (ghee)")),
        f("Burro chiarificato (ghee)", "Grassi", 900.0, 0.0, 0.0, 100.0, SRC_USDA,
            subs = listOf("Burro")),
        f("Semi di chia", "Frutta secca", 486.0, 16.5, 42.0, 30.7, SRC_USDA,
            subs = listOf("Semi di lino")),
        f("Semi di zucca", "Frutta secca", 559.0, 30.0, 11.0, 49.0, SRC_USDA,
            subs = listOf("Semi di girasole")),
        f("Semi di girasole", "Frutta secca", 584.0, 20.8, 20.0, 51.5, SRC_USDA,
            subs = listOf("Semi di zucca")),
        f("Arachidi", "Frutta secca", 567.0, 25.8, 16.1, 49.2, SRC_CREA,
            subs = listOf("Mandorle")),
        f("Pistacchi", "Frutta secca", 562.0, 20.0, 28.0, 45.0, SRC_USDA,
            subs = listOf("Mandorle")),
        f("Anacardi", "Frutta secca", 553.0, 18.2, 30.2, 43.9, SRC_USDA,
            subs = listOf("Mandorle")),
        f("Olive verdi", "Condimenti", 145.0, 1.0, 4.0, 15.0, SRC_CREA,
            subs = listOf("Olive nere")),
        f("Olive nere", "Condimenti", 167.0, 1.2, 6.0, 16.0, SRC_CREA,
            subs = listOf("Olive verdi")),
        f("Passata di pomodoro", "Condimenti", 29.0, 1.6, 5.6, 0.3, SRC_CREA,
            subs = listOf("Pomodori pelati")),
        f("Pesto alla genovese", "Condimenti", 450.0, 5.0, 6.0, 45.0, SRC_CREA,
            subs = listOf("Passata di pomodoro")),
        f("Miele", "Dolcificanti", 304.0, 0.3, 82.4, 0.0, SRC_CREA,
            subs = listOf("Zucchero bianco")),
        f("Zucchero bianco", "Dolcificanti", 387.0, 0.0, 100.0, 0.0, SRC_CREA,
            subs = listOf("Miele")),
        f("Confettura di frutta (senza zuccheri aggiunti)", "Dolcificanti", 160.0, 0.6, 39.0, 0.1, SRC_USDA,
            subs = listOf("Miele")),

        // ---------- Bevande e altro ----------
        f("Caffè espresso (senza zucchero)", "Bevande", 2.0, 0.1, 0.3, 0.0, SRC_USDA,
            subs = listOf("Tè verde (senza zucchero)")),
        f("Tè verde (senza zucchero)", "Bevande", 1.0, 0.0, 0.2, 0.0, SRC_USDA,
            subs = listOf("Caffè espresso (senza zucchero)")),
        f("Succo d'arancia (100%)", "Bevande", 45.0, 0.7, 10.4, 0.2, SRC_CREA,
            subs = listOf("Arance")),
        f("Latte di mandorla non zuccherato", "Bevande vegetali", 15.0, 0.6, 0.3, 1.2, SRC_USDA,
            subs = listOf("Bevanda di soia non zuccherata")),
        f("Latte d'avena", "Bevande vegetali", 46.0, 1.0, 7.0, 1.5, SRC_USDA,
            subs = listOf("Bevanda di soia non zuccherata")),
        f("Cioccolato fondente 85%", "Dolci", 598.0, 10.0, 22.0, 50.0, SRC_USDA,
            subs = listOf("Cioccolato fondente 70%")),
        f("Cioccolato fondente 70%", "Dolci", 546.0, 7.8, 45.9, 31.0, SRC_USDA,
            subs = listOf("Cioccolato fondente 85%")),

        // ---------- Ampliamento catalogo ----------
        // Voci aggiunte per colmare lacune reali del catalogo italiano: mancavano
        // ortaggi di uso quotidiano (cipolla, porri, cavoli), frutta comune
        // (ciliegie, mango, melagrana) e diversi tagli di carne e pesce.
        //
        // Valori per 100 g di parte edibile da USDA FoodData Central (SR Legacy);
        // la dicitura originale della fonte e' nel commento per verifica.
        // Dove la fibra e' inclusa nei carboidrati della fonte, i carboidrati
        // sono riportati al netto: il calcolo 4/4/9 resta cosi coerente con le
        // kcal dichiarate entro il 20%, la stessa tolleranza usata dalla
        // validazione dei piani generati.
        f("Cipolla", "Verdura", 40.0, 1.1, 9.3, 0.1, SRC_USDA, fiber = 1.7), // Onions, raw
        f("Cipollotto", "Verdura", 32.0, 1.8, 7.3, 0.2, SRC_USDA, fiber = 2.6), // Onions, spring or scallions (includes tops and
        f("Aglio", "Verdura", 149.0, 6.4, 33.1, 0.5, SRC_USDA, fiber = 2.1), // Garlic, raw
        f("Carciofi", "Verdura", 47.0, 3.3, 10.5, 0.1, SRC_USDA, fiber = 5.4), // Artichokes, (globe or french), raw
        f("Barbabietole", "Verdura", 43.0, 1.6, 9.6, 0.2, SRC_USDA, fiber = 2.8), // Beets, raw
        f("Cavolo bianco", "Verdura", 25.0, 1.3, 5.8, 0.1, SRC_USDA, fiber = 2.5), // Cabbage, raw
        f("Cavolo rosso", "Verdura", 31.0, 1.4, 7.4, 0.2, SRC_USDA, fiber = 2.1), // Cabbage, red, raw
        f("Cavolini di Bruxelles", "Verdura", 43.0, 3.4, 5.1, 0.3, SRC_USDA, fiber = 3.8), // Brussels sprouts, raw
        f("Indivia", "Verdura", 17.0, 1.2, 3.4, 0.2, SRC_USDA, fiber = 3.1), // Endive, raw
        f("Lattuga", "Verdura", 14.0, 0.9, 3.0, 0.1, SRC_USDA, fiber = 1.2), // Lettuce, iceberg (includes crisphead types), r
        f("Rape", "Verdura", 28.0, 0.9, 6.4, 0.1, SRC_USDA, fiber = 1.8), // Turnips, raw
        f("Ravanelli", "Verdura", 16.0, 0.7, 3.4, 0.1, SRC_USDA, fiber = 1.6), // Radishes, raw
        f("Prezzemolo", "Verdura", 36.0, 3.0, 3.0, 0.8, SRC_USDA, fiber = 3.3), // Parsley, fresh
        f("Basilico", "Verdura", 23.0, 3.1, 1.0, 0.6, SRC_USDA, fiber = 1.6), // Basil, fresh
        f("Erba cipollina", "Verdura", 30.0, 3.3, 1.8, 0.7, SRC_USDA, fiber = 2.5), // Chives, raw
        f("Taccole", "Verdura", 42.0, 2.8, 7.5, 0.2, SRC_USDA, fiber = 2.6), // Peas, edible-podded, raw
        f("Funghi pleurotus", "Verdura", 33.0, 3.3, 3.8, 0.4, SRC_USDA, fiber = 2.3), // Mushrooms, oyster, raw
        f("Zucca butternut", "Verdura", 45.0, 1.0, 11.7, 0.1, SRC_USDA, fiber = 2.0), // Squash, winter, butternut, raw
        f("Pomodorini", "Verdura", 18.0, 0.9, 3.9, 0.2, SRC_USDA, fiber = 1.2), // Tomatoes, red, ripe, raw, year round average
        f("Germogli di soia", "Verdura", 30.0, 3.0, 4.1, 0.2, SRC_USDA, fiber = 1.8), // Mung beans, mature seeds, sprouted, raw
        f("Mais dolce", "Verdura", 86.0, 3.3, 18.7, 1.4, SRC_USDA, fiber = 2.0), // Corn, sweet, yellow, raw
        f("Peperoncino", "Verdura", 40.0, 1.9, 8.8, 0.4, SRC_USDA, fiber = 1.5), // Peppers, hot chili, red, raw
        f("Sedano rapa", "Verdura", 42.0, 1.5, 9.2, 0.3, SRC_USDA, fiber = 1.8), // Celeriac, raw
        f("Ciliegie", "Frutta", 63.0, 1.1, 16.0, 0.2, SRC_USDA, fiber = 2.1), // Cherries, sweet, raw
        f("More", "Frutta", 43.0, 1.4, 9.6, 0.5, SRC_USDA, fiber = 5.3), // Blackberries, raw
        f("Ribes", "Frutta", 63.0, 1.4, 15.4, 0.4, SRC_USDA), // Currants, european black, raw
        f("Prugne fresche", "Frutta", 46.0, 0.7, 11.4, 0.3, SRC_USDA, fiber = 1.4), // Plums, raw
        f("Nettarine", "Frutta", 44.0, 1.1, 10.6, 0.3, SRC_USDA, fiber = 1.7), // Nectarines, raw
        f("Fichi freschi", "Frutta", 74.0, 0.8, 19.2, 0.3, SRC_USDA, fiber = 2.9), // Figs, raw
        f("Cocomero", "Frutta", 30.0, 0.6, 7.5, 0.1, SRC_USDA, fiber = 0.4), // Watermelon, raw
        f("Mango", "Frutta", 60.0, 0.8, 15.0, 0.4, SRC_USDA, fiber = 1.6), // Mangos, raw
        f("Melagrana", "Frutta", 83.0, 1.7, 18.7, 1.2, SRC_USDA, fiber = 4.0), // Pomegranates, raw
        f("Limoni", "Frutta", 29.0, 1.1, 6.5, 0.3, SRC_USDA, fiber = 2.8), // Lemons, raw, without peel
        f("Uvetta", "Frutta secca", 299.0, 3.3, 79.3, 0.2, SRC_USDA, fiber = 4.5), // Raisins, dark, seedless (Includes foods for US
        f("Prugne secche", "Frutta secca", 240.0, 2.2, 63.9, 0.4, SRC_USDA, fiber = 7.1), // Plums, dried (prunes), uncooked
        f("Noci pecan", "Frutta secca", 691.0, 9.2, 13.9, 72.0, SRC_USDA, fiber = 9.6), // Nuts, pecans
        f("Noci macadamia", "Frutta secca", 718.0, 7.9, 13.8, 75.8, SRC_USDA, fiber = 8.6), // Nuts, macadamia nuts, raw
        f("Semi di sesamo", "Frutta secca", 573.0, 17.7, 23.4, 49.7, SRC_USDA, fiber = 11.8), // Seeds, sesame seeds, whole, dried
        f("Riso parboiled", "Cereali", 374.0, 7.5, 80.9, 1.0, SRC_USDA, fiber = 1.8), // Rice, white, long-grain, parboiled, unenriched
        f("Farina di frumento (tipo 00)", "Cereali", 364.0, 10.3, 76.3, 1.0, SRC_USDA, fiber = 2.7), // Wheat flour, white, all-purpose, enriched, ble
        f("Grano saraceno", "Cereali", 343.0, 13.2, 71.5, 3.4, SRC_USDA, fiber = 10.0), // Buckwheat
        f("Miglio", "Cereali", 378.0, 11.0, 72.8, 4.2, SRC_USDA, fiber = 8.5), // Millet, raw
        f("Amaranto", "Cereali", 371.0, 13.6, 65.2, 7.0, SRC_USDA, fiber = 6.7), // Amaranth grain, uncooked
        f("Pane azzimo", "Cereali", 275.0, 9.1, 55.7, 1.2, SRC_USDA, fiber = 2.2), // Bread, pita, white, unenriched
        f("Crusca d'avena", "Cereali", 379.0, 13.2, 67.7, 6.5, SRC_USDA, fiber = 10.1), // Cereals, oats, regular and quick, not fortifie
        f("Germe di grano", "Cereali", 360.0, 23.1, 51.8, 9.7, SRC_USDA, fiber = 13.2), // Wheat germ, crude
        f("Fagioli neri", "Legumi", 341.0, 21.6, 62.4, 1.4, SRC_USDA, fiber = 15.5), // Beans, black, mature seeds, raw
        f("Fagioli rossi", "Legumi", 337.0, 22.5, 61.3, 1.1, SRC_USDA, fiber = 15.2), // Beans, kidney, red, mature seeds, raw
        f("Fagioli azuki", "Legumi", 329.0, 19.9, 62.9, 0.5, SRC_USDA, fiber = 12.7), // Beans, adzuki, mature seeds, raw
        f("Soia secca", "Legumi", 446.0, 36.5, 30.2, 19.9, SRC_USDA, fiber = 9.3), // Soybeans, mature seeds, raw
        f("Lupini", "Legumi", 371.0, 36.2, 40.4, 9.7, SRC_USDA, fiber = 18.9), // Lupins, mature seeds, raw
        f("Coniglio", "Carne", 136.0, 20.1, 0.0, 5.5, SRC_USDA), // Game meat, rabbit, domesticated, composite of 
        f("Anatra", "Carne", 135.0, 18.3, 0.9, 6.0, SRC_USDA), // Duck, domesticated, meat only, raw
        f("Petto di anatra", "Carne", 140.0, 27.6, 0.0, 2.5, SRC_USDA), // Duck, young duckling, domesticated, White Peki
        f("Fegato di vitello", "Carne", 140.0, 19.9, 2.9, 4.8, SRC_USDA), // Veal, variety meats and by-products, liver, ra
        f("Aringa", "Pesce", 158.0, 18.0, 0.0, 9.0, SRC_USDA), // Fish, herring, Atlantic, raw
        f("Salmone affumicato", "Pesce", 117.0, 18.3, 0.0, 4.3, SRC_USDA), // Fish, salmon, chinook, smoked
        f("Cernia", "Pesce", 92.0, 19.4, 0.0, 1.0, SRC_USDA), // Fish, grouper, mixed species, raw
        f("Triglia", "Pesce", 117.0, 19.4, 0.0, 3.8, SRC_USDA), // Fish, mullet, striped, raw
        f("Rana pescatrice", "Pesce", 76.0, 14.5, 0.0, 1.5, SRC_USDA), // Fish, monkfish, raw
        f("Ippoglosso", "Pesce", 186.0, 14.4, 0.0, 13.8, SRC_USDA), // Fish, halibut, Greenland, raw
        f("Scampi", "Pesce", 77.0, 16.5, 0.0, 0.8, SRC_USDA), // Crustaceans, lobster, northern, raw
        f("Uovo sodo", "Uova", 155.0, 12.6, 1.1, 10.6, SRC_USDA), // Egg, whole, cooked, hard-boiled
        f("Panna fresca", "Latticini", 340.0, 2.8, 2.8, 36.1, SRC_USDA), // Cream, fluid, heavy whipping
        f("Kefir", "Latticini", 43.0, 3.8, 4.8, 1.0, SRC_USDA), // Kefir, lowfat, plain, LIFEWAY
        f("Latte di capra", "Latticini", 69.0, 3.6, 4.5, 4.1, SRC_USDA), // Milk, goat, fluid, with added vitamin D
        f("Fontina", "Latticini", 389.0, 25.6, 1.6, 31.1, SRC_USDA), // Cheese, fontina
        f("Pecorino romano", "Latticini", 387.0, 31.8, 3.6, 26.9, SRC_USDA), // Cheese, romano
        f("Grana padano", "Latticini", 420.0, 28.4, 13.9, 27.8, SRC_USDA), // Cheese, parmesan, grated
        f("Gorgonzola", "Latticini", 353.0, 21.4, 2.3, 28.7, SRC_USDA), // Cheese, blue
        f("Mortadella", "Salumi", 311.0, 16.4, 3.0, 25.4, SRC_USDA), // Mortadella, beef, pork
        f("Salame", "Salumi", 336.0, 21.9, 2.4, 25.9, SRC_USDA), // Salami, cooked, beef and pork
        f("Pancetta", "Salumi", 393.0, 13.7, 0.0, 37.1, SRC_USDA), // Pork, cured, bacon, unprepared
        f("Salsiccia di suino", "Salumi", 396.0, 13.8, 2.7, 36.2, SRC_USDA), // Sausage, pork and beef, fresh, cooked
        f("Olio di arachidi", "Grassi", 884.0, 0.0, 0.0, 100.0, SRC_USDA), // Oil, peanut, salad or cooking
        f("Strutto", "Grassi", 902.0, 0.0, 0.0, 100.0, SRC_USDA), // Lard
        f("Margarina", "Grassi", 717.0, 0.2, 0.7, 80.7, SRC_USDA), // Margarine, regular, 80% fat, composite, stick,
        f("Porri", "Verdura", 61.0, 1.5, 14.2, 0.3, SRC_USDA, fiber = 1.8), // Leeks, raw
        f("Farina integrale", "Cereali", 340.0, 13.2, 72.0, 2.5, SRC_USDA, fiber = 10.7), // Wheat flour, whole-grain
        f("Agnello", "Carne", 126.0, 20.0, 0.0, 4.7, SRC_USDA), // Lamb, domestic, leg, lean only, raw
        f("Topinambur", "Tuberi", 73.0, 2.0, 17.4, 0.0, SRC_USDA, fiber = 1.6), // Jerusalem-artichokes, raw
    )


    /**
     * Prezzi indicativi in EUR/kg (stima dichiarata, non un dato di mercato in
     * tempo reale). Servono al BudgetEngine: quando un alimento non ha prezzo,
     * il costo NON viene inventato ma segnalato come non stimabile.
     */
    private val priceTableEurPerKg: Map<String, Double> = mapOf(
        "Petto di pollo" to 8.90, "Fesa di tacchino" to 11.50, "Lonza di maiale" to 8.50,
        "Fesa di manzo" to 13.90, "Carne macinata di manzo (magra)" to 9.90,
        "Uovo intero di gallina" to 4.20, "Albume d'uovo" to 6.50,
        "Merluzzo" to 9.50, "Nasello" to 7.90, "Platessa" to 8.50, "Orata" to 12.90,
        "Branzino" to 13.50, "Salmone" to 16.90, "Gamberi" to 15.90,
        "Tonno in scatola al naturale" to 12.90, "Bresaola" to 29.90,
        "Prosciutto crudo sgrassato" to 24.90,
        "Latte parzialmente scremato" to 1.20, "Latte scremato" to 1.10,
        "Yogurt bianco magro" to 2.90, "Yogurt greco 0% grassi" to 6.90,
        "Fiocchi di latte (cottage)" to 5.90, "Ricotta vaccina" to 5.50,
        "Proteine del siero (whey, polvere)" to 22.00, "Proteine vegetali (polvere)" to 24.00,
        "Lenticchie secche (crude)" to 2.80, "Ceci secchi (crudi)" to 2.60,
        "Fagioli secchi (crudi)" to 2.70, "Fagioli cannellini cotti" to 1.60,
        "Ceci cotti" to 1.60, "Tofu" to 8.90, "Tempeh" to 12.90, "Seitan" to 9.90,
        "Bevanda di soia non zuccherata" to 1.40,
        "Riso bianco (crudo)" to 2.20, "Riso basmati (crudo)" to 3.20,
        "Riso integrale (crudo)" to 2.80, "Pasta di semola (cruda)" to 1.60,
        "Pasta integrale (cruda)" to 2.10, "Cous cous (crudo)" to 3.50,
        "Pane bianco" to 2.90, "Pane integrale" to 3.40, "Fette biscottate" to 3.90,
        "Fiocchi d'avena" to 2.40, "Polenta (farina di mais cruda)" to 2.10,
        "Patate" to 1.20, "Patate dolci" to 2.50,
        "Mela" to 2.20, "Pera" to 2.50, "Banana" to 1.90, "Arance" to 1.80,
        "Kiwi" to 3.20, "Fragole" to 6.90, "Mirtilli" to 12.90,
        "Broccoli" to 2.90, "Cavolfiore" to 2.20, "Spinaci" to 3.90, "Zucchine" to 1.90,
        "Carote" to 1.40, "Peperoni" to 3.20, "Pomodori" to 2.50,
        "Insalata lattuga" to 3.20, "Rucola" to 6.90, "Funghi champignon" to 4.50,
        "Fagiolini" to 3.90,
        "Olio extravergine d'oliva" to 9.90, "Olio di semi" to 3.20,
        "Burro d'arachidi (100% arachidi)" to 12.90,
        "Mandorle" to 14.90, "Noci" to 16.90, "Nocciole" to 15.90, "Semi di lino" to 8.90,
        "Avocado" to 8.90
    )

    /**
     * Alimenti seed con prezzo applicato dove disponibile. Il prezzo non viene
     * mai inventato: gli alimenti non presenti nella tabella restano senza prezzo.
     */
    val foods: List<FoodItem> = rawFoods.map { it.copy(pricePerKgEur = priceTableEurPerKg[it.name]) }

    /**
     * Pesatura corretta per categoria.
     *
     * Il default era [FoodUnit.RAW] per tutte le 173 voci: bresaola, prosciutto,
     * pane, yogurt e tonno in scatola risultavano "crudo", una dicitura falsa
     * che porta l'utente a pesare il prodotto sbagliato.
     */
    private fun defaultUnitFor(category: String, name: String): FoodUnit = when {
        category in setOf("Salumi", "Latticini", "Bevande vegetali", "Integratori") ->
            FoodUnit.AS_SOLD
        category == "Pesce" && ("scatola" in name || "affumicato" in name) -> FoodUnit.AS_SOLD
        category == "Cereali" && ("Pane" in name || "Fette biscottate" in name ||
            "Grissini" in name || "Crackers" in name) -> FoodUnit.AS_SOLD
        else -> FoodUnit.RAW
    }

    private fun f(
        name: String,
        category: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        source: String,
        fiber: Double = 0.0,
        unit: FoodUnit = defaultUnitFor(category, name),
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
