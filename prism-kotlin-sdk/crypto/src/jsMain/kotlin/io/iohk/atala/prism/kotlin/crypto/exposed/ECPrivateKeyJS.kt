package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex

@JsExport
class ECPrivateKeyJS internal constructor(internal val privateKey: ECPrivateKey) {
    fun getHexEncoded(): String =
        privateKey.getHexEncoded()

    fun getEncoded(): ByteArray =
        privateKey.getEncoded()

    fun getDHex(): String =
        bytesToHex(getD())

    fun getD(): ByteArray {
        val d = privateKey.getD()
        return d.toByteArray()
    }
}

fun ECPrivateKey.toJs(): ECPrivateKeyJS =
    ECPrivateKeyJS(this)

fun ECPrivateKeyJS.toKotlin(): ECPrivateKey =
    this.privateKey
