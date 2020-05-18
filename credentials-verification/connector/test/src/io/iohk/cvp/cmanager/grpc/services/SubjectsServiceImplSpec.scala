package io.iohk.cvp.cmanager.grpc.services

import java.time.LocalDate
import java.util.UUID

import io.circe.Json
import io.iohk.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Subject}
import io.iohk.cvp.cmanager.repositories._
import io.iohk.cvp.grpc.GrpcAuthenticationHeaderParser
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.cmanager_api
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class SubjectsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.SubjectsServiceGrpc.SubjectsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new IssuerGroupsRepository(database)
  private lazy val issuersRepository = new IssuersRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val subjectsRepository = new IssuerSubjectsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new SignedRequestsAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services =
    Seq(
      cmanager_api.SubjectsServiceGrpc
        .bindService(
          new SubjectsServiceImpl(subjectsRepository, authenticator),
          executionContext
        )
    )

  private def createGroup(issuer: Issuer.Id): IssuerGroup = {
    issuerGroupsRepository.create(issuer, IssuerGroup.Name("Group X")).value.futureValue.right.value
  }

  "createSubject" should {
    "create a subject" in {
      val issuerId = createIssuer()
      val group = createGroup(issuerId)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val json = Json
          .obj(
            "universityAssignedId" -> Json.fromString("noneyet"),
            "fullName" -> Json.fromString("Alice Beakman"),
            "email" -> Json.fromString("alice@bkm.me"),
            "admissionDate" -> Json.fromString(LocalDate.now().toString)
          )

        val request = cmanager_api
          .CreateSubjectRequest(
            groupName = group.name.value,
            jsonData = json.noSpaces
          )

        val response = serviceStub.createSubject(request).subject.value
        val subjectId = Subject.Id(UUID.fromString(response.id))
        response.groupName must be(request.groupName)
        response.jsonData must be(json.noSpaces)

        // the new subject needs to exist
        val result = subjectsRepository.find(issuerId, subjectId).value.futureValue.right.value
        result mustNot be(empty)
        val storedSubject = result.value
        storedSubject.groupName.value must be(request.groupName)
        storedSubject.data must be(json)
        storedSubject.id must be(subjectId)
      }
    }
  }

  private def createIssuer(): Issuer.Id = {
    val id = Issuer.Id(UUID.randomUUID())
    participantsRepository
      .create(
        CreateParticipantRequest(
          ParticipantId(id.value),
          ParticipantType.Issuer,
          "Issuer",
          "did:prism:test",
          ParticipantLogo(Vector())
        )
      )
      .value
      .futureValue
    issuersRepository
      .insert(IssuersRepository.IssuerCreationData(id))
      .value
      .futureValue
    id
  }

  private def toParticipantId(issuer: Issuer.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
