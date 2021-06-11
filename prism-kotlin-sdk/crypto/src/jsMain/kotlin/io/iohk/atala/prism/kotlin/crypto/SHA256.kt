package io.iohk.atala.prism.kotlin.crypto

import hash

actual object SHA256 {
    actual fun compute(bytes: ByteArray): ByteArray {
        val array = bytes.map { it.toUByte().toInt() }.toTypedArray()
        val sha256Digest = hash.sha256.invoke().update(array).digest()
        return sha256Digest.map { it.toByte() }.toByteArray()
    }
}
