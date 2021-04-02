package io.iohk.atala.prism.kotlin.crypto

/**
 * Encrypted AES256-GCM data.
 *
 * @param data Encrypted data
 * @param iv Initialization vector
 * @param salt Salt used to derive key from password string
 */
data class AesEncryptedData(val data: List<Byte>, val iv: List<Byte>, val salt: List<Byte>?) {
    fun combined(): ByteArray = (data + iv).toByteArray()

    companion object {
        fun fromCombined(bytes: ByteArray): AesEncryptedData {
            return AesEncryptedData(
                data = bytes.takeLast(bytes.size - AesConfig.IV_SIZE),
                iv = bytes.take(AesConfig.IV_SIZE),
                salt = null
            )
        }
    }
}

/**
 * AES default parameters.
 */
object AesConfig {
    const val IV_SIZE = 64 // size in bits
    const val KEY_SIZE = 256 // size in bits
    const val AUTH_TAG_SIZE = 128 // size in bits
}

/**
 * AES256-GCM facade.
 *
 * Can be used with:
 *   - own key with provided IV
 *   - own key with auto-generated IV
 */
expect object Aes {

    /**
     * Encrypt data with a key. The IV is created randomly.
     */
    fun encrypt(data: ByteArray, key: ByteArray): AesEncryptedData

    /**
     * Encrypt data with a key and IV.
     */
    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): AesEncryptedData

    /**
     * Decrypt data with a key and IV.
     */
    fun decrypt(encryptedData: AesEncryptedData, key: ByteArray): ByteArray
}
