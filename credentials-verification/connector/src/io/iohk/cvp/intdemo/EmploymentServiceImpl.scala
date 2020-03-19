package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.grpc.stub.StreamObserver
import io.iohk.cvp.intdemo.protos.EmploymentServiceGrpc.EmploymentService
import io.iohk.cvp.intdemo.protos.{
  GetConnectionTokenRequest,
  GetConnectionTokenResponse,
  GetSubjectStatusRequest,
  GetSubjectStatusResponse
}
import credential.Credential
import io.circe.Json
import io.circe.Json.{arr, fromString, obj}
import io.iohk.connector.model.TokenString
import io.iohk.cvp.intdemo.EmploymentServiceImpl.{RequiredEmploymentData, getRequiredEmploymentData, issuerId}
import io.iohk.cvp.intdemo.SharedCredentials.{formatDate, jsonPrinter}
import io.iohk.cvp.models.ParticipantId

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import monix.execution.Scheduler.{global => scheduler}

class EmploymentServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(
    implicit ec: ExecutionContext
) extends EmploymentService {

  val service = new IntDemoService[RequiredEmploymentData](
    issuerId,
    connectorIntegration,
    intDemoRepository,
    schedulerPeriod,
    getRequiredEmploymentData(connectorIntegration),
    EmploymentServiceImpl.getEmploymentCredential,
    scheduler
  )

  override def getConnectionToken(request: GetConnectionTokenRequest): Future[GetConnectionTokenResponse] =
    service.getConnectionToken(request)

  override def getSubjectStatusStream(
      request: GetSubjectStatusRequest,
      responseObserver: StreamObserver[GetSubjectStatusResponse]
  ): Unit =
    service.getSubjectStatusStream(request, responseObserver)
}

object EmploymentServiceImpl {
  private val issuerId = ParticipantId("12c28b34-95be-4801-951e-c775f89d05ba")
  val credentialTypeId = "VerifiableCredential/AtalaEmploymentCredential"

  case class RequiredEmploymentData(idCredential: Credential, degreeCredential: Credential)

  private def getRequiredEmploymentData(connectorIntegration: ConnectorIntegration)(
      implicit ec: ExecutionContext
  ): TokenString => Future[Option[RequiredEmploymentData]] = { connectionToken =>
    getSharedCredentials(connectorIntegration)(ec)(connectionToken).map(toRequiredEmploymentData)
  }

  private def toRequiredEmploymentData(seq: Seq[Credential]): Option[RequiredEmploymentData] = {
    for {
      idCredential <- seq.find(credential => credential.typeId == IdServiceImpl.credentialTypeId)
      degreeCredential <- seq.find(credential => credential.typeId == DegreeServiceImpl.credentialTypeId)
    } yield RequiredEmploymentData(idCredential, degreeCredential)
  }

  private def getSharedCredentials(
      connectorIntegration: ConnectorIntegration
  )(implicit ec: ExecutionContext): TokenString => Future[Seq[Credential]] = { connectionToken =>
    SharedCredentials
      .getSharedCredentials(connectorIntegration, connectionToken, issuerId)(
        Set(IdServiceImpl.credentialTypeId, DegreeServiceImpl.credentialTypeId)
      )
  }

  def getEmploymentCredential(requiredEmploymentData: RequiredEmploymentData): Credential = {

    val idData = IdData.toIdData(requiredEmploymentData.idCredential)

    val employmentCredential = employmentCredentialJsonTemplate(
      id = "unknown",
      issuanceDate = LocalDate.now(),
      subjectFullName = idData.name,
      subjectDid = "unknown",
      employmentStartDate = LocalDate.now().minusMonths(1),
      employmentStatus = "Full time"
    ).printWith(jsonPrinter)

    Credential(typeId = EmploymentServiceImpl.credentialTypeId, credentialDocument = employmentCredential)
  }

  def employmentCredentialJsonTemplate(
      id: String,
      issuanceDate: LocalDate,
      subjectFullName: String,
      subjectDid: String,
      employmentStartDate: LocalDate,
      employmentStatus: String
  ): Json = {
    obj(
      "id" -> fromString(id),
      "type" -> arr(fromString("VerifiableCredential"), fromString("AtalaEmploymentCredential")),
      "issuer" -> obj(
        "id" -> fromString("did:atala:12c28b34-95be-4801-951e-c775f89d05ba"),
        "name" -> fromString("Atala Inc."),
        "address" -> fromString("67 Clasper Way, Herefoot, HF1 0AF")
      ),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "employmentStartDate" -> fromString(formatDate(employmentStartDate)),
      "employmentStatus" -> fromString(employmentStatus),
      "credentialSubject" -> obj(
        "id" -> fromString(subjectDid),
        "name" -> fromString(subjectFullName)
      )
    )
  }

}
