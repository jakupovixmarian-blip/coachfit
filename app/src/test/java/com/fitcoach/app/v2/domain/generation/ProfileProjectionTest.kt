package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.domain.anamnesis.AnamnesisAnswer
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisField
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileProjectionTest {
    @Test
    fun `confirmed answers become typed profile without filling optional values`() {
        val session = requiredSession()
        val confirmed = AnamnesisEngine.confirm(session, 99L)
        val result = ProfileProjection.from(confirmed) as ProfileProjectionResult.Success

        assertThat(result.profile.ageYears).isEqualTo(31)
        assertThat(result.profile.heightCm).isEqualTo(182.5)
        assertThat(result.profile.bodyFatPercent).isNull()
        assertThat(result.profile.promptContext()).contains("massa_grassa_percentuale=NON DISPONIBILE")
    }

    @Test
    fun `unconfirmed or unknown required profile cannot be projected`() {
        val missing = ProfileProjection.from(requiredSession())
        assertThat(missing).isInstanceOf(ProfileProjectionResult.Failure::class.java)

        val unknown = AnamnesisEngine.answer(
            requiredSession(),
            AnamnesisField.WEIGHT_KG,
            AnamnesisAnswer.unknown(1L)
        )
        assertThat(ProfileProjection.from(unknown)).isInstanceOf(ProfileProjectionResult.Failure::class.java)
    }

    private fun requiredSession(): AnamnesisSession {
        val values = mapOf(
            AnamnesisField.AGE to "31",
            AnamnesisField.BIOLOGICAL_SEX to "F",
            AnamnesisField.HEIGHT_CM to "182,5",
            AnamnesisField.WEIGHT_KG to "74",
            AnamnesisField.PRIMARY_GOAL to "ipertrofia",
            AnamnesisField.TRAINING_EXPERIENCE to "intermedio",
            AnamnesisField.EQUIPMENT to "palestra completa",
            AnamnesisField.TRAINING_DAYS to "4",
            AnamnesisField.BREAKFAST_PREFERENCE to "salato"
        )
        return values.entries.fold(AnamnesisSession("projection")) { session, (field, value) ->
            AnamnesisEngine.answer(session, field, AnamnesisAnswer.provided(value, 1L))
        }
    }
}
