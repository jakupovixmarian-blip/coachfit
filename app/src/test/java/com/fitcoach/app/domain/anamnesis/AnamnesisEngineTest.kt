package com.fitcoach.app.domain.anamnesis

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnamnesisEngineTest {

    private fun filled() = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        goal = Goal.RECOMP, trainingDaysPerWeek = 4,
        availableEquipment = setOf(Equipment.BARBELL, Equipment.DUMBBELL)
    )

    /** Risposta valida generica per una domanda, in base al suo tipo. */
    private fun defaultAnswerFor(q: AnamnesisEngine.Question): String = when (q.kind) {
        AnamnesisEngine.Kind.NUMBER -> "1"
        AnamnesisEngine.Kind.INT -> "1"
        AnamnesisEngine.Kind.BOOL -> "true"
        AnamnesisEngine.Kind.CHOICE, AnamnesisEngine.Kind.MULTI -> q.options.firstOrNull() ?: "1"
        AnamnesisEngine.Kind.TEXT -> "test"
    }

    @Test
    fun `empty profile reports essential data missing`() {
        val p = UserProfile()
        assertThat(p.missingEssentialData).containsAtLeast("età", "altezza", "peso")
        assertThat(p.isAnamnesisReady).isFalse()
    }

    @Test
    fun `next question skips already answered`() {
        val q = AnamnesisEngine.nextQuestion(filled())
        assertThat(q?.id).isEqualTo("bodyFatPercent")
    }

    @Test
    fun `skipping an optional question advances monotonically and does not loop`() {
        // Profilo con le domande iniziali risposte fino a una facoltativa non risposta.
        val optional = AnamnesisEngine.questions.first { !it.required }
        val before = AnamnesisEngine.questions.takeWhile { it.id != optional.id }
        var p = filled()
        before.forEach { p = AnamnesisEngine.applyAnswer(p, it.id, defaultAnswerFor(it)) }

        // La domanda corrente e quella facoltativa.
        assertThat(AnamnesisEngine.nextQuestion(p)?.id).isEqualTo(optional.id)

        // Saltandola, la successiva NON deve tornare sulla domanda saltata.
        val after = AnamnesisEngine.nextQuestionAfter(p, optional.id)
        assertThat(after?.id).isNotEqualTo(optional.id)
        // E' la prima non risposta DOPO quella saltata.
        val expected = AnamnesisEngine.questions
            .dropWhile { it.id != optional.id }.drop(1)
            .firstOrNull { !AnamnesisEngine.isAnswered(p, it.id) }
        assertThat(after?.id).isEqualTo(expected?.id)

        // Se tutte le successive sono risposte, non c'e' una prossima domanda.
        var allAfter = p
        AnamnesisEngine.questions.dropWhile { it.id != optional.id }.drop(1)
            .forEach { allAfter = AnamnesisEngine.applyAnswer(allAfter, it.id, defaultAnswerFor(it)) }
        assertThat(AnamnesisEngine.nextQuestionAfter(allAfter, optional.id)).isNull()
    }

    @Test
    fun `applyAnswer parses numbers and enums`() {
        var p = UserProfile()
        p = AnamnesisEngine.applyAnswer(p, "age", "31")
        p = AnamnesisEngine.applyAnswer(p, "sex", "female")
        p = AnamnesisEngine.applyAnswer(p, "heightCm", "172,5")
        p = AnamnesisEngine.applyAnswer(p, "weightKg", "64.2")
        assertThat(p.ageYears).isEqualTo(31)
        assertThat(p.sex).isEqualTo(Sex.FEMALE)
        assertThat(p.heightCm).isWithin(0.01).of(172.5)
        assertThat(p.weightKg).isWithin(0.01).of(64.2)
    }

    @Test
    fun `applyAnswer ignores invalid input leaving profile valid`() {
        val p = AnamnesisEngine.applyAnswer(UserProfile(ageYears = 40), "age", "abc")
        assertThat(p.ageYears).isEqualTo(40)
    }

    @Test
    fun `multi answer parses equipment set`() {
        val p = AnamnesisEngine.applyAnswer(UserProfile(), "availableEquipment", "BARBELL, DUMBBELL")
        assertThat(p.availableEquipment).containsExactly(Equipment.BARBELL, Equipment.DUMBBELL)
    }

    @Test
    fun `monthly budget of zero is treated as not set`() {
        val p = AnamnesisEngine.applyAnswer(UserProfile(), "monthlyFoodBudgetEur", "0")
        assertThat(p.monthlyFoodBudgetEur).isNull()
    }

    @Test
    fun `budget persons never below one`() {
        val p = AnamnesisEngine.applyAnswer(UserProfile(), "budgetPersons", "0")
        assertThat(p.budgetPersons).isEqualTo(1)
    }

    @Test
    fun `isAnamnesisReady requires confirmation flag`() {
        val p = filled()
        assertThat(p.missingEssentialData).isEmpty()
        assertThat(p.isAnamnesisReady).isFalse() // non ancora confermata
        assertThat(p.copy(anamnesisCompleted = true).isAnamnesisReady).isTrue()
    }

    @Test
    fun `contradiction detected when frequency exceeds available days`() {
        val p = filled().copy(trainingFrequencyPerWeek = 6, trainingDaysPerWeek = 3)
        assertThat(p.anamnesisContradictions).isNotEmpty()
    }

    @Test
    fun `summary contains key sections`() {
        val text = AnamnesisEngine.summary(filled())
        assertThat(text).contains("PROFILO PERSONALE")
        assertThat(text).contains("ESPERIENZA DI ALLENAMENTO")
        assertThat(text).contains("Ricomposizione corporea")
    }
}
