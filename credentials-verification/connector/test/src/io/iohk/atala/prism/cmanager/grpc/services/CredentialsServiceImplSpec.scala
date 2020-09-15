package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.syntax._
import io.grpc.ServerServiceDefinition
import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs
import io.iohk.atala.prism.cmanager.models.{GenericCredential, IssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation
import io.iohk.atala.prism.cmanager.repositories.{CredentialsRepository, IssuerSubjectsRepository}
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.prism.protos.cmanager_api.CredentialsServiceGrpc
import io.iohk.prism.protos.{cmanager_api, node_api, node_models}
import org.mockito.MockitoSugar
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.Future

class CredentialsServiceImplSpec extends RpcSpecBase with MockitoSugar {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private val usingApiAs = usingApiAsConstructor(new CredentialsServiceGrpc.CredentialsServiceBlockingStub(_, _))

  private lazy val credentialsRepository = new CredentialsRepository(database)
  private lazy val subjectsRepository = new IssuerSubjectsRepository(database)
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
          new CredentialsServiceImpl(credentialsRepository, subjectsRepository, authenticator, nodeMock),
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
        response.nodeCredentialId must be(empty)
        response.issuanceOperationHash must be(empty)
        response.encodedSignedCredential must be(empty)
        response.publicationStoredAt must be(0)
        response.externalId must be(subject.externalId.value)
      }
    }

    "create a generic credential given a extrenal subject id" in {
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
          credentialData = credentialData.noSpaces,
          groupName = issuerGroup.name.value,
          externalId = subject.externalId.value
        )

        serviceStub.createGenericCredential(request).genericCredential.value
        succeed
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
      val originalCredential = DataPreparation.createGenericCredential(issuer.id, subject.id)

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
          .withCmanagerCredentialId(originalCredential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(ByteString.copyFrom(mockOperationHash.value))

        serviceStub.publishCredential(request)

        verify(nodeMock).issueCredential(nodeRequest)

        val credentialList =
          credentialsRepository.getBy(issuer.id, subject.id).value.futureValue.right.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential.publicationData.value.nodeCredentialId must be(mockNodeCredentialId)
        updatedCredential.publicationData.value.issuanceOperationHash must be(mockOperationHash)
        updatedCredential.publicationData.value.encodedSignedCredential must be(mockEncodedSignedCredential)
        // the rest should remain unchanged
        updatedCredential.copy(publicationData = None) must be(originalCredential)
      }
    }

    "fail if issuer is trying to publish a credential he didn't create" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuer.id, subject.id)

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
          .withCmanagerCredentialId(originalCredential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(ByteString.copyFrom(mockOperationHash.value))

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        val credentialList =
          credentialsRepository.getBy(issuer.id, subject.id).value.futureValue.right.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential must be(originalCredential)
        updatedCredential.publicationData must be(empty)
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
          .withOperationHash(ByteString.copyFrom(mockOperationHash.value))

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        val credentialList =
          credentialsRepository.getBy(issuer.id, 10, None).value.futureValue.right.value

        credentialList must be(empty)
      }
    }

    "fail if operation hash does not match the protocol credential id" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuer.id, subject.id)

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
          .withCmanagerCredentialId(originalCredential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEncodedSignedCredential)
          .withNodeCredentialId(mockIncorrectNodeCredentialId)
          .withOperationHash(ByteString.copyFrom(mockOperationHash.value))

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        val credentialList =
          credentialsRepository.getBy(issuer.id, subject.id).value.futureValue.right.value

        credentialList.length must be(1)

        val updatedCredential = credentialList.headOption.value

        updatedCredential must be(originalCredential)
        updatedCredential.publicationData must be(empty)
      }
    }

    "fail if issuer is trying to publish an empty encoded signed credential" in {
      val issuerName = "Issuer 1"
      val issuer = DataPreparation.createIssuer(issuerName)
      val issuerGroup = DataPreparation.createIssuerGroup(issuer.id, IssuerGroup.Name("Group 1"))
      val subject = DataPreparation.createSubject(issuer.id, "Subject 1", issuerGroup.name)
      val originalCredential = DataPreparation.createGenericCredential(issuer.id, subject.id)

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
          .withCmanagerCredentialId(originalCredential.credentialId.value.toString)
          .withIssueCredentialOperation(issuanceOp)
          .withEncodedSignedCredential(mockEmptyEncodedSignedCredential)
          .withNodeCredentialId(mockNodeCredentialId)
          .withOperationHash(ByteString.copyFrom(mockOperationHash.value))

        verifyNoMoreInteractions(nodeMock)

        intercept[RuntimeException](
          serviceStub.publishCredential(request)
        )

        val credentialList =
          credentialsRepository.getBy(issuer.id, subject.id).value.futureValue.right.value

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
