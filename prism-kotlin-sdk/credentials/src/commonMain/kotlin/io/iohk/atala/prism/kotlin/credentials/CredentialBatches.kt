package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.identity.util.ECProtoOps
import io.iohk.atala.prism.kotlin.protos.AtalaOperation
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import io.iohk.atala.prism.kotlin.protos.IssueCredentialBatchOperation
import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation
import kotlinx.serialization.json.*
import pbandk.ByteArr
import pbandk.encodeToByteArray
import kotlin.js.JsExport
import kotlin.jvm.JvmStatic

@JsExport
data class CredentialBatch(
    val root: MerkleRoot,
    val proofs: List<MerkleInclusionProof>
)

@JsExport
object CredentialBatches {

    @JvmStatic
    fun batch(
        signedCredentials: List<PrismCredential>
    ): CredentialBatch {
        val merkleProofs = generateProofs(
            signedCredentials.map { it.hash() }
        )

        return CredentialBatch(merkleProofs.root, merkleProofs.proofs)
    }

    @JvmStatic
    fun verifyInclusion(
        signedCredential: PrismCredential,
        merkleRoot: MerkleRoot,
        inclusionProof: MerkleInclusionProof
    ): Boolean {
        return signedCredential.hash() == inclusionProof.hash &&
            verifyProof(merkleRoot, inclusionProof)
    }

    @JvmStatic
    fun computeCredentialBatchId(did: DID, merkleRoot: MerkleRoot): CredentialBatchId {
        val data = CredentialBatchData(did.suffix.value, ByteArr(merkleRoot.hash.value))
        return CredentialBatchId.fromDigest(SHA256Digest.compute(data.encodeToByteArray()))
    }

    fun createBatchAtalaOperation(
        issuerDID: DID,
        signingKeyId: String,
        issuingPrivateKey: ECPrivateKey,
        credentialsClaims: List<String>
    ): IssueBatchContext {
        require(issuerDID.isCanonicalForm()) {
            "DID should be in canonical form, found DID: $issuerDID"
        }

        val signedCredentials =
            credentialsClaims.map { credentialClaim ->
                JsonBasedCredential(
                    CredentialContent(
                        JsonObject(
                            mapOf(
                                Pair("id", JsonPrimitive(issuerDID.value)),
                                Pair("keyId", JsonPrimitive(signingKeyId)),
                                Pair(
                                    "credentialSubject",
                                    Json.parseToJsonElement(credentialClaim)
                                ),
                            )
                        )
                    )
                ).sign(issuingPrivateKey)
            }

        val (credentialsMerkleRoot, credentialsMerkleProofs) = CredentialBatches.batch(signedCredentials)

        val issueCredentialBatchAtalaOperation =
            AtalaOperation(
                AtalaOperation.Operation.IssueCredentialBatch(
                    IssueCredentialBatchOperation(
                        CredentialBatchData(
                            issuerDid = issuerDID.suffix.value,
                            merkleRoot = pbandk.ByteArr(credentialsMerkleRoot.hash.value)
                        )
                    )
                )
            )

        val credentialContexts = signedCredentials.zip(credentialsMerkleProofs).map {
            val (signedCredential, proof) = it
            CredentialInfo(signedCredential, proof)
        }

        return IssueBatchContext(
            signedAtalaOperation = ECProtoOps.signedAtalaOperation(
                issuingPrivateKey,
                signingKeyId,
                issueCredentialBatchAtalaOperation
            ),
            issuanceOperationHash = SHA256Digest.compute(issueCredentialBatchAtalaOperation.encodeToByteArray()),
            batchId = computeCredentialBatchId(issuerDID, credentialsMerkleRoot),
            credentialsAndProofs = credentialContexts
        )
    }
}

@JsExport
data class IssueBatchContext(
    val signedAtalaOperation: SignedAtalaOperation,
    val issuanceOperationHash: SHA256Digest,
    val batchId: CredentialBatchId,
    val credentialsAndProofs: List<CredentialInfo>
)

@JsExport
data class CredentialInfo(
    val signedCredential: PrismCredential,
    val inclusionProof: MerkleInclusionProof
)
