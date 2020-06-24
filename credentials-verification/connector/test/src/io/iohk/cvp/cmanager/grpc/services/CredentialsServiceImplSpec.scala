package io.iohk.cvp.cmanager.grpc.services

import io.circe.Json
import io.circe.syntax._
import io.grpc.ServerServiceDefinition
import io.iohk.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs
import io.iohk.cvp.cmanager.models.IssuerGroup
import io.iohk.cvp.cmanager.repositories.CredentialsRepository
import io.iohk.cvp.cmanager.repositories.common.DataPreparation
import io.iohk.cvp.grpc.GrpcAuthenticationHeaderParser
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.cmanager_api
import io.iohk.prism.protos.cmanager_api.CredentialsServiceGrpc
import org.mockito.MockitoSugar.mock
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class CredentialsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
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
          new CredentialsServiceImpl(credentialsRepository, authenticator),
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

}
