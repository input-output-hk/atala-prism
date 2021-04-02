package io.iohk.atala.prism.kotlin.crypto

import com.soywiz.krypto.SecureRandom
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps
import io.iohk.atala.prism.kotlin.crypto.util.toByteArray
import kotlinx.browser.window

/**
 * AES256-GCM iOS implementation.
 *
 * Can be used with:
 *   - own key with provided IV
 *   - own key with auto-generated IV
 *
 * @todo Only node.js implementation, the browser requires to use coroutines, and can be difficult to provide.
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
    actual fun encrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray
    ): AesEncryptedData {
        if (jsTypeOf(window) != "undefined") TODO("Browser version is not available.")

        val encryptedData = js(
            """
                var crypto = require('crypto')
                var cipher = crypto.createCipheriv("aes-256-gcm", key, iv)
                Buffer.concat([cipher.update(data), cipher.final(), cipher.getAuthTag()]).toString('hex')
            """
        )

        return AesEncryptedData(
            data = BytesOps.hexToBytes(encryptedData as String).toByteArray().toList(),
            iv = iv.toList(),
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
        if (jsTypeOf(window) != "undefined") TODO("Browser version is not available.")

        val iv = encryptedData.iv.toByteArray()
        val authTag = encryptedData.data.takeLast(AesConfig.AUTH_TAG_SIZE / 8).toByteArray()
        val encrypted = encryptedData.data.take(encryptedData.data.size - AesConfig.AUTH_TAG_SIZE / 8).toByteArray()

        val decryptedData = js(
            """
                var crypto = require('crypto')
                var cipher = crypto.createDecipheriv("aes-256-gcm", key, iv)
                cipher.setAuthTag(authTag)
                Buffer.concat([cipher.update(encrypted), cipher.final()]).toString('hex')
            """
        )

        return BytesOps.hexToBytes(decryptedData as String).toByteArray()
    }
}
