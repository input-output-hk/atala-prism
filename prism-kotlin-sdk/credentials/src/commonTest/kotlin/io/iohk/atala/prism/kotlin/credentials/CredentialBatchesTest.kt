package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.AtalaOperation
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pbandk.ByteArr
import pbandk.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CredentialBatchesTest {

    private val stateHashDummy = "5f7802238f5d64a48fda6cc13a9467b2065248d31a94129ed0c0ea96d9b341a0"

    @Test
    fun testUpdateDIDAtalaOperationShouldReturnSignedUpdateDIDOperation() {
        val singingKeyPair = EC.generateKeyPair()
        val singingKeyId = "signingKeyId"
        val did = DID.buildPrismDID(stateHashDummy)

        val credentialClaim1 = JsonObject(
            mapOf(
                Pair("name", JsonPrimitive("José López Portillo")),
                Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
            )
        )

        val credentialClaim2 = JsonObject(
            mapOf(
                Pair("name", JsonPrimitive("Farfadelly Linguiny")),
                Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
            )
        )

        val credentials1 =
            JsonBasedCredential(
                CredentialContent(
                    JsonObject(
                        mapOf(
                            Pair("id", JsonPrimitive(did.value)),
                            Pair("keyId", JsonPrimitive(singingKeyId)),
                            Pair(
                                "credentialSubject",
                                credentialClaim1
                            ),
                        )
                    )
                )
            )

        val credentials2 =
            JsonBasedCredential(
                CredentialContent(
                    JsonObject(
                        mapOf(
                            Pair("id", JsonPrimitive(did.value)),
                            Pair("keyId", JsonPrimitive(singingKeyId)),
                            Pair(
                                "credentialSubject",
                                credentialClaim2
                            ),
                        )
                    )
                )
            )

        val result = CredentialBatches.createBatchAtalaOperation(
            did,
            singingKeyId,
            singingKeyPair.privateKey,
            listOf(credentialClaim1.toString(), credentialClaim2.toString())
        )

        assertEquals(
            SHA256Digest.compute(result.signedAtalaOperation.operation!!.encodeToByteArray()),
            result.issuanceOperationHash
        )

        assertEquals(
            did.suffix.value,
            (result.signedAtalaOperation.operation as AtalaOperation).issueCredentialBatch!!.credentialBatchData!!.issuerDid
        )

        val resultMerkleRoot =
            (result.signedAtalaOperation.operation as AtalaOperation).issueCredentialBatch!!.credentialBatchData!!.merkleRoot

        val expectedBatchId = CredentialBatchId.fromDigest(
            SHA256Digest.compute(
                CredentialBatchData(
                    did.suffix.value,
                    resultMerkleRoot
                ).encodeToByteArray()
            )
        )

        assertEquals(
            expectedBatchId,
            result.batchId
        )

        assertTrue(
            EC.verify(
                credentials1.contentBytes,
                singingKeyPair.publicKey,
                result.credentialsAndProofs[0].signedCredential.signature!!
            )
        )

        assertTrue(
            EC.verify(
                credentials2.contentBytes,
                singingKeyPair.publicKey,
                result.credentialsAndProofs[1].signedCredential.signature!!
            )
        )

        assertTrue(
            CredentialBatches.verifyInclusion(
                result.credentialsAndProofs[0].signedCredential,
                result.credentialsAndProofs[0].inclusionProof.derivedRoot(),
                result.credentialsAndProofs[0].inclusionProof
            )
        )

        assertTrue(
            CredentialBatches.verifyInclusion(
                result.credentialsAndProofs[1].signedCredential,
                result.credentialsAndProofs[1].inclusionProof.derivedRoot(),
                result.credentialsAndProofs[1].inclusionProof
            )
        )

        assertEquals(resultMerkleRoot, ByteArr(result.credentialsAndProofs[0].inclusionProof.derivedRoot().hash.value))

        assertEquals(resultMerkleRoot, ByteArr(result.credentialsAndProofs[1].inclusionProof.derivedRoot().hash.value))
    }
}
