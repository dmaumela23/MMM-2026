package com.maumela.magnummanagement.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption with a key generated INSIDE the Android Keystore.
 * The key material never leaves secure hardware/OS storage and cannot be exported,
 * so a copy of the app's data files is useless without the device.
 *
 * Stored format:  base64(iv) ":" base64(ciphertext+authTag)
 * GCM is authenticated: tampered data fails to decrypt instead of returning garbage.
 */
class KeystoreTokenCipher(private val keyAlias: String = KEY_ALIAS) : TokenCipher {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey()) // Keystore generates a fresh random IV
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return encode(cipher.iv) + SEPARATOR + encode(cipherText)
    }

    override fun decrypt(payload: String): String {
        val parts = payload.split(SEPARATOR)
        require(parts.size == 2) { "Malformed encrypted token" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, decode(parts[0])),
        )
        return String(cipher.doFinal(decode(parts[1])), Charsets.UTF_8)
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mmm_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val SEPARATOR = ":"
    }
}