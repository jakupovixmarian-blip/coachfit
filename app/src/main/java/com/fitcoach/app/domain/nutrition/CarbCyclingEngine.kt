package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.engine.EngineParameters
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.EnergyResult
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Motore di Carb Cycling deterministico.
 *
 * Principi:
 *  - Il bilancio calorico settimanale e la somma dei target giornalieri.
 *  - Il deficit/surplus settimanale dipende SOLO dall'obiettivo, non dai singoli giorni.
 *  - Le proteine sono costanti e basate sul peso (g/kg per obiettivo).
 *  - I grassi hanno un minimo di sicurezza (~0.6 g/kg) e salgono nei giorni LOW.
 *  - I carboidrati riempiono le calorie rimanenti, con piu quota nei giorni HIGH.
 *  - Validazione: kcal deve corrispondere a 4*P + 4*C + 9*F entro tolleranza.
 */
object CarbCyclingEngine {

    // I valori vengono dal registro: un solo posto dove cambiarli, con la fonte
    // dichiarata accanto. Vedi EngineParameters.
    val KCAL_PER_G_PROTEIN get() = EngineParameters.kcalPerGramProtein.value
    val KCAL_PER_G_CARB get() = EngineParameters.kcalPerGramCarb.value
    val KCAL_PER_G_FAT get() = EngineParameters.kcalPerGramFat.value

    /** Tolleranza di coerenza kcal vs macro (kcal). */
    const val KCAL_TOLERANCE = 15.0

    /**
     * Marcatore del calo troppo rapido.
     *
     * Il piano non e' valido se chiede di perdere piu' dell'1% del peso a
     * settimana: e' un vincolo di sicurezza, non un consiglio. Il testo del
     * warning lo contiene, quindi [build] lo riconosce senza duplicare la
     * logica di calcolo.
     */
    const val TOO_FAST_MARKER = "Calo troppo rapido"

    /** Energia di 1 kg di massa corporea: 7.700 kcal. */
    private const val KCAL_PER_KG_BODY_MASS = 7700.0

    /**
     * Pavimento dei grassi in grammi, calcolato dalle calorie del giorno.
     *
     * Prima era un g/kg fisso (0,6, poi 0,8): entrambi numeri senza una fonte
     * primaria. Il pavimento dipende dalle calorie, non dal peso: Helms 2014
     * indica il 15-30% delle calorie dai grassi, e Whittaker & Wu 2021 mostrano
     * che sotto il 20% la testosterone cala.
     */
    /**
     * Quota dei grassi sulle calorie rimanenti (dopo le proteine).
     *
     * Il 0,30 cade dentro il 15-30% delle calorie indicato da Helms 2014.
     */
    private const val UNIT_FAT_SHARE = 0.30

    private const val TOTAL_DAYS = 7

    /** Scarto massimo dei grassi rispetto al riferimento, per tipo di giornata. */
    private const val MAX_FAT_SPREAD = 0.20

    /** Vantaggio minimo in kcal che i giorni ON devono conservare sui giorni OFF. */
    private const val MIN_KCAL_GAP_ON_VS_OFF = 100.0

    fun minFatGrams(dailyKcal: Double, weightKg: Double?): Double =
        EngineParameters.minFatGrams(dailyKcal, weightKg ?: 0.0)

    /**
     * Verifica se il carb cycling e giustificato per questo profilo.
     *
     * Non va applicato automaticamente a tutti. E' motivato quando esiste un
     * obiettivo di ricomposizione/definizione con un numero sufficiente di giorni
     * di allenamento (>= 3) e una buona capacita di recupero/aderenza, oppure
     * quando l'utente lo richiede esplicitamente. Per obiettivi di mantenimento
     * o forza con pochi allenamenti, si preferisce una distribuzione uniforme.
     */
    fun shouldApplyCarbCycling(profile: UserProfile): Boolean {
        if (profile.goal == Goal.MAINTENANCE) return false
        if (profile.trainingDaysPerWeek < 3) return false
        // Con recupero molto scarso il cycling e comunque poco utile: priorita al recupero.
        return true
    }

    /** Distribuzione uniforme (nessun carb cycling): un solo target per tutti i giorni. */
    fun buildUniform(
        profile: UserProfile,
        energy: EnergyResult
    ): CarbCyclingPlan {
        val weight = profile.weightKg ?: 70.0
        val adjFraction = weeklyAdjustmentFraction(profile.goal)
        val weeklyKcal = profile.calorieTargetOverride?.let { it * 7 }
            ?: (energy.tdee * 7 * (1 + adjFraction))
        val dailyKcal = weeklyKcal / 7.0
        val proteinG = weight * proteinPerKg(profile.goal)
        val warnings = mutableListOf<String>()

        val fatG = referenceFatGrams(dailyKcal, proteinG, profile.weightKg)
        val target = solveDay(CarbDayType.MEDIUM, dailyKcal, proteinG, fatG, warnings, profile)
        val targets = linkedMapOf(CarbDayType.MEDIUM to target)
        val floor = safetyFloor(profile, energy)
        if (target.kcal < floor) {
            warnings += "Target ${target.kcal.roundToInt()} kcal sotto la soglia di sicurezza (${floor.roundToInt()} kcal)."
        }
        return CarbCyclingPlan(
            targets = targets,
            highDays = 0,
            mediumDays = 7,
            lowDays = 0,
            weeklyKcal = weeklyKcal,
            weeklyKcalFromTarget = target.kcal * 7,
            weeklyProtein = proteinG * 7,
            weeklyCarbs = target.carbs * 7,
            weeklyFat = target.fat * 7,
            warnings = warnings,
            isValid = warnings.none { it.contains("soglia di sicurezza") },
            applied = false
        )
    }

    /**
     * Deficit/surplus settimanale come frazione del TDEE, per obiettivo.
     *
     * Sono convenzioni, non prescrizioni della letteratura: Helms non dice
     * "-20%", dice "0,5-1% del peso a settimana". Il vincolo reale si verifica a
     * valle con [impliedWeeklyLossPercent], che confronta il deficit proposto con
     * il peso della persona.
     */
    fun weeklyAdjustmentFraction(goal: Goal): Double = when (goal) {
        Goal.CUT -> -0.20
        Goal.RECOMP -> -0.08
        Goal.HYPERTROPHY -> 0.10
        Goal.MAINTENANCE -> 0.0
        Goal.STRENGTH -> 0.05
    }

    /**
     * Calo settimanale implicito, in percentuale del peso corporeo.
     *
     * Converte il deficit calorico in velocita' di calo: un deficit di 500 kcal
     * al giorno sono 3.500 kcal a settimana, cioe' circa 0,45 kg di massa
     * grassa (7.700 kcal per kg). Se il risultato esce da 0,5-1% il motore lo
     * segnala, perche' vuol dire che sta chiedendo un calo troppo rapido.
     *
     * @return percentuale del peso a settimana, o null se manca il peso.
     */
    fun impliedWeeklyLossPercent(
        tdee: Double,
        weeklyAdjustmentFraction: Double,
        weightKg: Double?
    ): Double? {
        if (weightKg == null || weightKg <= 0 || weeklyAdjustmentFraction >= 0) return null
        val weeklyDeficit = tdee * 7 * -weeklyAdjustmentFraction
        val kgPerWeek = weeklyDeficit / KCAL_PER_KG_BODY_MASS
        return kgPerWeek / weightKg * 100.0
    }

    /** Proteine g/kg per obiettivo. */
    fun proteinPerKg(goal: Goal): Double = EngineParameters.proteinPerKg(goal).value

    /**
     * Limite minimo calorico di sicurezza: non scendere sotto BMR*0.85
     * e mai sotto 1200 kcal (donne) / 1500 kcal (uomini).
     */
    fun safetyFloor(profile: UserProfile, energy: EnergyResult): Double {
        return maxOf(
            energy.bmr * EngineParameters.bmrFloorFactor.value,
            EngineParameters.kcalFloor(profile.sex).value
        )
    }

    /**
     * Distribuzione settimanale dei giorni HIGH/MEDIUM/LOW.
     * HIGH = giorni di allenamento piu pesanti, LOW = riposo/leggeri.
     *
     * Vincolo: i giorni NON-LOW (HIGH + MEDIUM) non possono essere meno dei
     * giorni di allenamento, altrimenti un giorno di allenamento finirebbe LOW
     * (carboidrati ridotti nel giorno in cui serve piu energia). Quindi MEDIUM
     * assorbe almeno i giorni di allenamento che eccedono i giorni HIGH.
     */
    fun distributeDays(trainingDays: Int): Triple<Int, Int, Int> {
        val td = trainingDays.coerceIn(0, 7)
        val (high, medium, low) = when (td) {
            0 -> Triple(0, 0, 7)
            1 -> Triple(0, 1, 6)
            2 -> Triple(1, 1, 5)
            3 -> Triple(1, 2, 4)
            4 -> Triple(2, 2, 3)
            5 -> Triple(2, 3, 2)
            6 -> Triple(3, 3, 1)
            else -> Triple(4, 3, 0)
        }
        // Garanzia esplicita: nessun giorno di allenamento resta LOW.
        val mediumFixed = maxOf(medium, td - high)
        return Triple(high, mediumFixed, 7 - high - mediumFixed)
    }

    /**
     * Distribuzione effettiva: quella scelta dall'utente se valida, altrimenti
     * quella derivata dai giorni di allenamento.
     *
     * La terna utente è accettata solo se tutti e tre i valori sono presenti,
     * non negativi e sommano a 7. Una terna parziale o incoerente viene
     * ignorata per intero: applicarne una parte produrrebbe un piano con
     * un numero di giornate diverso da sette.
     */
    fun effectiveDistribution(profile: UserProfile): Triple<Int, Int, Int> {
        val high = profile.carbHighDaysOverride
        val medium = profile.carbMediumDaysOverride
        val low = profile.carbLowDaysOverride
        val valid = high != null && medium != null && low != null &&
            high >= 0 && medium >= 0 && low >= 0 && high + medium + low == 7
        return if (valid) Triple(high, medium, low)
        else distributeDays(profile.trainingDaysPerWeek)
    }

    /**
     * Costruisce il piano di carb cycling.
     *
     * @param kcalShareHigh quota relativa dei carboidrati nei giorni HIGH (default 1.0)
     * @param kcalShareMedium quota relativa (default 0.65)
     * @param kcalShareLow quota relativa (default 0.35)
     */
    fun build(
        profile: UserProfile,
        energy: EnergyResult,
        highDays: Int = effectiveDistribution(profile).first,
        mediumDays: Int = effectiveDistribution(profile).second,
        lowDays: Int = effectiveDistribution(profile).third,
        // Escursione in GRAMMI tra MEDIUM e gli estremi (50-100 g documentati).
        // Sostituisce le vecchie quote percentuali, che erano una convenzione
        // interna e producevano escursioni del 69% invece di 50-100 g.
        carbSwingGrams: Double = EngineParameters.carbCyclingSwingGrams.value,
        proteinPerKgOverride: Double? = null,
        weeklyAdjustmentOverride: Double? = null
    ): CarbCyclingPlan {
        val weight = profile.weightKg ?: 70.0
        val warnings = mutableListOf<String>()

        // 1) Bilancio calorico settimanale
        val adjFraction = weeklyAdjustmentOverride ?: weeklyAdjustmentFraction(profile.goal)
        val weeklyKcal = profile.calorieTargetOverride?.let { it * 7 }
            ?: (energy.tdee * 7 * (1 + adjFraction))

        val totalDays = (highDays + mediumDays + lowDays).coerceAtLeast(1)
        val dailyAvg = weeklyKcal / totalDays

        // 2) Proteine costanti
        val proteinPerKg = proteinPerKgOverride ?: proteinPerKg(profile.goal)
        val proteinG = weight * proteinPerKg

        // 3) Grassi minimi di sicurezza
        // 0 = il pavimento viene calcolato dentro solveDay sulle kcal del giorno.
        val minFatG = 0.0

        // 4) Calorie giornaliere per tipo.
        //
        //    La ciclizzazione si fa sui CARBOIDRATI, in grammi: il giorno MEDIUM e'
        //    il riferimento, HIGH aggiunge carbSwingGrams e LOW li toglie. Le
        //    calorie del giorno seguono di conseguenza (4 kcal per grammo), cosi'
        //    l'escursione resta nei 50-100 g documentati qualunque sia il profilo.
        //    Prima le calorie venivano spostate con quote percentuali, e il peso
        //    relativo dei grassi faceva esplodere l'escursione al 69%.
        // Carboidrati e grassi si muovono in direzioni opposte, ma non con la
        // stessa forza: e' questo che va bilanciato.
        //
        // La guida chiede due cose insieme:
        //  - "giornate con alto apporto calorico E glucidico" nei giorni ON;
        //  - "giorno OFF: piu' spazio agli alimenti lipidici".
        // Se i grassi salgono troppo nei giorni OFF, le loro calorie superano
        // quelle dei giorni ON e la prima richiesta salta: e' successo davvero,
        // con 2 giorni HIGH e 2 LOW i LOW finivano 6 kcal sopra.
        //
        // L'escursione dei carboidrati vale +(75 g * 4) = 300 kcal. Ogni punto
        // di scarto sui grassi ne toglie 18 (2 * 9 g). Quindi lo scarto massimo
        // ammissibile si ricava: si sceglie il piu' piccolo tra il 20% del
        // riferimento e il valore che lascia almeno 100 kcal di vantaggio ai
        // giorni ON.
        val fatRef = referenceFatGrams(dailyAvg, proteinG, profile.weightKg)
        val carbSwingKcal = carbSwingGrams * EngineParameters.kcalPerGramCarb.value
        val maxSpreadForOrder = if (fatRef > 0) {
            (carbSwingKcal - MIN_KCAL_GAP_ON_VS_OFF) / (2.0 * KCAL_PER_G_FAT * fatRef)
        } else 0.0
        val spread = maxSpreadForOrder.coerceIn(0.0, MAX_FAT_SPREAD)

        val fatHigh = (fatRef * (1.0 - spread))
            .coerceIn(minFatGrams(dailyAvg, profile.weightKg), maxFatGrams(dailyAvg))
        val fatLow = (fatRef * (1.0 + spread))
            .coerceIn(minFatGrams(dailyAvg, profile.weightKg), maxFatGrams(dailyAvg))
        val fatForMedium = if (mediumDays > 0) {
            (fatRef * TOTAL_DAYS - fatHigh * highDays - fatLow * lowDays) / mediumDays
        } else 0.0
        val fatMedium = fatForMedium
            .coerceIn(minFatGrams(dailyAvg, profile.weightKg), maxFatGrams(dailyAvg))
        if (mediumDays > 0 && abs(fatMedium - fatForMedium) > 1.0) {
            warnings += "La ciclizzazione dei grassi non permette di mantenere esatta la media " +
                "settimanale: il giorno MEDIUM tocca un limite di sicurezza."
        }

        // I carboidrati settimanali sono quelli che restano dopo proteine e grassi.
        val weeklyFatG = fatHigh * highDays + fatMedium * mediumDays + fatLow * lowDays
        val weeklyCarbG = ((weeklyKcal - proteinG * KCAL_PER_G_PROTEIN * totalDays -
            weeklyFatG * KCAL_PER_G_FAT) / KCAL_PER_G_CARB).coerceAtLeast(0.0)
        val baseCarbG = weeklyCarbG / totalDays
        val carbHighG = baseCarbG + carbSwingGrams / 2.0
        val carbLowG = baseCarbG - carbSwingGrams / 2.0
        val carbMediumG = if (mediumDays > 0) {
            (weeklyCarbG - carbHighG * highDays - carbLowG * lowDays) / mediumDays
        } else 0.0

        val targets = linkedMapOf<CarbDayType, CarbDayTarget>()
        listOf(
            Triple(CarbDayType.HIGH, carbHighG, fatHigh),
            Triple(CarbDayType.MEDIUM, carbMediumG, fatMedium),
            Triple(CarbDayType.LOW, carbLowG, fatLow)
        ).forEach { (type, carbG, fatG) ->
            var carbs = carbG
            val minCarbG = profile.weightKg?.let { it * 0.5 } ?: 40.0
            if (type == CarbDayType.LOW && carbs < minCarbG) {
                warnings += "${type.label}: carboidrati alzati al minimo (${minCarbG.roundToInt()} g)."
                carbs = minCarbG
            }
            val pR = proteinG.roundToInt().toDouble()
            var cR = carbs.roundToInt().toDouble()
            var fR = fatG.roundToInt().toDouble()

            // Arrotondare ai grammi puo' portare i grassi appena sotto il
            // pavimento del 20%: si recupera dai carboidrati, cosi' le calorie
            // del giorno restano quelle previste.
            val dayKcal = pR * KCAL_PER_G_PROTEIN + cR * KCAL_PER_G_CARB + fR * KCAL_PER_G_FAT
            val floorG = minFatGrams(dayKcal, weight).roundToInt().toDouble()
            if (fR < floorG) {
                val missing = floorG - fR
                val carbCut = (missing * KCAL_PER_G_FAT / KCAL_PER_G_CARB).roundToInt().toDouble()
                val newCarbs = cR - carbCut
                if (newCarbs >= minCarbG) {
                    fR = floorG
                    cR = newCarbs
                } else {
                    // Non c'e' spazio nei carboidrati: alza i grassi e accetta
                    // qualche kcal in piu'.
                    fR = floorG
                }
            }
            targets[type] = CarbDayTarget(
                type = type,
                kcal = pR * KCAL_PER_G_PROTEIN + cR * KCAL_PER_G_CARB + fR * KCAL_PER_G_FAT,
                protein = pR,
                carbs = cR,
                fat = fR
            )
        }

        // Arrotondamento ai grammi: si riassorbe sul giorno MEDIUM, che e' quello
        // di riferimento e non ha un target proprio da rispettare.
        rebalanceRounding(targets, weeklyKcal, highDays, mediumDays, lowDays)

        val weeklyFromTarget = targets[CarbDayType.HIGH]!!.kcal * highDays +
            targets[CarbDayType.MEDIUM]!!.kcal * mediumDays +
            targets[CarbDayType.LOW]!!.kcal * lowDays

        // Il deficit non deve chiedere un calo piu' rapido di quello documentato.
        impliedWeeklyLossPercent(energy.tdee, adjFraction, profile.weightKg)?.let { pct ->
            val maxPct = EngineParameters.weeklyWeightLossMaxPct.value
            if (pct > maxPct) {
                warnings += "Calo troppo rapido: il deficit implica " +
                    "${"%.2f".format(pct)}% del peso a settimana, oltre il limite di " +
                    "$maxPct% indicato da Helms 2014 (rischio di perdere massa magra). " +
                    "Valuta un deficit piu' contenuto."
            }
        }

        val floor = safetyFloor(profile, energy)
        targets.forEach { (type, t) ->
            if (t.kcal < floor) {
                warnings += "${type.label}: ${t.kcal.roundToInt()} kcal sotto la soglia di sicurezza " +
                    "(${floor.roundToInt()} kcal). Verifica il target calorico."
            }
        }

        val balanceDiff = abs(weeklyFromTarget - weeklyKcal)
        if (balanceDiff > 50) {
            warnings += "Il totale settimanale dei giorni (${weeklyFromTarget.roundToInt()} kcal) " +
                "differisce dal bilancio pianificato (${weeklyKcal.roundToInt()} kcal)."
        }

        val isValid = warnings.none {
            it.contains("sotto la soglia di sicurezza") ||
                it.contains("differisce") ||
                it.contains(TOO_FAST_MARKER)
        }

        return CarbCyclingPlan(
            targets = targets,
            highDays = highDays,
            mediumDays = mediumDays,
            lowDays = lowDays,
            weeklyKcal = weeklyKcal,
            weeklyKcalFromTarget = weeklyFromTarget,
            weeklyProtein = proteinG * totalDays,
            weeklyCarbs = targets[CarbDayType.HIGH]!!.carbs * highDays +
                targets[CarbDayType.MEDIUM]!!.carbs * mediumDays +
                targets[CarbDayType.LOW]!!.carbs * lowDays,
            weeklyFat = targets[CarbDayType.HIGH]!!.fat * highDays +
                targets[CarbDayType.MEDIUM]!!.fat * mediumDays +
                targets[CarbDayType.LOW]!!.fat * lowDays,
            warnings = warnings,
            isValid = isValid
        )
    }

    /** Calcola le kcal dei giorni MEDIUM in modo che il totale settimanale torni. */
    private fun computeBalancingKcal(
        weeklyKcal: Double,
        highKcal: Double,
        highDays: Int,
        lowKcal: Double,
        lowDays: Int,
        mediumDays: Int
    ): Double {
        if (mediumDays <= 0) return 0.0
        val remaining = weeklyKcal - highKcal * highDays - lowKcal * lowDays
        return remaining / mediumDays
    }

    /**
     * Assorbe il residuo di arrotondamento sui carboidrati del giorno MEDIUM.
     *
     * I macro sono grammi interi, quindi la somma dei giorni non coincide con il
     * bilancio pianificato. Correggerla qui evita che l'utente veda un piano che
     * dichiara 2.500 kcal e ne contiene 2.470.
     */
    private fun rebalanceRounding(
        targets: MutableMap<CarbDayType, CarbDayTarget>,
        weeklyKcal: Double,
        highDays: Int,
        mediumDays: Int,
        lowDays: Int
    ) {
        val current = targets[CarbDayType.HIGH]!!.kcal * highDays +
            targets[CarbDayType.MEDIUM]!!.kcal * mediumDays +
            targets[CarbDayType.LOW]!!.kcal * lowDays
        val diff = weeklyKcal - current
        val pivot = if (mediumDays > 0) CarbDayType.MEDIUM else CarbDayType.HIGH
        val pivotDays = if (mediumDays > 0) mediumDays else highDays
        if (pivotDays <= 0) return
        val shiftG = (diff / (pivotDays * KCAL_PER_G_CARB)).roundToInt().toDouble()
        if (shiftG == 0.0) return
        val t = targets.getValue(pivot)
        val newCarbs = (t.carbs + shiftG).coerceAtLeast(0.0)
        targets[pivot] = t.copy(
            carbs = newCarbs,
            kcal = t.protein * KCAL_PER_G_PROTEIN + newCarbs * KCAL_PER_G_CARB +
                t.fat * KCAL_PER_G_FAT
        )
    }

    /** Grassi della giornata di riferimento, in grammi. */
    private fun referenceFatGrams(
        referenceKcal: Double,
        proteinG: Double,
        weightKg: Double?
    ): Double {
        val remaining = (referenceKcal - proteinG * KCAL_PER_G_PROTEIN).coerceAtLeast(0.0)
        val byShare = remaining * UNIT_FAT_SHARE / KCAL_PER_G_FAT
        val floor = minFatGrams(referenceKcal, weightKg)
        val cap = maxFatGrams(referenceKcal)
        return byShare.coerceIn(floor, maxOf(floor, cap))
    }

    /** Tetto dei grassi: il 30% delle calorie del giorno (Helms 2014). */
    private fun maxFatGrams(dailyKcal: Double): Double =
        dailyKcal * EngineParameters.maxFatKcalShare.value / KCAL_PER_G_FAT

    /**
     * Grammi di grassi che valgono `share` delle calorie del giorno, con i
     * carboidrati non ancora calcolati.
     *
     * Ricava le kcal residue da proteine e grassi e ripartisce: e' l'unico modo
     * perche' il pavimento resti rispettato anche quando le kcal del giorno
     * dipendono dai macro stessi.
     */
    private fun fatAtShare(dailyKcal: Double, proteinG: Double, share: Double): Double {
        val nonFatKcal = (dailyKcal - proteinG * KCAL_PER_G_PROTEIN).coerceAtLeast(0.0)
        return nonFatKcal * share / (1.0 - share) / KCAL_PER_G_FAT
    }

    /**
     * Risolve i macro di un giorno dati kcal, proteine e grassi.
     *
     * I grassi arrivano gia' calcolati e sono gli stessi per tutti i tipi di
     * giornata: la ciclizzazione muove i carboidrati, non i grassi.
     */
    private fun solveDay(
        type: CarbDayType,
        kcal: Double,
        proteinG: Double,
        referenceFatG: Double,
        warnings: MutableList<String>,
        profile: UserProfile
    ): CarbDayTarget {
        val proteinKcal = proteinG * KCAL_PER_G_PROTEIN
        var fatG = referenceFatG
        var carbKcal = kcal - proteinKcal - fatG * KCAL_PER_G_FAT
        val fatFloor = minFatGrams(kcal, profile.weightKg)
        if (carbKcal < 0) {
            // Riduci i grassi per fare spazio ai carboidrati minimi.
            fatG = fatFloor
            carbKcal = (kcal - proteinKcal - fatG * KCAL_PER_G_FAT).coerceAtLeast(0.0)
        }
        var carbG = carbKcal / KCAL_PER_G_CARB

        // Nei giorni LOW garantiamo comunque un minimo di carboidrati per il cervello.
        val minCarbG = profile.weightKg?.let { it * 0.5 } ?: 40.0
        if (type == CarbDayType.LOW && carbG < minCarbG) {
            carbG = minCarbG
        }

        // Ricalcolo kcal esatte dai macro arrotondati
        val pR = proteinG.roundToInt().toDouble()
        val cR = carbG.roundToInt().toDouble()
        val fR = fatG.roundToInt().toDouble()
        val kcalFromMacros = pR * KCAL_PER_G_PROTEIN + cR * KCAL_PER_G_CARB + fR * KCAL_PER_G_FAT

        if (abs(kcalFromMacros - kcal) > KCAL_TOLERANCE) {
            warnings += "${type.label}: coerenza kcal/macro fuori tolleranza " +
                "(${kcalFromMacros.roundToInt()} vs ${kcal.roundToInt()} kcal)."
        }

        return CarbDayTarget(
            type = type,
            kcal = kcalFromMacros,
            protein = pR,
            carbs = cR,
            fat = fR
        )
    }

    /** Verifica la coerenza matematica di un target. Ritorna l'errore in kcal. */
    fun validateCoherence(target: CarbDayTarget): Double {
        val expected = target.protein * KCAL_PER_G_PROTEIN +
            target.carbs * KCAL_PER_G_CARB +
            target.fat * KCAL_PER_G_FAT
        return abs(expected - target.kcal)
    }

    /** Distribuzione dei tipi di giornata sui 7 giorni (index 0=Lun .. 6=Dom). */
    fun assignDayTypes(highDays: Int, mediumDays: Int, lowDays: Int, trainingDayIndices: List<Int>): List<CarbDayType> {
        val types = MutableList(7) { CarbDayType.LOW }
        val sortedTraining = trainingDayIndices.sorted()
        // I primi `highDays` giorni di allenamento diventano HIGH, i successivi MEDIUM.
        sortedTraining.take(highDays).forEach { if (it in 0..6) types[it] = CarbDayType.HIGH }
        sortedTraining.drop(highDays).take(mediumDays).forEach { if (it in 0..6) types[it] = CarbDayType.MEDIUM }
        // Se restano giorni MEDIUM non assegnati a giorni di allenamento, usali sui giorni non-LOW.
        var remainingMedium = mediumDays - sortedTraining.drop(highDays).take(mediumDays).size
        var idx = 0
        while (remainingMedium > 0 && idx < 7) {
            if (types[idx] == CarbDayType.LOW) {
                types[idx] = CarbDayType.MEDIUM
                remainingMedium--
            }
            idx++
        }
        return types
    }
}
