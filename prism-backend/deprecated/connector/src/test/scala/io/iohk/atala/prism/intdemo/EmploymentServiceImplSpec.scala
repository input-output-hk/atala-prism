package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import io.circe.parser.parse
import io.iohk.atala.prism.intdemo.EmploymentServiceImpl.RequiredEmploymentData
import Testing._
import io.iohk.atala.prism.identity.{PrismDid => DID}
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import io.iohk.atala.prism.utils.Base64Utils

class EmploymentServiceImplSpec extends AnyFlatSpec {

  "getEmploymentCredential" should "return a correct employment credential" in {
    val (name, dateOfBirth) = ("Joe wong", LocalDate.of(1973, 6, 2))

    val idCredential = IdServiceImpl.getIdCredential((name, dateOfBirth))
    val degreeCredential = DegreeServiceImpl.getDegreeCredential(idCredential)

    val employmentCredential =
      EmploymentServiceImpl.getEmploymentCredential(
        RequiredEmploymentData(idCredential, degreeCredential)
      )

    val issuerDID = s"did:prism:${EmploymentServiceImpl.issuerId.uuid}"
    val issuanceDate = LocalDate.now()
    val issuerName = "Decentralized Inc."
    val issuerAddress = "67 Clasper Way, Herefoot, HF1 0AF"
    val issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID
    val holderName = name
    val credentialType = EmploymentServiceImpl.credentialTypeId
    val employmentStartDate = LocalDate.now().minusMonths(1)
    val employmentStatus = "Full-time"

    // Verify JSON document
    val jsonString =
      Base64Utils.decodeUrlToString(employmentCredential.encodedCredential)
    val document = parse(jsonString).toOption.value.hcursor

    document.jsonStr("issuerDid") shouldBe issuerDID
    document.jsonStr("issuanceKeyId") shouldBe issuanceKeyId
    document.jsonStr("issuerName") shouldBe issuerName
    document.jsonStr("issuerAddress") shouldBe issuerAddress
    document.jsonStr("issuanceDate") shouldBe DateTimeFormatter.ISO_LOCAL_DATE
      .format(issuanceDate)
    document.jsonStr(
      "employmentStartDate"
    ) shouldBe DateTimeFormatter.ISO_LOCAL_DATE.format(employmentStartDate)
    document.jsonStr("employmentStatus") shouldBe employmentStatus
    document.jsonStr("credentialSubject.credentialType") shouldBe credentialType
    document.jsonStr("credentialSubject.name") shouldBe holderName

    // Verify HTML view
    val expectedHtml = readResource("proof_of_employment.html")
      .replace("@issuerName", issuerName)
      .replace("@holderName", holderName)
      .replace("@issuerAddress", issuerAddress)
      .replace("@employmentStatus", employmentStatus)
      .replace(
        "@employmentStartDate",
        DateTimeFormatter.ISO_LOCAL_DATE.format(employmentStartDate)
      )

    document.jsonStr("credentialSubject.html") shouldBe expectedHtml
  }
}
