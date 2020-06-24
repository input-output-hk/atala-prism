package io.iohk.cvp.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.circe.parser.parse
import io.grpc.{Status, StatusException}
import io.iohk.connector.model.{Connection, ConnectionId, MessageId, TokenString}
import io.iohk.cvp.intdemo.IdServiceImplSpec._
import io.iohk.cvp.intdemo.Testing._
import io.iohk.prism.intdemo.protos.{intdemo_api, intdemo_models}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{mock, verify, when}
import org.scalatest.EitherValues._
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
      .setPersonalData(intdemo_api.SetPersonalDataRequest(token.token, "", Some(today())))
      .failed
      .futureValue
      .asInstanceOf[StatusException]

    response.getStatus shouldBe Status.INVALID_ARGUMENT
  }

  it should "reject empty date of birth" in idService { (_, _, idService) =>
    val response = idService
      .setPersonalData(intdemo_api.SetPersonalDataRequest(token.token, "name", None))
      .failed
      .futureValue
      .asInstanceOf[StatusException]

    response.getStatus shouldBe Status.INVALID_ARGUMENT
  }

  it should "update the personal info when the user's personal data is uploaded" in idService(
    intdemo_models.SubjectStatus.UNCONNECTED,
    None,
    None
  ) { (_, repository, idService) =>
    idService
      .setPersonalData(
        intdemo_api.SetPersonalDataRequest(
          token.token,
          name,
          Some(intdemo_models.Date(dob.getYear, dob.getMonthValue, dob.getDayOfMonth))
        )
      )
      .futureValue

    verify(repository).mergePersonalInfo(token, name, dob)
  }

  "getIdCredential" should "return a correct ID credential" in {
    val credential = IdServiceImpl.getIdCredential(("first-name", LocalDate.of(1973, 6, 2)))

    // Verify type
    credential.typeId shouldBe "VerifiableCredential/RedlandIdCredential"

    // Verify JSON document
    val document = parse(credential.credentialDocument).right.value.hcursor
    val today = LocalDate.now().atStartOfDay().toLocalDate
    val yesterday = today.minusDays(1)
    val issuanceDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(document.jsonStr("issuanceDate")))
    val expiryDate = issuanceDate.plusYears(10)
    val formattedExpiryDate = DateTimeFormatter.ISO_LOCAL_DATE.format(expiryDate)

    document.jsonStr("id") shouldBe "unknown"
    document.jsonArr("type") shouldBe List("VerifiableCredential", "RedlandIdCredential")
    document.jsonStr("issuer.id") shouldBe "did:atala:091d41cc-e8fc-4c44-9bd3-c938dcf76dff"
    document.jsonStr("issuer.name") shouldBe "Metropol City Government"
    // Test issuance to be today or yesterday, in case the test started to run yesterday
    issuanceDate should (be(today) or be(yesterday))
    document.jsonStr("expiryDate") shouldBe formattedExpiryDate
    document.jsonStr("credentialSubject.id") shouldBe "unknown"
    document.jsonStr("credentialSubject.identityNumber") shouldBe "RL-2DF6E5A51"
    document.jsonStr("credentialSubject.name") shouldBe "first-name"
    document.jsonStr("credentialSubject.dateOfBirth") shouldBe "1973-06-02"

    // Verify HTML view
    document.jsonStr("view.html") shouldBe
      s"""<div style="height: 310px; width: 600px; border: 1px solid #e5e5e5; border-radius: 10px; font-family: Helvetica; font-size: 18px">
         |    <div style="overflow: hidden; background-color: #e5e5e5; padding: 30px">
         |        <div style="float: left">
         |            <div style="font-size: 23px; color: #828282; margin-bottom: 15px">National ID Card</div>
         |            <div style="font-size: 30px; margin-bottom: 15px">Metropol City Government</div>
         |        </div>
         |        <div style="float: right; margin-top: 10px">
         |            <div style="height: 60px; width: 90px; background-color: #ffeaeb; text-align: center; border-radius: 5px">
         |                <p style="vertical-align: middle; display: inline-block; line-height: 1.5; color: #ff2d3b">Flag</p>
         |            </div>
         |        </div>
         |    </div>
         |    <div style="overflow: hidden; padding: 15px">
         |        <div style="float: left; margin: 0 5px">
         |            <div style="height: 120px; width: 120px; background-color: #ffeaeb; text-align: center; border-radius: 10px; margin: 0 10px">
         |                <p style="vertical-align: middle; display: inline-block; line-height: 5; color: #ff2d3b">Picture</p>
         |            </div>
         |        </div>
         |        <div style="float: left; margin: 0 20px">
         |            <div style="font-size: 14px; color: #828282; margin-bottom: 10px">Full Name</div>
         |            <div style="margin-bottom: 20px">first-name</div>
         |            <div style="font-size: 14px; color: #828282; margin-bottom: 10px">Identity Number</div>
         |            <div>RL-2DF6E5A51</div>
         |        </div>
         |        <div style="float: right; margin-right: 35px">
         |            <div style="font-size: 14px; color: #828282; margin-bottom: 10px">Date of Birth</div>
         |            <div style="margin-bottom: 20px">1973-06-02</div>
         |            <div style="font-size: 14px; color: #828282; margin-bottom: 10px">Expiration Date</div>
         |            <div>$formattedExpiryDate</div>
         |        </div>
         |    </div>
         |</div>
         |""".stripMargin
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
    idService(intdemo_models.SubjectStatus.UNCONNECTED, None, None)(testCode)
  }

  def idService(
      subjectStatus: intdemo_models.SubjectStatus,
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
    ()
  }

  def today(): intdemo_models.Date = toDate(LocalDate.now())

  def toDate(ld: LocalDate): intdemo_models.Date = {
    intdemo_models.Date(ld.getYear, ld.getMonthValue, ld.getDayOfMonth)
  }
}
