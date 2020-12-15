package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.crypto.util.toUnsignedByteList
import java.lang.IllegalStateException
import java.security.PrivateKey

actual class ECPrivateKey(private val key: PrivateKey) : ECKey() {
    override fun getEncoded(): List<Byte> {
        val d = when (key) {
            is org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey -> key.d
            else -> throw IllegalStateException("Unexpected private key implementation")
        }
        return d.toUnsignedByteList()
    }
}
