package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.syntax._
import doobie.implicits._
import io.grpc.ServerServiceDefinition
import io.iohk.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs
import io.iohk.cvp.cmanager.models.{GenericCredential, IssuerGroup}
import io.iohk.cvp.cmanager.repositories.CredentialsRepository
import io.iohk.cvp.cmanager.repositories.common.DataPreparation
import io.iohk.cvp.cmanager.repositories.daos.CredentialsDAO
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.grpc.GrpcAuthenticationHeaderParser
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.{cmanager_api, node_api, node_models}
import io.iohk.prism.protos.cmanager_api.CredentialsServiceGrpc

import org.mockito.MockitoSugar
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.Future

class CredentialsServiceImplSpec extends RpcSpecBase with MockitoSugar {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private val usingApiAs = usingApiAsConstructor(new CredentialsServiceGrpc.CredentialsServiceBlockingStub(_, _))

  private lazy val credentialsRepository = new CredentialsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new SignedRequestsAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services: Seq[ServerServiceDefinition] =
    Seq(
      cmanager_api.CredentialsServiceGrpc
        .bindService(
          new CredentialsServiceImpl(credentialsRepository, authenticator, nodeMock),
          executionContext
        )
    )

  "createGenericCredential" should {
    "create a generic credential" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)

      usingApiAs(ParticipantId(issuer.id.value)) { serviceStub =>
        val credentialData = Json.obj(
          "claim1" -> "claim 1".asJson,
          "claim2" -> "claim 2".asJson,
          "claim3" -> "claim 3".asJson
        )

        val request = cmanager_api.CreateGenericCredentialRequest(
          subjectId = subject.id.value.toString,
          credentialData = credentialData.noSpaces,
          groupName = issuerGroup.name.value
        )

        val response = serviceStub.createGenericCredential(request).genericCredential.value

        response.credentialId mustNot be(empty)
        response.issuerId must be(issuer.id.value.toString)
        response.subjectId must be(subject.id.value.toString)
        response.credentialData must be(request.credentialData)
        response.issuerName must be(issuerName)
        response.groupName must be(issuerGroup.name.value)
        io.circe.parser.parse(response.subjectData).right.value must be(subject.data)
      }
    }
  }

  "getGenericCredentials" should {
    "retrieve correct credentials" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val credential1 = DataPreparation.createGenericCredential(issuer.id, subject.id)
      val credential2 = DataPreparation.createGenericCredential(issuer.id, subject.id)
      val credential3 = DataPreparation.createGenericCredential(issuer.id, subject.id)

      val credentlal1Proto = ProtoCodecs.genericCredentialToProto(credential1)
      val credentlal2Proto = ProtoCodecs.genericCredentialToProto(credential2)
      val credentlal3Proto = ProtoCodecs.genericCredentialToProto(credential3)

      usingApiAs(ParticipantId(issuer.id.value)) { serviceStub =>
        val requestFirst = cmanager_api.GetGenericCredentialsRequest(
          limit = 1
        )

        val response = serviceStub.getGenericCredentials(requestFirst).credentials
        response.size must be(1)
        val retrievedCred = response.headOption.value
        retrievedCred must be(credentlal1Proto)
        retrievedCred mustNot be(credentlal2Proto)
        retrievedCred mustNot be(credentlal3Proto)

        val requestMoreThanExistent = cmanager_api.GetGenericCredentialsRequest(
          limit = 4
        )

        val allCredentials = serviceStub.getGenericCredentials(requestMoreThanExistent).credentials
        allCredentials.size must be(3)

        allCredentials.toSet must be(Set(credentlal1Proto, credentlal2Proto, credentlal3Proto))

        val requestLastTwo = cmanager_api.GetGenericCredentialsRequest(
          limit = 2,
          lastSeenCredentialId = credential1.credentialId.value.toString
        )

        val lastTwoCredentials = serviceStub.getGenericCredentials(requestLastTwo).credentials
        lastTwoCredentials.size must be(2)

        lastTwoCredentials.toSet must be(Set(credentlal2Proto, credentlal3Proto))
      }
    }
  }

  "publishCredential" should {
    "forward request to node and store data in database" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val credential = DataPreparation.createGenericCredential(issuer.id, subject.id)

      val mockDIDSuffix = s"did:prism:${SHA256Digest.compute("issuerDIDSuffic".getBytes()).hexValue}"
      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOp = buildSignedIssueCredentialOp(
        SHA256Digest.compute(mockEncodedSignedCredential.getBytes()),
        mockDIDSuffix
      )

      val nodeRequest = node_api
        .IssueCredentialRequest()
        .withSignedOperation(issuanceOp)

      doReturn(Future.successful(node_api.IssueCredentialResponse(mockNodeCredentialId)))
        .when(nodeMock)
        .issueCredential(nodeRequest)

      usingApiAs(ParticipantId(issuer.id.value)) { serviceStub =>
        val request = cmanager_api
          .PublishCredentialRequest()
          .withCmanagerCredentialId(credential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(mockOperationHash.hexValue)

        serviceStub.publishCredential(request)

        verify(nodeMock).issueCredential(nodeRequest)

        val storedCredential =
          CredentialsDAO.getPublicationData(credential.credentialId).transact(database).unsafeRunSync().value

        storedCredential.credentialId must be(credential.credentialId)
        storedCredential.nodeCredentialId must be(mockNodeCredentialId)
        storedCredential.issuanceOperationHash must be(mockOperationHash)
        storedCredential.encodedSignedCredential must be(mockEncodedSignedCredential)
      }
    }

    "fail if issuer is trying to publish a credential he didn't create" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val credential = DataPreparation.createGenericCredential(issuer.id, subject.id)

      val mockDIDSuffix = s"did:prism:${SHA256Digest.compute("issuerDIDSuffic".getBytes()).hexValue}"
      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOp = buildSignedIssueCredentialOp(
        SHA256Digest.compute(mockEncodedSignedCredential.getBytes()),
        mockDIDSuffix
      )

      val wrongIssuerId = UUID.randomUUID()

      usingApiAs(ParticipantId(wrongIssuerId)) { serviceStub =>
        val request = cmanager_api
          .PublishCredentialRequest()
          .withCmanagerCredentialId(credential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(mockOperationHash.hexValue)

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        CredentialsDAO.getPublicationData(credential.credentialId).transact(database).unsafeRunSync() must be(empty)
      }
    }

    "fail if issuer is trying to publish a credential that does not exist in the db" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)

      val mockDIDSuffix = s"did:prism:${SHA256Digest.compute("issuerDIDSuffic".getBytes()).hexValue}"
      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOp = buildSignedIssueCredentialOp(
        SHA256Digest.compute(mockEncodedSignedCredential.getBytes()),
        mockDIDSuffix
      )

      val unknownCredentialId = GenericCredential.Id(UUID.randomUUID())

      usingApiAs(ParticipantId(issuer.id.value)) { serviceStub =>
        val request = cmanager_api
          .PublishCredentialRequest()
          .withCmanagerCredentialId(unknownCredentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(mockOperationHash.hexValue)

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        CredentialsDAO.getPublicationData(unknownCredentialId).transact(database).unsafeRunSync() must be(empty)
      }
    }

    "fail if operation hash does not match the protocol credential id" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val credential = DataPreparation.createGenericCredential(issuer.id, subject.id)

      val mockDIDSuffix = s"did:prism:${SHA256Digest.compute("issuerDIDSuffic".getBytes()).hexValue}"
      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockIncorrectNodeCredentialId = SHA256Digest.compute("A DIFFERENT VALUE TO 000".getBytes()).hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"

      val issuanceOp = buildSignedIssueCredentialOp(
        SHA256Digest.compute(mockEncodedSignedCredential.getBytes()),
        mockDIDSuffix
      )

      val wrongIssuerId = UUID.randomUUID()

      usingApiAs(ParticipantId(wrongIssuerId)) { serviceStub =>
        val request = cmanager_api
          .PublishCredentialRequest()
          .withCmanagerCredentialId(credential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockIncorrectNodeCredentialId)
          .withOperationHash(mockOperationHash.hexValue)

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        CredentialsDAO.getPublicationData(credential.credentialId).transact(database).unsafeRunSync() must be(empty)
      }
    }

    "fail if issuer is trying to publish an empty encoded signed credential" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val credential = DataPreparation.createGenericCredential(issuer.id, subject.id)

      val mockDIDSuffix = s"did:prism:${SHA256Digest.compute("issuerDIDSuffic".getBytes()).hexValue}"
      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue

      val mockEmptyEncodedSignedCredential = ""

      val issuanceOp = buildSignedIssueCredentialOp(
        SHA256Digest.compute(mockEmptyEncodedSignedCredential.getBytes()),
        mockDIDSuffix
      )

      usingApiAs(ParticipantId(issuer.id.value)) { serviceStub =>
        val request = cmanager_api
          .PublishCredentialRequest()
          .withCmanagerCredentialId(credential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEmptyEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(mockOperationHash.hexValue)

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        CredentialsDAO.getPublicationData(credential.credentialId).transact(database).unsafeRunSync() must be(empty)
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
                  contentHash = ByteString.copyFrom(credentialHash.value)
                )
              )
            )
          )
        )
      )
    )
  }
}
