package io.iohk.atala.prism.intdemo

import cats.effect.unsafe.IORuntime
import cats.syntax.functor._
import io.circe.Json.fromString
import io.circe._
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.connector.model.Connection
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.intdemo.DegreeServiceImpl.getDegreeCredential
import io.iohk.atala.prism.intdemo.DegreeServiceImpl.getSharedIdCredential
import io.iohk.atala.prism.intdemo.DegreeServiceImpl.issuerId
import io.iohk.atala.prism.intdemo.DegreeServiceImpl.requestIdCredential
import io.iohk.atala.prism.intdemo.SharedCredentials.formatDate
import io.iohk.atala.prism.intdemo.SharedCredentials.getSharedCredentials
import io.iohk.atala.prism.intdemo.SharedCredentials.jsonPrinter
import io.iohk.atala.prism.intdemo.html.UniversityDegree
import io.iohk.atala.prism.intdemo.protos.intdemo_api
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.utils.Base64Utils

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class DegreeServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends intdemo_api.DegreeServiceGrpc.DegreeService {

  val service = new IntDemoService[credential_models.PlainTextCredential](
    issuerId = issuerId,
    connectorIntegration = connectorIntegration,
    intDemoRepository = intDemoRepository,
    schedulerPeriod = schedulerPeriod,
    requiredDataLoader = getSharedIdCredential(connectorIntegration),
    proofRequestIssuer = requestIdCredential(connectorIntegration),
    getCredential = getDegreeCredential
  )

  override def getConnectionToken(
      request: intdemo_api.GetConnectionTokenRequest
  ): Future[intdemo_api.GetConnectionTokenResponse] = {
    service.getConnectionToken(request)
  }

  override def getSubjectStatus(
      request: intdemo_api.GetSubjectStatusRequest
  ): Future[intdemo_api.GetSubjectStatusResponse] = {
    service.getSubjectStatus(request)
  }

  override def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit = {
    service.getSubjectStatusStream(request, responseObserver)
  }
}

object DegreeServiceImpl {

  // id of University of Innovation and Technology in connector_db/public/participants table
  val issuerId =
    ParticipantId.unsafeFrom("6c170e91-92b0-4265-909d-951c11f30caa")
  val credentialTypeId = "VerifiableCredential/AirsideDegreeCredential"

  case class DegreeCredentialHtmlTemplateData(
      degreeAwarded: String,
      issuerName: String,
      degreeResult: String,
      fullName: String,
      startDate: String,
      graduationDate: String
  )

  private def requestIdCredential(connectorIntegration: ConnectorIntegration)(
      connection: Connection
  )(implicit ec: ExecutionContext): Future[Unit] = {
    connectorIntegration
      .sendProofRequest(
        issuerId,
        connection.connectionId,
        credential_models.ProofRequest(
          Seq(IdServiceImpl.credentialTypeId),
          connection.connectionToken.token
        )
      )
      .as(())
  }

  private def getSharedIdCredential(connectorIntegration: ConnectorIntegration)(implicit
      ec: ExecutionContext
  ): TokenString => Future[Option[credential_models.PlainTextCredential]] =
    connectionToken =>
      getSharedCredentials(connectorIntegration, connectionToken, issuerId)(
        Set(IdServiceImpl.credentialTypeId)
      )
        .map(_.headOption)

  def getDegreeCredential(
      idCredential: credential_models.PlainTextCredential
  ): credential_models.PlainTextCredential = {
    val idCredentialData = IdCredentialData(idCredential)
    val degreeAwarded = "Bachelor of Science"
    val degreeResult = "First-class honors"
    val graduationDate = idCredentialData.dateOfBirth.plusYears(20)
    val startDate = LocalDate.now().minusYears(4)
    val issuerName = "University of Innovation and Technology"

    val credentialHtml = degreeCredentialHtmlTemplate(
      DegreeCredentialHtmlTemplateData(
        degreeAwarded = degreeAwarded,
        issuerName = issuerName,
        degreeResult = degreeResult,
        fullName = idCredentialData.name,
        startDate = formatDate(startDate),
        graduationDate = formatDate(graduationDate)
      )
    )

    val degreeCredentialJson = degreeCredentialJsonTemplate(
      degreeAwarded = degreeAwarded,
      degreeResult = degreeResult,
      startDate = startDate,
      issuerName = issuerName,
      issuerDID = s"did:prism:${issuerId.uuid}",
      issuanceDate = LocalDate.now(),
      holderName = idCredentialData.name,
      graduationDate = graduationDate,
      issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID,
      credentialType = credentialTypeId,
      holderDateOfBirth = idCredentialData.dateOfBirth,
      credentialHtml = credentialHtml
    )

    val credentialDocument = degreeCredentialJson.printWith(jsonPrinter)
    val credential = Try(
      new JsonBasedCredential(CredentialContent.fromString(credentialDocument), null)
    ).toEither

    credential match {
      case Left(_) =>
        throw new IllegalStateException(
          s"Error creating credential from string, the document follows: ${credentialDocument}"
        )
      case Right(credential) =>
        println("sending degree credential")
        credential_models.PlainTextCredential(
          encodedCredential = Base64Utils.encodeURL(credential.getCanonicalForm.getBytes)
        )
    }
  }

  private def degreeCredentialJsonTemplate(
      degreeAwarded: String,
      degreeResult: String,
      startDate: LocalDate,
      issuerName: String,
      graduationDate: LocalDate,
      credentialType: String,
      credentialHtml: String,
      issuerDID: String,
      issuanceKeyId: String,
      holderName: String,
      holderDateOfBirth: LocalDate,
      issuanceDate: LocalDate
  ) =
    Json.obj(
      "issuerDid" -> fromString(issuerDID),
      "issuerName" -> fromString(issuerName),
      "issuanceKeyId" -> fromString(issuanceKeyId),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "credentialSubject" -> Json.obj(
        "credentialType" -> fromString(credentialType),
        "html" -> fromString(credentialHtml),
        "name" -> fromString(holderName),
        "dateOfBirth" -> fromString(formatDate(holderDateOfBirth)),
        "graduationDate" -> fromString(formatDate(graduationDate)),
        "startDate" -> fromString(formatDate(startDate)),
        "degreeAwarded" -> fromString(degreeAwarded),
        "degreeResult" -> fromString(degreeResult)
      )
    )

  private def degreeCredentialHtmlTemplate(
      credentialData: DegreeCredentialHtmlTemplateData
  ): String = {
    UniversityDegree(credentialData).body
  }
}
