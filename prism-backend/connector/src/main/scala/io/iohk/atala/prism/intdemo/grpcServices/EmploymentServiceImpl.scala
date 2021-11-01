package io.iohk.atala.prism.intdemo

import cats.effect.unsafe.IORuntime

import java.time.LocalDate
import io.circe.Json
import io.circe.Json.fromString
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.intdemo.EmploymentServiceImpl.{RequiredEmploymentData, getRequiredEmploymentData, issuerId, requestIdAndDegreeCredentials}
import io.iohk.atala.prism.utils.Base64Utils
import io.iohk.atala.prism.intdemo.SharedCredentials.{formatDate, jsonPrinter}
import io.iohk.atala.prism.connector.model.{Connection, TokenString}
import io.iohk.atala.prism.intdemo.html.ProofOfEmployment
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.intdemo.protos.intdemo_api
import io.iohk.atala.prism.protos.credential_models

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.credentials.json.JsonBasedCredential

import scala.util.Try

class EmploymentServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit ec: ExecutionContext, runtime: IORuntime) extends intdemo_api.EmploymentServiceGrpc.EmploymentService {

  val service = new IntDemoService[RequiredEmploymentData](
    issuerId = issuerId,
    connectorIntegration = connectorIntegration,
    intDemoRepository = intDemoRepository,
    schedulerPeriod = schedulerPeriod,
    requiredDataLoader = getRequiredEmploymentData(connectorIntegration),
    proofRequestIssuer = requestIdAndDegreeCredentials(connectorIntegration),
    getCredential = EmploymentServiceImpl.getEmploymentCredential
  )

  override def getConnectionToken(
      request: intdemo_api.GetConnectionTokenRequest
  ): Future[intdemo_api.GetConnectionTokenResponse] =
    service.getConnectionToken(request)

  override def getSubjectStatus(
      request: intdemo_api.GetSubjectStatusRequest
  ): Future[intdemo_api.GetSubjectStatusResponse] = {
    service.getSubjectStatus(request)
  }

  override def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit =
    service.getSubjectStatusStream(request, responseObserver)
}

object EmploymentServiceImpl {
  // id of Decentralized Inc. in connector_db/public/participants table
  val issuerId =
    ParticipantId.unsafeFrom("12c28b34-95be-4801-951e-c775f89d05ba")
  val credentialTypeId = "VerifiableCredential/AtalaEmploymentCredential"

  case class EmploymentCredentialHtmlTemplateData(
      issuerName: String,
      issuerAddress: String,
      fullName: String,
      employmentStatus: String,
      employmentStartDate: String
  )

  case class RequiredEmploymentData(
      idCredential: credential_models.PlainTextCredential,
      degreeCredential: credential_models.PlainTextCredential
  )

  private def requestIdAndDegreeCredentials(
      connectorIntegration: ConnectorIntegration
  )(
      connection: Connection
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- connectorIntegration.sendProofRequest(
        issuerId,
        connection.connectionId,
        credential_models.ProofRequest(
          Seq(
            IdServiceImpl.credentialTypeId,
            DegreeServiceImpl.credentialTypeId
          ),
          connection.connectionToken.token
        )
      )
    } yield ()
  }

  private def getRequiredEmploymentData(
      connectorIntegration: ConnectorIntegration
  )(implicit
      ec: ExecutionContext
  ): TokenString => Future[Option[RequiredEmploymentData]] = { connectionToken =>
    getSharedCredentials(connectorIntegration)(ec)(connectionToken)
      .map(toRequiredEmploymentData)
  }

  private def toRequiredEmploymentData(
      seq: Seq[credential_models.PlainTextCredential]
  ): Option[RequiredEmploymentData] = {
    for {
      idCredential <- seq.find(credential =>
        SharedCredentials.getTypeId(
          credential
        ) == IdServiceImpl.credentialTypeId
      )
      degreeCredential <-
        seq.find(credential =>
          SharedCredentials.getTypeId(
            credential
          ) == DegreeServiceImpl.credentialTypeId
        )
    } yield RequiredEmploymentData(idCredential, degreeCredential)
  }

  private def getSharedCredentials(
      connectorIntegration: ConnectorIntegration
  )(implicit
      ec: ExecutionContext
  ): TokenString => Future[Seq[credential_models.PlainTextCredential]] = { connectionToken =>
    SharedCredentials
      .getSharedCredentials(connectorIntegration, connectionToken, issuerId)(
        Set(
          IdServiceImpl.credentialTypeId,
          DegreeServiceImpl.credentialTypeId
        )
      )
  }

  def getEmploymentCredential(
      requiredEmploymentData: RequiredEmploymentData
  ): credential_models.PlainTextCredential = {

    val idCredentialData = IdCredentialData(requiredEmploymentData.idCredential)
    val issuerName = "Decentralized Inc."
    val issuerAddress = "67 Clasper Way, Herefoot, HF1 0AF"
    val employmentStatus = "Full-time"
    val employmentStartDate = LocalDate.now().minusMonths(1)
    val issuanceDate = LocalDate.now()
    val issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID

    val credentialHtml = employmentCredentialHtmlTemplate(
      EmploymentCredentialHtmlTemplateData(
        issuerName = issuerName,
        issuerAddress = issuerAddress,
        fullName = idCredentialData.name,
        employmentStatus = employmentStatus,
        employmentStartDate = formatDate(employmentStartDate)
      )
    )

    val employmentCredentialJson = employmentCredentialJsonTemplate(
      issuerDID = s"did:prism:${issuerId.uuid}",
      issuanceDate = issuanceDate,
      issuerName = issuerName,
      issuerAddress = issuerAddress,
      issuanceKeyId = issuanceKeyId,
      credentialHtml = credentialHtml,
      holderName = idCredentialData.name,
      credentialType = credentialTypeId,
      employmentStartDate = employmentStartDate,
      employmentStatus = employmentStatus
    )

    val credentialDocument = employmentCredentialJson.printWith(jsonPrinter)
    val credential = Try(
      JsonBasedCredential.fromString(credentialDocument)
    ).toEither

    credential match {
      case Left(_) =>
        throw new IllegalStateException(
          s"Error creating credential from string, the document follows: ${credentialDocument}"
        )
      case Right(credential) =>
        credential_models.PlainTextCredential(
          encodedCredential = Base64Utils.encodeURL(credential.getCanonicalForm.getBytes)
        )
    }
  }

  private def employmentCredentialJsonTemplate(
      issuerDID: String,
      issuanceDate: LocalDate,
      issuerName: String,
      issuerAddress: String,
      issuanceKeyId: String,
      credentialHtml: String,
      holderName: String,
      credentialType: String,
      employmentStartDate: LocalDate,
      employmentStatus: String
  ) =
    Json.obj(
      "issuerDid" -> fromString(issuerDID),
      "issuanceKeyId" -> fromString(issuanceKeyId),
      "issuerName" -> fromString(issuerName),
      "issuerAddress" -> fromString(issuerAddress),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "employmentStartDate" -> fromString(formatDate(employmentStartDate)),
      "employmentStatus" -> fromString(employmentStatus),
      "credentialSubject" -> Json.obj(
        "credentialType" -> fromString(credentialType),
        "html" -> fromString(credentialHtml),
        "name" -> fromString(holderName)
      )
    )

  private def employmentCredentialHtmlTemplate(
      credentialData: EmploymentCredentialHtmlTemplateData
  ): String = {
    ProofOfEmployment(credentialData).body
  }
}
