package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.identity.DIDUrl

@JsExport
class DIDUrlJS private constructor(didUrl: DIDUrl) {
    companion object {
        @JsName("fromString")
        fun fromString(rawDidUrl: String): DIDUrlJS =
            DIDUrlJS(DIDUrl.fromString(rawDidUrl))
    }

    val keyId: String? = didUrl.keyId
}
