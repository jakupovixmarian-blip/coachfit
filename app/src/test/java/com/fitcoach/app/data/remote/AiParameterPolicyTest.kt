package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AiParameterPolicyTest {

    @Test
    fun `provider non openai usano sempre max_tokens`() {
        assertThat(AiParameterPolicy.tokenParamFor(AiProvider.GROQ, "o1"))
            .isEqualTo(TokenParam.MAX_TOKENS)
        assertThat(AiParameterPolicy.tokenParamFor(AiProvider.OPENROUTER, "gpt-5"))
            .isEqualTo(TokenParam.MAX_TOKENS)
        assertThat(AiParameterPolicy.tokenParamFor(AiProvider.DEEPSEEK, "gpt-4.1"))
            .isEqualTo(TokenParam.MAX_TOKENS)
        assertThat(AiParameterPolicy.tokenParamFor(AiProvider.CUSTOM, "o3"))
            .isEqualTo(TokenParam.MAX_TOKENS)
    }

    @Test
    fun `openai modelli recenti usano max_completion_tokens`() {
        listOf("o1", "o1-preview", "o1-mini", "o3", "o3-mini", "o4-mini", "gpt-4.1", "gpt-4.1-mini", "gpt-5", "gpt-5-nano")
            .forEach { model ->
                assertThat(AiParameterPolicy.tokenParamFor(AiProvider.OPENAI, model))
                    .isEqualTo(TokenParam.MAX_COMPLETION_TOKENS)
            }
    }

    @Test
    fun `openai modelli classici usano max_tokens`() {
        listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo")
            .forEach { model ->
                assertThat(AiParameterPolicy.tokenParamFor(AiProvider.OPENAI, model))
                    .isEqualTo(TokenParam.MAX_TOKENS)
            }
    }

    @Test
    fun `modello vuoto o non riconosciuto resta max_tokens`() {
        assertThat(AiParameterPolicy.tokenParamFor(AiProvider.OPENAI, ""))
            .isEqualTo(TokenParam.MAX_TOKENS)
        assertThat(AiParameterPolicy.tokenParamFor(AiProvider.OPENAI, "modello-custom"))
            .isEqualTo(TokenParam.MAX_TOKENS)
    }

    @Test
    fun `riconosce l'errore di parametro non supportato`() {
        assertThat(
            AiParameterPolicy.isUnsupportedTokenParamError(
                "Unsupported parameter: 'max_tokens' is not supported with this model. " +
                    "Use 'max_completion_tokens' instead."
            )
        ).isTrue()
        assertThat(
            AiParameterPolicy.isUnsupportedTokenParamError(
                "Invalid parameter: 'max_completion_tokens'"
            )
        ).isTrue()
    }

    @Test
    fun `non riconosce come token-param error altri errori`() {
        assertThat(AiParameterPolicy.isUnsupportedTokenParamError("Chiave API non valida")).isFalse()
        assertThat(AiParameterPolicy.isUnsupportedTokenParamError("rate limit exceeded")).isFalse()
        assertThat(AiParameterPolicy.isUnsupportedTokenParamError("")).isFalse()
    }
}
