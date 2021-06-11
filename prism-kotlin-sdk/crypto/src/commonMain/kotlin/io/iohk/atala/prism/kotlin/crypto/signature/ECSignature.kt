package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps

abstract class ECSignatureCommon {
    abstract fun getEncoded(): ByteArray

    fun getHexEncoded(): String =
        BytesOps.bytesToHex(getEncoded())

    /**
     * Conversion form P1363 to ASN.1/DER
     *
     * P1363 contains two integer wothout separator, ASN.1 signature format looks like:
     *
     * {{{
     *   ECDSASignature ::= SEQUENCE {
     *     r INTEGER,
     *     s INTEGER
     *   }
     * }}}
     *
     * Explaination for DER encoding:
     *
     * - 0x30 - is a SEQUENCE
     * - 0x02 - is a INTEGER
     *
     * Additional padding required by the requirement to hold values larger than 128 bytes.
     *
     * The solution is inspired by: https://github.com/pauldijou/jwt-scala/blob/master/core/src/main/scala/JwtUtils.scala#L254-L290
     */
    abstract fun toDer(): ByteArray

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ECSignatureCommon

        if (!getEncoded().contentEquals(other.getEncoded())) return false

        return true
    }

    override fun hashCode(): Int =
        getEncoded().hashCode()
}

expect class ECSignature(data: ByteArray) : ECSignatureCommon
