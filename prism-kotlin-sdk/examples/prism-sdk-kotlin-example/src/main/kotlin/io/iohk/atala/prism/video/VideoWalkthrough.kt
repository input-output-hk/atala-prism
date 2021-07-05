@file:Suppress("BlockingMethodInNonBlockingContext")

package io.iohk.atala.prism.video

import io.iohk.atala.prism.kotlin.credentials.BatchData
import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.credentials.CredentialVerification
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.credentials.utils.toTimestampInfoModel
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.Hash
import io.iohk.atala.prism.kotlin.extras.ProtoClientUtils
import io.iohk.atala.prism.kotlin.extras.ProtoUtils
import io.iohk.atala.prism.kotlin.extras.findPublicKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.identity.util.ECProtoOps
import io.iohk.atala.prism.kotlin.protos.CreateDIDRequest
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import io.iohk.atala.prism.kotlin.protos.GetBatchStateRequest
import io.iohk.atala.prism.kotlin.protos.GetCredentialRevocationTimeRequest
import io.iohk.atala.prism.kotlin.protos.GetDidDocumentRequest
import io.iohk.atala.prism.kotlin.protos.IssueCredentialBatchRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * This is the code example used for the Africa Special event.
 *
 * Make sure to run the node pointing to the Cardano Testnet
 */
@ExperimentalUnsignedTypes
suspend fun main() {
    fun pressEnterToContinue() {
        println("Press enter to continue...")
        while (System.`in`.read() != 10)
            println()
    }

    println(
        """
        Welcome to the PRISM walk-through, which covers:
        1. An Issuer generates a DID that gets published to Cardano, required to issue a credential.
        2. A Holder generates an Unpublished DID (no Cardano network involved), required to issue the credential.
        3. The Issuer generates a credential for Holder, publishing it to Cardano.
        4. A Verifier resolves the state of the Issuer DID, and the Holder's credential.
        5. Verifier runs the credential verification process.
        """.trimIndent()
    )
    println()

    val node = ProtoClientUtils.nodeClient("localhost", 50053)
    println("\n*************** STEP 1")
    println("\nLet's start by generating a DID for the credential's Issuer")
    pressEnterToContinue()

    // Generate Issuer's DID
    println("Generate Issuer's DID")
    val issuerMasterKeyPair = EC.generateKeyPair()
    val issuerCreatedDIDSignedOperation = ECProtoOps.signedAtalaOperation(
        issuerMasterKeyPair.privateKey,
        "master0",
        ProtoUtils.createDidAtalaOperation(issuerMasterKeyPair)
    )

    val issuerRegisterDIDResponse = node.CreateDID(CreateDIDRequest(issuerCreatedDIDSignedOperation))
    val issuerDID = DID.buildPrismDID(issuerRegisterDIDResponse.id)

    println(
        """
        Issuer DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
        - DID: $issuerDID
        - Operation identifier: ${issuerRegisterDIDResponse.operationId}
        """.trimIndent()
    )
    println("\nNext, let's generate a DID for the Holder, the credential will be issued to it")
    pressEnterToContinue()
    println("\n*************** STEP 2")

    // Generate Holder's DID
    val holderMasterKeyPair = EC.generateKeyPair()
    val holderUnpublishedDID = DID.createUnpublishedDID(holderMasterKeyPair.publicKey)
    println("Holder's Unpublished DID generated: $holderUnpublishedDID")
    println("\nNext, let's create a credential")
    pressEnterToContinue()
    println("\n*************** STEP 3")

    // Issuer generates a credential to Holder
    val issuanceKeyId = masterKeyId
    val holderCredentialContent = CredentialContent(
        JsonObject(
            mapOf(
                Pair("issuerDid", JsonPrimitive(issuerDID.value)),
                Pair("issuanceKeyId", JsonPrimitive(issuanceKeyId)),
                Pair(
                    "credentialSubject",
                    JsonObject(
                        mapOf(
                            Pair("did", JsonPrimitive(holderUnpublishedDID.value)),
                            Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
                        )
                    )
                ),
            )
        )
    )

    val holderUnsignedCredential = JsonBasedCredential(holderCredentialContent)
    val holderSignedCredential = holderUnsignedCredential.sign(issuerMasterKeyPair.privateKey)

    // Include the credential in a batch
    val (holderCredentialMerkleRoot, holderCredentialMerkleProofs) = CredentialBatches.batch(
        listOf(
            holderSignedCredential
        )
    )
    val credentialBatchData = CredentialBatchData(
        issuerDid = issuerDID.suffix.value, // This requires the suffix only, as the node stores only suffixes
        merkleRoot = pbandk.ByteArr(holderCredentialMerkleRoot.hash.value)
    )

    // Issuer publishes the credential to Cardano
    val signedIssueCredentialOperation = ECProtoOps.signedAtalaOperation(
        issuerMasterKeyPair.privateKey,
        "master0",
        ProtoUtils.issueCredentialBatchOperation(credentialBatchData)
    )
    val issuedCredentialResponse = node.IssueCredentialBatch(
        IssueCredentialBatchRequest(signedIssueCredentialOperation)
    )

    println(
        """
        Issuer: Credential issued to Holder, the transaction can take up to 10 minutes to be confirmed by the Cardano network
        - IssuerDID = $issuerDID
        - Operation identifier = ${issuedCredentialResponse.operationId}
        - Credential content = $holderUnsignedCredential
        - Signed credential = ${holderSignedCredential.canonicalForm}
        - Inclusion proof (encoded) = ${holderCredentialMerkleProofs.first().encode()}
        - Batch id = ${issuedCredentialResponse.batchId}
        """.trimIndent()
    )
    println("\nNext, let's resolve the issuer/credential details")
    pressEnterToContinue()
    println("\n*************** STEP 4")

    // Query the Node for the credential data
    println("Resolving issuer/credential details from the Node")
    val resolvedIssuerDIDDocument = node
        .GetDidDocument(GetDidDocumentRequest(did = issuerDID.value))
        .document!!

    val issuerIssuanceKey = resolvedIssuerDIDDocument.findPublicKey(issuanceKeyId)
    val resolvedCredentialBatchState = node.GetBatchState(
        GetBatchStateRequest(batchId = Hash.fromHex(issuedCredentialResponse.batchId).hexValue())
    )
    val resolvedCredentialBatchData = BatchData(
        issuedOn = resolvedCredentialBatchState.publicationLedgerData?.timestampInfo?.toTimestampInfoModel()!!,
        revokedOn = resolvedCredentialBatchState.revocationLedgerData?.timestampInfo?.toTimestampInfoModel()
    )
    val credentialRevocationTime = node.GetCredentialRevocationTime(
        GetCredentialRevocationTimeRequest(
            batchId = Hash.fromHex(issuedCredentialResponse.batchId).hexValue(),
            credentialHash = pbandk.ByteArr(holderUnsignedCredential.hash().value)
        )
    )
        .revocationLedgerData?.timestampInfo?.toTimestampInfoModel()
    println(
        """
        Credential data resolved:
        - Credential batch data: $resolvedCredentialBatchData
        - Credential revocation time: $credentialRevocationTime
        """.trimIndent()
    )

    // check the credential validity (which succeeds)
    println("\nNext, let's run the credential verification process")
    pressEnterToContinue()
    println("\n*************** STEP 5")
    println("Verifying credential")
    CredentialVerification.verify(
        keyData = issuerIssuanceKey!!,
        batchData = resolvedCredentialBatchData,
        credentialRevocationTime = credentialRevocationTime,
        merkleRoot = holderCredentialMerkleRoot,
        inclusionProof = holderCredentialMerkleProofs.first(),
        signedCredential = holderSignedCredential
    )
    println("Credential validity check passed")
}
