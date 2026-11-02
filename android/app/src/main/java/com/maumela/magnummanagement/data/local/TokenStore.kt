package com.maumela.magnummanagement.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.first

/**
 * Keeps the JWT on the device: encrypted with [TokenCipher], persisted in DataStore.
 *
 * OkHttp interceptors run on background threads and cannot call suspend functions, so the
 * decrypted token is also held in memory ([currentToken]). Call [load] once at app start.
 */
class TokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    @Volatile
    private var cachedToken: String? = null

    /** The token for the current session, or null when logged out. Safe from any thread. */
    fun currentToken(): String? = cachedToken

    /** Reads and decrypts the stored token. Anything unreadable counts as "logged out". */
    suspend fun load(): String? {
        val stored = try {
            dataStore.data.first()[KEY]
        } catch (_: IOException) {
            null
        }
        if (stored == null) {
            cachedToken = null
            return null
        }
        return try {
            cipher.decrypt(stored).also { cachedToken = it }
        } catch (_: Exception) {
            // Key lost, data corrupt or tampered: drop it and make the user log in again.
            clear()
            null
        }
    }

    suspend fun save(token: String) {
        val encrypted = cipher.encrypt(token)
        dataStore.edit { it[KEY] = encrypted }
        cachedToken = token
    }

    suspend fun clear() {
        cachedToken = null
        dataStore.edit { it.remove(KEY) }
    }

    companion object {
        val KEY = stringPreferencesKey("encrypted_token")
    }
}