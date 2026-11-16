package com.fitcoach.app.ui.screens.provider

import com.fitcoach.app.ui.screens.settings.ProviderId
import com.fitcoach.app.ui.screens.settings.ProviderUi

/* ============================================================================
 * Provider AI (design/screens.md §12) — dati di sola presentazione.
 * ProviderUi / ProviderId / ProviderStatus sono i tipi già usati dalle
 * impostazioni: nessuna duplicazione, la stessa istanza alimenta le due viste.
 * ========================================================================== */

data class ProviderOptionUi(
    val id: ProviderId,
    val name: String,
    val note: String,
    val baseUrlHint: String,
)

data class ProviderAiUiState(
    val provider: ProviderUi,
    val options: List<ProviderOptionUi> = emptyList(),
    val isDirty: Boolean = false,
    val canTest: Boolean = false,
    val canSave: Boolean = false,
    val isTesting: Boolean = false,
    val message: String? = null,
) {
    val needsKey: Boolean get() = provider.apiKeyMasked.isBlank()
}
