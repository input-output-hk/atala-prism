package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.connector.repositories.{ConnectionsRepository, RequestNoncesRepository}
import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos.StudentsServiceGrpc
import io.iohk.cvp.cmanager.repositories.{
  CredentialsRepository,
  IssuerGroupsRepository,
  IssuersRepository,
  StudentsRepository
}
import io.iohk.cvp.grpc.GrpcAuthenticationHeaderParser
import io.iohk.cvp.models.ParticipantId
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class StudentsServiceImplSpec extends RpcSpecBase {

  override val tables = List("credentials", "students", "issuer_groups", "issuers", "connections")

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new StudentsServiceGrpc.StudentsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new IssuerGroupsRepository(database)
  private lazy val issuersRepository = new IssuersRepository(database)
  private lazy val studentsRepository = new StudentsRepository(database)
  private lazy val credentialsRepository = new CredentialsRepository(database)
  private lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)(executionContext)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator =
    new SignedRequestsAuthenticator(
      connectionsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )
  override def services = Seq(
    StudentsServiceGrpc
      .bindService(new StudentsServiceImpl(studentsRepository, credentialsRepository, authenticator), executionContext)
  )

  private def createGroup(issuer: Issuer.Id): IssuerGroup = {
    issuerGroupsRepository.create(issuer, IssuerGroup.Name("Group X")).value.futureValue.right.value
  }

  "createStudent" should {
    "create a student" in {
      val issuerId = createIssuer()
      val group = createGroup(issuerId)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = protos
          .CreateStudentRequest(
            universityAssignedId = "noneyet",
            fullName = "Alice Beakman",
            email = "alice@bkm.me",
            groupName = group.name.value
          )
          .withAdmissionDate(protos.Date().withDay(1).withMonth(10).withYear(2000))

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
    issuersRepository
      .insert(IssuersRepository.IssuerCreationData(Issuer.Name("IOHK"), "did:prism:issuer1", None))
      .value
      .futureValue
      .right
      .value
  }

  private def toParticipantId(issuer: Issuer.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
