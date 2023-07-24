package io.iohk.atala.prism.intdemo

import java.time.LocalDate

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class IdDataSpec extends AnyFlatSpec {

  "IdData" should "extract the data from an IDCredential" in {
    val expectedFirstName = "first-name"
    val expectedDoB = LocalDate.of(1973, 6, 6)
    val credential =
      IdServiceImpl.getIdCredential((expectedFirstName, expectedDoB))
    val idData = IdCredentialData(credential)

    idData.name shouldBe expectedFirstName
    idData.dateOfBirth shouldBe expectedDoB
  }
}
