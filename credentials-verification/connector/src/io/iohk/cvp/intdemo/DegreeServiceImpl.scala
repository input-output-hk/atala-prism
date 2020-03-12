package io.iohk.cvp.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import credential.Credential
import io.circe._
import io.circe.Json.fromString
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, Message, TokenString}
import io.iohk.cvp.intdemo.DegreeServiceImpl.{getDegreeCredential, getSharedIdCredential, issuerId}
import io.iohk.cvp.intdemo.protos.DegreeServiceGrpc._
import io.iohk.cvp.intdemo.protos._
import io.iohk.cvp.models.ParticipantId
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class DegreeServiceImpl(
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration
)(
    implicit ec: ExecutionContext
) extends DegreeService {

  val service = new IntDemoService[Credential](
    issuerId,
    connectorIntegration,
    intDemoRepository,
    schedulerPeriod,
    getSharedIdCredential(connectorIntegration),
    getDegreeCredential,
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
}

object DegreeServiceImpl {
  val issuerId = ParticipantId("6c170e91-92b0-4265-909d-951c11f30caa")

  private val credentialTypeId = "VerifiableCredential/AirsideDegreeCredential"

  private val jsonPrinter = Printer(dropNullValues = false, indent = "  ")

  case class DegreeData(idData: IdData) {
    val degreeAwarded = "Bachelor of Science"
    val degreeResult = "Upper second class honours"
    val graduationYear = idData.dob.plusYears(20).getYear
  }

  def credentialsOfType(s: String)(messages: Seq[Message]): Seq[Credential] = {
    messages.collect {
      case message =>
        val credential = Credential.messageCompanion.parseFrom(message.content)
        if (credential.typeId == s)
          Some(credential)
        else
          None
    }.flatten
  }

  private def getSharedCredentials(connectorIntegration: ConnectorIntegration, connectionToken: TokenString)(
      typeId: String
  )(implicit ec: ExecutionContext): Future[Seq[Credential]] = {
    for {
      maybeConnection: Option[Connection] <- connectorIntegration.getConnectionByToken(connectionToken)
      messages <- maybeConnection
        .fold(Future.successful(Seq.empty[Message]))(
          connection => connectorIntegration.getMessages(issuerId, connection.connectionId)
        )
    } yield {
      credentialsOfType(typeId)(messages)
    }
  }

  private def getSharedIdCredential(connectorIntegration: ConnectorIntegration)(
      implicit ec: ExecutionContext
  ): TokenString => Future[Option[Credential]] =
    connectionToken =>
      getSharedCredentials(connectorIntegration, connectionToken)(IdServiceImpl.credentialTypeId).map(_.headOption)

  private def getDegreeCredential(idCredential: Credential): Credential = {

    val idData = IdData
      .toIdData(idCredential)
      .getOrElse(
        throw new IllegalStateException(
          s"The shared id credential is invalid. Document follows: '${idCredential.credentialDocument}''"
        )
      )

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

    Credential(
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
        "name" -> fromString("Air Side University")
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

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private def formatDate(d: LocalDate): String = {
    dateFormatter.format(d)
  }
}
