package io.iohk.atala.prism.intdemo

import io.iohk.atala.prism.intdemo.EmploymentServiceImpl.RequiredEmploymentData
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import java.time.LocalDate

class EmploymentDataSpec extends AnyFlatSpec {
  "EmploymentData" should "extract the data from an employment credential" in {
    val expectedFirstName = "First name"
    val expectedDoB = LocalDate.of(1973, 6, 6)
    val idCredential =
      IdServiceImpl.getIdCredential((expectedFirstName, expectedDoB))
    val degreeCredential = DegreeServiceImpl.getDegreeCredential(idCredential)
    val employmentCredential = EmploymentServiceImpl.getEmploymentCredential(
      RequiredEmploymentData(idCredential, degreeCredential)
    )
    val employmentData = EmploymentCredentialData(employmentCredential)

    employmentData.employerName shouldBe "Decentralized Inc."
    employmentData.employerAddress shouldBe "67 Clasper Way, Herefoot, HF1 0AF"
  }
}
