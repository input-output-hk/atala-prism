package io.iohk.atala.prism.kotlin.crypto

import java.security.MessageDigest

actual object SHA256 {
    @JvmStatic
    actual fun compute(bytes: List<Byte>): List<Byte> {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        return messageDigest.digest(bytes.toByteArray()).toList()
    }
}
