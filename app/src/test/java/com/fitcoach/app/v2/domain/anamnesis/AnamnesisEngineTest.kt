package com.fitcoach.app.v2.domain.anamnesis

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnamnesisEngineTest {
    private val now = 1_000L

    @Test
    fun `interview is progressive and unknown is preserved without defaults`() {
        var session = AnamnesisSession("session-1")
        assertThat(AnamnesisEngine.nextQuestion(session)).isEqualTo(AnamnesisField.AGE)

        session = AnamnesisEngine.answer(session, AnamnesisField.AGE, AnamnesisAnswer.unknown(now))
        assertThat(AnamnesisEngine.nextQuestion(session)).isEqualTo(AnamnesisField.BIOLOGICAL_SEX)
        assertThat(AnamnesisEngine.summary(session)).containsExactly("AGE: Non lo so")
        assertThat(AnamnesisEngine.review(session).unresolvedRequired).containsExactly(AnamnesisField.AGE)
    }

    @Test
    fun `required answers and contradictions block confirmation`() {
        val session = answerRequired(AnamnesisSession("session-2"), mapOf(
            AnamnesisField.AGE to "22",
            AnamnesisField.BIOLOGICAL_SEX to "M",
            AnamnesisField.HEIGHT_CM to "180",
            AnamnesisField.WEIGHT_KG to "80",
            AnamnesisField.PRIMARY_GOAL to "ipertrofia",
            AnamnesisField.TRAINING_EXPERIENCE to "attuale",
            AnamnesisField.EQUIPMENT to "palestra",
            AnamnesisField.TRAINING_DAYS to "4",
            AnamnesisField.BREAKFAST_PREFERENCE to "dolce"
        ))
        assertThat(AnamnesisEngine.review(session).canConfirm).isTrue()
        assertThat(AnamnesisEngine.confirm(session, now).confirmedAt).isEqualTo(now)

        val contradictory = AnamnesisEngine.answer(session, AnamnesisField.WEIGHT_KG, AnamnesisAnswer.provided("0", now))
        assertThat(AnamnesisEngine.review(contradictory).canConfirm).isFalse()
    }

    @Test
    fun `editing an answer invalidates prior confirmation`() {
        val session = answerRequired(AnamnesisSession("session-3"), mapOf(
            AnamnesisField.AGE to "30",
            AnamnesisField.BIOLOGICAL_SEX to "F",
            AnamnesisField.HEIGHT_CM to "165",
            AnamnesisField.WEIGHT_KG to "62",
            AnamnesisField.PRIMARY_GOAL to "mantenimento",
            AnamnesisField.TRAINING_EXPERIENCE to "mai",
            AnamnesisField.EQUIPMENT to "casa",
            AnamnesisField.TRAINING_DAYS to "3",
            AnamnesisField.BREAKFAST_PREFERENCE to "nessuna"
        ))
        val confirmed = AnamnesisEngine.confirm(session, now)
        val edited = AnamnesisEngine.answer(confirmed, AnamnesisField.WEIGHT_KG, AnamnesisAnswer.provided("63", now + 1))
        assertThat(edited.confirmedAt).isNull()
    }

    private fun answerRequired(session: AnamnesisSession, values: Map<AnamnesisField, String>): AnamnesisSession =
        values.entries.fold(session) { current, (field, value) ->
            AnamnesisEngine.answer(current, field, AnamnesisAnswer.provided(value, now))
        }
}
