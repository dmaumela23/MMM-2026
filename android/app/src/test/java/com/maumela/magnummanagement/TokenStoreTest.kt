package com.maumela.magnummanagement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.maumela.magnummanagement.data.local.TokenCipher
import com.maumela.magnummanagement.data.local.TokenStore
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Stand-in for the Keystore cipher. Reversible, refuses anything it did not produce. */
class FakeTokenCipher : TokenCipher {
    override fun encrypt(plain: String) = "ENC(" + plain.reversed() + ")"
    override fun decrypt(payload: String): String {
        require(payload.startsWith("ENC(") && payload.endsWith(")")) { "corrupt" }
        return payload.removePrefix("ENC(").removeSuffix(")").reversed()
    }
}

class TokenStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val token = "eyJhbGciOiJIUzI1NiJ9.payload.signature"

    private fun TestScope.newDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "token.preferences_pb") }

    // Verifies a saved token survives an app restart (a new TokenStore over the same file).
    @Test
    fun saveThenLoad_roundTripsToken() = runTest {
        val dataStore = newDataStore()
        TokenStore(dataStore, FakeTokenCipher()).save(token)
        assertEquals(token, TokenStore(dataStore, FakeTokenCipher()).load())
    }

    // Verifies the token is NOT stored as readable text.
    @Test
    fun storedValue_isEncryptedAtRest() = runTest {
        val dataStore = newDataStore()
        TokenStore(dataStore, FakeTokenCipher()).save(token)
        val raw = dataStore.data.first()[TokenStore.KEY]
        assertNotEquals(token, raw)
        assertFalse(raw!!.contains(token))
    }

    // Verifies the interceptor can read the token synchronously after saving.
    @Test
    fun save_makesTokenAvailableInMemory() = runTest {
        val store = TokenStore(newDataStore(), FakeTokenCipher())
        assertNull(store.currentToken())
        store.save(token)
        assertEquals(token, store.currentToken())
    }

    // Verifies logout removes the token from memory and from storage.
    @Test
    fun clear_removesTokenEverywhere() = runTest {
        val dataStore = newDataStore()
        val store = TokenStore(dataStore, FakeTokenCipher())
        store.save(token)
        store.clear()
        assertNull(store.currentToken())
        assertNull(store.load())
        assertNull(dataStore.data.first()[TokenStore.KEY])
    }

    // Verifies a fresh install (nothing stored) means "logged out".
    @Test
    fun load_withNothingStored_returnsNull() = runTest {
        assertNull(TokenStore(newDataStore(), FakeTokenCipher()).load())
    }

    // Verifies unreadable data (lost key, corruption) logs the user out instead of crashing.
    @Test
    fun load_withUndecryptableValue_treatsUserAsLoggedOutAndClearsIt() = runTest {
        val dataStore = newDataStore()
        dataStore.edit { it[TokenStore.KEY] = "garbage" }
        val store = TokenStore(dataStore, FakeTokenCipher())
        assertNull(store.load())
        assertNull(store.currentToken())
        assertNull(dataStore.data.first()[TokenStore.KEY])
    }
}