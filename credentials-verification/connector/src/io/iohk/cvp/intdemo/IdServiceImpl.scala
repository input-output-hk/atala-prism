package io.iohk.cvp.intdemo

import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.circe.Json.fromString
import io.circe._
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, TokenString}
import io.iohk.cvp.intdemo.IdServiceImpl._
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.intdemo.protos.intdemo_api
import io.iohk.prism.protos.credential_models
import javax.xml.bind.DatatypeConverter
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class IdServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(implicit
    ec: ExecutionContext
) extends intdemo_api.IDServiceGrpc.IDService {

  val service = new IntDemoService[(String, LocalDate)](
    issuerId = issuerId,
    connectorIntegration = connectorIntegration,
    intDemoRepository = intDemoRepository,
    schedulerPeriod = schedulerPeriod,
    requiredDataLoader = getPersonalData(intDemoRepository),
    proofRequestIssuer = noProofRequests,
    getCredential = getIdCredential,
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
          LocalDate.of(request.dateOfBirth.get.year, request.dateOfBirth.get.month, request.dateOfBirth.get.day)
        )
        .map(_ => intdemo_api.SetPersonalDataResponse())
    }
  }
}

object IdServiceImpl {

  val issuerId = ParticipantId("091d41cc-e8fc-4c44-9bd3-c938dcf76dff")

  val credentialTypeId = "VerifiableCredential/RedlandIdCredential"

  def noProofRequests(connection: Connection): Future[Unit] =
    Future.unit

  def idCredentialJsonTemplate(
      id: String,
      subjectIdNumber: String,
      issuanceDate: LocalDate,
      expiryDate: LocalDate,
      subjectDid: String,
      subjectFirstName: String,
      subjectDateOfBirth: LocalDate
  ): Json = {
    Json.obj(
      fields =
        "id" -> fromString(id),
      "type" -> Json.arr(fromString("VerifiableCredential"), fromString("RedlandIdCredential")),
      "issuer" -> Json.obj(
        fields =
          "id" -> fromString("did:atala:091d41cc-e8fc-4c44-9bd3-c938dcf76dff"),
        "name" -> fromString("Department of Interior, Republic of Redland")
      ),
      "issuanceDate" -> fromString(formatDate(issuanceDate)),
      "expiryDate" -> fromString(formatDate(expiryDate)),
      "credentialSubject" -> Json.obj(
        "id" -> fromString(subjectDid),
        "identityNumber" -> fromString(subjectIdNumber),
        "name" -> fromString(subjectFirstName),
        "dateOfBirth" -> fromString(formatDate(subjectDateOfBirth))
      )
    )
  }

  private def getPersonalData(
      intDemoRepository: IntDemoRepository
  ): TokenString => Future[Option[(String, LocalDate)]] =
    connectionToken => intDemoRepository.findPersonalInfo(connectionToken)

  private val jsonPrinter = Printer(dropNullValues = false, indent = "  ")

  def getIdCredential(requiredData: (String, LocalDate)): credential_models.Credential = {
    val (name, dob) = requiredData
    val id = "unknown"
    val subjectIdNumber = generateSubjectIdNumber(name + dateFormatter.format(dob))
    val issuanceDate = LocalDate.now()
    val expiryDate = issuanceDate.plusYears(10)
    val subjectDid = "unknown"
    val idCredential: String =
      idCredentialJsonTemplate(id, subjectIdNumber, issuanceDate, expiryDate, subjectDid, name, dob).printWith(
        jsonPrinter
      )
    credential_models.Credential(
      typeId = credentialTypeId,
      credentialDocument = idCredential
    )
  }

  def generateSubjectIdNumber(seedStr: String): String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(seedStr.getBytes("UTF-8"))
    s"RL-${DatatypeConverter.printHexBinary(md.digest).toUpperCase.take(9)}"
  }

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private def formatDate(d: LocalDate): String = {
    dateFormatter.format(d)
  }
}
