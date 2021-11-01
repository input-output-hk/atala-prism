package io.iohk.atala.prism.intdemo

import cats.effect.unsafe.IORuntime
import cats.syntax.functor._
import io.circe.Json.fromString
import io.circe._
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.connector.model.Connection
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.intdemo.IdServiceImpl._
import io.iohk.atala.prism.intdemo.html.IdCredential
import io.iohk.atala.prism.intdemo.protos.intdemo_api
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.utils.BytesOps
import io.iohk.atala.prism.utils.Base64Utils

import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class IdServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends intdemo_api.IDServiceGrpc.IDService {

  val service = new IntDemoService[(String, LocalDate)](
    issuerId = IdServiceImpl.issuerId,
    connectorIntegration = connectorIntegration,
    intDemoRepository = intDemoRepository,
    schedulerPeriod = schedulerPeriod,
    requiredDataLoader = getPersonalData(intDemoRepository),
    proofRequestIssuer = noProofRequests,
    getCredential = getIdCredential
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

  override def setPersonalData(
      request: intdemo_api.SetPersonalDataRequest
  ): Future[intdemo_api.SetPersonalDataResponse] = {
    if (request.dateOfBirth.isEmpty || request.firstName.isEmpty) {
      Future.failed(Status.INVALID_ARGUMENT.asException())
    } else {
      intDemoRepository
        .mergePersonalInfo(
          new TokenString(request.connectionToken),
          request.firstName,
          LocalDate.of(
            request.dateOfBirth.get.year,
            request.dateOfBirth.get.month,
            request.dateOfBirth.get.day
          )
        )
        .as(intdemo_api.SetPersonalDataResponse())
    }
  }
}

object IdServiceImpl {
  // id of Metropol City Government in connector_db/public/participants table
  val issuerId =
    ParticipantId.unsafeFrom("091d41cc-e8fc-4c44-9bd3-c938dcf76dff")
  val credentialTypeId = "VerifiableCredential/RedlandIdCredential"

  case class IdCredentialHtmlTemplateData(
      issuerName: String,
      identityNumber: String,
      dateOfBirth: String,
      fullName: String,
      expirationDate: String
  )

  @nowarn("cat=unused-params")
  def noProofRequests(connection: Connection): Future[Unit] =
    Future.unit

  private def idCredentialJsonTemplate(
      issuerName: String,
      credentialType: String,
      credentialHtml: String,
      issuerDID: String,
      issuanceKeyId: String,
      holderName: String,
      identityNumber: String,
      holderDateOfBirth: LocalDate,
      issuanceDate: LocalDate,
      expirationDate: LocalDate
  ) =
    Json.obj(
      "issuerDid" -> fromString(issuerDID),
      "issuanceKeyId" -> fromString(issuanceKeyId),
      "issuerName" -> fromString(issuerName),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "expiryDate" -> fromString(formatDate(expirationDate)),
      "credentialSubject" -> Json.obj(
        "credentialType" -> fromString(credentialType),
        "html" -> fromString(credentialHtml),
        "name" -> fromString(holderName),
        "dateOfBirth" -> fromString(formatDate(holderDateOfBirth)),
        "identityNumber" -> fromString(identityNumber)
      )
    )

  private def idCredentialHtmlTemplate(
      credentialData: IdCredentialHtmlTemplateData
  ): String = {
    IdCredential(credentialData).body
  }

  private def getPersonalData(
      intDemoRepository: IntDemoRepository
  ): TokenString => Future[Option[(String, LocalDate)]] =
    connectionToken => intDemoRepository.findPersonalInfo(connectionToken)

  private val jsonPrinter = Printer(dropNullValues = false, indent = "  ")

  def getIdCredential(
      nameAndDob: (String, LocalDate)
  ): credential_models.PlainTextCredential = {
    val (holderName, holderDateOfBirth) = nameAndDob
    val issuerName = "Metropol City Government"
    val identityNumber = generateSubjectIdNumber(
      holderName + dateFormatter.format(holderDateOfBirth)
    )
    val issuanceDate = LocalDate.now()
    val expirationDate = issuanceDate.plusYears(10)
    val issuerDID = s"did:prism:${issuerId.uuid}"
    val issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID

    val credentialHtml = idCredentialHtmlTemplate(
      IdCredentialHtmlTemplateData(
        issuerName = issuerName,
        identityNumber = identityNumber,
        dateOfBirth = dateFormatter.format(holderDateOfBirth),
        fullName = holderName,
        expirationDate = dateFormatter.format(expirationDate)
      )
    )
    val idCredentialJson =
      idCredentialJsonTemplate(
        issuerName = issuerName,
        credentialType = credentialTypeId,
        credentialHtml = credentialHtml,
        issuerDID = issuerDID,
        issuanceKeyId = issuanceKeyId,
        holderName = holderName,
        identityNumber = identityNumber,
        holderDateOfBirth = holderDateOfBirth,
        issuanceDate = issuanceDate,
        expirationDate = expirationDate
      )
    val credentialDocument = idCredentialJson.printWith(jsonPrinter)
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

  private[intdemo] def generateSubjectIdNumber(seedStr: String): String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(seedStr.getBytes("UTF-8"))
    s"RL-${BytesOps.bytesToHex(md.digest).toUpperCase.take(9)}"
  }

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private def formatDate(d: LocalDate): String = {
    dateFormatter.format(d)
  }
}
