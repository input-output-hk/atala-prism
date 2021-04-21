package io.iohk.atala.prism.management.console.services

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.models.InstitutionGroup
import io.iohk.atala.prism.models.Ledger.InMemory
import io.iohk.atala.prism.management.console.DataPreparation.{
  createContact,
  createGenericCredential,
  createInstitutionGroup,
  createParticipant,
  getBatchData,
  publishBatch,
  publishCredential
}
import io.iohk.atala.prism.management.console.models.GenericCredential
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.protos.connector_api.SendMessagesResponse
import io.iohk.atala.prism.protos.connector_models.MessageToSendByConnectionToken
import io.iohk.atala.prism.models.{TransactionId, TransactionInfo}
import io.iohk.atala.prism.protos.{common_models, connector_api, console_api, node_api, node_models}
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.Mockito.verify
import org.scalatest.OptionValues.convertOptionToValuable
import org.mockito.ArgumentMatchersSugar.*
import java.util.UUID

import com.google.protobuf.timestamp.Timestamp

import scala.concurrent.Future

class CredentialsServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {

  val keyPair = EC.generateKeyPair()
  val did = generateDid(keyPair.publicKey)

  "CredentialsServiceImpl.getGenericCredentials" should {

    "retrieve the revocation data when a credential is revoked" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val contact = createContact(issuerId, "Subject 1", None)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      val mockTransactionInfo = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
        ledger = InMemory
      )

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, mockTransactionInfo)
      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))
      publishCredential(
        issuerId,
        mockCredentialBatchId,
        originalCredential.credentialId,
        mockEncodedSignedCredential,
        mockMerkleProof
      )

      val mockRevocationTransactionId = TransactionId.from(SHA256Digest.compute("revocation".getBytes).value).value
      credentialsRepository
        .storeRevocationData(issuerId, originalCredential.credentialId, mockRevocationTransactionId)
        .value
        .futureValue
        .toOption
        .value

      val request = console_api.GetGenericCredentialsRequest().withLimit(10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        val response = serviceStub.getGenericCredentials(request)
        val revocationProof = response.credentials.head.revocationProof.value
        revocationProof.transactionId must be(mockRevocationTransactionId.toString)
        revocationProof.ledger.isInMemory must be(true)
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
      val contact = createContact(issuerId, "Subject 1", None)
      val originalCredential = createGenericCredential(issuerId, contact.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      val mockTransactionInfo = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
        ledger = InMemory
      )

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, mockTransactionInfo)
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

        val mockRevocationTransactionInfo = common_models
          .TransactionInfo()
          .withTransactionId(CredentialBatchId.fromDigest(SHA256Digest.compute("revocationRandomHash".getBytes())).id)
          .withLedger(common_models.Ledger.IN_MEMORY)

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
                  .withTransactionInfo(mockRevocationTransactionInfo)
              )
          )

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAsCredentials(rpcRequest) { serviceStub =>
          serviceStub.revokePublishedCredential(request)

          val revokedOnTransactionId = credentialsRepository
            .getBy(credential.credentialId)
            .value
            .futureValue
            .toOption
            .value
            .value
            .revokedOnTransactionId

          revokedOnTransactionId.value.toString must be(mockRevocationTransactionInfo.transactionId)
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

        val mockRevocationTransactionInfo = common_models
          .TransactionInfo()
          .withTransactionId(CredentialBatchId.fromDigest(SHA256Digest.compute("revocationRandomHash".getBytes())).id)
          .withLedger(common_models.Ledger.IN_MEMORY)

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
                  .withTransactionInfo(mockRevocationTransactionInfo)
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
                    issuerDID = didSuffix.value,
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
      val mockTransactionInfo =
        common_models
          .TransactionInfo()
          .withTransactionId(mockCredentialBatchId.id)
          .withLedger(common_models.Ledger.IN_MEMORY)

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
                .withTransactionInfo(mockTransactionInfo)
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
        publishResponse.transactionInfo mustBe Some(mockTransactionInfo)

        val (txId, ledger, hash) = getBatchData(mockCredentialBatchId).value
        txId.toString mustBe mockTransactionInfo.transactionId
        ledger mustBe (InMemory)
        hash mustBe SHA256Digest.compute(issuanceOp.operation.value.toByteArray)
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

        val result1 = credentialsRepository.getBy(credential1.credentialId).value.futureValue.toOption.value.value
        result1.sharedAt mustNot be(empty)

        val result2 = credentialsRepository.getBy(credential2.credentialId).value.futureValue.toOption.value.value
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

        val result1 = credentialsRepository.getBy(credential1.credentialId).value.futureValue.toOption.value.value
        result1.sharedAt must be(empty)

        val result2 = credentialsRepository.getBy(credential2.credentialId).value.futureValue.toOption.value.value
        result2.sharedAt must be(empty)
      }
    }
  }

  private def prepareCredentials(): (GenericCredential, GenericCredential) = {
    val issuerId = createParticipant("Issuer X", did)
    val subject = createContact(issuerId, "IOHK Student", None)

    val credential1 = createGenericCredential(issuerId, subject.contactId, "A")
    val credential2 = createGenericCredential(issuerId, subject.contactId, "B")

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
      val subject = createContact(issuerId, "Subject 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, subject.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      val mockTransactionInfo = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
        ledger = InMemory
      )

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, mockTransactionInfo)

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
          .value
          .futureValue
          .toOption
          .value
          .value
          .publicationData
          .value

        storedPublicationData.credentialBatchId mustBe mockCredentialBatchId
        storedPublicationData.issuanceOperationHash mustBe issuanceOpHash
        storedPublicationData.encodedSignedCredential mustBe mockEncodedSignedCredential
        storedPublicationData.transactionId mustBe mockTransactionInfo.transactionId
        storedPublicationData.ledger mustBe mockTransactionInfo.ledger
      }
    }

    "fail if issuer is trying to publish an empty encoded signed credential" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(issuerName, did)
      val issuerGroup = createInstitutionGroup(issuerId, InstitutionGroup.Name("group 1"))
      val subject = createContact(issuerId, "Subject 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, subject.contactId)

      val mockEmptyEncodedSignedCredential = ""

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      val mockTransactionInfo = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
        ledger = InMemory
      )

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, mockTransactionInfo)

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
          .value
          .futureValue
          .toOption
          .value
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

      val mockTransactionInfo = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
        ledger = InMemory
      )

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, mockTransactionInfo)

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
          .value
          .futureValue
          .toOption
          .value

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
      val subject = createContact(issuerId, "Subject 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, subject.contactId)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOpHash = SHA256Digest.compute("opHash".getBytes())
      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))

      val mockTransactionInfo = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
        ledger = InMemory
      )

      // we need to first store the batch data in the db
      publishBatch(mockCredentialBatchId, issuanceOpHash, mockTransactionInfo)

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
          .value
          .futureValue
          .toOption
          .value
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
      val subject = createContact(issuerId, "Subject 1", Some(issuerGroup.name))
      val originalCredential = createGenericCredential(issuerId, subject.contactId)

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
          .value
          .futureValue
          .toOption
          .value
          .value
          .publicationData

        storedPublicationData mustBe empty
      }
    }
  }

  "deleteCredentials" should {
    "delete draft and published, revoked credentials" in {
      val issuerId = createParticipant("Issuer X", did)
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        serviceStub.deleteCredentials(request)
        credentialsRepository.getBy(credential1.credentialId).toFuture.futureValue mustBe None
        credentialsRepository.getBy(credential2.credentialId).toFuture.futureValue mustBe None
      }

    }

    "do not delete credentials when one of them is published and not revoked" in {
      val issuerId = createParticipant("Issuer X", did)
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      publishCredential(issuerId, credential1)

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, credential2.credentialId.uuid.toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.deleteCredentials(request)
        )

        credentialsRepository.getBy(credential1.credentialId).toFuture.futureValue mustBe a[Some[_]]
        credentialsRepository.getBy(credential2.credentialId).toFuture.futureValue mustBe a[Some[_]]
      }
    }

    "do not delete credentials when invalid uuid is supplied as credential id" in {
      val issuerId = createParticipant("Issuer X", did)
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, "invalidUUIdzzzz")
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.deleteCredentials(request)
        )

        credentialsRepository.getBy(credential1.credentialId).toFuture.futureValue mustBe a[Some[_]]
        credentialsRepository.getBy(credential2.credentialId).toFuture.futureValue mustBe a[Some[_]]
      }
    }

    "do not delete credentials when one of them has incorrect id" in {
      val issuerId = createParticipant("Issuer X", did)
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      val request = console_api.DeleteCredentialsRequest(
        credentialsIds = List(credential1.credentialId.uuid.toString, UUID.randomUUID().toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.deleteCredentials(request)
        )

        credentialsRepository.getBy(credential1.credentialId).toFuture.futureValue mustBe a[Some[_]]
        credentialsRepository.getBy(credential2.credentialId).toFuture.futureValue mustBe a[Some[_]]
      }
    }
  }

}
