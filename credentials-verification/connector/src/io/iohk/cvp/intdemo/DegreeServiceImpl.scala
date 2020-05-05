package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.circe.Json.fromString
import io.circe._
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, TokenString}
import io.iohk.cvp.intdemo.DegreeServiceImpl.{getDegreeCredential, getSharedIdCredential, issuerId, requestIdCredential}
import io.iohk.cvp.intdemo.SharedCredentials.{formatDate, getSharedCredentials, jsonPrinter}
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.intdemo.protos.intdemo_api
import io.iohk.prism.protos.credential_models
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class DegreeServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit
    ec: ExecutionContext
) extends intdemo_api.DegreeServiceGrpc.DegreeService {

  val service = new IntDemoService[credential_models.Credential](
    issuerId = issuerId,
    connectorIntegration = connectorIntegration,
    intDemoRepository = intDemoRepository,
    schedulerPeriod = schedulerPeriod,
    requiredDataLoader = getSharedIdCredential(connectorIntegration),
    proofRequestIssuer = requestIdCredential(connectorIntegration),
    getCredential = getDegreeCredential,
    scheduler = scheduler
  )

  override def getConnectionToken(
      request: intdemo_api.GetConnectionTokenRequest
  ): Future[intdemo_api.GetConnectionTokenResponse] = {
    service.getConnectionToken(request)
  }

  override def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit = {
    service.getSubjectStatusStream(request, responseObserver)
  }
}

object DegreeServiceImpl {
  val issuerId = ParticipantId("6c170e91-92b0-4265-909d-951c11f30caa")

  val credentialTypeId = "VerifiableCredential/AirsideDegreeCredential"

  case class DegreeData(idData: IdData) {
    val degreeAwarded = "Bachelor of Science"
    val degreeResult = "Upper second class honours"
    val graduationYear = idData.dob.plusYears(20).getYear
  }

  private def requestIdCredential(connectorIntegration: ConnectorIntegration)(
      connection: Connection
  )(implicit ec: ExecutionContext): Future[Unit] = {
    connectorIntegration
      .sendProofRequest(
        issuerId,
        connection.connectionId,
        credential_models.ProofRequest(Seq(IdServiceImpl.credentialTypeId), connection.connectionToken.token)
      )
      .map(_ => ())
  }

  private def getSharedIdCredential(connectorIntegration: ConnectorIntegration)(implicit
      ec: ExecutionContext
  ): TokenString => Future[Option[credential_models.Credential]] =
    connectionToken =>
      getSharedCredentials(connectorIntegration, connectionToken, issuerId)(Set(IdServiceImpl.credentialTypeId))
        .map(_.headOption)

  def getDegreeCredential(idCredential: credential_models.Credential): credential_models.Credential = {
    val idData = IdData.toIdData(idCredential)

    val degreeData = DegreeData(idData)

    val degreeCredential = degreeCredentialJsonTemplate(
      id = "unknown",
      issuanceDate = LocalDate.now(),
      subjectFullName = idData.name,
      degreeAwarded = degreeData.degreeAwarded,
      degreeResult = degreeData.degreeResult,
      graduationYear = degreeData.graduationYear,
      subjectDid = "unknown"
    ).printWith(jsonPrinter)

    credential_models.Credential(
      typeId = credentialTypeId,
      credentialDocument = degreeCredential
    )
  }

  def degreeCredentialJsonTemplate(
      id: String,
      issuanceDate: LocalDate,
      subjectFullName: String,
      degreeAwarded: String,
      degreeResult: String,
      graduationYear: Int,
      subjectDid: String
  ): Json = {
    Json.obj(
      "id" -> fromString(id),
      "type" -> Json.arr(fromString("VerifiableCredential"), fromString("AirsideDegreeCredential")),
      "issuer" -> Json.obj(
        fields =
          "id" -> fromString("did:atala:6c170e91-92b0-4265-909d-951c11f30caa"),
        "name" -> fromString("University of Innovation and Technology")
      ),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "credentialSubject" -> Json.obj(
        "id" -> fromString(subjectDid),
        "name" -> fromString(subjectFullName),
        "degreeAwarded" -> fromString(degreeAwarded),
        "degreeResult" -> fromString(degreeResult),
        "graduationYear" -> fromString(String.valueOf(graduationYear))
      )
    )
  }
}
