package io.iohk.atala.prism.kotlin.crypto

import java.security.MessageDigest

@ExperimentalUnsignedTypes
actual object SHA256 {
    @JvmStatic
    actual fun compute(bytes: UByteArray): UByteArray {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        return messageDigest.digest(bytes.toByteArray()).toUByteArray()
    }
}
