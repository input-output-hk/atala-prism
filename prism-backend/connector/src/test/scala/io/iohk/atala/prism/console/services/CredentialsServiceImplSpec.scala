package io.iohk.atala.prism.console.services

import com.google.protobuf.ByteString
import io.circe
import io.circe.Json
import io.circe.syntax._
import io.grpc.{ServerServiceDefinition, StatusRuntimeException}
import io.iohk.atala.prism.{DIDUtil, RpcSpecBase}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{AtalaOperationId, ConnectorAuthenticator}
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.grpc.ProtoCodecs
import io.iohk.atala.prism.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.console.models.{GenericCredential, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.crypto.{ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.protos.console_api.{
  CredentialsServiceGrpc,
  GetBlockchainDataRequest,
  ShareCredentialResponse
}
import io.iohk.atala.prism.protos.console_models.CManagerGenericCredential
import io.iohk.atala.prism.protos.{common_models, console_api, node_api, node_models}
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.Assertion
import org.scalatest.OptionValues._

import scala.concurrent.Future

class CredentialsServiceImplSpec extends RpcSpecBase with MockitoSugar with ResetMocksAfterEachTest with DIDUtil {

  private val usingApiAs = usingApiAsConstructor(new CredentialsServiceGrpc.CredentialsServiceBlockingStub(_, _))

  private lazy implicit val credentialsRepository = new CredentialsRepository(database)
  private lazy val contactsRepository = new ContactsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private val credentialsIntegration = new CredentialsIntegrationService(credentialsRepository, nodeMock)
  private lazy val authenticator = new ConnectorAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services: Seq[ServerServiceDefinition] =
    Seq(
      console_api.CredentialsServiceGrpc
        .bindService(
          new CredentialsServiceImpl(
            credentialsRepository,
            contactsRepository,
            credentialsIntegration,
            authenticator,
            nodeMock
          ),
          executionContext
        )
    )

  "createGenericCredential" should {
    "create a generic credential" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = createDid
      testCreateGenericCredential(issuerName, keyPair, did)
    }

    "create a generic credential using unpublished did auth" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testCreateGenericCredential(issuerName, keyPair, did)
    }

    "create a generic credential given a extrenal subject id" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = createDid
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)

      val credentialData = Json.obj(
        "claim1" -> "claim 1".asJson,
        "claim2" -> "claim 2".asJson,
        "claim3" -> "claim 3".asJson
      )
      val request = console_api.CreateGenericCredentialRequest(
        credentialData = credentialData.noSpaces,
        groupName = issuerGroup.name.value,
        externalId = subject.externalId.value
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.createGenericCredential(request).genericCredential.value
        succeed
      }
    }
  }

  "getGenericCredentials" should {
    "retrieve correct credentials" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = createDid
      testRetrieveCorrectCredentials(issuerName, keyPair, did)
    }

    "retrieve correct credentials while use unpublished did auth" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testRetrieveCorrectCredentials(issuerName, keyPair, did)
    }

    "retrieve the revocation data when a credential is revoked" in {
      val issuerName = "Issuer 1"
      val (_, did) = createDid
      val issuerId = DataPreparation.createIssuer(issuerName, did = Some(did))
      val contact = DataPreparation.createContact(issuerId, "Subject 1", None, "")
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
        .value
        .futureValue
        .toOption
        .value

      val result = credentialsRepository
        .getBy(issuedBy = issuerId, limit = 10, offset = 0)
        .value
        .futureValue
        .toOption
        .value

      result.size must be(1)
      result.head.revokedOnOperationId.value must be(mockRevocationOperationId)
    }
  }

  "publishBatch" should {
    "forward request to node and store data in database" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = createDid
      testForwardRequestToNodeAndStoreData(issuerName, keyPair, did)
    }

    "forward request to node and store data in database while uses unpublished did auth" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testForwardRequestToNodeAndStoreData(issuerName, keyPair, did)
    }

    "fail if batch id returned by the node is different from the one computed by the console" in {
      val (keyPair, did) = createDid
      DataPreparation.createIssuer("issuerName", publicKey = Some(keyPair.publicKey), did = Some(did))

      val mockDIDSuffix = did.suffix
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val request = console_api
        .PublishBatchRequest()
        .withIssueCredentialBatchOperation(issuanceOp)

      val mockCredentialBatchId =
        CredentialBatchId.fromDigest(SHA256Digest.compute("SomeRandomHash".getBytes()))
      val mockOperationId = AtalaOperationId.fromVectorUnsafe(mockEncodedSignedCredentialHash.value).toProtoByteString

      val nodeRequest = node_api
        .IssueCredentialBatchRequest()
        .withSignedOperation(issuanceOp)

      doReturn(
        Future
          .successful(
            node_api
              .IssueCredentialBatchResponse()
              .withBatchId(mockCredentialBatchId.id)
              .withOperationId(mockOperationId)
          )
      ).when(nodeMock)
        .issueCredentialBatch(nodeRequest)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.publishBatch(request)
        )
      }
    }
  }

  "storePublishedCredential" should {
    "store a credential when the batch was published" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = createDid
      testStoreCredentialWhenBatchPublished(issuerName, keyPair, did)
    }

    "store a credential when the batch was published using unpublished did auth" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testStoreCredentialWhenBatchPublished(issuerName, keyPair, did)
    }

    "fail if issuer is trying to publish an empty encoded signed credential" in {
      val issuerName = "Issuer 1"
      val (keyPair, did) = createDid
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

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

      usingApiAs(rpcRequest) { serviceStub =>
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
      val (keyPair, did) = createDid
      DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))

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

      usingApiAs(rpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.storePublishedCredential(request)
        )

        err.getMessage.endsWith(s"Credential with ID $aRandomId does not exist") mustBe true

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
      val (keyPair, did) = createDid
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

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
      val (keyPair2, did2) = createDid
      DataPreparation.createIssuer(issuerName2, publicKey = Some(keyPair2.publicKey), did = Some(did2))

      val request = console_api
        .StorePublishedCredentialRequest()
        .withConsoleCredentialId(originalCredential.credentialId.uuid.toString)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withEncodedInclusionProof(mockMerkleProof.encode)
        .withBatchId(mockCredentialBatchId.id)

      val rpcRequestIssuer2 = SignedRpcRequest.generate(keyPair2, did2, request)

      usingApiAs(rpcRequestIssuer2) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.storePublishedCredential(request)
        )

        err.getMessage.endsWith("The credential was not issued by the specified issuer") mustBe true

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
      val (keyPair, did) = createDid
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

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

      usingApiAs(rpcRequest) { serviceStub =>
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

  private def cleanCredentialData(gc: CManagerGenericCredential): CManagerGenericCredential =
    gc.copy(credentialData = "", contactData = "")
  private def credentialJsonData(gc: CManagerGenericCredential): (Json, Json) =
    (circe.parser.parse(gc.credentialData).toOption.value, circe.parser.parse(gc.contactData).toOption.value)

  "getContactCredentials" should {
    "return contact's credentials" in {
      val (keyPair, did) = createDid
      testReturnContactCredentials(keyPair, did)
    }

    "return contact's credentials using unpublished did auth" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testReturnContactCredentials(keyPair, did)
    }

    "return empty list of credentials when not present" in {
      val (keyPair, did) = createDid
      val issuerId = DataPreparation.createIssuer("Issuer X", publicKey = Some(keyPair.publicKey), did = Some(did))
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val contactId = createContact(issuerId, "IOHK Student", group.name).contactId

      val request = console_api.GetContactCredentialsRequest(
        contactId = contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getContactCredentials(request)
        response.genericCredentials must be(empty)
      }
    }
  }

  "shareCredential" should {
    "work" in {
      val (keyPair, did) = createDid
      testShareCredential(keyPair, did)
    }
    "work while authed by unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testShareCredential(keyPair, did)
    }
  }

  "getBlockchainData" should {

    // this is the encoded JSON
    // {"type":["VerifiableCredential","RedlandIdCredential"],"id":"did:prism:123456678abcdefg","keyId":"Issuance-0"}
    // and we use a random base64URL as "signature" after the "." (we do not mind about the signature)
    val encodedSignedCredential =
      "eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUmVkbGFuZElkQ3JlZGVudGlhbCJdLCJpZCI6ImRpZDpwcmlzbToxMjM0NTY2NzhhYmNkZWZnIiwia2V5SWQiOiJJc3N1YW5jZS0wIn0.MEUCICmZ463ZZbwNbAuA8TuHFkO0PM0H1UfZtdk2V7YLKFVIAiEAuKELUaOFd75N753Bt2qeNm7ah5fPtvQhgbYzpwB2_Ow="
    val eitherBatchId = for {
      credential <- JsonBasedCredential.fromString(encodedSignedCredential)
      credentialHash = credential.hash
      issuerDID <- credential.content.issuerDid
    } yield CredentialBatchId.fromBatchData(issuerDID.suffix, MerkleRoot(credentialHash))
    val batchId = eitherBatchId.toOption.value
    val nodeRequest = node_api.GetBatchStateRequest(batchId.id)

    "return the expected transaction info when the credential is found" in {
      val (keyPair, did) = createDid
      testReturnExpectedTransactionInfo(keyPair, did)
    }

    "return the expected transaction info when the credential is found using unpublished did auth" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testReturnExpectedTransactionInfo(keyPair, did)
    }

    "return empty transaction info when the credential is not present" in {
      val (keyPair, did) = createDid
      DataPreparation.createIssuer("Issuer X", publicKey = Some(keyPair.publicKey), did = Some(did))

      val nodeResponse = Future
        .successful(
          node_api.GetBatchStateResponse(
            publicationLedgerData = None
          )
        )

      doReturn(nodeResponse)
        .when(nodeMock)
        .getBatchState(nodeRequest)

      val request = GetBlockchainDataRequest(encodedSignedCredential)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getBlockchainData(request)
        response.issuanceProof mustBe empty
      }
    }

    def testReturnExpectedTransactionInfo(keyPair: ECKeyPair, did: DID) = {
      DataPreparation.createIssuer("Issuer X", publicKey = Some(keyPair.publicKey), did = Some(did))
      val expectedTransactionInfoProto = common_models.TransactionInfo(
        transactionId = "3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a",
        ledger = common_models.Ledger.IN_MEMORY,
        None
      )
      val mockLedgerData = node_models.LedgerData(
        transactionId = expectedTransactionInfoProto.transactionId,
        ledger = expectedTransactionInfoProto.ledger
      )

      val nodeResponse = Future
        .successful(
          node_api
            .GetBatchStateResponse()
            .withPublicationLedgerData(mockLedgerData)
        )
      doReturn(nodeResponse)
        .when(nodeMock)
        .getBatchState(nodeRequest)

      val request = GetBlockchainDataRequest(encodedSignedCredential)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getBlockchainData(request)

        val issuanceProof = response.issuanceProof.value
        issuanceProof mustBe expectedTransactionInfoProto
      }
    }
  }

  "revokePublishedCredential" should {
    def withPublishedCredential[T](f: (GenericCredential, ECKeyPair, DID) => T) = {
      val (keyPair, did) = createDid
      withPublishedCredentialAndCustomKeysDid(keyPair, did)(f)
    }
    def withPublishedCredentialAndCustomKeysDid[T](keyPair: ECKeyPair, did: DID)(
        f: (GenericCredential, ECKeyPair, DID) => T
    ) = {
      val issuerName = "Issuer 1"
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
      val contact = DataPreparation.createContact(issuerId, "Subject 1", None, "")
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
      f(originalCredential, keyPair, did)
    }

    "return the generated transaction id" in {
      withPublishedCredential(testReturnTheGeneratedTransactionId)
    }

    "return the generated transaction id while authed by unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      withPublishedCredentialAndCustomKeysDid(keyPair, did)(testReturnTheGeneratedTransactionId)
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

        usingApiAs(rpcRequest) { serviceStub =>
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

        usingApiAs(rpcRequest) { serviceStub =>
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

        usingApiAs(rpcRequest) { serviceStub =>
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

        usingApiAs(rpcRequest) { serviceStub =>
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

        usingApiAs(rpcRequest) { serviceStub =>
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

        val mockRevocationOperationId = AtalaOperationId
          .random()
          .toProtoByteString

        val nodeRequest = node_api
          .RevokeCredentialsRequest()
          .withSignedOperation(revokeCredentialOp)

        doReturn(
          Future
            .successful(
              node_api
                .RevokeCredentialsResponse()
                .withOperationId(mockRevocationOperationId)
            )
        ).when(nodeMock).revokeCredentials(nodeRequest)

        val request = console_api
          .RevokePublishedCredentialRequest()
          .withCredentialId(credential.credentialId.uuid.toString)
          .withRevokeCredentialsOperation(revokeCredentialOp)

        val (keyPair, did) = createDid
        DataPreparation.createIssuer("malicious issuer", publicKey = Some(keyPair.publicKey), did = Some(did))
        val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

        usingApiAs(rpcRequest) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.revokePublishedCredential(request)
          }
        }
      }
    }
  }

  private def publish(
      issuerId: Institution.Id,
      consoleId: GenericCredential.Id,
      encodedSignedCredential: String
  )(implicit credentialsRepository: CredentialsRepository): Unit = {
    val mockHash = SHA256Digest.compute("test".getBytes)
    val mockMerkleProof = MerkleInclusionProof(
      mockHash,
      1, // a random index
      List(mockHash)
    )
    val mockBatchId = CredentialBatchId.fromDigest(mockHash)
    DataPreparation.publishBatch(mockBatchId, mockHash, AtalaOperationId.fromVectorUnsafe(mockHash.value))
    DataPreparation.publishCredential(issuerId, mockBatchId, consoleId, encodedSignedCredential, mockMerkleProof)
  }

  private def testCreateGenericCredential(issuerName: String, keyPair: ECKeyPair, did: DID) = {
    val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
    val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
    val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)

    val credentialData = Json.obj(
      "claim1" -> "claim 1".asJson,
      "claim2" -> "claim 2".asJson,
      "claim3" -> "claim 3".asJson
    )
    val request = console_api.CreateGenericCredentialRequest(
      contactId = subject.contactId.toString,
      credentialData = credentialData.noSpaces,
      groupName = issuerGroup.name.value
    )
    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { serviceStub =>
      val response = serviceStub.createGenericCredential(request).genericCredential.value

      val shouldBeSuccess = GenericCredential.Id.from(response.credentialId)

      shouldBeSuccess.isSuccess mustBe true
      response.issuerId must be(issuerId.toString)
      response.contactId must be(subject.contactId.toString)
      response.credentialData must be(request.credentialData)
      response.issuerName must be(issuerName)
      response.groupName must be(issuerGroup.name.value)
      io.circe.parser.parse(response.contactData).toOption.value must be(subject.data)
      response.issuanceOperationHash must be(empty)
      response.encodedSignedCredential must be(empty)
      response.publicationStoredAt must be(None)
      response.externalId must be(subject.externalId.value)
    }
  }

  private def testRetrieveCorrectCredentials(issuerName: String, keyPair: ECKeyPair, did: DID) = {
    val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
    val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
    val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
    val credential1 = DataPreparation.createGenericCredential(issuerId, subject.contactId)
    val credential2 = DataPreparation.createGenericCredential(issuerId, subject.contactId)
    val credential3 = DataPreparation.createGenericCredential(issuerId, subject.contactId)

    val credentlal1Proto = ProtoCodecs.genericCredentialToProto(credential1)
    val credentlal2Proto = ProtoCodecs.genericCredentialToProto(credential2)
    val credentlal3Proto = ProtoCodecs.genericCredentialToProto(credential3)

    val requestFirst = console_api.GetGenericCredentialsRequest(
      limit = 1
    )
    val rpcRequestFirst = SignedRpcRequest.generate(keyPair, did, requestFirst)

    usingApiAs(rpcRequestFirst) { serviceStub =>
      val response = serviceStub.getGenericCredentials(requestFirst).credentials
      response.size must be(1)
      val retrievedCred = response.headOption.value
      retrievedCred must be(credentlal1Proto)
      retrievedCred mustNot be(credentlal2Proto)
      retrievedCred mustNot be(credentlal3Proto)
    }

    val requestMoreThanExist = console_api.GetGenericCredentialsRequest(
      limit = 4
    )
    val rpcRequestMoreThanExist = SignedRpcRequest.generate(keyPair, did, requestMoreThanExist)
    usingApiAs(rpcRequestMoreThanExist) { serviceStub =>
      val allCredentials = serviceStub.getGenericCredentials(requestMoreThanExist).credentials
      allCredentials.size must be(3)

      allCredentials.toSet must be(Set(credentlal1Proto, credentlal2Proto, credentlal3Proto))
    }

    val requestLastTwo = console_api.GetGenericCredentialsRequest(
      limit = 2,
      offset = 1
    )
    val rpcRequestLastTwo = SignedRpcRequest.generate(keyPair, did, requestLastTwo)
    usingApiAs(rpcRequestLastTwo) { serviceStub =>
      val lastTwoCredentials = serviceStub.getGenericCredentials(requestLastTwo).credentials
      lastTwoCredentials.size must be(2)
    }
  }

  private def testForwardRequestToNodeAndStoreData(issuerName: String, keyPair: ECKeyPair, did: DID): Assertion = {
    val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
    val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
    val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
    val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

    val mockDIDSuffix = DID.buildPrismDID(SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue).suffix
    val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
    val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

    val issuanceOp = buildSignedIssueCredentialOp(
      mockEncodedSignedCredentialHash,
      mockDIDSuffix
    )

    val mockOperationHash = SHA256Digest.compute(issuanceOp.getOperation.toByteArray)
    val mockCredentialBatchId =
      CredentialBatchId.fromBatchData(mockDIDSuffix, MerkleRoot(mockEncodedSignedCredentialHash))

    val nodeRequest = node_api
      .IssueCredentialBatchRequest()
      .withSignedOperation(issuanceOp)

    doReturn(
      Future
        .successful(
          node_api
            .IssueCredentialBatchResponse()
            .withOperationId(AtalaOperationId.fromVectorUnsafe(mockOperationHash.value).toProtoByteString)
            .withBatchId(mockCredentialBatchId.id)
        )
    ).when(nodeMock)
      .issueCredentialBatch(nodeRequest)

    val request = console_api
      .PublishBatchRequest()
      .withIssueCredentialBatchOperation(issuanceOp)

    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { serviceStub =>
      val publishResponse = serviceStub.publishBatch(request)
      verify(nodeMock).issueCredentialBatch(nodeRequest)

      // we publish the credential data
      val mockHash = SHA256Digest.compute("".getBytes())
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))
      publishCredential(
        issuerId,
        mockCredentialBatchId,
        originalCredential.credentialId,
        mockEncodedSignedCredential,
        mockMerkleProof
      )

      val credentialList =
        credentialsRepository.getBy(issuerId, subject.contactId).value.futureValue.toOption.value

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value

      val publicationData = updatedCredential.publicationData.value

      publicationData.credentialBatchId must be(mockCredentialBatchId)
      publicationData.issuanceOperationHash must be(mockOperationHash)
      publicationData.encodedSignedCredential must be(mockEncodedSignedCredential)
      // the rest should remain unchanged
      updatedCredential.copy(publicationData = None) must be(originalCredential)

      publishResponse.batchId mustBe mockCredentialBatchId.id
      publishResponse.operationId must be(AtalaOperationId.fromVectorUnsafe(mockOperationHash.value).toProtoByteString)
    }
  }

  private def testStoreCredentialWhenBatchPublished(issuerName: String, keyPair: ECKeyPair, did: DID) = {
    val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
    val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
    val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
    val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

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

    usingApiAs(rpcRequest) { serviceStub =>
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
      storedPublicationData.atalaOperationId mustBe atalaOperationId
    }
  }

  private def testReturnContactCredentials(keyPair: ECKeyPair, did: DID) = {
    val issuerId = DataPreparation.createIssuer("Issuer X", publicKey = Some(keyPair.publicKey), did = Some(did))
    val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
    val contactId1 = createContact(issuerId, "IOHK Student", group.name).contactId
    val contactId2 = createContact(issuerId, "IOHK Student 2", group.name).contactId
    createGenericCredential(issuerId, contactId2, "A")
    val cred1 = createGenericCredential(issuerId, contactId1, "B")
    createGenericCredential(issuerId, contactId2, "C")
    val cred2 = createGenericCredential(issuerId, contactId1, "D")
    createGenericCredential(issuerId, contactId2, "E")

    val request = console_api.GetContactCredentialsRequest(
      contactId = contactId1.toString
    )
    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { serviceStub =>
      val response = serviceStub.getContactCredentials(request)
      val returnedCredentials = response.genericCredentials.toList
      val cleanCredentials = returnedCredentials map cleanCredentialData
      val credentialsJsons = returnedCredentials map credentialJsonData

      val expectedCredentials = List(cred1, cred2)
      val expectedCleanCredentials = expectedCredentials map {
        ProtoCodecs.genericCredentialToProto _ andThen cleanCredentialData
      }
      val expectedCredentialsJsons = expectedCredentials map {
        ProtoCodecs.genericCredentialToProto _ andThen credentialJsonData
      }
      cleanCredentials must be(expectedCleanCredentials)
      credentialsJsons must be(expectedCredentialsJsons)
      returnedCredentials.forall(_.issuanceOperationHash.isEmpty) must be(true)
    }
  }

  private def testShareCredential(keyPair: ECKeyPair, did: DID): ShareCredentialResponse = {
    val issuerId = DataPreparation.createIssuer("Issuer X", publicKey = Some(keyPair.publicKey), did = Some(did))
    val contactId = createContact(issuerId, "IOHK Student", None, "").contactId
    val credentialId = createGenericCredential(issuerId, contactId, "A").credentialId
    val mockCredential = "mockEncodedSignedCredential"

    publish(issuerId, credentialId, mockCredential)

    val request = console_api.ShareCredentialRequest(
      cmanagerCredentialId = credentialId.toString
    )
    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { serviceStub =>
      serviceStub.shareCredential(request)
    }
  }

  private def testReturnTheGeneratedTransactionId(
      credential: GenericCredential,
      keyPair: ECKeyPair,
      did: DID
  ): Assertion = {
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

    val mockRevocationOperationId = AtalaOperationId.random()

    val nodeRequest = node_api
      .RevokeCredentialsRequest()
      .withSignedOperation(revokeCredentialOp)

    doReturn(
      Future
        .successful(
          node_api
            .RevokeCredentialsResponse()
            .withOperationId(mockRevocationOperationId.toProtoByteString)
        )
    ).when(nodeMock).revokeCredentials(nodeRequest)

    val request = console_api
      .RevokePublishedCredentialRequest()
      .withCredentialId(credential.credentialId.uuid.toString)
      .withRevokeCredentialsOperation(revokeCredentialOp)

    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { serviceStub =>
      serviceStub.revokePublishedCredential(request)

      val revokedOnOperationId = credentialsRepository
        .getBy(credential.credentialId)
        .value
        .futureValue
        .toOption
        .value
        .value
        .revokedOnOperationId

      revokedOnOperationId must be(Some(mockRevocationOperationId))
    }
  }

}
