package io.iohk.atala.prism.intdemo

import io.circe.Json
import io.circe.Json.fromString
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.connector.model.Connection
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.intdemo.InsuranceServiceImpl.RequiredInsuranceData
import io.iohk.atala.prism.intdemo.InsuranceServiceImpl.getInsuranceCredential
import io.iohk.atala.prism.intdemo.InsuranceServiceImpl.getRequiredInsuranceData
import io.iohk.atala.prism.intdemo.InsuranceServiceImpl.requestIdAndEmploymentCredentials
import io.iohk.atala.prism.intdemo.SharedCredentials.formatDate
import io.iohk.atala.prism.intdemo.SharedCredentials.jsonPrinter
import io.iohk.atala.prism.intdemo.html.HealthCredential
import io.iohk.atala.prism.intdemo.protos.intdemo_api
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.utils.Base64Utils
import monix.execution.Scheduler.{global => scheduler}

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class InsuranceServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit
    ec: ExecutionContext
) extends intdemo_api.InsuranceServiceGrpc.InsuranceService {

  val service = new IntDemoService[RequiredInsuranceData](
    InsuranceServiceImpl.issuerId,
    connectorIntegration,
    intDemoRepository,
    schedulerPeriod,
    getRequiredInsuranceData(connectorIntegration),
    requestIdAndEmploymentCredentials(connectorIntegration),
    getInsuranceCredential,
    scheduler
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

object InsuranceServiceImpl {
  // id of Verified Insurance Ltd connector_db/public/participants table
  val issuerId =
    ParticipantId.unsafeFrom("a1cb7eee-65c1-4d7f-9417-db8a37a6212a")
  val credentialTypeId = "VerifiableCredential/AtalaCertificateOfInsurance"

  case class RequiredInsuranceData(
      idCredential: credential_models.PlainTextCredential,
      employmentCredential: credential_models.PlainTextCredential
  )

  case class InsuranceCredentialHtmlTemplateData(
      issuerName: String,
      productClass: String,
      policyNumber: String,
      fullName: String,
      expirationDate: String
  )

  private def requestIdAndEmploymentCredentials(
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
            EmploymentServiceImpl.credentialTypeId
          ),
          connection.connectionToken.token
        )
      )
    } yield ()
  }

  private def getRequiredInsuranceData(
      connectorIntegration: ConnectorIntegration
  )(implicit
      ec: ExecutionContext
  ): TokenString => Future[Option[RequiredInsuranceData]] = { connectionToken =>
    getSharedCredentials(connectorIntegration)(ec)(connectionToken)
      .map(toRequiredInsuranceData)
  }

  private def toRequiredInsuranceData(
      seq: Seq[credential_models.PlainTextCredential]
  ): Option[RequiredInsuranceData] = {
    for {
      idCredential <- seq.find(credential =>
        SharedCredentials.getTypeId(
          credential
        ) == IdServiceImpl.credentialTypeId
      )
      employmentCredential <-
        seq.find(credential =>
          SharedCredentials.getTypeId(
            credential
          ) == EmploymentServiceImpl.credentialTypeId
        )
    } yield RequiredInsuranceData(idCredential, employmentCredential)
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
          EmploymentServiceImpl.credentialTypeId
        )
      )
  }

  def getInsuranceCredential(
      requiredInsuranceData: RequiredInsuranceData
  ): credential_models.PlainTextCredential = {

    val idCredentialData = IdCredentialData(requiredInsuranceData.idCredential)
    val employmentCredentialData = EmploymentCredentialData(
      requiredInsuranceData.employmentCredential
    )
    val policyNumber = "ABC-123456789"
    val productClass = "Health Insurance"
    val issuerName = "Verified Insurance Ltd"
    val issuanceDate = LocalDate.now()
    val expirationDate = LocalDate.now().plusYears(1)

    val credentialHtml = insuranceCredentialHtmlTemplate(
      InsuranceCredentialHtmlTemplateData(
        issuerName = issuerName,
        productClass = productClass,
        policyNumber = policyNumber,
        fullName = idCredentialData.name,
        expirationDate = formatDate(expirationDate)
      )
    )

    val insuranceCredentialJson = insuranceCredentialJsonTemplate(
      credentialType = credentialTypeId,
      credentialHtml = credentialHtml,
      issuerName = issuerName,
      issuerDID = s"did:prism:${issuerId.uuid}",
      issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID,
      issuanceDate = issuanceDate,
      expirationDate = expirationDate,
      policyNumber = policyNumber,
      productClass = productClass,
      holderName = idCredentialData.name,
      holderDateOfBirth = idCredentialData.dateOfBirth,
      currentEmployerName = employmentCredentialData.employerName,
      currentEmployerAddress = employmentCredentialData.employerAddress
    )

    val credentialDocument = insuranceCredentialJson.printWith(jsonPrinter)

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

  private def insuranceCredentialJsonTemplate(
      credentialType: String,
      credentialHtml: String,
      issuerName: String,
      issuerDID: String,
      issuanceKeyId: String,
      issuanceDate: LocalDate,
      expirationDate: LocalDate,
      policyNumber: String,
      productClass: String,
      holderName: String,
      holderDateOfBirth: LocalDate,
      currentEmployerName: String,
      currentEmployerAddress: String
  ) =
    Json.obj(
      "issuerDid" -> fromString(issuerDID),
      "issuerName" -> fromString(issuerName),
      "issuanceKeyId" -> fromString(issuanceKeyId),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "expiryDate" -> fromString(formatDate(expirationDate)),
      "policyNumber" -> fromString(policyNumber),
      "productClass" -> fromString(productClass),
      "credentialSubject" -> Json.obj(
        "credentialType" -> fromString(credentialType),
        "html" -> fromString(credentialHtml),
        "name" -> fromString(holderName),
        "dateOfBirth" -> fromString(formatDate(holderDateOfBirth)),
        "currentEmployer" -> Json.obj(
          "name" -> fromString(currentEmployerName),
          "address" -> fromString(currentEmployerAddress)
        )
      )
    )

  private def insuranceCredentialHtmlTemplate(
      credentialData: InsuranceCredentialHtmlTemplateData
  ): String = {
    HealthCredential(credentialData).body
  }
}
