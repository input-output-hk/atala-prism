package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature

@JsExport
class ECSignatureJS internal constructor(internal val signature: ECSignature) {
    fun getHexEncoded(): String =
        signature.getHexEncoded()

    fun getEncoded(): ByteArray =
        signature.getEncoded()
}

fun ECSignature.toJs(): ECSignatureJS =
    ECSignatureJS(this)

fun ECSignatureJS.toKotlin(): ECSignature =
    this.signature
