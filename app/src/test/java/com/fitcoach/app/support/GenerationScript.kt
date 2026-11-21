package com.fitcoach.app.support

import com.fitcoach.app.data.remote.ChatCompletionResponse
import retrofit2.Response

/**
 * Script del modello per la generazione di un programma.
 *
 * La generazione esige che `consult_invictus` sia stato chiamato per entrambe le
 * aree prima di `submit_full_program` (`Prerequisite.consultations`). Un fixture
 * che consegna subito non riproduce il comportamento di un modello reale: viene
 * bloccato, e il test fallisce per un difetto del fixture e non del codice.
 */
object GenerationScript {

    /** Consultazioni che un modello reale effettua prima di consegnare. */
    fun consultations(): List<Response<ChatCompletionResponse>> = listOf(
        toolCallResponse("consult_invictus", """{"area":"allenamento"}"""),
        toolCallResponse("consult_invictus", """{"area":"nutrizione"}""")
    )

    /** Consultazioni seguite dalla consegna e dalla chiusura del turno. */
    fun consultationsThen(
        vararg rest: Response<ChatCompletionResponse>
    ): MutableList<Response<ChatCompletionResponse>> =
        (consultations() + rest).toMutableList()
}
