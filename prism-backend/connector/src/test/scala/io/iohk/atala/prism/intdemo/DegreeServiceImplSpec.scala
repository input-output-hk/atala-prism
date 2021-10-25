package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import io.circe.parser.parse
import Testing._
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import io.iohk.atala.prism.utils.Base64Utils
import io.iohk.atala.prism.identity.{PrismDid => DID}

class DegreeServiceImplSpec extends AnyFlatSpec {

  "getDegreeCredential" should "return a correct degree credential" in {
    val (name, dateOfBirth) = ("Jo Wong", LocalDate.of(1973, 6, 2))
    val idCredential = IdServiceImpl.getIdCredential((name, dateOfBirth))
    val degreeCredential = DegreeServiceImpl.getDegreeCredential(idCredential)

    // Verify JSON document
    val jsonString =
      Base64Utils.decodeUrlToString(degreeCredential.encodedCredential)
    val document = parse(jsonString).toOption.value.hcursor

    val degreeAwarded = "Bachelor of Science"
    val degreeResult = "First-class honors"
    val startDate = LocalDate.now().minusYears(4)
    val issuerName = "University of Innovation and Technology"
    val issuerDID = s"did:prism:${DegreeServiceImpl.issuerId.uuid}"
    val issuanceDate = LocalDate.now()
    val holderName = name
    val graduationDate = dateOfBirth.plusYears(20)
    val issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID
    val credentialType = DegreeServiceImpl.credentialTypeId
    val holderDateOfBirth = dateOfBirth

    document.jsonStr("issuerDid") shouldBe issuerDID
    document.jsonStr("issuerName") shouldBe issuerName
    document.jsonStr("issuanceKeyId") shouldBe issuanceKeyId
    document.jsonStr("issuanceDate") shouldBe DateTimeFormatter.ISO_LOCAL_DATE
      .format(issuanceDate)
    document.jsonStr("credentialSubject.credentialType") shouldBe credentialType
    document.jsonStr("credentialSubject.name") shouldBe holderName
    document.jsonStr(
      "credentialSubject.dateOfBirth"
    ) shouldBe DateTimeFormatter.ISO_LOCAL_DATE.format(
      holderDateOfBirth
    )
    document.jsonStr(
      "credentialSubject.graduationDate"
    ) shouldBe DateTimeFormatter.ISO_LOCAL_DATE.format(
      graduationDate
    )
    document.jsonStr(
      "credentialSubject.startDate"
    ) shouldBe DateTimeFormatter.ISO_LOCAL_DATE.format(startDate)
    document.jsonStr("credentialSubject.degreeAwarded") shouldBe degreeAwarded
    document.jsonStr("credentialSubject.degreeResult") shouldBe degreeResult

    // Verify HTML
    val expectedHtml =
      readResource("university_degree.html")
        .replace("@degreeAwarded", degreeAwarded)
        .replace("@issuerName", issuerName)
        .replace("@degreeResult", degreeResult)
        .replace("@holderName", holderName)
        .replace(
          "@startDate",
          DateTimeFormatter.ISO_LOCAL_DATE.format(startDate)
        )
        .replace(
          "@graduationDate",
          DateTimeFormatter.ISO_LOCAL_DATE.format(graduationDate)
        )

    document.jsonStr("credentialSubject.html") shouldBe expectedHtml
  }
}
