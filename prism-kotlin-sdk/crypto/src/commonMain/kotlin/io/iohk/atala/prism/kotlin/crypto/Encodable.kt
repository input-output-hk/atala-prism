package io.iohk.atala.prism.kotlin.crypto

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps

interface Encodable {
    /**
     * @return encoded version of the entity
     */
    fun getEncoded(): ByteArray

    /**
     * @return hex version of [getEncoded]
     */
    fun getHexEncoded(): String {
        return BytesOps.bytesToHex(getEncoded())
    }
}
