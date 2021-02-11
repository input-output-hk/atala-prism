package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign

actual class ECPrivateKey(internal val key: UByteArray) : ECKey() {
    override fun getEncoded(): List<Byte> {
        return key.toByteArray().toList()
    }

    actual fun getD(): BigInteger {
        return BigInteger.fromUByteArray(key, Sign.POSITIVE)
    }
}
