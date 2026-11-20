package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * La generazione e l'operazione piu pesante dell'app: costruisce un prompt con
 * centinaia di alimenti ed esercizi, serializza payload di migliaia di token e
 * risolve le grammature in un ciclo iterativo.
 *
 * Eseguita sul thread principale blocca la UI fino all'ANR, che Android presenta
 * all'utente come chiusura dell'app ("va in crash quando inizia a generare").
 * Questi test bloccano quella regressione.
 */
class GenerationThreadingCollaudoTest {

    private fun profile() = MarjanProfile.build()

    /** Dispatcher che registra se la generazione e stata spostata su di esso. */
    private class RecordingDispatcher : CoroutineDispatcher() {
        val used = AtomicBoolean(false)
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            used.set(true)
            block.run()
        }
    }

    private fun coordinator(): ProgramGenerationCoordinator {
        val service = AiProgramGenerationService(
            orchestrator = mockk(relaxed = true),
            chatRepository = mockk(relaxed = true),
            secureKeyStore = mockk(relaxed = true),
            json = Json { ignoreUnknownKeys = true },
            foodRepository = mockk(relaxed = true),
            exerciseRepository = mockk(relaxed = true)
        )
        return ProgramGenerationCoordinator(service)
    }

    @Test
    fun `la generazione viene spostata fuori dal thread chiamante`() = runBlocking {
        val recording = RecordingDispatcher()
        coordinator().generate(
            profile(),
            ProgramGenerationCoordinator.Scope.NUTRITION,
            dispatcher = recording
        )
        assertThat(recording.used.get()).isTrue()
    }

    @Test
    fun `un profilo incompleto restituisce un esito e non lancia`() = runBlocking {
        val incomplete = profile().copy(ageYears = null, weightKg = null)
        val result = coordinator().generate(incomplete, ProgramGenerationCoordinator.Scope.BOTH)
        assertThat(result).isInstanceOf(ProgramGenerationResult.AnamnesisNotConfirmed::class.java)
    }

    @Test
    fun `un guasto del servizio diventa un esito leggibile`() = runBlocking {
        // Le dipendenze sono mock: la generazione reale fallisce dentro il
        // servizio. Il coordinatore deve restituire Error, mai rilanciare.
        val result = coordinator().generate(profile(), ProgramGenerationCoordinator.Scope.NUTRITION)
        assertThat(result).isInstanceOf(ProgramGenerationResult.Error::class.java)
    }
}
