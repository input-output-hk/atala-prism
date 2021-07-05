package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation
import kotlin.js.JsExport

@JsExport
data class UpdateDIDContext(
    val updateDIDSignedOperation: SignedAtalaOperation,
    val operationHash: SHA256Digest
)
