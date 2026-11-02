package com.maumela.magnummanagement.data.local

/**
 * Encrypts and decrypts the JWT before it is stored.
 * An interface so the real Android Keystore version can be replaced by a fake in JVM unit tests.
 */
interface TokenCipher {
    /** Returns text that is safe to store (it reveals nothing about [plain]). */
    fun encrypt(plain: String): String

    /** Reverses [encrypt]. Throws if the data is corrupt or the key is no longer available. */
    fun decrypt(payload: String): String
}