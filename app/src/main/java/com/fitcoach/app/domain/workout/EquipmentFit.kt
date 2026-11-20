package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.UserProfile

/**
 * Decide se un esercizio e eseguibile con l'attrezzatura dichiarata.
 *
 * La regola stava ripetuta in tre punti (builder, validator, diagnostica) e
 * tutti e tre trattavano BODYWEIGHT come un'attrezzatura da possedere: chi non
 * spuntava "Solo corpo libero" nell'anamnesi si vedeva vietare plank e ponte
 * glutei. BODYWEIGHT e l'assenza di attrezzatura, non un oggetto da avere.
 */
object EquipmentFit {

    fun isAvailable(exercise: Exercise, available: Set<Equipment>): Boolean {
        if (available.isEmpty()) return true
        if (exercise.bodyweightOnly) return true
        if (exercise.equipment.isEmpty()) return true
        if (Equipment.BODYWEIGHT in exercise.equipment) return true
        return exercise.equipment.any { it in available }
    }

    fun isAvailable(exercise: Exercise, profile: UserProfile): Boolean =
        isAvailable(exercise, profile.availableEquipment)
}
