package io.iohk.atala.prism.kotlin.crypto.signature

@ExperimentalUnsignedTypes
expect class ECSignature(data: List<UByte>) {
    fun getEncoded(): List<Byte>

    fun getHexEncoded(): String

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
    fun toDer(): List<Byte>
}
