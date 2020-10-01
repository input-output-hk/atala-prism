package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.circe.parser.parse
import Testing._
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class DegreeServiceImplSpec extends AnyFlatSpec {

  "getDegreeCredential" should "return a correct degree credential" in {
    val idCredential = IdServiceImpl.getIdCredential(("name", LocalDate.of(1973, 6, 2)))
    val credential = DegreeServiceImpl.getDegreeCredential(idCredential)

    // Verify type
    credential.typeId shouldBe "VerifiableCredential/AirsideDegreeCredential"

    // Verify JSON document
    val document = parse(credential.credentialDocument).right.value.hcursor
    val issuanceDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(document.jsonStr("issuanceDate")))
    val formattedIssuanceDate = DateTimeFormatter.ISO_LOCAL_DATE.format(issuanceDate)
    val startDate = DateTimeFormatter.ISO_LOCAL_DATE.format(issuanceDate.minusYears(4))
    val today = LocalDate.now().atStartOfDay().toLocalDate
    val yesterday = today.minusDays(1)

    document.jsonStr("id") shouldBe "unknown"
    document.jsonArr("type") shouldBe List("VerifiableCredential", "AirsideDegreeCredential")
    document.jsonStr("issuer.id") shouldBe "did:atala:6c170e91-92b0-4265-909d-951c11f30caa"
    document.jsonStr("issuer.name") shouldBe "University of Innovation and Technology"
    // Test issuance to be today or yesterday, in case the test started to run yesterday
    issuanceDate should (be(today) or be(yesterday))
    document.jsonStr("credentialSubject.id") shouldBe "unknown"
    document.jsonStr("credentialSubject.name") shouldBe "name"
    document.jsonStr("credentialSubject.degreeAwarded") shouldBe "Bachelor of Science"
    document.jsonStr("credentialSubject.degreeResult") shouldBe "First-class honors"
    document.jsonStr("credentialSubject.startDate") shouldBe startDate
    document.jsonNum[Int]("credentialSubject.graduationYear") shouldBe 1993

    // Verify HTML view
    val expectedHtmlView =
      readResource("university_degree.html")
        .replace("@startDate", startDate)
        .replace("@graduationDate", formattedIssuanceDate)
    document.jsonStr("view.html") shouldBe expectedHtmlView
  }
}
