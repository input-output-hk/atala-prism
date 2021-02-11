package io.iohk.atala.prism.kotlin.crypto.signature

import cocoapods.Secp256k1Kit.*
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex
import io.iohk.atala.prism.kotlin.crypto.util.toUByteArray
import kotlinx.cinterop.*
import platform.posix.size_tVar

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
        return memScoped {
            val context = secp256k1_context_create(SECP256K1_CONTEXT_SIGN.convert())
            val output = memScope.allocArray<UByteVar>(ECConfig.PUBLIC_KEY_BYTE_SIZE * 2)
            val outputLen = alloc<size_tVar>()
            outputLen.value = (ECConfig.PUBLIC_KEY_BYTE_SIZE * 2).convert()
            val sig = alloc<secp256k1_ecdsa_signature>()
            for (i in 0 until ECConfig.SIGNATURE_BYTE_SIZE) sig.data[i] = data[i]
            val result = secp256k1_ecdsa_signature_serialize_der(context, output, outputLen.ptr, sig.ptr)
            if (result != 1) {
                error("Could not serialize signature to DER format")
            }
            output.toUByteArray(outputLen.value.convert()).toByteArray().toList()
        }
    }
}
