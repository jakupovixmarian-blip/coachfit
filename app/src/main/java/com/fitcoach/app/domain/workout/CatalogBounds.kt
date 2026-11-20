package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise

/**
 * Limiti di programmazione ricavati dal catalogo esercizi.
 *
 * Perche esiste: i controlli avevano soglie fisse scritte a mano (serie 1..8,
 * ripetizioni 1..30, recupero 15..600s, RIR 0..5). Erano sbagliate per
 * costruzione: il catalogo prescrive Plank a 30-60 secondi, quindi OGNI scheda
 * che lo includeva veniva respinta, e nessun modello poteva superare il
 * controllo. Una soglia fissa contraddice i dati che l'app stessa pubblica.
 *
 * Qui ogni limite e' calcolato dal record dell'esercizio: se il catalogo cambia,
 * i controlli cambiano con lui. Le tolleranze sono le uniche costanti, e sono
 * espresse come proporzioni della prescrizione, non come numeri assoluti.
 */
object CatalogBounds {

    /** Margine sui set rispetto a quelli prescritti dal catalogo. */
    private const val SETS_MARGIN = 1

    /** Margine sulle ripetizioni: consente un blocco di forza o di pump. */
    private const val REPS_MARGIN = 2

    /** Recupero accettato: dal 50% al 200% del valore prescritto. */
    private const val REST_MIN_RATIO = 0.5
    private const val REST_MAX_RATIO = 2.0

    /** RIR accettato: dal valore prescritto -2 a +2, entro il range fisiologico 0..5. */
    private const val RIR_MARGIN = 2

    /** Oltre questo valore un RIR non e' piu' una misura ma un errore. */
    private const val RIR_ABSOLUTE_MAX = 5

    fun sets(exercise: Exercise): IntRange {
        val anchor = exercise.defaultSets.coerceAtLeast(1)
        return (anchor - SETS_MARGIN).coerceAtLeast(1)..(anchor + SETS_MARGIN)
    }

    fun reps(exercise: Exercise): IntRange {
        val min = (exercise.minReps - REPS_MARGIN).coerceAtLeast(1)
        val max = (exercise.maxReps + REPS_MARGIN).coerceAtLeast(min)
        return min..max
    }

    fun rest(exercise: Exercise): IntRange {
        val anchor = exercise.restSeconds.coerceAtLeast(1)
        val min = (anchor * REST_MIN_RATIO).toInt().coerceAtLeast(1)
        val max = (anchor * REST_MAX_RATIO).toInt().coerceAtLeast(min)
        return min..max
    }

    fun rir(exercise: Exercise): IntRange {
        val min = (exercise.targetRir - RIR_MARGIN).coerceAtLeast(0)
        val max = (exercise.targetRir + RIR_MARGIN).coerceAtMost(RIR_ABSOLUTE_MAX)
        return min..max
    }

    /**
     * Verifica un esercizio programmato contro i limiti del suo record.
     *
     * Restituisce le violazioni descritte con valore atteso e ricevuto: l'AI
     * deve poter correggere, non indovinare.
     */
    fun violations(exercise: Exercise, sets: Int, minReps: Int, maxReps: Int, rest: Int, rir: Int): List<String> {
        val out = mutableListOf<String>()
        val setsRange = sets(exercise)
        if (sets !in setsRange) {
            out += "${exercise.name}: $sets serie, il catalogo ne prescrive ${exercise.defaultSets} " +
                "(ammesso ${setsRange.first}-${setsRange.last})"
        }
        val repsRange = reps(exercise)
        if (minReps !in repsRange || maxReps !in repsRange || maxReps < minReps) {
            out += "${exercise.name}: $minReps-$maxReps rip., il catalogo prescrive " +
                "${exercise.minReps}-${exercise.maxReps} (ammesso ${repsRange.first}-${repsRange.last})"
        }
        val restRange = rest(exercise)
        if (rest !in restRange) {
            out += "${exercise.name}: recupero ${rest}s, il catalogo prescrive ${exercise.restSeconds}s " +
                "(ammesso ${restRange.first}-${restRange.last}s)"
        }
        val rirRange = rir(exercise)
        if (rir !in rirRange) {
            out += "${exercise.name}: RIR $rir, il catalogo prescrive ${exercise.targetRir} " +
                "(ammesso ${rirRange.first}-${rirRange.last})"
        }
        return out
    }
}
