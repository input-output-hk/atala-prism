package io.iohk.cvp.intdemo

import java.time.LocalDate

import credential.{Credential, ProofRequest}
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
import io.iohk.cvp.intdemo.protos.InsuranceServiceGrpc.InsuranceService
import io.iohk.cvp.intdemo.protos.{
  GetConnectionTokenRequest,
  GetConnectionTokenResponse,
  GetSubjectStatusRequest,
  GetSubjectStatusResponse
}
import io.iohk.cvp.models.ParticipantId
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class InsuranceServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(
    implicit ec: ExecutionContext
) extends InsuranceService {

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

  override def getConnectionToken(request: GetConnectionTokenRequest): Future[GetConnectionTokenResponse] =
    service.getConnectionToken(request)

  override def getSubjectStatusStream(
      request: GetSubjectStatusRequest,
      responseObserver: StreamObserver[GetSubjectStatusResponse]
  ): Unit =
    service.getSubjectStatusStream(request, responseObserver)
}

object InsuranceServiceImpl {
  private val issuerId = ParticipantId("a1cb7eee-65c1-4d7f-9417-db8a37a6212a")
  private val credentialTypeId = "VerifiableCredential/AtalaCertificateOfInsurance"

  case class RequiredInsuranceData(idCredential: Credential, employmentCredential: Credential)

  private def requestIdAndEmploymentCredentials(connectorIntegration: ConnectorIntegration)(
      connection: Connection
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- connectorIntegration.sendProofRequest(
        issuerId,
        connection.connectionId,
        ProofRequest(IdServiceImpl.credentialTypeId, connection.connectionToken.token)
      )
      _ <- connectorIntegration.sendProofRequest(
        issuerId,
        connection.connectionId,
        ProofRequest(EmploymentServiceImpl.credentialTypeId, connection.connectionToken.token)
      )
    } yield ()
  }

  private def getRequiredInsuranceData(connectorIntegration: ConnectorIntegration)(
      implicit ec: ExecutionContext
  ): TokenString => Future[Option[RequiredInsuranceData]] = { connectionToken =>
    getSharedCredentials(connectorIntegration)(ec)(connectionToken).map(toRequiredInsuranceData)
  }

  private def toRequiredInsuranceData(seq: Seq[Credential]): Option[RequiredInsuranceData] = {
    for {
      idCredential <- seq.find(credential => credential.typeId == IdServiceImpl.credentialTypeId)
      employmentCredential <- seq.find(credential => credential.typeId == EmploymentServiceImpl.credentialTypeId)
    } yield RequiredInsuranceData(idCredential, employmentCredential)
  }

  private def getSharedCredentials(
      connectorIntegration: ConnectorIntegration
  )(implicit ec: ExecutionContext): TokenString => Future[Seq[Credential]] = { connectionToken =>
    SharedCredentials
      .getSharedCredentials(connectorIntegration, connectionToken, issuerId)(
        Set(IdServiceImpl.credentialTypeId, EmploymentServiceImpl.credentialTypeId)
      )
  }

  private def getInsuranceCredential(requiredInsuranceData: RequiredInsuranceData): Credential = {

    val idData = IdData.toIdData(requiredInsuranceData.idCredential)

    val employmentData = EmploymentData.toEmploymentData(requiredInsuranceData.employmentCredential)

    val insuranceCredential = insuranceCredentialJsonTemplate(
      id = "unknown",
      issuanceDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusYears(1),
      subjectFullName = idData.name,
      subjectDid = "unknown",
      policyNumber = "ABC-123456789",
      productClass = "Life Insurance",
      subjectDateOfBirth = idData.dob,
      employerName = employmentData.employerName,
      employerAddress = employmentData.employerAddress
    ).printWith(jsonPrinter)

    Credential(typeId = credentialTypeId, credentialDocument = insuranceCredential)
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
        "name" -> fromString("Atala Insurance Ltd.")
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
