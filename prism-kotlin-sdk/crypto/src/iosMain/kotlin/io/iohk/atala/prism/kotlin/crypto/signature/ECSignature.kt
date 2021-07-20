package io.iohk.atala.prism.kotlin.crypto.signature

actual class ECSignature actual constructor(private val data: ByteArray) : ECSignatureCommon() {
    override fun getEncoded(): ByteArray =
        data
}
