package io.iohk.atala.prism.console.services

import com.google.protobuf.ByteString
import io.circe
import io.circe.Json
import io.circe.syntax._
import io.grpc.ServerServiceDefinition
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.grpc.ProtoCodecs
import io.iohk.atala.prism.console.models.{GenericCredential, Institution, IssuerGroup, PublishCredential}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.credentials.SlayerCredentialId
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.protos.console_api.{CredentialsServiceGrpc, GetBlockchainDataRequest}
import io.iohk.atala.prism.protos.console_models.CManagerGenericCredential
import io.iohk.atala.prism.protos.{common_models, console_api, node_api, node_models}
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.OptionValues._

import scala.concurrent.Future

class CredentialsServiceImplSpec extends RpcSpecBase with MockitoSugar with ResetMocksAfterEachTest with DIDGenerator {

  private val usingApiAs = usingApiAsConstructor(new CredentialsServiceGrpc.CredentialsServiceBlockingStub(_, _))

  private lazy val credentialsRepository = new CredentialsRepository(database)
  private lazy val contactsRepository = new ContactsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
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
          new CredentialsServiceImpl(credentialsRepository, contactsRepository, authenticator, nodeMock),
          executionContext
        )
    )

  "createGenericCredential" should {
    "create a generic credential" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
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

        response.credentialId mustNot be(empty)
        response.issuerId must be(issuerId.toString)
        response.contactId must be(subject.contactId.toString)
        response.credentialData must be(request.credentialData)
        response.issuerName must be(issuerName)
        response.groupName must be(issuerGroup.name.value)
        io.circe.parser.parse(response.contactData).toOption.value must be(subject.data)
        response.nodeCredentialId must be(empty)
        response.issuanceOperationHash must be(empty)
        response.encodedSignedCredential must be(empty)
        response.publicationStoredAt must be(0)
        response.externalId must be(subject.externalId.value)
      }
    }

    "create a generic credential given a extrenal subject id" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
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
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
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

      val requestMoreThanExistent = console_api.GetGenericCredentialsRequest(
        limit = 4
      )
      val rpcRequestMoreThanExistent = SignedRpcRequest.generate(keyPair, did, requestMoreThanExistent)
      usingApiAs(rpcRequestMoreThanExistent) { serviceStub =>
        val allCredentials = serviceStub.getGenericCredentials(requestMoreThanExistent).credentials
        allCredentials.size must be(3)

        allCredentials.toSet must be(Set(credentlal1Proto, credentlal2Proto, credentlal3Proto))
      }

      val requestLastTwo = console_api.GetGenericCredentialsRequest(
        limit = 2,
        lastSeenCredentialId = credential1.credentialId.toString
      )
      val rpcRequestLastTwo = SignedRpcRequest.generate(keyPair, did, requestLastTwo)
      usingApiAs(rpcRequestLastTwo) { serviceStub =>
        val lastTwoCredentials = serviceStub.getGenericCredentials(requestLastTwo).credentials
        lastTwoCredentials.size must be(2)

        lastTwoCredentials.toSet must be(Set(credentlal2Proto, credentlal3Proto))
      }
    }
  }

  "publishCredential" should {
    "forward request to node and store data in database" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

      val mockDIDSuffix = SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockOperationHash = SHA256Digest.compute(issuanceOp.getOperation.toByteArray)
      val mockNodeCredentialId =
        SlayerCredentialId.compute(mockEncodedSignedCredentialHash, DID.buildPrismDID(mockDIDSuffix)).string
      val mockTransactionInfo =
        common_models
          .TransactionInfo()
          .withTransactionId(mockNodeCredentialId)
          .withLedger(common_models.Ledger.IN_MEMORY)

      val nodeRequest = node_api
        .IssueCredentialRequest()
        .withSignedOperation(issuanceOp)

      doReturn(
        Future
          .successful(node_api.IssueCredentialResponse(mockNodeCredentialId).withTransactionInfo(mockTransactionInfo))
      ).when(nodeMock)
        .issueCredential(nodeRequest)

      val request = console_api
        .PublishCredentialRequest()
        .withCmanagerCredentialId(originalCredential.credentialId.toString)
        .withIssueCredentialOperation(issuanceOp)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withNodeCredentialId(mockNodeCredentialId)
        .withOperationHash(ByteString.copyFrom(mockOperationHash.value.toArray))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.publishCredential(request)

        verify(nodeMock).issueCredential(nodeRequest)

        val credentialList =
          credentialsRepository.getBy(issuerId, subject.contactId).value.futureValue.toOption.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value
        val publicationData = updatedCredential.publicationData.value

        publicationData.nodeCredentialId must be(mockNodeCredentialId)
        publicationData.issuanceOperationHash must be(mockOperationHash)
        publicationData.encodedSignedCredential must be(mockEncodedSignedCredential)
        publicationData.transactionId must be(TransactionId.from(mockNodeCredentialId).value)
        publicationData.ledger must be(Ledger.InMemory)
        // the rest should remain unchanged
        updatedCredential.copy(publicationData = None) must be(originalCredential)
      }
    }

    "fail if issuer is trying to publish a credential he didn't create" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

      val mockDIDSuffix = SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockOperationHash = SHA256Digest.compute(issuanceOp.getOperation.toByteArray)
      val mockNodeCredentialId =
        SlayerCredentialId.compute(mockEncodedSignedCredentialHash, DID.buildPrismDID(mockDIDSuffix)).string

      val request = console_api
        .PublishCredentialRequest()
        .withCmanagerCredentialId(originalCredential.credentialId.toString)
        .withIssueCredentialOperation(issuanceOp)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withNodeCredentialId(mockNodeCredentialId)
        .withOperationHash(ByteString.copyFrom(mockOperationHash.value.toArray))

      val issuerName2 = "Issuer 2"
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
      val did2 = generateDid(publicKey2)
      DataPreparation.createIssuer(issuerName2, publicKey = Some(publicKey2), did = Some(did2))

      val wrongRpcRequest = SignedRpcRequest.generate(keyPair2, did2, request)

      usingApiAs(wrongRpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        err.getMessage must be("INTERNAL: requirement failed: The credential was not issued by the specified issuer")

        val credentialList =
          credentialsRepository.getBy(issuerId, subject.contactId).value.futureValue.toOption.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential must be(originalCredential)
        updatedCredential.publicationData must be(empty)
      }
    }

    "fail if issuer is trying to publish a credential that does not exist in the db" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))

      val mockDIDSuffix = SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockOperationHash = SHA256Digest.compute(issuanceOp.getOperation.toByteArray)
      val mockNodeCredentialId =
        SlayerCredentialId.compute(mockEncodedSignedCredentialHash, DID.buildPrismDID(mockDIDSuffix)).string

      val unknownCredentialId = GenericCredential.Id.random()
      val request = console_api
        .PublishCredentialRequest()
        .withCmanagerCredentialId(unknownCredentialId.toString)
        .withIssueCredentialOperation(issuanceOp)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withNodeCredentialId(mockNodeCredentialId)
        .withOperationHash(ByteString.copyFrom(mockOperationHash.value.toArray))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        err.getMessage.startsWith("INTERNAL: Credential with ID") must be(true)

        val credentialList =
          credentialsRepository.getBy(issuerId, 10, None).value.futureValue.toOption.value

        credentialList must be(empty)
      }
    }

    "fail if the issuer uses an incorrect protocol credential id" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

      val mockDIDSuffix = SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockOperationHash = SHA256Digest.compute(issuanceOp.getOperation.toByteArray)
      val mockIncorrectNodeCredentialId = SHA256Digest.compute("AN INCORRECT VALUE".getBytes()).hexValue

      val request = console_api
        .PublishCredentialRequest()
        .withCmanagerCredentialId(originalCredential.credentialId.toString)
        .withIssueCredentialOperation(issuanceOp)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withNodeCredentialId(mockIncorrectNodeCredentialId)
        .withOperationHash(ByteString.copyFrom(mockOperationHash.value.toArray))

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        err.getMessage.endsWith("Invalid credential protocol id") must be(true)

        val credentialList =
          credentialsRepository.getBy(issuerId, subject.contactId).value.futureValue.toOption.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential must be(originalCredential)
        updatedCredential.publicationData must be(empty)
      }
    }

    "fail if the issuer provides a hash which does not match with the provided operation" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

      val mockDIDSuffix = SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockIncorrectOperationHash = SHA256Digest.compute("AN INVALID VALUE".getBytes())
      val mockNodeCredentialId =
        SlayerCredentialId.compute(mockEncodedSignedCredentialHash, DID.buildPrismDID(mockDIDSuffix)).string

      val request = console_api
        .PublishCredentialRequest()
        .withCmanagerCredentialId(originalCredential.credentialId.toString)
        .withIssueCredentialOperation(issuanceOp)
        .withEncodedSignedCredential(mockEncodedSignedCredential)
        .withNodeCredentialId(mockNodeCredentialId)
        .withOperationHash(ByteString.copyFrom(mockIncorrectOperationHash.value.toArray))

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        err.getMessage.endsWith("Operation hash does not match the provided operation") must be(true)

        val credentialList =
          credentialsRepository.getBy(issuerId, subject.contactId).value.futureValue.toOption.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential must be(originalCredential)
        updatedCredential.publicationData must be(empty)
      }
    }

    "fail if issuer is trying to publish an empty encoded signed credential" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
      val issuerGroup = DataPreparation.createIssuerGroup(issuerId, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createContact(issuerId, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuerId, subject.contactId)

      val mockDIDSuffix = SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue
      val mockEmptyEncodedSignedCredential = ""
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEmptyEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockOperationHash = SHA256Digest.compute(issuanceOp.getOperation.toByteArray)
      val mockNodeCredentialId =
        SlayerCredentialId.compute(mockEncodedSignedCredentialHash, DID.buildPrismDID(mockDIDSuffix)).string

      val request = console_api
        .PublishCredentialRequest()
        .withCmanagerCredentialId(originalCredential.credentialId.toString)
        .withIssueCredentialOperation(issuanceOp)
        .withEncodedSignedCredential(mockEmptyEncodedSignedCredential)
        .withNodeCredentialId(mockNodeCredentialId)
        .withOperationHash(ByteString.copyFrom(mockOperationHash.value.toArray))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val err = intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        err.getMessage.endsWith("Empty encoded credential") must be(true)

        val credentialList =
          credentialsRepository.getBy(issuerId, subject.contactId).value.futureValue.toOption.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential must be(originalCredential)
        updatedCredential.publicationData must be(empty)
      }
    }
  }

  def buildSignedIssueCredentialOp(
      credentialHash: SHA256Digest,
      didSuffix: String
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = "mockKey",
      signature = ByteString.copyFrom("".getBytes()),
      operation = Some(
        node_models.AtalaOperation(
          operation = node_models.AtalaOperation.Operation.IssueCredential(
            node_models.IssueCredentialOperation(
              credentialData = Some(
                node_models.CredentialData(
                  issuer = didSuffix,
                  contentHash = ByteString.copyFrom(credentialHash.value.toArray)
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
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer("Issuer X", publicKey = Some(publicKey), did = Some(did))
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
        returnedCredentials.forall(_.issuanceProof.isEmpty) must be(true)
      }
    }

    "return empty list of credentials when not present" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer("Issuer X", publicKey = Some(publicKey), did = Some(did))
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
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = DataPreparation.createIssuer("Issuer X", publicKey = Some(publicKey), did = Some(did))
      val contactId = createContact(issuerId, "IOHK Student", None, "").contactId
      val credentialId = createGenericCredential(issuerId, contactId, "A").credentialId
      val mockCredential = "mockEncodedSignedCredential"
      val mockTransactionInfo = TransactionInfo(
        TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
        Ledger.InMemory,
        None
      )

      publish(issuerId, credentialId, mockCredential, mockTransactionInfo)

      val request = console_api.ShareCredentialRequest(
        cmanagerCredentialId = credentialId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.shareCredential(request)
      }
    }
  }

  "getBlockchainData" should {
    "return the expected transaction info when the credential is found" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      DataPreparation.createIssuer("Issuer X", publicKey = Some(publicKey), did = Some(did))
      val encodedSignedCredential =
        "eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUmVkbGFuZElkQ3JlZGVudGlhbCJdLCJpZCI6ImRpZDpwcmlzbToxMjM0NTY2NzhhYmNkZWZnIiwia2V5SWQiOiJJc3N1YW5jZS0wIn0.MEUCICmZ463ZZbwNbAuA8TuHFkO0PM0H1UfZtdk2V7YLKFVIAiEAuKELUaOFd75N753Bt2qeNm7ah5fPtvQhgbYzpwB2_Ow="
      val mockTransactionInfoProto = common_models.TransactionInfo(
        transactionId = "3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a",
        ledger = common_models.Ledger.IN_MEMORY,
        None
      )

      val nodeRequest = node_api.GetCredentialTransactionInfoRequest(
        SlayerCredentialId.compute(encodedSignedCredential).toOption.value.string
      )
      val nodeResponse = Future
        .successful(
          node_api
            .GetCredentialTransactionInfoResponse()
            .withIssuance(mockTransactionInfoProto)
        )
      doReturn(nodeResponse)
        .when(nodeMock)
        .getCredentialTransactionInfo(nodeRequest)

      val request = GetBlockchainDataRequest(encodedSignedCredential)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getBlockchainData(request)

        val issuanceProof = response.issuanceProof.value
        issuanceProof mustBe mockTransactionInfoProto
      }
    }

    "return empty transaction info when the credential is not present" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      DataPreparation.createIssuer("Issuer X", publicKey = Some(publicKey), did = Some(did))
      // this is the encoded JSON
      // {"type":["VerifiableCredential","RedlandIdCredential"],"id":"did:prism:123456678abcdefg","keyId":"Issuance-0"}
      // and we use a random base64URL as "signature" after the "." (we do not mind about the signature)
      val encodedSignedCredential =
        "eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUmVkbGFuZElkQ3JlZGVudGlhbCJdLCJpZCI6ImRpZDpwcmlzbToxMjM0NTY2NzhhYmNkZWZnIiwia2V5SWQiOiJJc3N1YW5jZS0wIn0.MEUCICmZ463ZZbwNbAuA8TuHFkO0PM0H1UfZtdk2V7YLKFVIAiEAuKELUaOFd75N753Bt2qeNm7ah5fPtvQhgbYzpwB2_Ow="

      val nodeRequest = node_api.GetCredentialTransactionInfoRequest(
        SlayerCredentialId.compute(encodedSignedCredential).toOption.value.string
      )
      val nodeResponse = Future
        .successful(
          node_api.GetCredentialTransactionInfoResponse(
            issuance = None
          )
        )

      doReturn(nodeResponse)
        .when(nodeMock)
        .getCredentialTransactionInfo(nodeRequest)

      val request = GetBlockchainDataRequest(encodedSignedCredential)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getBlockchainData(request)
        response.issuanceProof mustBe empty
      }
    }
  }

  private def publish(
      issuerId: Institution.Id,
      id: GenericCredential.Id,
      encodedSignedCredential: String,
      mockTransactionInfo: TransactionInfo
  ): Unit = {
    val _ = credentialsRepository
      .storePublicationData(
        issuerId,
        PublishCredential(
          id,
          SHA256Digest.compute("test".getBytes),
          "mockNodeCredentialId",
          encodedSignedCredential,
          mockTransactionInfo
        )
      )
      .value
      .futureValue
  }
}
