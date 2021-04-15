package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.SHA256

@JsExport
object SHA256JS {
    @JsName("compute")
    fun compute(bytes: ByteArray): ByteArray =
        SHA256.compute(bytes.toList()).toByteArray()
}
