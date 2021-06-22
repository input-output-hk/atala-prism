package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.credentials.PrismCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.protos.*
import pbandk.ByteArr
import pbandk.encodeToByteArray
import kotlin.js.JsExport

@JsExport
object ProtoUtils {
    fun createDidAtalaOperation(ecKeyPair: ECKeyPair): AtalaOperation {
        val publicKey =
            ecKeyPair.publicKey.toECKeyData().toPublicKey(masterKeyId, KeyUsage.MASTER_KEY)
        val didData = DIDData(publicKeys = listOf(publicKey))
        val createDIDOperation = CreateDIDOperation(didData)
        return AtalaOperation(AtalaOperation.Operation.CreateDid(createDIDOperation))
    }

    fun issueCredentialBatchOperation(data: CredentialBatchData): AtalaOperation {
        val isssueCredentialBatch = IssueCredentialBatchOperation(data)
        return AtalaOperation(AtalaOperation.Operation.IssueCredentialBatch(isssueCredentialBatch))
    }

    fun revokeCredentialsOperation(
        batchOperationHash: SHA256Digest,
        batchId: CredentialBatchId,
        credentials: List<PrismCredential>
    ): AtalaOperation {
        val revokeCredentialsService = RevokeCredentialsOperation(
            previousOperationHash = ByteArr(batchOperationHash.value),
            credentialBatchId = batchId.id,
            credentialsToRevoke = credentials.map { ByteArr(it.hash().value) }
        )
        return AtalaOperation(AtalaOperation.Operation.RevokeCredentials(revokeCredentialsService))
    }

    fun signedAtalaOperation(ecKeyPair: ECKeyPair, atalaOperation: AtalaOperation): SignedAtalaOperation {
        val signature = EC.sign(atalaOperation.encodeToByteArray(), ecKeyPair.privateKey)
        return SignedAtalaOperation(
            signedWith = masterKeyId,
            signature = ByteArr(signature.getEncoded()),
            operation = atalaOperation
        )
    }
}
