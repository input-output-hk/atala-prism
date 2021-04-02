package io.iohk.atala.prism.kotlin.crypto

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.*

/**
 * HMAC-SHA-256 iOS implementation.
 */
actual object HmacSha256 {

    /**
     * Compute HMAC-SHA-256 data authentication code using shared key.
     */
    actual fun compute(
        data: ByteArray,
        key: ByteArray
    ): ByteArray {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)

        key.usePinned { keyPinned ->
            data.usePinned { dataPinned ->
                digest.usePinned { digestPinned ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        keyPinned.addressOf(0),
                        key.size.convert(),
                        dataPinned.addressOf(0),
                        data.size.convert(),
                        digestPinned.addressOf(0)
                    )
                }
            }
        }

        return digest.toByteArray()
    }
}
