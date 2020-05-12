package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.iohk.cvp.intdemo.Testing._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class EmploymentServiceImplSpec extends FlatSpec {

  "employmentCredentialJsonTemplate" should "render an employment credential correctly" in {
    val json = EmploymentServiceImpl.employmentCredentialJsonTemplate(
      id = "credential-id",
      issuanceDate = LocalDate.of(1980, 2, 2),
      subjectDid = "did:atala:subject-did",
      subjectFullName = "name",
      employmentStartDate = LocalDate.of(1980, 1, 1),
      employmentStatus = "Some status"
    )

    val c = json.hcursor
    c.jsonStr("id") shouldBe "credential-id"
    c.jsonArr("type") shouldBe List("VerifiableCredential", "AtalaEmploymentCredential")
    c.jsonStr("issuer.id") shouldBe "did:atala:12c28b34-95be-4801-951e-c775f89d05ba"
    c.jsonStr("issuer.name") shouldBe "Decentralized Inc."
    c.jsonStr("issuer.address") shouldBe "67 Clasper Way, Herefoot, HF1 0AF"
    c.jsonStr("issuanceDate") shouldBe "1980-02-02"
    c.jsonStr("employmentStartDate") shouldBe "1980-01-01"
    c.jsonStr("employmentStatus") shouldBe "Some status"
    c.jsonStr("credentialSubject.id") shouldBe "did:atala:subject-did"
    c.jsonStr("credentialSubject.name") shouldBe "name"
  }
}
