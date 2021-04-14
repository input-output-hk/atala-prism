package io.iohk.atala.prism.example

import io.iohk.atala.prism.kotlin.credentials.BatchData
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.credentials.CredentialVerification
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.Hash
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.util.toByteArray
import io.iohk.atala.prism.kotlin.extras.ProtoClientUtils
import io.iohk.atala.prism.kotlin.extras.ProtoUtils
import io.iohk.atala.prism.kotlin.extras.RequestUtils
import io.iohk.atala.prism.kotlin.extras.findPublicKey
import io.iohk.atala.prism.kotlin.extras.toTimestampInfoModel
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray

object CompleteFlowTutorial {

    @ExperimentalUnsignedTypes
    suspend fun run() {
        println(
            """
            Welcome to the complete flow example, which covers:
            - Interacting with the PRISM node to claim/verify DIDs, as well as to issuer/verify credentials.
            - Interacting with the PRISM connector to connect with other entities and share credentials with those.
            - An issuer who claims its DID, using it to issue a credential to a holder.
            - A holder who claims its unpublished DID, who connects to the issuer to receive a credential.
            - A holder who claims a second unpublished DID to connect to the verifier.
            - A verifier who claims its DID, using it to connect to the holder to receive a credential.
            - The verifier receiving a credential from the connector, gathering the necessary data about it from the node
              and then, run the credential verification.
            """.trimIndent()
        )
        println()

        println("Creating the clients for the connector/node, which are expected to be running in localhost")
        println()
        val connector = ProtoClientUtils.connectorClient("localhost", 50051)
        val node = ProtoClientUtils.nodeClient("localhost", 50053)

        // the issuer claims an identity
        println("Issuer: Generates and registers a DID")
        val issuerMasterKeyPair = EC.generateKeyPair()
        val issuerCreateDIDOperation = ProtoUtils.createDidAtalaOperation(issuerMasterKeyPair)
        val issuerCreatedDIDSignedOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issuerCreateDIDOperation)

        // the issuer registers its identity to the node
        // Usually the DID would be registered with the node, but, the connector can handle that as well
        // val issuerDIDSuffix = node.CreateDID(CreateDIDRequest(signedOperation)).id
        val issuerRegisterDIDResponse = connector.RegisterDID(
            RegisterDIDRequest(
                createDIDOperation = issuerCreatedDIDSignedOperation,
                name = "The Issuer"
            )
        )
        val issuerDID = DID.fromString(issuerRegisterDIDResponse.did)

        // the DID takes some minutes to get confirmed by Cardano, in the mean time, the unpublished DID
        // can be used to authenticate requests to the backend
        val issuerUnpublishedDID = DID.createUnpublishedDID(issuerMasterKeyPair.publicKey)

        println(
            """
            Issuer DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
            - DID: ${issuerRegisterDIDResponse.did}
            - Cardano transaction id: ${issuerRegisterDIDResponse.transactionInfo?.transactionId}
            """.trimIndent()
        )
        println()

        // the issuer generates a token to connect with the credential subject
        val issuerGenerateConnectionTokenRequest = GenerateConnectionTokenRequest(count = 1)
        val issuerConnectionToken = connector.GenerateConnectionTokenAuth(
            issuerGenerateConnectionTokenRequest,
            RequestUtils.generateRequestMetadata(
                issuerUnpublishedDID.value,
                issuerMasterKeyPair.privateKey,
                issuerGenerateConnectionTokenRequest
            )
        )
            .tokens.first()
        println("Issuer: Token for connecting with the holder generated = $issuerConnectionToken")

        // the holder generates its identity to connect with the issuer
        val holderMasterKeyPair = EC.generateKeyPair()
        val holderUnpublishedDID = DID.createUnpublishedDID(holderMasterKeyPair.publicKey)
        println("Holder: First DID generated to connect with the Issuer = $holderUnpublishedDID")

        // the holder generates its identity to connect with the verifier
        // in PRISM, you are supposed to use different identities for every connection
        // TODO: We'll need to allow accepting connections even if the acceptor's identity already exists
        val holderMasterKeyPair2 = EC.generateKeyPair()
        val holderUnpublishedDID2 = DID.createUnpublishedDID(holderMasterKeyPair2.publicKey)
        println("Holder: Second DID generated to connect with the Verifier = $holderUnpublishedDID2")
        println()

        // the subject verifies the connection token details to make sure its connecting to the right entity
        val issuerConnectionTokenDetails = connector.GetConnectionTokenInfo(
            GetConnectionTokenInfoRequest(token = issuerConnectionToken)
        )
        println(
            """
            Holder: Check Issuer's connection token details:
            - Issuer name = ${issuerConnectionTokenDetails.creatorName}
            - Issuer DID  = ${issuerConnectionTokenDetails.creatorDID}
            """.trimIndent()
        )

        // the subject accepts the connection token to connect to the issuer
        // TODO: remove the userId from the response, its totally unnecessary
        // TODO: Fix failing call
        val holderAcceptsIssuerConnectionRequest = AddConnectionFromTokenRequest(
            token = issuerConnectionToken,
            // TODO: Send the DID instead when the backend supports it
            holderEncodedPublicKey = EncodedPublicKey(
                publicKey = pbandk.ByteArr(holderMasterKeyPair.publicKey.getEncoded().toByteArray())
            )
        )
        val holderIssuerConnection = connector.AddConnectionFromTokenAuth(
            holderAcceptsIssuerConnectionRequest,
            RequestUtils.generateRequestMetadata(
                holderUnpublishedDID.value,
                holderMasterKeyPair.privateKey,
                holderAcceptsIssuerConnectionRequest
            )
        ).connection!!
        println("Holder (DID 1): Connected to Issuer, connectionId = ${holderIssuerConnection.connectionId}")
        println()

        // the issuer generates a credential to the subject
        val holderCredentialContent = CredentialContent(
            JsonObject(
                mapOf(
                    Pair("issuerDid", JsonPrimitive(issuerDID.value)),
                    Pair("issuanceKeyId", JsonPrimitive("master0")),
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

        val holderUnsignedCredential = JsonBasedCredential(holderCredentialContent)
        val holderSignedCredential = holderUnsignedCredential.sign(issuerMasterKeyPair.privateKey)
        val (holderCredentialMerkleRoot, holderCredentialMerkleProofs) = CredentialBatches.batch(listOf(holderSignedCredential))
        val credentialBatchData = CredentialBatchData(
            issuerDID = issuerDID.suffix.value, // This requires the suffix only, as the node stores only suffixes
            merkleRoot = pbandk.ByteArr(holderCredentialMerkleRoot.hash.value.toByteArray())
        )
        val issueCredentialOperation = ProtoUtils.issueCredentialBatchOperation(credentialBatchData)

        // the issuer publishes the credential to Cardano
        val signedIssueCredentialOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issueCredentialOperation)
        val issuedCredentialResponse = node.IssueCredentialBatch(IssueCredentialBatchRequest(signedIssueCredentialOperation))
        println(
            """
            Issuer: Credential issued to Holder, the transaction can take up to 10 minutes to be confirmed by the Cardano network
            - IssuerDID = $issuerDID
            - Cardano transaction id = ${issuedCredentialResponse.transactionInfo?.transactionId}
            - Credential content = $holderUnsignedCredential
            - Signed credential = ${holderSignedCredential.canonicalForm}
            - Inclusion proof (encoded) = ${holderCredentialMerkleProofs.first().encode()}
            - Batch id = ${issuedCredentialResponse.batchId}
            """.trimIndent()
        )

        // the issuer sends the credential to the subject through the connector
        val credentialFromIssuerMessage = AtalaMessage(
            AtalaMessage.Message.PlainCredential(
                PlainTextCredential(
                    encodedCredential = holderSignedCredential.canonicalForm,
                    encodedMerkleProof = holderCredentialMerkleProofs.first().encode()
                )
            )
        )

        // the connector allows any kind of message, this is just a way to send a credential but you can define your own
        val issuerSendMessageRequest = SendMessageRequest(
            holderIssuerConnection.connectionId,
            pbandk.ByteArr(credentialFromIssuerMessage.encodeToByteArray())
        )
        connector.SendMessageAuth(
            issuerSendMessageRequest,
            RequestUtils.generateRequestMetadata(issuerUnpublishedDID.value, issuerMasterKeyPair.privateKey, issuerSendMessageRequest)
        )
        println("Issuer: Credential sent to Holder")
        println()

        // the holder receives the credential from the issuer
        val holderGetMessagesRequest = GetMessagesPaginatedRequest(limit = 1)
        val holderReceivedMessage = connector.GetMessagesPaginatedAuth(
            holderGetMessagesRequest,
            RequestUtils.generateRequestMetadata(
                holderUnpublishedDID.value,
                holderMasterKeyPair.privateKey,
                holderGetMessagesRequest
            )
        ).messages.first()

        val holderReceivedCredential = AtalaMessage
            .decodeFromByteArray(holderReceivedMessage.message.array)
            .plainCredential!!
        println(
            """
            Holder: Message received
            - Canonical credential = ${holderReceivedCredential.encodedCredential}
            - Inclusion proof = ${holderReceivedCredential.encodedMerkleProof}
            """.trimIndent()
        )
        println()

        // the verifier claims an identity, similar to the previous example done with the issuer
        println("Verifier: Generates and registers a DID")
        val verifierMasterKeyPair = EC.generateKeyPair()
        val verifierCreateDIDOperation = ProtoUtils.createDidAtalaOperation(verifierMasterKeyPair)
        val verifierCreateDIDSignedOperation = ProtoUtils.signedAtalaOperation(verifierMasterKeyPair, verifierCreateDIDOperation)

        val verifierRegisterDIDResponse = connector.RegisterDID(
            RegisterDIDRequest(
                createDIDOperation = verifierCreateDIDSignedOperation,
                name = "The Verifier"
            )
        )
        val verifierDID = DID.fromString(verifierRegisterDIDResponse.did)
        val verifierUnpublishedDID = DID.createUnpublishedDID(verifierMasterKeyPair.publicKey)
        println(
            """
            Verifier DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
            - DID: $verifierDID
            - Cardano transaction id: ${verifierRegisterDIDResponse.transactionInfo?.transactionId}
            """.trimIndent()
        )
        println()

        // the verifier generates a token to connect with the credential subject
        val verifierGenerateConnectionTokenRequest = GenerateConnectionTokenRequest(count = 1)
        val verifierConnectionToken = connector.GenerateConnectionTokenAuth(
            verifierGenerateConnectionTokenRequest,
            RequestUtils.generateRequestMetadata(
                verifierUnpublishedDID.value,
                verifierMasterKeyPair.privateKey,
                verifierGenerateConnectionTokenRequest
            )
        )
            .tokens
            .first()
        println("Verifier: Token for connecting with the holder generated = $verifierConnectionToken")
        println()

        // the subject accepts the connection token to connect to the verifier
        val holderAcceptsVerifierConnectionRequest = AddConnectionFromTokenRequest(
            token = verifierConnectionToken,
            // TODO: Send the DID instead when the backend supports it
            holderEncodedPublicKey = EncodedPublicKey(
                publicKey = pbandk.ByteArr(holderMasterKeyPair2.publicKey.getEncoded().toByteArray())
            )
        )
        val holderVerifierConnection = connector.AddConnectionFromTokenAuth(
            holderAcceptsVerifierConnectionRequest,
            RequestUtils.generateRequestMetadata(
                holderUnpublishedDID2.value,
                holderMasterKeyPair2.privateKey,
                holderAcceptsVerifierConnectionRequest
            )
        )
            .connection!!
        println("Holder (DID 2): Connected to Verifier, connectionId = ${holderVerifierConnection.connectionId}")

        // the subject shares a credential with the verifier
        val credentialFromHolderMessage = AtalaMessage(
            AtalaMessage.Message.PlainCredential(
                PlainTextCredential(
                    encodedCredential = holderReceivedCredential.encodedCredential,
                    encodedMerkleProof = holderReceivedCredential.encodedMerkleProof
                )
            )
        )

        val holderSendMessageRequest = SendMessageRequest(
            holderVerifierConnection.connectionId,
            pbandk.ByteArr(credentialFromHolderMessage.encodeToByteArray())
        )
        connector.SendMessageAuth(
            holderSendMessageRequest,
            RequestUtils.generateRequestMetadata(
                holderUnpublishedDID2.value,
                holderMasterKeyPair2.privateKey,
                holderSendMessageRequest
            )
        )
        println("Holder (DID 2): Credential sent to Verifier")
        println()

        // the verifier receives the credential shared from the subject
        val verifierGetMessagesRequest = GetMessagesPaginatedRequest(limit = 1)
        val verifierReceivedMessage = connector.GetMessagesPaginatedAuth(
            verifierGetMessagesRequest,
            RequestUtils.generateRequestMetadata(
                verifierUnpublishedDID.value,
                verifierMasterKeyPair.privateKey,
                verifierGetMessagesRequest
            )
        )
            .messages.first()
        val verifierReceivedCredential = AtalaMessage
            .decodeFromByteArray(verifierReceivedMessage.message.array)
            .plainCredential!!
        println(
            """
            Verifier: Message received
            - Canonical credential = ${verifierReceivedCredential.encodedCredential}
            - Inclusion proof = ${verifierReceivedCredential.encodedMerkleProof}
            """.trimIndent()
        )
        println()

        // decode the received credential
        val verifierReceivedJsonCredential = JsonBasedCredential.fromString(verifierReceivedCredential.encodedCredential)
        val verifierReceivedCredentialIssuerDID = verifierReceivedJsonCredential.content.getString("issuerDid")!!
        val verifierReceivedCredentialIssuanceKeyId = verifierReceivedJsonCredential.content.getString("issuanceKeyId")!!
        println(
            """
            Verifier: Received credential decoded
            - Credential: ${verifierReceivedJsonCredential.content}
            - Issuer DID: $verifierReceivedCredentialIssuerDID
            - Issuer issuance key id: $verifierReceivedCredentialIssuanceKeyId
            """.trimIndent()
        )
        println()

        // the verifier queries the node for the credential data
        println("Verifier: Resolving issuer/credential details from the node")
        val verifierReceivedCredentialIssuerDIDDocument = node.GetDidDocument(GetDidDocumentRequest(did = verifierReceivedCredentialIssuerDID)).document!!
        val verifierReceivedCredentialIssuerKey = verifierReceivedCredentialIssuerDIDDocument.findPublicKey(verifierReceivedCredentialIssuanceKeyId)
        val verifierReceivedCredentialMerkleProof = MerkleInclusionProof.decode(verifierReceivedCredential.encodedMerkleProof)

        val verifierReceivedCredentialBatchId = CredentialBatches.computeCredentialBatchId(
            DID.fromString(verifierReceivedCredentialIssuerDID),
            verifierReceivedCredentialMerkleProof.derivedRoot()
        )

        val verifierReceivedCredentialBatchState = node.GetBatchState(GetBatchStateRequest(batchId = Hash.fromHex(verifierReceivedCredentialBatchId.id).hexValue()))
        val verifierReceivedCredentialBatchData = BatchData(
            issuedOn = verifierReceivedCredentialBatchState.publicationLedgerData?.timestampInfo?.toTimestampInfoModel()!!,
            revokedOn = verifierReceivedCredentialBatchState.revocationLedgerData?.timestampInfo?.toTimestampInfoModel()
        )
        val verifierReceivedCredentialRevocationTime = node.GetCredentialRevocationTime(
            GetCredentialRevocationTimeRequest(
                batchId = Hash.fromHex(verifierReceivedCredentialBatchId.id).hexValue(),
                credentialHash = pbandk.ByteArr(verifierReceivedJsonCredential.hash().value.toByteArray())
            )
        )
            .revocationLedgerData?.timestampInfo?.toTimestampInfoModel()

        // the verifier checks the credential validity (which succeeds)
        println("Verifier: Verifying received credential")
        CredentialVerification.verify(
            keyData = verifierReceivedCredentialIssuerKey!!,
            batchData = verifierReceivedCredentialBatchData,
            credentialRevocationTime = verifierReceivedCredentialRevocationTime,
            merkleRoot = verifierReceivedCredentialMerkleProof.derivedRoot(), // TODO: We may want to receive this instead of computing it
            inclusionProof = verifierReceivedCredentialMerkleProof,
            signedCredential = verifierReceivedJsonCredential
        )

        // the issuer revokes the credential
        val issuerRevokeCredentialOperation = ProtoUtils.revokeCredentialsOperation(
            batchOperationHash = Hash.compute(issueCredentialOperation.encodeToByteArray().asList()),
            batchId = CredentialBatchId.fromString(issuedCredentialResponse.batchId)!!,
            credentials = listOf(holderSignedCredential)
        )
        val issuerRevokeCredentialSignedOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issuerRevokeCredentialOperation)
        val issuerCredentialRevocationResponse = node.RevokeCredentials(
            RevokeCredentialsRequest(issuerRevokeCredentialSignedOperation)
        )
        println(
            """
            Issuer: Credential revoked, the transaction can take up to 10 minutes to be confirmed by the Cardano network
            - Cardano transaction id: ${issuerCredentialRevocationResponse.transactionInfo?.transactionId}
            """.trimIndent()
        )
        println()

        // the verifier resolves the credential revocation time from the node
        println("Verifier: Checking the credential validity again, expect an error explaining that the credential is revoked")
        Thread.sleep(2000) // give some time to the backend to apply the operation
        val verifierReceivedCredentialRevocationTime2 = node.GetCredentialRevocationTime(
            GetCredentialRevocationTimeRequest(
                batchId = Hash.fromHex(verifierReceivedCredentialBatchId.id).hexValue(),
                credentialHash = pbandk.ByteArr(verifierReceivedJsonCredential.hash().value.toByteArray())
            )
        )
            .revocationLedgerData?.timestampInfo?.toTimestampInfoModel()

        // the verifier checks the credential validity (which fails)
        CredentialVerification.verify(
            keyData = verifierReceivedCredentialIssuerKey,
            batchData = verifierReceivedCredentialBatchData,
            credentialRevocationTime = verifierReceivedCredentialRevocationTime2,
            merkleRoot = verifierReceivedCredentialMerkleProof.derivedRoot(),
            inclusionProof = verifierReceivedCredentialMerkleProof,
            signedCredential = verifierReceivedJsonCredential
        )
    }
}
