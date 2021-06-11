package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix

@JsExport
object CredentialBatchIdCompanion {
    fun fromString(id: String): CredentialBatchId? =
        CredentialBatchId.fromString(id)

    fun fromDigest(digest: SHA256Digest): CredentialBatchId =
        CredentialBatchId.fromDigest(digest)

    fun fromBatchData(issuerDIDSuffix: DIDSuffix, merkleRoot: MerkleRoot): CredentialBatchId =
        CredentialBatchId.fromBatchData(issuerDIDSuffix, merkleRoot)

    fun random(): CredentialBatchId =
        CredentialBatchId.random()
}
