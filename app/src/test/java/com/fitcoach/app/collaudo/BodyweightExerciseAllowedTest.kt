package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.workout.EquipmentFit
import com.fitcoach.app.domain.workout.ExerciseSeedData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il programma veniva respinto con "esercizio 'Ponte glutei a corpo libero'
 * vietato o incompatibile con attrezzatura/limitazioni".
 *
 * L'esercizio non richiede attrezzatura, ma il filtro lo trattava come se ne
 * richiedesse una: senza "Solo corpo libero" fra le attrezzature dichiarate,
 * BODYWEIGHT non risultava posseduto. Il test usa il catalogo reale.
 */
class BodyweightExerciseAllowedTest {

    private val bodyweightExercises = ExerciseSeedData.exercises.filter {
        Equipment.BODYWEIGHT in it.equipment || it.bodyweightOnly
    }

    @Test
    fun `il catalogo contiene esercizi a corpo libero`() {
        assertTrue(
            "Attesi esercizi a corpo libero nel catalogo reale",
            bodyweightExercises.size >= 3
        )
        assertTrue(
            "Atteso il ponte glutei fra gli esercizi a corpo libero",
            bodyweightExercises.any { it.name == "Ponte glutei a corpo libero" }
        )
    }

    @Test
    fun `gli esercizi a corpo libero sono disponibili con sola attrezzatura da palestra`() {
        val profilo = UserProfile(availableEquipment = setOf(Equipment.BARBELL, Equipment.BENCH))
        val vietati = bodyweightExercises.filterNot { EquipmentFit.isAvailable(it, profilo) }
        assertTrue(
            "Esercizi a corpo libero vietati a chi ha i pesi: ${vietati.map { it.name }}",
            vietati.isEmpty()
        )
    }

    @Test
    fun `gli esercizi a corpo libero restano disponibili senza attrezzatura dichiarata`() {
        val profilo = UserProfile(availableEquipment = emptySet())
        bodyweightExercises.forEach { esercizio ->
            assertTrue(
                "'${esercizio.name}' deve essere eseguibile a mani vuote",
                EquipmentFit.isAvailable(esercizio, profilo)
            )
        }
    }

    @Test
    fun `un esercizio con bilanciere resta vietato a chi ha solo i manubri`() {
        val panca = ExerciseSeedData.exercises.first { it.name == "Panca piana con bilanciere" }
        val profilo = UserProfile(availableEquipment = setOf(Equipment.DUMBBELL))
        assertTrue(
            "La panca con bilanciere non deve passare con i soli manubri",
            !EquipmentFit.isAvailable(panca, profilo)
        )
    }
}
