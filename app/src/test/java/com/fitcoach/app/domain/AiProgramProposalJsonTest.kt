package com.fitcoach.app.domain

import com.fitcoach.app.domain.coach.AiProgramProposalJson
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class AiProgramProposalJsonTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodifica il contratto strutturato anche dentro code fence`() {
        val raw = """
            ```json
            {"schemaVersion":1,"strategy":"Forza controllata","objectives":["Aumentare la panca"],
             "monitoring":["peso"],"nutrition":{"days":[]},"workout":{"split":"FULL_BODY","rationale":"r","days":[]},
             "calendar":{"workoutDayIndices":[0],"restDayIndices":[1,2,3,4,5,6]}}
            ```
        """.trimIndent()

        val proposal = AiProgramProposalJson.decode(json, raw)

        assertThat(proposal).isNotNull()
        assertThat(proposal!!.strategy).isEqualTo("Forza controllata")
        assertThat(proposal.calendar.restDayIndices).hasSize(6)
    }

    @Test
    fun `rifiuta json troncato o non strutturato`() {
        assertThat(AiProgramProposalJson.decode(json, "{\"strategy\":")).isNull()
        assertThat(AiProgramProposalJson.decode(json, "testo senza json")).isNull()
    }
}
