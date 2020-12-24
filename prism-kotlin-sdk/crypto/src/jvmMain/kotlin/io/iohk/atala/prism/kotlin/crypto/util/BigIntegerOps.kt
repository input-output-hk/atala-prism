package io.iohk.atala.prism.kotlin.crypto.util

import com.ionspin.kotlin.bignum.integer.Sign
import java.lang.IllegalStateException
import java.math.BigInteger

fun BigInteger.toUnsignedByteArray(): ByteArray {
    return toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
}

fun BigInteger.toUnsignedByteList(): List<Byte> {
    return toByteArray().dropWhile { it == 0.toByte() }
}

fun ByteArray.toBigInteger(): com.ionspin.kotlin.bignum.integer.BigInteger {
    return com.ionspin.kotlin.bignum.integer.BigInteger.fromByteArray(this, Sign.POSITIVE)
}

fun BigInteger.toBigInteger(): com.ionspin.kotlin.bignum.integer.BigInteger {
    val sign = when (this.signum()) {
        -1 -> Sign.NEGATIVE
        0 -> Sign.ZERO
        1 -> Sign.POSITIVE
        else -> throw IllegalStateException("Illegal BigInteger sign")
    }
    return com.ionspin.kotlin.bignum.integer.BigInteger.fromByteArray(this.toUnsignedByteArray(), sign)
}

fun com.ionspin.kotlin.bignum.integer.BigInteger.toJavaBigInteger(): BigInteger {
    return BigInteger(this.signum(), this.toByteArray())
}
