package io.iohk.cvp.intdemo

import java.time.LocalDate

import org.scalatest.FlatSpec
import Testing._
import org.scalatest.Matchers._

class InsuranceServiceImplSpec extends FlatSpec {

  "insuranceCredentialJsonTemplate" should "render an insurance credential correctly" in {

    val json = InsuranceServiceImpl.insuranceCredentialJsonTemplate(
      id = "credential-id",
      issuanceDate = LocalDate.of(1966, 6, 6),
      subjectDid = "did:atala:subject-did",
      subjectFullName = "name",
      expiryDate = LocalDate.of(1967, 6, 6),
      policyNumber = "XYZ",
      productClass = "Body Part Insurance",
      subjectDateOfBirth = LocalDate.of(1945, 4, 5),
      employerName = "Self employed",
      employerAddress = "Buckingham Palace, London, SW1A 1AA"
    )

    val c = json.hcursor
    c.jsonStr("id") shouldBe "credential-id"
    c.jsonArr("type") shouldBe List("VerifiableCredential", "AtalaCertificateOfInsurance")
    c.jsonStr("issuer.id") shouldBe "did:atala:a1cb7eee-65c1-4d7f-9417-db8a37a6212a"
    c.jsonStr("issuer.name") shouldBe "Verified Insurance Ltd."
    c.jsonStr("issuanceDate") shouldBe "1966-06-06"
    c.jsonStr("expiryDate") shouldBe "1967-06-06"
    c.jsonStr("policyNumber") shouldBe "XYZ"
    c.jsonStr("productClass") shouldBe "Body Part Insurance"

    c.jsonStr("credentialSubject.id") shouldBe "did:atala:subject-did"
    c.jsonStr("credentialSubject.name") shouldBe "name"
    c.jsonStr("credentialSubject.dateOfBirth") shouldBe "1945-04-05"
    c.jsonStr("credentialSubject.currentEmployer.name") shouldBe "Self employed"
    c.jsonStr("credentialSubject.currentEmployer.address") shouldBe "Buckingham Palace, London, SW1A 1AA"
  }
}
