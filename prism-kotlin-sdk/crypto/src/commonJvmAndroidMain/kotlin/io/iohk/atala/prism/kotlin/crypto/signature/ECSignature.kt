package io.iohk.atala.prism.kotlin.crypto.signature

actual class ECSignature actual constructor(val data: ByteArray) : ECSignatureCommon() {
    override fun getEncoded(): ByteArray =
        data
}
