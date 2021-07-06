package io.iohk.atala.prism.management.console.services

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.models.{GenericCredential, InstitutionGroup, PaginatedQueryConstraints}
import io.iohk.atala.prism.management.console.DataPreparation.{
  createContact,
  createGenericCredential,
  createInstitutionGroup,
  createParticipant,
  getBatchData,
  publishBatch,
  publishCredential
}
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.protos.connector_api.SendMessagesResponse
import io.iohk.atala.prism.protos.connector_models.MessageToSendByConnectionToken
import io.iohk.atala.prism.protos.{common_models, connector_api, connector_models, console_api, node_api, node_models}
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.Mockito.verify
import org.scalatest.OptionValues.convertOptionToValuable
import org.mockito.ArgumentMatchersSugar.*

import java.util.UUID
import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.management.console.models.GenericCredential.SortBy
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus

import scala.concurrent.Future

class CredentialsServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDUtil {

  val keyPair = EC.generateKeyPair()
  val did = generateDid(keyPair.publicKey)

  "CredentialsServiceImpl.getGenericCredentials" should {

    "retrieve the revocation data when a credential is revoked" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val connectionToken = "connectionToken"
      val contact = createContact(issuerId, "Contact 1", None, connectionToken = connectionToken)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))
      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))
      publishCredential(
        issuerId,
        mockCredentialBatchId,
        originalCredential.credentialId,
        mockEncodedSignedCredential,
        mockMerkleProof
      )

      val mockRevocationOperationId = AtalaOperationId.random()
      credentialsRepository
        .storeRevocationData(issuerId, originalCredential.credentialId, mockRevocationOperationId)
        .unsafeRunSync()

      val contactConnection = connector_models.ContactConnection(
        connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
        connectionToken = connectionToken
      )
      connectorMock.getConnectionStatus(*).returns {
        Future.successful(
          List(contactConnection)
        )
      }

      val results = credentialsIntegrationService
        .getGenericCredentials(
          issuerId,
          new GenericCredential.PaginatedQuery(ordering = PaginatedQueryConstraints.ResultOrdering(SortBy.CreatedOn))
        )
        .futureValue
        .toOption
        .value

      results.data.head.genericCredential.revokedOnOperationId.value must be(mockRevocationOperationId)
    }

    "retrieve Credential with correct connection status given connection is present" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val connectionToken = "connectionToken"
      val connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED
      val contact = createContact(issuerId, "Contact 1", None, connectionToken = connectionToken)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))
      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))
      publishCredential(
        issuerId,
        mockCredentialBatchId,
        originalCredential.credentialId,
        mockEncodedSignedCredential,
        mockMerkleProof
      )

      val request = console_api.GetGenericCredentialsRequest().withLimit(10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val contactConnection = connector_models.ContactConnection(
        connectionStatus = connectionStatus,
        connectionToken = connectionToken
      )

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            List(contactConnection)
          )
        }
        val response = serviceStub.getGenericCredentials(request)
        response.credentials.head.connectionStatus must be(connectionStatus)
      }
    }

    "retrieve Credential with missing connection status given connector returned empty list of connections" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val connectionToken = "connectionToken"
      val contact = createContact(issuerId, "Contact 1", None, connectionToken = connectionToken)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))
      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))
      publishCredential(
        issuerId,
        mockCredentialBatchId,
        originalCredential.credentialId,
        mockEncodedSignedCredential,
        mockMerkleProof
      )

      val request = console_api.GetGenericCredentialsRequest().withLimit(10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val missingConnection = connector_models.ContactConnection(
        connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING,
        connectionToken = connectionToken
      )

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            List()
          )
        }
        val response = serviceStub.getGenericCredentials(request)
        response.credentials.head.connectionStatus must be(missingConnection.connectionStatus)
      }
    }
  }

  "CredentialsServiceImpl.revokePublishedCredential" should {
    def withPublishedCredential[T](f: (GenericCredential, ECKeyPair, DID) => T) = {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did = did)
      val contact = createContact(issuerId, "Contact 1", None)
      val originalCredential = createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))
      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))
      publishCredential(
        issuerId,
        mockCredentialBatchId,
        originalCredential.credentialId,
        mockEncodedSignedCredential,
        mockMerkleProof
      )
      f(originalCredential, keyPair, did)
    }

    "return the generated transaction id" in {
      withPublishedCredential { (credential, keyPair, did) =>
        val mockHash = SHA256Digest.compute("".getBytes())
        val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

        val revokeCredentialOp = node_models.SignedAtalaOperation(
          signedWith = "mockKey",
          signature = ByteString.copyFrom("".getBytes()),
          operation = Some(
            node_models.AtalaOperation(
              operation = node_models.AtalaOperation.Operation.RevokeCredentials(
                node_models
                  .RevokeCredentialsOperation()
                  .withCredentialBatchId(mockCredentialBatchId.id)
                  .withPreviousOperationHash(ByteString.copyFrom(mockHash.value.toArray))
                  .withCredentialsToRevoke(List(ByteString.copyFrom(mockHash.value.toArray)))
              )
            )
          )
        )

        val mockRevocationOperationId = AtalaOperationId.of(revokeCredentialOp)

        val nodeRequest = node_api
          .RevokeCredentialsRequest()
          .withSignedOperation(revokeCredentialOp)

        nodeMock
          .revokeCredentials(nodeRequest)
          .returns(
            Future
              .successful(
                node_api
                  .RevokeCredentialsResponse()
                  .withOperationId(mockRevocationOperationId.toProtoByteString)
              )
          )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          serviceStub.revokePublishedCredential(request)

          val revokedOnOperationId = credentialsRepository
            .getBy(credential.credentialId)
            .unsafeRunSync()
            .value
            .revokedOnOperationId

          revokedOnOperationId.value must be(mockRevocationOperationId)
        }
      }
    }

    "fail when the credentialId is missing" in {
      withPublishedCredential { (_, keyPair, did) =>
        val mockHash = SHA256Digest.compute("".getBytes())
        val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

        val revokeCredentialOp = node_models.SignedAtalaOperation(
          signedWith = "mockKey",
          signature = ByteString.copyFrom("".getBytes()),
          operation = Some(
            node_models.AtalaOperation(
              operation = node_models.AtalaOperation.Operation.RevokeCredentials(
                node_models
                  .RevokeCredentialsOperation()
                  .withCredentialBatchId(mockCredentialBatchId.id)
                  .withPreviousOperationHash(ByteString.copyFrom(mockHash.value.toArray))
                  .withCredentialsToRevoke(List(ByteString.copyFrom(mockHash.value.toArray)))
              )
            )
          )
        )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }

    "fail when the revokeCredentialsOperation is missing" in {
      withPublishedCredential { (credential, keyPair, did) =>
        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }

    "fail when the operation is not a revokeCredentials operation" in {
      withPublishedCredential { (credential, keyPair, did) =>
        val revokeCredentialOp = node_models.SignedAtalaOperation(
          signedWith = "mockKey",
          signature = ByteString.copyFrom("".getBytes()),
          operation = Some(node_models.AtalaOperation())
        )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }

    "fail when the whole batch is being revoked" in {
      withPublishedCredential { (credential, keyPair, did) =>
        val mockHash = SHA256Digest.compute("".getBytes())
        val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

        val revokeCredentialOp = node_models.SignedAtalaOperation(
          signedWith = "mockKey",
          signature = ByteString.copyFrom("".getBytes()),
          operation = Some(
            node_models.AtalaOperation(
              operation = node_models.AtalaOperation.Operation.RevokeCredentials(
                node_models
                  .RevokeCredentialsOperation()
                  .withCredentialBatchId(mockCredentialBatchId.id)
                  .withPreviousOperationHash(ByteString.copyFrom(mockHash.value.toArray))
              )
            )
          )
        )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }

    "fail when the more than 1 credentials are being revoked" in {
      withPublishedCredential { (credential, keyPair, did) =>
        val mockHash = SHA256Digest.compute("".getBytes())
        val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

        val revokeCredentialOp = node_models.SignedAtalaOperation(
          signedWith = "mockKey",
          signature = ByteString.copyFrom("".getBytes()),
          operation = Some(
            node_models.AtalaOperation(
              operation = node_models.AtalaOperation.Operation.RevokeCredentials(
                node_models
                  .RevokeCredentialsOperation()
                  .withCredentialBatchId(mockCredentialBatchId.id)
                  .withPreviousOperationHash(ByteString.copyFrom(mockHash.value.toArray))
                  .withCredentialsToRevoke(
                    List(
                      ByteString.copyFrom(mockHash.value.toArray),
                      ByteString.copyFrom(mockHash.value.toArray)
                    )
                  )
              )
            )
          )
        )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }

    "fail when the credentialId doesn't belong to the authenticated institution" in {
      withPublishedCredential { (credential, _, _) =>
        val mockHash = SHA256Digest.compute("".getBytes())
        val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

        val revokeCredentialOp = node_models.SignedAtalaOperation(
          signedWith = "mockKey",
          signature = ByteString.copyFrom("".getBytes()),
          operation = Some(
            node_models.AtalaOperation(
              operation = node_models.AtalaOperation.Operation.RevokeCredentials(
                node_models
                  .RevokeCredentialsOperation()
                  .withCredentialBatchId(mockCredentialBatchId.id)
                  .withPreviousOperationHash(ByteString.copyFrom(mockHash.value.toArray))
                  .withCredentialsToRevoke(List(ByteString.copyFrom(mockHash.value.toArray)))
              )
            )
          )
        )

        val mockRevocationOperationId = AtalaOperationId.of(revokeCredentialOp)

        val nodeRequest = node_api
          .RevokeCredentialsRequest()
          .withSignedOperation(revokeCredentialOp)

        nodeMock
          .revokeCredentials(nodeRequest)
          .returns(
            Future
              .successful(
                node_api
                  .RevokeCredentialsResponse()
                  .withOperationId(mockRevocationOperationId.toProtoByteString)
              )
          )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val keyPair = EC.generateKeyPair()
        val publicKey = keyPair.publicKey
        val did = generateDid(publicKey)
        createParticipant("malicious issuer", did = did)
        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }
  }

  "CredentialsServiceImpl.getLedgerData" should {
    val aHash = SHA256Digest.compute("random".getBytes())
    val aCredentialHash = ByteString.copyFrom(aHash.value.toArray)
    val illegalCredentialHash = ByteString.copyFrom(aHash.value.drop(1).toArray)
    val aBatchId = CredentialBatchId.random().id

    "fail when queried with invalid batchId" in {
      val illegalBatchId = "@@@#!?"
      createParticipant("Institution", did)
      val request = console_api.GetLedgerDataRequest(illegalBatchId, aCredentialHash)
      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.getLedgerData(request)
        )
      }
    }

    "fail when queried with invalid credentialHash" in {
      createParticipant("Institution", did)
      val request = console_api.GetLedgerDataRequest(aBatchId, illegalCredentialHash)
      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.getLedgerData(request)
        )
      }
    }

    val batchIssuanceLedgerData =
      node_models.LedgerData(
        transactionId = "tx id 1",
        ledger = common_models.Ledger.IN_MEMORY,
        timestampInfo = Some(node_models.TimestampInfo(1, 1, Some(new Timestamp(1, 0))))
      )

    val batchRevocationLedgerData =
      node_models.LedgerData(
        transactionId = "tx id 2",
        ledger = common_models.Ledger.CARDANO_MAINNET,
        timestampInfo = Some(node_models.TimestampInfo(2, 2, Some(new Timestamp(2, 0))))
      )

    val credentialRevocationLedgerData =
      node_models.LedgerData(
        transactionId = "tx id 3",
        ledger = common_models.Ledger.CARDANO_TESTNET,
        timestampInfo = Some(node_models.TimestampInfo(3, 3, Some(new Timestamp(3, 0))))
      )

    def nodeReturns(
        batchIssuanceLedgerData: Option[node_models.LedgerData],
        batchRevocationLedgerData: Option[node_models.LedgerData],
        credentialRevocationLedgerData: Option[node_models.LedgerData]
    ): Unit = {
      val aMerkleRoot = ByteString.copyFrom(SHA256Digest.compute("root".getBytes()).value.toArray)
      nodeMock
        .getBatchState(node_api.GetBatchStateRequest(aBatchId))
        .returns(
          Future.successful(
            node_api
              .GetBatchStateResponse("did:prism:aDID", aMerkleRoot, batchIssuanceLedgerData, batchRevocationLedgerData)
          )
        )
      nodeMock
        .getCredentialRevocationTime(node_api.GetCredentialRevocationTimeRequest(aBatchId, aCredentialHash))
        .returns(
          Future.successful(node_api.GetCredentialRevocationTimeResponse(credentialRevocationLedgerData))
        )
      ()
    }

    "return batch issuance data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(Some(batchIssuanceLedgerData), None, None)

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (Some(batchIssuanceLedgerData))
        response.batchRevocation mustBe (empty)
        response.credentialRevocation mustBe (empty)
      }
    }

    "return batch revocation data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(None, Some(batchRevocationLedgerData), None)

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (empty)
        response.batchRevocation mustBe (Some(batchRevocationLedgerData))
        response.credentialRevocation mustBe (empty)
      }
    }

    "return credential revocation data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(None, None, Some(credentialRevocationLedgerData))

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (empty)
        response.batchRevocation mustBe (empty)
        response.credentialRevocation mustBe (Some(credentialRevocationLedgerData))
      }
    }

    "return batch issuance data and credential revocation data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(Some(batchIssuanceLedgerData), None, Some(credentialRevocationLedgerData))

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (Some(batchIssuanceLedgerData))
        response.batchRevocation mustBe (empty)
        response.credentialRevocation mustBe (Some(credentialRevocationLedgerData))
      }
    }
  }

  "CredentialsServiceImpl.publishBatch" should {
    def buildSignedIssueCredentialOp(
        credentialHash: SHA256Digest,
        didSuffix: DIDSuffix
    ): node_models.SignedAtalaOperation = {
      node_models.SignedAtalaOperation(
        signedWith = "mockKey",
        signature = ByteString.copyFrom("".getBytes()),
        operation = Some(
          node_models.AtalaOperation(
            operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
              node_models.IssueCredentialBatchOperation(
                credentialBatchData = Some(
                  node_models.CredentialBatchData(
                    issuerDid = didSuffix.value,
                    merkleRoot = ByteString.copyFrom(credentialHash.value.toArray)
                  )
                )
              )
            )
          )
        )
      )
    }

    "forward request to node and store data in database" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant(issuerName, did)

      val mockDIDSuffix = DID.buildPrismDID(SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue).suffix
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockCredentialBatchId =
        CredentialBatchId.fromBatchData(mockDIDSuffix, MerkleRoot(mockEncodedSignedCredentialHash))
      val mockOperationId = AtalaOperationId.of(issuanceOp)

      val nodeRequest = node_api
        .IssueCredentialBatchRequest()
        .withSignedOperation(issuanceOp)

      nodeMock
        .issueCredentialBatch(nodeRequest)
        .returns(
          Future
            .successful(
              node_api
                .IssueCredentialBatchResponse()
                .withOperationId(mockOperationId.toProtoByteString)
                .withBatchId(mockCredentialBatchId.id)
            )
        )

      val request = console_api
        .PublishBatchRequest()
        .withIssueCredentialBatchOperation(issuanceOp)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        val publishResponse = serviceStub.publishBatch(request)
        verify(nodeMock).issueCredentialBatch(nodeRequest)

        publishResponse.batchId mustBe mockCredentialBatchId.id
        publishResponse.operationId mustBe mockOperationId.toProtoByteString

        val (operationId, hash) = getBatchData(mockCredentialBatchId).value
        hash mustBe SHA256Digest.compute(issuanceOp.operation.value.toByteArray)
        operationId mustBe mockOperationId
      }
    }

  }

  "CredentialsServiceImpl.shareCredentials" should {
    connectorMock.sendMessages(*, *).returns {
      Future.successful(SendMessagesResponse())
    }

    "send credentials to connector and mark them as shared" in {
      val (credential1, credential2) = prepareCredentials()

      val request = console_api.ShareCredentialsRequest(
        List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString),
        sendMessagesRequest =
          Some(connector_api.SendMessagesRequest(1.to(2).map(_ => MessageToSendByConnectionToken()))),
        sendMessagesRequestMetadata = Some(DataPreparation.connectorRequestMetadataProto)
      )

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        serviceStub.shareCredentials(request)

        val result1 = credentialsRepository.getBy(credential1.credentialId).unsafeRunSync().value
        result1.sharedAt mustNot be(empty)

        val result2 = credentialsRepository.getBy(credential2.credentialId).unsafeRunSync().value
        result2.sharedAt mustNot be(empty)
      }
    }

    "return error when credential ids are not valid UUIDs" in {
      assertShareCredentialsError {
        case (credential1, _) =>
          console_api.ShareCredentialsRequest(
            List(credential1.credentialId.uuid.toString, "invalidUuid"),
            sendMessagesRequest =
              Some(connector_api.SendMessagesRequest(1.to(2).map(_ => MessageToSendByConnectionToken()))),
            sendMessagesRequestMetadata = Some(DataPreparation.connectorRequestMetadataProto)
          )
      }
    }

    "return error when credential ids don't exist" in {
      assertShareCredentialsError {
        case (credential1, _) =>
          console_api.ShareCredentialsRequest(
            List(credential1.credentialId.uuid.toString, UUID.randomUUID().toString),
            sendMessagesRequest =
              Some(connector_api.SendMessagesRequest(1.to(2).map(_ => MessageToSendByConnectionToken()))),
            sendMessagesRequestMetadata = Some(DataPreparation.connectorRequestMetadataProto)
          )
      }
    }

    "return error when connector request metadata is empty" in {
      assertShareCredentialsError {
        case (credential1, credential2) =>
          console_api.ShareCredentialsRequest(
            List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString),
            sendMessagesRequest =
              Some(connector_api.SendMessagesRequest(1.to(2).map(_ => MessageToSendByConnectionToken()))),
            sendMessagesRequestMetadata = None
          )
      }
    }

    "return error when send message request is empty" in {
      assertShareCredentialsError {
        case (credential1, credential2) =>
          console_api.ShareCredentialsRequest(
            List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString),
            sendMessagesRequest = None,
            sendMessagesRequestMetadata = Some(DataPreparation.connectorRequestMetadataProto)
          )
      }
    }

    "return error when number of credential ids doesn't match number of messages to send" in {
      assertShareCredentialsError {
        case (credential1, credential2) =>
          console_api.ShareCredentialsRequest(
            List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString),
            sendMessagesRequest =
              Some(connector_api.SendMessagesRequest(1.to(3).map(_ => MessageToSendByConnectionToken()))),
            sendMessagesRequestMetadata = Some(DataPreparation.connectorRequestMetadataProto)
          )
      }
    }

    def assertShareCredentialsError(
        f: (GenericCredential, GenericCredential) => console_api.ShareCredentialsRequest
    ) = {
      val (credential1, credential2) = prepareCredentials()

      val request = f(credential1, credential2)

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        intercept[StatusRuntimeException] {
          serviceStub.shareCredentials(request)
        }

        val result1 = credentialsRepository.getBy(credential1.credentialId).unsafeRunSync().value
        result1.sharedAt must be(empty)

        val result2 = credentialsRepository.getBy(credential2.credentialId).unsafeRunSync().value
        result2.sharedAt must be(empty)
      }
    }
  }

  private def prepareCredentials(): (GenericCredential, GenericCredential) = {
    val issuerId = createParticipant("Issuer X", did)
    val contact = createContact(issuerId, "IOHK Student", None)

    val credential1 = createGenericCredential(issuerId, contact.contactId, "A")
    val credential2 = createGenericCredential(issuerId, contact.contactId, "B")

    publishCredential(issuerId, credential1)
    publishCredential(issuerId, credential2)

    (credential1, credential2)
  }

  "CredentialsServiceImpl.storePublishedCredential" should {
    "store a credential when the batch was published" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val issuerGroup = createInstitutionGroup(issuerId, InstitutionGroup.Name("group 1"))
      val contact = createContact(issuerId, "Contact 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val atalaOperationId = AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value)
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, atalaOperationId)

      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      val request = console_api
        .StorePublishedCredentialRequest()
        .withConsoleCredentialId(originalCredential.credentialId.uuid.toString)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withEncodedInclusionProof(mockMerkleProof.encode)
        .withBatchId(mockCredentialBatchId.id)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        serviceStub.storePublishedCredential(request)

        val storedPublicationData = credentialsRepository
          .getBy(originalCredential.credentialId)
          .unsafeRunSync()
          .value
          .publicationData
          .value

        storedPublicationData.credentialBatchId mustBe mockCredentialBatchId
        storedPublicationData.issuanceOperationHash mustBe issuanceOpHash
        storedPublicationData.encodedSignedCredential mustBe mockEncodedSignedCredential
        storedPublicationData.atalaOperationId mustBe atalaOperationId
      }
    }

    "fail if issuer is trying to publish an empty encoded signed credential" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val issuerGroup = createInstitutionGroup(issuerId, InstitutionGroup.Name("group 1"))
      val contact = createContact(issuerId, "Contact 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, contact.contactId)

      val mockEmptyEncodedSignedCredential = ""

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))

      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      val request = console_api
        .StorePublishedCredentialRequest()
        .withConsoleCredentialId(originalCredential.credentialId.uuid.toString)
        .withEncodedSignedCredential(mockEmptyEncodedSignedCredential)
        .withEncodedInclusionProof(mockMerkleProof.encode)
        .withBatchId(mockCredentialBatchId.id)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.storePublishedCredential(request)
        )

        err.getMessage.endsWith("Empty encoded credential") mustBe true

        val storedPublicationData = credentialsRepository
          .getBy(originalCredential.credentialId)
          .unsafeRunSync()
          .value
          .publicationData

        storedPublicationData mustBe empty
      }
    }

    "fail if issuer is trying to publish a credential that does not exist in the db" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant(issuerName, did)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))

      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      val aRandomId = GenericCredential.Id.random()
      val request = console_api
        .StorePublishedCredentialRequest()
        .withConsoleCredentialId(aRandomId.uuid.toString)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withEncodedInclusionProof(mockMerkleProof.encode)
        .withBatchId(mockCredentialBatchId.id)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.storePublishedCredential(request)
        )

        val storedCredential = credentialsRepository
          .getBy(aRandomId)
          .unsafeRunSync()

        storedCredential mustBe empty
      }
    }

    "fail if issuer is trying to publish a credential he didn't create" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did = did)
      val issuerGroup = createInstitutionGroup(issuerId, InstitutionGroup.Name("Group 1"))
      val contact = createContact(issuerId, "Contact 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, AtalaOperationId.fromVectorUnsafe(issuanceOpHash.value))

      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      // another issuer's data
      val issuerName2 = "Issuer 2"
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
      val did2 = generateDid(publicKey2)
      createParticipant(issuerName2, did = did2)

      val request = console_api
        .StorePublishedCredentialRequest()
        .withConsoleCredentialId(originalCredential.credentialId.uuid.toString)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withEncodedInclusionProof(mockMerkleProof.encode)
        .withBatchId(mockCredentialBatchId.id)

      val rpcRequestIssuer2 = SignedRpcRequest.generate(keyPair2, did2, request)

      usingApiAsCredentials(rpcRequestIssuer2) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.storePublishedCredential(request)
        )

        val storedPublicationData = credentialsRepository
          .getBy(originalCredential.credentialId)
          .unsafeRunSync()
          .value
          .publicationData

        storedPublicationData mustBe empty
      }
    }

    "fail if the associated batch was not stored yet" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did = did)
      val issuerGroup = createInstitutionGroup(issuerId, InstitutionGroup.Name("Group 1"))
      val contact = createContact(issuerId, "Contact 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      val request = console_api
        .StorePublishedCredentialRequest()
        .withConsoleCredentialId(originalCredential.credentialId.uuid.toString)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withEncodedInclusionProof(mockMerkleProof.encode)
        .withBatchId(mockCredentialBatchId.id)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.storePublishedCredential(request)
        )

        val storedPublicationData = credentialsRepository
          .getBy(originalCredential.credentialId)
          .unsafeRunSync()
          .value
          .publicationData

        storedPublicationData mustBe empty
      }
    }
  }

  "deleteCredentials" should {
    "delete draft and published, revoked credentials" in {
      val issuerId = createParticipant("Issuer X", did)
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        serviceStub.deleteCredentials(request)
        credentialsRepository.getBy(credential1.credentialId).unsafeRunSync() mustBe None
        credentialsRepository.getBy(credential2.credentialId).unsafeRunSync() mustBe None
      }

    }

    "do not delete credentials when one of them is published and not revoked" in {
      val issuerId = createParticipant("Issuer X", did)
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      publishCredential(issuerId, credential1)

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.deleteCredentials(request)
        )

        credentialsRepository.getBy(credential1.credentialId).unsafeRunSync() mustBe a[Some[_]]
        credentialsRepository.getBy(credential2.credentialId).unsafeRunSync() mustBe a[Some[_]]
      }
    }

    "do not delete credentials when invalid uuid is supplied as credential id" in {
      val issuerId = createParticipant("Issuer X", did)
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, "invalidUUIdzzzz")
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.deleteCredentials(request)
        )

        credentialsRepository.getBy(credential1.credentialId).unsafeRunSync() mustBe a[Some[_]]
        credentialsRepository.getBy(credential2.credentialId).unsafeRunSync() mustBe a[Some[_]]
      }
    }

    "do not delete credentials when one of them has incorrect id" in {
      val issuerId = createParticipant("Issuer X", did)
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, UUID.randomUUID().toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.deleteCredentials(request)
        )

        credentialsRepository.getBy(credential1.credentialId).unsafeRunSync() mustBe a[Some[_]]
        credentialsRepository.getBy(credential2.credentialId).unsafeRunSync() mustBe a[Some[_]]
      }
    }
  }

}
