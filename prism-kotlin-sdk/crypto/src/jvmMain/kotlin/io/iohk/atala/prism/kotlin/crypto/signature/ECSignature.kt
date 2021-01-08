package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.util.BytesOps.bytesToHex
import io.iohk.atala.prism.kotlin.util.toByteArray
import kotlin.experimental.and

@ExperimentalUnsignedTypes
actual data class ECSignature actual constructor(val data: List<UByte>) {
    actual fun getEncoded(): List<Byte> =
        data.map { it.toByte() }

    actual fun getHexEncoded(): String {
        return bytesToHex(data)
    }

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
    actual fun toDer(): List<Byte> {
        val size = data.size

        val rb = data.slice(0 until size / 2).toByteArray().dropWhile { it == 0.toByte() }.toMutableList()
        val sb = data.slice(size / 2 until size).toByteArray().dropWhile { it == 0.toByte() }.toMutableList()

        // pad values
        if ((rb[0] and 0x80.toByte()) > 0.toByte()) {
            rb.add(0, 0x0)
        }
        if ((sb[0] and 0x80.toByte()) > 0.toByte()) {
            sb.add(0, 0x0)
        }

        val len = rb.size + sb.size + 4

        val intro = if (len >= 128) byteArrayOf(0x30.toByte(), 0x81.toByte()) else byteArrayOf(0x30.toByte())
        val first = intro + byteArrayOf(len.toByte(), 0x02.toByte(), rb.size.toByte())
        val second = byteArrayOf(0x02.toByte(), sb.size.toByte())

        return first.toList() + rb.toList() + second.toList() + sb.toList()
    }
}
