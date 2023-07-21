package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.circe.parser.parse
import io.iohk.atala.prism.intdemo.EmploymentServiceImpl.RequiredEmploymentData
import io.iohk.atala.prism.intdemo.InsuranceServiceImpl.RequiredInsuranceData
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import Testing._
import org.scalatest.OptionValues._
import io.iohk.atala.prism.utils.Base64Utils
import io.iohk.atala.prism.identity.{PrismDid => DID}

class InsuranceServiceImplSpec extends AnyFlatSpec {

  "getInsuranceCredential" should "return a correct insurance credential" in {
    val (name, dateOfBirth) = ("Jo Wong", LocalDate.of(1973, 6, 2))

    val idCredential = IdServiceImpl.getIdCredential((name, dateOfBirth))
    val degreeCredential = DegreeServiceImpl.getDegreeCredential(idCredential)
    val employmentCredential =
      EmploymentServiceImpl.getEmploymentCredential(
        RequiredEmploymentData(idCredential, degreeCredential)
      )
    val insuranceCredential =
      InsuranceServiceImpl.getInsuranceCredential(
        RequiredInsuranceData(idCredential, employmentCredential)
      )

    val employmentCredentialData =
      EmploymentCredentialData(employmentCredential)

    // Verify JSON document
    val jsonString =
      Base64Utils.decodeUrlToString(insuranceCredential.encodedCredential)
    val document = parse(jsonString).toOption.value.hcursor

    val credentialType = InsuranceServiceImpl.credentialTypeId
    val issuerName = "Verified Insurance Ltd"
    val issuerDID = s"did:prism:${InsuranceServiceImpl.issuerId.uuid}"
    val issuanceKeyId = DID.getDEFAULT_MASTER_KEY_ID
    val issuanceDate = LocalDate.now()
    val expirationDate = issuanceDate.plusYears(1)
    val policyNumber = "ABC-123456789"
    val productClass = "Health Insurance"
    val holderName = name
    val holderDateOfBirth = dateOfBirth
    val currentEmployerName = employmentCredentialData.employerName
    val currentEmployerAddress = employmentCredentialData.employerAddress

    document.jsonStr("issuerDid") shouldBe issuerDID
    document.jsonStr("issuerName") shouldBe issuerName
    document.jsonStr("issuanceKeyId") shouldBe issuanceKeyId
    document.jsonStr("issuanceDate") shouldBe DateTimeFormatter.ISO_LOCAL_DATE
      .format(issuanceDate)
    document.jsonStr("expiryDate") shouldBe DateTimeFormatter.ISO_LOCAL_DATE
      .format(expirationDate)
    document.jsonStr("policyNumber") shouldBe policyNumber
    document.jsonStr("productClass") shouldBe productClass

    document.jsonStr("credentialSubject.credentialType") shouldBe credentialType
    document.jsonStr("credentialSubject.name") shouldBe holderName
    document.jsonStr(
      "credentialSubject.dateOfBirth"
    ) shouldBe DateTimeFormatter.ISO_LOCAL_DATE.format(
      holderDateOfBirth
    )

    document.jsonStr(
      "credentialSubject.currentEmployer.name"
    ) shouldBe currentEmployerName
    document.jsonStr(
      "credentialSubject.currentEmployer.address"
    ) shouldBe currentEmployerAddress

    // Verify HTML view
    val expectedHtml = readResource("health_credential.html")
      .replace("@issuerName", issuerName)
      .replace("@productClass", productClass)
      .replace("@policyNumber", policyNumber)
      .replace("@holderName", holderName)
      .replace(
        "@expirationDate",
        DateTimeFormatter.ISO_LOCAL_DATE.format(expirationDate)
      )

    document.jsonStr("credentialSubject.html") shouldBe expectedHtml
  }
}
