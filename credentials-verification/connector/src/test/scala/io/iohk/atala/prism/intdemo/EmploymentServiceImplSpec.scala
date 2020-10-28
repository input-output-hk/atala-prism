package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.circe.parser.parse
import io.iohk.atala.prism.intdemo.EmploymentServiceImpl.RequiredEmploymentData
import Testing._
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class EmploymentServiceImplSpec extends AnyFlatSpec {

  "getEmploymentCredential" should "return a correct employment credential" in {
    val idCredential = IdServiceImpl.getIdCredential(("name", LocalDate.of(1973, 6, 2)))
    val degreeCredential = DegreeServiceImpl.getDegreeCredential(idCredential)

    val credential =
      EmploymentServiceImpl.getEmploymentCredential(RequiredEmploymentData(idCredential, degreeCredential))

    // Verify type
    credential.typeId shouldBe "VerifiableCredential/AtalaEmploymentCredential"

    // Verify JSON document
    val document = parse(credential.credentialDocument).toOption.value.hcursor
    val issuanceDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(document.jsonStr("issuanceDate")))
    val employmentStartDate = DateTimeFormatter.ISO_LOCAL_DATE.format(issuanceDate.minusMonths(1))
    val today = LocalDate.now().atStartOfDay().toLocalDate
    val yesterday = today.minusDays(1)

    document.jsonStr("id") shouldBe "unknown"
    document.jsonArr("type") shouldBe List("VerifiableCredential", "AtalaEmploymentCredential")
    document.jsonStr("issuer.id") shouldBe "did:atala:12c28b34-95be-4801-951e-c775f89d05ba"
    document.jsonStr("issuer.name") shouldBe "Decentralized Inc."
    document.jsonStr("issuer.address") shouldBe "67 Clasper Way, Herefoot, HF1 0AF"
    // Test issuance to be today or yesterday, in case the test started to run yesterday
    issuanceDate should (be(today) or be(yesterday))
    document.jsonStr("employmentStartDate") shouldBe employmentStartDate
    document.jsonStr("employmentStatus") shouldBe "Full-time"
    document.jsonStr("credentialSubject.id") shouldBe "unknown"
    document.jsonStr("credentialSubject.name") shouldBe "name"

    // Verify HTML view
    val expectedHtmlView = readResource("proof_of_employment.html").replace("@employmentStartDate", employmentStartDate)
    document.jsonStr("view.html") shouldBe expectedHtmlView
  }
}
