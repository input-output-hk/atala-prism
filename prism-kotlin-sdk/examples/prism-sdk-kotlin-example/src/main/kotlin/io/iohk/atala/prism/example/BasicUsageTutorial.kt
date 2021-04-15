package io.iohk.atala.prism.example

import io.iohk.atala.prism.kotlin.credentials.*
import io.iohk.atala.prism.kotlin.credentials.content.*
import io.iohk.atala.prism.kotlin.credentials.json.*
import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.identity.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*

/**
 * This reflects the snippets that are in the markdown docs, allowing us to get autocompletion
 * while editing them.
 *
 * See /src/docs/usage-tutorial
 */
class BasicUsageTutorial {
    fun run() {
        val masterKeyPair = EC.generateKeyPair()
        val did = DID.createUnpublishedDID(masterKeyPair.publicKey)

        val credentialContent = CredentialContent(
            JsonObject(
                mapOf(
                    Pair("issuerDid", JsonPrimitive(did.value)),
                    Pair("issuanceKeyId", JsonPrimitive("Issuance-0")),
                    Pair(
                        "credentialSubject",
                        JsonObject(
                            mapOf(
                                Pair("name", JsonPrimitive("José López Portillo")),
                                Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
                            )
                        )
                    ),
                )
            )
        )

        val credential = JsonBasedCredential(credentialContent)
        val signedCredential = credential.sign(masterKeyPair.privateKey)
        val (merkleRoot, merkleProofs) = CredentialBatches.batch(listOf(signedCredential))

        // assume there is a block in Cardano that includes the DID, and the credential, which was confirmed 1 minute ago
        val didBlockInfo = TimestampInfo(
            atalaBlockTimestamp = Clock.System.now().minus(60, DateTimeUnit.SECOND).epochSeconds,
            atalaBlockSequenceNumber = 1,
            operationSequenceNumber = 1
        )
        val batchBlockInfo = TimestampInfo(
            atalaBlockTimestamp = Clock.System.now().minus(20, DateTimeUnit.SECOND).epochSeconds,
            atalaBlockSequenceNumber = 2,
            operationSequenceNumber = 2
        )

        // this metadata about the DID key should be retrieved from the Cardano network
        val keyData = KeyData(publicKey = masterKeyPair.publicKey, addedOn = didBlockInfo, revokedOn = null)

        // this credential batch metadata should be retrieved from the Cardano network
        val batchData = BatchData(issuedOn = batchBlockInfo, revokedOn = null)

        CredentialVerification.verify(
            keyData = keyData,
            batchData = batchData,
            credentialRevocationTime = null,
            merkleRoot = merkleRoot,
            inclusionProof = merkleProofs.first(), // the batch includes a single credential
            signedCredential = signedCredential
        )
    }
}
