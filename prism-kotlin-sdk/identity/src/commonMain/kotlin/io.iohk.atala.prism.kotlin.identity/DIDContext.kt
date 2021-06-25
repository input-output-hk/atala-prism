package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation
import kotlin.js.JsExport

@JsExport
data class DIDContext(
    val did: DID,
    val createDIDOperation: SignedAtalaOperation,
    val createDIDOperationHash: SHA256Digest
)
