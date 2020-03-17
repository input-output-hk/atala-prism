package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.iohk.cvp.intdemo.EmploymentServiceImpl.RequiredEmploymentData
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class EmploymentDataSpec extends FlatSpec {
  "EmploymentData" should "extract the data from an employment credential" in {
    val expectedFirstName = "first-name"
    val expectedDoB = LocalDate.of(1973, 6, 6)
    val idCredential = IdServiceImpl.getIdCredential(expectedFirstName, expectedDoB)
    val employmentCredential = EmploymentServiceImpl.getEmploymentCredential(
      RequiredEmploymentData(idCredential, DegreeServiceImpl.getDegreeCredential(idCredential))
    )
    val employmentData = EmploymentData.toEmploymentData(employmentCredential)

    employmentData.employerName shouldBe "Atala Inc."
    employmentData.employerAddress shouldBe "67 Clasper Way, Herefoot, HF1 0AF"
  }
}
