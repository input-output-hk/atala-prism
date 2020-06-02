package io.iohk.cvp.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import Testing._

class DegreeServiceImplSpec extends FlatSpec {

  "degreeCredentialJsonTemplate" should "render a Degree credential correctly" in {
    val d = LocalDate.now()
    val df = DateTimeFormatter.ISO_LOCAL_DATE.format(d)

    val json = DegreeServiceImpl.degreeCredentialJsonTemplate(
      id = "credential-id",
      issuanceDate = d,
      subjectDid = "did:atala:subject-did",
      subjectFullName = "name",
      degreeAwarded = "Bachelor of Science",
      degreeResult = "First-class honors",
      graduationYear = 1995
    )

    val c = json.hcursor
    c.jsonStr("id") shouldBe "credential-id"
    c.jsonArr("type") shouldBe List("VerifiableCredential", "AirsideDegreeCredential")
    c.jsonStr("issuer.id") shouldBe "did:atala:6c170e91-92b0-4265-909d-951c11f30caa"
    c.jsonStr("issuer.name") shouldBe "University of Innovation and Technology"
    c.jsonStr("issuanceDate") shouldBe df
    c.jsonStr("credentialSubject.id") shouldBe "did:atala:subject-did"
    c.jsonStr("credentialSubject.name") shouldBe "name"
    c.jsonStr("credentialSubject.degreeAwarded") shouldBe "Bachelor of Science"
    c.jsonStr("credentialSubject.degreeResult") shouldBe "First-class honors"
    c.jsonNum[Int]("credentialSubject.graduationYear") shouldBe 1995
  }
}
