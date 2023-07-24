package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import io.circe.parser.parse
import io.grpc.{Status, StatusException}
import io.iohk.atala.prism.connector.model.{Connection, ConnectionId, MessageId, TokenString}
import IdServiceImplSpec._
import Testing._
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.intdemo.protos.{intdemo_api, intdemo_models}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{mock, verify, when}
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, convertScalaFuture}
import io.iohk.atala.prism.utils.Base64Utils

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import io.iohk.atala.prism.identity.{PrismDid => DID}

class IdServiceImplSpec extends AnyFlatSpec {

  implicit val pc: PatienceConfig = PatienceConfig(1 second, 100 millis)

  "setPersonalData" should "reject empty first name" in idService { (_, _, idService) =>
    val response = idService
      .setPersonalData(
        intdemo_api.SetPersonalDataRequest(token.token, "", Some(today()))
      )
      .failed
      .futureValue
      .asInstanceOf[StatusException]

    response.getStatus shouldBe Status.INVALID_ARGUMENT
  }

  it should "reject empty date of birth" in idService { (_, _, idService) =>
    val response = idService
      .setPersonalData(
        intdemo_api.SetPersonalDataRequest(token.token, "name", None)
      )
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
          Some(
            intdemo_models
              .Date(dob.getYear, dob.getMonthValue, dob.getDayOfMonth)
          )
        )
      )
      .futureValue

    verify(repository).mergePersonalInfo(token, name, dob)
  }

  "getIdCredential" should "return a correct ID credential" in {

    val (name, dateOfBirth) = ("Joe wong", LocalDate.of(1973, 6, 2))

    val idCredential = IdServiceImpl.getIdCredential((name, dateOfBirth))

    val jsonString =
      Base64Utils.decodeUrlToString(idCredential.encodedCredential)
    val document = parse(jsonString).toOption.value.hcursor

    val issuerName = "Metropol City Government"
    val credentialType = IdServiceImpl.credentialTypeId
    val issuerDID = s"did:prism:${IdServiceImpl.issuerId.uuid}"
    val issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID
    val holderName = name
    val holderDateOfBirth = dateOfBirth
    val identityNumber =
      IdServiceImpl.generateSubjectIdNumber(
        holderName + DateTimeFormatter.ISO_LOCAL_DATE.format(holderDateOfBirth)
      )
    val issuanceDate = LocalDate.now()
    val expirationDate = issuanceDate.plusYears(10)

    document.jsonStr("issuerDid") shouldBe issuerDID
    document.jsonStr("issuanceKeyId") shouldBe issuanceKeyId
    document.jsonStr("issuerName") shouldBe issuerName
    document.jsonStr("issuanceDate") shouldBe DateTimeFormatter.ISO_LOCAL_DATE
      .format(issuanceDate)
    document.jsonStr("expiryDate") shouldBe DateTimeFormatter.ISO_LOCAL_DATE
      .format(expirationDate)
    document.jsonStr("credentialSubject.credentialType") shouldBe credentialType
    document.jsonStr("credentialSubject.name") shouldBe holderName
    document.jsonStr(
      "credentialSubject.dateOfBirth"
    ) shouldBe DateTimeFormatter.ISO_LOCAL_DATE.format(
      holderDateOfBirth
    )
    document.jsonStr("credentialSubject.identityNumber") shouldBe identityNumber

    // Verify HTML view
    val expectedHtmlView = readResource("id_credential.html")
      .replace("@issuerName", issuerName)
      .replace("@identityNumber", identityNumber)
      .replace(
        "@holderDateOfBirth",
        DateTimeFormatter.ISO_LOCAL_DATE.format(holderDateOfBirth)
      )
      .replace("@holderName", holderName)
      .replace(
        "@expirationDate",
        DateTimeFormatter.ISO_LOCAL_DATE.format(expirationDate)
      )

    document.jsonStr("credentialSubject.html") shouldBe expectedHtmlView
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

  def idService(
      testCode: (ConnectorIntegration, IntDemoRepository, IdServiceImpl) => Any
  ): Unit = {
    idService(intdemo_models.SubjectStatus.UNCONNECTED, None, None)(testCode)
  }

  def idService(
      subjectStatus: intdemo_models.SubjectStatus,
      connection: Option[Connection],
      personalInfo: Option[(String, LocalDate)]
  )(
      testCode: (ConnectorIntegration, IntDemoRepository, IdServiceImpl) => Any
  ): Unit = {
    val connectorIntegration = mock[ConnectorIntegration]
    val repository = mock[IntDemoRepository]

    val service = new IdServiceImpl(
      connectorIntegration,
      repository,
      schedulerPeriod = 1 milli
    )

    when(
      connectorIntegration.sendCredential(
        eqTo(issuerId),
        eqTo(connectionId),
        any
      )
    ).thenReturn(Future(messageId))
    when(connectorIntegration.getConnectionByToken(token))
      .thenReturn(Future(connection))
    when(repository.mergeSubjectStatus(eqTo(token), any)).thenReturn(Future(1))
    when(repository.mergePersonalInfo(eqTo(token), eqTo(name), eqTo(dob)))
      .thenReturn(Future(1))
    when(repository.findSubjectStatus(token))
      .thenReturn(Future(Some(subjectStatus)))
    when(repository.findPersonalInfo(token)).thenReturn(Future(personalInfo))

    testCode(connectorIntegration, repository, service)
    ()
  }

  def today(): intdemo_models.Date = toDate(LocalDate.now())

  def toDate(ld: LocalDate): intdemo_models.Date = {
    intdemo_models.Date(ld.getYear, ld.getMonthValue, ld.getDayOfMonth)
  }
}
