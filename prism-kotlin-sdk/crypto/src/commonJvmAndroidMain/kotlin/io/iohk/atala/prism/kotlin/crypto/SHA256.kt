package io.iohk.atala.prism.kotlin.crypto

import java.security.MessageDigest

actual object SHA256 {
    @JvmStatic
    actual fun compute(bytes: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        return messageDigest.digest(bytes)
    }
}
