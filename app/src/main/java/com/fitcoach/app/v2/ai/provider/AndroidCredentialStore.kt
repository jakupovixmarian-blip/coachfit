package com.fitcoach.app.v2.ai.provider

import com.fitcoach.app.data.remote.SecureKeyStore
import javax.inject.Inject

/** Bridges the 2.0 provider port to the Android Keystore-backed store. */
class AndroidCredentialStore @Inject constructor(
    private val secureKeyStore: SecureKeyStore
) : CredentialStore {
    override suspend fun read(ref: String): String? = secureKeyStore.getApiKey(ref)
}
