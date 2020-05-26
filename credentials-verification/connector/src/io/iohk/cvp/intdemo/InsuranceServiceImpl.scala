package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.circe.Json
import io.circe.Json.{arr, fromString, obj}
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, TokenString}
import io.iohk.cvp.intdemo.InsuranceServiceImpl.{
  RequiredInsuranceData,
  getInsuranceCredential,
  getRequiredInsuranceData,
  requestIdAndEmploymentCredentials
}
import io.iohk.cvp.intdemo.SharedCredentials.{formatDate, jsonPrinter}
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.intdemo.protos.intdemo_api
import io.iohk.prism.protos.credential_models
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

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

  override def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit =
    service.getSubjectStatusStream(request, responseObserver)
}

object InsuranceServiceImpl {
  private val issuerId = ParticipantId("a1cb7eee-65c1-4d7f-9417-db8a37a6212a")
  private val credentialTypeId = "VerifiableCredential/AtalaCertificateOfInsurance"

  case class RequiredInsuranceData(
      idCredential: credential_models.Credential,
      employmentCredential: credential_models.Credential
  )

  private def requestIdAndEmploymentCredentials(connectorIntegration: ConnectorIntegration)(
      connection: Connection
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- connectorIntegration.sendProofRequest(
        issuerId,
        connection.connectionId,
        credential_models.ProofRequest(
          Seq(IdServiceImpl.credentialTypeId, EmploymentServiceImpl.credentialTypeId),
          connection.connectionToken.token
        )
      )
    } yield ()
  }

  private def getRequiredInsuranceData(connectorIntegration: ConnectorIntegration)(implicit
      ec: ExecutionContext
  ): TokenString => Future[Option[RequiredInsuranceData]] = { connectionToken =>
    getSharedCredentials(connectorIntegration)(ec)(connectionToken).map(toRequiredInsuranceData)
  }

  private def toRequiredInsuranceData(seq: Seq[credential_models.Credential]): Option[RequiredInsuranceData] = {
    for {
      idCredential <- seq.find(credential => credential.typeId == IdServiceImpl.credentialTypeId)
      employmentCredential <- seq.find(credential => credential.typeId == EmploymentServiceImpl.credentialTypeId)
    } yield RequiredInsuranceData(idCredential, employmentCredential)
  }

  private def getSharedCredentials(
      connectorIntegration: ConnectorIntegration
  )(implicit ec: ExecutionContext): TokenString => Future[Seq[credential_models.Credential]] = { connectionToken =>
    SharedCredentials
      .getSharedCredentials(connectorIntegration, connectionToken, issuerId)(
        Set(IdServiceImpl.credentialTypeId, EmploymentServiceImpl.credentialTypeId)
      )
  }

  private def getInsuranceCredential(requiredInsuranceData: RequiredInsuranceData): credential_models.Credential = {

    val idData = IdData.toIdData(requiredInsuranceData.idCredential)

    val employmentData = EmploymentData.toEmploymentData(requiredInsuranceData.employmentCredential)

    val insuranceCredential = insuranceCredentialJsonTemplate(
      id = "unknown",
      issuanceDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusYears(1),
      subjectFullName = idData.name,
      subjectDid = "unknown",
      policyNumber = "ABC-123456789",
      productClass = "Health Insurance",
      subjectDateOfBirth = idData.dob,
      employerName = employmentData.employerName,
      employerAddress = employmentData.employerAddress
    ).printWith(jsonPrinter)

    credential_models.Credential(typeId = credentialTypeId, credentialDocument = insuranceCredential)
  }

  def insuranceCredentialJsonTemplate(
      id: String,
      issuanceDate: LocalDate,
      expiryDate: LocalDate,
      policyNumber: String,
      productClass: String,
      subjectFullName: String,
      subjectDid: String,
      subjectDateOfBirth: LocalDate,
      employerName: String,
      employerAddress: String
  ): Json = {
    obj(
      "id" -> fromString(id),
      "type" -> arr(fromString("VerifiableCredential"), fromString("AtalaCertificateOfInsurance")),
      "issuer" -> obj(
        "id" -> fromString("did:atala:a1cb7eee-65c1-4d7f-9417-db8a37a6212a"),
        "name" -> fromString("Verified Insurance Ltd.")
      ),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "expiryDate" -> fromString(formatDate(expiryDate)),
      "policyNumber" -> fromString(policyNumber),
      "productClass" -> fromString(productClass),
      "credentialSubject" -> obj(
        "id" -> fromString(subjectDid),
        "name" -> fromString(subjectFullName),
        "dateOfBirth" -> fromString(formatDate(subjectDateOfBirth)),
        "currentEmployer" -> obj(
          "name" -> fromString(employerName),
          "address" -> fromString(employerAddress)
        )
      )
    )
  }
}
