package io.iohk.atala.prism.kotlin.crypto.keys

@ExperimentalUnsignedTypes
actual class ECPrivateKey(private val key: UByteArray) : ECKey() {
    override fun getEncoded(): List<Byte> {
        return key.toByteArray().toList()
    }
}
