package io.iohk.atala.prism.kotlin.crypto

import com.soywiz.krypto.SecureRandom
import io.iohk.atala.prism.kotlin.crypto.AesConfig.AUTH_TAG_SIZE
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES256-GCM Java implementation.
 *
 * Can be used with:
 *   - own key with provided IV
 *   - own key with auto-generated IV
 */
actual object Aes {

    /**
     * Encrypt data with a key. The IV is created randomly.
     */
    actual fun encrypt(data: ByteArray, key: ByteArray): AesEncryptedData {
        val iv = SecureRandom.nextBytes(AesConfig.IV_SIZE)
        return encrypt(data, key, iv)
    }

    /**
     * Encrypt data with a key and IV.
     */
    actual fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): AesEncryptedData {
        val cipher = createCipher(Cipher.ENCRYPT_MODE, key, iv)

        return AesEncryptedData(
            cipher.doFinal(data).toList(),
            iv.toList(),
            salt = null
        )
    }

    /**
     * Decrypt data with key and IV.
     */
    actual fun decrypt(
        encryptedData: AesEncryptedData,
        key: ByteArray
    ): ByteArray {
        val iv = encryptedData.iv.toByteArray()
        val cipher = createCipher(Cipher.DECRYPT_MODE, key, iv)

        return cipher.doFinal(encryptedData.data.toByteArray())
    }

    /**
     * Cipher initialization.
     */
    private fun createCipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher {
        val derivedKey = SecretKeySpec(key, "AES")
        val parameters = GCMParameterSpec(AUTH_TAG_SIZE, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(mode, derivedKey, parameters)
        return cipher
    }
}
