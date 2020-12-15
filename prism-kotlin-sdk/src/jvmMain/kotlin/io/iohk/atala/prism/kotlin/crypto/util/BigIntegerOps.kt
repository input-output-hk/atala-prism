package io.iohk.atala.prism.kotlin.crypto.util

import java.math.BigInteger

fun BigInteger.toUnsignedByteArray(): ByteArray {
    return toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
}

fun BigInteger.toUnsignedByteList(): List<Byte> {
    return toByteArray().dropWhile { it == 0.toByte() }
}
