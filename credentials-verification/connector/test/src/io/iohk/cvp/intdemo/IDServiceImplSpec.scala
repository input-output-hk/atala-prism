package io.iohk.cvp.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import Testing._
import io.grpc.{Status, StatusException}
import io.iohk.connector.model.{Connection, ConnectionId, MessageId, TokenString}
import io.iohk.cvp.intdemo.IdServiceImplSpec._
import io.iohk.cvp.intdemo.protos.SubjectStatus.UNCONNECTED
import io.iohk.cvp.intdemo.protos._
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{mock, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, convertScalaFuture}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class IdServiceImplSpec extends FlatSpec {

  implicit val pc: PatienceConfig = PatienceConfig(1 second, 100 millis)

  "setPersonalData" should "reject empty first name" in idService { (_, _, idService) =>
    val response = idService
      .setPersonalData(SetPersonalDataRequest(token.token, "", Some(today())))
      .failed
      .futureValue
      .asInstanceOf[StatusException]

    response.getStatus shouldBe Status.INVALID_ARGUMENT
  }

  it should "reject empty date of birth" in idService { (_, _, idService) =>
    val response = idService
      .setPersonalData(SetPersonalDataRequest(token.token, "name", None))
      .failed
      .futureValue
      .asInstanceOf[StatusException]

    response.getStatus shouldBe Status.INVALID_ARGUMENT
  }

  it should "update the personal info when the user's personal data is uploaded" in idService(UNCONNECTED, None, None) {
    (_, repository, idService) =>
      idService
        .setPersonalData(
          SetPersonalDataRequest(token.token, name, Some(Date(dob.getYear, dob.getMonthValue, dob.getDayOfMonth)))
        )
        .futureValue

      verify(repository).mergePersonalInfo(token, name, dob)
  }

  "idCredentialTemplate" should "render an ID credential correctly" in {
    val d = LocalDate.now()
    val df = DateTimeFormatter.ISO_LOCAL_DATE.format(d)
    val d2 = d.plusYears(3)
    val df2 = DateTimeFormatter.ISO_LOCAL_DATE.format(d2)
    val dob = LocalDate.of(1973, 6, 6)
    val dobf = DateTimeFormatter.ISO_LOCAL_DATE.format(dob)

    val json = IdServiceImpl.idCredentialJsonTemplate(
      id = "credential-id",
      subjectIdNumber = "ABC-123",
      issuanceDate = d,
      expiryDate = d2,
      subjectDid = "did:atala:subject-did",
      subjectFirstName = "first-name",
      subjectDateOfBirth = LocalDate.of(1973, 6, 6)
    )

    val c = json.hcursor
    c.jsonStr("id") shouldBe "credential-id"
    c.jsonArr("type") shouldBe List("VerifiableCredential", "RedlandIdCredential")
    c.jsonStr("issuer.id") shouldBe "did:atala:091d41cc-e8fc-4c44-9bd3-c938dcf76dff"
    c.jsonStr("issuer.name") shouldBe "Department of Interior, Republic of Redland"
    c.jsonStr("issuanceDate") shouldBe df
    c.jsonStr("expiryDate") shouldBe df2
    c.jsonStr("credentialSubject.id") shouldBe "did:atala:subject-did"
    c.jsonStr("credentialSubject.identityNumber") shouldBe "ABC-123"
    c.jsonStr("credentialSubject.name") shouldBe "first-name"
    c.jsonStr("credentialSubject.dateOfBirth") shouldBe dobf
  }
}

object IdServiceImplSpec {
  implicit val ec: ExecutionContext = ExecutionContext.global
  private val connectionId = ConnectionId.random()
  private val messageId = MessageId.random()
  private val issuerId = IdServiceImpl.issuerId
  private val token = new TokenString("a token")
  private val name = "name"
  private val dob: LocalDate = LocalDate.of(1950, 1, 1)

  def idService(testCode: (ConnectorIntegration, IntDemoRepository, IdServiceImpl) => Any): Unit = {
    idService(UNCONNECTED, None, None)(testCode)
  }

  def idService(
      subjectStatus: SubjectStatus,
      connection: Option[Connection],
      personalInfo: Option[(String, LocalDate)]
  )(testCode: (ConnectorIntegration, IntDemoRepository, IdServiceImpl) => Any): Unit = {
    val connectorIntegration = mock[ConnectorIntegration]
    val repository = mock[IntDemoRepository]

    val service = new IdServiceImpl(connectorIntegration, repository, schedulerPeriod = 1 milli)

    when(connectorIntegration.sendCredential(eqTo(issuerId), eqTo(connectionId), any)).thenReturn(Future(messageId))
    when(connectorIntegration.getConnectionByToken(token)).thenReturn(Future(connection))
    when(repository.mergeSubjectStatus(eqTo(token), any)).thenReturn(Future(1))
    when(repository.mergePersonalInfo(eqTo(token), eqTo(name), eqTo(dob))).thenReturn(Future(1))
    when(repository.findSubjectStatus(token)).thenReturn(Future(Some(subjectStatus)))
    when(repository.findPersonalInfo(token)).thenReturn(Future(personalInfo))

    testCode(connectorIntegration, repository, service)
  }

  def today(): Date = toDate(LocalDate.now())

  def toDate(ld: LocalDate): Date = {
    Date(ld.getYear, ld.getMonthValue, ld.getDayOfMonth)
  }
}
