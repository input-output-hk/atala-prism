package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.externals.BN

actual class ECPrivateKey(val bigNumber: BN) : ECKey() {

    override fun getEncoded(): List<Byte> {
        val byteList = bigNumber.toArray().map { it.toByte() }
        val padding = List(ECConfig.PRIVATE_KEY_BYTE_SIZE - byteList.size) {
            0.toByte()
        }
        return padding + byteList
    }

    actual fun getD(): BigInteger {
        return BigInteger.parseString(bigNumber.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        if (!super.equals(other)) return false

        other as ECPrivateKey

        if (getHexEncoded() != other.getHexEncoded()) return false

        return true
    }

    override fun hashCode(): Int =
        getHexEncoded().hashCode()
}
