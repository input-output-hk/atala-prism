package io.iohk.atala.prism.kotlin.crypto

import BlockHash
import hash

/**
 * HMAC-SHA-256 JavaScript implementation.
 */
@JsExport
actual object HmacSha256 {

    /**
     * Compute HMAC-SHA-256 data authentication code using shared key.
     */
    actual fun compute(
        data: ByteArray,
        key: ByteArray
    ): ByteArray {
        val hmac = hash.hmac(hash.sha256 as BlockHash<Any>, key) // ugly type casting, but it works
        return hmac.update(data).digest().map { it.toByte() }.toByteArray()
    }
}
