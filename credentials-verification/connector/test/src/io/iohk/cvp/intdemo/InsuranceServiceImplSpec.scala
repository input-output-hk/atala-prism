package io.iohk.cvp.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalatest.FlatSpec
import Testing._
import io.circe.parser.parse
import io.iohk.cvp.intdemo.EmploymentServiceImpl.RequiredEmploymentData
import io.iohk.cvp.intdemo.InsuranceServiceImpl.RequiredInsuranceData
import org.scalatest.Matchers._
import io.iohk.cvp.intdemo.Testing._
import org.scalatest.EitherValues._

class InsuranceServiceImplSpec extends FlatSpec {

  "getInsuranceCredential" should "return a correct insurance credential" in {
    val idCredential = IdServiceImpl.getIdCredential(("name", LocalDate.of(1973, 6, 2)))
    val degreeCredential = DegreeServiceImpl.getDegreeCredential(idCredential)
    val employmentCredential =
      EmploymentServiceImpl.getEmploymentCredential(RequiredEmploymentData(idCredential, degreeCredential))
    val credential =
      InsuranceServiceImpl.getInsuranceCredential(RequiredInsuranceData(idCredential, employmentCredential))

    // Verify type
    credential.typeId shouldBe "VerifiableCredential/AtalaCertificateOfInsurance"

    // Verify JSON document
    val document = parse(credential.credentialDocument).right.value.hcursor
    val issuanceDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(document.jsonStr("issuanceDate")))
    val expiryDate = DateTimeFormatter.ISO_LOCAL_DATE.format(issuanceDate.plusYears(1))
    val today = LocalDate.now().atStartOfDay().toLocalDate
    val yesterday = today.minusDays(1)

    document.jsonStr("id") shouldBe "unknown"
    document.jsonArr("type") shouldBe List("VerifiableCredential", "AtalaCertificateOfInsurance")
    document.jsonStr("issuer.id") shouldBe "did:atala:a1cb7eee-65c1-4d7f-9417-db8a37a6212a"
    document.jsonStr("issuer.name") shouldBe "Verified Insurance Ltd."
    // Test issuance to be today or yesterday, in case the test started to run yesterday
    issuanceDate should (be(today) or be(yesterday))
    document.jsonStr("expiryDate") shouldBe expiryDate
    document.jsonStr("policyNumber") shouldBe "ABC-123456789"
    document.jsonStr("productClass") shouldBe "Health Insurance"

    document.jsonStr("credentialSubject.id") shouldBe "unknown"
    document.jsonStr("credentialSubject.name") shouldBe "name"
    document.jsonStr("credentialSubject.dateOfBirth") shouldBe "1973-06-02"
    document.jsonStr("credentialSubject.currentEmployer.name") shouldBe "Decentralized Inc."
    document.jsonStr("credentialSubject.currentEmployer.address") shouldBe "67 Clasper Way, Herefoot, HF1 0AF"

    // Verify HTML view
    val expectedHtmlView = readResource("health_credential.html").replace("@policyEndDate", expiryDate)
    document.jsonStr("view.html") shouldBe expectedHtmlView
  }
}
