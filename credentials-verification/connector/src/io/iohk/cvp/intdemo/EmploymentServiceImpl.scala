package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.circe.Json
import io.circe.Json.{arr, fromString, obj}
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, TokenString}
import io.iohk.cvp.intdemo.EmploymentServiceImpl.{
  RequiredEmploymentData,
  getRequiredEmploymentData,
  issuerId,
  requestIdAndDegreeCredentials
}
import io.iohk.cvp.intdemo.SharedCredentials.{formatDate, jsonPrinter}
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.intdemo.protos.intdemo_api
import io.iohk.prism.protos.credential_models
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class EmploymentServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit
    ec: ExecutionContext
) extends intdemo_api.EmploymentServiceGrpc.EmploymentService {

  val service = new IntDemoService[RequiredEmploymentData](
    issuerId = issuerId,
    connectorIntegration = connectorIntegration,
    intDemoRepository = intDemoRepository,
    schedulerPeriod = schedulerPeriod,
    requiredDataLoader = getRequiredEmploymentData(connectorIntegration),
    proofRequestIssuer = requestIdAndDegreeCredentials(connectorIntegration),
    getCredential = EmploymentServiceImpl.getEmploymentCredential,
    scheduler = scheduler
  )

  override def getConnectionToken(
      request: intdemo_api.GetConnectionTokenRequest
  ): Future[intdemo_api.GetConnectionTokenResponse] =
    service.getConnectionToken(request)

  override def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit =
    service.getSubjectStatusStream(request, responseObserver)
}

object EmploymentServiceImpl {
  private val issuerId = ParticipantId("12c28b34-95be-4801-951e-c775f89d05ba")
  val credentialTypeId = "VerifiableCredential/AtalaEmploymentCredential"

  case class RequiredEmploymentData(
      idCredential: credential_models.Credential,
      degreeCredential: credential_models.Credential
  )

  private def requestIdAndDegreeCredentials(connectorIntegration: ConnectorIntegration)(
      connection: Connection
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- connectorIntegration.sendProofRequest(
        issuerId,
        connection.connectionId,
        credential_models.ProofRequest(
          Seq(IdServiceImpl.credentialTypeId, DegreeServiceImpl.credentialTypeId),
          connection.connectionToken.token
        )
      )
    } yield ()
  }

  private def getRequiredEmploymentData(connectorIntegration: ConnectorIntegration)(implicit
      ec: ExecutionContext
  ): TokenString => Future[Option[RequiredEmploymentData]] = { connectionToken =>
    getSharedCredentials(connectorIntegration)(ec)(connectionToken).map(toRequiredEmploymentData)
  }

  private def toRequiredEmploymentData(seq: Seq[credential_models.Credential]): Option[RequiredEmploymentData] = {
    for {
      idCredential <- seq.find(credential => credential.typeId == IdServiceImpl.credentialTypeId)
      degreeCredential <- seq.find(credential => credential.typeId == DegreeServiceImpl.credentialTypeId)
    } yield RequiredEmploymentData(idCredential, degreeCredential)
  }

  private def getSharedCredentials(
      connectorIntegration: ConnectorIntegration
  )(implicit ec: ExecutionContext): TokenString => Future[Seq[credential_models.Credential]] = { connectionToken =>
    SharedCredentials
      .getSharedCredentials(connectorIntegration, connectionToken, issuerId)(
        Set(IdServiceImpl.credentialTypeId, DegreeServiceImpl.credentialTypeId)
      )
  }

  def getEmploymentCredential(requiredEmploymentData: RequiredEmploymentData): credential_models.Credential = {

    val idData = IdData.toIdData(requiredEmploymentData.idCredential)

    val employmentCredential = employmentCredentialJsonTemplate(
      id = "unknown",
      issuanceDate = LocalDate.now(),
      subjectFullName = idData.name,
      subjectDid = "unknown",
      employmentStartDate = LocalDate.now().minusMonths(1),
      employmentStatus = "Full time"
    ).printWith(jsonPrinter)

    credential_models.Credential(
      typeId = EmploymentServiceImpl.credentialTypeId,
      credentialDocument = employmentCredential
    )
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
