package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.engine.EngineEvidence
import com.fitcoach.app.domain.engine.EngineParameters
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Il registro dei parametri deve essere verificabile, non una dichiarazione di
 * intenti.
 *
 * Questi test esistono per un motivo concreto: in produzione c'era una soglia di
 * grassi di 0,6 g/kg che nessuna delle 1.046 regole del corpus conteneva. Nessuno
 * se ne era accorto perche' un numero non dichiarato non e' controllabile.
 * Adesso ogni parametro dichiara la sua origine, e se dichiara letteratura
 * primaria deve citare uno studio che esiste davvero.
 */
class EngineParametersCollaudoTest {

    @Test
    fun `ogni parametro dichiarato primario cita uno studio registrato`() {
        val primari = EngineParameters.all().filter { it.second.fromPrimary }
        assertThat(primari).isNotEmpty()
        primari.forEach { (name, p) ->
            assertThat(p.study).isNotNull()
            assertThat(EngineEvidence.byId(p.study!!.id)).isNotNull()
            // Il PMID c'e' per gli studi PubMed; per i riferimenti istituzionali
            // (FAO) la verificabilita' e' data dal link ufficiale.
            if (p.study!!.pmid.isEmpty()) {
                assertThat(p.study!!.sourceUrl).isNotNull()
            } else {
                assertThat(p.study!!.pmid).isNotEmpty()
            }
            // La frase esatta da cui viene il numero: senza, la citazione e' fumo.
            assertThat(p.study!!.quote).isNotEmpty()
        }
    }

    @Test
    fun `nessuna origine del registro rimanda alla prosa divulgativa`() {
        // Il corpus InVictus e' 1000 regole divulgative su 1046: va bene per
        // spiegare all'utente, non per fissare una soglia. L'origine che la
        // rappresentava e' stata rimossa dall'enum: questo test impedisce che
        // venga reintrodotta di nascosto.
        val origini = EngineParameters.Origin.entries.map { it.name }
        assertThat(origini).containsExactly("PRIMARY", "PRIMARY_RANGE", "ATWATER", "CLINICAL", "CONVENTION")
        assertThat(origini).doesNotContain("CORPUS")
    }

    @Test
    fun `le fonti citate sono registrate con PMID e citazione`() {
        EngineEvidence.all().forEach { study ->
            assertThat(study.citation).isNotEmpty()
            assertThat(study.quote).isNotEmpty()
            assertThat(study.url).startsWith("https://")
            // O c'e' un PMID (e allora l'URL lo contiene), o c'e' un link
            // istituzionale dichiarato. Una fonte senza nessuno dei due non
            // sarebbe verificabile.
            if (study.pmid.isNotEmpty()) {
                assertThat(study.url).contains(study.pmid)
            } else {
                assertThat(study.sourceUrl).isNotNull()
            }
        }
        // I sei studi che sostengono il registro.
        assertThat(EngineEvidence.all().map { it.id }).containsExactly(
            "Morton2018", "Jaeger2017", "Aragon2017",
            "Helms2014", "Xu2023", "FatTestosterone2021",
            "Black1996", "FaoWhoUnu2004"
        )
    }

    @Test
    fun `i grassi stanno nel 15-30 per cento indicato da Helms`() {
        assertThat(EngineParameters.minFatKcalShare.value).isEqualTo(0.15)
        assertThat(EngineParameters.maxFatKcalShare.value).isEqualTo(0.30)
        assertThat(EngineParameters.minFatKcalShare.study?.id).isEqualTo("Helms2014")
        assertThat(EngineParameters.maxFatKcalShare.study?.id).isEqualTo("Helms2014")
        assertThat(EngineParameters.minFatKcalShare.origin)
            .isEqualTo(EngineParameters.Origin.PRIMARY)
    }

    @Test
    fun `il pavimento grassi si calcola dalle calorie, non da un g per kg`() {
        // Su 2000 kcal il 15% sono 33 g e il 20% sono 44 g: vince il piu' alto,
        // perche' sotto il 20% la testosterone cala (Whittaker & Wu 2021).
        val g = EngineParameters.minFatGrams(dailyKcal = 2000.0, weightKg = 80.0)
        assertThat(g).isWithin(0.5).of(2000.0 * 0.20 / 9.0)
        // Il pavimento sale con le calorie e non dipende dal peso corporeo.
        val gAlto = EngineParameters.minFatGrams(dailyKcal = 3000.0, weightKg = 80.0)
        assertThat(gAlto).isGreaterThan(g)
        assertThat(EngineParameters.FAT_TEST_FLOOR_SHARE).isEqualTo(0.20)
    }

    @Test
    fun `le proteine stanno tra il minimo ISSN e il massimo per il deficit`() {
        Goal.entries.forEach { goal ->
            val p = EngineParameters.proteinPerKg(goal)
            // ISSN 2017: minimo 1,4 g/kg. Aragon 2017: in deficit fino a
            // 2,3-3,1 g/kg di massa magra, cioe' ~2,6 g/kg di peso al 15% di grasso.
            assertThat(p.value).isAtLeast(1.4)
            assertThat(p.value).isAtMost(2.6)
            assertThat(p.study?.id).isEqualTo("Morton2018")
        }
        // Il punto di saturazione e' quello misurato da Morton, non un arrotondamento.
        assertThat(EngineParameters.proteinSaturationPerKg.value).isEqualTo(1.62)
        assertThat(EngineParameters.proteinSaturationPerKg.study?.pmid).isEqualTo("28698222")
    }

    @Test
    fun `l'escursione dei carboidrati sta nell'intervallo documentato 50-100 g`() {
        val swing = EngineParameters.carbCyclingSwingGrams
        assertThat(swing.value).isAtLeast(50.0)
        assertThat(swing.value).isAtMost(100.0)
        assertThat(swing.origin).isEqualTo(EngineParameters.Origin.PRIMARY)
        assertThat(swing.study?.pmid).isEqualTo("37229479")
    }

    @Test
    fun `i fattori di attivita stanno nelle bande FAO`() {
        // Le bande FAO/WHO/UNU 2004 sono 1,40-1,69 / 1,70-1,99 / 2,00-2,40.
        // I vecchi 1,2 / 1,375 / 1,55 / 1,725 non appartenevano a nessuna banda.
        assertThat(EngineParameters.palSedentaryMin.value).isEqualTo(1.40)
        assertThat(EngineParameters.palSedentaryMax.value).isEqualTo(1.69)
        assertThat(EngineParameters.palModerateMin.value).isEqualTo(1.70)
        assertThat(EngineParameters.palModerateMax.value).isEqualTo(1.99)
        assertThat(EngineParameters.palVigorousMin.value).isEqualTo(2.00)
        assertThat(EngineParameters.palVigorousMax.value).isEqualTo(2.40)
        EngineParameters.all().filter { it.first.startsWith("pal") }.forEach { (_, p) ->
            assertThat(p.origin).isEqualTo(EngineParameters.Origin.PRIMARY)
        }
    }

    @Test
    fun `il fattore di attivita non esce mai dalle bande pubblicate`() {
        val profili = listOf(0, 1, 2, 3, 4, 5, 6, 7)
        profili.forEach { giorni ->
            val p = UserProfile(
                id = 1L, name = "T", ageYears = 30, sex = Sex.MALE,
                heightCm = 178.0, weightKg = 80.0, bodyFatPercent = 15.0,
                trainingDaysPerWeek = giorni
            )
            val f = EnergyCalculator.activityFactor(p)
            // Con un override il motore lo rispetta: qui si testa il calcolo.
            assertThat(f).isAtLeast(EngineParameters.palSedentaryMin.value)
            assertThat(f).isAtMost(EngineParameters.palVigorousMax.value)
        }
    }

    @Test
    fun `il calo settimanale implicito rispetta il limite di Helms`() {
        // 2.600 kcal di TDEE, deficit del 20%: 520 kcal/giorno.
        // 3.640 kcal a settimana / 7.700 = 0,47 kg, su 80 kg = 0,59%: dentro.
        val pct = CarbCyclingEngine.impliedWeeklyLossPercent(2600.0, -0.20, 80.0)!!
        assertThat(pct).isWithin(0.02).of(0.59)
        assertThat(pct).isAtLeast(EngineParameters.weeklyWeightLossMinPct.value)
        assertThat(pct).isAtMost(EngineParameters.weeklyWeightLossMaxPct.value)
        // Su un peso molto basso lo stesso deficit percentuale e' piu' aggressivo
        // in valore assoluto: 3.150 kcal a settimana su 50 kg sono 0,82% del
        // peso, contro 0,59% su 80 kg.
        val pctLeggero = CarbCyclingEngine.impliedWeeklyLossPercent(1800.0, -0.25, 50.0)!!
        assertThat(pctLeggero).isWithin(0.01).of(0.82)
        // E con un deficit del 30% su un peso basso si supera l'1%.
        val pctEccessivo = CarbCyclingEngine.impliedWeeklyLossPercent(1800.0, -0.35, 50.0)!!
        assertThat(pctEccessivo).isGreaterThan(EngineParameters.weeklyWeightLossMaxPct.value)
        // Con surplus o mantenimento non c'e' calo da valutare.
        assertThat(CarbCyclingEngine.impliedWeeklyLossPercent(2600.0, 0.10, 80.0)).isNull()
        assertThat(CarbCyclingEngine.impliedWeeklyLossPercent(2600.0, -0.20, null)).isNull()
    }

    @Test
    fun `le soglie di sicurezza non sono presentate come letteratura`() {
        // Il pavimento calorico e il limite dei giorni LOW non hanno uno studio:
        // devono dichiararsi convenzione o clinica, mai primaria.
        assertThat(EngineParameters.kcalFloor(Sex.MALE).origin)
            .isEqualTo(EngineParameters.Origin.CLINICAL)
        assertThat(EngineParameters.maxConsecutiveLowDays.origin)
            .isEqualTo(EngineParameters.Origin.CONVENTION)
        // E nessuno dei due deve fingersi sostenuto da una fonte.
        assertThat(EngineParameters.kcalFloor(Sex.MALE).study).isNull()
        assertThat(EngineParameters.maxConsecutiveLowDays.study).isNull()
    }
}
