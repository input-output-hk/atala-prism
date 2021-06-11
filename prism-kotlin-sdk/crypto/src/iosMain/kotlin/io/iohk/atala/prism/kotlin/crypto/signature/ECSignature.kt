package io.iohk.atala.prism.kotlin.crypto.signature

import cocoapods.Secp256k1Kit.*
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.util.toUByteArray
import kotlinx.cinterop.*
import platform.posix.size_tVar

actual class ECSignature actual constructor(private val data: ByteArray) : ECSignatureCommon() {
    override fun getEncoded(): ByteArray =
        data

    override fun toDer(): ByteArray {
        return memScoped {
            val context = secp256k1_context_create(SECP256K1_CONTEXT_SIGN.convert())
            val output = memScope.allocArray<UByteVar>(ECConfig.PUBLIC_KEY_BYTE_SIZE * 2)
            val outputLen = alloc<size_tVar>()
            outputLen.value = (ECConfig.PUBLIC_KEY_BYTE_SIZE * 2).convert()
            val sig = alloc<secp256k1_ecdsa_signature>()
            for (i in 0 until ECConfig.SIGNATURE_BYTE_SIZE) sig.data[i] = data[i].toUByte()
            val result = secp256k1_ecdsa_signature_serialize_der(context, output, outputLen.ptr, sig.ptr)
            if (result != 1) {
                error("Could not serialize signature to DER format")
            }
            output.toUByteArray(outputLen.value.convert()).toByteArray()
        }
    }
}
