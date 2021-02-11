package io.iohk.atala.prism.kotlin.crypto

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.*

actual object SHA256 {
    actual fun compute(bytes: List<Byte>): List<Byte> {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.toByteArray().usePinned { bytesPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(bytesPinned.addressOf(0), bytes.size.convert(), digestPinned.addressOf(0))
            }
        }
        return digest.toByteArray().toList()
    }
}
