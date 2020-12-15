package io.iohk.atala.prism.kotlin.crypto.keys

@ExperimentalUnsignedTypes
actual class ECPublicKey(private val keyBytes: UByteArray) : ECKey() {
    override fun getEncoded(): List<Byte> {
        return keyBytes.toByteArray().toList()
    }
}
