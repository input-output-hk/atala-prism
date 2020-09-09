package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup, Student}
import io.iohk.atala.prism.cmanager.repositories.{
  CredentialsRepository,
  IssuerGroupsRepository,
  IssuersRepository,
  StudentsRepository
}
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.{cmanager_api, common_models}
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class StudentsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.StudentsServiceGrpc.StudentsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new IssuerGroupsRepository(database)
  private lazy val issuersRepository = new IssuersRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val studentsRepository = new StudentsRepository(database)
  private lazy val credentialsRepository = new CredentialsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator =
    new SignedRequestsAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )
  override def services =
    Seq(
      cmanager_api.StudentsServiceGrpc
        .bindService(
          new StudentsServiceImpl(studentsRepository, credentialsRepository, authenticator),
          executionContext
        )
    )

  private def createGroup(issuer: Issuer.Id): IssuerGroup = {
    issuerGroupsRepository.create(issuer, IssuerGroup.Name("Group X")).value.futureValue.right.value
  }

  "createStudent" should {
    "create a student" in {
      val issuerId = createIssuer()
      val group = createGroup(issuerId)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = cmanager_api
          .CreateStudentRequest(
            universityAssignedId = "noneyet",
            fullName = "Alice Beakman",
            email = "alice@bkm.me",
            groupName = group.name.value
          )
          .withAdmissionDate(common_models.Date().withDay(1).withMonth(10).withYear(2000))

        val response = serviceStub.createStudent(request).student.value
        val studentId = Student.Id(UUID.fromString(response.id))
        response.universityAssignedId must be(request.universityAssignedId)
        response.fullName must be(request.fullName)
        response.email must be(request.email)
        response.groupName must be(request.groupName)
        response.admissionDate must be(request.admissionDate)

        // the new student needs to exist
        val result = studentsRepository.find(issuerId, studentId).value.futureValue.right.value
        result mustNot be(empty)
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
