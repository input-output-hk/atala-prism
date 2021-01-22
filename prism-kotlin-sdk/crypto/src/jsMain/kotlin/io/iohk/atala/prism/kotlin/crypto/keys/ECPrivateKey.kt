package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.externals.BN

actual class ECPrivateKey(val bigNumber: BN) : ECKey() {

    override fun getEncoded(): List<Byte> {
        return bigNumber.toArray().map { it.toByte() }
    }

    actual fun getD(): BigInteger {
        return BigInteger.parseString(bigNumber.toString())
    }

    @ExperimentalUnsignedTypes
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        if (!super.equals(other)) return false

        other as ECPrivateKey

        if (getHexEncoded() != other.getHexEncoded()) return false

        return true
    }

    @ExperimentalUnsignedTypes
    override fun hashCode(): Int =
        getHexEncoded().hashCode()
}
