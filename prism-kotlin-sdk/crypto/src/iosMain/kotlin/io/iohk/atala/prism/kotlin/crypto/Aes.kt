package io.iohk.atala.prism.kotlin.crypto

import com.soywiz.krypto.SecureRandom
import io.iohk.atala.prism.swift.cryptoKit.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

/**
 * AES256-GCM iOS implementation.
 *
 * Can be used with:
 *   - own key with provided IV
 *   - own key with auto-generated IV
 */
actual object Aes {

    actual fun encrypt(data: ByteArray, key: ByteArray): AesEncryptedData {
        val iv = SecureRandom.nextBytes(AesConfig.IV_SIZE)
        return encrypt(data, key, iv)
    }

    actual fun encrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray
    ): AesEncryptedData {
        autoreleasepool {
            return AesEncryptedData(
                data = SwiftCryptoKit.aes256GcmEncryptWithData(data.toData(), key.toData(), iv.toData()).getOrThrow().toByteArray(),
                iv = iv,
                salt = null
            )
        }
    }

    actual fun decrypt(
        encryptedData: AesEncryptedData,
        key: ByteArray
    ): ByteArray {
        autoreleasepool {
            return SwiftCryptoKit.aes256GcmDecryptWithData(
                data = encryptedData.data.toData(),
                key = key.toData(),
                iv = encryptedData.iv.toData()
            ).getOrThrow().toByteArray()
        }
    }

    private inline fun ByteArray.toData(): NSData {
        val pinned = pin()
        return NSData.create(pinned.addressOf(0), size.toULong()) { _, _ -> pinned.unpin() }
    }

    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(length.toInt()).apply {
            usePinned { memcpy(it.addressOf(0), bytes, length) }
        }
    }

    private fun DataWithError.getOrThrow(): NSData {
        this.failure()?.let { throw RuntimeException(it.description ?: "Unknown CryptoKit error.") }
        return success() ?: error("Invalid CryptoKit result.")
    }
}
