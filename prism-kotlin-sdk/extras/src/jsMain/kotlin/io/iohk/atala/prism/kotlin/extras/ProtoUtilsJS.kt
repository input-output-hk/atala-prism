package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.credentials.exposed.CredentialBatchIdJS
import io.iohk.atala.prism.kotlin.credentials.exposed.CredentialJS
import io.iohk.atala.prism.kotlin.credentials.exposed.toKotlin
import io.iohk.atala.prism.kotlin.crypto.exposed.ECKeyPairJS
import io.iohk.atala.prism.kotlin.crypto.exposed.SHA256DigestJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toEcKeyPair
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.protos.*

@JsExport
object ProtoUtilsJS {
    @JsName("createDidAtalaOperation")
    fun createDidAtalaOperation(ecKeyPair: ECKeyPairJS): AtalaOperation =
        ProtoUtils.createDidAtalaOperation(ecKeyPair.toEcKeyPair())

    @JsName("issueCredentialBatchOperation")
    fun issueCredentialBatchOperation(data: CredentialBatchData): AtalaOperation =
        ProtoUtils.issueCredentialBatchOperation(data)

    @JsName("revokeCredentialsOperation")
    fun revokeCredentialsOperation(
        batchOperationHash: SHA256DigestJS,
        batchId: CredentialBatchIdJS,
        credentials: Array<CredentialJS>
    ): AtalaOperation =
        ProtoUtils.revokeCredentialsOperation(
            batchOperationHash.toKotlin(),
            batchId.toKotlin(),
            credentials.map { it.toKotlin() }
        )

    @JsName("signedAtalaOperation")
    fun signedAtalaOperation(ecKeyPair: ECKeyPairJS, atalaOperation: AtalaOperation): SignedAtalaOperation =
        ProtoUtils.signedAtalaOperation(ecKeyPair.toEcKeyPair(), atalaOperation)
}
