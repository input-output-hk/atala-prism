package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.*
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleInclusionProofJS
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleRootJS

@JsExport
object CredentialVerificationJS {
    @JsName("verify")
    fun verify(
        keyData: KeyDataJS,
        credentialData: CredentialDataJS,
        credential: CredentialJS
    ) =
        CredentialVerification.verify(keyData.toKeyData(), credentialData.toCredentialData(), credential.credential)

    @JsName("verifyMerkle")
    fun verifyMerkle(
        keyData: KeyDataJS,
        batchData: BatchDataJS,
        credentialRevocationTime: TimestampInfoJS?,
        merkleRoot: MerkleRootJS,
        inclusionProof: MerkleInclusionProofJS,
        signedCredential: CredentialJS
    ) =
        CredentialVerification.verify(
            keyData.toKeyData(),
            batchData.toBatchData(),
            credentialRevocationTime?.internal,
            MerkleRoot(SHA256Digest.fromHex(merkleRoot.hash)),
            MerkleInclusionProof(
                SHA256Digest.fromHex(inclusionProof.hash),
                inclusionProof.index,
                inclusionProof.siblings.map { SHA256Digest.fromHex(it) }
            ),
            signedCredential.credential
        )
}
