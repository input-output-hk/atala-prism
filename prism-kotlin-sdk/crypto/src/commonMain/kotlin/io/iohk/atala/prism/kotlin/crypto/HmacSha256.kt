package io.iohk.atala.prism.kotlin.crypto

/**
 * HMAC-SHA-256 facade.
 */
expect object HmacSha256 {

    /**
     * Compute HMAC-SHA-256 data authentication code using shared key.
     */
    fun compute(data: ByteArray, key: ByteArray): ByteArray
}
