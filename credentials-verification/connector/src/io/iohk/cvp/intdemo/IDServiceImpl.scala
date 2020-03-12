package io.iohk.cvp.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import credential._
import io.circe._
import io.circe.Json.fromString
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.TokenString
import io.iohk.cvp.intdemo.IdServiceImpl._
import io.iohk.cvp.intdemo.protos.IDServiceGrpc._
import io.iohk.cvp.intdemo.protos._
import io.iohk.cvp.models.ParticipantId
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IdServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(
    implicit ec: ExecutionContext
) extends IDService {

  val service = new IntDemoService[(String, LocalDate)](
    issuerId,
    connectorIntegration,
    intDemoRepository,
    schedulerPeriod,
    getPersonalData(intDemoRepository),
    getIdCredential,
    scheduler
  )

  override def getConnectionToken(request: GetConnectionTokenRequest): Future[GetConnectionTokenResponse] = {
    service.getConnectionToken(request)
  }

  override def getSubjectStatusStream(
      request: GetSubjectStatusRequest,
      responseObserver: StreamObserver[GetSubjectStatusResponse]
  ): Unit = {
    service.getSubjectStatusStream(request, responseObserver)
  }

  override def setPersonalData(request: SetPersonalDataRequest): Future[SetPersonalDataResponse] = {
    if (request.dateOfBirth.isEmpty || request.firstName.isEmpty) {
      Future.failed(Status.INVALID_ARGUMENT.asException())
    } else {
      intDemoRepository
        .mergePersonalInfo(
          new TokenString(request.connectionToken),
          request.firstName,
          LocalDate.of(request.dateOfBirth.get.year, request.dateOfBirth.get.month, request.dateOfBirth.get.day)
        )
        .map(_ => SetPersonalDataResponse())
    }
  }
}

object IdServiceImpl {

  val issuerId = ParticipantId("091d41cc-e8fc-4c44-9bd3-c938dcf76dff")

  val credentialTypeId = "VerifiableCredential/RedlandIdCredential"

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

  def getIdCredential(requiredData: (String, LocalDate)): Credential = {
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
    Credential(
      typeId = credentialTypeId,
      credentialDocument = idCredential
    )
  }

  private def generateSubjectIdNumber(seedStr: String): String = {
    import java.nio.ByteBuffer
    val seed = ByteBuffer.wrap(seedStr.getBytes.take(4)).getInt
    val random = new Random(seed)
    s"RL-${(1 to 9).map(_ => random.nextInt(10)).mkString}"
  }

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private def formatDate(d: LocalDate): String = {
    dateFormatter.format(d)
  }
}
